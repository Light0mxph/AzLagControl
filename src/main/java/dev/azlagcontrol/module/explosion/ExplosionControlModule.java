package dev.azlagcontrol.module.explosion;

import dev.azlagcontrol.AzLagControl;
import dev.azlagcontrol.module.base.AbstractModule;
import dev.azlagcontrol.scheduler.TaskScheduler;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Explosion rate limiter.
 * Prevents TNT chain reactions and creeper swarms from killing server TPS.
 *
 * Two-layer approach:
 *   1. Per-tick explosion counter: cancel if over limit.
 *   2. Per-chunk TNT/creeper entity limits: cancel spawn if over limit.
 */
public final class ExplosionControlModule extends AbstractModule implements Listener {

    private final TaskScheduler scheduler;

    // Config
    private int maxPerTick;
    private int maxTntPerChunk;
    private int maxCreeperPerChunk;
    private boolean reduceBlockDamage;

    // State: reset every tick
    private final AtomicInteger explosionsThisTick = new AtomicInteger(0);

    public ExplosionControlModule(AzLagControl plugin) {
        super(plugin);
        this.scheduler = new TaskScheduler(plugin);
    }

    @Override
    public String getId() { return "explosion"; }

    @Override
    public String getDisplayName() { return "Explosion Control"; }

    @Override
    protected String getConfigKey() { return "explosion"; }

    @Override
    public void loadConfig() {
        maxPerTick          = cfg("max-per-tick", 8);
        maxTntPerChunk      = cfg("max-tnt-per-chunk", 25);
        maxCreeperPerChunk  = cfg("creeper.max-per-chunk", 20);
        reduceBlockDamage   = cfg("reduce-block-damage", false);
        loadWorldFilter();
    }

    @Override
    public void onEnable() {
        registerListener(this);
        // Reset per-tick counter every tick
        scheduler.runTimer(() -> explosionsThisTick.set(0), 1L, 1L);
        logInfo("Max explosions/tick=" + maxPerTick
                + " max-tnt/chunk=" + maxTntPerChunk
                + " reduce-block-damage=" + reduceBlockDamage);
    }

    @Override
    public void onDisable() {
        unregisterListener(this);
        scheduler.cancelAll();
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onExplosionPrime(ExplosionPrimeEvent event) {
        if (!isWorldEnabled(event.getEntity().getWorld())) return;

        // Check per-tick limit before explosion actually fires
        if (maxPerTick > 0 && explosionsThisTick.get() >= maxPerTick) {
            event.setCancelled(true);
            debug("Cancelled explosion prime (over per-tick limit): " + event.getEntity().getType());
            return;
        }

        // Check per-chunk TNT limit
        if (event.getEntity() instanceof TNTPrimed) {
            Chunk chunk = event.getEntity().getLocation().getChunk();
            if (maxTntPerChunk > 0 && countInChunk(chunk, TNTPrimed.class) >= maxTntPerChunk) {
                event.setCancelled(true);
                debug("Cancelled TNT explosion (over chunk limit)");
                return;
            }
        }

        // Check per-chunk creeper limit
        if (event.getEntity() instanceof Creeper) {
            Chunk chunk = event.getEntity().getLocation().getChunk();
            if (maxCreeperPerChunk > 0 && countInChunk(chunk, Creeper.class) >= maxCreeperPerChunk) {
                event.setCancelled(true);
                debug("Cancelled creeper explosion (over chunk limit)");
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (!isWorldEnabled(event.getLocation().getWorld())) return;

        int count = explosionsThisTick.incrementAndGet();

        // Cancel if over rate limit
        if (maxPerTick > 0 && count > maxPerTick) {
            event.setCancelled(true);
            return;
        }

        // Reduce block damage if configured
        if (reduceBlockDamage) {
            event.blockList().clear();
        }
    }

    private int countInChunk(Chunk chunk, Class<? extends Entity> type) {
        if (!chunk.isLoaded()) return 0;
        int count = 0;
        for (Entity e : chunk.getEntities()) {
            if (type.isInstance(e)) count++;
        }
        return count;
    }
}
