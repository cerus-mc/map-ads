package dev.cerus.mapads.image.transition.recorded;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.zip.GZIPInputStream;

public class BinaryRecordedTransition extends RecordedTransition implements AutoCloseable {

    private InputStream in;
    private byte version;
    private int width;
    private int height;
    private int currentIndex;
    private int frames;

    public BinaryRecordedTransition(final File file) throws IOException {
        this(new FileInputStream(file));
    }

    public BinaryRecordedTransition(final InputStream in) throws IOException {
        this.in = in;
        this.readHeader();
    }

    public BinaryRecordedTransition(final byte[] data) throws IOException {
        this(new ByteArrayInputStream(data));
    }

    private void readHeader() throws IOException {
        final byte[] header = new byte[6];
        this.in.read(header);
        if (!"MAPADS".equals(new String(header))) {
            throw new IllegalStateException("Not a recorded transition");
        }

        this.version = (byte) this.in.read();
        final byte[] buf = new byte[4];
        this.in.read(buf);
        this.width = ByteBuffer.wrap(buf).getInt();
        this.in.read(buf);
        this.height = ByteBuffer.wrap(buf).getInt();
        this.in.read(buf);
        this.frames = ByteBuffer.wrap(buf).getInt();

        this.switchToZipped();
    }

    private void switchToZipped() throws IOException {
        this.in = new GZIPInputStream(this.in);
    }

    @Override
    public byte[] getFrame(final int index) {
        if (index > this.currentIndex) {
            throw new IllegalArgumentException(index + " > " + this.currentIndex);
        }
        if (index < this.currentIndex) {
            throw new IllegalArgumentException(index + " < " + this.currentIndex);
        }

        try {
            byte[] buf = this.read(4);
            final int amt = ByteBuffer.wrap(buf).getInt();
            buf = this.read(amt);
            this.currentIndex++;
            return buf;
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] read(final int amount) throws IOException {
        final byte[] buf = new byte[amount];
        int totalRead = 0;
        while (totalRead < amount) {
            final int read = this.in.read(buf, totalRead, amount - totalRead);
            totalRead += read;
        }
        return buf;
    }

    @Override
    public int getVersion() {
        return this.version;
    }

    @Override
    public int getWidth() {
        return this.width;
    }

    @Override
    public int getHeight() {
        return this.height;
    }

    @Override
    public int getFrameCount() {
        return this.frames;
    }

    @Override
    public void close() throws Exception {
        this.in.close();
    }

}
