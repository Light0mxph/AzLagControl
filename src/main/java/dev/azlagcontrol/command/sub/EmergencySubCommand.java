package dev.azlagcontrol.command.sub;

import dev.azlagcontrol.AzLagControl;
import dev.azlagcontrol.module.emergency.EmergencyModeModule;
import dev.azlagcontrol.util.TextUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public final class EmergencySubCommand implements SubCommand {

    private final AzLagControl plugin;

    public EmergencySubCommand(AzLagControl plugin) { this.plugin = plugin; }

    @Override public String getName() { return "emergency"; }
    @Override public String getPermission() { return "azlagcontrol.emergency"; }
    @Override public String getUsage() { return "/azlag emergency <on|off|status> — manage emergency mode"; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        EmergencyModeModule em = plugin.getModuleManager().getEmergencyMode();
        String prefix = plugin.getConfigManager().getPrefix();

        if (em == null || !em.isLoaded()) {
            TextUtil.sendRaw(sender, "&cEmergency Mode module is not loaded."); return;
        }

        if (args.length < 2) {
            String status = em.isEmergencyActive() ? "&4ACTIVE" : "&aINACTIVE";
            TextUtil.sendRaw(sender, prefix + " Emergency Mode: " + status);
            TextUtil.sendRaw(sender, "  Usage: /azlag emergency <on|off>");
            return;
        }

        Player player = (sender instanceof Player p) ? p : null;

        switch (args[1].toLowerCase()) {
            case "on", "activate" -> {
                if (em.isEmergencyActive()) {
                    TextUtil.sendRaw(sender, prefix + " &cEmergency mode is already active.");
                } else {
                    em.activate(player);
                    plugin.getLogger().warning(sender.getName() + " manually activated emergency mode.");
                }
            }
            case "off", "deactivate" -> {
                if (!em.isEmergencyActive()) {
                    TextUtil.sendRaw(sender, prefix + " &cEmergency mode is not active.");
                } else {
                    em.deactivate(player);
                }
            }
            case "status" -> {
                String status = em.isEmergencyActive() ? "&4ACTIVE" : "&aINACTIVE";
                TextUtil.sendRaw(sender, prefix + " Emergency Mode: " + status);
            }
            default -> TextUtil.sendRaw(sender, "&cUsage: /azlag emergency <on|off|status>");
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) return List.of("on", "off", "status");
        return List.of();
    }
}
