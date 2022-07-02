package dev.cerus.mapads.screen.storage;

import dev.cerus.mapads.screen.AdScreen;
import dev.cerus.mapads.screen.ScreenGroup;
import dev.cerus.mapads.util.Mutable;
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
    private final List<ScreenGroup> groups = new ArrayList<>();
    private final File screenConfigFile;
    private final FileConfiguration screenConfig;
    private final File groupConfigFile;
    private final FileConfiguration groupConfig;

    public YamlAdScreenStorageImpl(final File screenConfigFile,
                                   final FileConfiguration screenConfig,
                                   final File groupConfigFile,
                                   final FileConfiguration groupConfig) {
        this.screenConfigFile = screenConfigFile;
        this.screenConfig = screenConfig;
        this.groupConfigFile = groupConfigFile;
        this.groupConfig = groupConfig;
        this.load();
    }

    private void load() {
        for (final String key : this.screenConfig.getKeys(false)) {
            final ConfigurationSection section = this.screenConfig.getConfigurationSection(key);
            final String id = section.getString("id");
            final int mapScreenId = section.getInt("screen-id");
            final String transition = section.getString("transition");
            final int fixedTime = section.getInt("fixed-time", -1);
            final double fixedPrice = section.getDouble("fixed-price", -1);
            final AdScreen adScreen = new AdScreen(id, mapScreenId, transition, fixedTime, fixedPrice);
            this.screens.add(adScreen);
        }

        for (final String key : this.groupConfig.getKeys(false)) {
            final ConfigurationSection section = this.groupConfig.getConfigurationSection(key);
            this.groups.add(new ScreenGroup(
                    key,
                    section.getString("name"),
                    section.getStringList("screens"),
                    Mutable.create(section.getInt("fixed-time", -1)),
                    Mutable.create(section.getDouble("fixed-price", -1))
            ));
        }
    }

    private void save() throws IOException {
        this.screenConfig.getKeys(false).forEach(s -> this.screenConfig.set(s, null));
        for (final AdScreen screen : this.screens) {
            final ConfigurationSection section = this.screenConfig.createSection(screen.getId());
            section.set("id", screen.getId());
            section.set("screen-id", screen.getScreenId());
            section.set("transition", screen.getTransition());
            section.set("fixed-time", screen.getFixedTime());
            section.set("fixed-price", screen.getFixedPrice());
        }
        this.screenConfig.save(this.screenConfigFile);

        this.groupConfig.getKeys(false).forEach(s -> this.groupConfig.set(s, null));
        for (final ScreenGroup group : this.groups) {
            final ConfigurationSection section = this.groupConfig.createSection(group.id());
            section.set("screens", group.screenIds());
            section.set("name", group.groupName());
            section.set("fixed-time", group.fixedTime().get());
            section.set("fixed-price", group.fixedPrice().get());
        }
        this.groupConfig.save(this.groupConfigFile);
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
            this.save();
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deleteAdScreen(final AdScreen screen) {
        this.screens.remove(screen);
        try {
            this.save();
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public ScreenGroup getScreenGroup(final String name) {
        return this.groups.stream()
                .filter(group -> group.id().equalsIgnoreCase(name))
                .findAny()
                .orElse(null);
    }

    @Override
    public void updateScreenGroup(final ScreenGroup group) {
        if (!this.groups.contains(group)) {
            this.groups.add(group);
        }
        try {
            this.save();
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deleteScreenGroup(final ScreenGroup group) {
        this.groups.remove(group);
        try {
            this.save();
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
    public List<ScreenGroup> getScreenGroups() {
        return this.groups;
    }

    @Override
    public void close() throws Exception {
        this.save();
    }

}
