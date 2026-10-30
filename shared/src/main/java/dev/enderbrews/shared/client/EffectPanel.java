package dev.enderbrews.shared.client;

import com.google.common.collect.Ordering;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.enderbrews.shared.core.EffectStatus;
import dev.enderbrews.shared.core.Protocol;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

/**
 * Makes the effect list next to the inventory clickable: click an endless effect to pause it.
 * Paused effects are no longer on the player, so they are drawn greyed out under the vanilla list,
 * where a click resumes them. The layout mirrors vanilla's effect list.
 */
final class EffectPanel {

    private static final int ROW_HEIGHT = 32;

    private EffectPanel() {
    }

    /** {@code maxWidth} is 32 in the narrow layout, which only shows icons. */
    private record Layout(int x, int top, int step, int maxWidth, List<MobEffectInstance> active,
                          List<MobEffect> paused) {

        boolean wide() {
            return maxWidth > ROW_HEIGHT;
        }

        /** The row under the mouse, or -1. Rows after the vanilla ones are paused effects. */
        int rowAt(double mouseX, double mouseY) {
            if (mouseX < x || mouseX > x + maxWidth || mouseY < top) {
                return -1;
            }
            int row = (int) ((mouseY - top) / step);
            boolean insideRow = mouseY - top - row * step <= ROW_HEIGHT;
            return insideRow && row < active.size() + paused.size() ? row : -1;
        }
    }

    private static Layout layout(Screen screen) {
        Protocol.Snapshot snapshot = EnderBrewsClient.snapshot();
        Minecraft minecraft = Minecraft.getInstance();
        if (snapshot == null || minecraft.player == null
                || !(screen instanceof EffectRenderingInventoryScreen<?> inventory) || !inventory.canSeeEffects()) {
            return null;
        }
        ClientPlatform platform = EnderBrewsClient.platform();
        int x = platform.guiLeft(inventory) + platform.imageWidth(inventory) + 2;
        int availableWidth = screen.width - x;
        List<MobEffectInstance> active = Ordering.natural().sortedCopy(minecraft.player.getActiveEffects());
        int step = active.size() > 5 ? 132 / (active.size() - 1) : 33;
        List<MobEffect> paused = new ArrayList<>();
        for (Protocol.Entry entry : snapshot.entries()) {
            if (entry.paused() && entry.status().state() == EffectStatus.State.ACTIVE) {
                BuiltInRegistries.MOB_EFFECT.getOptional(new ResourceLocation(entry.status().effectId())).ifPresent(paused::add);
            }
        }
        int maxWidth = availableWidth >= 120 ? 120 : ROW_HEIGHT;
        return new Layout(x, platform.guiTop(inventory), step, maxWidth, active, paused);
    }

    static void render(Screen screen, GuiGraphics graphics, int mouseX, int mouseY) {
        Layout layout = layout(screen);
        if (layout == null) {
            return;
        }
        Font font = Minecraft.getInstance().font;
        Protocol.Snapshot snapshot = EnderBrewsClient.snapshot();

        for (int i = 0; i < layout.paused().size(); i++) {
            MobEffect effect = layout.paused().get(i);
            int x = layout.x();
            int y = layout.top() + (layout.active().size() + i) * layout.step();
            Component name = PotionTooltips.name(effect, snapshot.find(PotionTooltips.effectId(effect)).status().amplifier());
            Component label = Component.translatable("enderbrews.effect.paused");
            RenderSystem.enableBlend();
            graphics.setColor(1.0F, 1.0F, 1.0F, 0.5F);
            graphics.blit(AbstractContainerScreen.INVENTORY_LOCATION, x, y, 0, layout.wide() ? 166 : 198, layout.maxWidth(), ROW_HEIGHT);
            graphics.blit(x + (layout.wide() ? 6 : 7), y + 7, 0, 18, 18, Minecraft.getInstance().getMobEffectTextures().get(effect));
            graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.disableBlend();
            if (layout.wide()) {
                graphics.drawString(font, name, x + 28, y + 6, 0xFF8F8F8F);
                graphics.drawString(font, label, x + 28, y + 16, 0xFF6F6F6F);
            }
        }

        if (!EnderBrewsClient.canPause()) {
            return;
        }
        int row = layout.rowAt(mouseX, mouseY);
        if (row >= layout.active().size()) {
            MobEffect effect = layout.paused().get(row - layout.active().size());
            tooltip(graphics, font, List.of(
                    PotionTooltips.name(effect, snapshot.find(PotionTooltips.effectId(effect)).status().amplifier()),
                    Component.translatable("enderbrews.effect.click_resume")), mouseX, mouseY);
        } else if (row >= 0 && layout.wide() && isPausable(layout.active().get(row), snapshot)) {
            // In the narrow layout vanilla already shows a tooltip on hover; don't draw over it.
            tooltip(graphics, font, List.of(Component.translatable("enderbrews.effect.click_pause")), mouseX, mouseY);
        }
    }

    private static void tooltip(GuiGraphics graphics, Font font, List<Component> lines, int mouseX, int mouseY) {
        graphics.renderTooltip(font, lines, Optional.empty(), mouseX, mouseY);
    }

    static boolean mouseClicked(Screen screen, double mouseX, double mouseY, int button) {
        Layout layout = layout(screen);
        if (button != 0 || layout == null || !EnderBrewsClient.canPause()) {
            return false;
        }
        int row = layout.rowAt(mouseX, mouseY);
        if (row < 0) {
            return false;
        }
        if (row >= layout.active().size()) {
            EnderBrewsClient.requestPause(PotionTooltips.effectId(layout.paused().get(row - layout.active().size())), false);
        } else if (isPausable(layout.active().get(row), EnderBrewsClient.snapshot())) {
            EnderBrewsClient.requestPause(PotionTooltips.effectId(layout.active().get(row).getEffect()), true);
        } else {
            return false;
        }
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        return true;
    }

    /** Whether this effect on the player is an endless one from Ender Brews. */
    private static boolean isPausable(MobEffectInstance effect, Protocol.Snapshot snapshot) {
        Protocol.Entry entry = snapshot.find(PotionTooltips.effectId(effect.getEffect()));
        return entry != null && !entry.paused() && entry.status().state() == EffectStatus.State.ACTIVE
                && effect.isInfiniteDuration() && effect.isAmbient();
    }
}
