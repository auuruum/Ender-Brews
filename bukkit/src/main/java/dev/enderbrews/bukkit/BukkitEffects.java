package dev.enderbrews.bukkit;

import dev.enderbrews.bukkit.core.EffectInfo;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionEffectTypeCategory;

/** Answers effect questions from the Bukkit registry. Effect tags are not available on Bukkit. */
final class BukkitEffects implements EffectInfo {

    static final BukkitEffects INSTANCE = new BukkitEffects();

    private BukkitEffects() {
    }

    static PotionEffectType type(String effectId) {
        NamespacedKey key = NamespacedKey.fromString(effectId);
        return key == null ? null : Registry.EFFECT.get(key);
    }

    static String id(PotionEffectType type) {
        return type.getKey().toString();
    }

    /** Vanilla translation key, so every client shows the name in its own language. */
    static String translationKey(String effectId) {
        NamespacedKey key = NamespacedKey.fromString(effectId);
        return key == null ? effectId : "effect." + key.getNamespace() + "." + key.getKey();
    }

    @Override
    public boolean exists(String effectId) {
        return type(effectId) != null;
    }

    @Override
    public boolean isHarmful(String effectId) {
        PotionEffectType type = type(effectId);
        return type != null && type.getCategory() == PotionEffectTypeCategory.HARMFUL;
    }

    @Override
    public boolean isInstant(String effectId) {
        PotionEffectType type = type(effectId);
        return type != null && type.isInstant();
    }

    @Override
    public boolean matches(String effectId, String entry) {
        return entry.equals(effectId);
    }
}
