package dev.enderbrews.shared.common;

import net.minecraft.server.level.ServerPlayer;

/** How the loader sends Ender Brews packets to players whose client has the mod. */
public interface ServerNetwork {

    /** Whether the player's client has the mod and listens for status packets. */
    boolean canSendStatus(ServerPlayer player);

    void sendStatus(ServerPlayer player, byte[] data);
}
