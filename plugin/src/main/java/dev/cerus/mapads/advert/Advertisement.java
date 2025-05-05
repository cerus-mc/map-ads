package dev.cerus.mapads.advert;

import dev.cerus.mapads.util.Either;
import java.util.UUID;

public class Advertisement {

    private final UUID advertId;
    private final UUID playerUuid;
    private final UUID imageId;
    private final Either<String, String> screenOrGroupId;
    private final long purchaseTimestamp;
    private final int purchasedMinutes;
    private final double pricePaid;
    private boolean reviewed;
    private int remainingMinutes;
    private boolean deleted;

    public Advertisement(final UUID advertId,
                         final UUID playerUuid,
                         final UUID imageId,
                         final Either<String, String> screenOrGroupId,
                         final long purchaseTimestamp,
                         final int purchasedMinutes,
                         final int remainingMinutes,
                         final double pricePaid,
                         final boolean reviewed) {
        this.advertId = advertId;
        this.playerUuid = playerUuid;
        this.imageId = imageId;
        this.screenOrGroupId = screenOrGroupId;
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

    public Either<String, String> getScreenOrGroupId() {
        return this.screenOrGroupId;
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

    public boolean isDeleted() {
        return this.deleted;
    }

    public void setDeleted(final boolean deleted) {
        this.deleted = deleted;
    }

    @Override
    public String toString() {
        return "Advertisement{" +
               "advertId=" + advertId +
               ", playerUuid=" + playerUuid +
               ", imageId=" + imageId +
               ", screenOrGroupId=" + screenOrGroupId +
               '}';
    }
}
