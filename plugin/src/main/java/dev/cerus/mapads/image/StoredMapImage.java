package dev.cerus.mapads.image;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class StoredMapImage {

    private final UUID id;
    private final byte width;
    private final byte height;
    private final byte[] compressedData;

    public StoredMapImage(final UUID id, final byte width, final byte height, final byte[] compressedData) {
        this.id = id;
        this.width = width;
        this.height = height;
        this.compressedData = compressedData;
    }

    public static StoredMapImage compress(final MapImage mapImage) {
        final ByteArrayOutputStream bout = new ByteArrayOutputStream();
        try (final GZIPOutputStream out = new GZIPOutputStream(bout)) {
            for (int x = 0; x < mapImage.getWidth() * 128; x++) {
                for (int y = 0; y < mapImage.getHeight() * 128; y++) {
                    out.write(mapImage.getGraphics().getPixel(x, y));
                }
            }
            out.finish();
            out.flush();
        } catch (final IOException e) {
            e.printStackTrace();
            return null;
        }
        return new StoredMapImage(mapImage.getId(), mapImage.getWidth(), mapImage.getHeight(), bout.toByteArray());
    }

    public MapImage decompress() {
        final byte[][] data = new byte[this.width * 128][this.height * 128];
        try (final ByteArrayInputStream bin = new ByteArrayInputStream(this.compressedData);
             final GZIPInputStream in = new GZIPInputStream(bin)) {
            final byte[] bytes = in.readAllBytes();
            int index = 0;
            for (int x = 0; x < this.width * 128; x++) {
                for (int y = 0; y < this.height * 128; y++) {
                    data[x][y] = bytes[index++];
                }
            }
        } catch (final IOException e) {
            e.printStackTrace();
            return null;
        }
        return new MapImage(this.id, this.width, this.height, data);
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

    public byte[] getCompressedData() {
        return this.compressedData;
    }

}
