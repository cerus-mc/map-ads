package dev.cerus.mapads.helpbook;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Bukkit;
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
        // How to open book:
        // Set book into player's hand
        // Send OpenBook packet
        // Reset player's hand item

        final ItemStack item = player.getInventory().getItemInMainHand();
        final String[] verSplit = Bukkit.getServer().getClass().getName().split("\\.");
        final String ver = verSplit[verSplit.length - 2];

        try {
            // CraftItemStack
            final Class<?> craftItemStackClass = Class.forName("org.bukkit.craftbukkit." + ver + ".inventory.CraftItemStack");
            final Method asNMSCopyMethod = craftItemStackClass.getDeclaredMethod("asNMSCopy", ItemStack.class);
            asNMSCopyMethod.setAccessible(true);

            // CraftPlayer
            final Class<?> craftPlayerClass = Class.forName("org.bukkit.craftbukkit." + ver + ".entity.CraftPlayer");
            final Object craftPlayer = craftPlayerClass.cast(player);

            // EntityPlayer
            final Method getHandleMethod = craftPlayerClass.getDeclaredMethod("getHandle");
            getHandleMethod.setAccessible(true);
            final Object nmsPlayer = getHandleMethod.invoke(craftPlayer);

            // PlayerConnection
            final Field playerConField = nmsPlayer.getClass().getDeclaredField("b");
            playerConField.setAccessible(true);
            final Object playerCon = playerConField.get(nmsPlayer);

            // PlayerConnection#sendPacket
            final Method sendPacketMethod = playerCon.getClass().getDeclaredMethod("sendPacket", Class.forName("net.minecraft.network.protocol.Packet"));
            sendPacketMethod.setAccessible(true);

            // PacketPlayOutSetSlot
            final Class<?> setSlotPacketClass = Class.forName("net.minecraft.network.protocol.game.PacketPlayOutSetSlot");
            final Constructor<?> setSlotPacketConst = setSlotPacketClass.getDeclaredConstructor(int.class, int.class, int.class, Class.forName("net.minecraft.world.item.ItemStack"));
            setSlotPacketConst.setAccessible(true);

            // EnumHand & PacketPlayOutOpenBook
            final Class<?> enumHandClass = Class.forName("net.minecraft.world.EnumHand");
            final Object enumHandMain = enumHandClass.getEnumConstants()[0];
            final Class<?> openBookPacketClass = Class.forName("net.minecraft.network.protocol.game.PacketPlayOutOpenBook");
            final Constructor<?> openBookPacketConst = openBookPacketClass.getDeclaredConstructor(enumHandClass);
            openBookPacketConst.setAccessible(true);

            // Instantiate packets
            final Object firstSetSlotPacket = setSlotPacketConst.newInstance(0, 0, player.getInventory().getHeldItemSlot() + 36, asNMSCopyMethod.invoke(null, book));
            final Object openBookPacket = openBookPacketConst.newInstance(enumHandMain);
            final Object secondSetSlotPacket = setSlotPacketConst.newInstance(0, 0, player.getInventory().getHeldItemSlot() + 36, asNMSCopyMethod.invoke(null, item));

            // Send packets
            sendPacketMethod.invoke(playerCon, firstSetSlotPacket);
            sendPacketMethod.invoke(playerCon, openBookPacket);
            sendPacketMethod.invoke(playerCon, secondSetSlotPacket);
        } catch (final NoSuchFieldException
                | ClassNotFoundException
                | InvocationTargetException
                | NoSuchMethodException
                | IllegalAccessException
                | InstantiationException e) {
            e.printStackTrace();
        }

        /*Packet<?> packetNms = new PacketPlayOutSetSlot(0, 0, player.getInventory().getHeldItemSlot() + 36, CraftItemStack.asNMSCopy(book));
        ((CraftPlayer) player).getHandle().b.sendPacket(packetNms);
        packetNms = new PacketPlayOutOpenBook(EnumHand.a);
        ((CraftPlayer) player).getHandle().b.sendPacket(packetNms);
        packetNms = new PacketPlayOutSetSlot(0, 0, player.getInventory().getHeldItemSlot() + 36, CraftItemStack.asNMSCopy(item));
        ((CraftPlayer) player).getHandle().b.sendPacket(packetNms);*/
    }

}
