package dev.azlagcontrol.integration;

import dev.azlagcontrol.AzLagControl;
import dev.azlagcontrol.module.emergency.EmergencyModeModule;
import dev.azlagcontrol.module.monitor.PerformanceMonitorModule;
import dev.azlagcontrol.util.TextUtil;
import dev.azlagcontrol.util.TpsUtil;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

/**
 * PlaceholderAPI expansion for AzLagControl.
 *
 * Available placeholders:
 *   %azlagcontrol_tps%         — Current TPS (formatted)
 *   %azlagcontrol_tps_1m%      — 1-minute TPS
 *   %azlagcontrol_tps_5m%      — 5-minute TPS
 *   %azlagcontrol_tps_15m%     — 15-minute TPS
 *   %azlagcontrol_mspt%        — MSPT (formatted)
 *   %azlagcontrol_memory_used% — Used memory (MB)
 *   %azlagcontrol_memory_max%  — Max memory (MB)
 *   %azlagcontrol_memory_pct%  — Memory percent (0-100)
 *   %azlagcontrol_entities%    — Total entity count
 *   %azlagcontrol_items%       — Total item count
 *   %azlagcontrol_chunks%      — Total loaded chunks
 *   %azlagcontrol_emergency%   — "true"/"false"
 */
public final class PlaceholderIntegration extends PlaceholderExpansion {

    private final AzLagControl plugin;

    public PlaceholderIntegration(AzLagControl plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() { return "azlagcontrol"; }

    @Override
    public String getAuthor() { return "AztrixDigitalStudio"; }

    @Override
    public String getVersion() { return plugin.getDescription().getVersion(); }

    @Override
    public boolean persist() { return true; } // Don't unregister on PAPI reload

    @Override
    public String onPlaceholderRequest(Player player, String params) {
        PerformanceMonitorModule monitor = plugin.getModuleManager().getPerformanceMonitor();
        EmergencyModeModule emergency = plugin.getModuleManager().getEmergencyMode();

        return switch (params.toLowerCase()) {
            case "tps" -> {
                if (monitor == null || !monitor.isLoaded()) yield "N/A";
                yield String.format("%.2f", monitor.getCurrentTPS());
            }
            case "tps_1m" -> {
                if (monitor == null || !monitor.isLoaded()) yield "N/A";
                double[] tps = monitor.getTPSHistory();
                yield String.format("%.2f", tps.length > 0 ? tps[0] : 20.0);
            }
            case "tps_5m" -> {
                if (monitor == null || !monitor.isLoaded()) yield "N/A";
                double[] tps = monitor.getTPSHistory();
                yield String.format("%.2f", tps.length > 1 ? tps[1] : 20.0);
            }
            case "tps_15m" -> {
                if (monitor == null || !monitor.isLoaded()) yield "N/A";
                double[] tps = monitor.getTPSHistory();
                yield String.format("%.2f", tps.length > 2 ? tps[2] : 20.0);
            }
            case "tps_colored" -> {
                if (monitor == null || !monitor.isLoaded()) yield "N/A";
                yield TextUtil.colorize(TpsUtil.formatTPS(monitor.getCurrentTPS()));
            }
            case "mspt" -> {
                if (monitor == null || !monitor.isLoaded()) yield "N/A";
                yield String.format("%.2f", monitor.getCurrentMSPT());
            }
            case "memory_used" -> {
                if (monitor == null || !monitor.isLoaded()) yield "N/A";
                yield TextUtil.formatMemory(monitor.getUsedMemory());
            }
            case "memory_max" -> {
                if (monitor == null || !monitor.isLoaded()) yield "N/A";
                yield TextUtil.formatMemory(monitor.getMaxMemory());
            }
            case "memory_pct" -> {
                if (monitor == null || !monitor.isLoaded()) yield "N/A";
                yield String.format("%.1f", monitor.getMemoryRatio() * 100);
            }
            case "entities" -> {
                if (monitor == null || !monitor.isLoaded()) yield "N/A";
                yield String.valueOf(monitor.getEntityCount());
            }
            case "items" -> {
                if (monitor == null || !monitor.isLoaded()) yield "N/A";
                yield String.valueOf(monitor.getItemCount());
            }
            case "chunks" -> {
                if (monitor == null || !monitor.isLoaded()) yield "N/A";
                yield String.valueOf(monitor.getChunkCount());
            }
            case "emergency" -> {
                if (emergency == null || !emergency.isLoaded()) yield "false";
                yield String.valueOf(emergency.isEmergencyActive());
            }
            default -> null;
        };
    }
}
