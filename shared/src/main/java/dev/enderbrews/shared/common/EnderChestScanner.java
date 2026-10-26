package dev.enderbrews.shared.common;

import dev.enderbrews.shared.core.EnderBrewsConfig;
import dev.enderbrews.shared.core.PotionTally;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.ItemContainerContents;

/** Counts the potions in a player's Ender Chest, including those inside shulker boxes there. */
final class EnderChestScanner {

    private EnderChestScanner() {
    }

    static PotionTally scan(ServerPlayer player, EnderBrewsConfig config) {
        PotionTally tally = new PotionTally();
        Container enderChest = player.getEnderChestInventory();
        for (int slot = 0; slot < enderChest.getContainerSize(); slot++) {
            ItemStack stack = enderChest.getItem(slot);
            countStack(stack, config, tally);
            if (config.countShulkerBoxes()) {
                // Shulker boxes (and any modded container item using the vanilla component) hold
                // their items here. Only one level deep: containers inside containers do not count.
                ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
                if (contents != null) {
                    contents.nonEmptyItemCopyStream().forEach(inner -> countStack(inner, config, tally));
                }
            }
        }
        return tally;
    }

    private static void countStack(ItemStack stack, EnderBrewsConfig config, PotionTally tally) {
        if (stack.isEmpty() || !isCountedPotion(stack, config)) {
            return;
        }
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        if (contents == null) {
            return;
        }
        Map<String, Integer> effects = new HashMap<>();
        for (MobEffectInstance effect : contents.getAllEffects()) {
            effects.merge(VanillaEffects.id(effect.getEffect()), effect.getAmplifier(), Math::max);
        }
        if (effects.isEmpty()) {
            return;
        }
        // Potions normally don't stack, but other mods may let them; every potion counts.
        for (int i = 0; i < stack.getCount(); i++) {
            tally.addPotion(effects);
        }
    }

    private static boolean isCountedPotion(ItemStack stack, EnderBrewsConfig config) {
        if (stack.is(Items.POTION)) {
            return true;
        }
        if (config.countSplashAndLingering() && (stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION))) {
            return true;
        }
        for (String entry : config.extraPotionItems()) {
            if (entry.startsWith("#")) {
                Identifier tag = Identifier.tryParse(entry.substring(1));
                if (tag != null && stack.is(TagKey.create(Registries.ITEM, tag))) {
                    return true;
                }
            } else if (BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(entry)) {
                return true;
            }
        }
        return false;
    }
}
