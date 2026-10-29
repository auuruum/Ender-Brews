package dev.enderbrews.shared.common;

import dev.enderbrews.shared.core.EffectInfo;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** Answers effect questions from the vanilla registry. */
final class VanillaEffects implements EffectInfo {

    private static final MethodHandle IS_INSTANT = isInstantHandle();
    static final VanillaEffects INSTANCE = new VanillaEffects();

    private VanillaEffects() {
    }

    static Optional<Holder.Reference<MobEffect>> holder(String effectId) {
        Identifier id = Identifier.tryParse(effectId);
        return id == null ? Optional.empty() : BuiltInRegistries.MOB_EFFECT.get(id);
    }

    static String id(Holder<MobEffect> effect) {
        return effect.unwrapKey().map(key -> key.identifier().toString()).orElse("unknown:unknown");
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
        return holder(effectId).map(h -> {
            try {
                return (boolean) IS_INSTANT.invoke(h.value());
            } catch (Throwable e) {
                throw new IllegalStateException(e);
            }
        }).orElse(false);
    }

    /** The misspelled isInstantenous was renamed to isInstantaneous in 26.2. */
    private static MethodHandle isInstantHandle() {
        MethodType type = MethodType.methodType(boolean.class);
        try {
            try {
                return MethodHandles.publicLookup().findVirtual(MobEffect.class, "isInstantaneous", type);
            } catch (NoSuchMethodException e) {
                return MethodHandles.publicLookup().findVirtual(MobEffect.class, "isInstantenous", type);
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public boolean matches(String effectId, String entry) {
        if (!entry.startsWith("#")) {
            return entry.equals(effectId);
        }
        Identifier tag = Identifier.tryParse(entry.substring(1));
        return tag != null && holder(effectId).map(h -> h.is(TagKey.create(Registries.MOB_EFFECT, tag))).orElse(false);
    }
}
