package dev.cerus.mapads.image.transition;

import dev.cerus.mapads.image.MapImage;
import dev.cerus.mapads.scheduler.ExecutorServiceScheduler;
import dev.cerus.mapads.scheduler.Scheduler;
import dev.cerus.mapads.scheduler.SchedulerRunnable;
import dev.cerus.mapads.util.ReviewerUtil;
import dev.cerus.maps.api.MapScreen;
import dev.cerus.maps.api.graphics.MapGraphics;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.NotNull;

public class ShiftTransition implements Transition {

    private static final int STEP = 16;
    private final Scheduler scheduler = ExecutorServiceScheduler.create(Executors.newScheduledThreadPool(1));

    @Override
    public void makeTransition(final @NotNull MapScreen screen, final MapImage oldImg, final @NotNull MapImage newImg) {
        if (oldImg != null && oldImg.getId().equals(newImg.getId())) {
            return;
        }

        final MapGraphics<?, ?> graphics = screen.getGraphics();
        this.scheduler.scheduleAtFixedRate(new SchedulerRunnable() {
            private int col = 1;

            @Override
            public void run() {
                if (this.col > (screen.getWidth() * 128) / STEP) {
                    this.cancel();
                    //graphics.place(newImg.getGraphics(), 0, 0, 1f, false);
                    screen.sendMaps(true, ReviewerUtil.getNonReviewingPlayers(screen));
                    return;
                }

                if (oldImg != null) {
                    graphics.place(oldImg.getGraphics(), this.col * STEP, 0, 1f, false);
                }
                graphics.place(newImg.getGraphics(), (this.col * STEP) - (newImg.getWidth() * 128), 0, 1f, false);
                this.col++;

                screen.sendMaps(false, ReviewerUtil.getNonReviewingPlayers(screen));
            }
        }, 0, 1000 / 20, TimeUnit.MILLISECONDS);
    }

    @Override
    public void cleanup() throws Exception {
        this.scheduler.close();
    }

}
