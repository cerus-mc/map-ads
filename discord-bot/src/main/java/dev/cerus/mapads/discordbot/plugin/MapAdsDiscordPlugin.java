package dev.cerus.mapads.discordbot.plugin;

import org.bukkit.plugin.java.JavaPlugin;

public class MapAdsDiscordPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        this.getLogger().info("Map-Ads Discord extension was enabled!");

        this.getServer().getScheduler().runTaskLater(this, () -> {
            if (!this.getServer().getPluginManager().isPluginEnabled("map-ads")) {
                this.getLogger().warning("Map-Ads plugin was not found! Disabling");
                this.getServer().getPluginManager().disablePlugin(this);
            }
        }, 20);
    }

}
