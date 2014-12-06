package com.tommytony.war.zone;

import com.google.common.base.Optional;
import com.tommytony.war.WarPlugin;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Representation of a war zone area, blocks, and settings.
 * Each zone is to only have one instance of Warzone.class at any given time while the server is running.
 */
public class Warzone {
    private static Pattern zoneName = Pattern.compile("[./%]+");
    private final WarPlugin plugin;
    private final String name;
    private final ZoneStorage db;
    private final ZoneConfig config;
    private Location teleport;

    /**
     * Load or create a war zone from the war settings store.
     *
     * @param plugin Instance of the plugin War.
     * @param name   Name of the zone. Used to locate the zone data file.
     */
    public Warzone(WarPlugin plugin, String name) {
        this.plugin = plugin;
        this.name = name;
        try {
            this.db = new ZoneStorage(this, plugin);
            this.config = new ZoneConfig(db.getConnection(), "settings", plugin.getConfig().getZoneDefaults());
        } catch (SQLException ex) {
            plugin.getLogger().error("Failed to load zone database and settings", ex);
            throw new RuntimeException("Can't create/load database for warzone " + name);
        }
    }

    /**
     * Check if the specified name is valid for a zone.
     * Specifically, characters that could be part of a file path or web URL.
     * '.', '/', and '%' are invalid.
     *
     * @param name Potential zone name to check.
     * @return true if the name can be used.
     */
    public static boolean zoneNameValid(String name) {
        Matcher m = zoneName.matcher(name);
        return m.find();
    }

    public String getName() {
        return name;
    }

    public Location getTeleport() {
        try {
            Optional<Location> lobby = db.getPosition("lobby", Optional.<World>absent());
            if (lobby.isPresent())
                return lobby.get();
            else throw new RuntimeException("No teleport location found for zone " + name);
        } catch (SQLException e) {
            plugin.getLogger().error("Retrieving teleport", e);
            throw new RuntimeException("Error in retrieving information from database");
        }
    }
}
