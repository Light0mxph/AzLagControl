package dev.azlagcontrol.util;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * TPS and MSPT utility.
 * Uses Paper's native getTPS()/getAverageTickTime() when available.
 * Falls back to manual tick-time measurement on Spigot.
 */
public final class TpsUtil {

    private static final boolean HAS_PAPER_TPS = detectPaperTps();
    private static final boolean HAS_PAPER_MSPT = detectPaperMspt();

    // Manual tracking — used only when Paper API is unavailable
    private static final int MAX_SAMPLES = 1200; // 60 seconds @ 20tps
    private static final Deque<Long> TICK_NANOS = new ArrayDeque<>(MAX_SAMPLES + 1);
    private static long lastTickNano = System.nanoTime();
    private static BukkitTask tickTask;

    private TpsUtil() {}

    public static void start(JavaPlugin plugin) {
        if (HAS_PAPER_TPS) return; // Paper handles this natively
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long now = System.nanoTime();
            synchronized (TICK_NANOS) {
                TICK_NANOS.addLast(now - lastTickNano);
                if (TICK_NANOS.size() > MAX_SAMPLES) TICK_NANOS.pollFirst();
            }
            lastTickNano = now;
        }, 1L, 1L);
    }

    public static void stop() {
        if (tickTask != null && !tickTask.isCancelled()) {
            tickTask.cancel();
            tickTask = null;
        }
    }

    /** Returns current TPS (1-minute window on Paper, 5-second window on Spigot). */
    public static double getTPS() {
        if (HAS_PAPER_TPS) {
            try {
                double[] arr = (double[]) Bukkit.getServer().getClass()
                        .getMethod("getTPS").invoke(Bukkit.getServer());
                return Math.min(20.0, arr[0]);
            } catch (Exception ignored) {}
        }
        return manualTPS(100);
    }

    /** Returns [1min, 5min, 15min] TPS array. */
    public static double[] getTPSHistory() {
        if (HAS_PAPER_TPS) {
            try {
                double[] arr = (double[]) Bukkit.getServer().getClass()
                        .getMethod("getTPS").invoke(Bukkit.getServer());
                for (int i = 0; i < arr.length; i++) arr[i] = Math.min(20.0, arr[i]);
                return arr;
            } catch (Exception ignored) {}
        }
        return new double[]{manualTPS(100), manualTPS(600), manualTPS(1200)};
    }

    /** Returns average MSPT for the last 20 ticks. */
    public static double getMSPT() {
        if (HAS_PAPER_MSPT) {
            try {
                return (double) Bukkit.getServer().getClass()
                        .getMethod("getAverageTickTime").invoke(Bukkit.getServer());
            } catch (Exception ignored) {}
        }
        return manualMSPT();
    }

    /** Returns a color-coded TPS string (green ≥18, yellow ≥15, red <15). */
    public static String formatTPS(double tps) {
        String color = tps >= 18.0 ? "&a" : tps >= 15.0 ? "&e" : "&c";
        return color + String.format("%.2f", tps);
    }

    /** Returns a color-coded MSPT string. */
    public static String formatMSPT(double mspt) {
        String color = mspt <= 40.0 ? "&a" : mspt <= 50.0 ? "&e" : "&c";
        return color + String.format("%.2f", mspt) + "ms";
    }

    private static double manualTPS(int samples) {
        synchronized (TICK_NANOS) {
            if (TICK_NANOS.isEmpty()) return 20.0;
            Long[] arr = TICK_NANOS.toArray(new Long[0]);
            int count = Math.min(samples, arr.length);
            long total = 0;
            for (int i = arr.length - count; i < arr.length; i++) total += arr[i];
            if (total == 0) return 20.0;
            return Math.min(20.0, (count * 1_000_000_000.0) / total);
        }
    }

    private static double manualMSPT() {
        synchronized (TICK_NANOS) {
            if (TICK_NANOS.isEmpty()) return 50.0;
            Long[] arr = TICK_NANOS.toArray(new Long[0]);
            int count = Math.min(20, arr.length);
            long total = 0;
            for (int i = arr.length - count; i < arr.length; i++) total += arr[i];
            return (double) total / count / 1_000_000.0;
        }
    }

    private static boolean detectPaperTps() {
        try {
            Method m = Bukkit.getServer().getClass().getMethod("getTPS");
            return m.getReturnType() == double[].class;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    private static boolean detectPaperMspt() {
        try {
            Bukkit.getServer().getClass().getMethod("getAverageTickTime");
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }
}
