package dev.cerus.mapads.image.transition.recorded.storage;

import com.zaxxer.hikari.HikariDataSource;
import dev.cerus.mapads.image.transition.recorded.BinaryRecordedTransition;
import dev.cerus.mapads.image.transition.recorded.RecordedTransition;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SqliteRecordedTransitionStorage implements RecordedTransitionStorage {

    private final Set<String> existingRecordings = new HashSet<>();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final HikariDataSource dataSource;

    public SqliteRecordedTransitionStorage(final HikariDataSource dataSource) {
        this.dataSource = dataSource;
        this.init();
    }

    public void init() {
        this.executorService.submit(() -> {
            try (final Connection connection = this.getConnection()) {
                PreparedStatement statement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS mapads_recordings (id VARCHAR(256) PRIMARY KEY, " +
                        "screen VARCHAR(64), transition VARCHAR(64), img_left VARCHAR(64), img_right VARCHAR(64), data MEDIUMTEXT, access BIGINT)");
                statement.executeUpdate();
                statement.close();

                statement = connection.prepareStatement("SELECT id FROM mapads_recordings");
                final ResultSet set = statement.executeQuery();
                while (set.next()) {
                    this.existingRecordings.add(set.getString("id"));
                }
                set.close();
                statement.close();
            } catch (final SQLException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public boolean has(final String screen, final String transitionId, final UUID imgId1, final UUID imgId2) {
        return this.existingRecordings.contains(this.id(screen, transitionId, imgId1, imgId2));
    }

    @Override
    public CompletableFuture<RecordedTransition> load(final String screen, final String transitionId, final UUID imgId1, final UUID imgId2) {
        final CompletableFuture<RecordedTransition> future = new CompletableFuture<>();
        this.executorService.submit(() -> {
            try (final Connection connection = this.getConnection()) {
                final PreparedStatement statement = connection.prepareStatement("SELECT * FROM mapads_recordings WHERE id = ?");
                statement.setString(1, this.id(screen, transitionId, imgId1, imgId2));
                final ResultSet resultSet = statement.executeQuery();
                if (resultSet.next()) {
                    future.complete(new BinaryRecordedTransition(
                            Base64.getDecoder().decode(resultSet.getString("data"))
                    ));
                } else {
                    future.complete(null);
                }
                resultSet.close();
                statement.close();
            } catch (final SQLException | IOException e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    @Override
    public CompletableFuture<Void> save(final String screen, final String transitionId, final UUID imgId1, final UUID imgId2, final byte[] data) {
        final CompletableFuture<Void> future = new CompletableFuture<>();
        this.executorService.submit(() -> {
            final String id = this.id(screen, transitionId, imgId1, imgId2);
            if (!this.existingRecordings.contains(id)) {
                this.existingRecordings.add(id);
            }

            try (final Connection connection = this.getConnection()) {
                final PreparedStatement statement = connection.prepareStatement("INSERT INTO mapads_recordings (id, screen, transition, img_left," +
                        " img_right, data, access) VALUES (?, ?, ?, ?, ?, ?, ?)");
                statement.setString(1, id);
                statement.setString(2, screen);
                statement.setString(3, transitionId);
                statement.setString(4, imgId1.toString());
                statement.setString(5, imgId2.toString());
                statement.setString(6, Base64.getEncoder().encodeToString(data));
                statement.setLong(7, System.currentTimeMillis());
                statement.executeUpdate();
                statement.close();
            } catch (final SQLException e) {
                future.completeExceptionally(e);
                return;
            }
            future.complete(null);
        });
        return future;
    }

    @Override
    public CompletableFuture<Void> resetLastAccess(final String screen, final String transitionId, final UUID imgId1, final UUID imgId2) {
        final CompletableFuture<Void> future = new CompletableFuture<>();
        this.executorService.submit(() -> {
            final String id = this.id(screen, transitionId, imgId1, imgId2);

            try (final Connection connection = this.getConnection()) {
                final PreparedStatement statement = connection.prepareStatement("UPDATE mapads_recordings SET access = ? WHERE id = ?");
                statement.setLong(1, System.currentTimeMillis());
                statement.setString(2, id);
                statement.executeUpdate();
                statement.close();
            } catch (final SQLException e) {
                future.completeExceptionally(e);
                return;
            }
            future.complete(null);
        });
        return future;
    }

    @Override
    public CompletableFuture<Void> deleteAll(final String screen, final String transitionId) {
        final CompletableFuture<Void> future = new CompletableFuture<>();
        this.executorService.submit(() -> {
            try (final Connection connection = this.getConnection()) {
                final PreparedStatement statement = connection.prepareStatement("DELETE FROM mapads_recordings WHERE screen = ? AND transition = ?");
                statement.setString(1, screen);
                statement.setString(2, transitionId);
                statement.executeUpdate();
                statement.close();
            } catch (final SQLException e) {
                future.completeExceptionally(e);
                return;
            }

            this.existingRecordings.removeIf(s -> s.startsWith(screen + "/" + transitionId + "/"));
            future.complete(null);
        });
        return future;
    }

    @Override
    public CompletableFuture<Void> deleteAll(final String screen) {
        final CompletableFuture<Void> future = new CompletableFuture<>();
        this.executorService.submit(() -> {
            try (final Connection connection = this.getConnection()) {
                final PreparedStatement statement = connection.prepareStatement("DELETE FROM mapads_recordings WHERE screen = ?");
                statement.setString(1, screen);
                statement.executeUpdate();
                statement.close();
            } catch (final SQLException e) {
                future.completeExceptionally(e);
                return;
            }

            this.existingRecordings.removeIf(s -> s.startsWith(screen + "/"));
            future.complete(null);
        });
        return future;
    }

    @Override
    public CompletableFuture<Void> deleteAll(final String screen, final UUID imageId) {
        final CompletableFuture<Void> future = new CompletableFuture<>();
        this.executorService.submit(() -> {
            try (final Connection connection = this.getConnection()) {
                final PreparedStatement statement = connection.prepareStatement("DELETE FROM mapads_recordings WHERE screen = ? AND (img_left = ? OR img_right = ?)");
                statement.setString(1, screen);
                statement.setString(2, imageId.toString());
                statement.setString(3, imageId.toString());
                statement.executeUpdate();
                statement.close();
            } catch (final SQLException e) {
                future.completeExceptionally(e);
                return;
            }

            this.existingRecordings.removeIf(s -> s.startsWith(screen + "/") && s.contains(imageId.toString()));
            future.complete(null);
        });
        return future;
    }

    @Override
    public CompletableFuture<Void> deleteOlderThan(final long timestamp) {
        final CompletableFuture<Void> future = new CompletableFuture<>();
        this.executorService.submit(() -> {
            try (final Connection connection = this.getConnection()) {
                PreparedStatement statement = connection.prepareStatement("SELECT id FROM mapads_recordings WHERE access <= ?");
                statement.setLong(1, timestamp);
                final ResultSet query = statement.executeQuery();
                while (query.next()) {
                    final String id = query.getString("id");
                    this.existingRecordings.remove(id);
                }
                statement.close();

                statement = connection.prepareStatement("DELETE FROM mapads_recordings WHERE access <= ?");
                statement.setLong(1, timestamp);
                statement.executeUpdate();
                statement.close();
            } catch (final SQLException e) {
                future.completeExceptionally(e);
                return;
            }
            future.complete(null);
        });
        return future;
    }

    private Connection getConnection() throws SQLException {
        return this.dataSource.getConnection();
    }

    private String id(final String screen, final String transition, final UUID img1, final UUID img2) {
        return screen + "/" + transition + "/" + img1 + "/" + img2;
    }

    @Override
    public void close() throws Exception {
        this.dataSource.close();
    }

}

