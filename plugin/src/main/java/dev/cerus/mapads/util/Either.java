package dev.cerus.mapads.util;

import java.util.function.Consumer;
import java.util.function.Function;

public class Either<A, B> {

    private final A a;
    private final B b;

    public Either(final A a, final B b) {
        if (b == null && a == null) {
            throw new IllegalArgumentException("One non null value is required");
        }
        if (a != null && b != null) {
            throw new IllegalArgumentException("Only one value can be non null");
        }
        this.a = a;
        this.b = b;
    }

    public <F, S> Either<F, S> mapToEither(final Function<A, F> funA, final Function<B, S> funB) {
        final F first = this.a == null ? null : funA.apply(this.a);
        final S second = this.b == null ? null : funB.apply(this.b);
        return new Either<>(first, second);
    }

    public <T> T map(final Function<A, T> funA, final Function<B, T> funB) {
        if (this.a != null) {
            return funA.apply(this.a);
        } else if (this.b != null) {
            return funB.apply(this.b);
        } else {
            throw new IllegalStateException();
        }
    }

    public void get(final Consumer<A> onA, final Consumer<B> onB) {
        if (this.a != null) {
            onA.accept(this.a);
        }
        if (this.b != null) {
            onB.accept(this.b);
        }
    }

    public Object get() {
        return this.a == null ? this.b : this.a;
    }

    public <T> T unsafeGet() {
        return (T) this.get();
    }

}
