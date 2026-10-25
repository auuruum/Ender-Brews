package dev.enderbrews.bukkit;

import dev.enderbrews.bukkit.core.EffectStatus;
import dev.enderbrews.bukkit.core.EnderBrewsConfig;
import dev.enderbrews.bukkit.core.Lang;
import dev.enderbrews.bukkit.core.Protocol;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.TranslatableComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

/** {@code /enderbrews}: the Bukkit twin of the mod's command, with the same clickable list. */
final class BukkitCommand implements TabExecutor {

    private final EnderBrewsPlugin plugin;

    BukkitCommand(EnderBrewsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String locale = sender instanceof Player player ? player.getLocale() : "en_us";
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("enderbrews.reload")) {
                sender.sendMessage(ChatColor.RED + command.getPermissionMessage());
                return true;
            }
            try {
                plugin.reloadEnderBrewsConfig();
                sender.sendMessage(ChatColor.GREEN + Lang.get(locale, "reload.done"));
            } catch (IOException | IllegalArgumentException e) {
                sender.sendMessage(ChatColor.RED + Lang.get(locale, "reload.failed").replace("%s", String.valueOf(e.getMessage())));
            }
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Lang.get(locale, "players.only"));
            return true;
        }
        if (args.length == 0) {
            list(player, locale);
            return true;
        }
        if (args.length == 2) {
            String action = args[0].toLowerCase(Locale.ROOT);
            if (!List.of("pause", "resume", "toggle").contains(action)) {
                return false;
            }
            Boolean pause = action.equals("toggle") ? null : action.equals("pause");
            setPaused(player, locale, EnderBrewsConfig.normalizeId(args[1]), pause);
            return true;
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = sender.hasPermission("enderbrews.reload")
                    ? List.of("toggle", "pause", "resume", "reload")
                    : List.of("toggle", "pause", "resume");
            return options.stream().filter(o -> o.startsWith(args[0].toLowerCase(Locale.ROOT))).toList();
        }
        if (args.length == 2 && sender instanceof Player player && !args[0].equalsIgnoreCase("reload")) {
            return plugin.statuses(player).stream()
                    .filter(s -> s.state() == EffectStatus.State.ACTIVE)
                    .map(EffectStatus::effectId)
                    .filter(id -> id.startsWith(args[1].toLowerCase(Locale.ROOT)))
                    .toList();
        }
        return List.of();
    }

    /**
     * One chat message; pause/resume send it again so the newest copy shows the current state.
     * Clients with the mod spot the hidden marker and replace the older copy in place.
     */
    private void list(Player player, String locale) {
        EnderBrewsConfig config = plugin.enderBrewsConfig();
        List<EffectStatus> statuses = plugin.statuses(player);
        TextComponent message = new TextComponent();
        TextComponent marker = new TextComponent("");
        marker.setInsertion(Protocol.LIST_MARKER);
        message.addExtra(marker);
        message.addExtra(format(locale, "list.header", plain(String.valueOf(config.threshold())), ChatColor.LIGHT_PURPLE));
        if (statuses.isEmpty()) {
            message.addExtra("\n");
            message.addExtra(format(locale, "list.empty", plain(String.valueOf(config.threshold())), ChatColor.GRAY));
        }
        for (EffectStatus status : statuses) {
            message.addExtra("\n");
            message.addExtra(line(player, config, status, locale));
        }
        player.spigot().sendMessage(message);
    }

    private BaseComponent line(Player player, EnderBrewsConfig config, EffectStatus status, String locale) {
        TextComponent line = new TextComponent();
        BaseComponent name = effectName(status.effectId(), status.amplifier());
        switch (status.state()) {
            case ACTIVE -> {
                boolean paused = plugin.isPaused(player, status.effectId());
                line.addExtra(colored(paused ? "⏸ " : "∞ ", paused ? ChatColor.GRAY : ChatColor.GOLD));
                name.setColor(paused ? ChatColor.GRAY : ChatColor.WHITE);
                line.addExtra(name);
                line.addExtra(colored(" — ", ChatColor.DARK_GRAY));
                line.addExtra(colored(Lang.get(locale, paused ? "list.paused" : "list.endless"), ChatColor.GRAY));
                if (config.allowPause()) {
                    String action = paused ? "resume" : "pause";
                    TextComponent button = colored(" " + Lang.get(locale, "button." + action), paused ? ChatColor.GREEN : ChatColor.YELLOW);
                    button.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/enderbrews " + action + " " + status.effectId()));
                    button.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(Lang.get(locale, "hover." + action))));
                    line.addExtra(button);
                }
            }
            case PROGRESS -> {
                line.addExtra(colored("· ", ChatColor.DARK_GRAY));
                name.setColor(ChatColor.GRAY);
                line.addExtra(name);
                line.addExtra(colored(" " + status.count() + "/" + status.threshold(), ChatColor.WHITE));
            }
            case LIMITED -> {
                line.addExtra(colored("… ", ChatColor.YELLOW));
                name.setColor(ChatColor.GRAY);
                line.addExtra(name);
                line.addExtra(colored(" — ", ChatColor.DARK_GRAY));
                line.addExtra(colored(Lang.get(locale, "list.limited"), ChatColor.YELLOW));
            }
            case BLOCKED -> {
                line.addExtra(colored("✕ ", ChatColor.RED));
                name.setColor(ChatColor.GRAY);
                line.addExtra(name);
                line.addExtra(colored(" — ", ChatColor.DARK_GRAY));
                line.addExtra(colored(Lang.get(locale, "list.blocked"), ChatColor.RED));
            }
        }
        return line;
    }

    private void setPaused(Player player, String locale, String effectId, Boolean pause) {
        if (!plugin.enderBrewsConfig().allowPause()) {
            player.sendMessage(ChatColor.RED + Lang.get(locale, "pause.disabled"));
            return;
        }
        if (BukkitEffects.type(effectId) == null) {
            player.spigot().sendMessage(format(locale, "effect.unknown", plain(effectId), ChatColor.RED));
            return;
        }
        boolean paused = pause != null ? pause : !plugin.isPaused(player, effectId);
        plugin.setPaused(player, effectId, paused);
        list(player, locale);
    }

    private static BaseComponent effectName(String effectId, int amplifier) {
        TranslatableComponent name = new TranslatableComponent(BukkitEffects.translationKey(effectId));
        if (amplifier >= 1 && amplifier <= 5) {
            name.addExtra(" ");
            name.addExtra(new TranslatableComponent("potion.potency." + amplifier));
        } else if (amplifier > 5) {
            name.addExtra(" " + (amplifier + 1));
        }
        return name;
    }

    private static TextComponent plain(String text) {
        return new TextComponent(text);
    }

    private static TextComponent colored(String text, ChatColor color) {
        TextComponent component = new TextComponent(text);
        component.setColor(color);
        return component;
    }

    private static BaseComponent format(String locale, String key, BaseComponent argument, ChatColor color) {
        String[] parts = Lang.parts(locale, key);
        TextComponent result = new TextComponent(parts[0]);
        result.setColor(color);
        for (int i = 1; i < parts.length; i++) {
            result.addExtra(argument.duplicate());
            result.addExtra(parts[i]);
        }
        return result;
    }
}
