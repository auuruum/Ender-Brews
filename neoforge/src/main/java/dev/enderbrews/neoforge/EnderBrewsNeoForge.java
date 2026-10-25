package dev.enderbrews.neoforge;

import dev.enderbrews.neoforge.common.EnderBrews;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** NeoForge entry point: forwards NeoForge events to the shared code. */
@Mod(EnderBrews.MOD_ID)
public final class EnderBrewsNeoForge {

    public EnderBrewsNeoForge() {
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
