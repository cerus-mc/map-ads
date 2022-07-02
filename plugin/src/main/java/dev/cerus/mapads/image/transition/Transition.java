package dev.cerus.mapads.image.transition;

import dev.cerus.mapads.image.MapImage;
import dev.cerus.mapads.image.transition.recorder.TransitionRecorder;
import dev.cerus.maps.api.MapScreen;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface Transition {

    void makeTransition(@NotNull MapScreen screen, @Nullable MapImage oldImg, @NotNull MapImage newImg, @NotNull TransitionRecorder recorder);

    default void cleanup() throws Exception {
    }

    default boolean isPerformanceIntensive() {
        return false;
    }

}
