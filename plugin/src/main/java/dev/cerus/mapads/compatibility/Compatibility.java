package dev.cerus.mapads.compatibility;

import dev.cerus.mapads.ConfigModel;
import dev.cerus.mapads.MapAdsPlugin;
import dev.cerus.mapads.screen.storage.AdScreenStorage;
import dev.cerus.mapads.util.FrameMarkerUtil;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public abstract class Compatibility implements Listener {

    protected static final Logger LOGGER = JavaPlugin.getPlugin(MapAdsPlugin.class).getLogger();

    private final ConfigModel configModel;
    private final AdScreenStorage adScreenStorage;

    public Compatibility(final ConfigModel configModel, final AdScreenStorage adScreenStorage) {
        this.configModel = configModel;
        this.adScreenStorage = adScreenStorage;

        Bukkit.getPluginManager().registerEvents(this, JavaPlugin.getPlugin(MapAdsPlugin.class));
    }

    @EventHandler
    public void onJoin(final PlayerJoinEvent event) {
        if (this.configModel.enableCustomDespawning) {
            this.inject(event.getPlayer());
        }
    }

    public abstract void inject(Player player);

    protected boolean cancelFrameDespawn(final int eid) {
        return this.configModel.enableCustomDespawning && FrameMarkerUtil.isFrameMarked(eid);
    }

    public abstract void despawnEntity(Player player, int eid);

    public abstract void spawnEntity(Player player, Entity entity);

    public abstract void allowMetas(int... ids);

    public abstract Entity getEntity(World world, int entityId);

}
