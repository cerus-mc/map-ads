package dev.cerus.mapads.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Dependency;
import co.aikar.commands.annotation.Subcommand;
import dev.cerus.mapads.advert.Advertisement;
import dev.cerus.mapads.advert.storage.AdvertStorage;
import dev.cerus.mapads.image.storage.ImageStorage;
import dev.cerus.mapads.lang.L10n;
import java.util.List;
import java.util.UUID;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.entity.Player;

@CommandAlias("mapads")
@Subcommand("advert")
@CommandPermission("mapads.command.advert")
public class AdvertCommand extends BaseCommand {

    @Dependency
    private AdvertStorage advertStorage;

    @Dependency
    private ImageStorage imageStorage;

    @Subcommand("list")
    @CommandCompletion("@mapads_names")
    public void handleList(final Player player, final String screenName) {
        final List<Advertisement> list = this.advertStorage.getRunningAdvertisements(screenName);
        player.sendMessage(L10n.get("prefix") + "§7" + list.size() + " ads");
        list.forEach(advertisement -> {
            player.spigot().sendMessage(new ComponentBuilder("§8- §f" + advertisement.getAdvertId().toString())
                    .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("§7Click to copy")))
                    .event(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, advertisement.getAdvertId().toString()))
                    .create());
        });
    }

    @Subcommand("remove")
    @CommandCompletion("ADVERT-ID")
    public void handleRemove(final Player player, final UUID advertId) {
        final Advertisement advert = this.advertStorage.getAdvert(advertId);
        if (advert == null) {
            player.sendMessage(L10n.get("prefix") + "§cAdvert not found");
            return;
        }

        this.advertStorage.deleteAdverts(advert.getAdvertId());
        this.imageStorage.deleteMapImages(advert.getImageId());
        player.sendMessage(L10n.get("prefix") + "§aAdvert was deleted");
    }

}
