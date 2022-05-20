package dev.cerus.mapads.image.transition;

import dev.cerus.mapads.image.MapImage;
import dev.cerus.mapads.util.ReviewerUtil;
import dev.cerus.maps.api.MapScreen;
import org.jetbrains.annotations.NotNull;

public class InstantTransition implements Transition {

    @Override
    public void makeTransition(final @NotNull MapScreen screen, final MapImage oldImg, final @NotNull MapImage newImg) {
        if (oldImg != null && oldImg.getId().equals(newImg.getId())) {
            return;
        }

        newImg.drawOnto(screen, 0, 0);
        screen.sendMaps(false, ReviewerUtil.getNonReviewingPlayers(screen));
    }

}
