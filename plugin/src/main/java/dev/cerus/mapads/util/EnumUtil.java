package dev.cerus.mapads.util;

public class EnumUtil {

    private EnumUtil() {
    }

    public static <T extends Enum<?>> T attemptGet(final String name, final T fallback) {
        try {
            return (T) Enum.valueOf(fallback.getClass(), name);
        } catch (final IllegalArgumentException ignored) {
            return fallback;
        }
    }

}
