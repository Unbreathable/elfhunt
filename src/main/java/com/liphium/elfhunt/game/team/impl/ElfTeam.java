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
        player.getInventory().setBoots(new ItemStackBuilder(Material.LEATHER_BOOTS).makeUnbreakable()
                .withLeatherColor(Color.RED).buildStack());
        player.getInventory().setHelmet(new ItemStackBuilder(Material.LEATHER_HELMET).makeUnbreakable()
                .withLeatherColor(Color.RED).buildStack());
        player.getInventory().setLeggings(new ItemStackBuilder(Material.NETHERITE_LEGGINGS).makeUnbreakable().buildStack());

        player.getInventory().addItem(new ItemStackBuilder(Material.STICK).makeUnbreakable()
                .withName(Component.text("§c§lCatcher")).withLore(
                        Component.text("§7§oWhen you hit a human with"),
                        Component.text("§7§othis stick they will be infected,"),
                        Component.text("§7§owhen you hit them again, they will"),
                        Component.text("§7§obe sent straight to the cell!"))
                .addEnchantment(Enchantment.KNOCKBACK, 2).buildStack());

        player.getInventory().addItem(new ItemStackBuilder(Material.CROSSBOW).withName(Component.text("§c§lCrossbow"))
                .withLore(Component.text("§7§oWhen you shoot at torches"),
                        Component.text("§7§othey'll be destroyed."))
                .makeUnbreakable().buildStack());

        ItemStack rocket = new ItemStackBuilder(Material.FIREWORK_ROCKET).withName(Component.text("§c§lRockets"))
                .withLore(Component.text("§7§oAmmo for your crossbow.")).buildStack();
        FireworkMeta meta = (FireworkMeta) rocket.getItemMeta();
        meta.setPower(3);
        meta.addEffect(FireworkEffect.builder().withColor(Color.RED).build());
        rocket.setItemMeta(meta);

        player.getInventory().setItemInOffHand(rocket);

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
