package dev.cerus.mapads.discordbot;

import java.util.UUID;

public class AdvertContext {

    private final UUID adId;
    private final String imageUrl;
    private final UUID playerId;
    private final String playerName;
    private final double price;
    private final long createdAt;
    private final int minutes;
    private final String screen;

    public AdvertContext(final UUID adId,
                         final String imageUrl,
                         final UUID playerId,
                         final String playerName,
                         final double price,
                         final long createdAt,
                         final int minutes,
                         final String screen) {
        this.adId = adId;
        this.imageUrl = imageUrl;
        this.playerId = playerId;
        this.playerName = playerName;
        this.price = price;
        this.createdAt = createdAt;
        this.minutes = minutes;
        this.screen = screen;
    }

    public UUID getAdId() {
        return this.adId;
    }

    public String getImageUrl() {
        return this.imageUrl;
    }

    public UUID getPlayerId() {
        return this.playerId;
    }

    public String getPlayerName() {
        return this.playerName;
    }

    public double getPrice() {
        return this.price;
    }

    public long getCreatedAt() {
        return this.createdAt;
    }

    public int getMinutes() {
        return this.minutes;
    }

    public String getScreen() {
        return this.screen;
    }

}
