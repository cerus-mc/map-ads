package dev.cerus.mapads.image.transition.recorded;

import dev.cerus.mapads.scheduler.ExecutorServiceScheduler;
import dev.cerus.mapads.scheduler.Scheduler;
import dev.cerus.mapads.scheduler.SchedulerRunnable;
import dev.cerus.maps.api.MapScreen;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class RecordedTransitions {

    private static final Scheduler SCHEDULER = ExecutorServiceScheduler.create(Executors.newScheduledThreadPool(2));

    public static void playTransition(final MapScreen screen, final RecordedTransition recording) {
        SCHEDULER.scheduleAtFixedRate(new SchedulerRunnable() {
            private int index;

            @Override
            public void run() {
                if (this.index >= recording.getFrameCount()) {
                    this.cancel();
                    return;
                }
                recording.drawFrame(screen, this.index++);
                screen.sendMaps(false);
            }

            @Override
            public void cancel() {
                super.cancel();
                if (recording instanceof AutoCloseable closeable) {
                    try {
                        closeable.close();
                    } catch (final Exception ignored) {
                    }
                }
            }
        }, 0, 50, TimeUnit.MILLISECONDS);
    }

    public static void stop() {
        try {
            SCHEDULER.close();
        } catch (final Exception ignored) {
        }
    }

}
