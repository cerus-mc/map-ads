package dev.cerus.mapads.economy;

import java.util.Optional;
import java.util.function.Supplier;

public class EconomyWrappers {

    private EconomyWrappers() {
    }

    public static EconomyWrapper<?> find() {
        return Attempt.<EconomyWrapper<?>>newAttempt()
                .attemptGet(VaultWrapper::attemptCreate)
                .ifUnsuccessful(PlayerPointsWrapper::attemptCreate)
                .get()
                .orElse(null);
    }

    private static final class Attempt<T> {

        private T val;

        public static <G> Attempt<G> newAttempt() {
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
