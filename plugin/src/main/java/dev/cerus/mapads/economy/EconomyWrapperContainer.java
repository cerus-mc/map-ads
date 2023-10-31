package dev.cerus.mapads.economy;

import org.jetbrains.annotations.NotNull;

public class EconomyWrapperContainer {

    private final EconomyWrapper<?> fallback;
    private EconomyWrapper<?> economyWrapper;

    public EconomyWrapperContainer(@NotNull final EconomyWrapper<?> fallback) {
        this.fallback = fallback;
    }

    public void complete(@NotNull final EconomyWrapper<?> economyWrapper) {
        this.economyWrapper = economyWrapper;
    }

    public boolean isAvailable() {
        return this.economyWrapper != null;
    }

    public @NotNull EconomyWrapper<?> get() {
        if (this.isAvailable()) {
            return this.economyWrapper;
        }
        return this.fallback;
    }

}
