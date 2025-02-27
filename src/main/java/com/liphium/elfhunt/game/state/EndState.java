package com.liphium.elfhunt.game.state;

import com.liphium.elfhunt.Elfhunt;
import com.liphium.elfhunt.game.GameState;
import com.liphium.elfhunt.util.LocationAPI;
import com.liphium.elfhunt.util.Messages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Objects;

public class EndState extends GameState {

    public EndState() {
        super("Ending", 20);
    }

    @Override
    public void start() {

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setGameMode(GameMode.SURVIVAL);
            player.teleport(Objects.requireNonNull(LocationAPI.getLocation("Elves")));

            player.getInventory().clear();
            player.getInventory().setHelmet(null);
            player.getInventory().setChestplate(null);
            player.getInventory().setLeggings(null);
            player.getInventory().setBoots(null);
        }

        Elfhunt.getInstance().getTaskManager().inject(new Runnable() {
            int tickCount = 0;

            @Override
            public void run() {

                if (tickCount++ >= 20) {
                    tickCount = 0;
                    if (!paused) count--;

                    Messages.actionBar(Component.text("Stoppen ", NamedTextColor.RED)
                            .append(Component.text("in ", NamedTextColor.GRAY))
                            .append(Component.text(count, NamedTextColor.RED, TextDecoration.BOLD))
                            .append(Component.text("..", NamedTextColor.GRAY))
                    );

                    if (count == 0) {
                        Bukkit.shutdown();
                    }
                }

            }
        });
    }

    @Override
    public void onInteract(PlayerInteractEvent event) {
        event.setCancelled(true);
    }

    @Override
    public void onBreak(BlockBreakEvent event) {
        event.setCancelled(true);
    }

    @Override
    public void onPlace(BlockPlaceEvent event) {
        event.setCancelled(true);
    }

    @Override
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        event.setCancelled(true);
    }

    @Override
    public void onDamage(EntityDamageEvent event) {
        event.setCancelled(true);
    }
}
