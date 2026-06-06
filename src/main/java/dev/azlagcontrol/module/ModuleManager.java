package dev.azlagcontrol.module;

import dev.azlagcontrol.AzLagControl;
import dev.azlagcontrol.module.analytics.AnalyticsModule;
import dev.azlagcontrol.module.base.AbstractModule;
import dev.azlagcontrol.module.chunk.ChunkControlModule;
import dev.azlagcontrol.module.cleaner.SmartCleanerModule;
import dev.azlagcontrol.module.emergency.EmergencyModeModule;
import dev.azlagcontrol.module.explosion.ExplosionControlModule;
import dev.azlagcontrol.module.hopper.HopperControlModule;
import dev.azlagcontrol.module.item.SmartItemControlModule;
import dev.azlagcontrol.module.mob.SmartMobControlModule;
import dev.azlagcontrol.module.monitor.PerformanceMonitorModule;
import dev.azlagcontrol.module.redstone.RedstoneControlModule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Manages all AzLagControl modules.
 * Load order matters: monitor must start first so emergency mode can read TPS.
 */
public final class ModuleManager {

    private final AzLagControl plugin;
    private final Map<String, AbstractModule> modules = new LinkedHashMap<>();

    private PerformanceMonitorModule performanceMonitor;
    private EmergencyModeModule emergencyMode;
    private SmartCleanerModule cleaner;
    private AnalyticsModule analytics;

    public ModuleManager(AzLagControl plugin) {
        this.plugin = plugin;
    }

    public void loadAll() {
        // Order: monitor first, emergency last (depends on monitor)
        performanceMonitor = register(new PerformanceMonitorModule(plugin));
        analytics          = register(new AnalyticsModule(plugin));
        cleaner            = register(new SmartCleanerModule(plugin));
        register(new SmartMobControlModule(plugin));
        register(new SmartItemControlModule(plugin));
        register(new RedstoneControlModule(plugin));
        register(new HopperControlModule(plugin));
        register(new ExplosionControlModule(plugin));
        register(new ChunkControlModule(plugin));
        emergencyMode      = register(new EmergencyModeModule(plugin));

        // Wire EmergencyMode to PerformanceMonitor for TPS callbacks
        if (emergencyMode.isEnabled()) {
            performanceMonitor.addTpsListener(emergencyMode::onTpsUpdate);
        }
    }

    private <T extends AbstractModule> T register(T module) {
        modules.put(module.getId(), module);
        enableModule(module);
        return module;
    }

    private void enableModule(AbstractModule module) {
        long start = System.currentTimeMillis();
        try {
            module.readEnabledFromConfig();
            if (!module.isEnabled()) {
                plugin.getLogger().info("  [" + module.getId() + "] disabled in config, skipping.");
                return;
            }
            module.loadConfig();
            module.onEnable();
            module.setLoaded(true);
            long ms = System.currentTimeMillis() - start;
            plugin.getLogger().info("  [+] " + module.getDisplayName() + " loaded in " + ms + "ms");
        } catch (Exception e) {
            module.setLoaded(false);
            plugin.getLogger().log(Level.SEVERE, "  [!] Failed to load module " + module.getId() + ": " + e.getMessage(), e);
        }
    }

    public void disableAll() {
        // Disable in reverse load order
        List<AbstractModule> reversed = new ArrayList<>(modules.values());
        Collections.reverse(reversed);
        for (AbstractModule module : reversed) {
            if (!module.isLoaded()) continue;
            try {
                module.onDisable();
                module.setLoaded(false);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING,
                        "Error disabling module " + module.getId() + ": " + e.getMessage(), e);
            }
        }
    }

    public void reloadAll() {
        disableAll();
        plugin.reloadConfig();
        modules.clear();
        loadAll();
    }

    @SuppressWarnings("unchecked")
    public <T extends AbstractModule> T get(Class<T> clazz) {
        for (AbstractModule m : modules.values()) {
            if (clazz.isInstance(m)) return (T) m;
        }
        return null;
    }

    public AbstractModule get(String id) {
        return modules.get(id);
    }

    public Map<String, AbstractModule> getAll() {
        return Collections.unmodifiableMap(modules);
    }

    public PerformanceMonitorModule getPerformanceMonitor() { return performanceMonitor; }
    public EmergencyModeModule getEmergencyMode() { return emergencyMode; }
    public SmartCleanerModule getCleaner() { return cleaner; }
    public AnalyticsModule getAnalytics() { return analytics; }
}
