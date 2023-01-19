package dev.cerus.mapads.util;

import dev.cerus.mapads.ConfigModel;

public class UrlVerificationUtil {

    private UrlVerificationUtil() {
    }

    public static boolean verify(final String host, final ConfigModel model) {
        if (model.trustedImageUrls.contains("*")) {
            return true;
        }

        final String[] hostSplit = host.split("\\.");
        outer:
        for (final String trustedImageUrl : model.trustedImageUrls) {
            final String[] trustedSplit = trustedImageUrl.split("\\.");
            if (trustedSplit.length != hostSplit.length) {
                continue;
            }

            for (int i = 0; i < hostSplit.length; i++) {
                final String hostPart = hostSplit[i];
                final String trustedPart = trustedSplit[i];
                if (!trustedPart.equals("*") && !hostPart.equalsIgnoreCase(trustedPart)) {
                    continue outer;
                }
            }
            return true;
        }
        return false;
    }

}
