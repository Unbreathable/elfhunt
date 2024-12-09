package com.liphium.elfhunt.listener.machines;

import org.bukkit.Location;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class Machine {

    public final Location location;
    private final boolean breakable;
    public boolean broken = false;

    public Machine(Location location, boolean breakable) {
        this.location = location;
        this.breakable = breakable;
    }

    public boolean isBreakable() {
        return breakable;
    }

    public Location getLocation() {
        return location;
    }

    public void tick() {
    }

    public void destroy() {
    }

    public void onInteract(PlayerInteractEvent event) {
    }

    public void onInteractAtEntity(PlayerInteractAtEntityEvent event) {
    }

}
