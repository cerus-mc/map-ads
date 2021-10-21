package dev.cerus.mapads.image;

import dev.cerus.maps.api.MapScreen;
import dev.cerus.maps.api.graphics.MapScreenGraphics;
import java.util.UUID;

public class MapImage {

    public static final UUID DEFAULT_IMAGE_ID = new UUID(0, 0);

    private final UUID id;
    private final byte width;
    private final byte height;
    private byte[][] data;

    public MapImage(final UUID id, final byte width, final byte height, final byte[][] data) {
        this.id = id;
        this.width = width;
        this.height = height;
        this.data = data;
    }

    public void drawOnto(final MapScreen screen) {
        if (this.width < screen.getWidth() || this.height < screen.getHeight()) {
            return;
        }

        final MapScreenGraphics graphics = screen.getGraphics();
        for (int x = 0; x < this.width * 128; x++) {
            for (int y = 0; y < this.height * 128; y++) {
                graphics.setPixel(x, y, this.data[x][y]);
            }
        }
    }

    public void free() {
        // Even though this might not do much because the GC is
        // pretty fast it still feels good to release resources
        this.data = null;
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

    public byte[][] getData() {
        return this.data;
    }

}
