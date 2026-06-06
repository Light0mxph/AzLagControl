package dev.azlagcontrol.module.analytics;

import dev.azlagcontrol.AzLagControl;
import dev.azlagcontrol.module.base.AbstractModule;
import dev.azlagcontrol.module.monitor.PerformanceMonitorModule;
import dev.azlagcontrol.scheduler.TaskScheduler;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.DoubleSummaryStatistics;
import java.util.stream.Collectors;

/**
 * Rolling performance analytics.
 * Subscribes to TPS samples and maintains a configurable history window.
 * Provides min/max/avg TPS and MSPT for admin diagnostics.
 */
public final class AnalyticsModule extends AbstractModule {

    private final TaskScheduler scheduler;

    // Config
    private int historyMinutes;

    // Sampling cadence: one sample every 20 seconds (400 ticks).
    private static final long SAMPLE_PERIOD_TICKS = 400L;
    private static final int SAMPLES_PER_MINUTE = (int) (60 * 20 / SAMPLE_PERIOD_TICKS); // = 3

    // State
    private final Deque<PerformanceSample> samples = new ArrayDeque<>();
    private int maxSamples;

    public AnalyticsModule(AzLagControl plugin) {
        super(plugin);
        this.scheduler = new TaskScheduler(plugin);
    }

    @Override
    public String getId() { return "analytics"; }

    @Override
    public String getDisplayName() { return "Analytics"; }

    @Override
    protected String getConfigKey() { return "analytics"; }

    @Override
    public void loadConfig() {
        historyMinutes = cfg("history-minutes", 60);
        // One sample every 20s → SAMPLES_PER_MINUTE per minute.
        maxSamples = Math.max(1, historyMinutes * SAMPLES_PER_MINUTE);
    }

    @Override
    public void onEnable() {
        scheduler.runTimer(this::recordSample, SAMPLE_PERIOD_TICKS, SAMPLE_PERIOD_TICKS);
        logInfo("History window=" + historyMinutes + "min (" + maxSamples + " samples)");
    }

    @Override
    public void onDisable() {
        scheduler.cancelAll();
    }

    private void recordSample() {
        PerformanceMonitorModule monitor =
                plugin.getModuleManager().getPerformanceMonitor();
        if (monitor == null || !monitor.isLoaded()) return;

        PerformanceSample sample = new PerformanceSample(
                Instant.now().getEpochSecond(),
                monitor.getCurrentTPS(),
                monitor.getCurrentMSPT(),
                monitor.getEntityCount(),
                monitor.getItemCount(),
                monitor.getChunkCount(),
                monitor.getUsedMemory()
        );

        synchronized (samples) {
            samples.addLast(sample);
            while (samples.size() > maxSamples) samples.pollFirst();
        }
    }

    /** Returns TPS statistics over the full history window. */
    public TpsStats getTpsStats() {
        synchronized (samples) {
            if (samples.isEmpty()) return TpsStats.EMPTY;
            DoubleSummaryStatistics stats = samples.stream()
                    .collect(Collectors.summarizingDouble(PerformanceSample::tps));
            return new TpsStats(stats.getMin(), stats.getMax(), stats.getAverage(), stats.getCount());
        }
    }

    /** Returns the last N samples. */
    public Deque<PerformanceSample> getRecentSamples(int count) {
        synchronized (samples) {
            Deque<PerformanceSample> result = new ArrayDeque<>();
            PerformanceSample[] arr = samples.toArray(new PerformanceSample[0]);
            for (int i = Math.max(0, arr.length - count); i < arr.length; i++) {
                result.addLast(arr[i]);
            }
            return result;
        }
    }

    public int getSampleCount() {
        synchronized (samples) { return samples.size(); }
    }

    public record PerformanceSample(
            long timestamp,
            double tps,
            double mspt,
            int entityCount,
            int itemCount,
            int chunkCount,
            long usedMemoryBytes
    ) {}

    public record TpsStats(double min, double max, double avg, long count) {
        static final TpsStats EMPTY = new TpsStats(20.0, 20.0, 20.0, 0);
    }
}
