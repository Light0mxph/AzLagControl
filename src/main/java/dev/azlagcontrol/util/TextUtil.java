package dev.azlagcontrol.util;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public final class TextUtil {

    private TextUtil() {}

    /** Translates &-color codes to Bukkit color codes. */
    public static String colorize(String text) {
        if (text == null) return "";
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    /** Sends a prefixed message to sender with color translation. */
    public static void send(CommandSender sender, String prefix, String message) {
        sender.sendMessage(colorize(prefix + " " + message));
    }

    /** Sends a plain (no prefix) message to sender. */
    public static void sendRaw(CommandSender sender, String message) {
        sender.sendMessage(colorize(message));
    }

    /** Replaces placeholders in template: {key} → value. */
    public static String replace(String template, Object... pairs) {
        if (pairs.length % 2 != 0) throw new IllegalArgumentException("Pairs must be even");
        String result = template;
        for (int i = 0; i < pairs.length; i += 2) {
            result = result.replace("{" + pairs[i] + "}", String.valueOf(pairs[i + 1]));
        }
        return result;
    }

    /** Formats bytes to human-readable string (MB, GB). */
    public static String formatMemory(long bytes) {
        if (bytes >= 1_073_741_824L) return String.format("%.2f GB", bytes / 1_073_741_824.0);
        if (bytes >= 1_048_576L) return String.format("%.2f MB", bytes / 1_048_576.0);
        if (bytes >= 1024L) return String.format("%.2f KB", bytes / 1024.0);
        return bytes + " B";
    }

    /** Formats seconds to human-readable time string. */
    public static String formatTime(long seconds) {
        if (seconds >= 3600) return (seconds / 3600) + "h " + ((seconds % 3600) / 60) + "m";
        if (seconds >= 60) return (seconds / 60) + "m " + (seconds % 60) + "s";
        return seconds + "s";
    }

    /** Builds a simple bar graph. E.g.: [████░░░░░░] 40% */
    public static String progressBar(double value, double max, int width) {
        int filled = (int) Math.round((value / max) * width);
        filled = Math.max(0, Math.min(width, filled));
        String color = (value / max) < 0.5 ? "&a" : (value / max) < 0.8 ? "&e" : "&c";
        return color + "█".repeat(filled) + "&8" + "░".repeat(width - filled);
    }
}
