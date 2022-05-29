package dev.cerus.mapads.gui;

import dev.cerus.mapads.MapAdsPlugin;
import dev.cerus.mapads.lang.L10n;
import dev.cerus.mapads.util.ItemBuilder;
import dev.pelkum.yamif.components.Button;
import dev.pelkum.yamif.components.Item;
import dev.pelkum.yamif.grid.Coordinate;
import dev.pelkum.yamif.grid.SlotRange;
import dev.pelkum.yamif.gui.GUI;
import dev.pelkum.yamif.gui.GUIBuilder;
import java.util.List;
import java.util.function.Consumer;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public abstract class PagedGui<T> {

    protected final List<T> items;
    protected final Player player;
    protected Consumer<T> clickCallback;
    protected GUI gui;
    protected int page;

    public PagedGui(final List<T> items, final Player player) {
        this(items, player, null);
    }

    public PagedGui(final List<T> items, final Player player, final Consumer<T> clickCallback) {
        this.items = items;
        this.player = player;
        this.clickCallback = clickCallback;
    }

    protected abstract String getTitle();

    protected abstract int getRows();

    protected abstract ItemStack getDisplayItem(T item);

    public void open() {
        final int rows = this.getRows();
        this.gui = new GUIBuilder(this.getTitle(), rows)
                .withInteractionPolicy(SlotRange.full(), false)
                .withComponents(
                        SlotRange.range(Coordinate.fromSlot(9 * (rows - 1)), Coordinate.fromSlot((9 * (rows - 1)) + 8)),
                        new Item(new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).setName(" ").build())
                )
                .build();
        this.setPage();
        this.gui.open(JavaPlugin.getPlugin(MapAdsPlugin.class), this.player);
    }

    private void setPage() {
        final int rows = this.getRows();
        this.gui.removeComponents(SlotRange.range(Coordinate.fromSlot(0), Coordinate.fromSlot(((rows - 1) * 9) - 1)));
        final List<T> list = this.items.subList(this.page * ((rows - 1) * 9), Math.min(this.items.size(), this.page * ((rows - 1) * 9) + ((rows - 1) * 9)));
        for (int i = 0; i < list.size(); i++) {
            final T item = list.get(i);
            this.gui.setComponents(SlotRange.single(Coordinate.fromSlot(i)),
                    new Button(this.getDisplayItem(item), event -> this.clickCallback.accept(item)));
        }

        this.setActionBar();
    }

    private void setActionBar() {
        final int rows = this.getRows();
        this.gui.setComponents(
                SlotRange.single(Coordinate.fromSlot(((rows - 1) * 9) + 4)),
                new Item(new ItemBuilder(Material.NETHER_STAR).setName(L10n.get("gui.page", (this.page + 1))).build())
        );
        this.gui.setComponents(
                SlotRange.single(Coordinate.fromSlot(((rows - 1) * 9) + 1)),
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
                SlotRange.single(Coordinate.fromSlot(((rows - 1) * 9) + 7)),
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
        return page >= 0 && page < Math.ceil(this.items.size() / (((double) (this.getRows() - 1)) * 9D));
    }

    protected void setClickCallback(final Consumer<T> clickCallback) {
        this.clickCallback = clickCallback;
    }

}
