package dev.cerus.mapads.lang;

import dev.cerus.mapads.MapAdsPlugin;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.logging.Logger;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class LangUpdater {

    private final Logger logger = JavaPlugin.getPlugin(MapAdsPlugin.class).getLogger();

    public void update(final File langFile, final FileConfiguration langConf) {
        final LangManifest langManifest = LangManifest.load();

        String version = langConf.getString("ver", "0");
        if (!version.matches("\\d+")) {
            version = "0";
        }

        final int verInt = Integer.parseInt(version);
        if (verInt >= langManifest.getCurrentVersion()) {
            return;
        }

        try {
            final File backup = new File(langFile.getPath() + ".backup");
            if (backup.exists()) {
                backup.delete();
            }
            Files.copy(langFile.toPath(), backup.toPath());
        } catch (final IOException e) {
            this.logger.warning("Failed to create language file backup: " + e.getMessage());
        }

        int ver = verInt;
        while (ver < langManifest.getCurrentVersion()) {
            final Map<String, Object> updates = langManifest.getUpdatesFor(ver + 1);
            updates.forEach((k, v) -> {
                final String key = k.replace(".", ",");
                if (!langConf.contains(key)) {
                    langConf.set(key, v);
                }
            });
            ver++;
            this.logger.info("Updated language file to v" + ver);
        }
        langConf.set("ver", String.valueOf(ver));

        try {
            langConf.save(langFile);
        } catch (final IOException e) {
            throw new IllegalStateException("Failed to save lang config", e);
        }
    }


}
