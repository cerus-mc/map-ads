package dev.cerus.mapads.image.transition;

import dev.cerus.mapads.image.MapImage;
import dev.cerus.mapads.image.transition.recorder.TransitionRecorder;
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

public class ShrinkingTransition implements Transition {

    private final Scheduler scheduler = ExecutorServiceScheduler.create(Executors.newScheduledThreadPool(1));

    @Override
    public void makeTransition(@NotNull final MapScreen screen, @Nullable final MapImage oldImg, @NotNull final MapImage newImg, @NotNull final TransitionRecorder recorder) {
        if (oldImg != null && oldImg.getId().equals(newImg.getId())) {
            return;
        }

        if (oldImg == null) {
            screen.getGraphics().place(newImg.getGraphics(), 0, 0);
            screen.sendMaps(true, ReviewerUtil.getNonReviewingPlayers(screen));
            return;
        }

        recorder.start(screen);
        final MapGraphics<?, ?> graphics = screen.getGraphics();
        this.scheduler.scheduleAtFixedRate(new SchedulerRunnable() {
            private double scaler = 1d;
            private double sub = 0.001;

            @Override
            public void run() {
                if (this.scaler <= 0d) {
                    graphics.place(newImg.getGraphics(), 0, 0, 1f, false);
                    screen.sendMaps(true, ReviewerUtil.getNonReviewingPlayers(screen));
                    recorder.record(screen);
                    this.cancel();
                    return;
                }

                graphics.place(newImg.getGraphics(), 0, 0, 1f, false);
                final MapGraphics<?, ?> scaledImg = ShrinkingTransition.this.resizeImage(oldImg.getGraphics(),
                        (int) ((oldImg.getWidth() * 128d) * this.scaler),
                        (int) ((oldImg.getHeight() * 128d) * this.scaler));
                graphics.place(scaledImg,
                        (graphics.getWidth() / 2) - (scaledImg.getWidth() / 2),
                        (graphics.getHeight() / 2) - (scaledImg.getHeight() / 2),
                        1f,
                        false);
                recorder.record(screen);

                screen.sendMaps(false, ReviewerUtil.getNonReviewingPlayers(screen));
                this.scaler -= this.sub;
                this.sub += 0.001d;
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
