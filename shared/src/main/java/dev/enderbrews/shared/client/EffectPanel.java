package dev.enderbrews.shared.client;

import com.google.common.collect.Ordering;
import dev.enderbrews.shared.core.EffectStatus;
import dev.enderbrews.shared.core.Protocol;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
//#if >=1.21.6
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.renderer.RenderPipelines;
//#elif >=1.21.2
import java.util.Optional;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderType;
//#else
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.Optional;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
//#endif

/**
 * Makes the effect list next to the inventory clickable: click an endless effect to pause it.
 * Paused effects are no longer on the player, so they are drawn greyed out under the vanilla list,
 * where a click resumes them. The layout mirrors vanilla's effect list of each Minecraft version.
 */
final class EffectPanel {

    //#if >=1.21.11
    private static final ResourceLocation BACKGROUND = ResourceLocation.withDefaultNamespace("container/inventory/effect_background");
    //#else
    private static final ResourceLocation BACKGROUND_LARGE = ResourceLocation.withDefaultNamespace("container/inventory/effect_background_large");
    private static final ResourceLocation BACKGROUND_SMALL = ResourceLocation.withDefaultNamespace("container/inventory/effect_background_small");
    //#endif
    private static final int ROW_HEIGHT = 32;
    private static final int TEXT_X_OFFSET = 32;
    private static final int SPACING = 7;
    private static final int GREYED = 0x80FFFFFF;

    private EffectPanel() {
    }

    /** {@code maxWidth} is 32 in the narrow layout, which only shows icons. */
    private record Layout(int x, int top, int step, int maxWidth, List<MobEffectInstance> active,
                          List<Holder<MobEffect>> paused) {

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
        //#if >=1.21.2
        if (snapshot == null || minecraft.player == null
                || !(screen instanceof AbstractContainerScreen<?> inventory) || !screen.showsActiveEffects()) {
            return null;
        }
        //#else
        if (snapshot == null || minecraft.player == null
                || !(screen instanceof EffectRenderingInventoryScreen<?> inventory) || !inventory.canSeeEffects()) {
            return null;
        }
        //#endif
        ClientPlatform platform = EnderBrewsClient.platform();
        int x = platform.guiLeft(inventory) + platform.imageWidth(inventory) + 2;
        int availableWidth = screen.width - x;
        List<MobEffectInstance> active = Ordering.natural().sortedCopy(minecraft.player.getActiveEffects());
        int step = active.size() > 5 ? 132 / (active.size() - 1) : 33;
        List<Holder<MobEffect>> paused = new ArrayList<>();
        for (Protocol.Entry entry : snapshot.entries()) {
            if (entry.paused() && entry.status().state() == EffectStatus.State.ACTIVE) {
                //#if >=1.21.2
                BuiltInRegistries.MOB_EFFECT.get(ResourceLocation.parse(entry.status().effectId())).ifPresent(paused::add);
                //#else
                BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.parse(entry.status().effectId())).ifPresent(paused::add);
                //#endif
            }
        }
        //#if >=1.21.11
        int maxWidth = availableWidth >= 120 ? availableWidth - SPACING : ROW_HEIGHT;
        //#else
        int maxWidth = availableWidth >= 120 ? 120 : ROW_HEIGHT;
        //#endif
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
            Holder<MobEffect> effect = layout.paused().get(i);
            int x = layout.x();
            int y = layout.top() + (layout.active().size() + i) * layout.step();
            Component name = PotionTooltips.name(effect, snapshot.find(PotionTooltips.effectId(effect)).status().amplifier());
            Component label = Component.translatable("enderbrews.effect.paused");
            //#if >=1.21.11
            int width = Math.min(layout.maxWidth(), TEXT_X_OFFSET + Math.max(font.width(name), font.width(label)) + SPACING);
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND, x, y, width, ROW_HEIGHT, GREYED);
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, Gui.getMobEffectSprite(effect), x + 7, y + 7, 18, 18, GREYED);
            if (width > TEXT_X_OFFSET + SPACING) {
                graphics.drawString(font, name, x + TEXT_X_OFFSET, y + 7, 0xFF8F8F8F);
                graphics.drawString(font, label, x + TEXT_X_OFFSET, y + 16, 0xFF6F6F6F);
            }
            //#else
            ResourceLocation background = layout.wide() ? BACKGROUND_LARGE : BACKGROUND_SMALL;
            int iconX = x + (layout.wide() ? 6 : 7);
            //#if >=1.21.6
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, background, x, y, layout.maxWidth(), ROW_HEIGHT, GREYED);
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, Gui.getMobEffectSprite(effect), iconX, y + 7, 18, 18, GREYED);
            //#elif >=1.21.2
            graphics.blitSprite(RenderType::guiTextured, background, x, y, layout.maxWidth(), ROW_HEIGHT, GREYED);
            graphics.blitSprite(RenderType::guiTextured, Minecraft.getInstance().getMobEffectTextures().get(effect), iconX, y + 7, 18, 18, GREYED);
            //#else
            RenderSystem.enableBlend();
            graphics.setColor(1.0F, 1.0F, 1.0F, 0.5F);
            graphics.blitSprite(background, x, y, layout.maxWidth(), ROW_HEIGHT);
            graphics.blit(iconX, y + 7, 0, 18, 18, Minecraft.getInstance().getMobEffectTextures().get(effect));
            graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.disableBlend();
            //#endif
            if (layout.wide()) {
                graphics.drawString(font, name, x + 28, y + 6, 0xFF8F8F8F);
                graphics.drawString(font, label, x + 28, y + 16, 0xFF6F6F6F);
            }
            //#endif
        }

        if (!EnderBrewsClient.canPause()) {
            return;
        }
        int row = layout.rowAt(mouseX, mouseY);
        if (row >= layout.active().size()) {
            Holder<MobEffect> effect = layout.paused().get(row - layout.active().size());
            tooltip(graphics, font, List.of(
                    PotionTooltips.name(effect, snapshot.find(PotionTooltips.effectId(effect)).status().amplifier()),
                    Component.translatable("enderbrews.effect.click_resume")), mouseX, mouseY);
        } else if (row >= 0 && layout.wide() && isPausable(layout.active().get(row), snapshot)) {
            // In the narrow layout vanilla already shows a tooltip on hover; don't draw over it.
            tooltip(graphics, font, List.of(Component.translatable("enderbrews.effect.click_pause")), mouseX, mouseY);
        }
    }

    private static void tooltip(GuiGraphics graphics, Font font, List<Component> lines, int mouseX, int mouseY) {
        //#if >=1.21.6
        // Loader post-render hooks run after the screen has already drawn its deferred tooltip.
        graphics.nextStratum();
        graphics.renderTooltip(font, lines.stream().map(line -> ClientTooltipComponent.create(line.getVisualOrderText())).toList(),
                mouseX, mouseY, DefaultTooltipPositioner.INSTANCE, null);
        //#else
        graphics.renderTooltip(font, lines, Optional.empty(), mouseX, mouseY);
        //#endif
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
