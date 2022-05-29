package dev.cerus.mapads.gui;

import dev.cerus.mapads.advert.Advertisement;
import dev.cerus.mapads.advert.storage.AdvertStorage;
import dev.cerus.mapads.image.storage.ImageStorage;
import dev.cerus.mapads.lang.L10n;
import dev.cerus.mapads.screen.storage.AdScreenStorage;
import dev.cerus.mapads.util.FormatUtil;
import dev.cerus.mapads.util.ItemBuilder;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.stream.Collectors;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class UnreviewedListGui extends PagedGui<Advertisement> {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

    private final ImageStorage imageStorage;
    private final AdScreenStorage adScreenStorage;
    private final AdvertStorage advertStorage;
    private final Economy economy;

    public UnreviewedListGui(final Player player,
                             final ImageStorage imageStorage,
                             final AdScreenStorage adScreenStorage,
                             final AdvertStorage advertStorage,
                             final Economy economy) {
        super(advertStorage.getPendingAdvertisements().stream()
                .sorted(Comparator.comparingLong(Advertisement::getPurchaseTimestamp))
                .collect(Collectors.toList()), player);
        this.imageStorage = imageStorage;
        this.adScreenStorage = adScreenStorage;
        this.advertStorage = advertStorage;
        this.economy = economy;

        this.setClickCallback(this::handleClick);
    }

    private void handleClick(final Advertisement advertisement) {
        final DetailsGui detailsGui = new DetailsGui(this.advertStorage,
                this.imageStorage,
                this.adScreenStorage,
                this.economy,
                this.player,
                advertisement);
        detailsGui.open();
    }

    @Override
    protected String getTitle() {
        return L10n.get("gui.reviewlist.title");
    }

    @Override
    protected int getRows() {
        return 4;
    }

    @Override
    protected ItemStack getDisplayItem(final Advertisement item) {
        return new ItemBuilder(Material.FILLED_MAP)
                .setName("§6Advertisement")
                .setLore(L10n.getList("gui.details.button.info.lore").stream()
                        .map(s -> s.replace("{0}", Bukkit.getOfflinePlayer(item.getPlayerUuid()).getName()))
                        .map(s -> s.replace("{1}", FormatUtil.formatMinutes(item.getPurchasedMinutes())))
                        .map(s -> s.replace("{2}", String.valueOf(item.getPricePaid())))
                        .map(s -> s.replace("{3}", item.getAdScreenId()))
                        .map(s -> s.replace("{4}", DATE_FORMAT.format(item.getPurchaseTimestamp())))
                        .collect(Collectors.toList()))
                .build();
    }

}
