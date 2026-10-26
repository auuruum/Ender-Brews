package dev.enderbrews.forge;

import dev.enderbrews.forge.common.EnderBrews;
import dev.enderbrews.forge.common.ServerNetwork;
import dev.enderbrews.forge.core.Protocol;
import io.netty.buffer.Unpooled;
import java.util.function.Consumer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.EventNetworkChannel;
import net.minecraftforge.network.PacketDistributor;

/** Forge entry point: forwards Forge events to the shared code. */
@Mod(EnderBrews.MOD_ID)
public final class EnderBrewsForge {

    // Optional: players and servers without Ender Brews can still connect.
    static final EventNetworkChannel STATUS = ChannelBuilder.named(Identifier.parse(Protocol.STATUS_CHANNEL))
            .optional().eventNetworkChannel();
    static final EventNetworkChannel PAUSE = ChannelBuilder.named(Identifier.parse(Protocol.PAUSE_CHANNEL))
            .optional().eventNetworkChannel();

    /** Set by {@link EnderBrewsForgeClient}; stays a no-op on dedicated servers. */
    static Consumer<byte[]> statusHandler = data -> {
    };

    public EnderBrewsForge() {
        PAUSE.addListener((CustomPayloadEvent event) -> {
            ServerPlayer sender = event.getSource().getSender();
            if (sender != null && event.getPayload() != null) {
                byte[] data = bytes(event.getPayload());
                event.getSource().enqueueWork(() -> EnderBrews.pausePacketReceived(sender, data));
            }
            event.getSource().setPacketHandled(true);
        });
        STATUS.addListener((CustomPayloadEvent event) -> {
            if (event.getSource().isClientSide() && event.getPayload() != null) {
                byte[] data = bytes(event.getPayload());
                event.getSource().enqueueWork(() -> statusHandler.accept(data));
            }
            event.getSource().setPacketHandled(true);
        });
        EnderBrews.setNetwork(new ServerNetwork() {
            @Override
            public boolean canSendStatus(ServerPlayer player) {
                return STATUS.isRemotePresent(player.connection.getConnection());
            }

            @Override
            public void sendStatus(ServerPlayer player, byte[] data) {
                STATUS.send(buffer(data), PacketDistributor.PLAYER.with(player));
            }
        });

        ServerStartedEvent.BUS.addListener(event -> EnderBrews.serverStarted(event.getServer(), FMLPaths.CONFIGDIR.get()));
        ServerStoppingEvent.BUS.addListener(event -> EnderBrews.serverStopping(event.getServer()));
        TickEvent.ServerTickEvent.Post.BUS.addListener(event -> EnderBrews.serverTick(event.server()));
        PlayerEvent.PlayerLoggedInEvent.BUS.addListener(event -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                EnderBrews.playerJoined(player);
            }
        });
        PlayerEvent.PlayerLoggedOutEvent.BUS.addListener(event -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                EnderBrews.playerLeft(player);
            }
        });
        RegisterCommandsEvent.BUS.addListener(event -> EnderBrews.registerCommands(event.getDispatcher()));

        if (FMLEnvironment.dist.isClient()) {
            EnderBrewsForgeClient.init();
        }
    }

    static FriendlyByteBuf buffer(byte[] data) {
        return new FriendlyByteBuf(Unpooled.wrappedBuffer(data));
    }

    private static byte[] bytes(FriendlyByteBuf buf) {
        byte[] data = new byte[buf.readableBytes()];
        buf.readBytes(data);
        return data;
    }
}
