package dev.cerus.mapads.screen.storage;

import dev.cerus.mapads.screen.AdScreen;
import dev.cerus.maps.plugin.map.MapScreenRegistry;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

public class YamlAdScreenStorageImpl implements AdScreenStorage {

    private final List<AdScreen> screens = new ArrayList<>();
    private final File file;
    private final FileConfiguration configuration;

    public YamlAdScreenStorageImpl(final File file, final FileConfiguration configuration) {
        this.file = file;
        this.configuration = configuration;
        this.loadScreens();
    }

    private void loadScreens() {
        for (final String key : this.configuration.getKeys(false)) {
            final ConfigurationSection section = this.configuration.getConfigurationSection(key);
            final String id = section.getString("id");
            final int mapScreenId = section.getInt("screen-id");
            final String transition = section.getString("transition");
            final AdScreen adScreen = new AdScreen(id, mapScreenId, transition);
            this.screens.add(adScreen);
        }
    }

    private void saveScreens() throws IOException {
        this.configuration.getKeys(false).forEach(s -> this.configuration.set(s, null));
        for (final AdScreen screen : this.screens) {
            final ConfigurationSection section = this.configuration.createSection(screen.getId());
            section.set("id", screen.getId());
            section.set("screen-id", screen.getScreenId());
            section.set("transition", screen.getTransition());
        }

        this.configuration.save(this.file);
    }

    @Override
    public AdScreen getAdScreen(final String id) {
        return this.screens.stream()
                .filter(adScreen -> adScreen.getId().equalsIgnoreCase(id))
                .findAny()
                .orElse(null);
    }

    @Override
    public AdScreen getAdScreen(final int id) {
        return this.screens.stream()
                .filter(adScreen -> adScreen.getScreenId() == id)
                .findAny()
                .orElse(null);
    }

    @Override
    public void updateAdScreen(final AdScreen adScreen) {
        if (!this.screens.contains(adScreen)) {
            this.screens.add(adScreen);
        }
        try {
            this.saveScreens();
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<AdScreen> getScreens() {
        return this.screens;
    }

    @Override
    public List<AdScreen> getBrokenScreens() {
        return this.screens.stream()
                .filter(adScreen -> MapScreenRegistry.getScreen(adScreen.getScreenId()) == null)
                .collect(Collectors.toList());
    }

    @Override
    public void close() throws Exception {
        this.saveScreens();
    }

}
