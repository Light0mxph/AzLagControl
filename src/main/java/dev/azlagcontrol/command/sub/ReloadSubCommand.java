package dev.azlagcontrol.command.sub;

import dev.azlagcontrol.AzLagControl;
import dev.azlagcontrol.util.TextUtil;
import org.bukkit.command.CommandSender;

public final class ReloadSubCommand implements SubCommand {

    private final AzLagControl plugin;

    public ReloadSubCommand(AzLagControl plugin) { this.plugin = plugin; }

    @Override public String getName() { return "reload"; }
    @Override public String getPermission() { return "azlagcontrol.reload"; }
    @Override public String getUsage() { return "/azlag reload — reload configuration"; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        String prefix = plugin.getConfigManager().getPrefix();
        TextUtil.sendRaw(sender, prefix + " &fReloading AzLagControl...");
        long start = System.currentTimeMillis();
        try {
            plugin.getModuleManager().reloadAll();
            long ms = System.currentTimeMillis() - start;
            TextUtil.sendRaw(sender, prefix + " &aReloaded successfully in &f" + ms + "ms&a.");
        } catch (Exception e) {
            TextUtil.sendRaw(sender, prefix + " &cReload failed: " + e.getMessage());
            plugin.getLogger().severe("Reload error: " + e.getMessage());
        }
    }
}
