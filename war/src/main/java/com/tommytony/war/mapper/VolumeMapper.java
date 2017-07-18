package com.tommytony.war.mapper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;

import com.tommytony.war.War;
import com.tommytony.war.volume.Volume;

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
			legacyLoad(volume, zoneName, world);
			save(volume, zoneName);
			War.war.getLogger().info("Volume " + volume.getName() + " for warzone " + zoneName + " converted to nimitz format!");
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

	@SuppressWarnings("deprecation")
	public static void legacyLoad(Volume volume, String zoneName, World world) {
		BufferedReader in = null;
		try {
			if (zoneName.equals("")) {
				in = new BufferedReader(new FileReader(new File(War.war.getDataFolder().getPath() + "/dat/volume-" + volume.getName() + ".dat"))); // for the warhub
			} else {
				in = new BufferedReader(new FileReader(new File(War.war.getDataFolder().getPath() + "/dat/warzone-" + zoneName + "/volume-" + volume.getName() + ".dat")));
			}
			String firstLine = in.readLine();
			if (firstLine != null && !firstLine.equals("")) {
				boolean height129Fix = false;
				int x1 = Integer.parseInt(in.readLine());
				int y1 = Integer.parseInt(in.readLine());
				if (y1 == 128) {
					height129Fix = true;
					y1 = 127;
				}
				int z1 = Integer.parseInt(in.readLine());
				in.readLine();
				int x2 = Integer.parseInt(in.readLine());
				int y2 = Integer.parseInt(in.readLine());
				if (y2 == 128) {
					height129Fix = true;
					y2 = 127;
				}
				int z2 = Integer.parseInt(in.readLine());

				volume.setCornerOne(world.getBlockAt(x1, y1, z1));
				volume.setCornerTwo(world.getBlockAt(x2, y2, z2));

				int blockReads = 0;
				for (int i = 0; i < volume.getSizeX(); i++) {
					for (int j = 0; j < volume.getSizeY(); j++) {
						for (int k = 0; k < volume.getSizeZ(); k++) {
							try {
								String blockLine = in.readLine();
								if (blockLine != null && !blockLine.equals("")) {
									String[] blockSplit = blockLine.split(",");
									if (blockLine != null && !blockLine.equals("") && blockSplit.length > 1) {
										int typeID = Integer.parseInt(blockSplit[0]);
										byte data = Byte.parseByte(blockSplit[1]);

										BlockState dummy = volume.getWorld().getBlockAt(x1 + i, y1 + j, z1 + k).getState();
										dummy.setTypeId(typeID);
										dummy.setRawData(data);
										volume.getBlocks().add(dummy);
									}
									blockReads++;
								}
							} catch (Exception e) {
								War.war.log("Unexpected error while reading block from volume " + volume.getName() + " file for zone " + zoneName + ". Blocks read so far: " + blockReads + "Position: x:" + i + " y:" + j + " z:" + k + ". " + e.getClass().getName() + " " + e.getMessage(), Level.WARNING);
								e.printStackTrace();
							}
						}
						if (height129Fix && j == volume.getSizeY() - 1) {
							for (int skip = 0; skip < volume.getSizeZ(); skip++) {
								in.readLine(); // throw away the extra vertical block I used to save pre 0.8
							}
						}
					}
				}
			}
		} catch (IOException e) {
			War.war.log("Failed to read volume file " + volume.getName() + " for warzone " + zoneName + ". " + e.getClass().getName() + " " + e.getMessage(), Level.WARNING);
			e.printStackTrace();
		} catch (Exception e) {
			War.war.log("Unexpected error caused failure to read volume file " + zoneName + " for warzone " + volume.getName() + ". " + e.getClass().getName() + " " + e.getMessage(), Level.WARNING);
			e.printStackTrace();
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					War.war.log("Failed to close file reader for volume " + volume.getName() + " for warzone " + zoneName + ". " + e.getClass().getName() + " " + e.getMessage(), Level.WARNING);
					e.printStackTrace();
				}
			}
		}
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
	
	/**
	 * Parses an inventory string
	 *
	 * @param String
	 *                invString string to parse
	 * @return List<ItemStack> Parsed items
	 */
	@Deprecated
	public static List<ItemStack> readInventoryString(String invString) {
		List<ItemStack> items = new ArrayList<ItemStack>();
		if (invString != null && !invString.equals("")) {
			String[] itemsStrSplit = invString.split(";;");
			for (String itemStr : itemsStrSplit) {
				String[] itemStrSplit = itemStr.split(";");
				if (itemStrSplit.length == 5) {
					ItemStack stack = new ItemStack(Integer.parseInt(itemStrSplit[0]), Integer.parseInt(itemStrSplit[1]));
					stack.setData(new MaterialData(stack.getTypeId(), Byte.parseByte(itemStrSplit[3])));
					short durability = (short) Integer.parseInt(itemStrSplit[2]);
					stack.setDurability(durability);

					// enchantments
					String[] enchantmentsSplit = itemStrSplit[4].split("::");
					for (String enchantmentStr : enchantmentsSplit) {
						if (!enchantmentStr.equals("")) {
							String[] enchantmentSplit = enchantmentStr.split(":");
							int enchantId = Integer.parseInt(enchantmentSplit[0]);
							int level = Integer.parseInt(enchantmentSplit[1]);
							War.war.safelyEnchant(stack, Enchantment.getById(enchantId), level);
						}
					}
					
					items.add(stack);
				} else if (itemStrSplit.length == 4) {
					ItemStack stack = new ItemStack(Integer.parseInt(itemStrSplit[0]), Integer.parseInt(itemStrSplit[1]));
					stack.setData(new MaterialData(stack.getTypeId(), Byte.parseByte(itemStrSplit[3])));
					short durability = (short) Integer.parseInt(itemStrSplit[2]);
					stack.setDurability(durability);
					items.add(stack);
				} else if (itemStrSplit.length == 3) {
					ItemStack stack = new ItemStack(Integer.parseInt(itemStrSplit[0]), Integer.parseInt(itemStrSplit[1]));
					short durability = (short) Integer.parseInt(itemStrSplit[2]);
					stack.setDurability(durability);
					items.add(stack);
				} else {
					items.add(new ItemStack(Integer.parseInt(itemStrSplit[0]), Integer.parseInt(itemStrSplit[1])));
				}
			}
		}
		return items;
	}

	/**
	 * Create a string out of a list of items
	 * 
	 * @param items	The list of items
	 * @return		The list as a string
	 */
	@Deprecated
	public static String buildInventoryStringFromItemList(List<ItemStack> items) {
		String extra = "";
		for (ItemStack item : items) {
			if (item != null) {
				extra += item.getTypeId() + ";" + item.getAmount() + ";" + item.getDurability();
				if (item.getData() != null) {
					extra += ";" + item.getData().getData();
				}
				if (item.getEnchantments().keySet().size() > 0) {
					String enchantmentsStr = "";
					for (Enchantment enchantment : item.getEnchantments().keySet()) {
						enchantmentsStr += enchantment.getId() + ":" + item.getEnchantments().get(enchantment) + "::";
					}
					extra += ";" + enchantmentsStr;
				}
				extra += ";;";
			}
		}
		
		return extra;
	}
	
	/**
	 * Extracts a list of items from and inventory
	 * 
	 * @param inv	The inventory
	 * @return		The inventory as a list
	 */
	@Deprecated
	public static List<ItemStack> getItemListFromInv(Inventory inv) {
		int size = inv.getSize();
		List<ItemStack> items = new ArrayList<ItemStack>();
		for (int invIndex = 0; invIndex < size; invIndex++) {
			ItemStack item = inv.getItem(invIndex);
			if (item != null && item.getType().getId() != Material.AIR.getId()) {
				items.add(item);
			}
		}
		return items;
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
