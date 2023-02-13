package dev.cerus.mapads.economy;

import me.activated.core.plugin.AquaCoreAPI;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;

public class AquaCoreWrapper implements EconomyWrapper<AquaCoreAPI> {

    public static AquaCoreWrapper attemptCreate() {
        final Plugin plugin = Bukkit.getPluginManager().getPlugin("AquaCore");
        if (plugin == null) {
            throw new IllegalStateException();
        }
        return new AquaCoreWrapper();
    }

    @Override
    public boolean withdraw(final OfflinePlayer player, final double amount) {
        if (player.isOnline()) {
            this.implementation().getPlayerData(player.getUniqueId()).removeCoins((int) amount);
            return true;
        }
        return false;
    }

    @Override
    public boolean deposit(final OfflinePlayer player, final double amount) {
        if (player.isOnline()) {
            this.implementation().getPlayerData(player.getUniqueId()).addCoins((int) amount);
            return true;
        }
        return false;
    }

    @Override
    public boolean has(final OfflinePlayer player, final double amount) {
        return player.isOnline() && this.implementation().getPlayerData(player.getUniqueId()).getCoins() >= amount;
    }

    @Override
    public String format(final double val) {
        return String.format("%,.2f", val);
    }

    @Override
    public String currencyNamePlural() {
        return "Coins";
    }

    @Override
    public AquaCoreAPI implementation() {
        return AquaCoreAPI.INSTANCE;
    }

}
