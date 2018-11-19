package com.tommytony.war.mapper;

import com.tommytony.war.War;
import com.tommytony.war.volume.Volume;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.sql.*;
import java.util.logging.Level;

/**
 *
 * @author tommytony
 *
 */
public class VolumeMapper {

	public static Volume loadVolume(String volumeName, String zoneName, World world) throws SQLException {
		Volume volume = new Volume(volumeName, world);
		VolumeMapper.load(volume, zoneName, world);
		return volume;
	}

	public static void load(Volume volume, String zoneName, World world) throws SQLException {
		File databaseFile = new File(War.war.getDataFolder(), String.format(
				"/dat/volume-%s.sl3", volume.getName()));
		if (!zoneName.isEmpty()) {
			databaseFile = new File(War.war.getDataFolder(),
					String.format("/dat/warzone-%s/volume-%s.sl3", zoneName,
							volume.getName()));
		}
		if (!databaseFile.exists()) {
			// dropped nimitz compatibility with the MC 1.13 update
			War.war.log("Volume " + volume.getName() + " for zone " + zoneName + " not found. Will not attempt converting legacy War version formats.", Level.WARNING);
			return;
		}
		Connection databaseConnection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.getPath());
		Statement stmt = databaseConnection.createStatement();
		ResultSet versionQuery = stmt.executeQuery("PRAGMA user_version");
		int version = versionQuery.getInt("user_version");
		versionQuery.close();
		if (version > DATABASE_VERSION) {
			try {
				throw new IllegalStateException("Unsupported zone format " + version);
			} finally {
				stmt.close();
				databaseConnection.close();
			}
		} else if (version < DATABASE_VERSION) {
			switch (version) {
				// Run some update SQL for each old version
			}
		}
		ResultSet cornerQuery = stmt.executeQuery("SELECT * FROM corners");
		cornerQuery.next();
		final Block corner1 = world.getBlockAt(cornerQuery.getInt("x"), cornerQuery.getInt("y"), cornerQuery.getInt("z"));
		cornerQuery.next();
		final Block corner2 = world.getBlockAt(cornerQuery.getInt("x"), cornerQuery.getInt("y"), cornerQuery.getInt("z"));
		cornerQuery.close();
		volume.setCornerOne(corner1);
		volume.setCornerTwo(corner2);
		ResultSet query = stmt.executeQuery("SELECT * FROM blocks");
		while (query.next()) {
			int x = query.getInt("x"), y = query.getInt("y"), z = query.getInt("z");
			BlockState modify = corner1.getRelative(x, y, z).getState();
			ItemStack data = new ItemStack(Material.valueOf(query.getString("type")), 0, query.getShort("data"));
			modify.setType(data.getType());
			modify.setData(data.getData());
			volume.getBlocks().add(modify);
		}
		query.close();
		stmt.close();
		databaseConnection.close();
	}

	public static final int DATABASE_VERSION = 1;
	public static void save(Volume volume, String zoneName) throws SQLException {
		File databaseFile = new File(War.war.getDataFolder(), String.format(
				"/dat/volume-%s.sl3", volume.getName()));
		if (!zoneName.isEmpty()) {
			databaseFile = new File(War.war.getDataFolder(),
					String.format("/dat/warzone-%s/volume-%s.sl3", zoneName,
							volume.getName()));
		}
		Connection databaseConnection = DriverManager
				.getConnection("jdbc:sqlite:" + databaseFile.getPath());
		Statement stmt = databaseConnection.createStatement();
		stmt.executeUpdate("PRAGMA user_version = " + DATABASE_VERSION);
		stmt.executeUpdate("CREATE TABLE IF NOT EXISTS blocks (x BIGINT, y BIGINT, z BIGINT, type TEXT, data SMALLINT)");
		stmt.executeUpdate("CREATE TABLE IF NOT EXISTS corners (pos INTEGER PRIMARY KEY NOT NULL UNIQUE, x INTEGER NOT NULL, y INTEGER NOT NULL, z INTEGER NOT NULL)");
		stmt.executeUpdate("DELETE FROM blocks");
		stmt.executeUpdate("DELETE FROM corners");
		stmt.close();
		PreparedStatement cornerStmt = databaseConnection
				.prepareStatement("INSERT INTO corners SELECT 1 AS pos, ? AS x, ? AS y, ? AS z UNION SELECT 2, ?, ?, ?");
		cornerStmt.setInt(1, volume.getCornerOne().getBlockX());
		cornerStmt.setInt(2, volume.getCornerOne().getBlockY());
		cornerStmt.setInt(3, volume.getCornerOne().getBlockZ());
		cornerStmt.setInt(4, volume.getCornerTwo().getBlockX());
		cornerStmt.setInt(5, volume.getCornerTwo().getBlockY());
		cornerStmt.setInt(6, volume.getCornerTwo().getBlockZ());
		cornerStmt.executeUpdate();
		cornerStmt.close();
		PreparedStatement dataStmt = databaseConnection
				.prepareStatement("INSERT INTO blocks VALUES (?, ?, ?, ?, ?)");
		databaseConnection.setAutoCommit(false);
		final int batchSize = 1000;
		int changed = 0;
		for (BlockState block : volume.getBlocks()) {
			final Location relLoc = ZoneVolumeMapper.rebase(
					volume.getCornerOne(), block.getLocation());
			dataStmt.setInt(1, relLoc.getBlockX());
			dataStmt.setInt(2, relLoc.getBlockY());
			dataStmt.setInt(3, relLoc.getBlockZ());
			dataStmt.setString(4, block.getType().toString());
			dataStmt.setShort(5, block.getData().toItemStack(1).getDurability());
			dataStmt.addBatch();
			if (++changed % batchSize == 0) {
				dataStmt.executeBatch();
			}
		}
		dataStmt.executeBatch(); // insert remaining records
		databaseConnection.commit();
		dataStmt.close();
		databaseConnection.close();
	}

	public static void delete(Volume volume) {
		File volFile = new File(War.war.getDataFolder(), String.format(
				"/dat/volume-%s.sl3", volume.getName()));
		boolean deletedData = volFile.delete();
		if (!deletedData) {
			War.war.log("Failed to delete file " + volFile.getName(), Level.WARNING);
		}
	}

}
