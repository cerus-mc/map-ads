package dev.cerus.mapads.economy;

import org.bukkit.OfflinePlayer;

public interface EconomyWrapper<T> {

    boolean withdraw(OfflinePlayer player, double amount);

    boolean deposit(OfflinePlayer player, double amount);

    boolean has(OfflinePlayer player, double amount);

    String format(double val);

    T implementation();

    default String asString() {
        return this.getClass().getSimpleName() + "<" + this.implementation().getClass().getName() + ">";
    }

}
