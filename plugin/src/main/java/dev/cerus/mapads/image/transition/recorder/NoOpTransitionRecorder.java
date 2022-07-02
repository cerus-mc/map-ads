package dev.cerus.mapads.image.transition.recorder;

import dev.cerus.maps.api.MapScreen;

public class NoOpTransitionRecorder implements TransitionRecorder {

    @Override
    public void start(final MapScreen screen) {
    }

    @Override
    public void record(final MapScreen screen) {
    }

    @Override
    public void end(final MapScreen screen) {
    }

    @Override
    public boolean wasStarted() {
        return false;
    }

}
