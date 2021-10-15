package dev.cerus.mapads.gui;

import dev.cerus.mapads.MapAdsPlugin;
import dev.cerus.mapads.lang.L10n;
import dev.cerus.mapads.screen.AdScreen;
import dev.cerus.mapads.screen.storage.AdScreenStorage;
import dev.cerus.mapads.util.ItemBuilder;
import dev.cerus.maps.api.MapScreen;
import dev.cerus.maps.plugin.map.MapScreenRegistry;
import dev.pelkum.yamif.components.Button;
import dev.pelkum.yamif.components.Item;
import dev.pelkum.yamif.grid.Coordinate;
import dev.pelkum.yamif.grid.SlotRange;
import dev.pelkum.yamif.gui.GUI;
import dev.pelkum.yamif.gui.GUIBuilder;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class AdScreenListGui {

    private final Player player;
    private final List<AdScreen> adScreens;
    private final Consumer<AdScreen> callback;
    private GUI gui;
    private int page;

    public AdScreenListGui(final Player player,
                           final AdScreenStorage adScreenStorage,
                           final Consumer<AdScreen> callback) {
        this(player, adScreenStorage, callback, adScreen -> true);
    }

    public AdScreenListGui(final Player player,
                           final AdScreenStorage adScreenStorage,
                           final Consumer<AdScreen> callback,
                           final Predicate<AdScreen> predicate) {
        this.player = player;
        this.adScreens = adScreenStorage.getScreens().stream()
                .filter(predicate)
                .collect(Collectors.toList());
        this.callback = callback;
    }

    public void open() {
        this.gui = new GUIBuilder(L10n.get("gui.list.title"), 4)
                .withInteractionPolicy(SlotRange.full(), false)
                .withComponents(
                        SlotRange.range(Coordinate.fromSlot(9 * 3), Coordinate.fromSlot(9 * 3 + 8)),
                        new Item(new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).setName(" ").build())
                )
                .build();
        this.setPage();
        this.gui.open(JavaPlugin.getPlugin(MapAdsPlugin.class), this.player);
    }

    private void setPage() {
        this.gui.removeComponents(SlotRange.range(Coordinate.fromSlot(0), Coordinate.fromSlot(3 * 9 - 1)));
        final List<AdScreen> list = this.adScreens.subList(this.page * (3 * 9), Math.min(this.adScreens.size(), this.page * (3 * 9) + (3 * 9)));
        for (int i = 0; i < list.size(); i++) {
            final AdScreen adScreen = list.get(i);
            final MapScreen mapScreen = MapScreenRegistry.getScreen(adScreen.getScreenId());
            this.gui.setComponents(SlotRange.single(Coordinate.fromSlot(i)), new Button(new ItemBuilder(Material.FILLED_MAP)
                    .setName("§6'" + adScreen.getId() + "'")
                    .addLore("§e#" + adScreen.getScreenId())
                    .addLore("§7" + mapScreen.getWidth() + "x" + mapScreen.getHeight()
                            + " (" + mapScreen.getWidth() * 128 + "x" + mapScreen.getHeight() * 128 + " pixel)")
                    .build(), event -> this.callback.accept(adScreen)));
        }

        this.setActionBar();
    }

    private void setActionBar() {
        this.gui.setComponents(
                SlotRange.single(Coordinate.fromSlot(3 * 9 + 4)),
                new Item(new ItemBuilder(Material.NETHER_STAR).setName(L10n.get("gui.page", (this.page + 1))).build())
        );
        this.gui.setComponents(
                SlotRange.single(Coordinate.fromSlot(3 * 9 + 1)),
                new Button(
                        new ItemBuilder(this.hasPage(this.page - 1) ? Material.SNOWBALL : Material.CLAY_BALL)
                                .setName(this.hasPage(this.page - 1) ? L10n.get("gui.prev_page.enabled") : L10n.get("gui.prev_page.disabled"))
                                .build(),
                        event -> {
                            if (this.hasPage(this.page - 1)) {
                                this.page--;
                                this.setPage();
                                this.player.playSound(this.player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1, 1);
                            }
                        }
                )
        );
        this.gui.setComponents(
                SlotRange.single(Coordinate.fromSlot(3 * 9 + 7)),
                new Button(
                        new ItemBuilder(this.hasPage(this.page + 1) ? Material.SNOWBALL : Material.CLAY_BALL)
                                .setName(this.hasPage(this.page + 1) ? L10n.get("gui.next_page.enabled") : L10n.get("gui.next_page.disabled"))
                                .build(),
                        event -> {
                            if (this.hasPage(this.page + 1)) {
                                this.page++;
                                this.setPage();
                                this.player.playSound(this.player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1, 1);
                            }
                        }
                )
        );
    }

    private boolean hasPage(final int page) {
        return page >= 0 && page < Math.ceil(this.adScreens.size() / (3D * 9D));
    }

}
