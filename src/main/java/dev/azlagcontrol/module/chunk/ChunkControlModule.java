package dev.azlagcontrol.module.chunk;

import dev.azlagcontrol.AzLagControl;
import dev.azlagcontrol.module.base.AbstractModule;
import dev.azlagcontrol.scheduler.TaskScheduler;
import dev.azlagcontrol.util.EntityUtil;
import dev.azlagcontrol.util.ServerUtil;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Entity;

import java.util.*;

/**
 * Chunk health monitor.
 * Periodically analyzes loaded chunks to find the most entity-heavy ones.
 * Alerts admins when a chunk exceeds the configured entity threshold.
 * Does NOT perform aggressive actions — monitoring and alerting only.
 *
 * Use /azlag chunks to view the worst chunks on demand.
 */
public final class ChunkControlModule extends AbstractModule {

    private final TaskScheduler scheduler;

    // Config
    private int analysisIntervalMinutes;
    private int trackTopN;
    private int alertEntityThreshold;

    // State: snapshot of worst chunks
    private final List<ChunkSnapshot> worstChunks = new ArrayList<>();
    private long lastAnalysisTime = 0L;

    public ChunkControlModule(AzLagControl plugin) {
        super(plugin);
        this.scheduler = new TaskScheduler(plugin);
    }

    @Override
    public String getId() { return "chunk-control"; }

    @Override
    public String getDisplayName() { return "Chunk Control"; }

    @Override
    protected String getConfigKey() { return "chunk-control"; }

    @Override
    public void loadConfig() {
        analysisIntervalMinutes = cfg("analysis-interval-minutes", 5);
        trackTopN               = cfg("track-top-n", 10);
        alertEntityThreshold    = cfg("alert-entity-threshold", 400);
        loadWorldFilter();
    }

    @Override
    public void onEnable() {
        long periodTicks = analysisIntervalMinutes * 60L * 20L;
        scheduler.runTimer(this::analyzeChunks, periodTicks, periodTicks);
        logInfo("Analyzing every " + analysisIntervalMinutes + "min, top " + trackTopN
                + " chunks tracked, alert-threshold=" + alertEntityThreshold);
    }

    @Override
    public void onDisable() {
        scheduler.cancelAll();
        worstChunks.clear();
    }

    /** Run analysis and return snapshot of worst chunks. */
    public List<ChunkSnapshot> analyzeChunks() {
        List<ChunkSnapshot> snapshots = new ArrayList<>();

        for (World world : getEnabledWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                Entity[] entities = chunk.getEntities();
                if (entities.length == 0) continue;

                EntityUtil.EntityCounts counts = EntityUtil.countChunkEntities(entities);
                snapshots.add(new ChunkSnapshot(
                        world.getName(),
                        chunk.getX(), chunk.getZ(),
                        counts
                ));

                if (alertEntityThreshold > 0 && counts.total() >= alertEntityThreshold) {
                    String msg = "&e[AzLagControl] &fHeavy chunk: &e" + world.getName()
                            + " &f[" + chunk.getX() + "," + chunk.getZ() + "]"
                            + " entities=&c" + counts.total();
                    ServerUtil.broadcastToPermission("azlagcontrol.stats", msg);
                }
            }
        }

        // Sort by total entity count descending, keep top N
        snapshots.sort((a, b) -> Integer.compare(b.counts().total(), a.counts().total()));
        List<ChunkSnapshot> top = snapshots.subList(0, Math.min(trackTopN, snapshots.size()));

        synchronized (worstChunks) {
            worstChunks.clear();
            worstChunks.addAll(top);
        }

        lastAnalysisTime = System.currentTimeMillis();
        logInfo("Analysis complete. Top chunk: "
                + (top.isEmpty() ? "none" : top.get(0).toString()));

        return top;
    }

    public List<ChunkSnapshot> getWorstChunks() {
        synchronized (worstChunks) {
            return new ArrayList<>(worstChunks);
        }
    }

    public long getLastAnalysisTime() { return lastAnalysisTime; }

    public record ChunkSnapshot(
            String world,
            int chunkX, int chunkZ,
            EntityUtil.EntityCounts counts
    ) {
        @Override
        public String toString() {
            return world + " [" + chunkX + "," + chunkZ + "] total=" + counts.total();
        }
    }
}
