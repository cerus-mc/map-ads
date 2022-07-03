package dev.cerus.mapads.task;

import dev.cerus.mapads.ConfigModel;
import dev.cerus.mapads.MapAdsPlugin;
import dev.cerus.mapads.compatibility.Compatibility;
import dev.cerus.mapads.screen.AdScreen;
import dev.cerus.mapads.screen.storage.AdScreenStorage;
import dev.cerus.mapads.util.ReviewerUtil;
import dev.cerus.mapads.util.ScreenViewerUtil;
import dev.cerus.maps.api.MapScreen;
import dev.cerus.maps.plugin.map.MapScreenRegistry;
import java.util.HashSet;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class FrameSendTask implements Runnable {

    public static final int TICK_PERIOD = 2 * 20;
    private static final double DIST_HARD = 30 * 30;
    private static final double DIST_SOFT = 15 * 15;

    private final ConfigModel configModel;
    private final AdScreenStorage adScreenStorage;
    private final Compatibility compatibility;
    private int ticks;

    public FrameSendTask(final ConfigModel configModel, final AdScreenStorage adScreenStorage, final Compatibility compatibility) {
        this.configModel = configModel;
        this.adScreenStorage = adScreenStorage;
        this.compatibility = compatibility;
    }

    @Override
    public void run() {
        for (final AdScreen screen : this.adScreenStorage.getScreens()) {
            final MapScreen mapScreen = MapScreenRegistry.getScreen(screen.getScreenId());
            if (mapScreen != null && mapScreen.getLocation() != null) {
                final Location screenLoc = mapScreen.getLocation();
                for (final Player player : ReviewerUtil.getNonReviewingPlayers(mapScreen, false)) {
                    if (!player.getWorld().getName().equals(screenLoc.getWorld().getName())) {
                        continue;
                    }

                    if (this.configModel.enableCustomDespawning) {
                        if (!ScreenViewerUtil.isViewer(mapScreen, player)) {
                            ScreenViewerUtil.addViewer(mapScreen, player);
                            this.spawnScreen(player, mapScreen);
                        }
                        if (this.ticks * (TICK_PERIOD / 20) >= 10) {
                            mapScreen.sendFrames(player);
                            mapScreen.sendMaps(true, player);
                        }
                    } else {
                        final double distance = player.getLocation().distanceSquared(screenLoc);
                        if (ScreenViewerUtil.isViewer(mapScreen, player) && distance > DIST_HARD) {
                            ScreenViewerUtil.removeViewer(mapScreen, player);
                        } else if (!ScreenViewerUtil.isViewer(mapScreen, player) && distance < DIST_HARD) {
                            if (this.compatibility != null && this.configModel.enableCustomDespawning) {
                                this.spawnScreen(player, mapScreen);
                            }
                            ScreenViewerUtil.addViewer(mapScreen, player);
                            mapScreen.sendFrames(player);
                            mapScreen.sendMaps(true, player);
                        } else if (distance > DIST_SOFT && distance < DIST_HARD) {
                            mapScreen.sendFrames(player);
                        }
                    }
                }
            }
        }

        if (this.ticks * (TICK_PERIOD / 20) >= 10) {
            this.ticks = 0;
        } else {
            this.ticks++;
        }

        /*for (final Integer screenId : MapScreenRegistry.getScreenIds()) {
            if (this.adScreenStorage.getAdScreen(screenId) != null) {
                final MapScreen screen = MapScreenRegistry.getScreen(screenId);
                final Player[] players = ReviewerUtil.getNonReviewingPlayers(screen).toArray(new Player[0]);
                screen.sendFrames(players);
            }
        }*/
    }

    private void spawnScreen(final Player player, final MapScreen mapScreen) {
        Bukkit.getServer().getScheduler().runTask(JavaPlugin.getPlugin(MapAdsPlugin.class), () -> {
            final Set<Entity> entities = new HashSet<>();
            for (final int[] arr : mapScreen.getFrameIds()) {
                for (final int eid : arr) {
                    final Entity entity = this.compatibility.getEntity(mapScreen.getLocation().getWorld(), eid);
                    if (entity != null) {
                        entities.add(entity);
                    }
                }
            }

            if (!entities.isEmpty()) {
                Bukkit.getServer().getScheduler().runTaskAsynchronously(JavaPlugin.getPlugin(MapAdsPlugin.class), () -> {
                    entities.forEach(entity -> this.compatibility.spawnEntity(player, entity));
                });
            }
        });
    }

}
