package dev.enderbrews.shared.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import org.junit.jupiter.api.Test;

class ProtocolTest {

    @Test
    void snapshotRoundTrips() {
        Protocol.Snapshot snapshot = new Protocol.Snapshot(true, List.of(
                new Protocol.Entry(new EffectStatus("minecraft:strength", 1, 300, 8, EffectStatus.State.ACTIVE), true),
                new Protocol.Entry(new EffectStatus("modid:éffect", 0, 3, 32, EffectStatus.State.PROGRESS), false)));
        assertEquals(snapshot, Protocol.decodeSnapshot(Protocol.encodeSnapshot(snapshot)));
    }

    @Test
    void pauseRequestsRoundTrip() {
        Protocol.PauseRequest one = new Protocol.PauseRequest("minecraft:speed", true);
        Protocol.PauseRequest all = new Protocol.PauseRequest(null, false);
        assertEquals(one, Protocol.decodePause(Protocol.encodePause(one)));
        assertEquals(all, Protocol.decodePause(Protocol.encodePause(all)));
    }

    @Test
    void unknownOrBrokenDataIsIgnored() {
        assertNull(Protocol.decodeSnapshot(new byte[] {9}));
        assertNull(Protocol.decodeSnapshot(new byte[] {1, 1, 5}));
        assertNull(Protocol.decodePause(new byte[0]));
    }
}
