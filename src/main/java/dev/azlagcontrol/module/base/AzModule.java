package dev.azlagcontrol.module.base;

/**
 * Contract for every AzLagControl module.
 * Lifecycle: loadConfig → onEnable → (running) → onDisable → reload → repeat
 */
public interface AzModule {

    /** Unique identifier used in config and logs. */
    String getId();

    /** Human-readable name for display. */
    String getDisplayName();

    /** Whether this module is enabled per config. */
    boolean isEnabled();

    /** Whether this module has been loaded and is currently active. */
    boolean isLoaded();

    /**
     * Read configuration from plugin config.
     * Called before onEnable and on reload.
     */
    void loadConfig();

    /** Start the module — register listeners, schedule tasks, etc. */
    void onEnable() throws Exception;

    /** Stop the module — unregister listeners, cancel tasks, release resources. */
    void onDisable();

    /** Reload: disable → reloadConfig → enable. */
    default void reload() throws Exception {
        if (isLoaded()) onDisable();
        loadConfig();
        onEnable();
    }
}
