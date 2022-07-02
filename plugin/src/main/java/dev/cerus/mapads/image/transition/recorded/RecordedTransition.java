package dev.cerus.mapads.image.transition.recorded;

import dev.cerus.maps.api.MapScreen;
import dev.cerus.maps.api.graphics.MapGraphics;

public abstract class RecordedTransition {

    public void drawFrame(final MapScreen screen, final int index) {
        if (screen.getWidth() * 128 != this.getWidth()
                || screen.getHeight() * 128 != this.getHeight()) {
            throw new IllegalArgumentException("Dimensions don't match");
        }

        final byte[] frame = this.getFrame(index);
        final MapGraphics<?, ?> graphics = screen.getGraphics();

        if (graphics.hasDirectAccessCapabilities()) {
            System.arraycopy(
                    frame,
                    0,
                    graphics.getDirectAccessData(),
                    0,
                    frame.length
            );
        } else {
            for (int x = 0; x < screen.getWidth(); x++) {
                for (int y = 0; y < screen.getHeight(); y++) {
                    graphics.setPixel(x, y, frame[x + y * (screen.getWidth() * 128)]);
                }
            }
        }
    }

    public abstract byte[] getFrame(int index);

    public abstract int getVersion();

    public abstract int getWidth();

    public abstract int getHeight();

    public abstract int getFrameCount();

}
