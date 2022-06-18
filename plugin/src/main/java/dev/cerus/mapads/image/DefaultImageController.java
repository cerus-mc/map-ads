package dev.cerus.mapads.image;

import dev.cerus.mapads.image.storage.ImageStorage;
import dev.cerus.mapads.screen.AdScreen;
import dev.cerus.maps.api.MapScreen;
import dev.cerus.maps.plugin.map.MapScreenRegistry;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class DefaultImageController {

    private final Map<String, MapImage> defaultImageMap = new HashMap<>();
    private final JavaPlugin plugin;

    public DefaultImageController(final JavaPlugin plugin, final ImageStorage imageStorage) {
        this.plugin = plugin;
        this.loadDefaultImages(imageStorage);
    }

    private void loadDefaultImages(final ImageStorage imageStorage) {
        final ConfigurationSection section = this.plugin.getConfig().getConfigurationSection("default-images");
        for (final String key : section.getKeys(false)) {
            final UUID imgId = UUID.fromString(section.getString(key));
            imageStorage.getMapImage(imgId).whenComplete((mapImage, throwable) -> {
                if (throwable != null) {
                    this.plugin.getLogger().severe("Failed to load default image for " + key + ": " + throwable.getMessage());
                    throwable.printStackTrace();
                    return;
                }
                if (mapImage == null) {
                    this.plugin.getLogger().warning("Default image for " + key + " not found");
                    return;
                }

                this.defaultImageMap.put(key, mapImage);
            });
        }
    }

    public MapImage getDefaultImage(final AdScreen adScreen) {
        final MapScreen mapScreen = MapScreenRegistry.getScreen(adScreen.getScreenId());
        if (mapScreen == null) {
            throw new IllegalStateException("Map screen " + adScreen.getScreenId() + " not found (referenced by " + adScreen.getId() + ")");
        }
        return Optional.ofNullable(this.defaultImageMap.get(adScreen.getId()))
                .orElseGet(() -> this.defaultImageMap.get((mapScreen.getWidth() * 128) + "x" + (mapScreen.getHeight() * 128)));
    }

    /**
     * @deprecated Per screen default images are now supported, use {@link DefaultImageController#getDefaultImage(AdScreen)} instead
     */
    @Deprecated
    public MapImage getDefaultImage(final int width, final int height) {
        return this.defaultImageMap.get(width + "x" + height);
    }

    public void setDefaultImage(final String key, final MapImage image) {
        this.defaultImageMap.put(key, image);
        final FileConfiguration config = this.plugin.getConfig();
        config.set("default-images." + key, image.getId().toString());
        this.plugin.saveConfig();
    }

}
