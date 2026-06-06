package dev.azlagcontrol.command.sub;

import org.bukkit.command.CommandSender;

import java.util.List;

/** Contract for all /azlag subcommands. */
public interface SubCommand {

    /** The subcommand name (e.g. "stats", "tps"). */
    String getName();

    /** Permission node required to run this command. */
    String getPermission();

    /** Short usage string shown in help. */
    String getUsage();

    /** Execute this subcommand. args[0] is always the subcommand name. */
    void execute(CommandSender sender, String[] args);

    /** Tab completions for this subcommand. */
    default List<String> tabComplete(CommandSender sender, String[] args) {
        return List.of();
    }
}
