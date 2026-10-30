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
    private static ServerNetwork network;

    private EnderBrews() {
    }

    /** The running server's state, or {@code null} before the server has started. */
    static EnderBrewsServer server() {
        return server;
    }

    /** Called once by the loader at startup, so the server can talk to clients that have the mod. */
    public static void setNetwork(ServerNetwork serverNetwork) {
        network = serverNetwork;
    }

    static String language(ServerPlayer player) {
        return network == null ? "en_us" : network.language(player);
    }

    public static void serverStarted(MinecraftServer minecraftServer, Path configDir) {
        server = new EnderBrewsServer(minecraftServer, configDir, network);
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

    /** A pause packet from a client that has the mod; must be called on the server thread. */
    public static void pausePacketReceived(ServerPlayer player, byte[] data) {
        if (server != null) {
            server.handlePausePacket(player, data);
        }
    }

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        EnderBrewsCommand.register(dispatcher);
    }
}
