package dev.azlagcontrol.util;

import org.bukkit.Location;
import org.bukkit.World;

/**
 * Packs block / chunk coordinates into a single long for use as HashMap keys.
 *
 * Block packing follows Minecraft's standard layout:
 *   X: 26 bits (±33,554,431)  Z: 26 bits  Y: 12 bits (-2048..2047)
 * World identity is folded in via a hash to avoid cross-world collisions.
 *
 * These keys are heuristic (throttling / detection), not authoritative —
 * astronomically rare collisions only cause a one-off false throttle.
 */
public final class BlockKey {

    private BlockKey() {}

    /** Packs a block location (world-aware) into a long. */
    public static long block(Location loc) {
        return block(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    /** Packs block coords directly (world-aware) — avoids Location allocation in hot paths. */
    public static long block(World world, int bx, int by, int bz) {
        long x = bx & 0x3FFFFFFL;   // 26 bits
        long z = bz & 0x3FFFFFFL;   // 26 bits
        long y = (by + 2048L) & 0xFFFL; // 12 bits, offset for negative Y
        long packed = (x << 38) | (z << 12) | y;
        // Fold world identity into the high bits without losing block precision:
        // XOR a stable world hash into the result. Collisions remain negligible.
        return packed ^ ((long) world.getUID().hashCode() << 40);
    }

    /** Packs a chunk coordinate (world-aware) into a long. */
    public static long chunk(Location loc) {
        return chunk(loc.getWorld(), loc.getBlockX(), loc.getBlockZ());
    }

    /** Packs a chunk coordinate from block coords directly — avoids Location allocation. */
    public static long chunk(World world, int bx, int bz) {
        long cx = (bx >> 4) & 0xFFFFFFFFL;
        long cz = (bz >> 4) & 0xFFFFFFFFL;
        long packed = (cx << 32) | cz;
        return packed ^ ((long) world.getUID().hashCode() << 16);
    }
}
