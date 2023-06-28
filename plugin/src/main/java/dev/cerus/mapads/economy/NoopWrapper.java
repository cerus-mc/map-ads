package dev.cerus.mapads.economy;

import org.bukkit.OfflinePlayer;

public class NoopWrapper implements EconomyWrapper<Void> {

    public static NoopWrapper create() {
        return new NoopWrapper();
    }

    @Override
    public boolean withdraw(OfflinePlayer player, double amount) {
        throw new IllegalStateException("Cannot withdraw when using NoopEconomy. This should not happen!");
    }

    @Override
    public boolean deposit(OfflinePlayer player, double amount) {
        throw new IllegalStateException("Cannot withdraw when using NoopEconomy. This should not happen!");
    }

    @Override
    public boolean has(OfflinePlayer player, double amount) {
        throw new IllegalStateException("Cannot withdraw when using NoopEconomy. This should not happen!");
    }

    @Override
    public String format(double val) {
        throw new IllegalStateException("Cannot withdraw when using NoopEconomy. This should not happen!");
    }

    @Override
    public String currencyNamePlural() {
        throw new IllegalStateException("Cannot withdraw when using NoopEconomy. This should not happen!");
    }

    @Override
    public Void implementation() {
        return null;
    }

    @Override
    public String asString() {
        return this.getClass().getSimpleName();
    }

    @Override
    public boolean isFunctional() {
        return false;
    }
}
