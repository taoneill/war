package com.tommytony.war.mapper;

import com.tommytony.war.War;
import com.tommytony.war.volume.Volume;
import com.tommytony.war.volume.ZoneVolume;
import org.apache.commons.lang.Validate;
import org.bukkit.World;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Loads and saves zone blocks to SQLite3 database.
 *
 * @author cmastudios
 * @since 1.8
 */
public class ZoneVolumeMapper extends VolumeMapper {

	/**
	 * Get a connection to the warzone database, converting blocks if not found.
	 * @param volume zone volume to load
	 * @param zoneName warzone to load
	 * @return an open connection to the sqlite file
	 * @throws SQLException
	 */
	public static Connection getZoneConnection(ZoneVolume volume, String zoneName) throws SQLException {
		File databaseFile = new File(War.war.getDataFolder(), String.format("/dat/warzone-%s/volume-%s.sl3", zoneName, volume.getName()));
		if (!databaseFile.exists()) {
			// dropped nimitz compatibility with the MC 1.13 update
			War.war.log("Warzone " + zoneName + " not found - creating new file. Will not attempt converting legacy War version formats.", Level.WARNING);
		}
		Connection databaseConnection = getConnection(databaseFile);
		int version = checkConvert(databaseConnection);
		switch (version) {
			case 0: // new file
				break;
			case 1:
			case 2:
				War.war.log(zoneName + " cannot be migrated from War 1.9 due to breaking MC1.13 changes - please resave.", Level.WARNING);
				convertSchema2_3(databaseConnection, "", false);
				for (String prefix : getStructures(databaseConnection)) {
					convertSchema2_3(databaseConnection, prefix, false);
				}
				break;
			case 3:
				break;
			default:
				throw new IllegalStateException(String.format("Unsupported volume format (was already converted to version: %d, current format: %d)", version, DATABASE_VERSION));
		}
		return databaseConnection;
	}

	private static List<String> getStructures(Connection databaseConnection) throws SQLException {
		List<String> structures = new ArrayList<>();
		Statement stmt = databaseConnection.createStatement();
		ResultSet q = stmt.executeQuery("SELECT name FROM sqlite_master WHERE type='table'");
		while (q.next()) {
			String name = q.getString("name");
			if (name.contains("structure") && name.contains("corners")) {
				structures.add(name.replace("corners", ""));
			}
		}
		q.close();
		stmt.close();
		return structures;
	}

	/**
	 * Loads the given volume
	 *
	 * @param databaseConnection Open connection to zone database
	 * @param volume Volume to load
	 * @param start Starting position to load blocks at
	 * @param total Amount of blocks to read
	 * @return Changed blocks
	 * @throws SQLException Error communicating with SQLite3 database
	 */
	public static int reloadZoneBlocks(Connection databaseConnection, ZoneVolume volume, int start, int total, boolean[][][] changes) throws SQLException {
		return loadBlocks(databaseConnection, volume, start, total, changes, false, "");
	}


	public static int saveStructure(Volume volume, Connection databaseConnection) throws SQLException {
		String prefix = String.format("structure_%d_", volume.getName().hashCode() & Integer.MAX_VALUE);
		saveCorners(databaseConnection, volume, prefix);
		return saveBlocks(databaseConnection, volume, prefix);
	}

	public static void loadStructure(Volume volume, Connection databaseConnection) throws SQLException {
		String prefix = String.format("structure_%d_", volume.getName().hashCode() & Integer.MAX_VALUE);
		World world = volume.getWorld();
		Validate.notNull(world, String.format("Cannot find the warzone for %s", prefix));
		loadCorners(databaseConnection, volume, world, prefix);
		loadBlocks(databaseConnection, volume, 0, 0, null, true, prefix);
	}

	/**
	 * Get total saved blocks for a warzone. This should only be called on nimitz-format warzones.
	 * @param volume Warzone volume
	 * @param zoneName Name of zone file
	 * @return Total saved blocks
	 * @throws SQLException
	 */
	public static int getTotalSavedBlocks(ZoneVolume volume, String zoneName) throws SQLException {
		Connection databaseConnection = getZoneConnection(volume, zoneName);
		Statement stmt = databaseConnection.createStatement();
		ResultSet sizeQuery = stmt.executeQuery("SELECT COUNT(*) AS total FROM blocks");
		int size = sizeQuery.getInt("total");
		sizeQuery.close();
		stmt.close();
		databaseConnection.close();
		return size;
	}

	/**
	 * Save all war zone blocks to a SQLite3 database file.
	 *
	 * @param volume Volume to save (takes corner data and loads from world).
	 * @param zoneName Name of warzone to save.
	 * @return amount of changed blocks
	 * @throws SQLException
	 */
	public static int saveZoneBlocksAndEntities(ZoneVolume volume, String zoneName) throws SQLException {
		File warzoneDir = new File(War.war.getDataFolder().getPath() + "/dat/warzone-" + zoneName);
		if (!warzoneDir.exists() && !warzoneDir.mkdirs()) {
			throw new RuntimeException("Failed to create warzone data directory");
		}
		Connection databaseConnection = getZoneConnection(volume, zoneName);
		int changed = 0;
		saveCorners(databaseConnection, volume, "");
		saveBlocks(databaseConnection, volume, "");
		saveEntities(databaseConnection, volume);
		databaseConnection.close();
		return changed;
	}

}
