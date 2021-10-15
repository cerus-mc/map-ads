package dev.cerus.mapads.gui;

import dev.cerus.mapads.ConfigModel;
import dev.cerus.mapads.MapAdsPlugin;
import dev.cerus.mapads.advert.Advertisement;
import dev.cerus.mapads.advert.storage.AdvertStorage;
import dev.cerus.mapads.api.event.AdvertCreateEvent;
import dev.cerus.mapads.helpbook.HelpBook;
import dev.cerus.mapads.image.ImageConverter;
import dev.cerus.mapads.image.ImageRetriever;
import dev.cerus.mapads.image.MapImage;
import dev.cerus.mapads.image.storage.ImageStorage;
import dev.cerus.mapads.lang.L10n;
import dev.cerus.mapads.screen.AdScreen;
import dev.cerus.mapads.screen.storage.AdScreenStorage;
import dev.cerus.mapads.util.FormatUtil;
import dev.cerus.mapads.util.ItemBuilder;
import dev.cerus.mapads.util.UrlVerificationUtil;
import dev.cerus.maps.api.MapScreen;
import dev.cerus.maps.plugin.map.MapScreenRegistry;
import dev.pelkum.yamif.components.Button;
import dev.pelkum.yamif.components.Component;
import dev.pelkum.yamif.components.Item;
import dev.pelkum.yamif.grid.Coordinate;
import dev.pelkum.yamif.grid.SlotRange;
import dev.pelkum.yamif.gui.GUI;
import dev.pelkum.yamif.gui.GUIBuilder;
import java.awt.image.BufferedImage;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import net.milkbowl.vault.economy.Economy;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

// WARNING: the code that follows will make you cry; a safety pig is provided below for your benefit.
//  _._ _..._ .-',     _.._(`))
// '-. `     '  /-._.-'    ',/
//    )         \            '.
//   / _    _    |             \
//  |  a    a    /              |
//  \   .-.                     ;
//   '-('' ).-'       ,'       ;
//      '-;           |      .'
//         \           \    /
//         | 7  .__  _.-\   \
//         | |  |  ``/  /`  /
//        /,_|  |   /,_/   /
//           /,_/      '`-'
// This will need some work in the future

public class CreateAdGui {

    private static final ItemStack BTN_MAP = new ItemBuilder(Material.MAP).setName(L10n.get("gui.create.button.map.name")).build();
    private static final ItemStack BTN_FRAME = new ItemBuilder(Material.ITEM_FRAME).setName(L10n.get("gui.create.button.frame.name")).build();
    private static final ItemStack BTN_GLOW_FRAME = new ItemBuilder(Material.GLOW_ITEM_FRAME).setName(L10n.get("gui.create.button.glow_frame.name")).build();

    private final AdvertStorage advertStorage;
    private final ImageStorage imageStorage;
    private final AdScreenStorage adScreenStorage;
    private final ImageRetriever imageRetriever;
    private final ImageConverter imageConverter;
    private final ConfigModel config;
    private final Economy economy;
    private final Player player;
    private final Context context;
    private GUI gui;
    private State state;

    public CreateAdGui(final AdvertStorage advertStorage,
                       final ImageStorage imageStorage,
                       final AdScreenStorage adScreenStorage,
                       final ImageRetriever imageRetriever,
                       final ImageConverter imageConverter,
                       final ConfigModel config,
                       final Economy economy,
                       final Player player) {
        this.advertStorage = advertStorage;
        this.imageStorage = imageStorage;
        this.adScreenStorage = adScreenStorage;
        this.imageRetriever = imageRetriever;
        this.imageConverter = imageConverter;
        this.config = config;
        this.economy = economy;
        this.player = player;
        this.context = new Context();
        this.context.selectedMinutes = config.minAdMins;
        this.context.dither = ImageConverter.Dither.NONE;
        this.state = State.EDITING;
    }

