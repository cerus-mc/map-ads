package dev.cerus.mapads.image.transition;

import dev.cerus.mapads.image.MapImage;
import dev.cerus.maps.api.MapScreen;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RandomTransition implements Transition {

    @Override
    public void makeTransition(@NotNull MapScreen screen, @Nullable MapImage oldImg, @NotNull MapImage newImg) {
        final List<String> transitionNames = TransitionRegistry.names().stream()
                .filter(s -> !s.equals("random"))
                .toList();
        TransitionRegistry.getTransition(transitionNames.get(ThreadLocalRandom.current().nextInt(transitionNames.size())))
                .makeTransition(screen, oldImg, newImg);
    }

}
