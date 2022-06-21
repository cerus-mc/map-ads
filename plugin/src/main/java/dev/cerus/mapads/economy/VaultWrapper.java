package dev.cerus.mapads.economy;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

public class VaultWrapper implements EconomyWrapper<Economy> {

    private final Economy economy;

    public VaultWrapper(final Economy economy) {
        this.economy = economy;
    }

    public static VaultWrapper attemptCreate() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            throw new IllegalStateException();
        }
        final RegisteredServiceProvider<Economy> registration = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (registration == null) {
            throw new IllegalStateException();
        }
        final Economy economy = registration.getProvider();
        return new VaultWrapper(economy);
    }

    @Override
    public boolean withdraw(final OfflinePlayer player, final double amount) {
        return this.economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    @Override
    public boolean deposit(final OfflinePlayer player, final double amount) {
        return this.economy.depositPlayer(player, amount).transactionSuccess();
    }

    @Override
    public boolean has(final OfflinePlayer player, final double amount) {
        return this.economy.has(player, amount);
    }

    @Override
    public String format(final double val) {
        return this.economy.format(val);
    }

    @Override
    public Economy implementation() {
        return this.economy;
    }

}
