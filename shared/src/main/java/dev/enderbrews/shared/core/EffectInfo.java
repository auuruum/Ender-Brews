package dev.enderbrews.shared.core;

/** What the rules need to know about an effect; each platform answers from its own registry. */
public interface EffectInfo {

    boolean exists(String effectId);

    boolean isHarmful(String effectId);

    boolean isInstant(String effectId);

    /** Whether {@code effectId} matches a config entry: an effect id or {@code #namespace:tag}. */
    boolean matches(String effectId, String entry);
}
