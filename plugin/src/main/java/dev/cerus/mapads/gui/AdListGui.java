package dev.cerus.mapads.gui;

import dev.cerus.mapads.advert.Advertisement;
import dev.cerus.mapads.advert.storage.AdvertStorage;
import dev.cerus.mapads.lang.L10n;
import dev.cerus.mapads.screen.AdScreen;
import dev.cerus.mapads.screen.storage.AdScreenStorage;
import dev.cerus.mapads.util.ItemBuilder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class AdListGui extends PagedGui<Advertisement> {

    public AdListGui(final AdScreenStorage adScreenStorage,
                     final AdvertStorage storage,
                     final AdScreen screen,
                     final Player player,
                     final Consumer<Advertisement> clickCallback) {
        super(new ArrayList<>() {
            {
                this.addAll(storage.getAdvertisements(screen.getId()));
                this.addAll(storage.getPendingAdvertisements().stream()
                        .filter(advertisement -> advertisement.getScreenOrGroupId().map(
                                s -> s.equals(screen.getId()),
                                s -> adScreenStorage.getScreenGroup(s).screenIds().contains(screen.getId())
                        ))
                        .toList());
            }
        }, player, clickCallback);
    }

    @Override
    protected String getTitle() {
        return L10n.get("gui.adverts.title");
    }

    @Override
    protected int getRows() {
        return 4;
    }

    @Override
    protected ItemStack getDisplayItem(final Advertisement item) {
        final Function<String, String> replacer = s -> s.replace("{name}", Bukkit.getOfflinePlayer(item.getPlayerUuid()).getName())
                .replace("{screen}", item.getScreenOrGroupId().unsafeGet())
                .replace("{price}", String.valueOf(item.getPricePaid()))
                .replace("{minrem}", String.valueOf(item.getRemainingMinutes()))
                .replace("{mintotal}", String.valueOf(item.getPurchasedMinutes()))
                .replace("{review}", L10n.get("gui.adverts.item.lore.reviewed." + (item.isReviewed() ? "yes" : "no")))
                .replace("{created}", new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new Date(item.getPurchaseTimestamp())));
        return new ItemBuilder(Material.PAINTING)
                .setName(replacer.apply(L10n.get("gui.adverts.item.name")))
                .setLore(L10n.getList("gui.adverts.item.lore").stream()
                        .map(replacer)
                        .collect(Collectors.toList()))
                .build();
    }

}
