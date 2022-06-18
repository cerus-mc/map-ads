package dev.cerus.mapads.gui;

import dev.cerus.mapads.MapAdsPlugin;
import dev.cerus.mapads.advert.Advertisement;
import dev.cerus.mapads.lang.L10n;
import dev.cerus.mapads.util.ItemBuilder;
import dev.pelkum.yamif.components.Button;
import dev.pelkum.yamif.components.Item;
import dev.pelkum.yamif.grid.Coordinate;
import dev.pelkum.yamif.grid.SlotRange;
import dev.pelkum.yamif.gui.GUI;
import dev.pelkum.yamif.gui.GUIBuilder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class AdDetailsGui {

    private final Advertisement advertisement;
    private final Player player;

    public AdDetailsGui(final Advertisement advertisement, final Player player) {
        this.advertisement = advertisement;
        this.player = player;
    }

    public void open() {
        final GUI gui = new GUIBuilder(L10n.get("gui.advdetails.title"), 3)
                .withInteractionPolicy(SlotRange.full(), false)
                .withComponents(SlotRange.full(), new Item(new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).setName(" ").build()))
                .withComponents(SlotRange.single(Coordinate.fromSlot(10)), new Item(this.getDisplayItem()))
                .withComponents(SlotRange.single(Coordinate.fromSlot(16)), new Button(new ItemBuilder(Material.BARRIER)
                        .setName(L10n.get("gui.advdetails.button.delete.name"))
                        .setLore(L10n.getList("gui.advdetails.button.delete.lore"))
                        .build(), event -> {
                    this.player.closeInventory();
                    this.player.performCommand("mapads advert remove " + this.advertisement.getAdvertId());
                }))
                .withComponents(SlotRange.single(Coordinate.fromSlot(15)), new Button(this.getReviewedItem(), event -> {
                    if (this.advertisement.isReviewed()) {
                        return;
                    }

                    this.player.closeInventory();
                    this.player.performCommand("mapads review single " + this.advertisement.getAdvertId());
                }))
                .build();
        gui.open(JavaPlugin.getPlugin(MapAdsPlugin.class), this.player);
    }

    private ItemStack getDisplayItem() {
        final Function<String, String> replacer = s -> s.replace("{name}", Bukkit.getOfflinePlayer(this.advertisement.getPlayerUuid()).getName())
                .replace("{screen}", this.advertisement.getScreenOrGroupId().unsafeGet())
                .replace("{price}", String.valueOf(this.advertisement.getPricePaid()))
                .replace("{minrem}", String.valueOf(this.advertisement.getRemainingMinutes()))
                .replace("{mintotal}", String.valueOf(this.advertisement.getPurchasedMinutes()))
                .replace("{review}", L10n.get("gui.adverts.item.lore.reviewed." + (this.advertisement.isReviewed() ? "yes" : "no")))
                .replace("{created}", new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new Date(this.advertisement.getPurchaseTimestamp())));
        return new ItemBuilder(Material.PAINTING)
                .setName(replacer.apply(L10n.get("gui.adverts.item.name")))
                .setLore(L10n.getList("gui.adverts.item.lore").stream()
                        .map(replacer)
                        .collect(Collectors.toList()))
                .build();
    }

    private ItemStack getReviewedItem() {
        if (this.advertisement.isReviewed()) {
            return new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).setName(" ").build();
        }
        return new ItemBuilder(Material.NETHER_STAR)
                .setName(L10n.get("gui.advdetails.button.review.name"))
                .setLore(L10n.getList("gui.advdetails.button.review.lore"))
                .build();
    }

}
