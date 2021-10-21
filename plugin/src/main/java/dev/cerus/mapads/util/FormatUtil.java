package dev.cerus.mapads.util;

public class FormatUtil {

    private FormatUtil() {
    }

    public static String formatSize(final long bytes) {
        final double kb = bytes / 1024D;
        final double mb = kb / 1024D;
        return String.format("%.2f ", kb > 1024 ? mb : kb) + (kb > 1024 ? "MB" : "KB");
    }

}
