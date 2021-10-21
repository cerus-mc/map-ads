package dev.cerus.mapads.image;

import com.github.regarrzo.fsd.ColorPalette;
import com.github.regarrzo.fsd.Ditherer;
import dev.cerus.maps.api.MapColor;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImageConverter {

    private static final ColorPalette COLOR_PALETTE = new ColorPalette(Arrays.stream(MapColor.values())
            .map(MapColor::getColor)
            .map(color -> color == null ? new Color(0, 0, 0, 0) : color)
            .toArray(Color[]::new));
    private static final Ditherer DITHERER = new Ditherer(COLOR_PALETTE);

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final ColorCache colorCache;

    public ImageConverter(final ColorCache colorCache) {
        this.colorCache = colorCache;
    }

    public MapImage convert(final BufferedImage image, final Dither dither) {
        if (image.getWidth() % 128 > 0 || image.getHeight() % 128 > 0) {
            throw new IllegalStateException("Invalid size: " + image.getWidth() + "x" + image.getHeight());
        }

        final byte w = (byte) (image.getWidth() / 128);
        final byte h = (byte) (image.getHeight() / 128);
        final byte[][] data = new byte[w * 128][h * 128];

        if (dither == Dither.FLOYD_STEINBERG) {
            DITHERER.dither(image);
        }
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                final int rgb = image.getRGB(x, y);
                if ((rgb >> 24) == 0x00) {
                    data[x][y] = (byte) MapColor.TRANSPARENT_0.getId();
                } else {
                    final Color color = new Color(rgb);
                    data[x][y] = this.colorCache == null
                            ? (byte) MapColor.rgbToMapColor(color.getRed(), color.getGreen(), color.getBlue()).getId()
                            : this.colorCache.getColor(color.getRed(), color.getGreen(), color.getBlue());
                }
            }
        }

        return new MapImage(UUID.randomUUID(), w, h, data);
    }

    public CompletableFuture<MapImage> convertAsync(final BufferedImage image, final Dither dither) {
        final CompletableFuture<MapImage> future = new CompletableFuture<>();
        this.executorService.execute(() -> {
            try {
                future.complete(this.convert(image, dither));
            } catch (final Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    public enum Dither {
        NONE, FLOYD_STEINBERG
    }

}
