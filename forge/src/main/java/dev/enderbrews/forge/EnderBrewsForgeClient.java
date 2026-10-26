package dev.enderbrews.forge;

import dev.enderbrews.forge.client.ClientPlatform;
import dev.enderbrews.forge.client.EnderBrewsClient;
import java.lang.reflect.Field;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.event.SystemMessageReceivedEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.config.ConfigTracker;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.network.ConnectionType;
import net.minecraftforge.network.NetworkContext;
import net.minecraftforge.network.PacketDistributor;

/** Optional client extras on Forge. Only loaded on the physical client. */
final class EnderBrewsForgeClient {

    private EnderBrewsForgeClient() {
    }

    static void init() {
        EnderBrewsClient.init(new ClientPlatform() {
            // Minecraft is not obfuscated since 26.1, so private fields can be found by name.
            private final Field allMessages = findField(ChatComponent.class, "allMessages");
            private final Field leftPos = findField(AbstractContainerScreen.class, "leftPos");
            private final Field topPos = findField(AbstractContainerScreen.class, "topPos");
            private final Field imageWidth = findField(AbstractContainerScreen.class, "imageWidth");

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
                return readInt(leftPos, screen);
            }

            @Override
            public int guiTop(AbstractContainerScreen<?> screen) {
                return readInt(topPos, screen);
            }

            @Override
            public int imageWidth(AbstractContainerScreen<?> screen) {
                return readInt(imageWidth, screen);
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

        RegisterKeyMappingsEvent.BUS.addListener(event -> event.register(EnderBrewsClient.PAUSE_ALL_KEY));
        TickEvent.ClientTickEvent.Post.BUS.addListener(event -> EnderBrewsClient.clientTick());
        ClientPlayerNetworkEvent.LoggingIn.BUS.addListener(event -> {
            // Forge 66.1.0 places this vanilla-server setup in the optional code-of-conduct handler.
            if (NetworkContext.get(event.getConnection()).getType() == ConnectionType.VANILLA
                    && ConfigTracker.configSets().get(ModConfig.Type.SERVER).stream()
                    .anyMatch(config -> config.getModId().equals("forge") && config.getConfigData() == null)) {
                ConfigTracker.loadDefaultServerConfigs();
            }
        });
        ClientPlayerNetworkEvent.LoggingOut.BUS.addListener(event -> EnderBrewsClient.disconnected());
        SystemMessageReceivedEvent.BUS.addListener((Predicate<SystemMessageReceivedEvent>) event -> {
            // Forge 26.3 passes the remote-message flag through the old isOverlay accessor.
            EnderBrewsClient.systemMessageReceived(event.getMessage());
            return false;
        });
        ItemTooltipEvent.BUS.addListener(event -> EnderBrewsClient.appendTooltip(event.getItemStack(), event.getToolTip()));
        ScreenEvent.Render.Post.BUS.addListener(event ->
                EnderBrewsClient.renderScreen(event.getScreen(), event.getGuiGraphics(), event.getMouseX(), event.getMouseY()));
        // Returning true cancels the click so the screen does not handle it too.
        ScreenEvent.MouseButtonPressed.Pre.BUS.addListener((Predicate<ScreenEvent.MouseButtonPressed.Pre>) event ->
                EnderBrewsClient.mouseClicked(event.getScreen(), event.getMouseX(), event.getMouseY(), event.getInfo().buttonInfo().button()));
    }

    private static int readInt(Field field, Object owner) {
        try {
            return field.getInt(owner);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
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
