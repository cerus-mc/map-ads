package dev.cerus.mapads.helpbook;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

public class HelpBook {

    private static List<BaseComponent[]> pages;
    private static ItemStack book;

    public static void init(final HelpBookConfiguration configuration) {
        HelpBook.pages = new ArrayList<>();
        for (final String page : configuration.getPages()) {
            final Component component = MiniMessage.get().parse(page);
            final BaseComponent[] serialized = BungeeComponentSerializer.get().serialize(component);
            HelpBook.pages.add(serialized);
        }

        book = new ItemStack(Material.WRITTEN_BOOK);
        final BookMeta meta = (BookMeta) book.getItemMeta();
        meta.spigot().setPages(HelpBook.pages);
        meta.setGeneration(BookMeta.Generation.TATTERED);
        meta.setTitle(configuration.getTitle());
        meta.setAuthor(configuration.getAuthor());
        book.setItemMeta(meta);
    }

    public static void open(final Player player) {
        player.openBook(book);
    }

}
