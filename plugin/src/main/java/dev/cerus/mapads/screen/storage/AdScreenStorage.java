package dev.cerus.mapads.screen.storage;

import dev.cerus.mapads.MapAdsPlugin;
import dev.cerus.mapads.screen.AdScreen;
import dev.cerus.mapads.screen.ScreenGroup;
import java.util.List;
import java.util.Optional;
import org.bukkit.plugin.java.JavaPlugin;

public interface AdScreenStorage extends AutoCloseable {

    AdScreen getAdScreen(String id);

    AdScreen getAdScreen(int id);

    void updateAdScreen(AdScreen adScreen);

    void deleteAdScreen(AdScreen screen);

    ScreenGroup getScreenGroup(String name);

    default Optional<ScreenGroup> getScreenGroupSafely(String name, String caller) {
        ScreenGroup group = getScreenGroup(name);
        if (group == null) {
            JavaPlugin.getPlugin(MapAdsPlugin.class).getLogger().warning("Deleted screen group is still being referenced by " + caller);
            return Optional.empty();
        }
        return Optional.of(group);
    }

    void updateScreenGroup(ScreenGroup group);

    void deleteScreenGroup(ScreenGroup group);

    List<AdScreen> getScreens();

    List<AdScreen> getBrokenScreens();

    List<ScreenGroup> getScreenGroups();

}
