package com.tommytony.war.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import org.apache.commons.lang.Validate;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;

/**
 * Storage class for MySQL configuration settings.
 *
 * @author cmastudios
 */
public class MySQLConfig {

	private ConfigurationSection section;

	/**
	 * Load the values from the specified section into the MySQL config.
	 *
	 * @param section Section to load MySQL settings from.
	 */
	public MySQLConfig(ConfigurationSection section) {
		this.section = section;
	}

	/**
	 * Create a new MySQL configuration section with default values.
	 */
	public MySQLConfig() {
		this(new MemoryConfiguration());
		section.set("enabled", false);
		section.set("host", "localhost");
		section.set("port", 3306);
		section.set("database", "war");
		section.set("username", "root");
		section.set("password", "meow");
		section.set("logging.enabled", false);
		section.set("logging.autoclear",
				"WHERE `date` < NOW() - INTERVAL 7 DAY");
	}

	/**
	 * Check if MySQL support is enabled.
	 *
	 * @return true if MySQL support is enabled, false otherwise.
	 */
	public boolean isEnabled() {
		return section.getBoolean("enabled");
	}

	/**
	 * Check if kill-death logging is enabled.
	 *
	 * @return true if kill-death logging is enabled, false otherwise.
	 */
	public boolean isLoggingEnabled() {
		return section.getBoolean("logging.enabled");
	}

	/**
	 * Get WHERE clause for automatic deletion from database table.
	 *
	 * @return deletion WHERE clause or empty string.
	 */
	public String getLoggingDeleteClause() {
		return section.getString("logging.autoclear", "");
	}

	private String getJDBCUrl() {
		return String.format("jdbc:mysql://%s:%d/%s?user=%s&password=%s",
				section.getString("host"), section.getInt("port"),
				section.getString("database"), section.getString("username"),
				section.getString("password"));
	}

	/**
	 * Get a connection to the MySQL database represented by this configuration.
	 *
	 * @return connection to MySQL database.
	 * @throws SQLException Error occured connecting to database.
	 * @throws IllegalArgumentException MySQL support is not enabled.
	 */
	public Connection getConnection() throws SQLException {
		Validate.isTrue(this.isEnabled(), "MySQL support is not enabled");
		return DriverManager.getConnection(this.getJDBCUrl());
	}

	/**
	 * Copy represented configuration into another configuration section.
	 *
	 * @param section Mutable section to write values in.
	 */
	public void saveTo(ConfigurationSection section) {
		Map<String, Object> values = this.section.getValues(true);
		for (Map.Entry<String, Object> entry : values.entrySet()) {
			section.set(entry.getKey(), entry.getValue());
		}
	}
}
