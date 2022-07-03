package dev.cerus.mapads.listener;

import dev.cerus.mapads.util.ScreenViewerUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitListener implements Listener {

    @EventHandler
    public void onQuit(final PlayerQuitEvent event) {
        ScreenViewerUtil.clear(event.getPlayer());
    }

}
