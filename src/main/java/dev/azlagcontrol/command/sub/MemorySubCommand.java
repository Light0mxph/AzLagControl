package dev.azlagcontrol.command.sub;

import dev.azlagcontrol.AzLagControl;
import dev.azlagcontrol.module.monitor.PerformanceMonitorModule;
import dev.azlagcontrol.util.TextUtil;
import org.bukkit.command.CommandSender;

public final class MemorySubCommand implements SubCommand {

    private final AzLagControl plugin;

    public MemorySubCommand(AzLagControl plugin) { this.plugin = plugin; }

    @Override public String getName() { return "memory"; }
    @Override public String getPermission() { return "azlagcontrol.stats"; }
    @Override public String getUsage() { return "/azlag memory — show JVM memory usage"; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        PerformanceMonitorModule monitor = plugin.getModuleManager().getPerformanceMonitor();
        if (monitor == null || !monitor.isLoaded()) {
            TextUtil.sendRaw(sender, "&cPerformance Monitor not loaded."); return;
        }

        long used = monitor.getUsedMemory();
        long max  = monitor.getMaxMemory();
        double ratio = monitor.getMemoryRatio();
        String prefix = plugin.getConfigManager().getPrefix();

        String bar = TextUtil.progressBar(used, max, 20);
        String color = ratio < 0.5 ? "&a" : ratio < 0.8 ? "&e" : "&c";

        TextUtil.sendRaw(sender, prefix + " &fMemory: " + color
                + TextUtil.formatMemory(used) + " &8/ &f" + TextUtil.formatMemory(max));
        TextUtil.sendRaw(sender, "  " + TextUtil.colorize(bar)
                + " " + color + String.format("%.1f%%", ratio * 100));
    }
}
