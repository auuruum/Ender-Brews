package dev.enderbrews.shared.client;

import dev.enderbrews.shared.core.Protocol;
import java.util.List;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;

/**
 * Keeps only the newest /enderbrews list in chat. The server marks the list with a hidden
 * insertion; when a new one arrives, older copies are removed so the list seems to update in place.
 */
final class ChatListReplacer {

    private ChatListReplacer() {
    }

    static void beforeShown(ChatComponent chat, Component message) {
        if (!hasMarker(message)) {
            return;
        }
        List<GuiMessage> messages = EnderBrewsClient.platform().chatMessages(chat);
        if (messages.removeIf(old -> hasMarker(old.content()))) {
            chat.rescaleChat();
        }
    }

    private static boolean hasMarker(Component component) {
        if (Protocol.LIST_MARKER.equals(component.getStyle().getInsertion())) {
            return true;
        }
        for (Component sibling : component.getSiblings()) {
            if (hasMarker(sibling)) {
                return true;
            }
        }
        return false;
    }
}
