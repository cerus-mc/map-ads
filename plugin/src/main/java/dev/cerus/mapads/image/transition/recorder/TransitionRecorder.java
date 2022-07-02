package dev.cerus.mapads.image.transition.recorder;

import dev.cerus.maps.api.MapScreen;
import java.util.function.Consumer;

public interface TransitionRecorder {

    static <T extends TransitionRecorder> CallbackTransitionRecorder<T> callback(final T other, final Consumer<T> callback) {
        return new FunctionalCallbackTransitionRecorder<>(other, callback);
    }

    static CompressingTransitionRecorder binary() {
        return new CompressingTransitionRecorder();
    }

    static TransitionRecorder noop() {
        return new NoOpTransitionRecorder();
    }

    void start(MapScreen screen);

    void record(MapScreen screen);

    void end(MapScreen screen);

    boolean wasStarted();

}
