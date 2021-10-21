package dev.cerus.mapads.image.transition;

import dev.cerus.mapads.image.MapImage;
import dev.cerus.mapads.scheduler.ExecutorServiceScheduler;
import dev.cerus.mapads.scheduler.Scheduler;
import dev.cerus.mapads.scheduler.SchedulerRunnable;
import dev.cerus.mapads.util.ReviewerUtil;
import dev.cerus.maps.api.MapScreen;
import dev.cerus.maps.api.graphics.MapScreenGraphics;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.NotNull;

public class ShiftTransition implements Transition {

    private static final int STEP = 8;
    private final Scheduler scheduler = ExecutorServiceScheduler.create(Executors.newScheduledThreadPool(1));

    @Override
    public void makeTransition(final @NotNull MapScreen screen, final MapImage oldImg, final @NotNull MapImage newImg) {
        if (oldImg != null && oldImg.getId().equals(newImg.getId())) {
            return;
        }

        final MapScreenGraphics graphics = screen.getGraphics();
        this.scheduler.scheduleAtFixedRate(new SchedulerRunnable() {
            private int col = 1;

            @Override
            public void run() {
                if (this.col > (screen.getWidth() * 128) / STEP) {
                    this.cancel();
                    return;
                }

                if (oldImg != null) {
                    for (int x = this.col * STEP; x < screen.getWidth() * 128; x++) {
                        for (int y = 0; y < screen.getHeight() * 128; y++) {
                            graphics.setPixel(x, y, oldImg.getData()[x - (this.col * STEP)][y]);
                        }
                    }
                }
                for (int x = 0; x < this.col * STEP; x++) {
                    for (int y = 0; y < screen.getHeight() * 128; y++) {
                        final int xx = (screen.getWidth() * 128) - ((this.col * STEP) - x);
                        graphics.setPixel(x, y, newImg.getData()[xx][y]);
                    }
                }
                this.col++;

                screen.update(MapScreen.DirtyHandlingPolicy.IGNORE, ReviewerUtil.getNonReviewingPlayers(screen));
            }
        }, 0, 1000 / 20, TimeUnit.MILLISECONDS);
    }

    @Override
    public void cleanup() throws Exception {
        this.scheduler.close();
    }

}
