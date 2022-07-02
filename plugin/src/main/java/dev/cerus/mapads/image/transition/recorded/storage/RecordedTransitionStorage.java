package dev.cerus.mapads.image.transition.recorded.storage;

import dev.cerus.mapads.image.transition.recorded.RecordedTransition;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface RecordedTransitionStorage extends AutoCloseable {

    boolean has(String screen, String transitionId, UUID imgId1, UUID imgId2);

    CompletableFuture<RecordedTransition> load(String screen, String transitionId, UUID imgId1, UUID imgId2);

    CompletableFuture<Void> save(String screen, String transitionId, UUID imgId1, UUID imgId2, byte[] data);

    CompletableFuture<Void> resetLastAccess(String screen, String transitionId, UUID imgId1, UUID imgId2);

    CompletableFuture<Void> deleteAll(String screen, String transitionId);

    CompletableFuture<Void> deleteAll(String screen);

    CompletableFuture<Void> deleteAll(String screen, UUID imageId);

    CompletableFuture<Void> deleteOlderThan(long timestamp);

}
