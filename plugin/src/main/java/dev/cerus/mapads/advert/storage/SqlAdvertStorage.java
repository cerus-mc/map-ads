package dev.cerus.mapads.advert.storage;

import dev.cerus.mapads.advert.Advertisement;
import dev.cerus.mapads.image.storage.ImageStorage;
import dev.cerus.mapads.image.transition.recorded.storage.RecordedTransitionStorage;
import dev.cerus.mapads.screen.ScreenGroup;
import dev.cerus.mapads.screen.storage.AdScreenStorage;
import dev.cerus.mapads.util.Either;
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
    private final AdScreenStorage adScreenStorage;
    private final ImageStorage imageStorage;
    private final RecordedTransitionStorage recordedTransitionStorage;

    protected SqlAdvertStorage(final AdScreenStorage adScreenStorage,
                               final ImageStorage imageStorage,
                               final RecordedTransitionStorage recordedTransitionStorage) {
        this.adScreenStorage = adScreenStorage;
        this.imageStorage = imageStorage;
        this.recordedTransitionStorage = recordedTransitionStorage;
    }

    protected void initTable() {
        this.run(() -> {
            try (final Connection connection = this.getConnection();
                 final PreparedStatement statement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS `mapads_ads` " +
                         "(id VARCHAR(64) PRIMARY KEY, player_uuid VARCHAR(64), img_id VARCHAR(64), ad_screen_id VARCHAR(64), purchase_timestamp BIGINT, " +
                         "purchased_minutes INT, remaining_minutes INT, price DOUBLE, reviewed INT, screen_group_id VARCHAR(64))")) {
                statement.executeUpdate();

                // There's no ADD COLUMN IF NOT EXISTS, so we have to do this. Sucks but what are you gonna do?
                try {
                    connection.prepareStatement("ALTER TABLE `mapads_ads` ADD COLUMN screen_group_id VARCHAR(64)").executeUpdate();
                } catch (final SQLException ignored) {
                }
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
                            new Either<>(
                                    resultSet.getString("ad_screen_id"),
                                    resultSet.getString("screen_group_id")
                            ),
                            resultSet.getLong("purchase_timestamp"),
                            resultSet.getInt("purchased_minutes"),
                            resultSet.getInt("remaining_minutes"),
                            resultSet.getDouble("price"),
                            resultSet.getInt("reviewed") == 1
                    );

                    if (advertisement.isReviewed()) {
                        this.advertisements.add(advertisement);
                        advertisement.getScreenOrGroupId().get(screenId -> {
                            final List<Advertisement> list = this.advertisementMap.computeIfAbsent(screenId, $ -> new ArrayList<>());
                            list.add(advertisement);
                        }, groupId -> {
                            final ScreenGroup group = this.adScreenStorage.getScreenGroup(groupId);
                            for (final String screenId : group.screenIds()) {
                                final List<Advertisement> list = this.advertisementMap.computeIfAbsent(screenId, $ -> new ArrayList<>());
                                list.add(advertisement);
                            }
                        });
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
                        advertisement.setDeleted(true);
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
        if (advertisement.isDeleted()) {
            return CompletableFuture.completedFuture(null);
        }

        final CompletableFuture<Void> future = new CompletableFuture<>();
        this.run(() -> {
            try (final Connection connection = this.getConnection();
                 final PreparedStatement statement = connection.prepareStatement(this.makeAdvertInsertQuery())) {
                statement.setString(1, advertisement.getAdvertId().toString());
                statement.setString(2, advertisement.getPlayerUuid().toString());
                statement.setString(3, advertisement.getImageId().toString());
                statement.setString(4, advertisement.getScreenOrGroupId().map(s -> s, s -> null)); // Screen id or null
                statement.setLong(5, advertisement.getPurchaseTimestamp());
                statement.setInt(6, advertisement.getPurchasedMinutes());
                statement.setInt(7, advertisement.getRemainingMinutes());
                statement.setDouble(8, advertisement.getPricePaid());
                statement.setInt(9, advertisement.isReviewed() ? 1 : 0);
                statement.setString(10, advertisement.getScreenOrGroupId().map(s -> null, s -> s)); // Group id or null
                statement.setString(11, advertisement.getPlayerUuid().toString());
                statement.setString(12, advertisement.getImageId().toString());
                statement.setString(13, advertisement.getScreenOrGroupId().map(s -> s, s -> null)); // Screen id or null
                statement.setLong(14, advertisement.getPurchaseTimestamp());
                statement.setInt(15, advertisement.getPurchasedMinutes());
                statement.setInt(16, advertisement.getRemainingMinutes());
                statement.setDouble(17, advertisement.getPricePaid());
                statement.setInt(18, advertisement.isReviewed() ? 1 : 0);
                statement.setString(19, advertisement.getScreenOrGroupId().map(s -> null, s -> s)); // Group id or null
                statement.executeUpdate();
                future.complete(null);

                if (!this.advertisements.contains(advertisement) && advertisement.isReviewed()) {
                    this.pendingAdvertisements.remove(advertisement);
                    this.advertisements.add(advertisement);
                    advertisement.getScreenOrGroupId().get(screenId -> {
                        final List<Advertisement> list = this.advertisementMap.computeIfAbsent(screenId, $ -> new ArrayList<>());
                        list.add(advertisement);
                        if (!this.indexMap.containsKey(screenId)) {
                            this.indexMap.put(screenId, 0);
                        }
                    }, groupId -> {
                        for (final String screenId : this.adScreenStorage.getScreenGroup(groupId).screenIds()) {
                            final List<Advertisement> list = this.advertisementMap.computeIfAbsent(screenId, $ -> new ArrayList<>());
                            list.add(advertisement);
                            if (!this.indexMap.containsKey(screenId)) {
                                this.indexMap.put(screenId, 0);
                            }
                        }
                    });
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
        return "INSERT INTO `mapads_ads` (id, player_uuid, img_id, ad_screen_id, purchase_timestamp, purchased_minutes, remaining_minutes, price, reviewed, screen_group_id) " +
                "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE player_uuid = ?, img_id = ?, ad_screen_id = ?, " +
                "purchase_timestamp = ?, purchased_minutes = ?, remaining_minutes = ?, price = ?, reviewed = ?, screen_group_id = ?";
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
            toDelete.forEach(advertisement -> this.recordedTransitionStorage.deleteAll(
                    advertisement.getScreenOrGroupId().unsafeGet(),
                    advertisement.getImageId()
            ));
        }
    }

    @Override
    public Advertisement getAdvert(final UUID id) {
        return this.advertisements.stream()
                .filter(advertisement -> advertisement.getAdvertId().equals(id))
                .findAny()
                .orElse(null);
    }

    @Override
    public List<Advertisement> getPendingAdvertisements() {
        return this.pendingAdvertisements;
    }

    @Override
    public List<Advertisement> getAdvertisements(final String screenName) {
        return this.advertisements.stream()
                .filter(advertisement -> advertisement.getRemainingMinutes() > 0)
                .filter(advertisement -> advertisement.getScreenOrGroupId().map(
                        screenId -> screenId.equals(screenName),
                        groupId -> this.adScreenStorage.getScreenGroup(groupId)
                                .screenIds().contains(screenName))
                )
                .collect(Collectors.toList());
    }

    @Override
    public List<Advertisement> getAdvertisements(final UUID owner) {
        final List<Advertisement> list = new ArrayList<>();
        this.advertisements.stream()
                .filter(advertisement -> advertisement.getPlayerUuid().equals(owner))
                .filter(advertisement -> !advertisement.isDeleted())
                .forEach(list::add);
        this.pendingAdvertisements.stream()
                .filter(advertisement -> advertisement.getPlayerUuid().equals(owner))
                .filter(advertisement -> !advertisement.isDeleted())
                .forEach(list::add);
        return list;
    }

}
