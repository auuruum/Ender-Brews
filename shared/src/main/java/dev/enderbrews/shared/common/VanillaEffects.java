package dev.enderbrews.shared.common;

import dev.enderbrews.shared.core.EffectInfo;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** Answers effect questions from the vanilla registry. */
final class VanillaEffects implements EffectInfo {

    static final VanillaEffects INSTANCE = new VanillaEffects();

    private VanillaEffects() {
    }

    static Optional<MobEffect> effect(String effectId) {
        ResourceLocation id = ResourceLocation.tryParse(effectId);
        return id == null ? Optional.empty() : BuiltInRegistries.MOB_EFFECT.getOptional(id);
    }

    static String id(MobEffect effect) {
        ResourceLocation id = BuiltInRegistries.MOB_EFFECT.getKey(effect);
        return id == null ? "unknown:unknown" : id.toString();
    }

    @Override
    public boolean exists(String effectId) {
        return effect(effectId).isPresent();
    }

    @Override
    public boolean isHarmful(String effectId) {
        return effect(effectId).map(e -> e.getCategory() == MobEffectCategory.HARMFUL).orElse(false);
    }

    @Override
    public boolean isInstant(String effectId) {
        return effect(effectId).map(MobEffect::isInstantenous).orElse(false);
    }

    @Override
    public boolean matches(String effectId, String entry) {
        if (!entry.startsWith("#")) {
            return entry.equals(effectId);
        }
        ResourceLocation tag = ResourceLocation.tryParse(entry.substring(1));
        return tag != null && effect(effectId)
                .flatMap(BuiltInRegistries.MOB_EFFECT::getResourceKey)
                .flatMap(BuiltInRegistries.MOB_EFFECT::getHolder)
                .map(h -> h.is(TagKey.create(Registries.MOB_EFFECT, tag)))
                .orElse(false);
    }
}