    public void open() {
        this.gui = new GUIBuilder("Create advertisement", 5)
                .withComponents(SlotRange.full(), this.makeGlassPane(Material.GRAY_STAINED_GLASS_PANE))
                .withComponents(SlotRange.row(0), this.makeGlassPane(Material.BLACK_STAINED_GLASS_PANE))
                .withComponents(SlotRange.row(4), this.makeGlassPane(Material.BLACK_STAINED_GLASS_PANE))
                .withComponents(SlotRange.column(0), this.makeGlassPane(Material.BLACK_STAINED_GLASS_PANE))
                .withComponents(SlotRange.column(8), this.makeGlassPane(Material.BLACK_STAINED_GLASS_PANE))
                .allowShiftClick()
                .doOnClose(inventoryCloseEvent -> {
                    if (this.state == State.CONFIRMING) {
                        Bukkit.getScheduler().runTaskLater(JavaPlugin.getPlugin(MapAdsPlugin.class), this::open, 1);
                    }
                })
                .build();
        this.updateGui();
        this.gui.open(JavaPlugin.getPlugin(MapAdsPlugin.class), this.player);

        this.player.playSound(this.player.getLocation(), Sound.BLOCK_CHEST_OPEN, 1, 1);
    }

    private void updateGui() {
        this.gui.setComponents(SlotRange.single(Coordinate.fromSlot(10)), new Button(this.makeMapBtn(), event -> {
            if (this.state == State.CONFIRMING) {
                return;
            }

            this.context.image = null;
            this.context.imgUrl = null;

            this.player.playSound(this.player.getLocation(), Sound.ENTITY_CHICKEN_EGG, 1, 1);
            final AdScreenListGui listGui = new AdScreenListGui(this.player, this.adScreenStorage, adScreen -> {
                if (adScreen != null) {
                    this.context.adScreen = adScreen;
                    this.context.mapScreen = MapScreenRegistry.getScreen(adScreen.getScreenId());
                    this.player.playSound(this.player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
                }
                this.state = State.EDITING;
                this.open();
            });
            listGui.open();
        }));
        this.gui.setComponents(SlotRange.single(Coordinate.fromSlot(11)), new Button(this.makeAmethystBtn(), event -> {
            if (this.state == State.CONFIRMING) {
                return;
            }

            if (this.context.dither == ImageConverter.Dither.NONE) {
                this.context.dither = ImageConverter.Dither.FLOYD_STEINBERG;
            } else {
                this.context.dither = ImageConverter.Dither.NONE;
            }

            this.player.playSound(this.player.getLocation(), Sound.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, 1, 1);
            this.updateGui();
        }));
        this.gui.setComponents(SlotRange.single(Coordinate.fromSlot(12)), new Button(this.makeFrameBtn(), event -> {
            if (this.state == State.CONFIRMING) {
                return;
            }
            if (this.context.adScreen == null || this.context.mapScreen == null) {
                this.raiseError(L10n.get("gui.create.error.choose_screen_first"));
                return;
            }

            this.player.playSound(this.player.getLocation(), Sound.ENTITY_CHICKEN_EGG, 1, 1);
            new AnvilGUI.Builder()
                    .plugin(JavaPlugin.getPlugin(MapAdsPlugin.class))
                    .onComplete((plr, s) -> {
                        Bukkit.getScheduler().runTaskLater(JavaPlugin.getPlugin(MapAdsPlugin.class), () -> {
                            final URI uri;
                            try {
                                uri = URI.create(s);
                            } catch (final IllegalArgumentException ignored) {
                                this.raiseError(L10n.get("gui.create.error.invalid_url"));
                                this.updateGui();
                                return;
                            }

                            final String host = uri.getHost();
                            if (host == null) {
                                this.raiseError(L10n.get("gui.create.error.invalid_url"));
                                this.updateGui();
                                return;
                            }

                            if (!UrlVerificationUtil.verify(host, this.config)) {
                                this.raiseError(L10n.get("gui.create.error.untrusted_site"));
                                this.updateGui();
                                return;
                            }

                            this.state = State.CONFIRMING;
                            this.updateGui();
                            this.imageRetriever.getImageAsync(uri.toString(), this.config.maxImageSize).whenComplete((result, throwable) -> {
                                if (result.getErr() != null) {
                                    this.state = State.ERROR_IMAGE;
                                    this.context.errorState = 3;
                                    this.raiseError(result.getErr().getMessage());
                                    this.updateGui();
                                    return;
                                }
                                if (!result.isImage()) {
                                    this.state = State.ERROR_IMAGE;
                                    this.context.errorState = 4;
                                    if (result.getType() == null) {
                                        this.raiseError(L10n.get("gui.create.error.not_image"));
                                    } else if (result.getType().startsWith("image")) {
                                        this.raiseError(L10n.get("gui.create.error.content_not_image"));
                                    } else {
                                        this.raiseError(L10n.get("gui.create.error.content_unsupported_image"));
                                    }
                                    this.updateGui();
                                    return;
                                }
                                final BufferedImage image = result.getImage();
                                if (image == null) {
                                    this.state = State.ERROR_IMAGE;
                                    this.context.errorState = 1;
                                    this.raiseError(L10n.get("gui.create.error.too_big", FormatUtil.formatSize(this.config.maxImageSize),
                                            FormatUtil.formatSize(result.getLen())));
                                    this.updateGui();
                                    return;
                                }
                                if (image.getWidth() % 128 != 0 || image.getHeight() % 128 != 0
                                        || image.getWidth() / 128 != this.context.mapScreen.getWidth()
                                        || image.getHeight() / 128 != this.context.mapScreen.getHeight()) {
                                    this.state = State.ERROR_IMAGE;
                                    this.context.errorState = 2;
                                    this.raiseError(L10n.get("gui.create.error.invalid_dimensions",
                                            this.context.mapScreen.getWidth() * 128, this.context.mapScreen.getHeight() * 128,
                                            image.getWidth(), image.getHeight()));
                                    this.updateGui();
                                    return;
                                }
                                this.imageConverter.convertAsync(image, this.context.dither).whenComplete((mapImage, throwable1) -> {
                                    this.state = State.EDITING;
                                    this.context.imgUrl = uri.toString();
                                    this.context.image = mapImage;
                                    this.updateGui();
                                    this.player.playSound(this.player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
                                });
                            });
                        }, 3);
                        return AnvilGUI.Response.close();
                    })
                    .text(L10n.get("gui.create.misc.enter_url"))
                    .onClose(plr -> Bukkit.getScheduler().runTask(JavaPlugin.getPlugin(MapAdsPlugin.class), this::open))
                    .open(this.player);
        }));
        this.gui.setComponents(SlotRange.single(Coordinate.fromSlot(36)), new Button(new ItemBuilder(Material.KNOWLEDGE_BOOK)
                .setName(L10n.get("gui.create.button.book.name"))
                .setLore(L10n.getList("gui.create.button.book.lore"))
                .build(), event -> {
            this.player.closeInventory();
            HelpBook.open(this.player);
        }));
        this.setMinuteSelector();
        this.gui.setComponents(SlotRange.single(Coordinate.fromSlot(31)), new Item(new ItemBuilder(Material.GOLD_INGOT)
                .setName(L10n.get("gui.create.button.gold.name", this.config.pricePerMin * this.context.selectedMinutes))
                .build()));
        this.setConcreteButtons();
    }

    private ItemStack makeAmethystBtn() {
        return new ItemBuilder(Material.AMETHYST_SHARD)
                .setName(L10n.get("gui.create.button.amethyst.name"))
                .setLore(L10n.getList("gui.create.button.amethyst.lore").stream()
                        .map(s -> s.replace("{0}", switch (this.context.dither) {
                            case NONE -> "None";
                            case FLOYD_STEINBERG -> "Floyd-Steinberg";
                            default -> "Unknown";
                        }))
                        .collect(Collectors.toList()))
                .build();
    }

    private void raiseError(final String err) {
        if (err == null) {
            this.gui.setComponents(SlotRange.single(Coordinate.fromSlot(28)), this.makeGlassPane(Material.GRAY_STAINED_GLASS_PANE));
            return;
        }

        final List<String> lore = new ArrayList<>();
        StringBuilder buf = new StringBuilder("§7");
        for (final String s : err.split("\\s+")) {
            buf.append(s).append(" ");
            if (buf.length() >= 30) {
                lore.add(buf.toString());
                buf = new StringBuilder("§7");
            }
        }
        lore.add(buf.toString());

        this.gui.setComponents(SlotRange.single(Coordinate.fromSlot(28)), new Item(new ItemBuilder(Material.BELL)
                .setName("§cError")
                .addLore(lore.toArray(new String[0]))
                .build()));
        this.player.playSound(this.player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1, 1);
    }

    private void setConcreteButtons() {
        if (this.state == State.CONFIRMING) {
            this.gui.setComponents(SlotRange.single(Coordinate.fromSlot(33)), new Item(new ItemBuilder(Material.LIGHT_GRAY_CONCRETE)
                    .setName(L10n.get("gui.create.button.purchase.name.disabled"))
                    .build()));
            this.gui.setComponents(SlotRange.single(Coordinate.fromSlot(34)), new Item(new ItemBuilder(Material.GRAY_CONCRETE)
                    .setName(L10n.get("gui.create.button.cancel.name.disabled"))
                    .build()));
        } else {
            this.gui.setComponents(SlotRange.single(Coordinate.fromSlot(33)), new Button(new ItemBuilder(Material.LIME_CONCRETE)
                    .setName(L10n.get("gui.create.button.purchase.name.enabled"))
                    .build(), event -> {
                this.raiseError(null);
                if (this.context.adScreen == null || this.context.mapScreen == null) {
                    this.raiseError(L10n.get("gui.create.error.choose_screen_first"));
                    return;
                }
                if (this.context.image == null) {
                    this.raiseError(L10n.get("gui.create.error.choose_image_first"));
                    return;
                }

                if (!this.economy.has(this.player, this.context.selectedMinutes * this.config.pricePerMin)) {
                    this.raiseError(L10n.get("gui.create.error.no_money"));
                    return;
                }

                this.state = State.CONFIRMING;
                this.updateGui();

                final Advertisement advertisement = new Advertisement(UUID.randomUUID(),
                        this.player.getUniqueId(),
                        this.context.image.getId(),
                        this.context.adScreen.getId(),
                        System.currentTimeMillis(),
                        this.context.selectedMinutes,
                        this.context.selectedMinutes,
                        this.context.selectedMinutes * this.config.pricePerMin,
                        false);
                final AdvertCreateEvent ev = new AdvertCreateEvent(this.player, advertisement);
                Bukkit.getPluginManager().callEvent(ev);
                if (ev.isCancelled()) {
                    this.state = State.EDITING;
                    this.raiseError(L10n.get("api.event.create.cancelled"));
                    this.updateGui();
                    return;
                }

                this.economy.withdrawPlayer(this.player, this.context.selectedMinutes * this.config.pricePerMin);
                this.imageStorage.updateMapImage(this.context.image).whenComplete((o, errImg) -> {
                    this.advertStorage.updateAdvert(advertisement).whenComplete((oo, errAdv) -> {
                        Bukkit.getScheduler().runTask(JavaPlugin.getPlugin(MapAdsPlugin.class), () -> {
                            this.state = State.EDITING;
                            this.player.closeInventory();
                            this.player.playSound(this.player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1);

                            Bukkit.getOnlinePlayers().forEach(plr -> {
                                if (plr.hasPermission("mapads.admin")) {
                                    plr.sendMessage(L10n.getPrefixed("gui.create.misc.broadcast"));
                                }
                            });
                        });
                    });
                });
            }));
            this.gui.setComponents(SlotRange.single(Coordinate.fromSlot(34)), new Button(new ItemBuilder(Material.RED_CONCRETE)
                    .setName(L10n.get("gui.create.button.cancel.name.enabled"))
                    .build(), event -> {
                this.player.closeInventory();
                this.player.playSound(this.player.getLocation(), Sound.BLOCK_CHEST_CLOSE, 1, 1);
            }));
        }
    }

    private void setMinuteSelector() {
        if (this.context.selectedMinutes <= this.config.minAdMins || this.state == State.CONFIRMING) {
            this.gui.setComponents(SlotRange.single(Coordinate.fromSlot(14)), this.makeGlassPane(Material.GRAY_STAINED_GLASS_PANE));
        } else {
            this.gui.setComponents(SlotRange.single(Coordinate.fromSlot(14)), new Button(new ItemBuilder(Material.IRON_NUGGET)
                    .setName(L10n.get("gui.create.button.minus_min.name"))
                    .setLore(L10n.getList("gui.create.button.minus_min.lore").stream()
                            .map(s -> s.replace("{0}", String.valueOf(this.config.minsStep)))
                            .collect(Collectors.toList()))
                    .build(), event -> {
                if (event.isShiftClick()) {
                    this.context.selectedMinutes = this.config.minAdMins;
                } else {
                    this.context.selectedMinutes = Math.max(this.config.minAdMins,
                            this.context.selectedMinutes - this.config.minsStep);
                }
                this.player.playSound(this.player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1, 0.9f);
                this.setMinuteSelector();
            }));
        }

        if (this.context.selectedMinutes >= this.config.maxAdMins || this.state == State.CONFIRMING) {
            this.gui.setComponents(SlotRange.single(Coordinate.fromSlot(16)), this.makeGlassPane(Material.GRAY_STAINED_GLASS_PANE));
        } else {
            this.gui.setComponents(SlotRange.single(Coordinate.fromSlot(16)), new Button(new ItemBuilder(Material.GOLD_NUGGET)
                    .setName(L10n.get("gui.create.button.plus_min.name"))
                    .setLore(L10n.getList("gui.create.button.plus_min.lore").stream()
                            .map(s -> s.replace("{0}", String.valueOf(this.config.minsStep)))
                            .collect(Collectors.toList()))
                    .build(), event -> {
                if (event.isShiftClick()) {
                    this.context.selectedMinutes = this.config.maxAdMins;
                } else {
                    this.context.selectedMinutes = Math.min(this.config.maxAdMins,
                            this.context.selectedMinutes + this.config.minsStep);
                }
                this.player.playSound(this.player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1, 1.1f);
                this.setMinuteSelector();
            }));
        }

        this.gui.setComponents(SlotRange.single(Coordinate.fromSlot(15)), new Item(new ItemBuilder(Material.CLOCK)
                .setName(L10n.get("gui.create.button.info_min.name", this.context.selectedMinutes))
                .setLore(L10n.getList("gui.create.button.info_min.lore").stream()
                        .map(s -> s.replace("{0}", String.valueOf(this.config.minAdMins)))
                        .map(s -> s.replace("{1}", String.valueOf(this.config.maxAdMins)))
                        .collect(Collectors.toList()))
                .build()));
    }

    private ItemStack makeFrameBtn() {
        return switch (this.state) {
            case ERROR_IMAGE -> new ItemBuilder(BTN_FRAME).setLore(L10n.get("gui.create.error_image."
                    + (this.context.errorState > 4 ? "def" : this.context.errorState))).build();
            case CONFIRMING -> new ItemBuilder(BTN_FRAME).setLore(L10n.get("gui.create.misc.confirming_image")).build();
            default -> this.context.image != null
                    ? new ItemBuilder(BTN_GLOW_FRAME).setLore(L10n.getList("gui.create.button.glow_frame.lore").stream()
                    .map(s -> s.replace("{0}", String.valueOf(this.context.imgUrl)))
                    .collect(Collectors.toList())).build()
                    : new ItemBuilder(BTN_FRAME).setLore(L10n.getList("gui.create.button.frame.lore").stream()
                    .map(s -> s.replace("{0}", String.join(", ", this.config.trustedImageUrls)))
                    .collect(Collectors.toList())).build();
        };
    }

    private ItemStack makeMapBtn() {
        if (this.context.adScreen == null || this.context.mapScreen == null) {
            return new ItemBuilder(BTN_MAP).setLore(L10n.getList("gui.create.button.map.lore.noscreen")).build();
        }
        return new ItemBuilder(BTN_MAP).setLore(L10n.getList("gui.create.button.map.lore.screen").stream()
                .map(s -> s.replace("{0}", this.context.adScreen.getId()))
                .map(s -> s.replace("{1}", String.valueOf(this.context.mapScreen.getWidth())))
                .map(s -> s.replace("{2}", String.valueOf(this.context.mapScreen.getHeight())))
                .collect(Collectors.toList())).build();
    }

    private Component makeGlassPane(final Material material) {
        return new Item(new ItemBuilder(material).setName(" ").build());
    }

    private enum State {
        EDITING, CONFIRMING, ERROR_IMAGE, ERROR_PRICE
    }

    private class Context {

        public String imgUrl;
        public MapImage image;
        public AdScreen adScreen;
        public MapScreen mapScreen;
        public int selectedMinutes;
        public ImageConverter.Dither dither;
        // ERROR_IMAGE: 0 = Url not trusted, 1 = Image too big, 2 = Invalid proportions, 3 = Unknown, 4 = Not img
        // ERROR_PRICE: 0 = Not enough money
        public int errorState;

    }

}
