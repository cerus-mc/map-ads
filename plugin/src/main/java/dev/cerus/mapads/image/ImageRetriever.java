package dev.cerus.mapads.image;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.imageio.ImageIO;

public class ImageRetriever {

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public CompletableFuture<Result> getImageAsync(final String url, final int maxSize) {
        final CompletableFuture<Result> future = new CompletableFuture<>();
        this.executorService.execute(() -> future.complete(this.getImage(url, maxSize)));
        return future;
    }

    public Result getImage(final String url, final int maxSize) {
        try {
            final HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestProperty("User-Agent", "github.com/cerus-mc/map-ads");
            connection.setRequestProperty("Accept", "*/*");
            connection.setDoInput(true);

            String contentType = connection.getContentType();
            if (contentType != null) {
                contentType = contentType.split(";")[0];
            }

            final int contentLength = connection.getContentLength();

            if (contentLength <= 0 || contentLength > maxSize) {
                connection.disconnect();
                return new Result(null, contentLength, null, contentType);
            }

            final InputStream inputStream = connection.getInputStream();
            final BufferedImage image = ImageIO.read(inputStream);
            return new Result(image, contentLength, null, contentType);
        } catch (final IOException e) {
            return new Result(null, -1, e, null);
        }
    }

    public static class Result {

        private final BufferedImage image;
        private final int len;
        private final Throwable err;
        private final String type;

        public Result(final BufferedImage image, final int len, final Throwable err, final String type) {
            this.image = image;
            this.len = len;
            this.err = err;
            this.type = type;
        }

        public BufferedImage getImage() {
            return this.image;
        }

        public int getLen() {
            return this.len;
        }

        public Throwable getErr() {
            return this.err;
        }

        public String getType() {
            return this.type;
        }

        public boolean isImage() {
            return this.type != null && (this.type.equals("image/png") || this.type.equals("image/jpeg"));
        }

    }

}
