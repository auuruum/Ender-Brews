package dev.enderbrews.bukkit;

import dev.enderbrews.bukkit.core.EffectStatus;
import dev.enderbrews.bukkit.core.EnderBrewsConfig;
import dev.enderbrews.bukkit.core.EndlessRules;
import dev.enderbrews.bukkit.core.PauseStore;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.GameMode;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;

/** Bukkit / Spigot / Paper / Purpur entry point. Mirrors the mod's EnderBrewsServer. */
public final class EnderBrewsPlugin extends JavaPlugin implements Listener {

    private final Map<UUID, PlayerState> players = new HashMap<>();
    private EnderBrewsConfig config = EnderBrewsConfig.defaults();
    private PauseStore pauses;
    private long ticks;

    @Override
    public void onEnable() {
        try {
            reloadEnderBrewsConfig();
        } catch (IOException | IllegalArgumentException e) {
            getLogger().severe("Could not read " + EnderBrewsConfig.FILE_NAME + ", using defaults: " + e.getMessage());
        }
        pauses = new PauseStore(getDataFolder().toPath().resolve("paused.txt"));
        try {
            pauses.load();
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Could not read paused effects", e);
        }

        getServer().getPluginManager().registerEvents(this, this);
        PluginCommand command = Objects.requireNonNull(getCommand("enderbrews"));
        BukkitCommand executor = new BukkitCommand(this);
        command.setExecutor(executor);
        command.setTabCompleter(executor);
        getServer().getScheduler().runTaskTimer(this, this::tick, 1L, 1L);
        // Handles /reload and late enabling: players already online.
        getServer().getOnlinePlayers().forEach(player -> state(player).dirty = true);
    }

    @Override
    public void onDisable() {
        // Players are saved when the server stops; take endless effects off before that.
        getServer().getOnlinePlayers().forEach(this::removeEndless);
    }

    void reloadEnderBrewsConfig() throws IOException {
        config = EnderBrewsConfig.load(getDataFolder().toPath().resolve(EnderBrewsConfig.FILE_NAME));
        players.values().forEach(state -> state.dirty = true);
    }

    EnderBrewsConfig enderBrewsConfig() {
        return config;
    }

    private void tick() {
        boolean recount = ++ticks % config.checkIntervalTicks() == 0;
        for (Player player : getServer().getOnlinePlayers()) {
            PlayerState state = state(player);
            if (recount || state.dirty) {
                refresh(player, state);
            }
            BukkitEndlessEffects.keep(player, state.applied, config);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (config.removeOnLogout()) {
            BukkitEndlessEffects.removeLeftovers(event.getPlayer());
        }
        state(event.getPlayer()).dirty = true;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (config.removeOnLogout()) {
            removeEndless(event.getPlayer());
        }
        players.remove(event.getPlayer().getUniqueId());
    }

    private void removeEndless(Player player) {
        PlayerState state = players.get(player.getUniqueId());
        if (state != null) {
            state.applied.forEach((type, amplifier) -> BukkitEndlessEffects.remove(player, type, amplifier));
        }
    }

    List<EffectStatus> statuses(Player player) {
        PlayerState state = state(player);
        refresh(player, state);
        return state.statuses;
    }

    boolean isPaused(Player player, String effectId) {
        return config.allowPause() && pauses.isPaused(player.getUniqueId(), effectId);
    }

    void setPaused(Player player, String effectId, boolean paused) {
        pauses.setPaused(player.getUniqueId(), effectId, paused);
        try {
            pauses.save();
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Could not save paused effects", e);
        }
        refresh(player, state(player));
    }

    private PlayerState state(Player player) {
        return players.computeIfAbsent(player.getUniqueId(), id -> new PlayerState());
    }

    private void refresh(Player player, PlayerState state) {
        state.statuses = EndlessRules.evaluate(BukkitEnderChestScanner.scan(player, config), config, BukkitEffects.INSTANCE);

        Map<PotionEffectType, Integer> endless = new LinkedHashMap<>();
        if (player.getGameMode() != GameMode.SPECTATOR || config.applyInSpectator()) {
            for (EffectStatus status : state.statuses) {
                PotionEffectType type = BukkitEffects.type(status.effectId());
                if (status.state() == EffectStatus.State.ACTIVE && type != null && !isPaused(player, status.effectId())) {
                    endless.put(type, status.amplifier());
                }
            }
        }
        state.applied.forEach((type, amplifier) -> {
            if (!Objects.equals(endless.get(type), amplifier)) {
                BukkitEndlessEffects.remove(player, type, amplifier);
            }
        });
        state.applied = endless;
        state.dirty = false;
    }

    private static final class PlayerState {
        List<EffectStatus> statuses = List.of();
        Map<PotionEffectType, Integer> applied = new LinkedHashMap<>();
        boolean dirty = true;
    }
}
