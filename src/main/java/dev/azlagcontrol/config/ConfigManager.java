package dev.azlagcontrol.config;

import dev.azlagcontrol.AzLagControl;

import java.io.File;

public final class ConfigManager {

    private final AzLagControl plugin;

    public ConfigManager(AzLagControl plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
    }

    public void reload() {
        plugin.reloadConfig();
    }

    /** Returns the config file path for reference. */
    public File getConfigFile() {
        return new File(plugin.getDataFolder(), "config.yml");
    }

    public String getPrefix() {
        return plugin.getConfig().getString("core.prefix", "&8[&6AzLagControl&8]&r");
    }

    public boolean isDebug() {
        return plugin.getConfig().getBoolean("core.debug", false);
    }
}
