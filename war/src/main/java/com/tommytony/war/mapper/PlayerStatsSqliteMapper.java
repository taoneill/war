package com.tommytony.war.mapper;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;

import com.tommytony.war.War;

public class PlayerStatsSqliteMapper extends PlayerStatsDatabaseMapper {
	
	@Override
	protected Connection getConnection() {
		try {
			return DriverManager.getConnection("jdbc:sqlite:plugins/War/" + War.war.getStorageConfig().getString("database.sql.database_name"));
		} catch (SQLException e) {
			War.war.log("Failed to connect to sqlite database", Level.SEVERE);
			return null;
		}
	}

	@Override
	protected String getDatabaseString() {
		return "sqlite";
	}
}
