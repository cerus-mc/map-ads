package dev.cerus.mapads.image.transition.recorder;

import dev.cerus.maps.api.MapScreen;
import dev.cerus.maps.api.graphics.MapGraphics;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public abstract class BinaryTransitionRecorder implements TransitionRecorder, AutoCloseable {

    private static final String HEADER = "MAPADS";
    private static final byte VERSION = 1;
    private Boolean state;

    @Override
    public void start(final MapScreen screen) {
        if (this.state != null) {
            throw new IllegalStateException("Recorder has been used already");
        }

        this.setup();

        this.state = true;
        this.writeHeader(screen);
    }

    @Override
    public void end(final MapScreen screen) {
        if (this.state == null || !this.state) {
            throw new IllegalStateException("Recorder has not started or already ended");
        }

        this.state = false;
        this.finish();
    }

    @Override
    public void record(final MapScreen screen) {
        if (this.state == null || !this.state) {
            throw new IllegalStateException("Recorder has not started or already ended");
        }

        this.writeFrame(screen.getGraphics());
    }

    protected void writeFrame(final MapGraphics<?, ?> graphics) {
        if (graphics.hasDirectAccessCapabilities()) {
            this.write(ByteBuffer.allocate(4).putInt(graphics.getDirectAccessData().length).array());
            this.write(graphics.getDirectAccessData());
        } else {
            this.write(ByteBuffer.allocate(4).putInt(graphics.getWidth() * graphics.getHeight()).array());
            for (int x = 0; x < graphics.getWidth(); x++) {
                for (int y = 0; y < graphics.getHeight(); y++) {
                    this.write(graphics.getPixel(x, y));
                }
            }
        }
    }

    private void writeHeader(final MapScreen screen) {
        this.writePre(HEADER.getBytes(StandardCharsets.UTF_8));
        this.writePre(VERSION);
        this.writePre(ByteBuffer.allocate(4).putInt(screen.getWidth() * 128).array());
        this.writePre(ByteBuffer.allocate(4).putInt(screen.getHeight() * 128).array());
    }

    protected void writePre(final byte... arr) {
        for (final byte b : arr) {
            this.writePre(b);
        }
    }

    protected void write(final byte... arr) {
        for (final byte b : arr) {
            this.write(b);
        }
    }

    protected abstract void setup();

    protected abstract void finish();

    protected abstract void writePre(int b);

    protected abstract void write(int b);

    @Override
    public boolean wasStarted() {
        return this.state != null;
    }

}
