package dev.cerus.mapads.util;

public class Mutable<T> {

    private T obj;

    public Mutable(final T obj) {
        this.obj = obj;
    }

    public static <T> Mutable<T> empty() {
        return create(null);
    }

    public static <T> Mutable<T> create(final T v) {
        return new Mutable<>(v);
    }

    public void set(final T t) {
        this.obj = t;
    }

    public T get() {
        return this.obj;
    }

    public <G> G getAs() {
        return (G) this.get();
    }

}
