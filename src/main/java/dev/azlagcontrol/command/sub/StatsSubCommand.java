package dev.azlagcontrol.command.sub;

import dev.azlagcontrol.AzLagControl;
import dev.azlagcontrol.module.analytics.AnalyticsModule;
import dev.azlagcontrol.module.emergency.EmergencyModeModule;
import dev.azlagcontrol.module.monitor.PerformanceMonitorModule;
import dev.azlagcontrol.util.TextUtil;
import dev.azlagcontrol.util.TpsUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

public final class StatsSubCommand implements SubCommand {

    private final AzLagControl plugin;

    public StatsSubCommand(AzLagControl plugin) { this.plugin = plugin; }

    @Override public String getName() { return "stats"; }
    @Override public String getPermission() { return "azlagcontrol.stats"; }
    @Override public String getUsage() { return "/azlag stats — full server stats"; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        PerformanceMonitorModule monitor = plugin.getModuleManager().getPerformanceMonitor();
        EmergencyModeModule emergency = plugin.getModuleManager().getEmergencyMode();
        AnalyticsModule analytics = plugin.getModuleManager().getAnalytics();

        String prefix = plugin.getConfigManager().getPrefix();
        String v = plugin.getDescription().getVersion();

        TextUtil.sendRaw(sender, "&8&m                                        ");
        TextUtil.sendRaw(sender, " &6AzLagControl &fv" + v
                + (emergency != null && emergency.isEmergencyActive()
                   ? " &4⚠ EMERGENCY MODE ACTIVE" : ""));
        TextUtil.sendRaw(sender, "&8&m                                        ");

        if (monitor != null && monitor.isLoaded()) {
            double[] tps = monitor.getTPSHistory();
            double t1  = tps.length > 0 ? tps[0] : 20.0;
            double t5  = tps.length > 1 ? tps[1] : 20.0;
            double t15 = tps.length > 2 ? tps[2] : 20.0;

            TextUtil.sendRaw(sender, " &fTPS:    " + TpsUtil.formatTPS(t1)
                    + " &81m&f / " + TpsUtil.formatTPS(t5)
                    + " &85m&f / " + TpsUtil.formatTPS(t15) + " &815m");
            TextUtil.sendRaw(sender, " &fMSPT:   " + TpsUtil.formatMSPT(monitor.getCurrentMSPT()));
            TextUtil.sendRaw(sender, " &fMemory: &e"
                    + TextUtil.formatMemory(monitor.getUsedMemory())
                    + " &8/ &f" + TextUtil.formatMemory(monitor.getMaxMemory())
                    + String.format(" &8(%.1f%%)", monitor.getMemoryRatio() * 100));
            TextUtil.sendRaw(sender, " &fEntities: &e" + monitor.getEntityCount()
                    + "  &fItems: &e" + monitor.getItemCount()
                    + "  &fChunks: &e" + monitor.getChunkCount());
            TextUtil.sendRaw(sender, " &fPlayers: &e" + monitor.getPlayerCount()
                    + " &8/ &f" + Bukkit.getMaxPlayers());
        } else {
            TextUtil.sendRaw(sender, " &cPerformance Monitor not loaded.");
        }

        if (analytics != null && analytics.isLoaded()) {
            AnalyticsModule.TpsStats stats = analytics.getTpsStats();
            if (stats.count() > 0) {
                TextUtil.sendRaw(sender, " &fTPS history (&e" + plugin.getConfig().getInt("analytics.history-minutes", 60) + "min&f):"
                        + " min=" + TpsUtil.formatTPS(stats.min())
                        + " avg=" + TpsUtil.formatTPS(stats.avg())
                        + " max=" + TpsUtil.formatTPS(stats.max()));
            }
        }

        TextUtil.sendRaw(sender, "&8&m                                        ");
    }
}
