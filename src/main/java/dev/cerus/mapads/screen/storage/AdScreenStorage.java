package dev.cerus.mapads.screen.storage;

import dev.cerus.mapads.screen.AdScreen;
import java.util.List;

public interface AdScreenStorage extends AutoCloseable {

    AdScreen getAdScreen(String id);

    AdScreen getAdScreen(int id);

    void updateAdScreen(AdScreen adScreen);

    List<AdScreen> getScreens();

    List<AdScreen> getBrokenScreens();

}
