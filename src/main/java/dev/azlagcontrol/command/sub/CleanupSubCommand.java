package dev.azlagcontrol.command.sub;

import dev.azlagcontrol.AzLagControl;
import dev.azlagcontrol.module.cleaner.SmartCleanerModule;
import dev.azlagcontrol.util.TextUtil;
import org.bukkit.command.CommandSender;

public final class CleanupSubCommand implements SubCommand {

    private final AzLagControl plugin;

    public CleanupSubCommand(AzLagControl plugin) { this.plugin = plugin; }

    @Override public String getName() { return "cleanup"; }
    @Override public String getPermission() { return "azlagcontrol.cleanup"; }
    @Override public String getUsage() { return "/azlag cleanup — trigger manual cleanup now"; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        SmartCleanerModule cleaner = plugin.getModuleManager().getCleaner();
        String prefix = plugin.getConfigManager().getPrefix();

        if (cleaner == null || !cleaner.isLoaded()) {
            TextUtil.sendRaw(sender, "&cSmart Cleaner module is not loaded."); return;
        }

        TextUtil.sendRaw(sender, prefix + " &fRunning manual cleanup...");
        int removed = cleaner.performCleanup(true);
        // Broadcast is handled inside performCleanup, but also log to sender
        plugin.getLogger().info(sender.getName() + " triggered manual cleanup: " + removed + " removed");
    }
}
