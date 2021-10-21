package dev.cerus.mapads.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Dependency;
import co.aikar.commands.annotation.Subcommand;
import dev.cerus.mapads.image.transition.TransitionRegistry;
import dev.cerus.mapads.lang.L10n;
import dev.cerus.mapads.screen.AdScreen;
import dev.cerus.mapads.screen.storage.AdScreenStorage;
import dev.cerus.mapads.util.ReviewerUtil;
import dev.cerus.maps.api.MapScreen;
import dev.cerus.maps.plugin.map.MapScreenRegistry;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

@CommandAlias("mapads")
@Subcommand("screen")
@CommandPermission("mapads.command.screen")
public class ScreenCommand extends BaseCommand {

    @Dependency
    private AdScreenStorage adScreenStorage;

    @Subcommand("teleport")
    @CommandCompletion("@mapads_names")
    public void handleScreenTeleport(final Player player, final String name) {
        final AdScreen adScreen = this.adScreenStorage.getAdScreen(name);
        if (adScreen == null) {
            player.sendMessage(L10n.getPrefixed("error.screen_not_found"));
            return;
        }
        final MapScreen screen = MapScreenRegistry.getScreen(adScreen.getScreenId());
        if (screen == null) {
            player.sendMessage(L10n.getPrefixed("error.screen_broken"));
            return;
        }
        ReviewerUtil.teleportTo(player, screen);
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
    }

    @Subcommand("create")
    @CommandCompletion("@nothing @maps_ids")
    public void handleScreenCreate(final Player player, final String name, final int screenId) {
        if (this.adScreenStorage.getAdScreen(name) != null) {
            player.sendMessage(L10n.getPrefixed("error.name_taken"));
            return;
        }
        final MapScreen screen = MapScreenRegistry.getScreen(screenId);
        if (screen == null) {
            player.sendMessage(L10n.getPrefixed("error.does_not_exist", screenId));
            return;
        }
        final AdScreen adScreen = new AdScreen(name, screenId, "instant");
        this.adScreenStorage.updateAdScreen(adScreen);
        player.sendMessage(L10n.getPrefixed("success.created", name));
    }

    @Subcommand("set screenid")
    @CommandCompletion("@mapads_names @maps_ids")
    public void handleScreenSetScreenId(final Player player, final String name, final int screenId) {
        final AdScreen adScreen = this.adScreenStorage.getAdScreen(name);
        if (adScreen == null) {
            player.sendMessage(L10n.getPrefixed("error.screen_not_found"));
            return;
        }
        final MapScreen screen = MapScreenRegistry.getScreen(screenId);
        if (screen == null) {
            player.sendMessage(L10n.getPrefixed("error.does_not_exist", screenId));
            return;
        }
        adScreen.setScreenId(screenId);
        this.adScreenStorage.updateAdScreen(adScreen);
        player.sendMessage(L10n.getPrefixed("success.updated"));
    }

    @Subcommand("set transition")
    @CommandCompletion("@mapads_names @mapads_transitions")
    public void handleScreenSetTransition(final Player player, final String name, final String transition) {
        final AdScreen adScreen = this.adScreenStorage.getAdScreen(name);
        if (adScreen == null) {
            player.sendMessage(L10n.getPrefixed("error.screen_not_found"));
            return;
        }
        if (TransitionRegistry.getTransition(transition) == null) {
            player.sendMessage(L10n.getPrefixed("error.transition_not_found"));
            return;
        }
        adScreen.setTransition(transition);
        this.adScreenStorage.updateAdScreen(adScreen);
        player.sendMessage(L10n.getPrefixed("success.updated"));
    }

}
