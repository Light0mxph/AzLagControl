package dev.azlagcontrol.module.emergency;

import dev.azlagcontrol.AzLagControl;
import dev.azlagcontrol.event.AzEmergencyModeChangeEvent;
import dev.azlagcontrol.module.base.AbstractModule;
import dev.azlagcontrol.module.hopper.HopperControlModule;
import dev.azlagcontrol.scheduler.TaskScheduler;
import dev.azlagcontrol.util.EntityUtil;
import dev.azlagcontrol.util.ServerUtil;
import dev.azlagcontrol.util.TpsUtil;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;

/**
 * Emergency Mode: auto-activates when server TPS drops critically low.
 *
 * Progressive measures (configurable):
 *   1. Reduce view distance
 *   2. Disable mob spawning
 *   3. Aggressive entity purge
 *   4. Remove all TNT entities
 *   (5. Disable hoppers — very aggressive, off by default)
 *
 * Auto-deactivates when TPS stabilizes above deactivate threshold for
 * stability-duration-seconds.
 *
 * Called by PerformanceMonitorModule via tpsListener callback.
 */
public final class EmergencyModeModule extends AbstractModule implements Listener {

    private final TaskScheduler scheduler;

    // Config
    private double autoActivateTps;
    private double autoDeactivateTps;
    private int stabilityDurationSeconds;
    private boolean reduceViewDistance;
    private int targetViewDistance;
    private boolean disableMobSpawning;
    private boolean aggressivePurge;
    private int purgeIntervalSeconds;
    private boolean removeTnt;
    private boolean disableHoppers;
    private String msgActivate;
    private String msgDeactivate;
    private String msgManualActivate;
    private String msgManualDeactivate;

    // State
    private boolean emergencyActive = false;
    private long stableAboveThresholdMs = 0L;
    private final Map<String, Integer> originalViewDistances = new HashMap<>();
    private BukkitTask purgeTask;

    public EmergencyModeModule(AzLagControl plugin) {
        super(plugin);
        this.scheduler = new TaskScheduler(plugin);
    }

    @Override
    public String getId() { return "emergency"; }

    @Override
    public String getDisplayName() { return "Emergency Mode"; }

    @Override
    protected String getConfigKey() { return "emergency"; }

    @Override
    public void loadConfig() {
        autoActivateTps          = cfg("auto-activate-tps", 13.0);
        autoDeactivateTps        = cfg("auto-deactivate-tps", 17.0);
        stabilityDurationSeconds = cfg("stability-duration-seconds", 30);
        reduceViewDistance       = cfg("actions.reduce-view-distance.enabled", true);
        targetViewDistance       = cfg("actions.reduce-view-distance.target", 6);
        disableMobSpawning       = cfg("actions.disable-mob-spawning", true);
        aggressivePurge          = cfg("actions.aggressive-entity-purge.enabled", true);
        purgeIntervalSeconds     = cfg("actions.aggressive-entity-purge.interval-seconds", 20);
        removeTnt                = cfg("actions.remove-tnt", true);
        disableHoppers           = cfg("actions.disable-hoppers", false);
        msgActivate              = cfg("messages.activate",
                "&c[AzLagControl] &4EMERGENCY MODE ACTIVATED &c— TPS: &4{tps}");
        msgDeactivate            = cfg("messages.deactivate",
                "&a[AzLagControl] &aEmergency mode deactivated. TPS stable: &2{tps}");
        msgManualActivate        = cfg("messages.manual-activate",
                "&c[AzLagControl] &4Emergency mode manually activated by &c{player}");
        msgManualDeactivate      = cfg("messages.manual-deactivate",
                "&a[AzLagControl] &aEmergency mode manually deactivated by &2{player}");
    }

    @Override
    public void onEnable() {
        // Listener for mob spawn cancellation in emergency mode
        registerListener(this);
        logInfo("Auto-activate below TPS=" + autoActivateTps
                + " auto-deactivate above=" + autoDeactivateTps);
    }

    @Override
    public void onDisable() {
        if (emergencyActive) deactivate(null);
        unregisterListener(this);
        scheduler.cancelAll();
    }

    /** Called by PerformanceMonitorModule every sample interval. */
    public void onTpsUpdate(double tps) {
        if (!emergencyActive) {
            if (tps < autoActivateTps) {
                activate(null);
            }
        } else {
            // Track how long we've been stable
            if (tps >= autoDeactivateTps) {
                if (stableAboveThresholdMs == 0L) stableAboveThresholdMs = System.currentTimeMillis();
                long stableMs = System.currentTimeMillis() - stableAboveThresholdMs;
                if (stableMs >= stabilityDurationSeconds * 1000L) {
                    deactivate(null);
                }
            } else {
                stableAboveThresholdMs = 0L;
            }
        }
    }

