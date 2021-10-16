package dev.cerus.mapads.advert.storage;

import dev.cerus.mapads.advert.Advertisement;
import dev.cerus.mapads.image.storage.ImageStorage;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public abstract class SqlAdvertStorage implements AdvertStorage {

    protected final List<Advertisement> advertisements = new ArrayList<>();
    protected final List<Advertisement> pendingAdvertisements = new ArrayList<>();
    protected final Map<String, List<Advertisement>> advertisementMap = new HashMap<>();
    protected final Map<String, Integer> indexMap = new HashMap<>();
    private final ImageStorage imageStorage;

    protected SqlAdvertStorage(final ImageStorage imageStorage) {
        this.imageStorage = imageStorage;
    }

    protected void initTable() {
        this.run(() -> {
            try (final Connection connection = this.getConnection();
                 final PreparedStatement statement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS `mapads_ads` " +
                         "(id VARCHAR(64) PRIMARY KEY, player_uuid VARCHAR(64), img_id VARCHAR(64), ad_screen_id VARCHAR(64), purchase_timestamp BIGINT, " +
                         "purchased_minutes INT, remaining_minutes INT, price DOUBLE, reviewed INT)")) {
                statement.executeUpdate();
            } catch (final SQLException e) {
                e.printStackTrace();
            }
        });
    }

    protected void loadAllAds() {
        this.run(() -> {
            try (final Connection connection = this.getConnection();
                 final PreparedStatement statement = connection.prepareStatement("SELECT * FROM `mapads_ads` ORDER BY purchase_timestamp ASC")) {
                final ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    final Advertisement advertisement = new Advertisement(
                            UUID.fromString(resultSet.getString("id")),
                            UUID.fromString(resultSet.getString("player_uuid")),
                            UUID.fromString(resultSet.getString("img_id")),
                            resultSet.getString("ad_screen_id"),
                            resultSet.getLong("purchase_timestamp"),
                            resultSet.getInt("purchased_minutes"),
                            resultSet.getInt("remaining_minutes"),
                            resultSet.getDouble("price"),
                            resultSet.getInt("reviewed") == 1
                    );

                    if (advertisement.isReviewed()) {
                        this.advertisements.add(advertisement);
                        final List<Advertisement> list = this.advertisementMap.getOrDefault(advertisement.getAdScreenId(), new ArrayList<>());
                        list.add(advertisement);
                        this.advertisementMap.put(advertisement.getAdScreenId(), list);
                    } else {
                        this.pendingAdvertisements.add(advertisement);
                    }
                }
                resultSet.close();
            } catch (final SQLException e) {
                e.printStackTrace();
            }
            this.deleteExpiredAds();
        });
    }

    @Override
    public void nextAdvert(final String screenId) {
        this.deleteExpiredAds();
        this.indexMap.put(screenId, this.indexMap.getOrDefault(screenId, 0) + 1);
        if (this.indexMap.get(screenId) >= this.advertisementMap.computeIfAbsent(screenId, o -> new ArrayList<>()).size()) {
            this.indexMap.put(screenId, 0);
        }
    }

    @Override
    public int getIndex(final String screenId) {
        return this.indexMap.computeIfAbsent(screenId, uuid -> 0);
    }

    @Override
    public Advertisement getCurrentAdvert(final String screenId) {
        final List<Advertisement> advertisements = this.advertisementMap.computeIfAbsent(screenId, uuid -> new ArrayList<>());
        int index = this.getIndex(screenId);
        if (index >= advertisements.size() && !advertisements.isEmpty()) {
            this.nextAdvert(screenId);
            index = this.getIndex(screenId);
        }
        return advertisements.isEmpty() ? null : advertisements.get(index);
    }

    @Override
    public CompletableFuture<Void> deleteAdverts(final UUID... ids) {
        final CompletableFuture<Void> future = new CompletableFuture<>();
        this.run(() -> {
            Arrays.stream(ids)
                    .map(uuid -> this.advertisements.stream()
                            .filter(advertisement -> advertisement.getAdvertId().equals(uuid))
                            .findAny()
                            .orElse(this.pendingAdvertisements.stream()
                                    .filter(advertisement -> advertisement.getAdvertId().equals(uuid))
                                    .findAny()
                                    .orElse(null)))
                    .filter(Objects::nonNull)
                    .forEach(advertisement -> {
                        this.pendingAdvertisements.remove(advertisement);
                        this.advertisements.remove(advertisement);
                        for (final String key : this.advertisementMap.keySet()) {
                            this.advertisementMap.get(key).remove(advertisement);
                        }
                    });

            final String arrStr = Arrays.stream(ids)
                    .map(UUID::toString)
                    .map(s -> "'" + s + "'")
                    .collect(Collectors.joining(","));
            try (final Connection connection = this.getConnection();
                 final PreparedStatement statement = connection.prepareStatement("DELETE FROM `mapads_ads` WHERE id IN (" + arrStr + ")")) {
                statement.executeUpdate();
                future.complete(null);
            } catch (final SQLException e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    @Override
    public CompletableFuture<Void> updateAdvert(final Advertisement advertisement) {
        final CompletableFuture<Void> future = new CompletableFuture<>();
        this.run(() -> {
            try (final Connection connection = this.getConnection();
                 final PreparedStatement statement = connection.prepareStatement(this.makeAdvertInsertQuery())) {
                statement.setString(1, advertisement.getAdvertId().toString());
                statement.setString(2, advertisement.getPlayerUuid().toString());
                statement.setString(3, advertisement.getImageId().toString());
                statement.setString(4, advertisement.getAdScreenId().toString());
                statement.setLong(5, advertisement.getPurchaseTimestamp());
                statement.setInt(6, advertisement.getPurchasedMinutes());
                statement.setInt(7, advertisement.getRemainingMinutes());
                statement.setDouble(8, advertisement.getPricePaid());
                statement.setInt(9, advertisement.isReviewed() ? 1 : 0);
                statement.setString(10, advertisement.getPlayerUuid().toString());
                statement.setString(11, advertisement.getImageId().toString());
                statement.setString(12, advertisement.getAdScreenId().toString());
                statement.setLong(13, advertisement.getPurchaseTimestamp());
                statement.setInt(14, advertisement.getPurchasedMinutes());
                statement.setInt(15, advertisement.getRemainingMinutes());
                statement.setDouble(16, advertisement.getPricePaid());
                statement.setInt(17, advertisement.isReviewed() ? 1 : 0);
                statement.executeUpdate();
                future.complete(null);

                if (!this.advertisements.contains(advertisement) && advertisement.isReviewed()) {
                    this.pendingAdvertisements.remove(advertisement);
                    this.advertisements.add(advertisement);
                    final List<Advertisement> list = this.advertisementMap.getOrDefault(advertisement.getAdScreenId(), new ArrayList<>());
                    list.add(advertisement);
                    this.advertisementMap.put(advertisement.getAdScreenId(), list);
                    if (!this.indexMap.containsKey(advertisement.getAdScreenId())) {
                        this.indexMap.put(advertisement.getAdScreenId(), 0);
                    }
                } else if (!this.pendingAdvertisements.contains(advertisement) && !advertisement.isReviewed()) {
                    this.pendingAdvertisements.add(advertisement);
                }
            } catch (final SQLException e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    protected String makeAdvertInsertQuery() {
        return "INSERT INTO `mapads_ads` (id, player_uuid, img_id, ad_screen_id, purchase_timestamp, purchased_minutes, remaining_minutes, price, reviewed) " +
                "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE player_uuid = ?, img_id = ?, ad_screen_id = ?, " +
                "purchase_timestamp = ?, purchased_minutes = ?, remaining_minutes = ?, price = ?, reviewed = ?";
    }

    protected abstract Connection getConnection() throws SQLException;

    protected abstract void run(Runnable runnable);

    protected void deleteExpiredAds() {
        final Set<Advertisement> toDelete = this.advertisements.stream()
                .filter(advertisement -> advertisement.getRemainingMinutes() <= 0)
                .collect(Collectors.toSet());
        if (!toDelete.isEmpty()) {
            this.advertisements.removeAll(toDelete);
            for (final String uuid : this.advertisementMap.keySet()) {
                final List<Advertisement> advertisements = this.advertisementMap.get(uuid);
                advertisements.removeAll(toDelete);
            }

            this.deleteAdverts(toDelete.stream()
                    .map(Advertisement::getAdvertId)
                    .toArray(UUID[]::new));
            this.imageStorage.deleteMapImages(toDelete.stream()
                    .map(Advertisement::getImageId)
                    .toArray(UUID[]::new));
        }
    }

    @Override
    public List<Advertisement> getPendingAdvertisements() {
        return this.pendingAdvertisements;
    }

}
