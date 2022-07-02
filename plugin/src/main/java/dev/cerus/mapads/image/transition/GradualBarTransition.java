package dev.cerus.mapads.image.transition;

import dev.cerus.mapads.image.MapImage;
import dev.cerus.mapads.image.transition.recorder.TransitionRecorder;
import dev.cerus.mapads.scheduler.ExecutorServiceScheduler;
import dev.cerus.mapads.scheduler.Scheduler;
import dev.cerus.mapads.scheduler.SchedulerRunnable;
import dev.cerus.mapads.util.ReviewerUtil;
import dev.cerus.maps.api.MapScreen;
import dev.cerus.maps.api.graphics.MapGraphics;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.NotNull;

public class GradualBarTransition implements Transition {

    private static final int STEP = 8;
    private final Scheduler scheduler = ExecutorServiceScheduler.create(Executors.newScheduledThreadPool(1));

    @Override
    public void makeTransition(final @NotNull MapScreen screen, final MapImage oldImg, final @NotNull MapImage newImg, @NotNull final TransitionRecorder recorder) {
        if (oldImg != null && oldImg.getId().equals(newImg.getId())) {
            return;
        }

        recorder.start(screen);
        final MapGraphics<?, ?> graphics = screen.getGraphics();
        this.scheduler.scheduleAtFixedRate(new SchedulerRunnable() {
            private int x = 0;

            @Override
            public void run() {
                final MapGraphics<?, ?> imageGraphics = newImg.getGraphics();
                if (graphics.hasDirectAccessCapabilities()) {
                    for (int y = 0; y < screen.getHeight() * 128; y++) {
                        System.arraycopy(imageGraphics.getDirectAccessData(), imageGraphics.index(this.x, y), graphics.getDirectAccessData(), graphics.index(this.x, y), STEP);
                    }
                } else {
                    for (int xs = 0; xs < STEP; xs++) {
                        for (int y = 0; y < screen.getHeight() * 128; y++) {
                            graphics.setPixel(this.x + xs, y, imageGraphics.getPixel(this.x + xs, y));
                        }
                    }
                }
                screen.sendMaps(false, ReviewerUtil.getNonReviewingPlayers(screen));
                recorder.record(screen);

                if ((this.x = this.x + STEP) >= screen.getWidth() * 128) {
                    screen.sendMaps(true, ReviewerUtil.getNonReviewingPlayers(screen));
                    this.cancel();
                }
            }

            @Override
            public void cancel() {
                super.cancel();
                recorder.end(screen);
            }
        }, 0, 1000 / 20, TimeUnit.MILLISECONDS);
    }

    @Override
    public void cleanup() throws Exception {
        this.scheduler.close();
    }

}
