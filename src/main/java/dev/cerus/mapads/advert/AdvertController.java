package dev.cerus.mapads.advert;

import dev.cerus.mapads.MapAdsPlugin;
import dev.cerus.mapads.advert.storage.AdvertStorage;
import dev.cerus.mapads.image.MapImage;
import dev.cerus.mapads.image.storage.ImageStorage;
import dev.cerus.mapads.image.transition.Transition;
import dev.cerus.mapads.image.transition.TransitionRegistry;
import dev.cerus.mapads.screen.AdScreen;
import dev.cerus.mapads.util.ReviewerUtil;
import dev.cerus.maps.api.MapScreen;
import dev.cerus.maps.plugin.map.MapScreenRegistry;
import java.util.HashMap;
import java.util.Map;

public class AdvertController {

    private final Map<String, Context> contextMap = new HashMap<>();
    private final AdvertStorage advertStorage;
    private final ImageStorage imageStorage;
    private final MapAdsPlugin plugin;

    public AdvertController(final MapAdsPlugin plugin,
                            final AdvertStorage advertStorage,
                            final ImageStorage imageStorage) {
        this.plugin = plugin;
        this.advertStorage = advertStorage;
        this.imageStorage = imageStorage;
    }

    public void update(final AdScreen screen) {
        final MapScreen mapScreen = MapScreenRegistry.getScreen(screen.getScreenId());
        if (mapScreen == null) {
            return;
        }

        final Context context = this.contextMap.computeIfAbsent(screen.getId(), o -> new Context());

        // Get the default image if we either don't have an ad to
        // display or if we are explicitly asked to display the default image
        final MapImage image = context.displayDefaultImg || context.currentAdvertImage == null
                ? this.plugin.getDefaultImageSupplier().apply(mapScreen.getWidth() * 128, mapScreen.getHeight() * 128)
                : context.currentAdvertImage;
        if (image != null) {
            // Do the transition thingy
            final Transition transition = TransitionRegistry.getOrDefault(screen.getTransition());
            transition.makeTransition(mapScreen, context.prevImg, image);
            mapScreen.update(MapScreen.DirtyHandlingPolicy.IGNORE, ReviewerUtil.getNonReviewingPlayers(mapScreen));

            // If we're displaying an ad we need to decrement its amount of remaining minutes
            if (context.currentAdvert != null) {
                context.currentAdvert.setRemainingMinutes(context.currentAdvert.getRemainingMinutes() - 1);
                this.advertStorage.updateAdvert(context.currentAdvert);
            }
        }
        context.prevImg = image;

        // Poll the next ad if we did not display the default image
        if (!context.displayDefaultImg) {
            this.advertStorage.nextAdvert(screen.getId());
        }
        final Advertisement currentAdvert = this.advertStorage.getCurrentAdvert(screen.getId());
        if (currentAdvert == null) {
            // We don't have an ad, so we just set the default image
            context.currentAdvertImage = this.plugin.getDefaultImageSupplier()
                    .apply(mapScreen.getWidth() * 128, mapScreen.getHeight() * 128);
            context.currentAdvert = null;
            return;
        }
        if (context.displayDefaultImg) {
            // Don't display the default image again
            context.displayDefaultImg = false;
        } else if (this.advertStorage.getIndex(screen.getId()) == 0) {
            // Display the default image every time the cycle starts
            context.displayDefaultImg = true;
            context.currentAdvert = null;
            return;
        }
        context.currentAdvert = currentAdvert;

        // Fetch the image of the next ad
        // We do this at the end because we have about a minute of time for this operation
        this.imageStorage.getMapImage(currentAdvert.getImageId()).whenComplete((mapImage, throwable) -> {
            context.currentAdvertImage = mapImage;
        });
    }

    public static class Context {

        private MapImage prevImg;
        private Advertisement currentAdvert;
        private MapImage currentAdvertImage;
        private boolean displayDefaultImg;

    }

}
