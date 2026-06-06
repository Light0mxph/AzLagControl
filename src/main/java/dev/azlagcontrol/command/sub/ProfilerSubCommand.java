package dev.azlagcontrol.command.sub;

import dev.azlagcontrol.AzLagControl;
import dev.azlagcontrol.module.analytics.AnalyticsModule;
import dev.azlagcontrol.util.TextUtil;
import dev.azlagcontrol.util.TpsUtil;
import org.bukkit.command.CommandSender;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Deque;

public final class ProfilerSubCommand implements SubCommand {

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());

    private final AzLagControl plugin;

    public ProfilerSubCommand(AzLagControl plugin) { this.plugin = plugin; }

    @Override public String getName() { return "profiler"; }
    @Override public String getPermission() { return "azlagcontrol.profiler"; }
    @Override public String getUsage() { return "/azlag profiler [last N] — show performance history"; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        AnalyticsModule analytics = plugin.getModuleManager().getAnalytics();
        String prefix = plugin.getConfigManager().getPrefix();

        if (analytics == null || !analytics.isLoaded()) {
            TextUtil.sendRaw(sender, "&cAnalytics module not loaded."); return;
        }

        int count = 10;
        if (args.length >= 3) {
            try { count = Math.min(50, Integer.parseInt(args[2])); }
            catch (NumberFormatException ignored) {}
        }

        Deque<AnalyticsModule.PerformanceSample> samples = analytics.getRecentSamples(count);
        AnalyticsModule.TpsStats stats = analytics.getTpsStats();

        TextUtil.sendRaw(sender, prefix + " &fLast " + samples.size() + " samples:");
        TextUtil.sendRaw(sender, " &8Time     TPS    MSPT   Entities  Memory");

        for (AnalyticsModule.PerformanceSample s : samples) {
            String time = TIME_FMT.format(Instant.ofEpochSecond(s.timestamp()));
            TextUtil.sendRaw(sender, " &7" + time
                    + "  " + TpsUtil.formatTPS(s.tps())
                    + "  " + TpsUtil.formatMSPT(s.mspt())
                    + "  &f" + s.entityCount()
                    + "  &f" + TextUtil.formatMemory(s.usedMemoryBytes()));
        }

        if (stats.count() > 0) {
            TextUtil.sendRaw(sender, " &fHistory: min=" + TpsUtil.formatTPS(stats.min())
                    + " avg=" + TpsUtil.formatTPS(stats.avg())
                    + " max=" + TpsUtil.formatTPS(stats.max())
                    + " &8(n=" + stats.count() + ")");
        }
    }
}
