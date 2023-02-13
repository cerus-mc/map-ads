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

public class BlinkTransition implements Transition {

    private static final int STEP = 8;
    private final Scheduler scheduler = ExecutorServiceScheduler.create(Executors.newScheduledThreadPool(1));
    private final boolean reversed;

    public BlinkTransition(final boolean reversed) {
        this.reversed = reversed;
    }

    @Override
    public void makeTransition(final @NotNull MapScreen screen, final MapImage oldImg, final @NotNull MapImage newImg, @NotNull final TransitionRecorder recorder) {
        if (oldImg != null && oldImg.getId().equals(newImg.getId())) {
            return;
        }

        recorder.start(screen);
        final MapGraphics<?, ?> graphics = screen.getGraphics();
        this.scheduler.scheduleAtFixedRate(new SchedulerRunnable() {
            private int y = BlinkTransition.this.reversed ? ((screen.getHeight() * 128) / STEP) / 2 : 0;

            @Override
            public void run() {
                try {
                    final MapGraphics<?, ?> imageGraphics = newImg.getGraphics();
                    if (graphics.hasDirectAccessCapabilities()) {
                    /*for (int yy = 0; yy < STEP; yy++) {
                        System.arraycopy(imageGraphics.getDirectAccessData(), imageGraphics.index(0, yy + (this.y * STEP)),
                                graphics.getDirectAccessData(), graphics.index(0, yy + (this.y * STEP)), screen.getWidth() * 128 * STEP);
                        System.arraycopy(imageGraphics.getDirectAccessData(), imageGraphics.index(0, (screen.getHeight() * 128) - (yy + (this.y * STEP))),
                                graphics.getDirectAccessData(), graphics.index(0, (screen.getHeight() * 128) - (yy + (this.y * STEP))), screen.getWidth() * 128);
                    }*/

                        System.arraycopy(imageGraphics.getDirectAccessData(), imageGraphics.index(0, this.y * STEP),
                                graphics.getDirectAccessData(), graphics.index(0, this.y * STEP), screen.getWidth() * 128 * STEP);
                        System.arraycopy(imageGraphics.getDirectAccessData(), imageGraphics.index(0, (screen.getHeight() * 128) - ((this.y + 1) * STEP)),
                                graphics.getDirectAccessData(), graphics.index(0, (screen.getHeight() * 128) - ((this.y + 1) * STEP)), screen.getWidth() * 128 * STEP);
                    } else {
                        for (int yy = 0; yy < STEP; yy++) {
                            for (int xx = 0; xx < screen.getWidth() * 128; xx++) {
                                graphics.setPixel(xx, yy + (this.y * STEP), imageGraphics.getPixel(xx, yy + ((this.y + 1) * STEP)));
                                graphics.setPixel(xx, (screen.getHeight() * 128) - (yy + (this.y * STEP)),
                                        imageGraphics.getPixel(xx, (screen.getHeight() * 128) - (yy + ((this.y + 1) * STEP))));
                            }
                        }
                    }
                    screen.sendMaps(false, ReviewerUtil.getNonReviewingPlayers(screen));
                    recorder.record(screen);

                    if ((BlinkTransition.this.reversed ? --this.y : ++this.y) >= ((screen.getHeight() * 128) / STEP) / 2) {
                        screen.sendMaps(true, ReviewerUtil.getNonReviewingPlayers(screen));
                        this.cancel();
                    }
                } catch (final Throwable ttt) {
                    System.err.println(ttt.getMessage());
                    ttt.printStackTrace();
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
