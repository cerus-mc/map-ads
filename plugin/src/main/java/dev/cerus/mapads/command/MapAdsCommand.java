package dev.cerus.mapads.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Dependency;
import co.aikar.commands.annotation.Subcommand;
import dev.cerus.mapads.ConfigModel;
import dev.cerus.mapads.MapAdsPlugin;
import dev.cerus.mapads.advert.storage.AdvertStorage;
import dev.cerus.mapads.economy.EconomyWrapper;
import dev.cerus.mapads.gui.CreateAdGui;
import dev.cerus.mapads.image.DefaultImageController;
import dev.cerus.mapads.image.ImageConverter;
import dev.cerus.mapads.image.ImageRetriever;
import dev.cerus.mapads.image.storage.ImageStorage;
import dev.cerus.mapads.lang.L10n;
import dev.cerus.mapads.screen.storage.AdScreenStorage;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

@CommandAlias("mapads")
public class MapAdsCommand extends BaseCommand {

    @Dependency
    private ImageStorage imageStorage;

    @Dependency
    private AdvertStorage advertStorage;

    @Dependency
    private AdScreenStorage adScreenStorage;

    @Dependency
    private ImageRetriever imageRetriever;

    @Dependency
    private ImageConverter imageConverter;

    @Dependency
    private DefaultImageController defaultImageController;

    @Dependency
    private ConfigModel configModel;

    @Dependency
    private EconomyWrapper<?> economy;

    @Default
    public void handle(final Player player) {
        final MapAdsPlugin mapAdsPlugin = JavaPlugin.getPlugin(MapAdsPlugin.class);
        if (mapAdsPlugin.getConfig().getBoolean("disable-default-command")) {
            player.sendMessage("§cUnknown command");
            return;
        }

        final PluginDescriptionFile description = mapAdsPlugin.getDescription();
        player.sendMessage(L10n.get("prefix") + "§7v" + description.getVersion());
        player.sendMessage(L10n.get("prefix") + "§7Made by " + String.join(" & ", description.getAuthors()));
        player.spigot().sendMessage(new ComponentBuilder(L10n.get("prefix"))
                .append(new ComponentBuilder("§9§n" + description.getWebsite())
                        .event(new ClickEvent(ClickEvent.Action.OPEN_URL, description.getWebsite()))
                        .create())
                .create());
    }

    @Subcommand("advertise")
    @CommandPermission("mapads.command.advertise")
    public void handleAdvertise(final Player player) {
        final CreateAdGui createAdGui = new CreateAdGui(this.advertStorage,
                this.imageStorage,
                this.adScreenStorage,
                this.imageRetriever,
                this.imageConverter,
                this.configModel,
                this.economy,
                player);
        createAdGui.open();
    }

}
