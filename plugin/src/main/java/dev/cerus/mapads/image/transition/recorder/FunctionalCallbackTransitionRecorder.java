package dev.cerus.mapads.image.transition.recorder;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class FunctionalCallbackTransitionRecorder<T extends TransitionRecorder> extends CallbackTransitionRecorder<T> {

    private final Consumer<T> callback;
    private final CompletableFuture<T> future;

    public FunctionalCallbackTransitionRecorder(final T backing) {
        this(backing, null);
    }

    public FunctionalCallbackTransitionRecorder(final T backing, final Consumer<T> callback) {
        super(backing);
        this.callback = callback;
        this.future = new CompletableFuture<>();
    }

    @Override
    public void recorderDone(final T recorder) {
        if (this.callback != null) {
            this.callback.accept(recorder);
        }
        this.future.complete(recorder);
    }

    public CompletableFuture<T> toFuture() {
        return this.future;
    }

}
