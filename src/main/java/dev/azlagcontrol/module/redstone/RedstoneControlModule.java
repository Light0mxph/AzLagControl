package dev.azlagcontrol.module.redstone;

import dev.azlagcontrol.AzLagControl;
import dev.azlagcontrol.module.base.AbstractModule;
import dev.azlagcontrol.scheduler.TaskScheduler;
import dev.azlagcontrol.util.BlockKey;
import dev.azlagcontrol.util.ServerUtil;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockRedstoneEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Redstone update rate limiter and clock detector.
 *
 * Tracks updates per chunk per second.
 * Detects blocks that fire excessively (suspected redstone clocks).
 * Actions: LOG, THROTTLE (cancel excess), ALERT (notify admins).
 *
 * Note: BlockPhysicsEvent is HIGH frequency. We minimize work per event.
 * The counter reset runs once per second to keep state clean.
 */
public final class RedstoneControlModule extends AbstractModule implements Listener {

    private final TaskScheduler scheduler;

    // Config
    private int maxUpdatesPerChunkPerSecond;
    private boolean clockDetectionEnabled;
    private int clockThreshold;
    private int clockWindowTicks;
    private Action action;
    private boolean notifyAdmins;

    // State: chunk key → update count this second
    private final Map<Long, AtomicInteger> chunkUpdateCounts = new HashMap<>();
    // State: block key → [fireCount, windowStart ticks]
    private final Map<Long, long[]> blockFireHistory = new HashMap<>();

    // Ticks since server start. long so it never overflows (int would wrap
    // after ~3.4y of uptime and corrupt the elapsed-window math).
    private long currentTick = 0L;

    public RedstoneControlModule(AzLagControl plugin) {
        super(plugin);
        this.scheduler = new TaskScheduler(plugin);
    }

    @Override
    public String getId() { return "redstone"; }

    @Override
    public String getDisplayName() { return "Redstone Control"; }

    @Override
    protected String getConfigKey() { return "redstone"; }

    @Override
    public void loadConfig() {
        maxUpdatesPerChunkPerSecond = cfg("max-updates-per-chunk-per-second", 800);
        clockDetectionEnabled       = cfg("clock-detection.enabled", true);
        clockThreshold              = cfg("clock-detection.threshold", 60);
        clockWindowTicks            = cfg("clock-detection.window-ticks", 100);
        notifyAdmins                = cfg("clock-detection.notify-admins", true);
        String actionStr            = cfg("clock-detection.action", "THROTTLE");
        try { action = Action.valueOf(actionStr.toUpperCase()); }
        catch (IllegalArgumentException e) { action = Action.THROTTLE; }
        loadWorldFilter();
    }

    @Override
    public void onEnable() {
        registerListener(this);
        // Reset per-second counters every 20 ticks
        scheduler.runTimer(() -> {
            chunkUpdateCounts.clear();
            currentTick += 20;
            // Clean old block history entries
            if (currentTick % 200 == 0) cleanBlockHistory();
        }, 20L, 20L);
        logInfo("Max updates/chunk/s=" + maxUpdatesPerChunkPerSecond
                + " clock-detection=" + clockDetectionEnabled + " action=" + action);
    }

    @Override
    public void onDisable() {
        unregisterListener(this);
        scheduler.cancelAll();
        chunkUpdateCounts.clear();
        blockFireHistory.clear();
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onRedstoneChange(BlockRedstoneEvent event) {
        if (!isWorldEnabled(event.getBlock().getWorld())) return;

        Location loc = event.getBlock().getLocation();
        long chunkKey = BlockKey.chunk(loc);

        // Track chunk update count
        AtomicInteger count = chunkUpdateCounts.computeIfAbsent(chunkKey, k -> new AtomicInteger(0));
        int updates = count.incrementAndGet();

        // Clock detection
        if (clockDetectionEnabled) {
            detectClock(loc, updates);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBlockPhysics(BlockPhysicsEvent event) {
        // BlockPhysicsEvent is the single highest-frequency event in the game.
        // If we aren't throttling, this handler can only waste CPU — bail before
        // any map lookup or allocation.
        if (action != Action.THROTTLE || maxUpdatesPerChunkPerSecond <= 0) return;

        Block block = event.getBlock();
        if (!isWorldEnabled(block.getWorld())) return;

        // Use block coords directly — no Location object allocated per event.
        long chunkKey = BlockKey.chunk(block.getWorld(), block.getX(), block.getZ());
        AtomicInteger count = chunkUpdateCounts.computeIfAbsent(chunkKey, k -> new AtomicInteger(0));

        if (count.incrementAndGet() > maxUpdatesPerChunkPerSecond) {
            event.setCancelled(true);
        }
    }

    private void detectClock(Location loc, int chunkUpdates) {
        long blockKey = BlockKey.block(loc);
        long[] history = blockFireHistory.computeIfAbsent(blockKey, k -> new long[]{0L, currentTick});

        long elapsed = currentTick - history[1];
        if (elapsed > clockWindowTicks) {
            // Reset window
            history[0] = 1;
            history[1] = currentTick;
            return;
        }

        history[0]++;
        if (history[0] >= clockThreshold) {
            handleClockDetection(loc, history[0]);
            history[0] = 0; // Reset to avoid repeated alerts
        }
    }

    private void handleClockDetection(Location loc, long fireCount) {
        String worldName = loc.getWorld().getName();
        String blockPos = loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();

        switch (action) {
            case LOG -> logWarn("Suspected redstone clock at " + worldName + " [" + blockPos + "]"
                    + " fires=" + fireCount);
            case THROTTLE -> {
                logWarn("Throttling redstone clock at " + worldName + " [" + blockPos + "]");
                if (notifyAdmins) {
                    ServerUtil.broadcastToPermission("azlagcontrol.stats",
                            "&e[AzLagControl] &fRedstone clock detected at &e" + worldName
                                    + " &f[" + blockPos + "]");
                }
            }
            case ALERT -> {
                if (notifyAdmins) {
                    ServerUtil.broadcastToPermission("azlagcontrol.stats",
                            "&c[AzLagControl] &fRedstone clock at &c" + worldName
                                    + " &f[" + blockPos + "] — &c" + fireCount + " fires/window");
                }
            }
        }
    }

    private void cleanBlockHistory() {
        blockFireHistory.entrySet().removeIf(e -> (currentTick - e.getValue()[1]) > clockWindowTicks * 3);
    }

    public enum Action { LOG, THROTTLE, ALERT }
}
