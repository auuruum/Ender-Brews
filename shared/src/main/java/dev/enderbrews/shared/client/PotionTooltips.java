package dev.enderbrews.shared.client;

import dev.enderbrews.shared.core.EffectStatus;
import dev.enderbrews.shared.core.Protocol;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;

/** Adds "5/8 in Ender Chest" or "endless" lines to potion tooltips. */
final class PotionTooltips {

    private PotionTooltips() {
    }

    static void append(ItemStack stack, List<Component> lines) {
        Protocol.Snapshot snapshot = EnderBrewsClient.snapshot();
        if (snapshot == null || !stack.is(Items.POTION)) {
            return;
        }
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        if (contents == null) {
            return;
        }
        Set<String> seen = new HashSet<>();
        for (MobEffectInstance effect : contents.getAllEffects()) {
            String effectId = effectId(effect.getEffect());
            Protocol.Entry entry = snapshot.find(effectId);
            if (entry == null || !seen.add(effectId)) {
                continue;
            }
            EffectStatus status = entry.status();
            Component name = name(effect.getEffect(), status.amplifier());
            lines.add(switch (status.state()) {
                case ACTIVE -> entry.paused()
                        ? Component.translatable("enderbrews.tooltip.paused", name).withStyle(ChatFormatting.GRAY)
                        : Component.translatable("enderbrews.tooltip.endless", name).withStyle(ChatFormatting.GOLD);
                case PROGRESS -> Component.translatable("enderbrews.tooltip.progress", name, status.count(), status.threshold())
                        .withStyle(ChatFormatting.LIGHT_PURPLE);
                case LIMITED -> Component.translatable("enderbrews.tooltip.limited", name).withStyle(ChatFormatting.YELLOW);
                case BLOCKED -> Component.translatable("enderbrews.tooltip.blocked", name).withStyle(ChatFormatting.DARK_GRAY);
            });
        }
    }

    static String effectId(Holder<MobEffect> effect) {
        return effect.unwrapKey().map(key -> key.location().toString()).orElse("");
    }

    /** Effect name with its level, written the way the inventory shows it. */
    static MutableComponent name(Holder<MobEffect> effect, int amplifier) {
        MutableComponent name = effect.value().getDisplayName().copy();
        if (amplifier >= 1 && amplifier <= 9) {
            name.append(CommonComponents.SPACE).append(Component.translatable("enchantment.level." + (amplifier + 1)));
        }
        return name;
    }
}
