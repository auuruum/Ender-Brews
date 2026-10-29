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
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.network.PacketDistributor;
//#if >=1.21.6
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraftforge.eventbus.api.bus.BusGroup;
//#if >=1.21.9
import net.minecraftforge.fml.config.ConfigTracker;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.network.ConnectionType;
import net.minecraftforge.network.NetworkContext;
//#endif
//#else
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
//#endif

/** Optional client extras on Forge. Only loaded on the physical client. */
final class EnderBrewsForgeClient {

    private EnderBrewsForgeClient() {
    }

    private static void initPlatform() {
        EnderBrewsClient.init(new ClientPlatform() {
            // Forge 1.20.6+ runs with Mojang names, so private fields can be found by name.
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
    }

    //#if >=1.21.6
    static void init(BusGroup modBusGroup) {
        initPlatform();
        RegisterKeyMappingsEvent.getBus(modBusGroup).addListener(event -> event.register(EnderBrewsClient.PAUSE_ALL_KEY));
        TickEvent.ClientTickEvent.Post.BUS.addListener(event -> EnderBrewsClient.clientTick());
        //#if >=1.21.9
        ClientPlayerNetworkEvent.LoggingIn.BUS.addListener(event -> {
            // Newer Forge loads its default server configs for vanilla servers in the optional
            // code-of-conduct handler; without them a Forge client on Paper misbehaves.
            if (NetworkContext.get(event.getConnection()).getType() == ConnectionType.VANILLA
                    && ConfigTracker.INSTANCE.configSets().get(ModConfig.Type.SERVER).stream()
                    .anyMatch(config -> config.getModId().equals("forge") && config.getConfigData() == null)) {
                ConfigTracker.INSTANCE.loadDefaultServerConfigs();
            }
        });
        //#endif
        ClientPlayerNetworkEvent.LoggingOut.BUS.addListener(event -> EnderBrewsClient.disconnected());
        SystemMessageReceivedEvent.BUS.addListener((Consumer<SystemMessageReceivedEvent>) event -> {
            if (!event.isOverlay()) {
                EnderBrewsClient.systemMessageReceived(event.getMessage());
            }
        });
        ItemTooltipEvent.BUS.addListener(event -> EnderBrewsClient.appendTooltip(event.getItemStack(), event.getToolTip()));
        ScreenEvent.Render.Post.BUS.addListener(event ->
                EnderBrewsClient.renderScreen(event.getScreen(), event.getGuiGraphics(), event.getMouseX(), event.getMouseY()));
        // Returning true cancels the click so the screen does not handle it too.
        ScreenEvent.MouseButtonPressed.Pre.BUS.addListener((Predicate<ScreenEvent.MouseButtonPressed.Pre>) event ->
                //#if >=1.21.9
                EnderBrewsClient.mouseClicked(event.getScreen(), event.getMouseX(), event.getMouseY(), event.getInfo().button()));
                //#else
                EnderBrewsClient.mouseClicked(event.getScreen(), event.getMouseX(), event.getMouseY(), event.getButton()));
                //#endif
    }
    //#else
    static void init(IEventBus modBus) {
        initPlatform();
        modBus.addListener((RegisterKeyMappingsEvent event) -> event.register(EnderBrewsClient.PAUSE_ALL_KEY));
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END) {
                EnderBrewsClient.clientTick();
            }
        });
        MinecraftForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> EnderBrewsClient.disconnected());
        // Forge 51 (Minecraft 1.21) has no SystemMessageReceivedEvent; there the chat list just isn't replaced.
        try {
            Class.forName("net.minecraftforge.client.event.SystemMessageReceivedEvent");
            SystemMessages.register();
        } catch (ClassNotFoundException e) {
            // Older Forge: skip.
        }
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

    /** Its own class, so Forge versions without the event never load it. */
    private static final class SystemMessages {
        static void register() {
            MinecraftForge.EVENT_BUS.addListener((SystemMessageReceivedEvent event) -> {
                if (!event.isOverlay()) {
                    EnderBrewsClient.systemMessageReceived(event.getMessage());
                }
            });
        }
    }
    //#endif

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
