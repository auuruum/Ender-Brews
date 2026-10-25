package dev.enderbrews.fabric;

import dev.enderbrews.fabric.common.EnderBrews;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;

/** Fabric and Quilt entry point: forwards Fabric API events to the shared code. */
public final class EnderBrewsFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(server ->
                EnderBrews.serverStarted(server, FabricLoader.getInstance().getConfigDir()));
        ServerLifecycleEvents.SERVER_STOPPING.register(EnderBrews::serverStopping);
        ServerTickEvents.END_SERVER_TICK.register(EnderBrews::serverTick);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> EnderBrews.playerJoined(handler.player));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> EnderBrews.playerLeft(handler.player));
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                EnderBrews.registerCommands(dispatcher));
    }
}
