package dev.enderbrews.shared.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.enderbrews.shared.core.EffectStatus;
import dev.enderbrews.shared.core.Protocol;
import java.util.List;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * Optional client side. The mod works fully on the server alone; with it on the client too, players
 * get a few extras once the server (mod or plugin) starts sending status packets:
 * <ul>
 *     <li>the /enderbrews chat list updates in place instead of piling up;</li>
 *     <li>clicking an endless effect in the inventory pauses it, paused ones show greyed out below;</li>
 *     <li>potion tooltips show how many of them are in the Ender Chest;</li>
 *     <li>a key (unbound by default) pauses or resumes all endless effects.</li>
 * </ul>
 * Loaders forward their events to the static methods here.
 */
public final class EnderBrewsClient {

    public static final KeyMapping.Category KEY_CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(Protocol.NAMESPACE, "main"));
    public static final KeyMapping PAUSE_ALL_KEY = new KeyMapping("key.enderbrews.pause_all",
            InputConstants.UNKNOWN.getValue(), KEY_CATEGORY);

    private static ClientPlatform platform;
    private static Protocol.Snapshot snapshot;

    private EnderBrewsClient() {
    }

    public static void init(ClientPlatform clientPlatform) {
        platform = clientPlatform;
    }

    static ClientPlatform platform() {
        return platform;
    }

    /** The latest status from the server, or null if the server does not run Ender Brews. */
    static Protocol.Snapshot snapshot() {
        return snapshot;
    }

    public static void statusReceived(byte[] data) {
        Protocol.Snapshot decoded = Protocol.decodeSnapshot(data);
        if (decoded != null) {
            snapshot = decoded;
        }
    }

    public static void disconnected() {
        snapshot = null;
    }

    static boolean canPause() {
        return snapshot != null && snapshot.allowPause() && platform.canSendPause();
    }

    static void requestPause(String effectId, boolean paused) {
        if (canPause()) {
            platform.sendPause(Protocol.encodePause(new Protocol.PauseRequest(effectId, paused)));
        }
    }

    public static void clientTick() {
        while (PAUSE_ALL_KEY.consumeClick()) {
            if (snapshot == null) {
                continue;
            }
            // Pause everything if anything is still running, otherwise resume everything.
            boolean anyRunning = snapshot.entries().stream()
                    .anyMatch(e -> e.status().state() == EffectStatus.State.ACTIVE && !e.paused());
            requestPause(null, anyRunning);
        }
    }

    /** Called before a system chat message is shown. */
    public static void systemMessageReceived(Component message) {
        ChatListReplacer.beforeShown(Minecraft.getInstance().gui.hud.getChat(), message);
    }

    public static void appendTooltip(ItemStack stack, List<Component> lines) {
        PotionTooltips.append(stack, lines);
    }

    public static void renderScreen(Screen screen, GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        EffectPanel.render(screen, graphics, mouseX, mouseY);
    }

    /** Returns true if the click was used and the screen should not handle it. */
    public static boolean mouseClicked(Screen screen, double mouseX, double mouseY, int button) {
        return EffectPanel.mouseClicked(screen, mouseX, mouseY, button);
    }
}
