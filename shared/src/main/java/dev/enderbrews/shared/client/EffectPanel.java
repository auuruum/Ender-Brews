package dev.enderbrews.shared.client;

import com.google.common.collect.Ordering;
import com.mojang.blaze3d.platform.InputConstants;
import dev.enderbrews.shared.core.EffectStatus;
import dev.enderbrews.shared.core.Protocol;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

/**
 * Makes the effect list next to the inventory clickable: click an endless effect to pause it.
 * Paused effects are no longer on the player, so they are drawn greyed out under the vanilla list,
 * where a click resumes them. The layout mirrors vanilla's EffectsInInventory.
 */
final class EffectPanel {

    private static final Identifier BACKGROUND = Identifier.withDefaultNamespace("container/inventory/effect_background");
    private static final int ROW_HEIGHT = 32;
    private static final int TEXT_X_OFFSET = 32;
    private static final int SPACING = 7;
    private static final int GREYED = 0x80FFFFFF;
    private static final MethodHandle TOOLTIP = tooltipHandle();
    private static final int LEFT_MOUSE_BUTTON = leftMouseButton();

    private EffectPanel() {
    }

    private record Layout(int x, int top, int step, int maxWidth, List<MobEffectInstance> active,
                          List<Holder<MobEffect>> paused) {

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
                || !(screen instanceof AbstractContainerScreen<?> inventory) || !screen.showsActiveEffects()) {
            return null;
        }
        ClientPlatform platform = EnderBrewsClient.platform();
        int x = platform.guiLeft(inventory) + platform.imageWidth(inventory) + 2;
        int availableWidth = screen.width - x;
        if (availableWidth < ROW_HEIGHT) {
            return null;
        }
        List<MobEffectInstance> active = Ordering.natural().sortedCopy(minecraft.player.getActiveEffects());
        int step = active.size() > 5 ? 132 / (active.size() - 1) : 33;
        List<Holder<MobEffect>> paused = new ArrayList<>();
        for (Protocol.Entry entry : snapshot.entries()) {
            if (entry.paused() && entry.status().state() == EffectStatus.State.ACTIVE) {
                BuiltInRegistries.MOB_EFFECT.get(Identifier.parse(entry.status().effectId())).ifPresent(paused::add);
            }
        }
        int maxWidth = availableWidth >= 120 ? availableWidth - SPACING : ROW_HEIGHT;
        return new Layout(x, platform.guiTop(inventory), step, maxWidth, active, paused);
    }

    static void render(Screen screen, GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        Layout layout = layout(screen);
        if (layout == null) {
            return;
        }
        Font font = Minecraft.getInstance().font;
        Protocol.Snapshot snapshot = EnderBrewsClient.snapshot();

        for (int i = 0; i < layout.paused().size(); i++) {
            Holder<MobEffect> effect = layout.paused().get(i);
            int y = layout.top() + (layout.active().size() + i) * layout.step();
            Component name = PotionTooltips.name(effect, snapshot.find(PotionTooltips.effectId(effect)).status().amplifier());
            Component label = Component.translatable("enderbrews.effect.paused");
            int width = Math.min(layout.maxWidth(), TEXT_X_OFFSET + Math.max(font.width(name), font.width(label)) + SPACING);
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND, layout.x(), y, width, ROW_HEIGHT, GREYED);
            // Same sprite as vanilla's getMobEffectSprite, which moved from Gui to Hud in 26.2.
            Identifier sprite = effect.unwrapKey().map(key -> key.identifier().withPrefix("mob_effect/"))
                    .orElseGet(MissingTextureAtlasSprite::getLocation);
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, layout.x() + 7, y + 7, 18, 18, GREYED);
            if (width > TEXT_X_OFFSET + SPACING) {
                graphics.text(font, name, layout.x() + TEXT_X_OFFSET, y + 7, 0xFF8F8F8F);
                graphics.text(font, label, layout.x() + TEXT_X_OFFSET, y + 16, 0xFF6F6F6F);
            }
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
        } else if (row >= 0 && layout.maxWidth() > ROW_HEIGHT && isPausable(layout.active().get(row), snapshot)) {
            // In the narrow layout vanilla already shows a tooltip on hover; don't draw over it.
            tooltip(graphics, font, List.of(Component.translatable("enderbrews.effect.click_pause")), mouseX, mouseY);
        }
    }

    private static void tooltip(GuiGraphicsExtractor graphics, Font font, List<Component> lines, int mouseX, int mouseY) {
        // Loader post-render hooks run after Screen has already flushed deferred tooltips.
        graphics.nextStratum();
        try {
            TOOLTIP.invoke(graphics, font, lines.stream().map(line -> ClientTooltipComponent.create(line.getVisualOrderText())).toList(),
                    mouseX, mouseY, (ClientTooltipPositioner) DefaultTooltipPositioner.INSTANCE, (Identifier) null);
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }

    /** 26.3 added a trailing extraSpaceAfterFirstLine flag to tooltip(); 26.1 and 26.2 lack it. */
    private static MethodHandle tooltipHandle() {
        MethodType type = MethodType.methodType(void.class, Font.class, List.class, int.class, int.class,
                ClientTooltipPositioner.class, Identifier.class);
        MethodHandles.Lookup lookup = MethodHandles.publicLookup();
        try {
            try {
                return MethodHandles.insertArguments(lookup.findVirtual(GuiGraphicsExtractor.class, "tooltip",
                        type.appendParameterTypes(boolean.class)), 7, false);
            } catch (NoSuchMethodException e) {
                return lookup.findVirtual(GuiGraphicsExtractor.class, "tooltip", type);
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Read at runtime: javac would inline the constant, and it is 0 before 26.3 but 1 since. */
    private static int leftMouseButton() {
        try {
            return InputConstants.class.getField("MOUSE_BUTTON_LEFT").getInt(null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    static boolean mouseClicked(Screen screen, double mouseX, double mouseY, int button) {
        Layout layout = layout(screen);
        if (button != LEFT_MOUSE_BUTTON || layout == null || !EnderBrewsClient.canPause()) {
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
