package dev.enderbrews.shared.common;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.enderbrews.shared.core.EffectStatus;
import dev.enderbrews.shared.core.EnderBrewsConfig;
import dev.enderbrews.shared.core.Lang;
import dev.enderbrews.shared.core.Protocol;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;

/**
 * {@code /enderbrews} lists the player's endless effects with clickable pause buttons, so it works
 * for players without the mod on their client. Subcommands: toggle/pause/resume and reload.
 */
final class EnderBrewsCommand {

    private static final SuggestionProvider<CommandSourceStack> COUNTED_EFFECTS = (ctx, builder) -> {
        EnderBrewsServer server = EnderBrews.server();
        ServerPlayer player = ctx.getSource().getPlayer();
        if (server != null && player != null) {
            return SharedSuggestionProvider.suggest(server.statuses(player).stream()
                    .filter(s -> s.state() == EffectStatus.State.ACTIVE)
                    .map(EffectStatus::effectId), builder);
        }
        return builder.buildFuture();
    };

    private EnderBrewsCommand() {
    }

    static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("enderbrews")
                .executes(EnderBrewsCommand::list)
                .then(Commands.literal("toggle").then(effectArgument(null)))
                .then(Commands.literal("pause").then(effectArgument(true)))
                .then(Commands.literal("resume").then(effectArgument(false)))
                .then(Commands.literal("reload")
                        .requires(source -> source.hasPermission(2))
                        .executes(EnderBrewsCommand::reload)));
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, ?> effectArgument(Boolean pause) {
        return Commands.argument("effect", ResourceLocationArgument.id())
                .suggests(COUNTED_EFFECTS)
                .executes(ctx -> setPaused(ctx, pause));
    }

    private static int list(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        EnderBrewsServer server = EnderBrews.server();
        String locale = locale(player);
        if (server == null) {
            player.sendSystemMessage(text(locale, "not.ready").withStyle(ChatFormatting.RED));
            return 0;
        }
        sendList(player, server, locale);
        return 1;
    }

    /**
     * Sends the whole list as one chat message. Vanilla clients cannot edit a message that was
     * already sent, so pause/resume send the list again: the newest copy at the bottom of the chat
     * always shows the current state. A client that has the mod spots the hidden marker and removes
     * the older copy, so for those players the list updates in place.
     */
    private static void sendList(ServerPlayer player, EnderBrewsServer server, String locale) {
        EnderBrewsConfig config = server.config();
        List<EffectStatus> statuses = server.statuses(player);
        MutableComponent message = Component.empty()
                .append(Component.literal("").withStyle(Style.EMPTY.withInsertion(Protocol.LIST_MARKER)))
                .append(format(locale, "list.header", Component.literal(String.valueOf(config.threshold())))
                        .withStyle(ChatFormatting.LIGHT_PURPLE));
        if (statuses.isEmpty()) {
            message.append("\n").append(format(locale, "list.empty", Component.literal(String.valueOf(config.threshold())))
                    .withStyle(ChatFormatting.GRAY));
        }
        for (EffectStatus status : statuses) {
            message.append("\n").append(line(player, server, status, locale));
        }
        player.sendSystemMessage(message);
    }

    private static Component line(ServerPlayer player, EnderBrewsServer server, EffectStatus status, String locale) {
        MutableComponent name = effectName(status.effectId(), status.amplifier());
        MutableComponent line = Component.empty();
        switch (status.state()) {
            case ACTIVE -> {
                boolean paused = server.isPaused(player, status.effectId());
                line.append(Component.literal(paused ? "⏸ " : "∞ ").withStyle(paused ? ChatFormatting.GRAY : ChatFormatting.GOLD))
                        .append(name.withStyle(paused ? ChatFormatting.GRAY : ChatFormatting.WHITE))
                        .append(Component.literal(" — ").withStyle(ChatFormatting.DARK_GRAY))
                        .append(text(locale, paused ? "list.paused" : "list.endless").withStyle(ChatFormatting.GRAY));
                if (server.config().allowPause()) {
                    String action = paused ? "resume" : "pause";
                    line.append(" ").append(text(locale, "button." + action)
                            .withStyle(Style.EMPTY
                                    .withColor(paused ? ChatFormatting.GREEN : ChatFormatting.YELLOW)
                                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                            "/enderbrews " + action + " " + status.effectId()))
                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                            text(locale, "hover." + action)))));
                }
            }
            case PROGRESS -> line.append(Component.literal("· ").withStyle(ChatFormatting.DARK_GRAY))
                    .append(name.withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(" " + status.count() + "/" + status.threshold()).withStyle(ChatFormatting.WHITE));
            case LIMITED -> line.append(Component.literal("… ").withStyle(ChatFormatting.YELLOW))
                    .append(name.withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(" — ").withStyle(ChatFormatting.DARK_GRAY))
                    .append(text(locale, "list.limited").withStyle(ChatFormatting.YELLOW));
            case BLOCKED -> line.append(Component.literal("✕ ").withStyle(ChatFormatting.RED))
                    .append(name.withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(" — ").withStyle(ChatFormatting.DARK_GRAY))
                    .append(text(locale, "list.blocked").withStyle(ChatFormatting.RED));
        }
        return line;
    }

    /** {@code pause}: true pauses, false resumes, null toggles. */
    private static int setPaused(CommandContext<CommandSourceStack> ctx, Boolean pause) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        EnderBrewsServer server = EnderBrews.server();
        String locale = locale(player);
        if (server == null) {
            player.sendSystemMessage(text(locale, "not.ready").withStyle(ChatFormatting.RED));
            return 0;
        }
        if (!server.config().allowPause()) {
            player.sendSystemMessage(text(locale, "pause.disabled").withStyle(ChatFormatting.RED));
            return 0;
        }
        String effectId = ResourceLocationArgument.getId(ctx, "effect").toString();
        Optional<MobEffect> effect = VanillaEffects.effect(effectId);
        if (effect.isEmpty()) {
            player.sendSystemMessage(format(locale, "effect.unknown", Component.literal(effectId)).withStyle(ChatFormatting.RED));
            return 0;
        }
        boolean paused = pause != null ? pause : !server.isPaused(player, effectId);
        server.setPaused(player, effectId, paused);
        sendList(player, server, locale);
        return 1;
    }

    private static int reload(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String locale = source.getPlayer() != null ? locale(source.getPlayer()) : "en_us";
        EnderBrewsServer server = EnderBrews.server();
        if (server == null) {
            source.sendFailure(text(locale, "not.ready"));
            return 0;
        }
        try {
            server.reload();
            source.sendSuccess(() -> text(locale, "reload.done"), true);
            return 1;
        } catch (IOException | IllegalArgumentException e) {
            source.sendFailure(format(locale, "reload.failed", Component.literal(String.valueOf(e.getMessage()))));
            return 0;
        }
    }

    /** Effect names use vanilla translation keys, so every client shows them in its own language. */
    private static MutableComponent effectName(String effectId, int amplifier) {
        MutableComponent name = VanillaEffects.effect(effectId)
                .map(effect -> Component.translatable(effect.getDescriptionId()))
                .orElseGet(() -> Component.literal(effectId));
        if (amplifier >= 1 && amplifier <= 5) {
            name.append(" ").append(Component.translatable("potion.potency." + amplifier));
        } else if (amplifier > 5) {
            name.append(" " + (amplifier + 1));
        }
        return name;
    }

    private static String locale(ServerPlayer player) {
        return EnderBrews.language(player);
    }

    private static MutableComponent text(String locale, String key) {
        return Component.literal(Lang.get(locale, key));
    }

    private static MutableComponent format(String locale, String key, Component argument) {
        String[] parts = Lang.parts(locale, key);
        MutableComponent result = Component.literal(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            result.append(argument).append(parts[i]);
        }
        return result;
    }
}
