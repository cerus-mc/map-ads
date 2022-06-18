package dev.cerus.mapads.gui;

import dev.cerus.mapads.MapAdsPlugin;
import dev.cerus.mapads.lang.L10n;
import dev.cerus.mapads.screen.AdScreen;
import dev.cerus.mapads.screen.ScreenGroup;
import dev.cerus.mapads.screen.storage.AdScreenStorage;
import dev.cerus.mapads.util.Either;
import dev.cerus.mapads.util.ItemBuilder;
import dev.cerus.maps.api.MapScreen;
import dev.cerus.maps.plugin.map.MapScreenRegistry;
import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.plugin.java.JavaPlugin;

public class AdScreenListGui extends PagedGui<Either<AdScreen, ScreenGroup>> {

    private final AdScreenStorage adScreenStorage;

    public AdScreenListGui(final Player player,
                           final AdScreenStorage adScreenStorage,
                           final Consumer<Either<AdScreen, ScreenGroup>> callback,
                           final boolean groups) {
        this(player, adScreenStorage, callback, adScreen -> true, groups);
    }

    public AdScreenListGui(final Player player,
                           final AdScreenStorage adScreenStorage,
                           final Consumer<Either<AdScreen, ScreenGroup>> callback,
                           final Predicate<AdScreen> predicate,
                           final boolean groups) {
        super(new ArrayList<>() {
            {
                if (!groups || !JavaPlugin.getPlugin(MapAdsPlugin.class).getConfigModel().onlyGroups) {
                    this.addAll(adScreenStorage.getScreens().stream()
                            .filter(predicate)
                            .map(s -> new Either<AdScreen, ScreenGroup>(s, null))
                            .toList());
                }
                if (groups) {
                    this.addAll(adScreenStorage.getScreenGroups().stream()
                            .filter(group -> group.screenIds().stream()
                                    .map(adScreenStorage::getAdScreen)
                                    .filter(Objects::nonNull)
                                    .allMatch(predicate))
                            .map(group -> new Either<AdScreen, ScreenGroup>(null, group))
                            .toList());
                }
            }
        }, player, callback);
        this.adScreenStorage = adScreenStorage;
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
    protected ItemStack getDisplayItem(final Either<AdScreen, ScreenGroup> either) {
        return either.map(item -> {
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
                            + " (" + (mapScreen.getWidth() * 128) + "x" + (mapScreen.getHeight() * 128) + " pixel)")
                    .addLore(" ")
                    .addLore(locationStr)
                    .build();
        }, group -> new ItemBuilder(Material.FILLED_MAP)
                .setName("§d" + group.groupName())
                .setLore("§8» §7" + String.join(", ",
                        group.sizeStrings(this.adScreenStorage)))
                .custom(itemMeta -> {
                    final MapMeta meta = (MapMeta) itemMeta;
                    meta.setColor(Color.SILVER);
                })
                .build());
    }

}
