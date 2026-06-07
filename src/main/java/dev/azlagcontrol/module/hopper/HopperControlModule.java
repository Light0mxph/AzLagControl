package dev.azlagcontrol.module.hopper;

import dev.azlagcontrol.AzLagControl;
import dev.azlagcontrol.module.base.AbstractModule;
import dev.azlagcontrol.scheduler.TaskScheduler;
import dev.azlagcontrol.util.BlockKey;
import dev.azlagcontrol.util.TpsUtil;
import org.bukkit.block.Block;
import org.bukkit.block.Hopper;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;

import java.util.HashMap;
import java.util.Map;

/**
 * Hopper transfer throttler.
 * Limits how often each hopper can move items by tracking last-transfer tick.
 * Dramatically reduces InventoryMoveItemEvent call frequency without breaking transfer logic.
 *
 * Note: On Paper, HopperOptimize in paper.yml handles this more efficiently at the engine level.
 * This module provides a fallback and additional control layer.
 */
public final class HopperControlModule extends AbstractModule implements Listener {

    private final TaskScheduler scheduler;

    // Config
    private int tickRate;
    private double disableBelowTps;

    // State: hopper location hash → last transfer tick
    private final Map<Long, Long> lastTransferTick = new HashMap<>();
    private long currentTick = 0L;
    private boolean forceDisabled = false;
    // Set by EmergencyModeModule. Independent of the TPS-driven forceDisabled flag
    // so the two cannot clobber each other.
    private boolean emergencyDisabled = false;

    public HopperControlModule(AzLagControl plugin) {
        super(plugin);
        this.scheduler = new TaskScheduler(plugin);
    }

    @Override
    public String getId() { return "hopper"; }

    @Override
    public String getDisplayName() { return "Hopper Control"; }

    @Override
    protected String getConfigKey() { return "hopper"; }

    @Override
    public void loadConfig() {
        tickRate        = Math.max(1, cfg("tick-rate", 16));
        disableBelowTps = cfg("disable-below-tps", 0.0);
        loadWorldFilter();
    }

    @Override
    public void onEnable() {
        registerListener(this);

        // Increment internal tick counter every tick
        scheduler.runTimer(() -> {
            currentTick++;
            // Prune stale entries every 1200 ticks (1 minute)
            if (currentTick % 1200 == 0) pruneStaleEntries();
        }, 1L, 1L);

        // Check TPS threshold if configured
        if (disableBelowTps > 0) {
            scheduler.runTimer(() -> {
                double tps = TpsUtil.getTPS();
                boolean shouldDisable = tps < disableBelowTps;
                if (shouldDisable != forceDisabled) {
                    forceDisabled = shouldDisable;
                    if (forceDisabled) logWarn("Hoppers disabled — TPS=" + String.format("%.1f", tps));
                    else logInfo("Hoppers re-enabled — TPS=" + String.format("%.1f", tps));
                }
            }, 100L, 100L); // check every 5 seconds
        }

        logInfo("Tick rate=" + tickRate
                + (disableBelowTps > 0 ? " disable-below-tps=" + disableBelowTps : ""));
    }

    @Override
    public void onDisable() {
        unregisterListener(this);
        scheduler.cancelAll();
        lastTransferTick.clear();
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        // Only care about hopper-initiated transfers
        if (event.getInitiator().getType() != InventoryType.HOPPER) return;

        Block block = event.getInitiator().getLocation().getBlock();
        if (!(block.getState() instanceof Hopper)) return;

        if (!isWorldEnabled(block.getWorld())) return;

        if (forceDisabled || emergencyDisabled) {
            event.setCancelled(true);
            return;
        }

        long key = BlockKey.block(block.getLocation());
        Long lastTick = lastTransferTick.get(key);

        if (lastTick != null && (currentTick - lastTick) < tickRate) {
            event.setCancelled(true);
            return;
        }

        lastTransferTick.put(key, currentTick);
    }

    private void pruneStaleEntries() {
        lastTransferTick.entrySet().removeIf(e -> (currentTick - e.getValue()) > 1200L);
    }

    /** Toggled by Emergency Mode to halt all hopper transfers while active. */
    public void setEmergencyDisabled(boolean disabled) { this.emergencyDisabled = disabled; }

    public boolean isForceDisabled() { return forceDisabled; }
    public boolean isEmergencyDisabled() { return emergencyDisabled; }
    public int getTickRate() { return tickRate; }
}
