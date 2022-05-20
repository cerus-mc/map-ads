package dev.cerus.mapads.image;

import dev.cerus.maps.api.MapScreen;
import dev.cerus.maps.api.graphics.MapGraphics;
import dev.cerus.maps.api.graphics.StandaloneMapGraphics;
import java.util.UUID;

public class MapImage {

    public static final UUID DEFAULT_IMAGE_ID = new UUID(0, 0);

    private final UUID id;
    private final byte width;
    private final byte height;
    private MapGraphics<?, ?> graphics;

    public MapImage(final UUID id, final byte width, final byte height, final byte[][] data) {
        this.id = id;
        this.width = width;
        this.height = height;
        this.graphics = StandaloneMapGraphics.standalone(width * 128, height * 128);

        for (int x = 0; x < width * 128; x++) {
            for (int y = 0; y < height * 128; y++) {
                this.graphics.setPixel(x, y, data[x][y]);
            }
        }
    }

    public void drawOnto(final MapScreen screen, final int atX, final int atY) {
        if (this.width < screen.getWidth() || this.height < screen.getHeight()) {
            return;
        }

        final MapGraphics<?, ?> graphics = screen.getGraphics();
        graphics.place(this.graphics, atX, atY);
    }

    public void free() {
        // Even though this might not do much because the GC is
        // pretty fast it still feels good to release resources
        this.graphics = null;
    }

    public UUID getId() {
        return this.id;
    }

    public byte getWidth() {
        return this.width;
    }

    public byte getHeight() {
        return this.height;
    }

    public MapGraphics<?, ?> getGraphics() {
        return this.graphics;
    }

}
