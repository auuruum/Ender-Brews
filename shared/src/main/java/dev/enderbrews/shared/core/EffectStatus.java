package dev.enderbrews.shared.core;

/**
 * Where one effect stands for a player.
 *
 * @param amplifier the level that is endless (for {@link State#ACTIVE}) or that {@code count} refers to
 * @param count     potions counted for that level
 */
public record EffectStatus(String effectId, int amplifier, int count, int threshold, State state) {

    public enum State {
        /** Enough potions: the effect is endless (unless the player paused it). */
        ACTIVE,
        /** Not enough potions yet. */
        PROGRESS,
        /** Enough potions, but the player already has the maximum number of endless effects. */
        LIMITED,
        /** The config does not allow this effect to become endless. */
        BLOCKED
    }
}
