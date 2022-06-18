package dev.cerus.mapads.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Dependency;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Subcommand;
import dev.cerus.mapads.advert.Advertisement;
import dev.cerus.mapads.advert.storage.AdvertStorage;
import dev.cerus.mapads.gui.AdDetailsGui;
import dev.cerus.mapads.gui.AdListGui;
import dev.cerus.mapads.gui.AdScreenListGui;
import dev.cerus.mapads.gui.ConfirmationGui;
import dev.cerus.mapads.image.storage.ImageStorage;
import dev.cerus.mapads.lang.L10n;
import dev.cerus.mapads.screen.AdScreen;
import dev.cerus.mapads.screen.storage.AdScreenStorage;
import dev.cerus.mapads.util.ItemBuilder;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.entity.Player;

@CommandAlias("mapads")
@Subcommand("advert")
@CommandPermission("mapads.command.advert")
public class AdvertCommand extends BaseCommand {

    @Dependency
    private AdvertStorage advertStorage;

    @Dependency
    private ImageStorage imageStorage;

    @Dependency
    private AdScreenStorage adScreenStorage;

    @Subcommand("list")
    @CommandCompletion("@mapads_names")
    public void handleList(final Player player, @Optional final String screenName) {
        if (screenName == null) {
            new AdScreenListGui(player, this.adScreenStorage, adScreen ->
                    player.performCommand("mapads advert list " + adScreen.getId())).open();
        } else {
            final AdScreen adScreen = this.adScreenStorage.getAdScreen(screenName);
            if (adScreen == null) {
                player.sendMessage(L10n.getPrefixed("error.screen_not_found"));
                return;
            }

            new AdListGui(this.advertStorage, adScreen, player, advertisement ->
                    new AdDetailsGui(advertisement, player).open()).open();
        }
    }

    @Subcommand("remove")
    @CommandCompletion("ADVERT-ID")
    public void handleRemove(final Player player, final UUID advertId) {
        final Advertisement advert = this.advertStorage.getAdvert(advertId);
        if (advert == null) {
            player.sendMessage(L10n.getPrefixed("error.advert_not_found"));
            return;
        }

        new ConfirmationGui("Delete ad?", () ->
                new ItemBuilder(Material.PAPER)
                        .setName(L10n.get("command.advert.remove.confirm.name"))
                        .setLore(L10n.getList("command.advert.remove.confirm.lore"))
                        .build())
                .onYes(() -> {
                    player.closeInventory();
                    this.advertStorage.deleteAdverts(advert.getAdvertId());
                    this.imageStorage.deleteMapImages(advert.getImageId());
                    player.sendMessage(L10n.getPrefixed("success.ad_deleted"));
                }).onNo(player::closeInventory).open(player);
    }

}
