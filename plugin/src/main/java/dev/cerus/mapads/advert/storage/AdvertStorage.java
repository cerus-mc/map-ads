package dev.cerus.mapads.advert.storage;

import dev.cerus.mapads.advert.Advertisement;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface AdvertStorage extends AutoCloseable {

    Advertisement getCurrentAdvert(String screenId);

    void nextAdvert(String screenId);

    int getIndex(String screenId);

    CompletableFuture<Void> updateAdvert(Advertisement advertisement);

    CompletableFuture<Void> deleteAdverts(UUID... ids);

    List<Advertisement> getPendingAdvertisements();

}
