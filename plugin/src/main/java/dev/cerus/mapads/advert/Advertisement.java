package dev.cerus.mapads.advert;

import java.util.UUID;

public class Advertisement {

    private final UUID advertId;
    private final UUID playerUuid;
    private final UUID imageId;
    private final String adScreenId;
    private final long purchaseTimestamp;
    private final int purchasedMinutes;
    private final double pricePaid;
    private boolean reviewed;
    private int remainingMinutes;

    public Advertisement(final UUID advertId,
                         final UUID playerUuid,
                         final UUID imageId,
                         final String adScreenId,
                         final long purchaseTimestamp,
                         final int purchasedMinutes,
                         final int remainingMinutes,
                         final double pricePaid,
                         final boolean reviewed) {
        this.advertId = advertId;
        this.playerUuid = playerUuid;
        this.imageId = imageId;
        this.adScreenId = adScreenId;
        this.purchaseTimestamp = purchaseTimestamp;
        this.purchasedMinutes = purchasedMinutes;
        this.remainingMinutes = remainingMinutes;
        this.pricePaid = pricePaid;
        this.reviewed = reviewed;
    }

    public UUID getAdvertId() {
        return this.advertId;
    }

    public UUID getPlayerUuid() {
        return this.playerUuid;
    }

    public UUID getImageId() {
        return this.imageId;
    }

    public String getAdScreenId() {
        return this.adScreenId;
    }

    public long getPurchaseTimestamp() {
        return this.purchaseTimestamp;
    }

    public double getPricePaid() {
        return this.pricePaid;
    }

    public int getPurchasedMinutes() {
        return this.purchasedMinutes;
    }

    public int getRemainingMinutes() {
        return this.remainingMinutes;
    }

    public void setRemainingMinutes(final int remainingMinutes) {
        this.remainingMinutes = remainingMinutes;
    }

    public boolean isReviewed() {
        return this.reviewed;
    }

    public void setReviewed(final boolean reviewed) {
        this.reviewed = reviewed;
    }

}
