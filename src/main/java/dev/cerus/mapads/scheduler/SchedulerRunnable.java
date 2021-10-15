package dev.cerus.mapads.scheduler;

public abstract class SchedulerRunnable implements Runnable {

    private int id;
    private Scheduler scheduler;

    public void cancel() {
        this.scheduler.cancel(this.getId());
    }

    public int getId() {
        return this.id;
    }

    public void setId(final int id) {
        this.id = id;
    }

    public Scheduler getScheduler() {
        return this.scheduler;
    }

    public void setScheduler(final Scheduler scheduler) {
        this.scheduler = scheduler;
    }

}
