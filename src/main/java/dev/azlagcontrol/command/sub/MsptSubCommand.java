package dev.azlagcontrol.command.sub;

import dev.azlagcontrol.AzLagControl;
import dev.azlagcontrol.module.monitor.PerformanceMonitorModule;
import dev.azlagcontrol.util.TextUtil;
import dev.azlagcontrol.util.TpsUtil;
import org.bukkit.command.CommandSender;

public final class MsptSubCommand implements SubCommand {

    private final AzLagControl plugin;

    public MsptSubCommand(AzLagControl plugin) { this.plugin = plugin; }

    @Override public String getName() { return "mspt"; }
    @Override public String getPermission() { return "azlagcontrol.stats"; }
    @Override public String getUsage() { return "/azlag mspt — show milliseconds per tick"; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        PerformanceMonitorModule monitor = plugin.getModuleManager().getPerformanceMonitor();
        if (monitor == null || !monitor.isLoaded()) {
            TextUtil.sendRaw(sender, "&cPerformance Monitor not loaded.");
            return;
        }

        double mspt = monitor.getCurrentMSPT();
        String prefix = plugin.getConfigManager().getPrefix();
        TextUtil.sendRaw(sender, prefix + " &fMSPT: " + TpsUtil.formatMSPT(mspt)
                + " &8(vanilla target: &f50ms&8)");
    }
}
