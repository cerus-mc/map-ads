package dev.cerus.mapads.util;

import dev.cerus.mapads.lang.L10n;

public class FormatUtil {

    private FormatUtil() {
    }

    public static String formatSize(final long bytes) {
        final double kb = bytes / 1024D;
        final double mb = kb / 1024D;
        return String.format("%.2f ", kb > 1024 ? mb : kb) + (kb > 1024 ? "MB" : "KB");
    }

    public static String formatMinutes(final int mins) {
        if (mins <= 0) {
            return L10n.get("gui.create.format.minute", mins);
        }

        final int d = mins / 60 / 24;
        final int h = mins / 60 % 24;
        final int m = mins % 60;
        return (d > 0 ? L10n.get("gui.create.format.day", d) : "")
                + (h > 0 ? L10n.get("gui.create.format.hour", h) : "")
                + (m > 0 ? L10n.get("gui.create.format.minute", m) : "");
    }

}
