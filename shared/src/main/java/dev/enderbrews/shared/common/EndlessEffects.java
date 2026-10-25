package dev.enderbrews.shared.common;

import dev.enderbrews.shared.core.EnderBrewsConfig;
import java.util.ArrayList;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

/**
 * Gives and takes endless effects. An endless effect is an infinite, ambient effect instance;
 * that pair of flags is how we recognise our own effects among everything else on the player.
 *
 * <p>Vanilla does most of the edge cases for us: drinking a stronger potion hides the endless
 * effect underneath it and restores it when the potion runs out, and a weaker or equal potion
 * simply does nothing. Milk, totems and {@code /effect clear} remove everything; {@link #keep}
 * runs every tick and puts endless effects back right away.
 */
final class EndlessEffects {

    private EndlessEffects() {
    }

    static MobEffectInstance create(Holder<MobEffect> effect, int amplifier, EnderBrewsConfig config) {
        return new MobEffectInstance(effect, MobEffectInstance.INFINITE_DURATION, amplifier, true,
                config.showParticles(), config.showIcon());
    }

    static boolean isOurs(MobEffectInstance instance, int amplifier) {
        return instance.isInfiniteDuration() && instance.isAmbient() && instance.getAmplifier() == amplifier;
    }

    /** Makes sure every endless effect is on the player, unless a stronger one already covers it. */
    static void keep(ServerPlayer player, Map<Holder<MobEffect>, Integer> endless, EnderBrewsConfig config) {
        endless.forEach((effect, amplifier) -> {
            MobEffectInstance current = player.getEffect(effect);
            if (current == null
                    || current.getAmplifier() < amplifier
                    || (current.getAmplifier() == amplifier && !current.isInfiniteDuration())) {
                player.addEffect(create(effect, amplifier, config));
            }
        });
    }

    /** Takes one endless effect away without touching effects that came from elsewhere. */
    static void remove(ServerPlayer player, Holder<MobEffect> effect, int amplifier) {
        MobEffectInstance current = player.getEffect(effect);
        if (current == null) {
            return;
        }
        if (isOurs(current, amplifier)) {
            player.removeEffect(effect);
        } else if (!current.isInfiniteDuration()) {
            // A drunk potion is on top and vanilla keeps our endless effect hidden underneath it, to
            // bring it back when the potion runs out. Re-add the potion on its own to drop that copy.
            MobEffectInstance potion = new MobEffectInstance(effect, current.getDuration(), current.getAmplifier(),
                    current.isAmbient(), current.isVisible(), current.showIcon());
            player.removeEffect(effect);
            player.addEffect(potion);
        }
    }

    /** Removes endless effects left on a player, e.g. saved by a crash before they could be taken away. */
    static void removeLeftovers(ServerPlayer player) {
        for (MobEffectInstance instance : new ArrayList<>(player.getActiveEffects())) {
            if (instance.isInfiniteDuration() && instance.isAmbient()) {
                player.removeEffect(instance.getEffect());
            }
        }
    }
}
