package com.liphium.elfhunt.game.team.impl;

import com.liphium.core.util.ItemStackBuilder;
import com.liphium.elfhunt.game.team.Team;
import com.liphium.elfhunt.util.LocationAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.Objects;


public class HunterTeam extends Team {

    public HunterTeam() {
        super("Hunters", "§c§l", Material.IRON_SWORD);
    }

    @Override
    public void giveKit(Player player, boolean teleport) {
        if (teleport) {
            player.teleport(Objects.requireNonNull(LocationAPI.getLocation("Hunters")));
        }
    }

    @Override
    public void sendStartMessage() {
        for (Player player : getPlayers()) {

            player.sendMessage(Component.text(" "));
            player.sendMessage(Component.text("    §7You are a §a§lhunter§7!"));
            player.sendMessage(Component.text(" "));
            player.sendMessage(Component.text("§7Prevent the §aelves §7from giving out"));
            player.sendMessage(Component.text("§apresents §7and be happy about it."));
            player.sendMessage(Component.text(" "));

        }
    }

    @Override
    public void handleWin() {

        Bukkit.broadcast(Component.text(" "));
        Bukkit.broadcast(Component.text("   §cThe §c§lHunters §7won the §cgame§7!"));
        Bukkit.broadcast(Component.text(" "));
        Bukkit.broadcast(Component.text("§7The §celves §7weren't able to hand out all"));
        Bukkit.broadcast(Component.text("§cpresents §7in time. What a shame!"));
        Bukkit.broadcast(Component.text(" "));

        for (Player player : getPlayers()) {
            player.showTitle(Title.title(
                    Component.text("Victory Royale", NamedTextColor.GREEN, TextDecoration.BOLD),
                    Component.empty(),
                    Title.Times.times(Duration.ofSeconds(1), Duration.ofSeconds(3), Duration.ofSeconds(1))
            ));
            player.playSound(player.getLocation(), Sound.ITEM_GOAT_HORN_SOUND_1, 1f, 1f);
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!getPlayers().contains(player)) {
                player.showTitle(Title.title(
                        Component.text("Game Over", NamedTextColor.RED, TextDecoration.BOLD),
                        Component.empty(),
                        Title.Times.times(Duration.ofSeconds(1), Duration.ofSeconds(3), Duration.ofSeconds(1))
                ));
                player.playSound(player.getLocation(), Sound.ITEM_GOAT_HORN_SOUND_1, 1f, 1f);
            }
        }

    }
}
