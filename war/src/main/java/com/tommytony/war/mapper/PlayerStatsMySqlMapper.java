package com.tommytony.war.mapper;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;

import com.tommytony.war.War;

public class PlayerStatsMySqlMapper extends PlayerStatsDatabaseMapper {

	@Override
	protected String getDatabaseString() {
		return "mysql";
	}

	@Override
	protected Connection getConnection() {
		try {
		    return DriverManager.getConnection("jdbc:mysql://"
				    + War.war.getStorageConfig().getString("database.sql.host") + "/"
				    + War.war.getStorageConfig().getString("database.sql.databasename") + "?user="
				    + War.war.getStorageConfig().getString("database.sql.user") + "&password="
				    + War.war.getStorageConfig().getString("database.sql.password")
	        );
		} catch(SQLException e) {
			War.war.log("Failed to estabish connection to the mysql database", Level.WARNING);
			return null;
		}
	}
}
