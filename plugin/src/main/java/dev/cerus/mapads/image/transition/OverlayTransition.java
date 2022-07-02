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
import org.jetbrains.annotations.Nullable;

public class OverlayTransition implements Transition {

    private static final int STEP = 8;
    private final Scheduler scheduler = ExecutorServiceScheduler.create(Executors.newScheduledThreadPool(1));

    @Override
    public void makeTransition(@NotNull final MapScreen screen, @Nullable final MapImage oldImg, @NotNull final MapImage newImg, @NotNull final TransitionRecorder recorder) {
        if (oldImg != null && oldImg.getId().equals(newImg.getId())) {
            return;
        }

        recorder.start(screen);
        final MapGraphics<?, ?> graphics = screen.getGraphics();
        this.scheduler.scheduleAtFixedRate(new SchedulerRunnable() {
            private int col = 1;

            @Override
            public void run() {
                if (this.col > (screen.getWidth() * 128) / STEP) {
                    this.cancel();
                    screen.sendMaps(true, ReviewerUtil.getNonReviewingPlayers(screen));
                    return;
                }

                graphics.place(newImg.getGraphics(), (this.col * STEP) - (newImg.getWidth() * 128), 0, 1f, false);
                recorder.record(screen);
                this.col++;

                screen.sendMaps(false, ReviewerUtil.getNonReviewingPlayers(screen));
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
