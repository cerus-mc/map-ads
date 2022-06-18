package dev.cerus.mapads.lang;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import dev.cerus.mapads.MapAdsPlugin;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LangManifest {

    private final int currentVersion;
    private final Map<Integer, Map<String, Object>> updateMap;

    public LangManifest(final int currentVersion, final Map<Integer, Map<String, Object>> updateMap) {
        this.currentVersion = currentVersion;
        this.updateMap = updateMap;
    }

    public static LangManifest load() {
        final JsonObject jsonObj;
        try (final InputStream res = MapAdsPlugin.class.getClassLoader().getResourceAsStream("lang_manifest.json");
             final InputStreamReader inRead = new InputStreamReader(res)) {
            jsonObj = new JsonParser().parse(inRead).getAsJsonObject();
        } catch (final IOException e) {
            throw new IllegalStateException("Failed to load language manifest", e);
        }

        final int ver = jsonObj.get("version").getAsInt();
        final Map<Integer, Map<String, Object>> updateMap = new HashMap<>();
        for (final Map.Entry<String, JsonElement> entry : jsonObj.entrySet()) {
            if (!entry.getKey().matches("\\d+")) {
                continue;
            }

            final int v = Integer.parseInt(entry.getKey());
            final JsonObject o = entry.getValue().getAsJsonObject();
            updateMap.put(v, o.entrySet().stream()
                    .filter(e -> e.getValue() instanceof JsonPrimitive || e.getValue() instanceof JsonArray)
                    .map(e -> {
                        final Object val;
                        if (e.getValue() instanceof JsonArray arr) {
                            val = new ArrayList<String>();
                            arr.forEach(elem -> ((List<String>) val).add(elem.getAsString()));
                        } else {
                            val = e.getValue().getAsString();
                        }
                        return Map.entry(e.getKey(), val);
                    })
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
        }
        return new LangManifest(ver, updateMap);
    }

    public int getCurrentVersion() {
        return this.currentVersion;
    }

    public Map<String, Object> getUpdatesFor(final int ver) {
        return this.updateMap.get(ver);
    }

}