    /** Activates emergency mode. player = null means auto-triggered. */
    public void activate(org.bukkit.entity.Player player) {
        if (emergencyActive) return;
        emergencyActive = true;
        stableAboveThresholdMs = 0L;

        double tps = TpsUtil.getTPS();
        String msg = (player == null ? msgActivate : msgManualActivate)
                .replace("{tps}", String.format("%.2f", tps))
                .replace("{player}", player != null ? player.getName() : "AUTO");
        ServerUtil.broadcastAll(msg);

        applyMeasures();

        AzEmergencyModeChangeEvent event = new AzEmergencyModeChangeEvent(true, tps, player);
        Bukkit.getPluginManager().callEvent(event);

        logInfo("Emergency mode ACTIVATED. TPS=" + String.format("%.2f", tps));
    }

    /** Deactivates emergency mode. player = null means auto-triggered. */
    public void deactivate(org.bukkit.entity.Player player) {
        if (!emergencyActive) return;
        emergencyActive = false;
        stableAboveThresholdMs = 0L;

        double tps = TpsUtil.getTPS();
        String msg = (player == null ? msgDeactivate : msgManualDeactivate)
                .replace("{tps}", String.format("%.2f", tps))
                .replace("{player}", player != null ? player.getName() : "AUTO");
        ServerUtil.broadcastAll(msg);

        restoreMeasures();

        if (purgeTask != null) {
            scheduler.cancel(purgeTask);
            purgeTask = null;
        }

        AzEmergencyModeChangeEvent event = new AzEmergencyModeChangeEvent(false, tps, player);
        Bukkit.getPluginManager().callEvent(event);

        logInfo("Emergency mode DEACTIVATED. TPS=" + String.format("%.2f", tps));
    }

    private void applyMeasures() {
        if (reduceViewDistance) applyViewDistance(targetViewDistance);

        if (removeTnt) removeTntEntities();

        if (disableHoppers) toggleHoppers(true);

        if (aggressivePurge) {
            long periodTicks = purgeIntervalSeconds * 20L;
            purgeTask = scheduler.runTimer(this::aggressivePurge, periodTicks, periodTicks);
        }
    }

    private void restoreMeasures() {
        if (reduceViewDistance) restoreViewDistances();
        if (disableHoppers) toggleHoppers(false);
    }

    /** Halts/restores hopper transfers via the Hopper Control module, if loaded. */
    private void toggleHoppers(boolean disabled) {
        HopperControlModule hopper = plugin.getModuleManager().get(HopperControlModule.class);
        if (hopper == null || !hopper.isLoaded()) {
            logWarn("disable-hoppers requested but Hopper Control module is not loaded — skipping.");
            return;
        }
        hopper.setEmergencyDisabled(disabled);
        logInfo("Hopper transfers " + (disabled ? "disabled" : "re-enabled") + " (emergency).");
    }

    private void applyViewDistance(int distance) {
        originalViewDistances.clear();
        for (World world : Bukkit.getWorlds()) {
            try {
                originalViewDistances.put(world.getName(), world.getViewDistance());
                world.setViewDistance(Math.min(world.getViewDistance(), distance));
            } catch (NoSuchMethodError | UnsupportedOperationException e) {
                // Older forks / pure Spigot may lack runtime view-distance control.
                logWarn("View distance control unsupported on this server — skipping.");
                return;
            }
        }
    }

    private void restoreViewDistances() {
        for (World world : Bukkit.getWorlds()) {
            Integer original = originalViewDistances.get(world.getName());
            if (original == null) continue;
            try {
                world.setViewDistance(original);
            } catch (NoSuchMethodError | UnsupportedOperationException ignored) {
                // Nothing to restore if the API isn't available.
            }
        }
        originalViewDistances.clear();
    }

    private void removeTntEntities() {
        int removed = 0;
        for (World world : Bukkit.getWorlds()) {
            for (Entity e : world.getEntities()) {
                if (e instanceof TNTPrimed) { e.remove(); removed++; }
            }
        }
        if (removed > 0) logInfo("Removed " + removed + " TNT entities.");
    }

    private void aggressivePurge() {
        int removed = 0;
        for (World world : Bukkit.getWorlds()) {
            for (Entity e : world.getEntities()) {
                if (e instanceof Player) continue;
                if (e instanceof Item || e instanceof Arrow
                        || (e instanceof Mob && !(e instanceof Tameable t && t.isTamed())
                            && e.getCustomName() == null && !EntityUtil.isBoss(e))) {
                    e.remove();
                    removed++;
                }
            }
        }
        logInfo("Emergency purge: removed " + removed + " entities.");
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!emergencyActive || !disableMobSpawning) return;
        // Allow natural spawning from player actions (eggs, breeding) even in emergency
        CreatureSpawnEvent.SpawnReason reason = event.getSpawnReason();
        if (reason == CreatureSpawnEvent.SpawnReason.CUSTOM
                || reason == CreatureSpawnEvent.SpawnReason.SPAWNER_EGG
                || reason == CreatureSpawnEvent.SpawnReason.BREEDING) return;
        event.setCancelled(true);
    }

    public boolean isEmergencyActive() { return emergencyActive; }
}
