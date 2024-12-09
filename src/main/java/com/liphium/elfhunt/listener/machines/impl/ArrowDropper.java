package com.liphium.elfhunt.listener.machines.impl;

import com.liphium.core.util.ItemStackBuilder;
import com.liphium.elfhunt.listener.machines.Machine;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.util.Vector;

public class ArrowDropper extends Machine {

    private final ArmorStand stand;

    public ArrowDropper(Location location) {
        super(location, true);

        stand = location.getWorld().spawn(location.clone().add(0.5, -0.5, 0.5), ArmorStand.class);

        stand.setCustomNameVisible(true);
        stand.customName(Component.text("Arrows", NamedTextColor.RED).appendSpace()
                .append(Component.text("are produced here.", NamedTextColor.GRAY)));
        stand.setGravity(false);
        stand.setInvisible(true);
        stand.setInvulnerable(true);
        stand.setRemoveWhenFarAway(false);
    }

    int count = 20, tickCount = 0;

    @Override
    public void tick() {
        if (broken) return;

        if (tickCount++ >= 20) {
            tickCount = 0;

            count--;
            stand.customName(Component.text("Arrow", NamedTextColor.RED).appendSpace()
                    .append(Component.text("in", NamedTextColor.GRAY)).appendSpace()
                    .append(Component.text(count, NamedTextColor.RED, TextDecoration.BOLD)).appendSpace()
                    .append(Component.text("..", NamedTextColor.GRAY))
            );

            if (count == 0) {
                count = 20;

                ItemStack arrow = new ItemStackBuilder(Material.ARROW).buildStack();
                FireworkMeta meta = (FireworkMeta) arrow.getItemMeta();
                meta.setPower(3);
                meta.addEffect(FireworkEffect.builder().withColor(Color.RED).build());
                arrow.setItemMeta(meta);

                Item item = location.getWorld().dropItem(location.clone().add(0.5, 1.5, 0.5), arrow);
                item.setVelocity(new Vector());
            }
        }
    }
    
    @Override
    public void destroy() {
        stand.remove();
    }
}
