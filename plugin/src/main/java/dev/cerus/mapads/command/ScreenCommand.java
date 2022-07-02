package dev.cerus.mapads.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Dependency;
import co.aikar.commands.annotation.Subcommand;
import dev.cerus.mapads.advert.Advertisement;
import dev.cerus.mapads.advert.storage.AdvertStorage;
import dev.cerus.mapads.image.transition.TransitionRegistry;
import dev.cerus.mapads.image.transition.recorded.storage.RecordedTransitionStorage;
import dev.cerus.mapads.lang.L10n;
import dev.cerus.mapads.screen.AdScreen;
import dev.cerus.mapads.screen.ScreenGroup;
import dev.cerus.mapads.screen.storage.AdScreenStorage;
import dev.cerus.mapads.util.FrameMarkerUtil;
import dev.cerus.mapads.util.ReviewerUtil;
import dev.cerus.maps.api.MapScreen;
import dev.cerus.maps.plugin.map.MapScreenRegistry;
import java.util.UUID;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

@CommandAlias("mapads")
@Subcommand("screen")
@CommandPermission("mapads.command.screen")
public class ScreenCommand extends BaseCommand {

    @Dependency
    private AdScreenStorage adScreenStorage;

    @Dependency
    private AdvertStorage advertStorage;

    @Dependency
    private RecordedTransitionStorage recordedTransitionStorage;

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

    @Subcommand("delete")
    @CommandCompletion("@mapads_names")
    public void handleScreenDelete(final Player player, final String name) {
        final AdScreen adScreen = this.adScreenStorage.getAdScreen(name);
        if (adScreen == null) {
            player.sendMessage(L10n.getPrefixed("error.screen_not_found"));
            return;
        }

        final MapScreen mapScreen = MapScreenRegistry.getScreen(adScreen.getScreenId());
        if (mapScreen != null) {
            FrameMarkerUtil.unmark(mapScreen);
        }

        this.adScreenStorage.deleteAdScreen(adScreen);
        player.sendMessage(L10n.getPrefixed("success.deleted"));

        for (final ScreenGroup group : this.adScreenStorage.getScreenGroups()) {
            if (group.screenIds().contains(adScreen.getId())) {
                group.screenIds().remove(adScreen.getId());
                this.adScreenStorage.updateScreenGroup(group);
            }
        }

        this.recordedTransitionStorage.deleteAll(adScreen.getId());
        final UUID[] adIds = this.advertStorage.getAdvertisements(adScreen.getId()).stream()
                .map(Advertisement::getAdvertId)
                .toArray(UUID[]::new);
        if (adIds.length > 0) {
            this.advertStorage.deleteAdverts(adIds);
        }
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

        FrameMarkerUtil.mark(screen);
        final AdScreen adScreen = new AdScreen(name, screenId, "instant", -1, -1);
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

        FrameMarkerUtil.mark(screen);
        final MapScreen oldScreen = MapScreenRegistry.getScreen(adScreen.getScreenId());
        if (oldScreen != null) {
            FrameMarkerUtil.unmark(oldScreen);
        }

        this.recordedTransitionStorage.deleteAll(adScreen.getId());
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

        this.recordedTransitionStorage.deleteAll(adScreen.getId(), adScreen.getTransition());
        adScreen.setTransition(transition);
        this.adScreenStorage.updateAdScreen(adScreen);
        player.sendMessage(L10n.getPrefixed("success.updated"));
        if (TransitionRegistry.getTransition(transition).isPerformanceIntensive()) {
            player.sendMessage(L10n.getPrefixed("misc.intensive_transition"));
        }
    }

    @Subcommand("set fixedtime")
    @CommandCompletion("@mapads_names @range:1-60")
    public void handleScreenSetFixedTime(final Player player, final String name, final int fixedTime) {
        final AdScreen adScreen = this.adScreenStorage.getAdScreen(name);
        if (adScreen == null) {
            player.sendMessage(L10n.getPrefixed("error.screen_not_found"));
            return;
        }
        if (fixedTime <= 0) {
            player.sendMessage(L10n.getPrefixed("error.value_negative"));
            return;
        }

        adScreen.setFixedTime(fixedTime);
        this.adScreenStorage.updateAdScreen(adScreen);
        player.sendMessage(L10n.getPrefixed("success.updated"));
    }

    @Subcommand("set fixedprice")
    @CommandCompletion("@mapads_names 1000|5000|15000")
    public void handleScreenSetFixedTime(final Player player, final String name, final double fixedPrice) {
        final AdScreen adScreen = this.adScreenStorage.getAdScreen(name);
        if (adScreen == null) {
            player.sendMessage(L10n.getPrefixed("error.screen_not_found"));
            return;
        }
        if (fixedPrice <= 0) {
            player.sendMessage(L10n.getPrefixed("error.value_negative"));
            return;
        }

        adScreen.setFixedPrice(fixedPrice);
        this.adScreenStorage.updateAdScreen(adScreen);
        player.sendMessage(L10n.getPrefixed("success.updated"));
    }

    @Subcommand("remove fixedtime")
    @CommandCompletion("@mapads_names")
    public void handleScreenRemoveFixedTime(final Player player, final String name) {
        final AdScreen adScreen = this.adScreenStorage.getAdScreen(name);
        if (adScreen == null) {
            player.sendMessage(L10n.getPrefixed("error.screen_not_found"));
            return;
        }

        adScreen.setFixedTime(-1);
        this.adScreenStorage.updateAdScreen(adScreen);
        player.sendMessage(L10n.getPrefixed("success.updated"));
    }

    @Subcommand("remove fixedprice")
    @CommandCompletion("@mapads_names")
    public void handleScreenRemoveFixedPrice(final Player player, final String name) {
        final AdScreen adScreen = this.adScreenStorage.getAdScreen(name);
        if (adScreen == null) {
            player.sendMessage(L10n.getPrefixed("error.screen_not_found"));
            return;
        }

        adScreen.setFixedPrice(-1);
        this.adScreenStorage.updateAdScreen(adScreen);
        player.sendMessage(L10n.getPrefixed("success.updated"));
    }

}
