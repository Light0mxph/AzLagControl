package dev.azlagcontrol.command;

import dev.azlagcontrol.AzLagControl;
import dev.azlagcontrol.command.sub.*;
import dev.azlagcontrol.util.TextUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Root handler for /azlagcontrol, /azlag, /alc.
 * Routes to registered SubCommands.
 */
public final class AzLagCommand implements CommandExecutor, TabCompleter {

    private final AzLagControl plugin;
    private final Map<String, SubCommand> commands = new LinkedHashMap<>();

    public AzLagCommand(AzLagControl plugin) {
        this.plugin = plugin;
        register(new StatsSubCommand(plugin));
        register(new TpsSubCommand(plugin));
        register(new MsptSubCommand(plugin));
        register(new MemorySubCommand(plugin));
        register(new EntitiesSubCommand(plugin));
        register(new ChunksSubCommand(plugin));
        register(new CleanupSubCommand(plugin));
        register(new ReloadSubCommand(plugin));
        register(new EmergencySubCommand(plugin));
        register(new DiagnoseSubCommand(plugin));
        register(new ProfilerSubCommand(plugin));
    }

    private void register(SubCommand sub) {
        commands.put(sub.getName().toLowerCase(), sub);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("azlagcontrol.use")) {
            TextUtil.sendRaw(sender, "&cYou don't have permission to use AzLagControl.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subName = args[0].toLowerCase();

        if (subName.equals("help") || subName.equals("?")) {
            sendHelp(sender);
            return true;
        }

        SubCommand sub = commands.get(subName);
        if (sub == null) {
            TextUtil.sendRaw(sender, "&cUnknown subcommand: &7" + subName
                    + "&c. Use &f/azlag help &cfor help.");
            return true;
        }

        if (!sender.hasPermission(sub.getPermission())) {
            TextUtil.sendRaw(sender, "&cYou don't have permission: &7" + sub.getPermission());
            return true;
        }

        sub.execute(sender, args);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            return commands.entrySet().stream()
                    .filter(e -> e.getKey().startsWith(partial))
                    .filter(e -> sender.hasPermission(e.getValue().getPermission()))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
        }

        if (args.length >= 2) {
            SubCommand sub = commands.get(args[0].toLowerCase());
            if (sub != null && sender.hasPermission(sub.getPermission())) {
                return sub.tabComplete(sender, args);
            }
        }

        return List.of();
    }

    private void sendHelp(CommandSender sender) {
        String prefix = plugin.getConfigManager().getPrefix();
        TextUtil.sendRaw(sender, "&8&m                                        ");
        TextUtil.sendRaw(sender, " &6AzLagControl &fv"
                + plugin.getDescription().getVersion() + " Commands");
        TextUtil.sendRaw(sender, "&8&m                                        ");

        for (SubCommand sub : commands.values()) {
            if (sender.hasPermission(sub.getPermission())) {
                TextUtil.sendRaw(sender, "  &e/" + sub.getUsage());
            }
        }
        TextUtil.sendRaw(sender, "&8&m                                        ");
    }
}
