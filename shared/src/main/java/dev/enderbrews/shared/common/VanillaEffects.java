package dev.enderbrews.shared.common;

import dev.enderbrews.shared.core.EffectInfo;
import java.util.Optional;
import net.minecraft.core.Holder;
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

    static Optional<Holder.Reference<MobEffect>> holder(String effectId) {
        ResourceLocation id = ResourceLocation.tryParse(effectId);
        return id == null ? Optional.empty() : BuiltInRegistries.MOB_EFFECT.getHolder(id);
    }

    static String id(Holder<MobEffect> effect) {
        return effect.unwrapKey().map(key -> key.location().toString()).orElse("unknown:unknown");
    }

    @Override
    public boolean exists(String effectId) {
        return holder(effectId).isPresent();
    }

    @Override
    public boolean isHarmful(String effectId) {
        return holder(effectId).map(h -> h.value().getCategory() == MobEffectCategory.HARMFUL).orElse(false);
    }

    @Override
    public boolean isInstant(String effectId) {
        return holder(effectId).map(h -> h.value().isInstantenous()).orElse(false);
    }

    @Override
    public boolean matches(String effectId, String entry) {
        if (!entry.startsWith("#")) {
            return entry.equals(effectId);
        }
        ResourceLocation tag = ResourceLocation.tryParse(entry.substring(1));
        return tag != null && holder(effectId).map(h -> h.is(TagKey.create(Registries.MOB_EFFECT, tag))).orElse(false);
    }
}
