package dev.cerus.mapads.api.event;

import dev.cerus.mapads.advert.Advertisement;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

public class AdvertCreateEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final Advertisement advertisement;
    private boolean cancelled;

    public AdvertCreateEvent(@NotNull final Player who, final Advertisement advertisement) {
        super(who);
        this.advertisement = advertisement;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public Advertisement getAdvertisement() {
        return this.advertisement;
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public void setCancelled(final boolean cancel) {
        this.cancelled = cancel;
    }

}
