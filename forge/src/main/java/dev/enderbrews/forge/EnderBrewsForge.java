package dev.enderbrews.forge;

import dev.enderbrews.forge.common.EnderBrews;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

/** Forge entry point: forwards Forge events to the shared code. */
@Mod(EnderBrews.MOD_ID)
public final class EnderBrewsForge {

    public EnderBrewsForge() {
        MinecraftForge.EVENT_BUS.addListener((ServerStartedEvent event) ->
                EnderBrews.serverStarted(event.getServer(), FMLPaths.CONFIGDIR.get()));
        MinecraftForge.EVENT_BUS.addListener((ServerStoppingEvent event) -> EnderBrews.serverStopping(event.getServer()));
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END) {
                EnderBrews.serverTick(event.getServer());
            }
        });
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                EnderBrews.playerJoined(player);
            }
        });
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                EnderBrews.playerLeft(player);
            }
        });
        MinecraftForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> EnderBrews.registerCommands(event.getDispatcher()));
    }
}
