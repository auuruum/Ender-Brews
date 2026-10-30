package dev.enderbrews.bukkit;

import dev.enderbrews.bukkit.core.EffectInfo;
import java.util.Set;
import org.bukkit.NamespacedKey;
import org.bukkit.potion.PotionEffectType;

/** Answers effect questions from the Bukkit registry. Effect tags are not available on Bukkit. */
final class BukkitEffects implements EffectInfo {

    static final BukkitEffects INSTANCE = new BukkitEffects();

    // shortcut: the Bukkit 1.20.1 API has no effect categories, so this lists vanilla's harmful effects.
    private static final Set<String> HARMFUL = Set.of("minecraft:slowness", "minecraft:mining_fatigue",
            "minecraft:instant_damage", "minecraft:nausea", "minecraft:blindness", "minecraft:hunger",
            "minecraft:weakness", "minecraft:poison", "minecraft:wither", "minecraft:levitation",
            "minecraft:unluck", "minecraft:darkness");

    private BukkitEffects() {
    }

    static PotionEffectType type(String effectId) {
        NamespacedKey key = NamespacedKey.fromString(effectId);
        return key == null ? null : PotionEffectType.getByKey(key);
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
        return HARMFUL.contains(effectId);
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
