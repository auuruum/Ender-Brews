package dev.enderbrews.forge;

import dev.enderbrews.forge.client.ClientPlatform;
import dev.enderbrews.forge.client.EnderBrewsClient;
import java.lang.reflect.Field;
import java.util.List;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.event.SystemMessageReceivedEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.network.PacketDistributor;

/** Optional client extras on Forge. Only loaded on the physical client. */
final class EnderBrewsForgeClient {

    private EnderBrewsForgeClient() {
    }

    static void init(IEventBus modBus) {
        EnderBrewsClient.init(new ClientPlatform() {
            // Forge 1.20.6+ runs with Mojang names, so the private field can be found by name.
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
                        && EnderBrewsForge.PAUSE.isRemotePresent(Minecraft.getInstance().getConnection().getConnection());
            }

            @Override
            public void sendPause(byte[] data) {
                EnderBrewsForge.PAUSE.send(EnderBrewsForge.buffer(data), PacketDistributor.SERVER.noArg());
            }
        });
        EnderBrewsForge.statusHandler = EnderBrewsClient::statusReceived;

        modBus.addListener((RegisterKeyMappingsEvent event) -> event.register(EnderBrewsClient.PAUSE_ALL_KEY));
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END) {
                EnderBrewsClient.clientTick();
            }
        });
        MinecraftForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> EnderBrewsClient.disconnected());
        MinecraftForge.EVENT_BUS.addListener((SystemMessageReceivedEvent event) -> {
            if (!event.isOverlay()) {
                EnderBrewsClient.systemMessageReceived(event.getMessage());
            }
        });
        MinecraftForge.EVENT_BUS.addListener((ItemTooltipEvent event) ->
                EnderBrewsClient.appendTooltip(event.getItemStack(), event.getToolTip()));
        MinecraftForge.EVENT_BUS.addListener((ScreenEvent.Render.Post event) ->
                EnderBrewsClient.renderScreen(event.getScreen(), event.getGuiGraphics(), event.getMouseX(), event.getMouseY()));
        MinecraftForge.EVENT_BUS.addListener((ScreenEvent.MouseButtonPressed.Pre event) -> {
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
