package dev.cerus.mapads.api.event;

import dev.cerus.mapads.advert.Advertisement;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class AdvertReviewEvent extends Event implements Cancellable {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final Advertisement advertisement;
    private final Result result;
    private final Player reviewer;
    private final boolean discord;
    private boolean cancelled;

    public AdvertReviewEvent(final Player reviewer, final Advertisement advertisement, final Result result) {
        this.reviewer = reviewer;
        this.advertisement = advertisement;
        this.result = result;
        this.discord = reviewer == null;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public boolean isDiscord() {
        return this.discord;
    }

    public Player getReviewer() {
        return this.reviewer;
    }

    public Advertisement getAdvertisement() {
        return this.advertisement;
    }

    public Result getResult() {
        return this.result;
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public void setCancelled(final boolean cancel) {
        this.cancelled = cancel;
    }

    public enum Result {
        ACCEPT, DENY
    }

}
