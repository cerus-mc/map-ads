package dev.cerus.mapads.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Dependency;
import co.aikar.commands.annotation.Subcommand;
import dev.cerus.mapads.MapAdsPlugin;
import dev.cerus.mapads.advert.Advertisement;
import dev.cerus.mapads.advert.storage.AdvertStorage;
import dev.cerus.mapads.discordbot.diagnostics.Diagnosis;
import dev.cerus.mapads.lang.L10n;
import dev.cerus.mapads.premium.Premium;
import dev.cerus.mapads.screen.AdScreen;
import dev.cerus.mapads.screen.ScreenGroup;
import dev.cerus.mapads.screen.storage.AdScreenStorage;
import dev.cerus.mapads.util.DumpBuilder;
import dev.cerus.mapads.util.Either;
import dev.cerus.maps.plugin.map.MapScreenRegistry;
import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

@CommandAlias("mapads")
@Subcommand("diagnose")
@CommandPermission("mapads.command.diagnose")
public class DiagnoseCommand extends BaseCommand {

    @Dependency private MapAdsPlugin plugin;
    @Dependency private AdvertStorage advertStorage;
    @Dependency private AdScreenStorage adScreenStorage;

    // TODO: Implement proper dumps
    @Subcommand("dump")
    @CommandPermission("mapads.command.diagnose.dump")
    public void handleDump(final CommandSender sender) {
        DumpBuilder.create(new File(this.plugin.getDataFolder(), "dump.txt"))
                .outlinedText(
                        "Map-Ads Debug Dump"
                )
                .blank()
                .line("BASICS")
                .line("Version: %s", this.plugin.getDescription().getVersion())
                .line("Premium: %s".formatted(Premium.dump()))
                .line("MC: %s".formatted(Bukkit.getVersion()))
                .blank()
                .line("MAP SCREENS")
                .line(MapScreenRegistry.getScreenIds().stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(", ")))
                .blank()
                .line("AD SCREENS")
                .table(
                        new String[]{"ID", "Screen ID"},
                        () -> adScreenStorage.getScreens().stream()
                                .map(AdScreen::getId)
                                .toArray(String[]::new),
                        () -> adScreenStorage.getScreens().stream()
                                .map(AdScreen::getScreenId)
                                .map(String::valueOf)
                                .toArray(String[]::new)
                )
                .blank()
                .line("SCREEN GROUPS")
                .table(
                        new String[]{"ID", "Group Name", "Screen IDs"},
                        () -> adScreenStorage.getScreenGroups().stream()
                                .map(ScreenGroup::id)
                                .toArray(String[]::new),
                        () -> adScreenStorage.getScreenGroups().stream()
                                .map(ScreenGroup::groupName)
                                .toArray(String[]::new),
                        () -> adScreenStorage.getScreenGroups().stream()
                                .map(ScreenGroup::screenIds)
                                .map(o -> String.join("; ", o))
                                .toArray(String[]::new)
                )
                .blank()
                .line("ADVERTS")
                .table(
                        new String[]{"ID", "Player ID", "Image ID", "Screen / Group", "Mins Left"},
                        () -> advertStorage.getAllAdvertisements().stream()
                                .map(Advertisement::getAdvertId)
                                .map(UUID::toString)
                                .toArray(String[]::new),
                        () -> advertStorage.getAllAdvertisements().stream()
                                .map(Advertisement::getPlayerUuid)
                                .map(UUID::toString)
                                .toArray(String[]::new),
                        () -> advertStorage.getAllAdvertisements().stream()
                                .map(Advertisement::getImageId)
                                .map(UUID::toString)
                                .toArray(String[]::new),
                        () -> advertStorage.getAllAdvertisements().stream()
                                .map(Advertisement::getScreenOrGroupId)
                                .map(Either::toString)
                                .toArray(String[]::new),
                        () -> advertStorage.getAllAdvertisements().stream()
                                .map(Advertisement::getRemainingMinutes)
                                .map(String::valueOf)
                                .toArray(String[]::new)
                )
                .write();

        sender.sendMessage(ChatColor.GREEN + "Done");
    }

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
