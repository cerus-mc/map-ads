package dev.cerus.mapads.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Dependency;
import co.aikar.commands.annotation.Subcommand;
import dev.cerus.mapads.ConfigModel;
import dev.cerus.mapads.advert.storage.AdvertStorage;
import dev.cerus.mapads.gui.CreateAdGui;
import dev.cerus.mapads.image.DefaultImageController;
import dev.cerus.mapads.image.ImageConverter;
import dev.cerus.mapads.image.ImageRetriever;
import dev.cerus.mapads.image.storage.ImageStorage;
import dev.cerus.mapads.screen.storage.AdScreenStorage;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;

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
    private Economy economy;

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
