package dev.enderbrews.shared.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/** How many potions of each effect and amplifier a player keeps in their Ender Chest. */
public final class PotionTally {

    private final Map<String, NavigableMap<Integer, Integer>> counts = new LinkedHashMap<>();

    /**
     * Counts one potion. {@code effects} maps effect id to amplifier; a potion that lists the same
     * effect twice should pass it once with the highest amplifier.
     */
    public void addPotion(Map<String, Integer> effects) {
        effects.forEach((effectId, amplifier) ->
                counts.computeIfAbsent(effectId, id -> new TreeMap<>()).merge(Math.max(0, amplifier), 1, Integer::sum));
    }

    public boolean isEmpty() {
        return counts.isEmpty();
    }

    /** Effect id to (amplifier to number of potions). */
    public Map<String, NavigableMap<Integer, Integer>> counts() {
        return Collections.unmodifiableMap(counts);
    }
}
