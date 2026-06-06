package dev.azlagcontrol.command.sub;

import dev.azlagcontrol.AzLagControl;
import dev.azlagcontrol.util.EntityUtil;
import dev.azlagcontrol.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;

public final class EntitiesSubCommand implements SubCommand {

    private final AzLagControl plugin;

    public EntitiesSubCommand(AzLagControl plugin) { this.plugin = plugin; }

    @Override public String getName() { return "entities"; }
    @Override public String getPermission() { return "azlagcontrol.stats"; }
    @Override public String getUsage() { return "/azlag entities — entity counts per world"; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        String prefix = plugin.getConfigManager().getPrefix();
        TextUtil.sendRaw(sender, prefix + " &fEntity breakdown:");

        int grandTotal = 0;
        for (World world : Bukkit.getWorlds()) {
            Entity[] entities = world.getEntities().toArray(new Entity[0]);
            EntityUtil.EntityCounts counts = EntityUtil.countChunkEntities(entities);
            grandTotal += counts.total();

            TextUtil.sendRaw(sender, "  &7" + world.getName() + " &8—"
                    + " &ftotal=&e" + counts.total()
                    + " &fmonsters=&c" + counts.monsters()
                    + " &fanimals=&a" + counts.animals()
                    + " &fvillagers=&b" + counts.villagers()
                    + " &fitems=&d" + counts.items());
        }
        TextUtil.sendRaw(sender, "  &fGrand total: &e" + grandTotal + " entities");
    }
}
