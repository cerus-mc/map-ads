package dev.cerus.mapads.image.transition;

import dev.cerus.mapads.image.MapImage;
import dev.cerus.mapads.image.transition.recorder.TransitionRecorder;
import dev.cerus.maps.api.MapScreen;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RandomTransition implements Transition {

    @Override
    public void makeTransition(@NotNull final MapScreen screen, @Nullable final MapImage oldImg, @NotNull final MapImage newImg, @NotNull final TransitionRecorder recorder) {
        final List<String> transitionNames = TransitionRegistry.names().stream()
                .filter(s -> !s.equals("random"))
                .toList();
        TransitionRegistry.getTransition(transitionNames.get(ThreadLocalRandom.current().nextInt(transitionNames.size())))
                .makeTransition(screen, oldImg, newImg, recorder);
    }

}
