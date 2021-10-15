package dev.cerus.mapads.scheduler;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ExecutorServiceScheduler implements Scheduler {

    private final Map<Integer, ScheduledFuture<?>> futureMap = new HashMap<>();
    private final ScheduledExecutorService executorService;
    private int idCounter = 0;

    private ExecutorServiceScheduler(final ScheduledExecutorService executorService) {
        this.executorService = executorService;
    }

    public static Scheduler create(final ScheduledExecutorService backing) {
        return new ExecutorServiceScheduler(backing);
    }

    @Override
    public void scheduleAtFixedRate(final SchedulerRunnable runnable, final long delay, final long period, final TimeUnit timeUnit) {
        runnable.setId(this.idCounter++);
        runnable.setScheduler(this);
        this.futureMap.put(runnable.getId(), this.executorService.scheduleAtFixedRate(runnable, delay, period, timeUnit));
    }

    @Override
    public void cancel(final int id) {
        final ScheduledFuture<?> future = this.futureMap.remove(id);
        if (future != null) {
            future.cancel(false);
        }
    }

    @Override
    public void close() throws Exception {
        this.executorService.shutdown();
    }

}
