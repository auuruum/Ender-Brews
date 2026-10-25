package dev.enderbrews.shared.common;

import com.mojang.brigadier.CommandDispatcher;
import java.nio.file.Path;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Entry points the Fabric, NeoForge and Forge loaders call from their own events. */
public final class EnderBrews {

    public static final String MOD_ID = "enderbrews";
    public static final Logger LOG = LoggerFactory.getLogger("Ender Brews");

    private static EnderBrewsServer server;

    private EnderBrews() {
    }

    /** The running server's state, or {@code null} before the server has started. */
    static EnderBrewsServer server() {
        return server;
    }

    public static void serverStarted(MinecraftServer minecraftServer, Path configDir) {
        server = new EnderBrewsServer(minecraftServer, configDir);
    }

    public static void serverStopping(MinecraftServer minecraftServer) {
        if (server != null) {
            server.stopping(minecraftServer);
            server = null;
        }
    }

    public static void serverTick(MinecraftServer minecraftServer) {
        if (server != null) {
            server.tick(minecraftServer);
        }
    }

    public static void playerJoined(ServerPlayer player) {
        if (server != null) {
            server.playerJoined(player);
        }
    }

    public static void playerLeft(ServerPlayer player) {
        if (server != null) {
            server.playerLeft(player);
        }
    }

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        EnderBrewsCommand.register(dispatcher);
    }
}
