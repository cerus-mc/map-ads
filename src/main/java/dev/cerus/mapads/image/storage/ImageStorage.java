package dev.cerus.mapads.image.storage;

import dev.cerus.mapads.image.MapImage;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface ImageStorage extends AutoCloseable {

    CompletableFuture<Void> updateMapImage(MapImage mapImage);

    CompletableFuture<MapImage> getMapImage(UUID id);

    CompletableFuture<Void> deleteMapImages(UUID... ids);

}
