package dev.azlagcontrol.module.mob;

import dev.azlagcontrol.AzLagControl;
import dev.azlagcontrol.module.base.AbstractModule;
import dev.azlagcontrol.scheduler.TaskScheduler;
import dev.azlagcontrol.util.EntityUtil;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.SpawnerSpawnEvent;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Prevents mob overpopulation per chunk.
 * Two-layer protection:
 *   1. Spawn-time prevention: cancel spawn event if chunk already at limit.
 *   2. Overflow purge: periodic task removes entities over the soft-limit.
 */
public final class SmartMobControlModule extends AbstractModule implements Listener {

    private final TaskScheduler scheduler;

    // Per-chunk limits (0 = unlimited)
    private int limitTotal;
    private int limitAnimals;
    private int limitMonsters;
    private int limitVillagers;
    private int limitWater;
    private int limitAmbient;

    // Overflow purge
    private boolean overflowEnabled;
    private int overflowIntervalSeconds;
    private boolean protectNamedOnOverflow;

    // Spawn reason filter (empty = monitor all reasons)
    private final EnumSet<CreatureSpawnEvent.SpawnReason> monitoredReasons =
            EnumSet.noneOf(CreatureSpawnEvent.SpawnReason.class);
    private boolean allReasons = true;

    // Whitelist — types that bypass limits
    private final Set<EntityType> whitelist = new HashSet<>();

    public SmartMobControlModule(AzLagControl plugin) {
        super(plugin);
        this.scheduler = new TaskScheduler(plugin);
    }

    @Override
    public String getId() { return "mob-control"; }

    @Override
    public String getDisplayName() { return "Smart Mob Control"; }

    @Override
    protected String getConfigKey() { return "mob-control"; }

    @Override
    public void loadConfig() {
        limitTotal    = cfg("per-chunk.total", 150);
        limitAnimals  = cfg("per-chunk.animals", 40);
        limitMonsters = cfg("per-chunk.monsters", 80);
        limitVillagers= cfg("per-chunk.villagers", 20);
        limitWater    = cfg("per-chunk.water-creatures", 20);
        limitAmbient  = cfg("per-chunk.ambient", 30);

        overflowEnabled         = cfg("overflow-purge.enabled", true);
        overflowIntervalSeconds = cfg("overflow-purge.interval", 120);
        protectNamedOnOverflow  = cfg("overflow-purge.protect-named", true);

        List<String> rawReasons = plugin.getConfig().getStringList("mob-control.monitored-reasons");
        monitoredReasons.clear();
        if (rawReasons.isEmpty()) {
            allReasons = true;
        } else {
            allReasons = false;
            for (String s : rawReasons) {
                try { monitoredReasons.add(CreatureSpawnEvent.SpawnReason.valueOf(s.toUpperCase())); }
                catch (IllegalArgumentException ignored) {}
            }
        }

        whitelist.clear();
        for (String s : plugin.getConfig().getStringList("mob-control.whitelist")) {
            try { whitelist.add(EntityType.valueOf(s.toUpperCase())); }
            catch (IllegalArgumentException ignored) {}
        }

        loadWorldFilter();
    }

    @Override
    public void onEnable() {
        registerListener(this);

        if (overflowEnabled) {
            long periodTicks = overflowIntervalSeconds * 20L;
            scheduler.runTimer(this::runOverflowPurge, periodTicks, periodTicks);
        }

        logInfo("Limits — total=" + limitTotal + " animals=" + limitAnimals
                + " monsters=" + limitMonsters + " villagers=" + limitVillagers);
    }

    @Override
    public void onDisable() {
        unregisterListener(this);
        scheduler.cancelAll();
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!isWorldEnabled(event.getLocation().getWorld())) return;
        if (whitelist.contains(event.getEntityType())) return;
        if (!allReasons && !monitoredReasons.contains(event.getSpawnReason())) return;

        Chunk chunk = event.getLocation().getChunk();
        if (!chunk.isLoaded()) return;

        EntityUtil.EntityCounts counts = EntityUtil.countChunkEntities(chunk.getEntities());

        if (wouldExceedLimit(event.getEntity(), counts)) {
            event.setCancelled(true);
            debug("Cancelled spawn of " + event.getEntityType()
                    + " in " + chunk.getWorld().getName()
                    + " [" + chunk.getX() + "," + chunk.getZ() + "]"
                    + " total=" + counts.total());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onSpawnerSpawn(SpawnerSpawnEvent event) {
        if (!isWorldEnabled(event.getLocation().getWorld())) return;
        if (whitelist.contains(event.getEntityType())) return;

        Chunk chunk = event.getLocation().getChunk();
        if (!chunk.isLoaded()) return;

        EntityUtil.EntityCounts counts = EntityUtil.countChunkEntities(chunk.getEntities());

        if (wouldExceedLimit(event.getEntity(), counts)) {
            event.setCancelled(true);
        }
    }

    private boolean wouldExceedLimit(Entity entity, EntityUtil.EntityCounts counts) {
        if (limitTotal > 0 && counts.total() >= limitTotal) return true;
        if (EntityUtil.isMonster(entity) && limitMonsters > 0 && counts.monsters() >= limitMonsters) return true;
        if (EntityUtil.isAnimal(entity) && limitAnimals > 0 && counts.animals() >= limitAnimals) return true;
        if (EntityUtil.isVillagerType(entity) && limitVillagers > 0 && counts.villagers() >= limitVillagers) return true;
        if (EntityUtil.isWaterCreature(entity) && limitWater > 0 && counts.water() >= limitWater) return true;
        if (entity instanceof Ambient && limitAmbient > 0 && counts.ambient() >= limitAmbient) return true;
        return false;
    }

    private void runOverflowPurge() {
        int removed = 0;
        for (World world : getEnabledWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                removed += purgeChunkOverflow(chunk);
            }
        }
        if (removed > 0) {
            logInfo("Overflow purge removed " + removed + " mobs.");
        }
    }

    private int purgeChunkOverflow(Chunk chunk) {
        Entity[] entities = chunk.getEntities();
        EntityUtil.EntityCounts counts = EntityUtil.countChunkEntities(entities);

        // Only purge if we're significantly over limit to avoid over-aggression
        if (limitTotal > 0 && counts.total() <= limitTotal) return 0;

        int removed = 0;
        int mobCount = 0;

        for (Entity entity : entities) {
            if (!(entity instanceof Mob)) continue;
            if (whitelist.contains(entity.getType())) continue;
            if (EntityUtil.isBoss(entity)) continue;
            if (protectNamedOnOverflow && entity.getCustomName() != null) continue;
            if (entity instanceof Tameable t && t.isTamed()) continue;

            mobCount++;
            if (limitTotal > 0 && mobCount > limitTotal) {
                entity.remove();
                removed++;
            }
        }

        return removed;
    }
}
