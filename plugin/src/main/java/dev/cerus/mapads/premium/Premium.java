package dev.cerus.mapads.premium;

public class Premium {

    private static String user;
    private static String resource;
    private static String nonce;

    public static void init() {
        user = "%%__USER__%%";
        resource = "%%__RESOURCE__%%";
        nonce = "%%__NONCE__%%";
    }

    public static String getNonce() {
        return nonce;
    }

    public static String getResource() {
        return resource;
    }

    public static String getUser() {
        return user;
    }

    public static long to64BitIdentifier() {
        // Map resource and member id into a 64-bit integer.
        // Will be used for verification when requesting support.
        try {
            long l = 0;
            l |= Integer.parseInt(getResource());
            l = l << 32;
            l |= Integer.parseInt(getUser());
            return l;
        } catch (final NumberFormatException ignored) {
            return (500L) << 32;
        }
    }

    public static boolean isPremium() {
        return !user.startsWith("%%") && !resource.startsWith("%%") && !nonce.startsWith("%%");
    }

}
