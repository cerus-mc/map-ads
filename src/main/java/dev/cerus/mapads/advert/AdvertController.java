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
        System.out.println("[DEBUG] '" + screen.getId() + "' #" + screen.getScreenId() + " | Update start");

        final MapImage image = context.displayDefaultImg || context.currentAdvertImage == null
                ? this.plugin.getDefaultImageSupplier().apply(mapScreen.getWidth() * 128, mapScreen.getHeight() * 128)
                : context.currentAdvertImage;
        if (image != null) {
            System.out.println("[DEBUG] '" + screen.getId() + "' #" + screen.getScreenId() + " | Img draw start");
            final Transition transition = TransitionRegistry.getOrDefault(screen.getTransition());
            transition.makeTransition(mapScreen, context.prevImg, image);
            mapScreen.update(MapScreen.DirtyHandlingPolicy.IGNORE, ReviewerUtil.getNonReviewingPlayers(mapScreen));

            if (context.currentAdvert != null) {
                System.out.println("[DEBUG] '" + screen.getId() + "' #" + screen.getScreenId() + " | Advert update");
                context.currentAdvert.setRemainingMinutes(context.currentAdvert.getRemainingMinutes() - 1);
                this.advertStorage.updateAdvert(context.currentAdvert);
                System.out.println("[DEBUG] '" + screen.getId() + "' #" + screen.getScreenId() + " | Advert remaining = " + context.currentAdvert.getRemainingMinutes());
            }
            System.out.println("[DEBUG] '" + screen.getId() + "' #" + screen.getScreenId() + " | Img draw end");
        }
        context.prevImg = image;

        if (!context.displayDefaultImg) {
            System.out.println("[DEBUG] '" + screen.getId() + "' #" + screen.getScreenId() + " | Next advert");
            this.advertStorage.nextAdvert(screen.getId());
        }
        final Advertisement currentAdvert = this.advertStorage.getCurrentAdvert(screen.getId());
        if (currentAdvert == null) {
            System.out.println("[DEBUG] '" + screen.getId() + "' #" + screen.getScreenId() + " | Advert null, setting default img");
            context.currentAdvertImage = this.plugin.getDefaultImageSupplier()
                    .apply(mapScreen.getWidth() * 128, mapScreen.getHeight() * 128);
            context.currentAdvert = null;
            return;
        }
        if (context.displayDefaultImg) {
            System.out.println("[DEBUG] '" + screen.getId() + "' #" + screen.getScreenId() + " | displayDefaultImg = false");
            context.displayDefaultImg = false;
        } else if (this.advertStorage.getIndex(screen.getId()) == 0) {
            System.out.println("[DEBUG] '" + screen.getId() + "' #" + screen.getScreenId() + " | displayDefaultImg = true");
            context.displayDefaultImg = true;
            context.currentAdvert = null;
            return;
        }
        context.currentAdvert = currentAdvert;

        System.out.println("[DEBUG] '" + screen.getId() + "' #" + screen.getScreenId() + " | Polling next img");
        this.imageStorage.getMapImage(currentAdvert.getImageId()).whenComplete((mapImage, throwable) -> {
            System.out.println("[DEBUG] '" + screen.getId() + "' #" + screen.getScreenId() + " | Next img done (t null = " + (throwable == null) + ", img null = " + (mapImage == null) + ")");
            if (throwable != null) {
                System.err.println(throwable.getMessage());
                throwable.printStackTrace();
            }
            context.currentAdvertImage = mapImage;
        });

/*        final int index = this.advertStorage.getIndex();
        final MapImage image = this.displayDefaultImg ? this.emptyImage : this.currentAdvertImage;

        // Display
        this.adScreenStorage.getScreens().forEach(adScreen -> {
            final MapScreen mapScreen = MapScreenRegistry.getScreen(adScreen.getMapScreenId());
            if (mapScreen != null) {
                image.drawOnto(mapScreen);
                mapScreen.update(MapScreen.DirtyHandlingPolicy.IGNORE);
            }
        });

        if (!this.displayDefaultImg) {
            final Advertisement currentAdvert = this.advertStorage.getCurrentAdvert();
            currentAdvert.setRemainingMinutes(currentAdvert.getRemainingMinutes() - 1);
            this.advertStorage.updateAdvert(currentAdvert);

            this.advertStorage.nextAdvert();
            if (index != 0 && this.advertStorage.getIndex() == 0) {
                this.displayDefaultImg = true;
            }
            this.imageStorage.getMapImage(this.advertStorage.getCurrentAdvert().getImageId())
                    .whenComplete((mapImage, throwable) -> {
                        this.currentAdvertImage = mapImage;
                    });
        } else {
            this.displayDefaultImg = false;
        }*/
    }

    public static class Context {

        private MapImage prevImg;
        private Advertisement currentAdvert;
        private MapImage currentAdvertImage;
        private boolean displayDefaultImg;

    }

}
