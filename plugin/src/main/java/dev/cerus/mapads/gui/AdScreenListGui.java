package dev.cerus.mapads.gui;

import dev.cerus.mapads.lang.L10n;
import dev.cerus.mapads.screen.AdScreen;
import dev.cerus.mapads.screen.storage.AdScreenStorage;
import dev.cerus.mapads.util.ItemBuilder;
import dev.cerus.maps.api.MapScreen;
import dev.cerus.maps.plugin.map.MapScreenRegistry;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class AdScreenListGui extends PagedGui<AdScreen> {

    public AdScreenListGui(final Player player,
                           final AdScreenStorage adScreenStorage,
                           final Consumer<AdScreen> callback) {
        this(player, adScreenStorage, callback, adScreen -> true);
    }

    public AdScreenListGui(final Player player,
                           final AdScreenStorage adScreenStorage,
                           final Consumer<AdScreen> callback,
                           final Predicate<AdScreen> predicate) {
        super(adScreenStorage.getScreens().stream()
                .filter(predicate)
                .collect(Collectors.toList()), player, callback);
    }

    @Override
    protected String getTitle() {
        return L10n.get("gui.list.title");
    }

    @Override
    protected int getRows() {
        return 4;
    }

    @Override
    protected ItemStack getDisplayItem(final AdScreen item) {
        final MapScreen mapScreen = MapScreenRegistry.getScreen(item.getScreenId());
        final Location location = mapScreen.getLocation();
        final String locationStr = "§7§o@ " + (this.player.getWorld().getName().equals(location.getWorld().getName())
                ? "" : "(" + location.getWorld().getName() + ") ")
                + location.getBlockX() + " "
                + location.getBlockY() + " "
                + location.getBlockZ() + " ";
        return new ItemBuilder(Material.FILLED_MAP)
                .setName("§6'" + item.getId() + "'")
                .addLore("§e#" + item.getScreenId())
                .addLore("§7" + mapScreen.getWidth() + "x" + mapScreen.getHeight()
                        + " (" + mapScreen.getWidth() * 128 + "x" + mapScreen.getHeight() * 128 + " pixel)")
                .addLore(" ")
                .addLore(locationStr)
                .build();
    }

}
