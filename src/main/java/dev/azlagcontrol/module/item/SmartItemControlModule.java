package dev.azlagcontrol.module.item;

import dev.azlagcontrol.AzLagControl;
import dev.azlagcontrol.module.base.AbstractModule;
import dev.azlagcontrol.scheduler.TaskScheduler;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Ground item management.
 * Prevents item spam per chunk and merges nearby identical items.
 */
public final class SmartItemControlModule extends AbstractModule implements Listener {

    private final TaskScheduler scheduler;

    // Config
    private int perChunkLimit;
    private double mergeRadius;
    private int lifetimeTicks;
    private int cleanupIntervalSeconds;
    private boolean protectNamed;

    public SmartItemControlModule(AzLagControl plugin) {
        super(plugin);
        this.scheduler = new TaskScheduler(plugin);
    }

    @Override
    public String getId() { return "item-control"; }

    @Override
    public String getDisplayName() { return "Smart Item Control"; }

    @Override
    protected String getConfigKey() { return "item-control"; }

    @Override
    public void loadConfig() {
        perChunkLimit          = cfg("per-chunk-limit", 200);
        mergeRadius            = cfg("merge-radius", 2.5);
        lifetimeTicks          = cfg("lifetime-ticks", -1);
        cleanupIntervalSeconds = cfg("cleanup-interval", 120);
        protectNamed           = cfg("protect-named", true);
        loadWorldFilter();
    }

    @Override
    public void onEnable() {
        registerListener(this);

        if (cleanupIntervalSeconds > 0) {
            long periodTicks = cleanupIntervalSeconds * 20L;
            scheduler.runTimer(this::runPeriodicCleanup, periodTicks, periodTicks);
        }

        logInfo("Per-chunk limit=" + perChunkLimit
                + " merge-radius=" + mergeRadius
                + " cleanup-interval=" + cleanupIntervalSeconds + "s");
    }

    @Override
    public void onDisable() {
        unregisterListener(this);
        scheduler.cancelAll();
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent event) {
        Item item = event.getEntity();
        if (!isWorldEnabled(item.getWorld())) return;

        Chunk chunk = item.getLocation().getChunk();
        if (!chunk.isLoaded()) return;

        // Apply custom lifetime. Vanilla items despawn at 6000 ticks lived.
        // Pre-aging the item makes it despawn sooner. setTicksLived requires >= 1.
        if (lifetimeTicks > 0 && lifetimeTicks < 6000) {
            int preAge = Math.max(1, 6000 - lifetimeTicks);
            item.setTicksLived(preAge);
        }

        // Count items in chunk and cancel if over limit
        if (perChunkLimit > 0) {
            int itemCount = 0;
            for (Entity e : chunk.getEntities()) {
                if (e instanceof Item) itemCount++;
            }
            if (itemCount >= perChunkLimit) {
                event.setCancelled(true);
                debug("Cancelled item spawn in " + chunk.getWorld().getName()
                        + " [" + chunk.getX() + "," + chunk.getZ() + "]"
                        + " items=" + itemCount);
                return;
            }
        }

        // Merge with nearby identical items
        if (mergeRadius > 0) {
            tryMerge(item);
        }
    }

    private void tryMerge(Item newItem) {
        ItemStack stack = newItem.getItemStack();
        if (stack.getMaxStackSize() <= 1) return;

        Location loc = newItem.getLocation();
        for (Entity nearby : newItem.getNearbyEntities(mergeRadius, mergeRadius, mergeRadius)) {
            if (!(nearby instanceof Item existingItem)) continue;
            if (!existingItem.isValid() || existingItem.equals(newItem)) continue;
            if (protectNamed && existingItem.getCustomName() != null) continue;

            ItemStack existing = existingItem.getItemStack();
            if (!existing.isSimilar(stack)) continue;

            int total = existing.getAmount() + stack.getAmount();
            int maxStack = stack.getMaxStackSize();

            if (total <= maxStack) {
                existing.setAmount(total);
                existingItem.setItemStack(existing);
                newItem.remove();
                return;
            } else {
                // Fill existing stack, leave remainder on new item
                existing.setAmount(maxStack);
                existingItem.setItemStack(existing);
                stack.setAmount(total - maxStack);
                newItem.setItemStack(stack);
            }
        }
    }

    private void runPeriodicCleanup() {
        int removed = 0;
        for (World world : getEnabledWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                if (perChunkLimit <= 0) continue;
                List<Item> items = new ArrayList<>();
                for (Entity e : chunk.getEntities()) {
                    if (e instanceof Item item) {
                        if (protectNamed && item.getCustomName() != null) continue;
                        items.add(item);
                    }
                }
                if (items.size() > perChunkLimit) {
                    // Remove oldest items first (they've been ticking longest)
                    items.sort((a, b) -> Integer.compare(b.getTicksLived(), a.getTicksLived()));
                    int excess = items.size() - perChunkLimit;
                    for (int i = 0; i < excess; i++) {
                        items.get(i).remove();
                        removed++;
                    }
                }
            }
        }
        if (removed > 0) {
            logInfo("Periodic cleanup removed " + removed + " items.");
        }
    }
}
