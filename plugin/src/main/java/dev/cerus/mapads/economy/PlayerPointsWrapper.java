package dev.cerus.mapads.economy;

import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.black_ixx.playerpoints.util.PointsUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;

public class PlayerPointsWrapper implements EconomyWrapper<PlayerPointsAPI> {

    private final PlayerPointsAPI api;

    public PlayerPointsWrapper() {
        this.api = PlayerPoints.getInstance().getAPI();
    }

    public static PlayerPointsWrapper attemptCreate() {
        final Plugin plugin = Bukkit.getPluginManager().getPlugin("PlayerPoints");
        if (plugin == null) {
            throw new IllegalStateException();
        }
        return new PlayerPointsWrapper();
    }

    @Override
    public boolean withdraw(final OfflinePlayer player, final double amount) {
        return this.api.take(player.getUniqueId(), (int) amount);
    }

    @Override
    public boolean deposit(final OfflinePlayer player, final double amount) {
        return this.api.give(player.getUniqueId(), (int) amount);
    }

    @Override
    public boolean has(final OfflinePlayer player, final double amount) {
        return this.api.look(player.getUniqueId()) >= amount;
    }

    @Override
    public String format(final double val) {
        return PointsUtils.formatPoints((long) val);
    }

    @Override
    public PlayerPointsAPI implementation() {
        return this.api;
    }

}
