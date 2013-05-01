package com.tommytony.war.mapper;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;

import com.tommytony.war.War;

/**
 * @author grinning
 * @package com.tommytony.war.mapper
 * @description A subclass that completes the Database Mapper class for the Sqlite Database
 */

public class PlayerStatsSqliteMapper extends PlayerStatsDatabaseMapper {
	
	@Override
	protected Connection getConnection() {
		try {
			Class.forName("org.sqlite.JDBC");
			return DriverManager.getConnection("jdbc:sqlite:" + this.parseDatabaseName(War.war.getStorageConfig().getString("database.sql.databaselocation")));
		} catch (SQLException e) {
			War.war.log("Failed to connect to sqlite database ", Level.SEVERE);
			e.printStackTrace();
			return null;
		} catch (ClassNotFoundException e) {
			War.war.log("No Sqlite library found!", Level.SEVERE);
			return null;
		}
	}

	@Override
	protected String getDatabaseString() {
		return "sqlite";
	}
}
