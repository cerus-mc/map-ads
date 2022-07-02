package dev.cerus.mapads.image.transition.recorder;

import dev.cerus.maps.api.MapScreen;

public abstract class CallbackTransitionRecorder<T extends TransitionRecorder> implements TransitionRecorder {

    private final T backing;

    public CallbackTransitionRecorder(final T backing) {
        this.backing = backing;
    }

    @Override
    public void start(final MapScreen screen) {
        this.backing.start(screen);
    }

    @Override
    public void record(final MapScreen screen) {
        this.backing.record(screen);
    }

    @Override
    public void end(final MapScreen screen) {
        this.backing.end(screen);
        this.recorderDone(this.backing);
    }

    public abstract void recorderDone(T recorder);

    @Override
    public boolean wasStarted() {
        return this.backing.wasStarted();
    }

}
