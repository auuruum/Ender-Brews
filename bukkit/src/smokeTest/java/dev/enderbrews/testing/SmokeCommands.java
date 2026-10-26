package dev.enderbrews.testing;

import java.util.Arrays;
import java.util.Objects;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/** Test-only bridge: console-driven checks must dispatch commands with a real Player sender. */
public final class SmokeCommands extends JavaPlugin {
    @Override
    public void onEnable() {
        Objects.requireNonNull(getCommand("ebsmoke")).setExecutor((sender, command, label, args) -> {
            if (!(sender instanceof ConsoleCommandSender) || args.length < 2) return false;
            Player player = getServer().getPlayerExact(args[0]);
            if (player == null) return false;
            return player.performCommand(String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
        });
    }
}
