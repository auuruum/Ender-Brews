package dev.enderbrews.shared.client;

import com.google.common.collect.Ordering;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.enderbrews.shared.core.EffectStatus;
import dev.enderbrews.shared.core.Protocol;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

/**
 * Makes the effect list next to the inventory clickable: click an endless effect to pause it.
 * Paused effects are no longer on the player, so they are drawn greyed out under the vanilla list,
 * where a click resumes them. The layout mirrors vanilla's EffectRenderingInventoryScreen.
 */
final class EffectPanel {

    private static final ResourceLocation BACKGROUND_LARGE = ResourceLocation.withDefaultNamespace("container/inventory/effect_background_large");
    private static final ResourceLocation BACKGROUND_SMALL = ResourceLocation.withDefaultNamespace("container/inventory/effect_background_small");
    private static final int ROW_HEIGHT = 32;

    private EffectPanel() {
    }

    private record Layout(int x, int top, int step, boolean wide, List<MobEffectInstance> active,
                          List<Holder<MobEffect>> paused) {

        int width() {
            return wide ? 120 : 32;
        }

        /** The row under the mouse, or -1. Rows after the vanilla ones are paused effects. */
        int rowAt(double mouseX, double mouseY) {
            if (mouseX < x || mouseX > x + width() || mouseY < top) {
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
        List<MobEffectInstance> active = Ordering.natural().sortedCopy(minecraft.player.getActiveEffects());
        int step = active.size() > 5 ? 132 / (active.size() - 1) : 33;
        List<Holder<MobEffect>> paused = new ArrayList<>();
        for (Protocol.Entry entry : snapshot.entries()) {
            if (entry.paused() && entry.status().state() == EffectStatus.State.ACTIVE) {
                BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.parse(entry.status().effectId())).ifPresent(paused::add);
            }
        }
        return new Layout(x, platform.guiTop(inventory), step, screen.width - x >= 120, active, paused);
    }

    static void render(Screen screen, GuiGraphics graphics, int mouseX, int mouseY) {
        Layout layout = layout(screen);
        if (layout == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Protocol.Snapshot snapshot = EnderBrewsClient.snapshot();

        for (int i = 0; i < layout.paused().size(); i++) {
            Holder<MobEffect> effect = layout.paused().get(i);
            int y = layout.top() + (layout.active().size() + i) * layout.step();
            RenderSystem.enableBlend();
            graphics.setColor(1.0F, 1.0F, 1.0F, 0.5F);
            graphics.blitSprite(layout.wide() ? BACKGROUND_LARGE : BACKGROUND_SMALL, layout.x(), y, layout.width(), ROW_HEIGHT);
            graphics.blit(layout.x() + (layout.wide() ? 6 : 7), y + 7, 0, 18, 18, minecraft.getMobEffectTextures().get(effect));
            graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.disableBlend();
            if (layout.wide()) {
                int amplifier = snapshot.find(PotionTooltips.effectId(effect)).status().amplifier();
                graphics.drawString(minecraft.font, PotionTooltips.name(effect, amplifier), layout.x() + 28, y + 6, 0x8F8F8F);
                graphics.drawString(minecraft.font, Component.translatable("enderbrews.effect.paused"), layout.x() + 28, y + 16, 0x6F6F6F);
            }
        }

        if (!EnderBrewsClient.canPause()) {
            return;
        }
        int row = layout.rowAt(mouseX, mouseY);
        if (row >= layout.active().size()) {
            Holder<MobEffect> effect = layout.paused().get(row - layout.active().size());
            graphics.renderTooltip(minecraft.font, List.of(
                    PotionTooltips.name(effect, snapshot.find(PotionTooltips.effectId(effect)).status().amplifier()),
                    Component.translatable("enderbrews.effect.click_resume")), Optional.empty(), mouseX, mouseY);
        } else if (row >= 0 && layout.wide() && isPausable(layout.active().get(row), snapshot)) {
            // In the narrow layout vanilla already shows a tooltip on hover; don't draw over it.
            graphics.renderTooltip(minecraft.font, Component.translatable("enderbrews.effect.click_pause"), mouseX, mouseY);
        }
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
