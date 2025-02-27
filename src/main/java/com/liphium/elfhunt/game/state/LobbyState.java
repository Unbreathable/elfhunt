package com.liphium.elfhunt.game.state;

import com.liphium.core.Core;
import com.liphium.core.util.ItemStackBuilder;
import com.liphium.elfhunt.Elfhunt;
import com.liphium.elfhunt.game.GameState;
import com.liphium.elfhunt.listener.machines.Machine;
import com.liphium.elfhunt.util.LocationAPI;
import com.liphium.elfhunt.util.Messages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.time.Duration;

public class LobbyState extends GameState {

    // Constants
    private final int NEEDED_PlAYERS = 1;

    public LobbyState() {
        super("Waiting for players", 200);
    }

    @Override
    public void start() {

        Location location = LocationAPI.getLocation("Elves");
        if (location != null && location.getWorld() != null) {
            location.getWorld().setTime(0);
            location.getWorld().setThundering(false);
            location.getWorld().setStorm(false);
            location.getWorld().setGameRule(GameRule.NATURAL_REGENERATION, false);
            location.getWorld().setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
            location.getWorld().setGameRule(GameRule.DO_WEATHER_CYCLE, false);
            location.getWorld().setDifficulty(Difficulty.PEACEFUL);

            // World cleanup
            for (Entity entity : location.getWorld().getEntities()) {
                if (!(entity instanceof Player)) entity.remove();
            }

        } else {
            Bukkit.broadcast(Component.text("Please set up the server first.", NamedTextColor.RED));
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setHealth(20);
            player.setFoodLevel(20);
            player.getActivePotionEffects().clear();
        }

        Elfhunt.getInstance().getTaskManager().inject(new Runnable() {
            int tickCount = 0;

            @Override
            public void run() {

                if (tickCount++ >= 20) {
                    tickCount = 0;

                    // World cleanup
                    for (Entity entity : location.getWorld().getEntities()) {
                        if (entity.getType().equals(EntityType.ITEM)) entity.remove();
                    }

                    if (!Bukkit.getOnlinePlayers().isEmpty()) {
                        if (!paused) count--;

                        if (count <= 5) {

                            if (count == 0) {

                                for (Player player : Bukkit.getOnlinePlayers()) {
                                    player.showTitle(Title.title(
                                            Component.text("Elfhunt", NamedTextColor.GREEN, TextDecoration.BOLD),
                                            Component.text("Christmas Special", NamedTextColor.GRAY),
                                            Title.Times.times(Duration.ofSeconds(0), Duration.ofSeconds(3), Duration.ofSeconds(1))
                                    ));
                                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                                }

                                Elfhunt.getInstance().getTaskManager().uninject(this);
                                Elfhunt.getInstance().getGameManager().setCurrentState(new IngameState());
                                return;
                            }

                            for (Player player : Bukkit.getOnlinePlayers()) {
                                player.showTitle(Title.title(
                                        Component.text(count, NamedTextColor.GREEN, TextDecoration.BOLD)
                                                .append(Component.text("..", NamedTextColor.GRAY)),
                                        Component.text("Elfhunt", NamedTextColor.GRAY),
                                        Title.Times.times(Duration.ofSeconds(0), Duration.ofSeconds(3), Duration.ofSeconds(1))
                                ));
                                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 1f);
                            }

                        } else if (count % 10 == 0 && count <= 100) {
                            Bukkit.broadcast(Elfhunt.PREFIX
                                    .append(Component.text("The ", NamedTextColor.GRAY))
                                    .append(Component.text("game ", NamedTextColor.GREEN))
                                    .append(Component.text("starts in ", NamedTextColor.GRAY))
                                    .append(Component.text(count + " seconds", NamedTextColor.GREEN))
                                    .append(Component.text(".", NamedTextColor.GRAY))
                            );
                        }

                        if (paused) {
                            Messages.actionBar(Component.text("Countdown ", NamedTextColor.GRAY)
                                    .append(Component.text("paused", NamedTextColor.GREEN)));
                        } else {
                            Messages.actionBar(Component.text(count, NamedTextColor.GREEN, TextDecoration.BOLD)
                                    .append(Component.text("..", NamedTextColor.GRAY)));
                        }
                    } else {

                        Messages.actionBar(Component.text("Waiting for ", NamedTextColor.GRAY)
                                .append(Component.text("players", NamedTextColor.GREEN))
                                .append(Component.text(".. (", NamedTextColor.GRAY))
                                .append(Component.text(Bukkit.getOnlinePlayers().size(), NamedTextColor.GREEN))
                                .append(Component.text("/", NamedTextColor.GRAY))
                                .append(Component.text(NEEDED_PlAYERS, NamedTextColor.GREEN))
                                .append(Component.text(")", NamedTextColor.GRAY)));
                        count = 199;
                    }

                }

            }
        });

    }

    @Override
    public void onInteract(PlayerInteractEvent event) {
        if (event.getItem() != null && event.getItem().getType().equals(Material.SADDLE)) {
            Core.getInstance().getScreens().open(event.getPlayer(), 1);
        }
    }

    @Override
    public void onInteractAtEntity(PlayerInteractAtEntityEvent event) {
        event.setCancelled(event.getPlayer().getGameMode() != GameMode.CREATIVE);
    }

    @Override
    public void join(Player player) {
        player.setGameMode(GameMode.SURVIVAL);
        player.setHealth(20);

        player.getInventory().clear();
        player.getInventory().setHelmet(null);
        player.getInventory().setChestplate(null);
        player.getInventory().setLeggings(null);
        player.getInventory().setBoots(null);

        player.getInventory().setItem(4, new ItemStackBuilder(Material.SADDLE).withName(Component.text("§a§lTeams §7(Right-click)"))
                .withLore(Component.text("§7§oJoin a team.")).buildStack());

        player.teleport(LocationAPI.getLocation("Elves"));

        Elfhunt.getInstance().getTaskManager().inject(new Runnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks++ >= 10) {
                    player.teleport(LocationAPI.getLocation("Elves"));
                    Elfhunt.getInstance().getTaskManager().uninject(this);
                }
            }
        });
    }

    @Override
    public void onDamage(EntityDamageEvent event) {
        event.setCancelled(true);
    }

    @Override
    public void onBreak(BlockBreakEvent event) {
        if (event.getPlayer().getGameMode().equals(GameMode.CREATIVE)) return;
        event.setCancelled(true);
    }

    @Override
    public void onDrop(PlayerDropItemEvent event) {
        event.setCancelled(true);
    }

    @Override
    public void onPlace(BlockPlaceEvent event) {
        if (event.getPlayer().getGameMode().equals(GameMode.CREATIVE)) return;
        event.setCancelled(true);
    }
}
