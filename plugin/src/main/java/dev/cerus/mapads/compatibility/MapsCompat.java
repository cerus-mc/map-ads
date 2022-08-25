package dev.cerus.mapads.compatibility;

import dev.cerus.maps.plugin.MapsPlugin;
import org.bukkit.plugin.java.JavaPlugin;

public class MapsCompat {

    private static Boolean verThree;

    private MapsCompat() {
    }

    public static boolean isThreeOrAbove() {
        if (verThree == null) {
            final String version = JavaPlugin.getPlugin(MapsPlugin.class).getDescription().getVersion();
            verThree = Integer.parseInt(version.split("\\.")[0]) >= 3;
        }
        return verThree;
    }

}
