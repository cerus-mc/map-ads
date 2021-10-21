package dev.cerus.mapads.gui;

import dev.cerus.mapads.MapAdsPlugin;
import dev.cerus.mapads.advert.Advertisement;
import dev.cerus.mapads.advert.storage.AdvertStorage;
import dev.cerus.mapads.image.storage.ImageStorage;
import dev.cerus.mapads.lang.L10n;
import dev.cerus.mapads.screen.storage.AdScreenStorage;
import dev.cerus.mapads.util.ItemBuilder;
import dev.pelkum.yamif.components.Button;
import dev.pelkum.yamif.components.Item;
import dev.pelkum.yamif.grid.Coordinate;
import dev.pelkum.yamif.grid.SlotRange;
import dev.pelkum.yamif.gui.GUI;
import dev.pelkum.yamif.gui.GUIBuilder;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class UnreviewedListGui {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

    private final Player player;
    private final List<Advertisement> pendingAdverts;
    private final ImageStorage imageStorage;
    private final AdScreenStorage adScreenStorage;
    private final AdvertStorage advertStorage;
    private final Economy economy;
    private GUI gui;
    private int page;

    public UnreviewedListGui(final Player player,
                             final ImageStorage imageStorage,
                             final AdScreenStorage adScreenStorage,
                             final AdvertStorage advertStorage,
                             final Economy economy) {
        this.player = player;
        this.imageStorage = imageStorage;
        this.adScreenStorage = adScreenStorage;
        this.advertStorage = advertStorage;
        this.economy = economy;
        this.pendingAdverts = this.advertStorage.getPendingAdvertisements().stream()
                .sorted(Comparator.comparingLong(Advertisement::getPurchaseTimestamp))
                .collect(Collectors.toList());
    }

    public void open() {
        this.gui = new GUIBuilder(L10n.get("gui.reviewlist.title"), 4)
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
        final List<Advertisement> list = this.pendingAdverts.subList(this.page * (3 * 9), Math.min(this.pendingAdverts.size(), this.page * (3 * 9) + (3 * 9)));
        for (int i = 0; i < list.size(); i++) {
            final Advertisement advertisement = list.get(i);
            this.gui.setComponents(SlotRange.single(Coordinate.fromSlot(i)), new Button(new ItemBuilder(Material.FILLED_MAP)
                    .setName("§6Advertisement")
                    .setLore(L10n.getList("gui.details.button.info.lore").stream()
                            .map(s -> s.replace("{0}", Bukkit.getOfflinePlayer(advertisement.getPlayerUuid()).getName()))
                            .map(s -> s.replace("{1}", String.valueOf(advertisement.getPurchasedMinutes())))
                            .map(s -> s.replace("{2}", String.valueOf(advertisement.getPricePaid())))
                            .map(s -> s.replace("{3}", advertisement.getAdScreenId()))
                            .map(s -> s.replace("{4}", DATE_FORMAT.format(advertisement.getPurchaseTimestamp())))
                            .collect(Collectors.toList()))
                    .build(), event -> {
                final DetailsGui detailsGui = new DetailsGui(this.advertStorage,
                        this.imageStorage,
                        this.adScreenStorage,
                        this.economy,
                        this.player,
                        advertisement);
                detailsGui.open();
            }));
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
        return page >= 0 && page < Math.ceil(this.pendingAdverts.size() / (3D * 9D));
    }

}
