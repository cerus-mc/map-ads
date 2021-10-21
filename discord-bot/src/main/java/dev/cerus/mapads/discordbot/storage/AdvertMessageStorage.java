package dev.cerus.mapads.discordbot.storage;

import java.util.UUID;

public interface AdvertMessageStorage extends AutoCloseable {

    void loadAll();

    UUID get(long messageId);

    Long get(UUID adId);

    Long getChannel(long messageId);

    void update(long messageId, long channelId, UUID adId);

    void delete(final long messageId);

    void delete(final UUID adId);

}
