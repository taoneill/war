package com.tommytony.war.zone;

import java.sql.*;

/**
 * The zone configuration settings database.
 */
public class ZoneConfig {
    /**
     * Database configuration descriptor.
     */
    private final Connection conn;
    /**
     * Table of values to manage. May be a table in a zone database or the main war database.
     */
    private final String table;
    /**
     * Root zone config, for fallback. Null if this is the war main settings.
     */
    private final ZoneConfig parent;

    /**
     * Manages a zone configuration section.
     *
     * @param database Active database to use.
     * @param table    Table name to use in database. Created if it does not exist. Needs to be trusted input.
     * @param parent   Parent zone config, for fallback. Could be zone config for a team or war global for zones.
     * @throws SQLException
     */
    public ZoneConfig(Connection database, String table, ZoneConfig parent) throws SQLException {
        this.conn = database;
        this.table = table;
        this.parent = parent;
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(String.format("CREATE TABLE IF NOT EXISTS %s (option TEXT, value BLOB)", table));
        }
    }

    /**
     * Manages a zone configuration section.
     *
     * @param database Active database to use.
     * @param table    Table name to use in database. Created if it does not exist. Needs to be trusted input.
     * @throws SQLException
     */
    public ZoneConfig(Connection database, String table) throws SQLException {
        this(database, table, null);
    }

    /**
     * Get the value of an integer setting.
     *
     * @param setting The type of setting to look up.
     * @return the value of the setting or the default if not found.
     * @throws SQLException
     */
    public int getInt(ZoneSetting setting) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(String.format("SELECT value FROM %s WHERE option = ?", table))) {
            stmt.setString(1, setting.name());
            try (ResultSet result = stmt.executeQuery()) {
                if (result.next()) {
                    // found an override for this config level
                    return result.getInt(1);
                } else if (parent != null) {
                    // look for it in zone/global configs; will be recursive upwards
                    return parent.getInt(setting);
                } else {
                    // the hard-coded value for fallback
                    return (Integer) setting.getDefaultValue();
                }
            }
        }
    }

}
