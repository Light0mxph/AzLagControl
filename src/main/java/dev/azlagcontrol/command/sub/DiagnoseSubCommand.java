package dev.azlagcontrol.command.sub;

import dev.azlagcontrol.AzLagControl;
import dev.azlagcontrol.module.base.AbstractModule;
import dev.azlagcontrol.util.ServerUtil;
import dev.azlagcontrol.util.TextUtil;
import dev.azlagcontrol.util.TpsUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.util.Map;

/**
 * Runs a diagnostic report: module states, server environment, key metrics.
 */
public final class DiagnoseSubCommand implements SubCommand {

    private final AzLagControl plugin;

    public DiagnoseSubCommand(AzLagControl plugin) { this.plugin = plugin; }

    @Override public String getName() { return "diagnose"; }
    @Override public String getPermission() { return "azlagcontrol.diagnose"; }
    @Override public String getUsage() { return "/azlag diagnose — full system diagnostic"; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        String prefix = plugin.getConfigManager().getPrefix();

        TextUtil.sendRaw(sender, "&8&m                                        ");
        TextUtil.sendRaw(sender, " &6AzLagControl Diagnostic Report");
        TextUtil.sendRaw(sender, "&8&m                                        ");

        // Server environment
        TextUtil.sendRaw(sender, " &fServer: &e" + Bukkit.getName() + " " + Bukkit.getVersion());
        // Folia is detected for info only — AzLagControl uses the global Bukkit
        // scheduler + unsynchronized maps, so it is NOT Folia-safe. Flag it loudly.
        TextUtil.sendRaw(sender, " &fPaper: &e" + ServerUtil.isPaper()
                + "  &fFolia: &e" + ServerUtil.isFolia()
                + (ServerUtil.isFolia() ? " &c(NOT SUPPORTED — run Paper)" : ""));
        TextUtil.sendRaw(sender, " &fJava: &e" + System.getProperty("java.version"));

        // Current metrics
        TextUtil.sendRaw(sender, " &fTPS: " + TpsUtil.formatTPS(TpsUtil.getTPS())
                + "  &fMSPT: " + TpsUtil.formatMSPT(TpsUtil.getMSPT()));
        TextUtil.sendRaw(sender, " &fMemory: &e"
                + TextUtil.formatMemory(ServerUtil.usedMemoryBytes())
                + " &8/ &e" + TextUtil.formatMemory(ServerUtil.maxMemoryBytes()));
        TextUtil.sendRaw(sender, " &fEntities: &e" + ServerUtil.totalEntityCount()
                + "  &fItems: &e" + ServerUtil.totalItemCount()
                + "  &fChunks: &e" + ServerUtil.totalLoadedChunks());

        // Module states
        TextUtil.sendRaw(sender, " &fModules:");
        for (Map.Entry<String, AbstractModule> entry :
                plugin.getModuleManager().getAll().entrySet()) {
            AbstractModule m = entry.getValue();
            String stateColor = m.isLoaded() ? "&a" : m.isEnabled() ? "&e" : "&8";
            String state = m.isLoaded() ? "LOADED" : m.isEnabled() ? "ERROR" : "DISABLED";
            TextUtil.sendRaw(sender, "  " + stateColor + "● &f" + m.getDisplayName()
                    + " &8(" + state + ")");
        }

        // Integrations
        TextUtil.sendRaw(sender, " &fIntegrations:");
        boolean hasPapi = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
        boolean hasSpark = Bukkit.getPluginManager().getPlugin("spark") != null;
        TextUtil.sendRaw(sender, "  " + (hasPapi ? "&a" : "&c") + "● &fPlaceholderAPI: "
                + (hasPapi ? "&aPresent" : "&cNot found"));
        TextUtil.sendRaw(sender, "  " + (hasSpark ? "&a" : "&c") + "● &fspark: "
                + (hasSpark ? "&aPresent" : "&cNot found"));

        TextUtil.sendRaw(sender, "&8&m                                        ");
    }
}
