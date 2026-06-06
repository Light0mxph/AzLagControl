package dev.azlagcontrol.scheduler;

import dev.azlagcontrol.AzLagControl;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Centralized task scheduler wrapper.
 * Tracks all scheduled tasks so they can be cancelled cleanly on reload/shutdown.
 * Each module should use its own TaskScheduler instance.
 */
public final class TaskScheduler {

    private final AzLagControl plugin;
    private final List<BukkitTask> tasks = new CopyOnWriteArrayList<>();

    public TaskScheduler(AzLagControl plugin) {
        this.plugin = plugin;
    }

    /** Schedule a sync repeating task. */
    public BukkitTask runTimer(Runnable runnable, long delayTicks, long periodTicks) {
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                runnable.run();
            }
        }.runTaskTimer(plugin, delayTicks, periodTicks);
        tasks.add(task);
        return task;
    }

    /** Schedule a sync delayed task. */
    public BukkitTask runLater(Runnable runnable, long delayTicks) {
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                runnable.run();
            }
        }.runTaskLater(plugin, delayTicks);
        tasks.add(task);
        return task;
    }

    /** Schedule a sync task to run next tick. */
    public BukkitTask runSync(Runnable runnable) {
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                runnable.run();
            }
        }.runTask(plugin);
        tasks.add(task);
        return task;
    }

    /** Cancel a single task and remove from tracking. */
    public void cancel(BukkitTask task) {
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }
        tasks.remove(task);
    }

    /** Cancel all tracked tasks. Call on module disable. */
    public void cancelAll() {
        List<BukkitTask> copy = new ArrayList<>(tasks);
        for (BukkitTask task : copy) {
            if (task != null && !task.isCancelled()) {
                task.cancel();
            }
        }
        tasks.clear();
    }

    public int getActiveTaskCount() {
        return (int) tasks.stream().filter(t -> !t.isCancelled()).count();
    }
}
