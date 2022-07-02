package dev.cerus.mapads.image.transition.recorder;

import dev.cerus.maps.api.graphics.MapGraphics;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.zip.GZIPOutputStream;

public class CompressingTransitionRecorder extends BinaryTransitionRecorder {

    private ByteArrayOutputStream byteOut;
    private ByteArrayOutputStream tempByteOut;
    private GZIPOutputStream zipOut;
    private byte[] data;
    private int frames;

    @Override
    protected void setup() {
        try {
            this.byteOut = new ByteArrayOutputStream();
            this.tempByteOut = new ByteArrayOutputStream();
            this.zipOut = new GZIPOutputStream(this.tempByteOut);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void finish() {
        try {
            this.zipOut.finish();
            this.zipOut.flush();
            this.zipOut.close();
            this.byteOut.write(ByteBuffer.allocate(4).putInt(this.frames).array());
            this.byteOut.write(this.tempByteOut.toByteArray());
            this.data = this.byteOut.toByteArray();
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void writeFrame(final MapGraphics<?, ?> graphics) {
        super.writeFrame(graphics);
        this.frames++;
    }

    @Override
    protected void writePre(final int b) {
        this.byteOut.write(b);
    }

    @Override
    protected void write(final int b) {
        try {
            this.zipOut.write(b);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() throws Exception {
        this.zipOut.close();
    }

    public byte[] getData() {
        return this.data;
    }

}
