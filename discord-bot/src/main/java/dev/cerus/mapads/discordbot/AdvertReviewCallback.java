package dev.cerus.mapads.discordbot;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

public interface AdvertReviewCallback extends BiFunction<UUID, Boolean, CompletableFuture<Boolean>> {
}
