package dev.enderbrews.shared.common;

import dev.enderbrews.shared.core.Protocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Custom payloads for clients that have the mod. The body is the raw {@link Protocol} bytes, so it
 * matches what the Bukkit plugin sends over its plugin channels.
 */
public final class EnderBrewsPayloads {

    private EnderBrewsPayloads() {
    }

    /** Server to client. */
    public record Status(byte[] data) implements CustomPacketPayload {
        public static final Type<Status> TYPE = new Type<>(Identifier.parse(Protocol.STATUS_CHANNEL));
        public static final StreamCodec<FriendlyByteBuf, Status> CODEC = StreamCodec.of(
                (buf, payload) -> buf.writeBytes(payload.data()), buf -> new Status(readRest(buf)));

        @Override
        public Type<Status> type() {
            return TYPE;
        }
    }

    /** Client to server. */
    public record Pause(byte[] data) implements CustomPacketPayload {
        public static final Type<Pause> TYPE = new Type<>(Identifier.parse(Protocol.PAUSE_CHANNEL));
        public static final StreamCodec<FriendlyByteBuf, Pause> CODEC = StreamCodec.of(
                (buf, payload) -> buf.writeBytes(payload.data()), buf -> new Pause(readRest(buf)));

        @Override
        public Type<Pause> type() {
            return TYPE;
        }
    }

    private static byte[] readRest(FriendlyByteBuf buf) {
        byte[] data = new byte[buf.readableBytes()];
        buf.readBytes(data);
        return data;
    }
}
