package com.liphium.elfhunt.listener.machines.impl;

import com.liphium.core.util.ItemStackBuilder;
import com.liphium.elfhunt.listener.machines.Machine;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class ItemDropper extends Machine {

    private final ArmorStand stand;
    private final String name;
    private final NamedTextColor color;
    private final ItemStack toDrop;
    private final int dropRate;

    public ItemDropper(Location location, String name, NamedTextColor color, ItemStack toDrop, int ticks) {
        super(location, true);

        this.name = name;
        this.color = color;
        this.toDrop = toDrop;
        this.dropRate = ticks;

        stand = location.getWorld().spawn(location.clone().add(0.5, -0.5, 0.5), ArmorStand.class);

        stand.setCustomNameVisible(true);
        stand.customName(Component.text(name, color).appendSpace()
                .append(Component.text("production happens here.", NamedTextColor.GRAY)));
        stand.setGravity(false);
        stand.setInvisible(true);
        stand.setInvulnerable(true);
        stand.setRemoveWhenFarAway(false);
        count = ticks;
    }

    int count = 10, tickCount = 0;

    @Override
    public void tick() {
        if (broken) return;

        if (tickCount++ >= 20) {
            tickCount = 0;

            count--;
            stand.customName(Component.text(name, color).appendSpace()
                    .append(Component.text("in", NamedTextColor.GRAY)).appendSpace()
                    .append(Component.text(count, NamedTextColor.RED, TextDecoration.BOLD)).appendSpace()
                    .append(Component.text("..", NamedTextColor.GRAY))
            );

            if (count == 0) {
                count = dropRate;

                Item item = location.getWorld().dropItem(location.clone().add(0.5, 1.5, 0.5), toDrop);
                item.setVelocity(new Vector());
            }
        }
    }
    
    @Override
    public void destroy() {
        stand.remove();
    }
}
