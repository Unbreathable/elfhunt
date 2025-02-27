package com.liphium.elfhunt.util;

import com.liphium.elfhunt.Elfhunt;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.HashMap;

public class LocationAPI {

    private static final HashMap<String, Location> locations = new HashMap<>();

    public static void setLocation(String locationName, Location loc) {
        Elfhunt.getInstance().getConfig().set(locationName, loc);
        Elfhunt.getInstance().saveConfig();
        locations.put(locationName, loc);
    }

    public static boolean exists(String locationName) {
        return Elfhunt.getInstance().getConfig().isSet(locationName);
    }

    /**
     * Get a location by name in the elfhunt world
     *
     * @param locationName Name of the location in the config file
     * @return The location in the specified world
     */
    public static Location getLocation(String locationName) {

        // Send a warning when the location doesn't exist yet
        if (!exists(locationName)) {
            Bukkit.broadcast(Component.text("Location " + locationName + " not found!").color(NamedTextColor.RED));
            return null;
        }

        // Check if the location has already been cached
        if (locations.containsKey(locationName)) {
            return locations.get(locationName);
        }

        // Make the location
        Location location = Elfhunt.getInstance().getConfig().getLocation(locationName);

        if (location != null)
            location.setWorld(Bukkit.getWorld("elfhunt"));

        // Cache the location for use in the future
        locations.put(locationName, location);
        return location;
    }

}
