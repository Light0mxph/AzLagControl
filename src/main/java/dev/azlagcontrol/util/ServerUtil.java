package dev.azlagcontrol.util;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;

public final class ServerUtil {

    private static final boolean IS_PAPER;
    private static final boolean IS_FOLIA;

    static {
        IS_PAPER = classExists("io.papermc.paper.configuration.GlobalConfiguration")
                || classExists("com.destroystokyo.paper.PaperConfig");
        IS_FOLIA = classExists("io.papermc.paper.threadedregions.RegionizedServer");
    }

    private ServerUtil() {}

    public static boolean isPaper() { return IS_PAPER; }
    public static boolean isFolia() { return IS_FOLIA; }

    /** Total live entity count across all worlds. */
    public static int totalEntityCount() {
        int count = 0;
        for (World world : Bukkit.getWorlds()) {
            count += world.getEntityCount();
        }
        return count;
    }

    /** Total item (dropped) count across all worlds. */
    public static int totalItemCount() {
        int count = 0;
        for (World world : Bukkit.getWorlds()) {
            for (Entity e : world.getEntities()) {
                if (e instanceof org.bukkit.entity.Item) count++;
            }
        }
        return count;
    }

    /** Total loaded chunk count across all worlds. */
    public static int totalLoadedChunks() {
        int count = 0;
        for (World world : Bukkit.getWorlds()) {
            count += world.getLoadedChunks().length;
        }
        return count;
    }

    /** Online player count. */
    public static int playerCount() {
        return Bukkit.getOnlinePlayers().size();
    }

    /** Used heap memory in bytes. */
    public static long usedMemoryBytes() {
        MemoryUsage heap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        return heap.getUsed();
    }

    /** Max heap memory in bytes. */
    public static long maxMemoryBytes() {
        MemoryUsage heap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        return heap.getMax();
    }

    /** Memory usage as 0.0–1.0 ratio. */
    public static double memoryRatio() {
        long max = maxMemoryBytes();
        if (max <= 0) return 0.0;
        return (double) usedMemoryBytes() / max;
    }

    /** Broadcasts message to all online players with the given permission. */
    public static void broadcastToPermission(String permission, String message) {
        String colored = TextUtil.colorize(message);
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission(permission)) p.sendMessage(colored);
        }
    }

    /** Broadcasts to all online players. */
    public static void broadcastAll(String message) {
        Bukkit.broadcastMessage(TextUtil.colorize(message));
    }

    private static boolean classExists(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
