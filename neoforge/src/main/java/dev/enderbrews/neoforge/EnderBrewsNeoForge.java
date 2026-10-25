package dev.enderbrews.neoforge;

import dev.enderbrews.neoforge.common.EnderBrews;
import dev.enderbrews.neoforge.common.EnderBrewsPayloads;
import dev.enderbrews.neoforge.common.ServerNetwork;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** NeoForge entry point: forwards NeoForge events to the shared code. */
@Mod(EnderBrews.MOD_ID)
public final class EnderBrewsNeoForge {

    /** Set by {@link EnderBrewsNeoForgeClient}; stays a no-op on dedicated servers. */
    static Consumer<byte[]> statusHandler = data -> {
    };

    public EnderBrewsNeoForge(IEventBus modBus) {
        modBus.addListener((RegisterPayloadHandlersEvent event) -> {
            // Optional: players and servers without Ender Brews can still connect.
            PayloadRegistrar registrar = event.registrar("1").optional();
            registrar.playToClient(EnderBrewsPayloads.Status.TYPE, EnderBrewsPayloads.Status.CODEC,
                    (payload, context) -> statusHandler.accept(payload.data()));
            registrar.playToServer(EnderBrewsPayloads.Pause.TYPE, EnderBrewsPayloads.Pause.CODEC, (payload, context) -> {
                if (context.player() instanceof ServerPlayer player) {
                    EnderBrews.pausePacketReceived(player, payload.data());
                }
            });
        });
        EnderBrews.setNetwork(new ServerNetwork() {
            @Override
            public boolean canSendStatus(ServerPlayer player) {
                return player.connection.hasChannel(EnderBrewsPayloads.Status.TYPE);
            }

            @Override
            public void sendStatus(ServerPlayer player, byte[] data) {
                PacketDistributor.sendToPlayer(player, new EnderBrewsPayloads.Status(data));
            }
        });

        NeoForge.EVENT_BUS.addListener((ServerStartedEvent event) ->
                EnderBrews.serverStarted(event.getServer(), FMLPaths.CONFIGDIR.get()));
        NeoForge.EVENT_BUS.addListener((ServerStoppingEvent event) -> EnderBrews.serverStopping(event.getServer()));
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> EnderBrews.serverTick(event.getServer()));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                EnderBrews.playerJoined(player);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                EnderBrews.playerLeft(player);
            }
        });
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> EnderBrews.registerCommands(event.getDispatcher()));
    }
}
