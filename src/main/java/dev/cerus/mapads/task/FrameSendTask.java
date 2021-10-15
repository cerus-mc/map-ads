package dev.cerus.mapads.task;

import dev.cerus.mapads.screen.storage.AdScreenStorage;
import dev.cerus.mapads.util.ReviewerUtil;
import dev.cerus.maps.api.MapScreen;
import dev.cerus.maps.plugin.map.MapScreenRegistry;
import org.bukkit.entity.Player;

public class FrameSendTask implements Runnable {

    private final AdScreenStorage adScreenStorage;

    public FrameSendTask(final AdScreenStorage adScreenStorage) {
        this.adScreenStorage = adScreenStorage;
    }

    @Override
    public void run() {
        for (final Integer screenId : MapScreenRegistry.getScreenIds()) {
            if (this.adScreenStorage.getAdScreen(screenId) != null) {
                final MapScreen screen = MapScreenRegistry.getScreen(screenId);
                final Player[] players = ReviewerUtil.getNonReviewingPlayers(screen).toArray(new Player[0]);
                screen.sendFramesOnly(players);
            }
        }

        ReviewerUtil.getReviewingPlayers().forEach(player -> {
            final MapScreen fakeScreen = ReviewerUtil.getFakeScreen(player);
            fakeScreen.sendFramesOnly(player);
        });
    }

}
