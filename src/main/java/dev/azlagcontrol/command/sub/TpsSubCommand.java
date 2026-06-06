package dev.azlagcontrol.command.sub;

import dev.azlagcontrol.AzLagControl;
import dev.azlagcontrol.module.monitor.PerformanceMonitorModule;
import dev.azlagcontrol.util.TextUtil;
import dev.azlagcontrol.util.TpsUtil;
import org.bukkit.command.CommandSender;

public final class TpsSubCommand implements SubCommand {

    private final AzLagControl plugin;

    public TpsSubCommand(AzLagControl plugin) { this.plugin = plugin; }

    @Override public String getName() { return "tps"; }
    @Override public String getPermission() { return "azlagcontrol.stats"; }
    @Override public String getUsage() { return "/azlag tps — show TPS (1m/5m/15m)"; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        PerformanceMonitorModule monitor = plugin.getModuleManager().getPerformanceMonitor();
        if (monitor == null || !monitor.isLoaded()) {
            TextUtil.sendRaw(sender, "&cPerformance Monitor module is not loaded.");
            return;
        }

        double[] tpsArr = monitor.getTPSHistory();
        double t1 = tpsArr.length > 0 ? tpsArr[0] : 20.0;
        double t5 = tpsArr.length > 1 ? tpsArr[1] : 20.0;
        double t15 = tpsArr.length > 2 ? tpsArr[2] : 20.0;

        String prefix = plugin.getConfigManager().getPrefix();
        TextUtil.sendRaw(sender, prefix + " &fTPS &81m&f: " + TpsUtil.formatTPS(t1)
                + "  &85m&f: " + TpsUtil.formatTPS(t5)
                + "  &815m&f: " + TpsUtil.formatTPS(t15));
    }
}
