package dev.cerus.mapads.listener;

import dev.cerus.mapads.MapAdsPlugin;
import dev.cerus.mapads.advert.storage.AdvertStorage;
import dev.cerus.mapads.lang.L10n;
import dev.cerus.mapads.screen.AdScreen;
import dev.cerus.mapads.screen.storage.AdScreenStorage;
import dev.cerus.mapads.update.UpdaterChecker;
import dev.cerus.maps.api.MapScreen;
import dev.cerus.maps.plugin.map.MapScreenRegistry;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class PlayerJoinListener implements Listener {

    private final MapAdsPlugin plugin;
    private final AdScreenStorage adScreenStorage;
    private final AdvertStorage advertStorage;

    public PlayerJoinListener(final MapAdsPlugin plugin, final AdScreenStorage adScreenStorage, final AdvertStorage advertStorage) {
        this.plugin = plugin;
        this.adScreenStorage = adScreenStorage;
        this.advertStorage = advertStorage;
    }

    @EventHandler
    public void onJoin(final PlayerJoinEvent event) {
        final Player player = event.getPlayer();

        Bukkit.getScheduler().runTaskLaterAsynchronously(JavaPlugin.getPlugin(MapAdsPlugin.class), () -> {
            for (final AdScreen screen : this.adScreenStorage.getScreens()) {
                final MapScreen mapScreen = MapScreenRegistry.getScreen(screen.getScreenId());
                if (mapScreen != null) {
                    mapScreen.sendFrames(player);
                    mapScreen.sendMaps(true, player);
                }
            }
        }, 5);

        if (player.hasPermission("mapads.update") && this.plugin.getConfigModel().updateMessage) {
            UpdaterChecker.getNewestVersion().whenComplete((ver, throwable) -> {
                if (throwable != null) {
                    throwable.printStackTrace();
                    this.plugin.getLogger().severe("Failed to check for updates");
                    return;
                }

                final boolean hasUpdate = UpdaterChecker.isGreater(ver, this.plugin.getDescription().getVersion());
                if (hasUpdate) {
                    player.sendMessage(L10n.getPrefixed("misc.update.0"));
                    player.sendMessage(L10n.getPrefixed("misc.update.1", "https://www.spigotmc.org/resources/96918/"));
                }
            });
        }

        if (!player.hasPermission("mapads.admin")) {
            return;
        }

        if (this.plugin.areScreensLoaded()) {
            for (final AdScreen brokenScreen : this.adScreenStorage.getBrokenScreens()) {
                player.sendMessage(L10n.getPrefixed("misc.broken_screen.0", brokenScreen.getId()));
                player.sendMessage(L10n.getPrefixed("misc.broken_screen.1", brokenScreen.getId()));
            }
        }

        if (!this.advertStorage.getPendingAdvertisements().isEmpty()) {
            player.sendMessage(L10n.getPrefixed("misc.pending", this.advertStorage.getPendingAdvertisements().size()));
        }
    }

}
