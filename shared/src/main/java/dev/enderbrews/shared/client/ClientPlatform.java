package dev.enderbrews.shared.client;

import java.util.List;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

/** The few things the client features need that each loader exposes differently. */
public interface ClientPlatform {

    /** The chat's message history, modifiable. */
    List<GuiMessage> chatMessages(ChatComponent chat);

    int guiLeft(AbstractContainerScreen<?> screen);

    int guiTop(AbstractContainerScreen<?> screen);

    int imageWidth(AbstractContainerScreen<?> screen);

    /** Whether the server listens for pause packets (it runs Ender Brews). */
    boolean canSendPause();

    void sendPause(byte[] data);
}
