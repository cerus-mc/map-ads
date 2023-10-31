package dev.cerus.mapads.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Dependency;
import co.aikar.commands.annotation.Subcommand;
import dev.cerus.mapads.MapAdsPlugin;
import dev.cerus.mapads.discordbot.diagnostics.Diagnosis;
import dev.cerus.mapads.lang.L10n;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import org.bukkit.command.CommandSender;

@CommandAlias("mapads")
@Subcommand("diagnose")
@CommandPermission("mapads.command.diagnose")
public class DiagnoseCommand extends BaseCommand {

    @Dependency private MapAdsPlugin plugin;

    // TODO: Implement proper dumps
    /*@Subcommand("dump")
    @CommandPermission("mapads.command.diagnose.dump")
    public void handleDump(final CommandSender sender) {
        DumpBuilder.create(new File(this.plugin.getDataFolder(), "dump.txt"))
                .outlinedText(
                        "Map-Ads Debug Dump"
                )
                .blank()
                .line("Version: %s", this.plugin.getDescription().getVersion())
                .blank()
                .table(List.of(
                        new String[] {"abc def 123456", "Hey", "aaaaaaaa"},
                        new String[] {"This is some random column", "works", "magnesiumbus"},
                        new String[] {"ztuizuikzug", "zjsdfg", "fgfddfgfg"}
                ))
                .write();
    }*/

    @Subcommand("discordbot")
    @CommandPermission("mapads.command.diagnose.discordbot")
    public void handleDiscordBot(final CommandSender sender) {
        final CompletableFuture<Collection<Diagnosis>> future = this.plugin.runBotDiagnostics();
        if (future == null) {
            sender.sendMessage(L10n.getPrefixed("misc.diagnostics.no_bot"));
            return;
        }

        sender.sendMessage(L10n.getPrefixed("misc.please_wait"));
        future.whenComplete((diagnoses, throwable) -> {
            sender.sendMessage(L10n.getPrefixed("misc.diagnostics.info", diagnoses.size()));
            for (final Diagnosis diagnosis : diagnoses) {
                sender.sendMessage(L10n.getPrefixed("misc.diagnostics.diagnose."
                                                    + (diagnosis.success() ? "success" : "error"), diagnosis.message()));
                if (diagnosis.error() != null) {
                    this.plugin.getLogger().log(Level.SEVERE, "Discord bot returned erroneous diagnosis", diagnosis.error());
                }
            }
        });
    }

}
