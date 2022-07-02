package dev.cerus.mapads.image.transition;

import dev.cerus.mapads.image.MapImage;
import dev.cerus.mapads.image.transition.recorder.TransitionRecorder;
import dev.cerus.mapads.scheduler.ExecutorServiceScheduler;
import dev.cerus.mapads.scheduler.Scheduler;
import dev.cerus.mapads.scheduler.SchedulerRunnable;
import dev.cerus.mapads.util.ReviewerUtil;
import dev.cerus.maps.api.MapScreen;
import dev.cerus.maps.api.graphics.MapGraphics;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.NotNull;

public class PixelateSmallTransition implements Transition {

    private final Scheduler scheduler = ExecutorServiceScheduler.create(Executors.newScheduledThreadPool(1));

    @Override
    public void makeTransition(final @NotNull MapScreen screen, final MapImage oldImg, final @NotNull MapImage newImg, @NotNull final TransitionRecorder recorder) {
        if (oldImg != null && oldImg.getId().equals(newImg.getId())) {
            return;
        }

        final int[][] coordinateTemplate = new int[64][2];
        int n = 0;
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                coordinateTemplate[(i * 8) + j] = new int[] {j, n};
            }
            n++;
        }

        final List<int[]>[][] coordsListArr = new List[screen.getWidth()][screen.getHeight()];
        for (int x = 0; x < screen.getWidth(); x++) {
            for (int y = 0; y < screen.getHeight(); y++) {
                final ArrayList<int[]> list = new ArrayList<>(Arrays.asList(coordinateTemplate));
                Collections.shuffle(list);
                coordsListArr[x][y] = list;
            }
        }

        recorder.start(screen);
        final MapGraphics<?, ?> graphics = screen.getGraphics();
        this.scheduler.scheduleAtFixedRate(new SchedulerRunnable() {
            private int count = 0;

            @Override
            public void run() {
                if (this.count >= 64) {
                    this.cancel();
                    screen.sendMaps(true, ReviewerUtil.getNonReviewingPlayers(screen));
                    return;
                }

                for (int sx = 0; sx < screen.getWidth(); sx++) {
                    for (int sy = 0; sy < screen.getHeight(); sy++) {
                        final List<int[]> coordsList = coordsListArr[sx][sy];
                        final int[] coords = coordsList.remove(0);
                        final int baseX = coords[0] * 16 + (sx * 128);
                        final int baseY = coords[1] * 16 + (sy * 128);

                        if (graphics.hasDirectAccessCapabilities()) {
                            for (int y = baseY; y < baseY + 16; y++) {
                                System.arraycopy(newImg.getGraphics().getDirectAccessData(),
                                        newImg.getGraphics().index(baseX, y),
                                        graphics.getDirectAccessData(),
                                        graphics.index(baseX, y),
                                        16);
                            }
                        } else {
                            for (int x = baseX; x < baseX + 16; x++) {
                                for (int y = baseY; y < baseY + 16; y++) {
                                    graphics.setPixel(x, y, newImg.getGraphics().getPixel(x, y));
                                }
                            }
                        }
                    }
                }

                this.count++;
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
