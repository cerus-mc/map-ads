package dev.cerus.mapads.util;

import java.util.OptionalInt;
import org.bukkit.permissions.Permissible;

public class PermissionUtil {

    private PermissionUtil() {
    }

    public static OptionalInt getValue(final Permissible actor, final String permKey) {
        return actor.getEffectivePermissions().stream()
                .filter(info -> info.getPermission().startsWith(permKey))
                .map(info -> info.getPermission().substring(permKey.length()))
                .filter(s -> s.matches("\\d+"))
                .mapToInt(Integer::parseInt)
                .max();
    }

}
