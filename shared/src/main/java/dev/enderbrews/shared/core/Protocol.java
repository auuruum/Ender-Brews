package dev.enderbrews.shared.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * The small protocol between a server running Ender Brews (mod or plugin) and a client that has the
 * mod installed. Players without the mod on their client never see these packets. Payloads are plain
 * bytes so the Bukkit plugin and every mod loader write exactly the same thing.
 */
public final class Protocol {

    public static final String NAMESPACE = "enderbrews";
    /** Server to client: where every effect stands. */
    public static final String STATUS_CHANNEL = NAMESPACE + ":status";
    /** Client to server: pause or resume endless effects. */
    public static final String PAUSE_CHANNEL = NAMESPACE + ":pause";
    /** Hidden marker on the /enderbrews chat list so the client can replace the previous copy. */
    public static final String LIST_MARKER = NAMESPACE + ":list";

    private static final int VERSION = 1;

    private Protocol() {
    }

    /** What the client knows about the player's endless effects. */
    public record Snapshot(boolean allowPause, List<Entry> entries) {

        public Entry find(String effectId) {
            for (Entry entry : entries) {
                if (entry.status().effectId().equals(effectId)) {
                    return entry;
                }
            }
            return null;
        }
    }

    public record Entry(EffectStatus status, boolean paused) {
    }

    /** A pause request: one effect, or every endless effect when {@code effectId} is null. */
    public record PauseRequest(String effectId, boolean paused) {
    }

    public static byte[] encodeSnapshot(Snapshot snapshot) {
        return write(out -> {
            out.writeByte(VERSION);
            out.writeBoolean(snapshot.allowPause());
            writeVarInt(out, snapshot.entries().size());
            for (Entry entry : snapshot.entries()) {
                EffectStatus status = entry.status();
                writeString(out, status.effectId());
                writeVarInt(out, status.amplifier());
                writeVarInt(out, status.count());
                writeVarInt(out, status.threshold());
                out.writeByte(status.state().ordinal());
                out.writeBoolean(entry.paused());
            }
        });
    }

    /** Returns null for data this version does not understand. */
    public static Snapshot decodeSnapshot(byte[] data) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
            if (in.readByte() != VERSION) {
                return null;
            }
            boolean allowPause = in.readBoolean();
            int size = readVarInt(in);
            List<Entry> entries = new ArrayList<>(size);
            EffectStatus.State[] states = EffectStatus.State.values();
            for (int i = 0; i < size; i++) {
                String effectId = readString(in);
                int amplifier = readVarInt(in);
                int count = readVarInt(in);
                int threshold = readVarInt(in);
                int state = in.readByte();
                boolean paused = in.readBoolean();
                if (state < 0 || state >= states.length) {
                    return null;
                }
                entries.add(new Entry(new EffectStatus(effectId, amplifier, count, threshold, states[state]), paused));
            }
            return new Snapshot(allowPause, List.copyOf(entries));
        } catch (IOException e) {
            return null;
        }
    }

    public static byte[] encodePause(PauseRequest request) {
        return write(out -> {
            out.writeByte(VERSION);
            out.writeBoolean(request.effectId() != null);
            if (request.effectId() != null) {
                writeString(out, request.effectId());
            }
            out.writeBoolean(request.paused());
        });
    }

    /** Returns null for data this version does not understand. */
    public static PauseRequest decodePause(byte[] data) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
            if (in.readByte() != VERSION) {
                return null;
            }
            String effectId = in.readBoolean() ? readString(in) : null;
            return new PauseRequest(effectId, in.readBoolean());
        } catch (IOException e) {
            return null;
        }
    }

    private interface Writer {
        void write(DataOutputStream out) throws IOException;
    }

    private static byte[] write(Writer writer) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            writer.write(out);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return bytes.toByteArray();
    }

    // Same VarInt and string layout as Minecraft's FriendlyByteBuf.

    private static void writeVarInt(DataOutputStream out, int value) throws IOException {
        while ((value & ~0x7F) != 0) {
            out.writeByte((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        out.writeByte(value);
    }

    private static int readVarInt(DataInputStream in) throws IOException {
        int value = 0;
        for (int shift = 0; shift < 35; shift += 7) {
            int b = in.readUnsignedByte();
            value |= (b & 0x7F) << shift;
            if ((b & 0x80) == 0) {
                return value;
            }
        }
        throw new IOException("VarInt too long");
    }

    private static void writeString(DataOutputStream out, String value) throws IOException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        writeVarInt(out, bytes.length);
        out.write(bytes);
    }

    private static String readString(DataInputStream in) throws IOException {
        int length = readVarInt(in);
        if (length < 0 || length > 32767) {
            throw new IOException("bad string length " + length);
        }
        return new String(in.readNBytes(length), StandardCharsets.UTF_8);
    }
}
