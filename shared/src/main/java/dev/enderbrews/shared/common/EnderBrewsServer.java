package dev.enderbrews.shared.common;

import dev.enderbrews.shared.core.EffectStatus;
import dev.enderbrews.shared.core.EnderBrewsConfig;
import dev.enderbrews.shared.core.EndlessRules;
import dev.enderbrews.shared.core.PauseStore;
import dev.enderbrews.shared.core.Protocol;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.level.storage.LevelResource;

/** Ender Brews state for one running server (dedicated or singleplayer). */
public final class EnderBrewsServer {

    private final Path configFile;
    private final PauseStore pauses;
    private final ServerNetwork network;
    private final Map<UUID, PlayerState> players = new HashMap<>();
    private EnderBrewsConfig config = EnderBrewsConfig.defaults();
    private long ticks;

    EnderBrewsServer(MinecraftServer server, Path configDir, ServerNetwork network) {
        this.configFile = configDir.resolve(EnderBrewsConfig.FILE_NAME);
        this.network = network;
        this.pauses = new PauseStore(server.getWorldPath(LevelResource.ROOT).resolve("enderbrews-paused.txt"));
        try {
            reload();
        } catch (IOException | IllegalArgumentException e) {
            EnderBrews.LOG.error("Could not read {}, using defaults: {}", configFile, e.getMessage());
        }
        try {
            pauses.load();
        } catch (IOException e) {
            EnderBrews.LOG.error("Could not read paused effects", e);
        }
    }

    /** Re-reads the config file. Throws if it cannot be read, keeping the old config. */
    void reload() throws IOException {
        config = EnderBrewsConfig.load(configFile);
        players.values().forEach(state -> state.dirty = true);
    }

    EnderBrewsConfig config() {
        return config;
    }

    void tick(MinecraftServer server) {
        boolean recount = ++ticks % config.checkIntervalTicks() == 0;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            PlayerState state = players.computeIfAbsent(player.getUUID(), id -> new PlayerState());
            if (recount || state.dirty) {
                refresh(player, state);
            }
            EndlessEffects.keep(player, state.applied, config);
        }
    }

    void playerJoined(ServerPlayer player) {
        if (config.removeOnLogout()) {
            EndlessEffects.removeLeftovers(player);
        }
        players.computeIfAbsent(player.getUUID(), id -> new PlayerState()).dirty = true;
    }

    void playerLeft(ServerPlayer player) {
        PlayerState state = players.remove(player.getUUID());
        if (state != null && config.removeOnLogout()) {
            state.applied.forEach((effect, amplifier) -> EndlessEffects.remove(player, effect, amplifier));
        }
    }

    void stopping(MinecraftServer server) {
        // Players are saved while the server shuts down; take endless effects off before that.
        server.getPlayerList().getPlayers().forEach(this::playerLeft);
    }

    /** Counts the player's Ender Chest right now and returns where every effect stands. */
    List<EffectStatus> statuses(ServerPlayer player) {
        PlayerState state = players.computeIfAbsent(player.getUUID(), id -> new PlayerState());
        refresh(player, state);
        return state.statuses;
    }

    boolean isPaused(ServerPlayer player, String effectId) {
        return config.allowPause() && pauses.isPaused(player.getUUID(), effectId);
    }

    void setPaused(ServerPlayer player, String effectId, boolean paused) {
        pauses.setPaused(player.getUUID(), effectId, paused);
        savePausesAndRefresh(player);
    }

    /** Pauses every endless effect, or resumes every paused one. */
    void setAllPaused(ServerPlayer player, boolean paused) {
        for (EffectStatus status : statuses(player)) {
            if (status.state() == EffectStatus.State.ACTIVE) {
                pauses.setPaused(player.getUUID(), status.effectId(), paused);
            }
        }
        savePausesAndRefresh(player);
    }

    /** A pause request from a client that has the mod (its pause button or key). */
    void handlePausePacket(ServerPlayer player, byte[] data) {
        Protocol.PauseRequest request = Protocol.decodePause(data);
        if (request == null || !config.allowPause()) {
            return;
        }
        if (request.effectId() == null) {
            setAllPaused(player, request.paused());
        } else if (VanillaEffects.holder(request.effectId()).isPresent()) {
            setPaused(player, request.effectId(), request.paused());
        }
    }

    private void savePausesAndRefresh(ServerPlayer player) {
        try {
            pauses.save();
        } catch (IOException e) {
            EnderBrews.LOG.error("Could not save paused effects", e);
        }
        refresh(player, players.computeIfAbsent(player.getUUID(), id -> new PlayerState()));
    }

    private void refresh(ServerPlayer player, PlayerState state) {
        state.statuses = EndlessRules.evaluate(EnderChestScanner.scan(player, config), config, VanillaEffects.INSTANCE);

        Map<Holder<MobEffect>, Integer> endless = new LinkedHashMap<>();
        if (!player.isSpectator() || config.applyInSpectator()) {
            for (EffectStatus status : state.statuses) {
                if (status.state() == EffectStatus.State.ACTIVE && !isPaused(player, status.effectId())) {
                    VanillaEffects.holder(status.effectId()).ifPresent(effect -> endless.put(effect, status.amplifier()));
                }
            }
        }
        state.applied.forEach((effect, amplifier) -> {
            if (!Objects.equals(endless.get(effect), amplifier)) {
                EndlessEffects.remove(player, effect, amplifier);
            }
        });
        state.applied = endless;
        state.dirty = false;
        syncStatus(player, state);
    }

    /** Tells a client that has the mod where its effects stand, whenever that changes. */
    private void syncStatus(ServerPlayer player, PlayerState state) {
        if (network == null || !network.canSendStatus(player)) {
            return;
        }
        List<Protocol.Entry> entries = state.statuses.stream()
                .map(status -> new Protocol.Entry(status, isPaused(player, status.effectId())))
                .toList();
        byte[] data = Protocol.encodeSnapshot(new Protocol.Snapshot(config.allowPause(), entries));
        if (!Arrays.equals(data, state.lastSent)) {
            state.lastSent = data;
            network.sendStatus(player, data);
        }
    }

    private static final class PlayerState {
        List<EffectStatus> statuses = List.of();
        Map<Holder<MobEffect>, Integer> applied = new LinkedHashMap<>();
        boolean dirty = true;
        byte[] lastSent;
    }
}
