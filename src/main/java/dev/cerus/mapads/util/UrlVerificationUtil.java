package dev.cerus.mapads.util;

import dev.cerus.mapads.ConfigModel;
import java.util.Arrays;

public class UrlVerificationUtil {

    private UrlVerificationUtil() {
    }

    public static boolean verify(final String host, final ConfigModel model) {
        final String[] hostSplit = host.split("\\.");
        outer:
        for (final String trustedImageUrl : model.trustedImageUrls) {
            final String[] trustedSplit = trustedImageUrl.split("\\.");
            if (trustedSplit.length != hostSplit.length) {
                System.out.println("[DEBUG] {URLV} " + trustedSplit.length + " != " + hostSplit.length);
                continue;
            }
            System.out.println("[DEBUG] {URLV} " + Arrays.toString(trustedSplit) + " " + Arrays.toString(hostSplit));

            for (int i = 0; i < hostSplit.length; i++) {
                final String hostPart = hostSplit[i];
                final String trustedPart = trustedSplit[i];
                System.out.println("[DEBUG] {URLV} " + i + " | " + hostPart + " equalsIgnoreCase " + trustedPart);
                if (!trustedPart.equals("*") && !hostPart.equalsIgnoreCase(trustedPart)) {
                    continue outer;
                }
            }
            return true;
        }
        return false;
    }

}
