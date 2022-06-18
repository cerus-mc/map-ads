package dev.cerus.mapads.lang;

import dev.cerus.mapads.MapAdsPlugin;
import java.io.File;
import java.io.IOException;
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

        int ver = verInt;
        while (ver < langManifest.getCurrentVersion()) {
            final Map<String, String> updates = langManifest.getUpdatesFor(ver + 1);
            updates.forEach((k, v) -> {
                if (!langConf.contains(k)) {
                    langConf.set(k.replace(".", ","), v);
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
