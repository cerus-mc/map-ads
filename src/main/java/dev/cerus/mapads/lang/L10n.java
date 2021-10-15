package dev.cerus.mapads.lang;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class L10n {

    private static final Map<String, String> stringMap = new HashMap<>();
    private static final Map<String, List<String>> stringListMap = new HashMap<>();

    public static String getPrefixed(final String key, final Object... params) {
        return get("prefix") + get(key, params);
    }

    public static String get(final String key, final Object... params) {
        return String.format(stringMap.getOrDefault(key, key), params);
    }

    public static List<String> getList(final String key) {
        return stringListMap.getOrDefault(key, Collections.singletonList(key));
    }

    public static void put(final String k, final String v) {
        stringMap.put(k, v);
    }

    public static void put(final String k, final List<String> v) {
        stringListMap.put(k, v);
    }

}
