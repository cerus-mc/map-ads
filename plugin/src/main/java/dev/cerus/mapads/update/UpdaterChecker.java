package dev.cerus.mapads.update;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UpdaterChecker {

    private static final int ID = 96918;
    private static final ExecutorService exec = Executors.newSingleThreadExecutor();

    public static CompletableFuture<String> getNewestVersion() {
        final CompletableFuture<String> future = new CompletableFuture<>();
        exec.execute(() -> {
            try {
                final HttpURLConnection connection = (HttpURLConnection) new URL("https://api.spigotmc.org/simple/0.2/index.php?" +
                        "action=getResource&id=" + ID).openConnection();
                connection.setDoInput(true);
                if (connection.getResponseCode() != 200) {
                    future.complete(null);
                    return;
                }

                final JsonElement element = new JsonParser().parse(new InputStreamReader(connection.getInputStream()));
                future.complete(element.getAsJsonObject().get("current_version").getAsString());
            } catch (final IOException e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    public static boolean isGreater(final String ver, final String currVer) {
        final int[] verNums = Arrays.stream(ver.split("\\."))
                .mapToInt(Integer::parseInt)
                .toArray();
        final int[] currVerNums = Arrays.stream(currVer.split("\\."))
                .mapToInt(Integer::parseInt)
                .toArray();
        if (currVerNums.length != verNums.length) {
            return verNums.length > currVerNums.length;
        }

        for (int i = 0; i < ver.length(); i++) {
            if (verNums[i] > currVerNums[i]) {
                return true;
            } else if (verNums[i] < currVerNums[i]) {
                return false;
            }
        }
        return false;
    }

    public static void stop() {
        exec.shutdown();
    }

}
