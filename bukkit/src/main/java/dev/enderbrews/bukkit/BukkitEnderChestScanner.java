package dev.enderbrews.bukkit;

import dev.enderbrews.bukkit.core.EnderBrewsConfig;
import dev.enderbrews.bukkit.core.PotionTally;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionType;

/** Counts the potions in a player's Ender Chest, including those inside shulker boxes there. */
final class BukkitEnderChestScanner {

    private BukkitEnderChestScanner() {
    }

    static PotionTally scan(Player player, EnderBrewsConfig config) {
        PotionTally tally = new PotionTally();
        for (ItemStack stack : player.getEnderChest().getContents()) {
            if (stack == null || stack.getType().isAir()) {
                continue;
            }
            countStack(stack, config, tally);
            // Only one level deep: containers inside containers do not count.
            if (config.countShulkerBoxes()
                    && stack.getItemMeta() instanceof BlockStateMeta meta
                    && meta.hasBlockState()
                    && meta.getBlockState() instanceof Container container) {
                for (ItemStack inner : container.getInventory().getContents()) {
                    if (inner != null && !inner.getType().isAir()) {
                        countStack(inner, config, tally);
                    }
                }
            }
        }
        return tally;
    }

    private static void countStack(ItemStack stack, EnderBrewsConfig config, PotionTally tally) {
        if (!isCountedPotion(stack.getType(), config)) {
            return;
        }
        ItemMeta itemMeta = stack.getItemMeta();
        if (!(itemMeta instanceof PotionMeta meta)) {
            return;
        }
        Map<String, Integer> effects = new HashMap<>();
        PotionType base = meta.getBasePotionType();
        if (base != null) {
            for (PotionEffect effect : base.getPotionEffects()) {
                effects.merge(BukkitEffects.id(effect.getType()), effect.getAmplifier(), Math::max);
            }
        }
        for (PotionEffect effect : meta.getCustomEffects()) {
            effects.merge(BukkitEffects.id(effect.getType()), effect.getAmplifier(), Math::max);
        }
        if (effects.isEmpty()) {
            return;
        }
        for (int i = 0; i < stack.getAmount(); i++) {
            tally.addPotion(effects);
        }
    }

    private static boolean isCountedPotion(Material type, EnderBrewsConfig config) {
        if (type == Material.POTION) {
            return true;
        }
        if (config.countSplashAndLingering() && (type == Material.SPLASH_POTION || type == Material.LINGERING_POTION)) {
            return true;
        }
        for (String entry : config.extraPotionItems()) {
            if (entry.startsWith("#")) {
                NamespacedKey key = NamespacedKey.fromString(entry.substring(1));
                Tag<Material> tag = key == null ? null : Bukkit.getTag(Tag.REGISTRY_ITEMS, key, Material.class);
                if (tag != null && tag.isTagged(type)) {
                    return true;
                }
            } else if (type.getKey().toString().equals(entry)) {
                return true;
            }
        }
        return false;
    }
}
