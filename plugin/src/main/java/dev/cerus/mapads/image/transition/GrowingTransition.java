package dev.cerus.mapads.image.transition;

import dev.cerus.mapads.image.MapImage;
import dev.cerus.mapads.scheduler.ExecutorServiceScheduler;
import dev.cerus.mapads.scheduler.Scheduler;
import dev.cerus.mapads.scheduler.SchedulerRunnable;
import dev.cerus.mapads.util.ReviewerUtil;
import dev.cerus.maps.api.MapScreen;
import dev.cerus.maps.api.graphics.MapGraphics;
import dev.cerus.maps.api.graphics.StandaloneMapGraphics;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GrowingTransition implements Transition {

    private final Scheduler scheduler = ExecutorServiceScheduler.create(Executors.newScheduledThreadPool(1));

    @Override
    public void makeTransition(@NotNull final MapScreen screen, @Nullable final MapImage oldImg, @NotNull final MapImage newImg) {
        if (oldImg != null && oldImg.getId().equals(newImg.getId())) {
            return;
        }

        final MapGraphics<?, ?> graphics = screen.getGraphics();
        this.scheduler.scheduleAtFixedRate(new SchedulerRunnable() {
            private double scaler = 0.05d;
            private double add = 0.001;

            @Override
            public void run() {
                if (this.scaler >= 1d) {
                    this.cancel();
                    graphics.place(newImg.getGraphics(), 0, 0, 1f, false);
                    screen.sendMaps(true, ReviewerUtil.getNonReviewingPlayers(screen));
                    return;
                }

                final MapGraphics<?, ?> scaledImg = GrowingTransition.this.resizeImage(newImg.getGraphics(),
                        (int) ((newImg.getWidth() * 128d) * this.scaler),
                        (int) ((newImg.getHeight() * 128d) * this.scaler));
                graphics.place(scaledImg,
                        (graphics.getWidth() / 2) - (scaledImg.getWidth() / 2),
                        (graphics.getHeight() / 2) - (scaledImg.getHeight() / 2),
                        1f,
                        false);

                screen.sendMaps(false, ReviewerUtil.getNonReviewingPlayers(screen));
                this.scaler += this.add;
                this.add += 0.001d;
            }
        }, 0, 1000 / 20, TimeUnit.MILLISECONDS);
    }

    @Override
    public void cleanup() throws Exception {
        this.scheduler.close();
    }

    private MapGraphics<?, ?> resizeImage(final MapGraphics<?, ?> img, final int toW, final int toH) {
        final double xRatio = ((double) img.getWidth()) / ((double) toW);
        final double yRatio = ((double) img.getHeight()) / ((double) toH);

        final MapGraphics<MapGraphics<?, ?>, ?> newImg = StandaloneMapGraphics.standalone(toW, toH);
        for (int y = 0; y < toH; y++) {
            for (int x = 0; x < toW; x++) {
                final int pX = (int) Math.floor(x * xRatio);
                final int pY = (int) Math.floor(y * yRatio);
                newImg.setPixel(x, y, img.getPixel(pX, pY));
            }
        }
        return newImg;
    }

}
