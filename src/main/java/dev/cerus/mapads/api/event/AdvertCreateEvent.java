package dev.cerus.mapads.api.event;

import dev.cerus.mapads.advert.Advertisement;
import dev.cerus.mapads.image.MapImage;
import java.awt.image.BufferedImage;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

public class AdvertCreateEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final Advertisement advertisement;
    private final String imageUrl;
    private final MapImage convertedImage;
    private final BufferedImage originalImage;
    private boolean cancelled;

    public AdvertCreateEvent(@NotNull final Player who,
                             final Advertisement advertisement,
                             final String imageUrl,
                             final MapImage convertedImage,
                             final BufferedImage originalImage) {
        super(who);
        this.advertisement = advertisement;
        this.imageUrl = imageUrl;
        this.convertedImage = convertedImage;
        this.originalImage = originalImage;
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

    public String getImageUrl() {
        return this.imageUrl;
    }

    public MapImage getConvertedImage() {
        return this.convertedImage;
    }

    public BufferedImage getOriginalImage() {
        return this.originalImage;
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
