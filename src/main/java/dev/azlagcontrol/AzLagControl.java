package dev.azlagcontrol;

import dev.azlagcontrol.command.AzLagCommand;
import dev.azlagcontrol.config.ConfigManager;
import dev.azlagcontrol.integration.IntegrationManager;
import dev.azlagcontrol.module.ModuleManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

/**
 * AzLagControl — Professional Lag Control Suite for Minecraft servers.
 *
 * Entry point. Owns lifecycle of ConfigManager, ModuleManager, IntegrationManager.
 * All heavy logic lives in modules — this class stays thin.
 *
 * Developed by AztrixDigitalStudio.
 */
public final class AzLagControl extends JavaPlugin {

    private static AzLagControl instance;

    private ConfigManager configManager;
    private ModuleManager moduleManager;
    private IntegrationManager integrationManager;

    @Override
    public void onEnable() {
        instance = this;

        printBanner();

        try {
            configManager = new ConfigManager(this);
            configManager.load();

            moduleManager = new ModuleManager(this);
            moduleManager.loadAll();

            integrationManager = new IntegrationManager(this);
            integrationManager.loadAll();

            AzLagCommand cmd = new AzLagCommand(this);
            org.bukkit.command.PluginCommand command = getCommand("azlagcontrol");
            if (command == null) {
                throw new IllegalStateException("Command 'azlagcontrol' missing from plugin.yml");
            }
            command.setExecutor(cmd);
            command.setTabCompleter(cmd);

            getLogger().info("AzLagControl enabled successfully.");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Fatal error during startup: " + e.getMessage(), e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (moduleManager != null) {
            moduleManager.disableAll();
        }
        if (integrationManager != null) {
            integrationManager.unloadAll();
        }
        getLogger().info("AzLagControl disabled.");
        instance = null;
    }

    public void debug(String message) {
        if (configManager != null && configManager.isDebug()) {
            getLogger().info("[Debug] " + message);
        }
    }

    public static AzLagControl getInstance() { return instance; }
    public ConfigManager getConfigManager() { return configManager; }
    public ModuleManager getModuleManager() { return moduleManager; }
    public IntegrationManager getIntegrationManager() { return integrationManager; }

    private void printBanner() {
        getLogger().info("╔══════════════════════════════════════╗");
        getLogger().info("║        AzLagControl v" + getDescription().getVersion() + "          ║");
        getLogger().info("║  Professional Lag Control Suite      ║");
        getLogger().info("║  by AztrixDigitalStudio              ║");
        getLogger().info("╚══════════════════════════════════════╝");
    }
}
