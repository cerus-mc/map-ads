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
import org.jetbrains.annotations.Nullable;

public class OverlayTransition implements Transition {

    private static final int STEP = 8;
    private final Scheduler scheduler = ExecutorServiceScheduler.create(Executors.newScheduledThreadPool(1));

    @Override
    public void makeTransition(@NotNull final MapScreen screen, @Nullable final MapImage oldImg, @NotNull final MapImage newImg) {
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
                    screen.sendMaps(true, ReviewerUtil.getNonReviewingPlayers(screen));
                    return;
                }

                /*for (int x = 0; x < this.col * STEP; x++) {
                    for (int y = 0; y < screen.getHeight() * 128; y++) {
                        final int xx = (screen.getWidth() * 128) - ((this.col * STEP) - x);
                        graphics.setPixel(x, y, newImg.getData()[xx][y]);
                    }
                }*/
                graphics.place(newImg.getGraphics(), (this.col * STEP) - (newImg.getWidth() * 128), 0);
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
