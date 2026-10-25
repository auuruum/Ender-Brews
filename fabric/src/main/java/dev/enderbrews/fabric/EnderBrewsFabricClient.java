package dev.enderbrews.fabric;

import dev.enderbrews.fabric.client.ClientPlatform;
import dev.enderbrews.fabric.client.EnderBrewsClient;
import dev.enderbrews.fabric.common.EnderBrewsPayloads;
import java.util.List;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

/** Optional client extras on Fabric and Quilt. The access widener opens the fields used here. */
public final class EnderBrewsFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        EnderBrewsClient.init(new ClientPlatform() {
            @Override
            public List<GuiMessage> chatMessages(ChatComponent chat) {
                return chat.allMessages;
            }

            @Override
            public int guiLeft(AbstractContainerScreen<?> screen) {
                return screen.leftPos;
            }

            @Override
            public int guiTop(AbstractContainerScreen<?> screen) {
                return screen.topPos;
            }

            @Override
            public int imageWidth(AbstractContainerScreen<?> screen) {
                return screen.imageWidth;
            }

            @Override
            public boolean canSendPause() {
                return ClientPlayNetworking.canSend(EnderBrewsPayloads.Pause.TYPE);
            }

            @Override
            public void sendPause(byte[] data) {
                ClientPlayNetworking.send(new EnderBrewsPayloads.Pause(data));
            }
        });

        ClientPlayNetworking.registerGlobalReceiver(EnderBrewsPayloads.Status.TYPE, (payload, context) ->
                EnderBrewsClient.statusReceived(payload.data()));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> EnderBrewsClient.disconnected());
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            if (!overlay) {
                EnderBrewsClient.systemMessageReceived(message);
            }
            return true;
        });
        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> EnderBrewsClient.appendTooltip(stack, lines));
        KeyBindingHelper.registerKeyBinding(EnderBrewsClient.PAUSE_ALL_KEY);
        ClientTickEvents.END_CLIENT_TICK.register(client -> EnderBrewsClient.clientTick());
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            ScreenEvents.afterRender(screen).register((s, graphics, mouseX, mouseY, delta) ->
                    EnderBrewsClient.renderScreen(s, graphics, mouseX, mouseY));
            ScreenMouseEvents.allowMouseClick(screen).register((s, mouseX, mouseY, button) ->
                    !EnderBrewsClient.mouseClicked(s, mouseX, mouseY, button));
        });
    }
}
