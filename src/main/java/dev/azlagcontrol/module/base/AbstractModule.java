package dev.azlagcontrol.module.base;

import dev.azlagcontrol.AzLagControl;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

/**
 * Base implementation for all AzLagControl modules.
 *
 * Subclasses must implement:
 *   - getId() + getDisplayName()
 *   - loadConfig()
 *   - onEnable()
 *   - onDisable()
 *
 * World filtering is handled here — call isWorldEnabled(world) before processing.
 */
public abstract class AbstractModule implements AzModule {

    protected final AzLagControl plugin;
    private boolean loaded = false;
    private boolean enabled = false;

    private final Set<String> allowedWorlds = new HashSet<>();
    private boolean allWorldsEnabled = true;

    protected AbstractModule(AzLagControl plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean isLoaded() {
        return loaded;
    }

    public void setLoaded(boolean loaded) {
        this.loaded = loaded;
    }

    /** Returns the top-level config section key for this module. */
    protected abstract String getConfigKey();

    /** Convenience: get config section for this module. May return null if not configured. */
    protected ConfigurationSection getSection() {
        return plugin.getConfig().getConfigurationSection(getConfigKey());
    }

    /**
     * Convenience: get a value from this module's config section.
     * Numeric defaults are coerced safely — YAML may store an int where a
     * double is expected (e.g. "0" instead of "0.0"), which would otherwise
     * throw ClassCastException on unboxing at the call site.
     */
    @SuppressWarnings("unchecked")
    protected <T> T cfg(String path, T def) {
        ConfigurationSection sec = getSection();
        if (sec == null) return def;
        Object val = sec.get(path);
        if (val == null) return def;

        // Numeric coercion to match the default's runtime type
        if (def instanceof Number && val instanceof Number n) {
            if (def instanceof Double)  return (T) Double.valueOf(n.doubleValue());
            if (def instanceof Integer) return (T) Integer.valueOf(n.intValue());
            if (def instanceof Long)    return (T) Long.valueOf(n.longValue());
            if (def instanceof Float)   return (T) Float.valueOf(n.floatValue());
        }

        try {
            return (T) val;
        } catch (ClassCastException e) {
            return def;
        }
    }

    /** Load world filter from config section. */
    protected void loadWorldFilter() {
        ConfigurationSection sec = getSection();
        if (sec == null) {
            allWorldsEnabled = true;
            return;
        }
        List<String> worlds = sec.getStringList("worlds");
        allowedWorlds.clear();
        if (worlds.isEmpty() || worlds.contains("*")) {
            allWorldsEnabled = true;
        } else {
            allWorldsEnabled = false;
            allowedWorlds.addAll(worlds);
        }
    }

    /** Returns true if module should process this world. */
    protected boolean isWorldEnabled(World world) {
        if (world == null) return false;
        return allWorldsEnabled || allowedWorlds.contains(world.getName());
    }

    /** Returns all worlds the module should operate on. */
    protected Set<World> getEnabledWorlds() {
        if (allWorldsEnabled) return new HashSet<>(Bukkit.getWorlds());
        Set<World> result = new HashSet<>();
        for (String name : allowedWorlds) {
            World w = Bukkit.getWorld(name);
            if (w != null) result.add(w);
        }
        return result;
    }

    /** Registers a Bukkit listener for this module. Tracked for clean unregistration. */
    protected void registerListener(Listener listener) {
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);
    }

    /** Unregisters all listeners for a given Listener instance. */
    protected void unregisterListener(Listener listener) {
        HandlerList.unregisterAll(listener);
    }

    protected void log(Level level, String message) {
        plugin.getLogger().log(level, "[" + getDisplayName() + "] " + message);
    }

    protected void logInfo(String message) { log(Level.INFO, message); }
    protected void logWarn(String message) { log(Level.WARNING, message); }
    protected void logError(String message, Throwable t) {
        plugin.getLogger().log(Level.SEVERE, "[" + getDisplayName() + "] " + message, t);
    }

    protected void debug(String message) {
        plugin.debug("[" + getDisplayName() + "] " + message);
    }

    /** Internal: set enabled from config. Called by ModuleManager. */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /** Reads the enabled flag from config. */
    public boolean readEnabledFromConfig() {
        ConfigurationSection sec = getSection();
        if (sec == null) return false;
        this.enabled = sec.getBoolean("enabled", false);
        return this.enabled;
    }
}
