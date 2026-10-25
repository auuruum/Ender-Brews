package dev.enderbrews.neoforge;

import dev.enderbrews.neoforge.client.ClientPlatform;
import dev.enderbrews.neoforge.client.EnderBrewsClient;
import dev.enderbrews.neoforge.common.EnderBrews;
import dev.enderbrews.neoforge.common.EnderBrewsPayloads;
import java.lang.reflect.Field;
import java.util.List;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Optional client extras on NeoForge. */
@Mod(value = EnderBrews.MOD_ID, dist = Dist.CLIENT)
public final class EnderBrewsNeoForgeClient {

    public EnderBrewsNeoForgeClient(IEventBus modBus) {
        EnderBrewsClient.init(new ClientPlatform() {
            // NeoForge runs with Mojang names, so the private field can be found by name.
            private final Field allMessages = findField(ChatComponent.class, "allMessages");

            @Override
            @SuppressWarnings("unchecked")
            public List<GuiMessage> chatMessages(ChatComponent chat) {
                try {
                    return (List<GuiMessage>) allMessages.get(chat);
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            }

            @Override
            public int guiLeft(AbstractContainerScreen<?> screen) {
                return screen.getGuiLeft();
            }

            @Override
            public int guiTop(AbstractContainerScreen<?> screen) {
                return screen.getGuiTop();
            }

            @Override
            public int imageWidth(AbstractContainerScreen<?> screen) {
                return screen.getXSize();
            }

            @Override
            public boolean canSendPause() {
                return Minecraft.getInstance().getConnection() != null
                        && Minecraft.getInstance().getConnection().hasChannel(EnderBrewsPayloads.Pause.TYPE);
            }

            @Override
            public void sendPause(byte[] data) {
                PacketDistributor.sendToServer(new EnderBrewsPayloads.Pause(data));
            }
        });
        EnderBrewsNeoForge.statusHandler = EnderBrewsClient::statusReceived;

        modBus.addListener((RegisterKeyMappingsEvent event) -> event.register(EnderBrewsClient.PAUSE_ALL_KEY));
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> EnderBrewsClient.clientTick());
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> EnderBrewsClient.disconnected());
        NeoForge.EVENT_BUS.addListener((ClientChatReceivedEvent.System event) -> {
            if (!event.isOverlay()) {
                EnderBrewsClient.systemMessageReceived(event.getMessage());
            }
        });
        NeoForge.EVENT_BUS.addListener((ItemTooltipEvent event) ->
                EnderBrewsClient.appendTooltip(event.getItemStack(), event.getToolTip()));
        NeoForge.EVENT_BUS.addListener((ScreenEvent.Render.Post event) ->
                EnderBrewsClient.renderScreen(event.getScreen(), event.getGuiGraphics(), event.getMouseX(), event.getMouseY()));
        NeoForge.EVENT_BUS.addListener((ScreenEvent.MouseButtonPressed.Pre event) -> {
            if (EnderBrewsClient.mouseClicked(event.getScreen(), event.getMouseX(), event.getMouseY(), event.getButton())) {
                event.setCanceled(true);
            }
        });
    }

    private static Field findField(Class<?> owner, String name) {
        try {
            Field field = owner.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException(e);
        }
    }
}
