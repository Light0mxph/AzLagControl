package dev.azlagcontrol.module.cleaner;

import dev.azlagcontrol.AzLagControl;
import dev.azlagcontrol.event.AzCleanupEvent;
import dev.azlagcontrol.module.base.AbstractModule;
import dev.azlagcontrol.scheduler.TaskScheduler;
import dev.azlagcontrol.util.EntityUtil;
import dev.azlagcontrol.util.ServerUtil;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * Smart entity/item cleaner.
 * Broadcasts warnings → cleans → fires AzCleanupEvent.
 *
 * Protection order: boss > named > tamed > leashed > passenger > player-drop grace.
 * No aggressive mob cleanup unless explicitly enabled.
 */
public final class SmartCleanerModule extends AbstractModule {

    private final TaskScheduler scheduler;

    // Config
    private int intervalSeconds;
    private List<Integer> warnTimes;
    private String msgWarn;
    private String msgDone;
    private String msgManualDone;

    private boolean removeDrops;
    private boolean removeArrows;
    private boolean removeProjectiles;
    private boolean removeMobs;
    private boolean removeVehicles;
    private boolean removeMisc;

    private boolean protectNamed;
    private boolean protectTamed;
    private boolean protectLeashed;
    private boolean protectPassengers;
    private boolean protectBosses;
    private boolean protectFromPlayers;
    private int playerDropGrace;

    private Set<EntityType> protectedTypes;

    // State
    private int countdown;
    private BukkitTask countdownTask;
    private PlayerDropListener dropListener;

    // Track when items were dropped by players (entity UUID → drop time ms)
    private final Map<UUID, Long> playerDropTimes = new WeakHashMap<>();

    public SmartCleanerModule(AzLagControl plugin) {
        super(plugin);
        this.scheduler = new TaskScheduler(plugin);
    }

    @Override
    public String getId() { return "cleaner"; }

    @Override
    public String getDisplayName() { return "Smart Cleaner"; }

    @Override
    protected String getConfigKey() { return "cleaner"; }

    @Override
    public void loadConfig() {
        intervalSeconds  = cfg("interval", 300);
        msgWarn          = cfg("messages.warn", "&e[AzLagControl] &fAutomatic cleanup in &c{time} &fseconds!");
        msgDone          = cfg("messages.done", "&a[AzLagControl] &fCleanup complete! Removed &c{removed} &fentities.");
        msgManualDone    = cfg("messages.manual-done", "&a[AzLagControl] &fManual cleanup complete! Removed &c{removed} &fentities.");

        removeDrops      = cfg("remove.dropped-items", true);
        removeArrows     = cfg("remove.arrows", true);
        removeProjectiles= cfg("remove.other-projectiles", true);
        removeMobs       = cfg("remove.mobs", false);
        removeVehicles   = cfg("remove.vehicles", false);
        removeMisc       = cfg("remove.miscellaneous", true);

        protectNamed     = cfg("protect.named", true);
        protectTamed     = cfg("protect.tamed", true);
        protectLeashed   = cfg("protect.leashed", true);
        protectPassengers= cfg("protect.passengers", true);
        protectBosses    = cfg("protect.bosses", true);
        protectFromPlayers= cfg("protect.from-players", true);
        playerDropGrace  = cfg("protect.player-drop-grace", 10);

        List<String> rawProtected = plugin.getConfig().getStringList("cleaner.protected-types");
        protectedTypes = new HashSet<>();
        for (String s : rawProtected) {
            try { protectedTypes.add(EntityType.valueOf(s.toUpperCase())); }
            catch (IllegalArgumentException ignored) {}
        }

        warnTimes = plugin.getConfig().getIntegerList("cleaner.warn-times");
        if (warnTimes.isEmpty()) warnTimes = List.of(60, 30, 10, 5, 3, 2, 1);

        loadWorldFilter();
    }

    @Override
    public void onEnable() {
        // Register listener to track player drops
        dropListener = new PlayerDropListener(playerDropTimes, plugin);
        registerListener(dropListener);

        startCountdown();
        logInfo("Cleanup every " + intervalSeconds + "s. Drops=" + removeDrops
                + " Mobs=" + removeMobs + " Arrows=" + removeArrows);
    }

    @Override
    public void onDisable() {
        scheduler.cancelAll();
        if (dropListener != null) {
            unregisterListener(dropListener);
            dropListener = null;
        }
        playerDropTimes.clear();
    }

    private void startCountdown() {
        countdown = intervalSeconds;
        countdownTask = scheduler.runTimer(() -> {
            countdown--;
            if (warnTimes.contains(countdown)) {
                String msg = msgWarn.replace("{time}", String.valueOf(countdown));
                ServerUtil.broadcastAll(msg);
            }
            if (countdown <= 0) {
                performCleanup(false);
                countdown = intervalSeconds; // reset
            }
        }, 20L, 20L); // every second
    }

    /**
     * Executes a cleanup sweep across all enabled worlds.
     * @param manual true if triggered by command
     * @return number of entities removed
     */
    public int performCleanup(boolean manual) {
        long graceMs = playerDropGrace * 1000L;
        long now = System.currentTimeMillis();

        int removed = 0;
        List<World> worlds = new ArrayList<>();

        for (World world : Bukkit.getWorlds()) {
            if (!isWorldEnabled(world)) continue;
            worlds.add(world);

            for (Entity entity : world.getEntities()) {
                if (shouldRemove(entity, now, graceMs)) {
                    entity.remove();
                    removed++;
                }
            }
        }

        String msg = (manual ? msgManualDone : msgDone)
                .replace("{removed}", String.valueOf(removed));
        ServerUtil.broadcastAll(msg);

        AzCleanupEvent event = new AzCleanupEvent(worlds, removed, manual);
        Bukkit.getPluginManager().callEvent(event);

        logInfo("Cleanup complete: removed " + removed + " entities. Manual=" + manual);
        return removed;
    }

    private boolean shouldRemove(Entity entity, long nowMs, long graceMs) {
        if (!entity.isValid()) return false;
        if (protectedTypes.contains(entity.getType())) return false;

        // Protection checks
        if (EntityUtil.isProtected(entity, protectNamed, protectTamed,
                protectLeashed, protectBosses, protectPassengers)) return false;

        // Player-drop grace period
        if (protectFromPlayers && entity instanceof Item) {
            Long dropTime = playerDropTimes.get(entity.getUniqueId());
            if (dropTime != null && (nowMs - dropTime) < graceMs) return false;
        }

        // What to remove
        if (entity instanceof Item) return removeDrops;
        if (entity instanceof Arrow) return removeArrows;
        if (entity instanceof Projectile) return removeProjectiles;
        if (entity instanceof Vehicle) return removeVehicles;

        if (entity instanceof Mob) {
            if (EntityUtil.isBoss(entity)) return false;
            return removeMobs;
        }

        // Misc: experience orbs, fireworks, etc.
        if (entity instanceof ExperienceOrb || entity instanceof Firework) return removeMisc;

        return false;
    }

    /** Returns seconds until next scheduled cleanup. */
    public int getNextCleanupSeconds() { return countdown; }
}
