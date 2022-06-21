package dev.cerus.mapads.compatibility;

import dev.cerus.mapads.ConfigModel;
import dev.cerus.mapads.screen.storage.AdScreenStorage;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class Below17Compatibility extends Compatibility {

    public Below17Compatibility(final ConfigModel configModel, final AdScreenStorage adScreenStorage) {
        super(configModel, adScreenStorage);
    }

    @Override
    public void inject(final Player player) {
        try {
            final Object networkManager = this.getNetworkManager(player);
            final Field channelField = networkManager.getClass().getDeclaredField("channel");
            channelField.setAccessible(true);
            final Method pipelineMethod = channelField.getType().getDeclaredMethod("pipeline");
            pipelineMethod.setAccessible(true);

            final Class<?> packetDestroyEntityClass = Class.forName("net.minecraft.server." + this.getVersion() + ".PacketPlayOutEntityDestroy");
            final Field entityIdsFieldDestr = packetDestroyEntityClass.getDeclaredField("a");
            entityIdsFieldDestr.setAccessible(true);

            final Object channel = channelField.get(networkManager);
            final ChannelPipeline pipeline = (ChannelPipeline) pipelineMethod.invoke(channel);
            pipeline.addBefore("packet_handler", "mapads_compat", new ChannelDuplexHandler() {
                @Override
                public void write(final ChannelHandlerContext ctx, final Object msg, final ChannelPromise promise) throws Exception {
                    if (packetDestroyEntityClass.isInstance(msg)) {
                        final int[] eids = ((int[]) entityIdsFieldDestr.get(msg));
                        for (final int eid : eids) {
                            if (Below17Compatibility.this.cancelFrameDespawn(eid)) {
                                player.sendMessage("cancel despawn " + eid);
                                return;
                            }
                        }
                    }
                    super.write(ctx, msg, promise);
                }
            });
        } catch (final NoSuchFieldException | ClassNotFoundException | InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
            LOGGER.severe("Failed to inject compatibility layer into player " + player.getName());
        }
    }


    @Override
    public void spawnEntity(final Player player, final Entity entity) {
        try {
            final Class<?> packetClass = Class.forName("net.minecraft.server." + this.getVersion() + ".PacketPlayOutSpawnEntity");
            final Constructor<?> constr = Arrays.stream(packetClass.getDeclaredConstructors()).filter(c -> c.getParameterCount() == 1).findFirst().orElseThrow();

            final Class<?> craftEntityClass = Class.forName("org.bukkit.craftbukkit." + this.getVersion() + ".entity.CraftEntity");
            final Method getHandleMethod = craftEntityClass.getDeclaredMethod("getHandle");
            getHandleMethod.setAccessible(true);
            final Object nmsEntity = getHandleMethod.invoke(entity);

            final Object packet = constr.newInstance(nmsEntity);
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
        player.sendMessage("sending " + packet.getClass().getSimpleName());
        try {
            final Object networkManager = this.getNetworkManager(player);
            final Method sendPacketMethod = networkManager.getClass().getDeclaredMethod("sendPacket", packet.getClass().getInterfaces()[0]);
            sendPacketMethod.setAccessible(true);
            sendPacketMethod.invoke(networkManager, packet);
        } catch (final InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private Object getNetworkManager(final Player player) {
        try {
            final Class<?> craftPlayerClass = Class.forName("org.bukkit.craftbukkit." + this.getVersion() + ".entity.CraftPlayer");
            final Method getHandleMethod = craftPlayerClass.getDeclaredMethod("getHandle");
            getHandleMethod.setAccessible(true);
            final Field playerConnectionField = getHandleMethod.getReturnType().getDeclaredField("playerConnection");
            playerConnectionField.setAccessible(true);
            final Field networkManagerField = playerConnectionField.getType().getDeclaredField("networkManager");
            networkManagerField.setAccessible(true);

            final Object entityPlayer = getHandleMethod.invoke(player);
            final Object playerConnection = playerConnectionField.get(entityPlayer);
            return networkManagerField.get(playerConnection);
        } catch (final NoSuchFieldException | ClassNotFoundException | InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public Entity getEntity(final World world, final int entityId) {
        try {
            final Method getHandleMethod = world.getClass().getDeclaredMethod("getHandle");
            getHandleMethod.setAccessible(true);
            final Object nmsWorld = getHandleMethod.invoke(world);

            final Method getEntityMethod = nmsWorld.getClass().getDeclaredMethod("getEntity", int.class);
            getEntityMethod.setAccessible(true);
            final Object nmsEntity = getEntityMethod.invoke(nmsWorld, entityId);
            if (nmsEntity == null) {
                return null;
            }

            final Method getBukkitEntityMethod = Class.forName("net.minecraft.server." + this.getVersion() + ".Entity")
                    .getDeclaredMethod("getBukkitEntity");
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
