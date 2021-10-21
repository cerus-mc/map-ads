package dev.cerus.mapads.discordbot.storage;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SQLiteAdvertMessageStorage implements AdvertMessageStorage {

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Map<Long, Long> messageToChannelMap = new HashMap<>();
    private final Map<Long, UUID> messageToAdMap = new HashMap<>();
    private final Map<UUID, Long> adToMessageMap = new HashMap<>();
    private final String path;

    public SQLiteAdvertMessageStorage(final String path) {
        this.path = path;
    }

    @Override
    public void loadAll() {
        this.executorService.execute(() -> {
            try (final Connection connection = this.getCon();
                 final PreparedStatement statement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS `mapads_discord_messages` " +
                         "(msg_id BIGINT PRIMARY KEY, ad_id VARCHAR(64), channel_id BIGINT)")) {
                statement.executeUpdate();
            } catch (final SQLException e) {
                e.printStackTrace();
            }
            try (final Connection connection = this.getCon();
                 final PreparedStatement statement = connection.prepareStatement("SELECT * FROM `mapads_discord_messages`");
                 final ResultSet set = statement.executeQuery()) {
                while (set.next()) {
                    final long msgId = set.getLong("msg_id");
                    final UUID adId = UUID.fromString(set.getString("ad_id"));
                    this.messageToChannelMap.put(msgId, set.getLong("channel_id"));
                    this.messageToAdMap.put(msgId, adId);
                    this.adToMessageMap.put(adId, msgId);
                }
            } catch (final SQLException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public UUID get(final long messageId) {
        return this.messageToAdMap.get(messageId);
    }

    @Override
    public Long get(final UUID adId) {
        return this.adToMessageMap.get(adId);
    }

    @Override
    public Long getChannel(final long messageId) {
        return this.messageToChannelMap.get(messageId);
    }

    @Override
    public void update(final long messageId, final long channelId, final UUID adId) {
        this.messageToAdMap.put(messageId, adId);
        this.adToMessageMap.put(adId, messageId);
        this.messageToChannelMap.put(messageId, channelId);
        this.executorService.execute(() -> {
            try (final Connection connection = this.getCon();
                 final PreparedStatement statement = connection.prepareStatement("INSERT INTO `mapads_discord_messages` (msg_id, ad_id, channel_id) " +
                         "VALUES (?, ?, ?) ON CONFLICT(msg_id) DO UPDATE SET ad_id = ?")) {
                statement.setLong(1, messageId);
                statement.setString(2, adId.toString());
                statement.setLong(3, channelId);
                statement.setString(4, adId.toString());
                statement.executeUpdate();
            } catch (final SQLException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void delete(final long messageId) {
        this.adToMessageMap.remove(this.messageToAdMap.remove(messageId));
        this.messageToChannelMap.remove(messageId);
        this.executorService.execute(() -> {
            try (final Connection connection = this.getCon();
                 final PreparedStatement statement = connection.prepareStatement("DELETE FROM `mapads_discord_messages` WHERE msg_id = ?")) {
                statement.setLong(1, messageId);
                statement.executeUpdate();
            } catch (final SQLException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void delete(final UUID adId) {
        if (this.adToMessageMap.containsKey(adId)) {
            this.delete(this.adToMessageMap.get(adId));
        }
    }

    private Connection getCon() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + this.path);
    }

    @Override
    public void close() throws Exception {
        this.executorService.shutdown();
    }

}
