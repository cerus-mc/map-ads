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

public class AlphaTransition implements Transition {

    private final Scheduler scheduler = ExecutorServiceScheduler.create(Executors.newScheduledThreadPool(1));

    @Override
    public void makeTransition(@NotNull final MapScreen screen, @Nullable final MapImage oldImg, @NotNull final MapImage newImg) {
        if (oldImg != null && oldImg.getId().equals(newImg.getId())) {
            return;
        }

        final MapGraphics<?, ?> graphics = screen.getGraphics();
        this.scheduler.scheduleAtFixedRate(new SchedulerRunnable() {
            private float alpha = 0.05f;
            private int t = 0;

            @Override
            public void run() {
                if (this.t < 3) {
                    this.t++;
                    return;
                }
                this.t = 0;
                if (this.alpha > 1f) {
                    this.cancel();
                    graphics.place(newImg.getGraphics(), 0, 0, 1f);
                    screen.sendMaps(true, ReviewerUtil.getNonReviewingPlayers(screen));
                    return;
                }

                graphics.place(newImg.getGraphics(), 0, 0, this.alpha);
                screen.sendMaps(false, ReviewerUtil.getNonReviewingPlayers(screen));
                this.alpha += 0.05f;
            }
        }, 0, 1000 / 20, TimeUnit.MILLISECONDS);
    }

    @Override
    public void cleanup() throws Exception {
        this.scheduler.close();
    }

    @Override
    public boolean isPerformanceIntensive() {
        return true;
    }
}
