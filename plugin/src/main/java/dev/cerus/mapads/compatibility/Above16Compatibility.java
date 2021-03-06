package dev.cerus.mapads.compatibility;

import dev.cerus.mapads.ConfigModel;
import dev.cerus.mapads.screen.storage.AdScreenStorage;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;

public class Above16Compatibility extends Compatibility {

    public Above16Compatibility(final ConfigModel configModel, final AdScreenStorage adScreenStorage) {
        super(configModel, adScreenStorage);
    }

    @Override
    public void inject(final Player player) {
        try {
            final Object networkMan = this.getNetworkManager(player);
            final Field channelField = Arrays.stream(networkMan.getClass().getDeclaredFields())
                    .filter(field -> field.getType() == Channel.class)
                    .findFirst()
                    .orElseThrow();
            channelField.setAccessible(true);

            final Class<?> packetDestroyEntityClass = Class.forName("net.minecraft.network.protocol.game.PacketPlayOutEntityDestroy");

            final Channel channel = (Channel) channelField.get(networkMan);
            channel.pipeline().addBefore("packet_handler", "mapads_compat", new ChannelDuplexHandler() {
                @Override
                public void write(final ChannelHandlerContext ctx, final Object msg, final ChannelPromise promise) throws Exception {
                    if (packetDestroyEntityClass.isInstance(msg)) {
                        final int[] eids = Above16Compatibility.this.getEntityIds(msg);
                        for (final int eid : eids) {
                            if (Above16Compatibility.this.cancelFrameDespawn(eid)) {
                                return;
                            }
                        }
                    }
                    super.write(ctx, msg, promise);
                }
            });
        } catch (final IllegalAccessException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private int[] getEntityIds(final Object destroyPacket) throws IllegalAccessException {
        final Field field = Arrays.stream(destroyPacket.getClass().getDeclaredFields())
                .filter(f -> !Modifier.isStatic(f.getModifiers()))
                .findFirst()
                .orElseThrow();
        field.setAccessible(true);
        if (field.getType() == int.class) {
            return new int[] {(int) field.get(destroyPacket)};
        } else if (field.getType() == int[].class) {
            return (int[]) field.get(destroyPacket);
        } else {
            final List<Integer> intList = (List<Integer>) field.get(destroyPacket);
            final int[] arr = new int[intList.size()];
            for (int i = 0; i < intList.size(); i++) {
                arr[i] = intList.get(i);
            }
            return arr;
        }
    }

    @Override
    public void spawnEntity(final Player player, final Entity entity) {
        try {
            final Class<?> entityClass = Class.forName("net.minecraft.world.entity.Entity");

            final Class<?> packetClass = Class.forName("net.minecraft.network.protocol.game.PacketPlayOutSpawnEntity");
            final Constructor<?> constr = Arrays.stream(packetClass.getDeclaredConstructors())
                    .filter(c -> c.getParameterCount() == 2)
                    .filter(c -> c.getParameterTypes()[0] == entityClass)
                    .filter(c -> c.getParameterTypes()[1] == int.class)
                    .findFirst()
                    .orElseThrow();

            final Class<?> craftEntityClass = Class.forName("org.bukkit.craftbukkit." + this.getVersion() + ".entity.CraftEntity");
            final Method getHandleMethod = craftEntityClass.getDeclaredMethod("getHandle");
            getHandleMethod.setAccessible(true);
            final Object nmsEntity = getHandleMethod.invoke(entity);

            int extra = 0;
            if (entity instanceof ItemFrame frame) {
                try {
                    final Class<?> dirClass = Class.forName("net.minecraft.core.EnumDirection");
                    final Field idField = dirClass.getDeclaredField("l");
                    idField.setAccessible(true);

                    final Object[] enumConstants = dirClass.getEnumConstants();
                    for (int i = 0; i < enumConstants.length; i++) {
                        final Object enumConstant = enumConstants[i];
                        final String id = (String) idField.get(enumConstant);
                        if (id.equals(frame.getRotation().name().toLowerCase())) {
                            extra = i;
                            break;
                        }
                    }
                } catch (final NoSuchFieldException e) {
                    throw new RuntimeException(e);
                }
            }

            final Object packet = constr.newInstance(nmsEntity, extra);
            this.sendPacket(player, packet);
        } catch (final ClassNotFoundException
                       | InvocationTargetException
                       | NoSuchMethodException
                       | IllegalAccessException
                       | InstantiationException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendPacket(final Player player, final Object packet) {
        try {
            final Class<?> packetClass = Class.forName("net.minecraft.network.protocol.Packet");

            final Object networkManager = this.getNetworkManager(player);
            final Method sendPacketMethod = Arrays.stream(networkManager.getClass().getDeclaredMethods())
                    .filter(method -> method.getParameterCount() == 1)
                    .filter(method -> method.getParameterTypes()[0] == packetClass)
                    .findFirst()
                    .orElseThrow();
            sendPacketMethod.setAccessible(true);

            sendPacketMethod.invoke(networkManager, packet);
        } catch (final InvocationTargetException | IllegalAccessException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private Object getNetworkManager(final Player player) {
        try {
            final Class<?> craftPlayerClass = player.getClass();
            final Method getHandleMethod = craftPlayerClass.getDeclaredMethod("getHandle");
            getHandleMethod.setAccessible(true);
            final Object handle = getHandleMethod.invoke(player);

            final Class<?> playerConClass = Class.forName("net.minecraft.server.network.PlayerConnection");
            final Field playerConField = Arrays.stream(handle.getClass().getDeclaredFields())
                    .filter(field -> field.getType() == playerConClass)
                    .findFirst()
                    .orElseThrow();
            playerConField.setAccessible(true);
            final Object playerCon = playerConField.get(handle);

            final Class<?> networkManClass = Class.forName("net.minecraft.network.NetworkManager");
            final Field networkManField = Arrays.stream(playerConClass.getDeclaredFields())
                    .filter(field -> field.getType() == networkManClass)
                    .findFirst()
                    .orElseThrow();
            networkManField.setAccessible(true);
            return networkManField.get(playerCon);
        } catch (final ClassNotFoundException
                       | InvocationTargetException
                       | NoSuchMethodException
                       | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public Entity getEntity(final World world, final int entityId) {
        try {
            final Method getHandleMethod = world.getClass().getDeclaredMethod("getHandle");
            getHandleMethod.setAccessible(true);
            final Object nmsWorld = getHandleMethod.invoke(world);

            final Class<?> nmsEntityClass = Class.forName("net.minecraft.world.entity.Entity");

            final Method getEntityMethod = Arrays.stream(nmsWorld.getClass().getDeclaredMethods())
                    .filter(method -> method.getParameterCount() == 1)
                    .filter(method -> method.getParameterTypes()[0] == int.class)
                    .filter(method -> method.getReturnType() == nmsEntityClass)
                    .findFirst()
                    .orElseThrow();
            getEntityMethod.setAccessible(true);
            final Object nmsEntity = getEntityMethod.invoke(nmsWorld, entityId);
            if (nmsEntity == null) {
                return null;
            }

            final Method getBukkitEntityMethod = nmsEntityClass.getDeclaredMethod("getBukkitEntity");
            getBukkitEntityMethod.setAccessible(true);
            return (Entity) getBukkitEntityMethod.invoke(nmsEntity);
        } catch (final ClassNotFoundException
                       | InvocationTargetException
                       | NoSuchMethodException
                       | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private String getVersion() {
        final String className = Bukkit.getServer().getClass().getName();
        final String[] parts = className.split("\\.");
        return parts.length == 5 ? parts[3] : null;
    }

}
