package dev.cerus.mapads.advert;

import dev.cerus.mapads.MapAdsPlugin;
import dev.cerus.mapads.advert.storage.AdvertStorage;
import dev.cerus.mapads.image.DefaultImageController;
import dev.cerus.mapads.image.MapImage;
import dev.cerus.mapads.image.storage.ImageStorage;
import dev.cerus.mapads.image.transition.Transition;
import dev.cerus.mapads.image.transition.TransitionRegistry;
import dev.cerus.mapads.image.transition.recorded.RecordedTransition;
import dev.cerus.mapads.image.transition.recorded.RecordedTransitions;
import dev.cerus.mapads.image.transition.recorded.storage.RecordedTransitionStorage;
import dev.cerus.mapads.image.transition.recorder.TransitionRecorder;
import dev.cerus.mapads.premium.Premium;
import dev.cerus.mapads.screen.AdScreen;
import dev.cerus.mapads.screen.storage.AdScreenStorage;
import dev.cerus.mapads.util.ReviewerUtil;
import dev.cerus.maps.api.MapScreen;
import dev.cerus.maps.api.graphics.FastMapScreenGraphics;
import dev.cerus.maps.plugin.map.MapScreenRegistry;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class AdvertController {

    private final Map<String, Context> contextMap = new HashMap<>();
    private final AdvertStorage advertStorage;
    private final ImageStorage imageStorage;
    private final DefaultImageController defaultImageController;
    private final AdScreenStorage adScreenStorage;
    private final MapAdsPlugin plugin;
    private final RecordedTransitionStorage recordedTransitionStorage;

    public AdvertController(final MapAdsPlugin plugin,
                            final AdvertStorage advertStorage,
                            final ImageStorage imageStorage,
                            final DefaultImageController defaultImageController,
                            final AdScreenStorage adScreenStorage,
                            final RecordedTransitionStorage recordedTransitionStorage) {
        this.plugin = plugin;
        this.advertStorage = advertStorage;
        this.imageStorage = imageStorage;
        this.defaultImageController = defaultImageController;
        this.adScreenStorage = adScreenStorage;
        this.recordedTransitionStorage = recordedTransitionStorage;
    }

    public void update(final AdScreen screen) {
        final MapScreen mapScreen = MapScreenRegistry.getScreen(screen.getScreenId());
        if (mapScreen == null) {
            return;
        }
        if (!(mapScreen.getGraphics() instanceof FastMapScreenGraphics)) {
            mapScreen.useFastGraphics(true);
        }

        final Context context = this.contextMap.computeIfAbsent(screen.getId(), o -> {
            final Context ctx = new Context();
            ctx.stay = 1; // TODO
            return ctx;
        });

        // Get the default image if we either don't have an ad to
        // display or if we are explicitly asked to display the default image
        final MapImage image = context.displayDefaultImg || context.currentAdvertImage == null
                ? this.defaultImageController.getDefaultImage(screen)
                : context.currentAdvertImage;

        if (image != null) {
            final String screenOrGroupId = context.currentAdvert == null ? screen.getId() : context.currentAdvert.getScreenOrGroupId().unsafeGet();
            // Check if a transition recording exists
            if (screenOrGroupId != null && context.prevImg != null
                    && this.recordedTransitionStorage.has(screenOrGroupId, screen.getTransition(), context.prevImg.getId(), image.getId())) {
                // Load the recoding
                this.recordedTransitionStorage.load(screenOrGroupId, screen.getTransition(), context.prevImg.getId(), image.getId())
                        .whenComplete((recordedTransition, throwable) -> {
                            if (throwable != null) {
                                this.plugin.getLogger().log(Level.SEVERE, "Could not play recorded transition (" + screen.getId()
                                        + "<" + screen.getTransition() + ">, " + context.prevImg.getId() + " -> " + image.getId() + ")", throwable);
                                return;
                            }
                            if (recordedTransition == null) {
                                return;
                            }

                            // Play the recording
                            RecordedTransitions.playTransition(MapScreenRegistry.getScreen(screen.getScreenId()), recordedTransition);
                            this.recordedTransitionStorage.resetLastAccess(screenOrGroupId, screen.getTransition(), context.prevImg.getId(), image.getId());
                        });
            } else {
                // Init recorder
                final TransitionRecorder recorder;
                if (this.plugin.getConfigModel().enableTransitionRecording && Premium.isPremium() && screenOrGroupId != null
                        && context.prevImg != null && (context.currentAdvert == null || !context.currentAdvert.isDeleted())) {
                    final String transitionId = screen.getTransition();
                    final UUID imgLeft = context.prevImg.getId();
                    final UUID imgRight = image.getId();
                    recorder = TransitionRecorder.callback(TransitionRecorder.binary(), rec -> {
                        if (!rec.wasStarted()) {
                            // Don't save non-existing recordings
                            return;
                        }
                        this.recordedTransitionStorage.save(
                                screenOrGroupId,
                                transitionId,
                                imgLeft,
                                imgRight,
                                rec.getData()
                        ).whenComplete((unused, throwable) -> {
                            if (throwable != null) {
                                this.plugin.getLogger().log(Level.SEVERE, "Could not save transition recording (" + screen.getId()
                                        + "<" + screen.getTransition() + ">, " + context.prevImg.getId() + " -> " + image.getId() + ")", throwable);
                            }
                        });
                    });
                } else {
                    recorder = TransitionRecorder.noop();
                }

                // Do the transition thingy
                final Transition transition = TransitionRegistry.getOrDefault(screen.getTransition());
                transition.makeTransition(mapScreen, context.prevImg, image, recorder);
                mapScreen.sendMaps(false, ReviewerUtil.getNonReviewingPlayers(mapScreen));
            }

            // If we're displaying an ad we need to decrement its amount of remaining minutes
            if (context.currentAdvert != null) {
                final boolean shouldRemoveMinute = context.currentAdvert.getScreenOrGroupId().map(
                        screenId -> true,
                        groupId -> this.plugin.getConfigModel().deductEachScreenInGroup
                                || this.adScreenStorage.getScreenGroup(groupId).screenIds().indexOf(screen.getId()) == 0
                );
                if (shouldRemoveMinute) {
                    context.currentAdvert.setRemainingMinutes(context.currentAdvert.getRemainingMinutes() - 1);
                    this.advertStorage.updateAdvert(context.currentAdvert);
                }
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
            context.currentAdvertImage = this.defaultImageController.getDefaultImage(screen);
            context.currentAdvert = null;
            return;
        }
        if (context.displayDefaultImg) {
            // Don't display the default image again
            context.displayDefaultImg = false;
        } else if (this.advertStorage.getIndex(screen.getId()) == 0 && !screen.isNoDefaultImage()) {
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
        private RecordedTransition recordedTransition;
        private int stay;

    }

}
