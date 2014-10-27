package com.tommytony.war.zone;

import com.tommytony.war.WarPlugin;

import java.io.File;
import java.sql.*;

/**
 * Manages the war zone database file, which contains all the data for the war zone.
 */
public class ZoneStorage implements AutoCloseable {
    private static int DATABASE_VERSION = 1;
    private final Warzone zone;
    private final WarPlugin plugin;
    private final Connection connection;
    private final File dataStore;

    /**
     * Initiates a database for a new or existing database.
     *
     * @param zone   The server war zone object for this database.
     * @param plugin The war plugin, for storage information and configuration.
     * @throws SQLException
     */
    ZoneStorage(Warzone zone, WarPlugin plugin) throws SQLException {
        this.zone = zone;
        this.plugin = plugin;
        dataStore = new File(plugin.getDataDir(), String.format("%s.warzone", zone.getName()));
        connection = DriverManager.getConnection("jdbc:sqlite:" + dataStore.getPath());
        this.upgradeDatabase();
    }

    Connection getConnection() {
        return connection;
    }

    /**
     * Check the database stored version information and perform upgrade tasks if necessary.
     *
     * @throws SQLException
     */
    private void upgradeDatabase() throws SQLException {
        int version;
        try (
                Statement stmt = connection.createStatement();
                ResultSet resultSet = stmt.executeQuery("PRAGMA user_version");
        ) {
            version = resultSet.getInt("user_version");
        }
        if (version > DATABASE_VERSION) {
            // version is from a future version
            throw new IllegalStateException(String.format("Unsupported zone version: %d. War current version: %d",
                    version, DATABASE_VERSION));
        } else if (version == 0) {
            // brand new database file
        } else if (version < DATABASE_VERSION) {
            // upgrade
            switch (version) {
                // none yet
                default:
                    // some odd bug or people messing with their database
                    throw new IllegalStateException(String.format("Unsupported zone version: %d.", version));
            }
        }
    }

    /**
     * Closes this resource, relinquishing any underlying resources.
     *
     * @throws Exception if this resource cannot be closed
     */
    @Override
    public void close() throws Exception {
        connection.close();
    }
}
