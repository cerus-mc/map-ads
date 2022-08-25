package dev.cerus.mapads.task;

import dev.cerus.mapads.ConfigModel;
import dev.cerus.mapads.compatibility.MapsCompat;
import dev.cerus.mapads.screen.AdScreen;
import dev.cerus.mapads.screen.storage.AdScreenStorage;
import dev.cerus.mapads.util.ReviewerUtil;
import dev.cerus.mapads.util.ScreenViewerUtil;
import dev.cerus.maps.api.MapScreen;
import dev.cerus.maps.plugin.map.MapScreenRegistry;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class FrameSendTask implements Runnable {

    public static final int TICK_PERIOD = 2 * 20;
    private final double distHard;
    private final double distSoft;

    private final ConfigModel configModel;
    private final AdScreenStorage adScreenStorage;
    private int ticks;

    public FrameSendTask(final ConfigModel configModel, final AdScreenStorage adScreenStorage) {
        this.configModel = configModel;
        this.adScreenStorage = adScreenStorage;

        this.distHard = configModel.despawnRange * configModel.despawnRange;
        this.distSoft = Math.max(2 * 2, Math.pow(configModel.despawnRange - 10, 2));
    }

    @Override
    public void run() {
        try {
            for (final AdScreen screen : this.adScreenStorage.getScreens()) {
                final MapScreen mapScreen = MapScreenRegistry.getScreen(screen.getScreenId());
                if (mapScreen != null && mapScreen.getLocation() != null) {
                    final Location screenLoc = mapScreen.getLocation();
                    for (final Player player : ReviewerUtil.getNonReviewingPlayers(mapScreen, false)) {
                        if (!player.getWorld().getName().equals(screenLoc.getWorld().getName())) {
                            continue;
                        }

                        final double distance = player.getLocation().distanceSquared(screenLoc);
                        if (ScreenViewerUtil.isViewer(mapScreen, player) && distance > this.distHard) {
                            ScreenViewerUtil.removeViewer(mapScreen, player);
                            if (MapsCompat.isThreeOrAbove()) {
                                mapScreen.despawnFrames(player);
                            }
                        } else if (!ScreenViewerUtil.isViewer(mapScreen, player) && distance < this.distHard) {
                            ScreenViewerUtil.addViewer(mapScreen, player);
                            if (MapsCompat.isThreeOrAbove()) {
                                mapScreen.spawnFrames(player);
                            } else {
                                mapScreen.sendFrames(player);
                            }
                            mapScreen.sendMaps(true, player);
                        } else if (distance > this.distSoft && distance < this.distHard) {
                            mapScreen.sendFrames(player);
                        }
                    }
                }
            }

            if (this.ticks * (TICK_PERIOD / 20) >= 10) {
                this.ticks = 0;
            } else {
                this.ticks++;
            }
        } catch (final Throwable t) {
            System.out.println(t.getMessage());
        }
    }

}
