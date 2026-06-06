package dev.azlagcontrol.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired when emergency mode is activated or deactivated.
 */
public class AzEmergencyModeChangeEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final boolean activated;
    private final double tps;
    private final Player triggeredBy; // null if auto-triggered

    public AzEmergencyModeChangeEvent(boolean activated, double tps, Player triggeredBy) {
        this.activated = activated;
        this.tps = tps;
        this.triggeredBy = triggeredBy;
    }

    public boolean isActivated() { return activated; }
    public double getTps() { return tps; }
    public Player getTriggeredBy() { return triggeredBy; }
    public boolean isAutoTriggered() { return triggeredBy == null; }

    @Override
    public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
