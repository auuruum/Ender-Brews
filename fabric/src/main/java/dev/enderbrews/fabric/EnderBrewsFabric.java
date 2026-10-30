package dev.enderbrews.fabric;

import dev.enderbrews.fabric.common.EnderBrews;
import dev.enderbrews.fabric.common.ServerNetwork;
import dev.enderbrews.fabric.core.Protocol;
import io.netty.buffer.Unpooled;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** Fabric and Quilt entry point: forwards Fabric API events to the shared code. */
public final class EnderBrewsFabric implements ModInitializer {

    static final ResourceLocation STATUS = new ResourceLocation(Protocol.STATUS_CHANNEL);
    static final ResourceLocation PAUSE = new ResourceLocation(Protocol.PAUSE_CHANNEL);
    /** Filled by {@link dev.enderbrews.fabric.mixin.ServerPlayerMixin}. */
    public static final Map<UUID, String> LANGUAGES = new ConcurrentHashMap<>();

    @Override
    public void onInitialize() {
        ServerPlayNetworking.registerGlobalReceiver(PAUSE, (server, player, handler, buf, responseSender) -> {
            byte[] data = bytes(buf);
            server.execute(() -> EnderBrews.pausePacketReceived(player, data));
        });
        EnderBrews.setNetwork(new ServerNetwork() {
            @Override
            public boolean canSendStatus(ServerPlayer player) {
                return ServerPlayNetworking.canSend(player, STATUS);
            }

            @Override
            public void sendStatus(ServerPlayer player, byte[] data) {
                ServerPlayNetworking.send(player, STATUS, buffer(data));
            }

            @Override
            public String language(ServerPlayer player) {
                return LANGUAGES.getOrDefault(player.getUUID(), "en_us");
            }
        });

        ServerLifecycleEvents.SERVER_STARTED.register(server ->
                EnderBrews.serverStarted(server, FabricLoader.getInstance().getConfigDir()));
        ServerLifecycleEvents.SERVER_STOPPING.register(EnderBrews::serverStopping);
        ServerTickEvents.END_SERVER_TICK.register(EnderBrews::serverTick);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> EnderBrews.playerJoined(handler.player));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            EnderBrews.playerLeft(handler.player);
            LANGUAGES.remove(handler.player.getUUID());
        });
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                EnderBrews.registerCommands(dispatcher));
    }

    static FriendlyByteBuf buffer(byte[] data) {
        return new FriendlyByteBuf(Unpooled.wrappedBuffer(data));
    }

    static byte[] bytes(FriendlyByteBuf buf) {
        byte[] data = new byte[buf.readableBytes()];
        buf.readBytes(data);
        return data;
    }
}
