package dev.cerus.mapads.image.transition;

import dev.cerus.mapads.premium.Premium;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class TransitionRegistry {

    private static final Map<String, Transition> TRANSITION_MAP = new LinkedHashMap<>() {
        {
            this.put("instant", new InstantTransition());
            if (Premium.isPremium()) {
                // If you didn't purchase the resource you'll have to
                // modify this to unlock all the features :P
                this.put("gradual_bar", new GradualBarTransition());
                this.put("pixelate_big", new PixelateBigTransition());
                this.put("pixelate_small", new PixelateSmallTransition());
                this.put("shift", new ShiftTransition());
            }
        }
    };

    public static boolean register(final String name, final Transition transition) {
        if (TRANSITION_MAP.containsKey(name)) {
            return false;
        }
        TRANSITION_MAP.put(name, transition);
        return true;
    }

    public static void cleanup() throws Exception {
        for (final Transition transition : TRANSITION_MAP.values()) {
            transition.cleanup();
        }
    }

    public static Transition getTransition(final String name) {
        return TRANSITION_MAP.get(name);
    }

    public static Transition getOrDefault(final String name) {
        final Transition transition = getTransition(name);
        return transition == null ? getTransition("instant") : transition;
    }

    public static Set<String> names() {
        return TRANSITION_MAP.keySet();
    }

}
