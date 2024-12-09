package com.liphium.elfhunt.listener.machines.impl;

import com.liphium.core.Core;
import com.liphium.elfhunt.listener.machines.Machine;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;


public class PresentReceiver extends Machine {

    private final ArmorStand stand;
    private final ArrayList<String> names = new ArrayList<>(List.of(
            "Julian", "Thorben", "Crash", "Jonas", "Shadow", "Tim",
            "Atlas", "Animo", "Louis", "Liph", "Ritso", "Glaze", "Jumbo",
            "JuliPvP99", "Night", "Maya", "Theo", "Prime", "Lofi Girl", "Alex",
            "Steve", "Arch", "Colin", "Dani", "Dave", "David", "Eric", "Lofi Boy",
            "Matt", "Elon Musk", "Linus LTT", "Neko"
    ));

    public PresentReceiver(Location location) {
        super(location, false);

        stand = location.getWorld().spawn(location.clone().add(0, 0, 0), ArmorStand.class);
        setupStand();
    }

    public PresentReceiver(ArmorStand stand) {
        super(stand.getLocation(), false);

        this.stand = stand;
        setupStand();
    }

    void setupStand() {
        stand.setCustomNameVisible(true);
        stand.customName(Component.text("Unnamed", NamedTextColor.WHITE, TextDecoration.BOLD));
        stand.setGravity(false);
        stand.setInvulnerable(true);
        stand.setRemoveWhenFarAway(false);
        stand.setBasePlate(false);

        // Get her some drip
        stand.getEquipment().setHelmet(new ItemStack(Material.PLAYER_HEAD));
        ItemStack chestplate = new ItemStack(Material.LEATHER_CHESTPLATE);
        ItemStack leggings = new ItemStack(Material.LEATHER_LEGGINGS);
        ItemStack boots = new ItemStack(Material.LEATHER_BOOTS);
        LeatherArmorMeta chestplateMeta = (LeatherArmorMeta) chestplate.getItemMeta();
        chestplateMeta.setColor(Color.fromRGB(255, 255, 255));
        chestplate.setItemMeta(chestplateMeta);
        LeatherArmorMeta leggingsMeta = (LeatherArmorMeta) leggings.getItemMeta();
        leggingsMeta.setColor(Color.fromRGB(255, 255, 255));
        leggings.setItemMeta(leggingsMeta);
        LeatherArmorMeta bootsMeta = (LeatherArmorMeta) boots.getItemMeta();
        bootsMeta.setColor(Color.fromRGB(255, 255, 255));
        boots.setItemMeta(bootsMeta);
        stand.getEquipment().setChestplate(chestplate);
        stand.getEquipment().setLeggings(leggings);
        stand.getEquipment().setBoots(boots);
    }

    @Override
    public void onInteractAtEntity(PlayerInteractAtEntityEvent event) {
        if (event.getRightClicked().equals(stand)) {
            Core.getInstance().getScreens().open(event.getPlayer(), 3);
            event.setCancelled(true);
        }
    }

    @Override
    public void destroy() {
        stand.remove();
    }
}
