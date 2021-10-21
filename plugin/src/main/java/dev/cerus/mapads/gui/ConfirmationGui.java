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
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class ConfirmationGui {

    private static final Renderer YES_RENDERER = () -> new ItemBuilder(Material.GREEN_CONCRETE)
            .setName(L10n.get("gui.confirm.yes"))
            .build();
    private static final Renderer NO_RENDERER = () -> new ItemBuilder(Material.RED_CONCRETE)
            .setName(L10n.get("gui.confirm.no"))
            .build();

    private final String title;
    private final Renderer yesRenderer;
    private final Renderer noRenderer;
    private final Renderer questionRenderer;
    private Runnable yesCallback;
    private Runnable noCallback;

    public ConfirmationGui(final String title, final Renderer questionRenderer) {
        this(title, YES_RENDERER, NO_RENDERER, questionRenderer);
    }

    public ConfirmationGui(final String title, final Renderer yesRenderer, final Renderer noRenderer, final Renderer questionRenderer) {
        this.title = title;
        this.yesRenderer = yesRenderer;
        this.noRenderer = noRenderer;
        this.questionRenderer = questionRenderer;
    }

    public static ConfirmationGui create(final String title, final Renderer questionRenderer) {
        return new ConfirmationGui(title, questionRenderer);
    }

    public static ConfirmationGui create(final String title, final Renderer yesRenderer, final Renderer noRenderer, final Renderer questionRenderer) {
        return new ConfirmationGui(title, yesRenderer, noRenderer, questionRenderer);
    }

    public ConfirmationGui onYes(final Runnable runnable) {
        this.yesCallback = runnable;
        return this;
    }

    public ConfirmationGui onNo(final Runnable runnable) {
        this.noCallback = runnable;
        return this;
    }

    public void open(final Player player) {
        final GUI gui = new GUIBuilder(this.title, 3)
                .withInteractionPolicy(SlotRange.full(), false)
                .withComponents(SlotRange.full(), new Item(new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).setName(" ").build()))
                .withComponents(SlotRange.single(Coordinate.fromSlot(10)), new Button(this.yesRenderer.render(), event -> {
                    if (this.yesCallback != null) {
                        this.yesCallback.run();
                    }
                }))
                .withComponents(SlotRange.single(Coordinate.fromSlot(16)), new Button(this.noRenderer.render(), event -> {
                    if (this.noCallback != null) {
                        this.noCallback.run();
                    }
                }))
                .withComponents(SlotRange.single(Coordinate.fromSlot(13)), new Item(this.questionRenderer.render()))
                .build();
        gui.open(JavaPlugin.getPlugin(MapAdsPlugin.class), player);
    }

    public interface Renderer {

        ItemStack render();

    }

}
