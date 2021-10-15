package dev.cerus.mapads.image;

import dev.cerus.mapads.image.storage.ImageStorage;
import java.util.HashMap;
import java.util.Map;
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
                    this.plugin.getLogger().severe("Failed to load default image for size " + key + ": " + throwable.getMessage());
                    throwable.printStackTrace();
                    return;
                }
                if (mapImage == null) {
                    this.plugin.getLogger().warning("Default image for size " + key + " not found");
                    return;
                }

                this.defaultImageMap.put(key, mapImage);
            });
        }
    }

    public MapImage getDefaultImage(final int width, final int height) {
        return this.defaultImageMap.get(width + "x" + height);
    }

    public void setDefaultImage(final MapImage image) {
        final int w = image.getWidth() * 128;
        final int h = image.getHeight() * 128;
        this.defaultImageMap.put(w + "x" + h, image);

        final FileConfiguration config = this.plugin.getConfig();
        config.set("default-images." + w + "x" + h, image.getId().toString());
        this.plugin.saveConfig();
    }

}
