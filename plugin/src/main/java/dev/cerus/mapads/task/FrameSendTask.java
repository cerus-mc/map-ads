package dev.cerus.mapads.task;

import dev.cerus.mapads.ConfigModel;
import dev.cerus.mapads.MapAdsPlugin;
import dev.cerus.mapads.compatibility.Compatibility;
import dev.cerus.mapads.screen.AdScreen;
import dev.cerus.mapads.screen.storage.AdScreenStorage;
import dev.cerus.mapads.util.ReviewerUtil;
import dev.cerus.maps.api.MapScreen;
import dev.cerus.maps.plugin.map.MapScreenRegistry;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import net.jodah.expiringmap.ExpiringMap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class FrameSendTask implements Runnable {

    public static final int TICK_PERIOD = 2 * 20;
    private static final double DIST_HARD = 30 * 30;
    private static final double DIST_SOFT = 15 * 15;

    private final ExpiringMap<UUID, List<Integer>> playerScreenMap = ExpiringMap.builder().expiration(15, TimeUnit.SECONDS).build();
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
                for (final Player player : ReviewerUtil.getNonReviewingPlayers(mapScreen)) {
                    if (!player.getWorld().getName().equals(screenLoc.getWorld().getName())) {
                        continue;
                    }

                    final List<Integer> screenList = this.playerScreenMap.computeIfAbsent(player.getUniqueId(), $ -> new ArrayList<>());
                    if (this.configModel.enableCustomDespawning) {
                        if (!screenList.contains(mapScreen.getId())) {
                            screenList.add(mapScreen.getId());
                            this.spawnScreen(player, mapScreen);
                            player.sendMessage("spawn screen " + mapScreen.getId());
                        }
                        if (this.ticks * (TICK_PERIOD / 20) >= 10) {
                            player.sendMessage("tick send " + mapScreen.getId());
                            mapScreen.sendFrames(player);
                            mapScreen.sendMaps(true, player);
                        }
                    } else {
                        final double distance = player.getLocation().distanceSquared(screenLoc);
                        if (screenList.contains(mapScreen.getId()) && distance > DIST_HARD) {
                            screenList.remove((Integer) mapScreen.getId());
                        } else if (!screenList.contains(mapScreen.getId()) && distance < DIST_HARD) {
                            if (this.compatibility != null && this.configModel.enableCustomDespawning) {
                                this.spawnScreen(player, mapScreen);
                            }
                            screenList.add(mapScreen.getId());
                            mapScreen.sendFrames(player);
                            mapScreen.sendMaps(true, player);
                        } else if (distance > DIST_SOFT && distance < DIST_HARD) {
                            mapScreen.sendFrames(player);
                        }
                    }
                    this.playerScreenMap.resetExpiration(player.getUniqueId());
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
