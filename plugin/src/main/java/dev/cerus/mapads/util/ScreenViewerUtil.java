package dev.cerus.mapads.util;

import dev.cerus.maps.api.MapScreen;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;

public class ScreenViewerUtil {

    private static final Map<UUID, Set<Integer>> PLAYER_SCREEN_MAP = new ConcurrentHashMap<>();

    private ScreenViewerUtil() {
    }

    public static void addViewer(final MapScreen screen, final Player player) {
        PLAYER_SCREEN_MAP.computeIfAbsent(player.getUniqueId(), $ -> new HashSet<>()).add(screen.getId());
    }

    public static void removeViewer(final MapScreen screen, final Player player) {
        if (isViewer(screen, player)) {
            PLAYER_SCREEN_MAP.get(player.getUniqueId()).remove(screen.getId());
        }
    }

    public static boolean isViewer(final MapScreen screen, final Player player) {
        return PLAYER_SCREEN_MAP.containsKey(player.getUniqueId())
                && PLAYER_SCREEN_MAP.get(player.getUniqueId()).contains(screen.getId());
    }

    public static void clear(final Player player) {
        PLAYER_SCREEN_MAP.remove(player.getUniqueId());
    }

}
