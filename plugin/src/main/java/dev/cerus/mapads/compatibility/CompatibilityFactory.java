package dev.cerus.mapads.compatibility;

import dev.cerus.mapads.MapAdsPlugin;
import dev.cerus.mapads.screen.storage.AdScreenStorage;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class CompatibilityFactory {

    private static final Logger LOGGER = JavaPlugin.getPlugin(MapAdsPlugin.class).getLogger();

    public Compatibility makeCompatibilityLayer(final MapAdsPlugin plugin, final AdScreenStorage adScreenStorage) {
        final String version = Bukkit.getVersion();
        final String mcVer = version.substring(version.indexOf("MC:") + 4, version.length() - 1);
        if (!mcVer.matches("1\\.\\d{2}(\\.\\d+)?")) {
            LOGGER.severe("Failed to decode server version. Compatibility layer cannot be used.");
            return null;
        }

        final String[] split = mcVer.split("\\.");
        final int major = Integer.parseInt(split[1]);

        if (major < 17) {
            return new Below17Compatibility(plugin.getConfigModel(), adScreenStorage);
        } else {
            return new Above16Compatibility(plugin.getConfigModel(), adScreenStorage);
        }
    }

}
