package com.liphium.elfhunt.command;

import com.liphium.elfhunt.Elfhunt;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TimerCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender cs, Command cmd, String label, String[] args) {

        if (cs instanceof Player player && player.hasPermission("timer")) {

            if (args.length == 0) {

                sendHelp(player);
            } else if (args[0].equalsIgnoreCase("pause") || args[0].equalsIgnoreCase("resume")) {

                boolean paused = Elfhunt.getInstance().getGameManager().getCurrentState().paused;
                Elfhunt.getInstance().getGameManager().getCurrentState().paused = !paused;

                if (paused) {
                    player.sendMessage(Elfhunt.PREFIX.append(Component.text("§7Der §cTimer §7wurde §cfortgesetzt§7!")));
                } else {
                    player.sendMessage(Elfhunt.PREFIX.append(Component.text("§7Der §cTimer §7wurde §cpausiert§7!")));
                }
            } else if (args[0].equalsIgnoreCase("skip")) {

                if (Elfhunt.getInstance().getGameManager().getCurrentState().count <= 5) {
                    player.sendMessage(Elfhunt.PREFIX.append(Component.text("§7Der §cTimer §7wurde bereits §cverschnellert§7!")));
                    return false;
                }

                player.sendMessage(Elfhunt.PREFIX.append(Component.text("§7Du hast den §cTimer §7verschnellert§7!")));

                Elfhunt.getInstance().getGameManager().getCurrentState().count = 10;
            } else {
                sendHelp(player);
            }

        }

        return false;
    }

    public void sendHelp(Player player) {
        player.sendMessage(Component.text(" "));
        player.sendMessage(Component.text("    §cCounter§8: §c§l" + Elfhunt.getInstance().getGameManager().getCurrentState().count));

        Component status = Elfhunt.getInstance().getGameManager().getCurrentState().paused
                ? Component.text("§c§lPausiert §8(§c/timer resume§8)")
                : Component.text("§c§lLaufend §8(§c/timer pause§8)");
        player.sendMessage(Component.text("    §cStatus§8: ").append(status));
        player.sendMessage(Component.text("§c/timer skip §8-> §7Setzt den Timer auf 5 Sekunden."));
        player.sendMessage(Component.text(" "));
    }
}
