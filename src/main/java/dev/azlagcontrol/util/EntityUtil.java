package dev.azlagcontrol.util;

import org.bukkit.entity.*;

import java.util.Set;

/**
 * Entity classification and protection checks.
 * Centralizes all entity-related decisions to avoid scattered instanceof chains.
 */
public final class EntityUtil {

    // Boss entity types — always protected
    private static final Set<EntityType> BOSS_TYPES = Set.of(
            EntityType.ENDER_DRAGON,
            EntityType.WITHER,
            EntityType.ELDER_GUARDIAN,
            EntityType.WARDEN
    );

    private EntityUtil() {}

    /**
     * Returns true if entity should be protected from automated removal.
     * Checks: named, tamed, leashed, boss, passenger, or rider.
     */
    public static boolean isProtected(Entity entity, boolean checkNamed, boolean checkTamed,
                                      boolean checkLeashed, boolean checkBosses,
                                      boolean checkPassengers) {
        if (entity == null || !entity.isValid()) return false;

        if (checkBosses && isBoss(entity)) return true;
        if (checkNamed && entity.getCustomName() != null) return true;
        if (checkPassengers && (!entity.getPassengers().isEmpty() || entity.isInsideVehicle())) return true;
        if (checkLeashed && entity instanceof LivingEntity le && le.isLeashed()) return true;
        if (checkTamed && entity instanceof Tameable t && t.isTamed()) return true;

        return false;
    }

    public static boolean isBoss(Entity entity) {
        return BOSS_TYPES.contains(entity.getType());
    }

    /** Returns true if entity is a hostile mob. */
    public static boolean isMonster(Entity entity) {
        return entity instanceof Monster || entity instanceof Ghast
                || entity instanceof Slime || entity instanceof MagmaCube
                || entity instanceof Phantom;
    }

    /** Returns true if entity is a passive/neutral animal. Ambient mobs (bats)
     *  are tracked separately under the ambient limit, not here. */
    public static boolean isAnimal(Entity entity) {
        return entity instanceof Animals || entity instanceof Golem;
    }

    /** Returns true if entity is a villager-type entity. */
    public static boolean isVillagerType(Entity entity) {
        return entity instanceof Villager || entity instanceof WanderingTrader
                || entity instanceof AbstractVillager;
    }

    /** Returns true if entity is a water creature. */
    public static boolean isWaterCreature(Entity entity) {
        return entity instanceof WaterMob;
    }

    /** Counts entities in a chunk by category. */
    public static EntityCounts countChunkEntities(Entity[] entities) {
        int total = 0, monsters = 0, animals = 0, villagers = 0, water = 0, ambient = 0,
                items = 0, projectiles = 0, vehicles = 0, other = 0;

        for (Entity e : entities) {
            if (!e.isValid()) continue;
            total++;
            // Mirror isMonster(): MagmaCube extends Slime, Phantom does not.
            if (e instanceof Monster || e instanceof Ghast || e instanceof Slime
                    || e instanceof Phantom) monsters++;
            else if (e instanceof Animals || e instanceof Golem) animals++;
            else if (e instanceof AbstractVillager) villagers++;
            else if (e instanceof WaterMob) water++;
            else if (e instanceof Ambient) ambient++;
            else if (e instanceof Item) items++;
            else if (e instanceof Projectile) projectiles++;
            else if (e instanceof Vehicle) vehicles++;
            else other++;
        }

        return new EntityCounts(total, monsters, animals, villagers, water, ambient,
                items, projectiles, vehicles, other);
    }

    public record EntityCounts(
            int total, int monsters, int animals, int villagers, int water, int ambient,
            int items, int projectiles, int vehicles, int other
    ) {}
}
