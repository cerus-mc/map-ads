package dev.cerus.mapads.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Dependency;
import co.aikar.commands.annotation.Subcommand;
import dev.cerus.mapads.advert.Advertisement;
import dev.cerus.mapads.advert.storage.AdvertStorage;
import dev.cerus.mapads.economy.EconomyWrapper;
import dev.cerus.mapads.gui.DetailsGui;
import dev.cerus.mapads.gui.UnreviewedListGui;
import dev.cerus.mapads.image.storage.ImageStorage;
import dev.cerus.mapads.screen.storage.AdScreenStorage;
import org.bukkit.entity.Player;

@CommandAlias("mapads")
@Subcommand("review")
@CommandPermission("mapads.command.review")
public class ReviewCommand extends BaseCommand {

    @Dependency
    private AdvertStorage advertStorage;

    @Dependency
    private ImageStorage imageStorage;

    @Dependency
    private AdScreenStorage adScreenStorage;

    @Dependency
    private EconomyWrapper<?> economy;

    @Subcommand("single")
    @CommandCompletion("@mapads_adverts")
    public void handleReviewSingle(final Player player, final String advertId) {
        final Advertisement advertisement = this.advertStorage.getPendingAdvertisements().stream()
                .filter(adv -> adv.getAdvertId().toString().equals(advertId))
                .findAny()
                .orElse(null);
        if (advertisement != null) {
            final DetailsGui detailsGui = new DetailsGui(this.advertStorage, this.imageStorage, this.adScreenStorage, this.economy, player, advertisement);
            detailsGui.open();
        }
    }

    @Subcommand("list")
    public void handleReviewList(final Player player) {
        final UnreviewedListGui gui = new UnreviewedListGui(player, this.imageStorage, this.adScreenStorage, this.advertStorage, this.economy);
        gui.open();
    }

}
