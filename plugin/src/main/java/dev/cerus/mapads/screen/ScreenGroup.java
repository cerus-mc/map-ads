package dev.cerus.mapads.screen;

import dev.cerus.mapads.screen.storage.AdScreenStorage;
import dev.cerus.mapads.util.Mutable;
import dev.cerus.maps.plugin.map.MapScreenRegistry;
import java.util.List;
import java.util.Objects;

public record ScreenGroup(String id, String groupName, List<String> screenIds, Mutable<Integer> fixedTime, Mutable<Double> fixedPrice) {

    public List<String> sizeStrings(final AdScreenStorage storage) {
        return this.screenIds.stream()
                .map(storage::getAdScreen)
                .filter(Objects::nonNull)
                .map(AdScreen::getScreenId)
                .map(MapScreenRegistry::getScreen)
                .filter(Objects::nonNull)
                .map(screen -> (screen.getWidth() * 128) + "x" + (screen.getHeight() * 128))
                .distinct()
                .toList();
    }

}
