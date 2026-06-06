package dev.azlagcontrol.module.cleaner;

import dev.azlagcontrol.AzLagControl;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;

import java.util.Map;
import java.util.UUID;

/** Records when players drop items so the cleaner can respect the grace period. */
final class PlayerDropListener implements Listener {

    private final Map<UUID, Long> dropTimes;

    PlayerDropListener(Map<UUID, Long> dropTimes, AzLagControl plugin) {
        this.dropTimes = dropTimes;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        dropTimes.put(event.getItemDrop().getUniqueId(), System.currentTimeMillis());
    }
}
