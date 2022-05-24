package dev.cerus.mapads.task;

import dev.cerus.mapads.screen.AdScreen;
import dev.cerus.mapads.screen.storage.AdScreenStorage;
import dev.cerus.maps.api.MapScreen;
import dev.cerus.maps.plugin.map.MapScreenRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import net.jodah.expiringmap.ExpiringMap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class FrameSendTask implements Runnable {

    private static final double DIST_HARD = 30 * 30;
    private static final double DIST_SOFT = 15 * 15;

    private final ExpiringMap<UUID, List<Integer>> playerScreenMap = ExpiringMap.builder().expiration(15, TimeUnit.SECONDS).build();
    private final AdScreenStorage adScreenStorage;

    public FrameSendTask(final AdScreenStorage adScreenStorage) {
        this.adScreenStorage = adScreenStorage;
    }

    @Override
    public void run() {
        for (final AdScreen screen : this.adScreenStorage.getScreens()) {
            final MapScreen mapScreen = MapScreenRegistry.getScreen(screen.getScreenId());
            if (mapScreen != null && mapScreen.getLocation() != null) {
                final Location screenLoc = mapScreen.getLocation();
                for (final Player player : Bukkit.getOnlinePlayers()) {
                    if (!player.getWorld().getName().equals(screenLoc.getWorld().getName())) {
                        continue;
                    }

                    final List<Integer> screenList = this.playerScreenMap.computeIfAbsent(player.getUniqueId(), $ -> new ArrayList<>());
                    final double distance = player.getLocation().distanceSquared(screenLoc);
                    if (screenList.contains(mapScreen.getId()) && distance > DIST_HARD) {
                        screenList.remove((Integer) mapScreen.getId());
                    } else if (!screenList.contains(mapScreen.getId()) && distance < DIST_HARD) {
                        screenList.add(mapScreen.getId());
                        mapScreen.sendFrames(player);
                        mapScreen.sendMaps(true, player);
                    } else if (distance < DIST_SOFT) {
                        mapScreen.sendFrames(player);
                    }
                    this.playerScreenMap.resetExpiration(player.getUniqueId());
                }
            }
        }

        /*for (final Integer screenId : MapScreenRegistry.getScreenIds()) {
            if (this.adScreenStorage.getAdScreen(screenId) != null) {
                final MapScreen screen = MapScreenRegistry.getScreen(screenId);
                final Player[] players = ReviewerUtil.getNonReviewingPlayers(screen).toArray(new Player[0]);
                screen.sendFrames(players);
            }
        }*/
    }

}
