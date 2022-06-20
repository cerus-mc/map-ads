package dev.cerus.mapads.util;

import dev.cerus.maps.api.MapScreen;
import java.util.HashSet;
import java.util.Set;

public class FrameMarkerUtil {

    private static final Set<Integer> frames = new HashSet<>();
    private static final Set<Integer> screens = new HashSet<>();

    private FrameMarkerUtil() {
    }

    public static void mark(final MapScreen screen) {
        screens.add(screen.getId());
        for (final int[] arr : screen.getFrameIds()) {
            for (final int eid : arr) {
                frames.add(eid);
            }
        }
    }

    public static void unmark(final MapScreen screen) {
        screens.remove(screen.getId());
        for (final int[] arr : screen.getFrameIds()) {
            for (final int eid : arr) {
                frames.remove(eid);
            }
        }
    }

    public static boolean isFrameMarked(final int eid) {
        return frames.contains(eid);
    }

    public static boolean isScreenMarked(final int sid) {
        return screens.contains(sid);
    }

}
