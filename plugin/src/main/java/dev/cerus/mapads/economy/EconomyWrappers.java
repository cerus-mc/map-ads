package dev.cerus.mapads.economy;

import dev.cerus.mapads.ConfigModel;
import dev.cerus.mapads.MapAdsPlugin;
import java.util.Optional;
import java.util.function.Supplier;
import org.bukkit.plugin.java.JavaPlugin;

public class EconomyWrappers {

    private EconomyWrappers() {
    }

    public static EconomyWrapper<?> find() {
        return Attempt.<EconomyWrapper<?>>newAttempt()
                .attemptGet(EconomyWrappers::override)
                .ifUnsuccessful(VaultWrapper::attemptCreate)
                .ifUnsuccessful(PlayerPointsWrapper::attemptCreate)
                .ifUnsuccessful(AquaCoreWrapper::attemptCreate)
                .get()
                .orElse(NoopWrapper.create());
    }

    private static EconomyWrapper<?> override() {
        final ConfigModel configModel = JavaPlugin.getPlugin(MapAdsPlugin.class).getConfigModel();
        return switch (configModel.economyOverride.toUpperCase()) {
            case "VAULT" -> VaultWrapper.attemptCreate();
            case "PLAYERPOINTS" -> PlayerPointsWrapper.attemptCreate();
            case "AQUACORE" -> AquaCoreWrapper.attemptCreate();
            default -> null;
        };
    }

    private static final class Attempt<T> {

        private T val;

        public static <V> Attempt<V> newAttempt() {
            return new Attempt<>();
        }

        public Attempt<T> attemptGet(final Supplier<T> supplier) {
            try {
                this.val = supplier.get();
            } catch (final Throwable ignored) {
            }
            return this;
        }

        public Attempt<T> ifUnsuccessful(final Supplier<T> supplier) {
            if (this.val == null) {
                this.attemptGet(supplier);
            }
            return this;
        }

        public Optional<T> get() {
            return Optional.ofNullable(this.val);
        }

    }

}
