package dev.azlagcontrol.event;

import org.bukkit.World;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.Collections;
import java.util.List;

/**
 * Fired after AzLagControl performs a cleanup sweep.
 * Plugins can listen to this to react to cleanup results.
 */
public class AzCleanupEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final List<World> affectedWorlds;
    private final int entitiesRemoved;
    private final boolean manual;

    public AzCleanupEvent(List<World> affectedWorlds, int entitiesRemoved, boolean manual) {
        this.affectedWorlds = Collections.unmodifiableList(affectedWorlds);
        this.entitiesRemoved = entitiesRemoved;
        this.manual = manual;
    }

    public List<World> getAffectedWorlds() { return affectedWorlds; }
    public int getEntitiesRemoved() { return entitiesRemoved; }
    public boolean isManual() { return manual; }

    @Override
    public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
