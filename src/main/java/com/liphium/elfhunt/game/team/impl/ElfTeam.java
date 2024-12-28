package com.liphium.elfhunt.game.team.impl;

import com.liphium.core.util.ItemStackBuilder;
import com.liphium.elfhunt.game.team.Team;
import com.liphium.elfhunt.util.LocationAPI;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;

import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

public class ElfTeam extends Team {

    public ElfTeam() {
        super("Elves", "§a§l", Material.SPRUCE_SAPLING);
    }

    @Override
    public void giveKit(Player player, boolean teleport) {
        if (teleport) {
            player.teleport(Objects.requireNonNull(LocationAPI.getLocation("Elves")));
        }
    }

    @Override
    public void sendStartMessage() {
        for (Player player : getPlayers()) {

            player.sendMessage(Component.text(" "));
            player.sendMessage(Component.text("    §7You are an §a§lelf§7!"));
            player.sendMessage(Component.text(" "));
            player.sendMessage(Component.text("§7Deliver all §apresents §7into town and"));
            player.sendMessage(Component.text("§7make sure §aeveryone §7gets one."));
            player.sendMessage(Component.text(" "));
        }
    }

    @Override
    public void handleWin() {

        Bukkit.broadcast(Component.text(" "));
        Bukkit.broadcast(Component.text("   §aThe §a§lElves §7won the §agame§7!"));
        Bukkit.broadcast(Component.text(" "));
        Bukkit.broadcast(Component.text("§7All §apresents §7were delivered and the §aeveryone"));
        Bukkit.broadcast(Component.text("§7can enjoy a beautiful §aChristmas§7!"));
        Bukkit.broadcast(Component.text(" "));

        for (Player player : getPlayers()) {
            player.sendTitle("§a§lVictory Royale", null, 10, 60, 10);
            player.playSound(player.getLocation(), Sound.ITEM_GOAT_HORN_SOUND_1, 1f, 1f);
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!getPlayers().contains(player)) {
                player.sendTitle("§c§lGame Over", null, 10, 60, 10);
                player.playSound(player.getLocation(), Sound.ITEM_GOAT_HORN_SOUND_1, 1f, 1f);
            }
        }

    }
}
