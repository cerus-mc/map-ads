package dev.cerus.mapads.util;

import dev.cerus.mapads.MapAdsPlugin;
import dev.cerus.mapads.image.MapImage;
import dev.cerus.mapads.lang.L10n;
import dev.cerus.maps.api.MapScreen;
import dev.cerus.maps.api.version.VersionAdapter;
import dev.cerus.maps.version.VersionAdapterFactory;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import net.jodah.expiringmap.ExpiringMap;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class ReviewerUtil {

    private static final VersionAdapter VERSION_ADAPTER = new VersionAdapterFactory().makeAdapter();
    private static final ExpiringMap<UUID, Context> REVIEWER_MAP = ExpiringMap.builder()
            .expiration(1, TimeUnit.MINUTES)
            .expirationListener((o, o2) -> {
                Player player = ((Context) o2).player;
                if (!player.isOnline()) {
                    return;
                }
                ((Context) o2).screen.spawnFrames(player);
                ((Context) o2).screen.sendMaps(true, player);
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(L10n.get("misc.img_viewing")));
            })
            .build();

    public static void teleportTo(final Player player, final MapScreen screen) {
        final Location location = screen.getLocation();
        player.teleport(location);
        if (REVIEWER_MAP.containsKey(player.getUniqueId())) {
            Bukkit.getScheduler().runTaskLater(JavaPlugin.getPlugin(MapAdsPlugin.class), () ->
                    sendImage(player, screen, REVIEWER_MAP.get(player.getUniqueId()).image), 5);
        }
    }

    public static void sendImage(final Player player, final MapScreen screen, final MapImage image) {
        final MapScreen fakeScreen = new MapScreen((int) (System.currentTimeMillis() / 1000), VERSION_ADAPTER, screen.getWidth(), screen.getHeight());
        fakeScreen.setFrames(screen.getFrames());
        image.drawOnto(fakeScreen, 0, 0);
        fakeScreen.spawnFrames(player);
        fakeScreen.sendMaps(true, player);

        if (REVIEWER_MAP.containsKey(player.getUniqueId())) {
            REVIEWER_MAP.get(player.getUniqueId()).fakeScreen = fakeScreen;
        }
    }

    public static void markAsReviewer(final Player player, final MapImage image, final MapScreen screen) {
        if (REVIEWER_MAP.containsKey(player.getUniqueId())) {
            final Context ctx = REVIEWER_MAP.remove(player.getUniqueId());
            ctx.screen.spawnFrames(player);
            ctx.screen.sendMaps(true, player);
        }

        REVIEWER_MAP.put(player.getUniqueId(), new Context(player, image, screen));
    }

    public static Set<Player> getNonReviewingPlayers(final MapScreen screen) {
        return getNonReviewingPlayers(screen, true);
    }

    public static Set<Player> getNonReviewingPlayers(final MapScreen screen, final boolean checkViewer) {
        return Bukkit.getOnlinePlayers().stream()
                .filter(player -> !REVIEWER_MAP.containsKey(player.getUniqueId())
                        || REVIEWER_MAP.get(player.getUniqueId()).screen != screen)
                .filter(player -> !checkViewer || ScreenViewerUtil.isViewer(screen, player))
                .collect(Collectors.toSet());
    }

    public static Set<Player> getReviewingPlayers() {
        return REVIEWER_MAP.keySet().stream()
                .map(Bukkit::getPlayer)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    public static MapScreen getFakeScreen(final Player reviewer) {
        return REVIEWER_MAP.get(reviewer.getUniqueId()).fakeScreen;
    }

    private static class Context {

        public Player player;
        public MapImage image;
        public MapScreen screen;
        public MapScreen fakeScreen;

        public Context(final Player player, final MapImage image, final MapScreen screen) {
            this.player = player;
            this.image = image;
            this.screen = screen;
        }

    }

}
