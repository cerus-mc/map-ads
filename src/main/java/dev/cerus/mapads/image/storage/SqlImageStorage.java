package dev.cerus.mapads.image.storage;

import dev.cerus.mapads.image.MapImage;
import dev.cerus.mapads.image.StoredMapImage;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public abstract class SqlImageStorage implements ImageStorage {

    protected void initTable() {
        this.run(() -> {
            try (final Connection connection = this.getConnection();
                 final PreparedStatement statement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS `mapads_images` " +
                         "(id VACHAR(64) PRIMARY KEY, width TINYINT, height TINYINT, imgdata MEDIUMTEXT)")) {
                statement.executeUpdate();
            } catch (final SQLException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public CompletableFuture<Void> updateMapImage(final MapImage mapImage) {
        final CompletableFuture<Void> future = new CompletableFuture<>();
        this.run(() -> {
            final byte[] compressedData = StoredMapImage.compress(mapImage).getCompressedData();
            final String encodedData = new String(Base64.getEncoder().encode(compressedData));

            try (final Connection connection = this.getConnection();
                 final PreparedStatement statement = connection.prepareStatement(this.makeImageInsertQuery())) {
                statement.setString(1, mapImage.getId().toString());
                statement.setByte(2, mapImage.getWidth());
                statement.setByte(3, mapImage.getHeight());
                statement.setString(4, encodedData);
                statement.setByte(5, mapImage.getWidth());
                statement.setByte(6, mapImage.getHeight());
                statement.setString(7, encodedData);
                statement.executeUpdate();

                future.complete(null);
            } catch (final SQLException e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    @Override
    public CompletableFuture<MapImage> getMapImage(final UUID id) {
        final CompletableFuture<MapImage> future = new CompletableFuture<>();
        this.run(() -> {
            try (final Connection connection = this.getConnection();
                 final PreparedStatement statement = connection.prepareStatement("SELECT * FROM `mapads_images` WHERE id = ?")) {
                statement.setString(1, id.toString());
                final ResultSet resultSet = statement.executeQuery();
                if (resultSet.next()) {
                    final byte[] imgdata = Base64.getDecoder().decode(resultSet.getString("imgdata").getBytes(StandardCharsets.UTF_8));
                    final StoredMapImage storedMapImage = new StoredMapImage(id,
                            resultSet.getByte("width"),
                            resultSet.getByte("height"),
                            imgdata);
                    future.complete(storedMapImage.decompress());
                } else {
                    future.complete(null);
                }
                resultSet.close();
            } catch (final Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    @Override
    public CompletableFuture<Void> deleteMapImages(final UUID... ids) {
        final CompletableFuture<Void> future = new CompletableFuture<>();
        this.run(() -> {
            final String arrStr = Arrays.stream(ids)
                    .map(UUID::toString)
                    .map(s -> "'" + s + "'")
                    .collect(Collectors.joining(","));
            try (final Connection connection = this.getConnection();
                 final PreparedStatement statement = connection.prepareStatement("DELETE FROM `mapads_images` WHERE id IN (" + arrStr + ")")) {
                statement.executeUpdate();
                future.complete(null);
            } catch (final SQLException e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    protected String makeImageInsertQuery() {
        return "INSERT INTO `mapads_images` (id, width, height, imgdata) " +
                "VALUES(?, ?, ?, ?) ON DUPLICATE KEY UPDATE width = ?, height = ?, imgdata = ?";
    }

    protected abstract Connection getConnection() throws SQLException;

    protected abstract void run(Runnable runnable);

}
