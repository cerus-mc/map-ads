package dev.cerus.mapads.screen.storage;

import dev.cerus.mapads.screen.AdScreen;
import dev.cerus.mapads.screen.ScreenGroup;
import java.util.List;

public interface AdScreenStorage extends AutoCloseable {

    AdScreen getAdScreen(String id);

    AdScreen getAdScreen(int id);

    void updateAdScreen(AdScreen adScreen);

    void deleteAdScreen(AdScreen screen);

    ScreenGroup getScreenGroup(String name);

    void updateScreenGroup(ScreenGroup group);

    void deleteScreenGroup(ScreenGroup group);

    List<AdScreen> getScreens();

    List<AdScreen> getBrokenScreens();

    List<ScreenGroup> getScreenGroups();

}
