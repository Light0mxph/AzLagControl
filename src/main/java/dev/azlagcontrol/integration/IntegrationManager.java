package dev.azlagcontrol.integration;

import dev.azlagcontrol.AzLagControl;
import org.bukkit.Bukkit;

/**
 * Loads optional plugin integrations. Every integration is fully optional —
 * the plugin runs normally if none are present.
 *
 * PlaceholderAPI is handled directly here because PlaceholderExpansion already
 * defines register()/unregister() with its own (final) contract, so it cannot
 * also implement the generic AzIntegration interface.
 */
public final class IntegrationManager {

    private final AzLagControl plugin;

    private PlaceholderIntegration placeholderIntegration;

    public IntegrationManager(AzLagControl plugin) {
        this.plugin = plugin;
    }

    public void loadAll() {
        // PlaceholderAPI
        if (plugin.getConfig().getBoolean("integrations.placeholder-api.enabled", true)) {
            if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                try {
                    placeholderIntegration = new PlaceholderIntegration(plugin);
                    placeholderIntegration.register();
                    plugin.getLogger().info("  [+] PlaceholderAPI integration loaded.");
                } catch (Throwable t) {
                    placeholderIntegration = null;
                    plugin.getLogger().warning("  [!] Failed to register PlaceholderAPI expansion: " + t.getMessage());
                }
            } else {
                plugin.getLogger().info("  [-] PlaceholderAPI not found, skipping integration.");
            }
        }

        // Spark (info only)
        if (plugin.getConfig().getBoolean("integrations.spark.enabled", true)) {
            if (Bukkit.getPluginManager().getPlugin("spark") != null) {
                plugin.getLogger().info("  [+] spark detected — profiler data available.");
            }
        }
    }

    public void unloadAll() {
        if (placeholderIntegration != null) {
            try { placeholderIntegration.unregister(); } catch (Throwable ignored) {}
            placeholderIntegration = null;
        }
    }

    public PlaceholderIntegration getPlaceholderIntegration() {
        return placeholderIntegration;
    }
}
