package dev.azlagcontrol.command.sub;

import dev.azlagcontrol.AzLagControl;
import dev.azlagcontrol.module.chunk.ChunkControlModule;
import dev.azlagcontrol.util.TextUtil;
import org.bukkit.command.CommandSender;

import java.util.List;

public final class ChunksSubCommand implements SubCommand {

    private final AzLagControl plugin;

    public ChunksSubCommand(AzLagControl plugin) { this.plugin = plugin; }

    @Override public String getName() { return "chunks"; }
    @Override public String getPermission() { return "azlagcontrol.stats"; }
    @Override public String getUsage() { return "/azlag chunks [analyze] — show chunk stats"; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        ChunkControlModule module = plugin.getModuleManager().get(ChunkControlModule.class);
        String prefix = plugin.getConfigManager().getPrefix();

        if (module == null || !module.isLoaded()) {
            TextUtil.sendRaw(sender, "&cChunk Control module is not loaded."); return;
        }

        boolean forceAnalyze = args.length > 1 && args[1].equalsIgnoreCase("analyze");
        if (forceAnalyze) {
            TextUtil.sendRaw(sender, prefix + " &fRunning chunk analysis...");
            module.analyzeChunks();
        }

        List<ChunkControlModule.ChunkSnapshot> worst = module.getWorstChunks();
        if (worst.isEmpty()) {
            TextUtil.sendRaw(sender, prefix + " &fNo chunk data yet. Try &e/azlag chunks analyze");
            return;
        }

        TextUtil.sendRaw(sender, prefix + " &fTop heavy chunks:");
        int rank = 1;
        for (ChunkControlModule.ChunkSnapshot snap : worst) {
            TextUtil.sendRaw(sender, "  &8#" + rank++ + " &7" + snap.world()
                    + " &8[" + snap.chunkX() + "," + snap.chunkZ() + "]"
                    + " &ftotal=&c" + snap.counts().total()
                    + " &fmobs=&e" + (snap.counts().monsters() + snap.counts().animals())
                    + " &fitems=&d" + snap.counts().items());
        }

        if (module.getLastAnalysisTime() > 0) {
            long agoSec = (System.currentTimeMillis() - module.getLastAnalysisTime()) / 1000;
            TextUtil.sendRaw(sender, "  &8Last analysis: &7" + agoSec + "s ago");
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) return List.of("analyze");
        return List.of();
    }
}
