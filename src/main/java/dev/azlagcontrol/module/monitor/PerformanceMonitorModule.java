package dev.azlagcontrol.module.monitor;

import dev.azlagcontrol.AzLagControl;
import dev.azlagcontrol.module.base.AbstractModule;
import dev.azlagcontrol.scheduler.TaskScheduler;
import dev.azlagcontrol.util.ServerUtil;
import dev.azlagcontrol.util.TpsUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Tracks TPS, MSPT, memory, entity count, chunk count.
 * Provides TPS listener callbacks for other modules (e.g., EmergencyMode).
 * Sends admin alerts when TPS drops below configured thresholds.
 */
public final class PerformanceMonitorModule extends AbstractModule {

    private final TaskScheduler scheduler;
    private final List<Consumer<Double>> tpsListeners = new ArrayList<>();

    // Config
    private int sampleIntervalTicks;
    private boolean alertsEnabled;
    private double tpsWarning;
    private double tpsCritical;
    private String msgWarning;
    private String msgCritical;
    private long alertCooldownMs;

    // State
    private long lastAlertMs = 0L;

    public PerformanceMonitorModule(AzLagControl plugin) {
        super(plugin);
        this.scheduler = new TaskScheduler(plugin);
    }

    @Override
    public String getId() { return "monitor"; }

    @Override
    public String getDisplayName() { return "Performance Monitor"; }

    @Override
    protected String getConfigKey() { return "monitor"; }

    @Override
    public void loadConfig() {
        sampleIntervalTicks = cfg("sample-interval-ticks", 20);
        alertsEnabled       = cfg("alerts.enabled", true);
        tpsWarning          = cfg("alerts.tps-warning", 18.0);
        tpsCritical         = cfg("alerts.tps-critical", 15.0);
        msgWarning          = cfg("alerts.messages.warning",
                "&e[AzLagControl] &fServer TPS is low: &e{tps}");
        msgCritical         = cfg("alerts.messages.critical",
                "&c[AzLagControl] &cCRITICAL TPS: &4{tps}");
        alertCooldownMs     = cfg("alerts.cooldown-seconds", 60) * 1000L;

        TpsUtil.start(plugin);
    }

    @Override
    public void onEnable() {
        scheduler.runTimer(this::sample, sampleIntervalTicks, sampleIntervalTicks);
        logInfo("Monitoring every " + sampleIntervalTicks + " ticks. Alerts: " + alertsEnabled);
    }

    @Override
    public void onDisable() {
        scheduler.cancelAll();
        TpsUtil.stop();
        tpsListeners.clear();
    }

    private void sample() {
        double tps = TpsUtil.getTPS();

        // Notify listeners (e.g. EmergencyMode)
        for (Consumer<Double> listener : tpsListeners) {
            try { listener.accept(tps); } catch (Exception ignored) {}
        }

        // Admin alerts
        if (alertsEnabled) checkAlerts(tps);

        // Guard the debug payload: totalLoadedChunks() allocates a Chunk[] of every
        // loaded chunk, and the string is built eagerly. Skip it all when debug is off.
        if (plugin.getConfigManager().isDebug()) {
            plugin.debug("TPS=" + String.format("%.2f", tps)
                    + " MSPT=" + String.format("%.2f", TpsUtil.getMSPT()) + "ms"
                    + " Entities=" + ServerUtil.totalEntityCount()
                    + " Chunks=" + ServerUtil.totalLoadedChunks());
        }
    }

    private void checkAlerts(double tps) {
        if (tps >= tpsWarning) return; // all fine
        long now = System.currentTimeMillis();
        if (now - lastAlertMs < alertCooldownMs) return;

        String msg;
        if (tps < tpsCritical) {
            msg = msgCritical.replace("{tps}", String.format("%.2f", tps))
                             .replace("{threshold}", String.format("%.1f", tpsCritical));
        } else {
            msg = msgWarning.replace("{tps}", String.format("%.2f", tps))
                            .replace("{threshold}", String.format("%.1f", tpsWarning));
        }

        ServerUtil.broadcastToPermission("azlagcontrol.stats", msg);
        lastAlertMs = now;
    }

    /** Register a listener to be notified of TPS samples. Thread-safe read. */
    public void addTpsListener(Consumer<Double> listener) {
        tpsListeners.add(listener);
    }

    // ── Getters for commands / analytics ──────────────────────────────────

    public double getCurrentTPS() { return TpsUtil.getTPS(); }
    public double[] getTPSHistory() { return TpsUtil.getTPSHistory(); }
    public double getCurrentMSPT() { return TpsUtil.getMSPT(); }
    public long getUsedMemory() { return ServerUtil.usedMemoryBytes(); }
    public long getMaxMemory() { return ServerUtil.maxMemoryBytes(); }
    public double getMemoryRatio() { return ServerUtil.memoryRatio(); }
    public int getEntityCount() { return ServerUtil.totalEntityCount(); }
    public int getItemCount() { return ServerUtil.totalItemCount(); }
    public int getChunkCount() { return ServerUtil.totalLoadedChunks(); }
    public int getPlayerCount() { return ServerUtil.playerCount(); }
}
