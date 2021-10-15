package dev.cerus.mapads.scheduler;

import java.util.concurrent.TimeUnit;

public interface Scheduler extends AutoCloseable {

    void scheduleAtFixedRate(SchedulerRunnable runnable, long delay, long period, TimeUnit timeUnit);

    void cancel(int id);

}
