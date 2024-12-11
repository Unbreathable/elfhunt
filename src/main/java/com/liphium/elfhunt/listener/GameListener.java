package com.liphium.elfhunt.listener;

import com.liphium.elfhunt.Elfhunt;
import com.liphium.elfhunt.game.state.LobbyState;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;

public class GameListener implements Listener {

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Elfhunt.getInstance().getGameManager().getCurrentState().onInteract(event);
    }

    @EventHandler
    public void onInteractAtEntity(PlayerInteractAtEntityEvent event) {
        Elfhunt.getInstance().getGameManager().getCurrentState().onInteractAtEntity(event);
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        Elfhunt.getInstance().getGameManager().getCurrentState().onBreak(event);
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        Elfhunt.getInstance().getGameManager().getCurrentState().onPlace(event);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Elfhunt.getInstance().getGameManager().getCurrentState().onMove(event);
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        Elfhunt.getInstance().getGameManager().getCurrentState().onDamage(event);
    }

    @EventHandler
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        Elfhunt.getInstance().getGameManager().getCurrentState().onDamageByEntity(event);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Elfhunt.getInstance().getGameManager().getCurrentState().onDeath(event);
    }

    @EventHandler
    public void onDropItem(PlayerDropItemEvent event) {
        Elfhunt.getInstance().getGameManager().getCurrentState().onDrop(event);
    }

    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent event) {
        if (event.getEntityType().equals(EntityType.ITEM)
                || event.getEntityType().equals(EntityType.FIREWORK_ROCKET)
                || event.getEntityType().equals(EntityType.ARMOR_STAND)
                || event.getEntityType().equals(EntityType.POTION)
                || event.getEntityType().equals(EntityType.AREA_EFFECT_CLOUD)) {
            Elfhunt.getInstance().getGameManager().getCurrentState().onSpawn(event);
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onRocket(FireworkExplodeEvent event) {
        Elfhunt.getInstance().getGameManager().getCurrentState().onFirework(event);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Elfhunt.getInstance().getGameManager().getCurrentState().onRespawn(event);
    }

    @EventHandler
    public void onFood(FoodLevelChangeEvent event) {
        if (Elfhunt.getInstance().getGameManager().getCurrentState() instanceof LobbyState) {
            return;
        }

        event.setCancelled(true);
    }
}
