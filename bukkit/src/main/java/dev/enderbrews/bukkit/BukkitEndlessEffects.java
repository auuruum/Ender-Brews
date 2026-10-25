package dev.enderbrews.bukkit;

import dev.enderbrews.bukkit.core.EnderBrewsConfig;
import java.util.Map;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Gives and takes endless effects; the Bukkit twin of the mod's EndlessEffects. An endless effect
 * is an infinite, ambient effect. Vanilla handles stronger and weaker potions on top of it, and
 * {@link #keep} puts endless effects back every tick after milk, totems or /effect clear.
 */
final class BukkitEndlessEffects {

    private BukkitEndlessEffects() {
    }

    static PotionEffect create(PotionEffectType type, int amplifier, EnderBrewsConfig config) {
        return new PotionEffect(type, PotionEffect.INFINITE_DURATION, amplifier, true,
                config.showParticles(), config.showIcon());
    }

    static boolean isOurs(PotionEffect effect, int amplifier) {
        return effect.isInfinite() && effect.isAmbient() && effect.getAmplifier() == amplifier;
    }

    static void keep(Player player, Map<PotionEffectType, Integer> endless, EnderBrewsConfig config) {
        endless.forEach((type, amplifier) -> {
            PotionEffect current = player.getPotionEffect(type);
            if (current == null
                    || current.getAmplifier() < amplifier
                    || (current.getAmplifier() == amplifier && !current.isInfinite())) {
                player.addPotionEffect(create(type, amplifier, config));
            }
        });
    }

    static void remove(Player player, PotionEffectType type, int amplifier) {
        PotionEffect current = player.getPotionEffect(type);
        if (current == null) {
            return;
        }
        if (isOurs(current, amplifier)) {
            player.removePotionEffect(type);
        } else if (!current.isInfinite()) {
            // A drunk potion is on top with our endless effect hidden underneath; keep only the potion.
            PotionEffect potion = new PotionEffect(type, current.getDuration(), current.getAmplifier(),
                    current.isAmbient(), current.hasParticles(), current.hasIcon());
            player.removePotionEffect(type);
            player.addPotionEffect(potion);
        }
    }

    static void removeLeftovers(Player player) {
        for (PotionEffect effect : player.getActivePotionEffects()) {
            if (effect.isInfinite() && effect.isAmbient()) {
                player.removePotionEffect(effect.getType());
            }
        }
    }
}
