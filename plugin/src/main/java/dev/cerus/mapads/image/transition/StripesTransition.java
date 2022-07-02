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

public class StripesTransition implements Transition {

    private static final int STEP = 8;
    private static final int WSTEP = 64;
    private final Scheduler scheduler = ExecutorServiceScheduler.create(Executors.newScheduledThreadPool(1));

    @Override
    public void makeTransition(final @NotNull MapScreen screen, final MapImage oldImg, final @NotNull MapImage newImg, @NotNull final TransitionRecorder recorder) {
        if (oldImg != null && oldImg.getId().equals(newImg.getId())) {
            return;
        }

        recorder.start(screen);
        final MapGraphics<?, ?> graphics = screen.getGraphics();
        this.scheduler.scheduleAtFixedRate(new SchedulerRunnable() {
            private int row = 0;

            @Override
            public void run() {
                if (this.row >= (screen.getHeight() * 128) / STEP) {
                    graphics.place(newImg.getGraphics(), 0, 0);
                    screen.sendMaps(true, ReviewerUtil.getNonReviewingPlayers(screen));
                    recorder.record(screen);
                    this.cancel();
                    return;
                }

                for (int col = 0; col < (screen.getWidth() * 128) / WSTEP; col++) {
                    final boolean dir = col % 2 == 0; // True: Up, False: Down
                    for (
                            int y = (dir ? ((screen.getHeight() * 128) - (STEP * this.row + STEP)) : (STEP * this.row));
                            y < (dir ? ((screen.getHeight() * 128) - (STEP * this.row)) : (STEP * this.row + STEP));
                            y++
                    ) {
                        if (graphics.hasDirectAccessCapabilities()) {
                            System.arraycopy(newImg.getGraphics().getDirectAccessData(),
                                    newImg.getGraphics().index(col * WSTEP, y),
                                    graphics.getDirectAccessData(),
                                    graphics.index(col * WSTEP, y),
                                    WSTEP);
                        } else {
                            for (int x = col * WSTEP; x < col * WSTEP + WSTEP; x++) {
                                graphics.setPixel(x, y, newImg.getGraphics().getPixel(x, y));
                            }
                        }
                    }
                }
                this.row++;

                screen.sendMaps(false, ReviewerUtil.getNonReviewingPlayers(screen));
                recorder.record(screen);
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
