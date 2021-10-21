package dev.cerus.mapads.image;

import dev.cerus.mapads.MapAdsPlugin;
import dev.cerus.maps.api.MapColor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Logger;
import org.bukkit.plugin.java.JavaPlugin;

public class ColorCache {

    private final byte[][][] colors = new byte[256][256][256];

    private ColorCache() {
    }

    public static ColorCache fromInputStream(final InputStream inputStream) throws IOException {
        final ColorCache colorCache = new ColorCache();
        for (int r = 0; r < 256; r++) {
            for (int g = 0; g < 256; g++) {
                for (int b = 0; b < 256; b++) {
                    colorCache.colors[r][g][b] = (byte) inputStream.read();
                }
            }
        }
        return colorCache;
    }

    public static ColorCache generate() {
        final Logger logger = JavaPlugin.getPlugin(MapAdsPlugin.class).getLogger();

        final ColorCache colorCache = new ColorCache();
        for (int r = 0; r < 256; r++) {
            final int step = (int) (r * 100d / 255d);
            logger.info("Generating color cache / " + step + "% of 100%");
            for (int g = 0; g < 256; g++) {
                for (int b = 0; b < 256; b++) {
                    final MapColor mapColor = MapColor.rgbToMapColor(r, g, b);
                    colorCache.colors[r][g][b] = (byte) mapColor.getId();
                }
            }
        }
        return colorCache;
    }

    public void write(final OutputStream outputStream) throws IOException {
        for (int r = 0; r < 256; r++) {
            for (int g = 0; g < 256; g++) {
                for (int b = 0; b < 256; b++) {
                    outputStream.write(this.colors[r][g][b]);
                }
            }
        }
    }

    public byte getColor(final int r, final int g, final int b) {
        return this.colors[r][g][b];
    }

}
