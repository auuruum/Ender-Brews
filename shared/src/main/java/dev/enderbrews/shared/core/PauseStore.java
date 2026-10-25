package dev.enderbrews.shared.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/**
 * Which endless effects each player has paused. Survives death, relogging and restarts.
 * Stored as one line per player: {@code <uuid> <effect>,<effect>}.
 */
public final class PauseStore {

    private final Path file;
    private final Map<UUID, Set<String>> paused = new HashMap<>();

    public PauseStore(Path file) {
        this.file = file;
    }

    public void load() throws IOException {
        paused.clear();
        if (Files.notExists(file)) {
            return;
        }
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            String[] parts = line.trim().split("\\s+", 2);
            if (parts.length < 2) {
                continue;
            }
            try {
                Set<String> effects = new TreeSet<>(List.of(parts[1].split(",")));
                effects.removeIf(String::isBlank);
                paused.put(UUID.fromString(parts[0]), effects);
            } catch (IllegalArgumentException ignored) {
                // Skip lines someone broke by hand.
            }
        }
    }

    public void save() throws IOException {
        List<String> lines = new ArrayList<>();
        paused.forEach((player, effects) -> {
            if (!effects.isEmpty()) {
                lines.add(player + " " + String.join(",", effects));
            }
        });
        Files.createDirectories(file.getParent());
        Files.write(file, lines, StandardCharsets.UTF_8);
    }

    public boolean isPaused(UUID player, String effectId) {
        return paused.getOrDefault(player, Set.of()).contains(effectId);
    }

    public void setPaused(UUID player, String effectId, boolean pause) {
        Set<String> effects = paused.computeIfAbsent(player, p -> new TreeSet<>());
        if (pause) {
            effects.add(effectId);
        } else {
            effects.remove(effectId);
        }
    }
}
