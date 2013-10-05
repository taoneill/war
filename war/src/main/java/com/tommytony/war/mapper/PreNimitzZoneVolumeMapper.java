package com.tommytony.war.mapper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.Dispenser;
import org.bukkit.block.Sign;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;


import com.tommytony.war.War;
import com.tommytony.war.job.DeferredBlockResetsJob;
import com.tommytony.war.job.ZoneVolumeSaveJob;
import com.tommytony.war.utility.DeferredBlockReset;
import com.tommytony.war.volume.Volume;
import com.tommytony.war.volume.ZoneVolume;

/**
 * The ZoneVolumeMapper take the blocks from disk and sets them in the worlds, since the ZoneVolume doesn't hold its blocks in memory like regular Volumes.
 *
 * @author tommytony, Tim Düsterhus
 * @package com.tommytony.war.mappers
 */
@SuppressWarnings("deprecation")
public class PreNimitzZoneVolumeMapper {

	/**
	 * Loads the given volume
	 *
	 * @param ZoneVolume
	 *                volume Volume to load
	 * @param String
	 *                zoneName Zone to load the volume from
	 * @param World
	 *                world The world the zone is located
	 * @param boolean onlyLoadCorners Should only the corners be loaded
	 * @return integer Changed blocks
	 */
	public static int load(ZoneVolume volume, String zoneName, World world, boolean onlyLoadCorners) {
		File cornersFile = new File(War.war.getDataFolder().getPath() + "/dat/warzone-" + zoneName + "/volume-" + volume.getName() + ".corners");
		File blocksFile = new File(War.war.getDataFolder().getPath() + "/dat/warzone-" + zoneName + "/volume-" + volume.getName() + ".blocks");
		File signsFile = new File(War.war.getDataFolder().getPath() + "/dat/warzone-" + zoneName + "/volume-" + volume.getName() + ".signs");
		File invsFile = new File(War.war.getDataFolder().getPath() + "/dat/warzone-" + zoneName + "/volume-" + volume.getName() + ".invs");
		int noOfResetBlocks = 0;
		boolean failed = false;
		if (!blocksFile.exists()) {
			// The post 1.6 formatted files haven't been created yet so
			// we need to use the old load.
			noOfResetBlocks = PreDeGaulleZoneVolumeMapper.load(volume, zoneName, world, onlyLoadCorners);

			// The new 1.6 files aren't created yet. We just reset the zone (except deferred blocks which will soon execute on main thread ),
			// so let's save to the new format as soon as the zone is fully reset.
			PreNimitzZoneVolumeMapper.saveAsJob(volume, zoneName, 2);
			War.war.log("Warzone " + zoneName + " file converted!", Level.INFO);

			return noOfResetBlocks;
		} else {
			// 1.6 file exist, so go ahead with reset
			BufferedReader cornersReader = null;
			FileInputStream blocksStream = null;
			BufferedReader signsReader = null;
			BufferedReader invsReader = null;
			try {
				cornersReader = new BufferedReader(new FileReader(cornersFile));
				blocksStream = new FileInputStream(blocksFile);
				signsReader = new BufferedReader(new FileReader(signsFile));
				invsReader = new BufferedReader(new FileReader(invsFile));

				// Get the corners
				cornersReader.readLine();
				int x1 = Integer.parseInt(cornersReader.readLine());
				int y1 = Integer.parseInt(cornersReader.readLine());
				int z1 = Integer.parseInt(cornersReader.readLine());
				cornersReader.readLine();
				int x2 = Integer.parseInt(cornersReader.readLine());
				int y2 = Integer.parseInt(cornersReader.readLine());
				int z2 = Integer.parseInt(cornersReader.readLine());

				volume.setCornerOne(world.getBlockAt(x1, y1, z1));
				volume.setCornerTwo(world.getBlockAt(x2, y2, z2));

				// Allocate block byte arrays
				int noOfBlocks = volume.getSizeX() * volume.getSizeY() * volume.getSizeZ();
				byte[] blockBytes = new byte[noOfBlocks * 2]; // one byte for type, one for data

				blocksStream.read(blockBytes); // read it all

				// Now use the block bytes to reset the world blocks
				if (!onlyLoadCorners) {
					DeferredBlockResetsJob deferred = new DeferredBlockResetsJob(world);
					int blockReads = 0, visitedBlocks = 0, x = 0, y = 0, z = 0, i = 0, j = 0, k = 0;
					int diskBlockType;
					byte diskBlockData;
					Block worldBlock;
					int worldBlockId;
					volume.clearBlocksThatDontFloat();
					x = volume.getMinX();
					for (i = 0; i < volume.getSizeX(); i++) {
						y = volume.getMinY();
						for (j = 0; j < volume.getSizeY(); j++) {
							z = volume.getMinZ();
							for (k = 0; k < volume.getSizeZ(); k++) {
								try {
									diskBlockType = blockBytes[visitedBlocks * 2];
									diskBlockData = blockBytes[visitedBlocks * 2 + 1];

									worldBlock = volume.getWorld().getBlockAt(x, y, z);
									worldBlockId = worldBlock.getTypeId();
									if (worldBlockId != diskBlockType || (worldBlockId == diskBlockType && worldBlock.getData() != diskBlockData) || (worldBlockId == diskBlockType && worldBlock.getData() == diskBlockData && (diskBlockType == Material.WALL_SIGN.getId() || diskBlockType == Material.SIGN_POST.getId() || diskBlockType == Material.CHEST.getId() || diskBlockType == Material.DISPENSER.getId()))) {
										if (diskBlockType == Material.WALL_SIGN.getId() || diskBlockType == Material.SIGN_POST.getId()) {
											// Signs read
											String linesStr = signsReader.readLine();
											String[] lines = linesStr.split(";;");

											// Signs set
											if (diskBlockType == Material.WALL_SIGN.getId() && ((diskBlockData & 0x04) == 0x04) && i + 1 != volume.getSizeX()) {
												// A sign post hanging on a wall south of here needs that block to be set first
												deferred.add(new DeferredBlockReset(x, y, z, diskBlockType, diskBlockData, lines));
											} else {
												worldBlock.setType(Material.getMaterial(diskBlockType));
												BlockState state = worldBlock.getState();
												state.setData(new org.bukkit.material.Sign(diskBlockType, diskBlockData));
												if (state instanceof Sign) {
													Sign sign = (Sign) state;
													if (lines != null && sign.getLines() != null) {
														if (lines.length > 0) {
															sign.setLine(0, lines[0]);
														}
														if (lines.length > 1) {
															sign.setLine(1, lines[1]);
														}
														if (lines.length > 2) {
															sign.setLine(2, lines[2]);
														}
														if (lines.length > 3) {
															sign.setLine(3, lines[3]);
														}
														sign.update(true);
													}
												}
											}
										} else if (diskBlockType == Material.CHEST.getId()) {
											// Chests read
											List<ItemStack> items = VolumeMapper.readInventoryString(invsReader.readLine());

											// Chests set
											worldBlock.setType(Material.getMaterial(diskBlockType));
											worldBlock.setData(diskBlockData);
											BlockState state = worldBlock.getState();
											if (state instanceof Chest) {
												Chest chest = (Chest) state;
												if (items != null) {
													int ii = 0;
													chest.getInventory().clear();
													for (ItemStack item : items) {
														if (item != null) {
															chest.getInventory().setItem(ii, item);
															ii++;
														}
													}
													chest.update(true);
												}
											}
										} else if (diskBlockType == Material.DISPENSER.getId()) {
											// Dispensers read
											List<ItemStack> items = VolumeMapper.readInventoryString(invsReader.readLine());

											// Dispensers set
											worldBlock.setType(Material.getMaterial(diskBlockType));
											worldBlock.setData(diskBlockData);
											BlockState state = worldBlock.getState();
											if (state instanceof Dispenser) {
												Dispenser dispenser = (Dispenser) state;
												if (items != null) {
													int ii = 0;
													dispenser.getInventory().clear();
													for (ItemStack item : items) {
														if (item != null) {
															dispenser.getInventory().setItem(ii, item);
															ii++;
														}
													}
													dispenser.update(true);
												}
											}
										} else if (diskBlockType == Material.WOODEN_DOOR.getId() || diskBlockType == Material.IRON_DOOR_BLOCK.getId()) {
											// Door blocks
											deferred.add(new DeferredBlockReset(x, y, z, diskBlockType, diskBlockData));
										} else if (((diskBlockType == Material.TORCH.getId() && ((diskBlockData & 0x02) == 0x02)) || (diskBlockType == Material.REDSTONE_TORCH_OFF.getId() && ((diskBlockData & 0x02) == 0x02)) || (diskBlockType == Material.REDSTONE_TORCH_ON.getId() && ((diskBlockData & 0x02) == 0x02)) || (diskBlockType == Material.LEVER.getId() && ((diskBlockData & 0x02) == 0x02)) || (diskBlockType == Material.STONE_BUTTON.getId() && ((diskBlockData & 0x02) == 0x02)) || (diskBlockType == Material.LADDER.getId() && ((diskBlockData & 0x04) == 0x04)) || (diskBlockType == Material.RAILS.getId() && ((diskBlockData & 0x02) == 0x02))) && i + 1 != volume.getSizeX()) {
											// Blocks that hang on a block south of themselves need to make sure that block is there before placing themselves... lol
											// Change the block itself later on:
											deferred.add(new DeferredBlockReset(x, y, z, diskBlockType, diskBlockData));
										} else {
											// regular block
											if (diskBlockType >= 0) {
												worldBlock.setType(Material.getMaterial(diskBlockType));
												worldBlock.setData(diskBlockData);
											} else {
												// The larger than 127 block types were stored as bytes, 
												// but now -128 to -1 are the result of the bad cast from byte 
												// to int array above. To make matters worse let's make this
												// quick a dirty patch. Anyway everything will break horribly 
												// once block ids get higher than 255.
												worldBlock.setType(Material.getMaterial(256 + diskBlockType));
												worldBlock.setData(diskBlockData);
											}
										}
										noOfResetBlocks++;
									}
									visitedBlocks++;

									blockReads++;

								} catch (Exception e) {
									if (!failed) {
										// Don't spam the console
										War.war.getLogger().warning("Failed to reset block in zone volume " + volume.getName() + ". " + "Blocks read: " + blockReads + ". Visited blocks so far:" + visitedBlocks + ". Blocks reset: " + noOfResetBlocks + ". Error at x:" + x + " y:" + y + " z:" + z + ". Exception:" + e.getClass().toString() + " " + e.getMessage());
										e.printStackTrace();
										failed = true;
									}
								} finally {
									z++;
								}
							}
							y++;
						}
						x++;
					}
					if (!deferred.isEmpty()) {
						War.war.getServer().getScheduler().scheduleSyncDelayedTask(War.war, deferred, 2);
					}
				}
			} catch (FileNotFoundException e) {
				War.war.log("Failed to find volume file " + volume.getName() + " for warzone " + zoneName + ". " + e.getClass().getName() + " " + e.getMessage(), Level.WARNING);
				e.printStackTrace();
			} catch (IOException e) {
				War.war.log("Failed to read volume file " + volume.getName() + " for warzone " + zoneName + ". " + e.getClass().getName() + " " + e.getMessage(), Level.WARNING);
				e.printStackTrace();
			} finally {
				try {
					if (cornersReader != null) {
						cornersReader.close();
					}
					if (blocksStream != null) {
						blocksStream.close();
					}
					if (signsReader != null) {
						signsReader.close();
					}
					if (invsReader != null) {
						invsReader.close();
					}
				} catch (IOException e) {
					War.war.log("Failed to close volume file " + volume.getName() + " for warzone " + zoneName + ". " + e.getClass().getName() + " " + e.getMessage(), Level.WARNING);
					e.printStackTrace();
				}
			}
			return noOfResetBlocks;
		}
	}

	/**
	 * Saves the given volume
	 *
	 * @param Volume
	 *                volume Volume to save
	 * @param String
	 *                zoneName The warzone the volume is located
	 * @return integer Number of written blocks
	 */
	public static int save(Volume volume, String zoneName) {
		int noOfSavedBlocks = 0;
		if (volume.hasTwoCorners()) {
			BufferedWriter cornersWriter = null;
			FileOutputStream blocksOutput = null;
			BufferedWriter signsWriter = null;
			BufferedWriter invsWriter = null;
			try {
				(new File(War.war.getDataFolder().getPath() + "/dat/warzone-" + zoneName)).mkdir();
				String path = War.war.getDataFolder().getPath() + "/dat/warzone-" + zoneName + "/volume-" + volume.getName();
				cornersWriter = new BufferedWriter(new FileWriter(new File(path + ".corners")));
				blocksOutput = new FileOutputStream(new File(path + ".blocks"));
				signsWriter = new BufferedWriter(new FileWriter(new File(path + ".signs")));
				invsWriter = new BufferedWriter(new FileWriter(new File(path + ".invs")));

				cornersWriter.write("corner1");
				cornersWriter.newLine();
				cornersWriter.write(Integer.toString(volume.getCornerOne().getBlockX()));
				cornersWriter.newLine();
				cornersWriter.write(Integer.toString(volume.getCornerOne().getBlockY()));
				cornersWriter.newLine();
				cornersWriter.write(Integer.toString(volume.getCornerOne().getBlockZ()));
				cornersWriter.newLine();
				cornersWriter.write("corner2");
				cornersWriter.newLine();
				cornersWriter.write(Integer.toString(volume.getCornerTwo().getBlockX()));
				cornersWriter.newLine();
				cornersWriter.write(Integer.toString(volume.getCornerTwo().getBlockY()));
				cornersWriter.newLine();
				cornersWriter.write(Integer.toString(volume.getCornerTwo().getBlockZ()));
				cornersWriter.newLine();

				int x = 0;
				int y = 0;
				int z = 0;
				Block block;
				int typeId;
				byte data;
				BlockState state;

				x = volume.getMinX();
				for (int i = 0; i < volume.getSizeX(); i++) {
					y = volume.getMinY();
					for (int j = 0; j < volume.getSizeY(); j++) {
						z = volume.getMinZ();
						for (int k = 0; k < volume.getSizeZ(); k++) {
							try {
								block = volume.getWorld().getBlockAt(x, y, z);
								typeId = block.getTypeId();
								data = block.getData();
								state = block.getState();

								blocksOutput.write((byte) typeId);
								blocksOutput.write(data);

								if (state instanceof Sign) {
									// Signs
									String extra = "";
									Sign sign = (Sign) state;
									if (sign.getLines() != null) {
										for (String line : sign.getLines()) {
											extra += line + ";;";
										}
										signsWriter.write(extra);
										signsWriter.newLine();
									}
								} else if (state instanceof Chest) {
									// Chests
									Chest chest = (Chest) state;
									Inventory inv = chest.getInventory();
									List<ItemStack> items = VolumeMapper.getItemListFromInv(inv);
									invsWriter.write(VolumeMapper.buildInventoryStringFromItemList(items));
									invsWriter.newLine();
								} else if (state instanceof Dispenser) {
									// Dispensers
									Dispenser dispenser = (Dispenser) state;
									Inventory inv = dispenser.getInventory();
									List<ItemStack> items = VolumeMapper.getItemListFromInv(inv);
									invsWriter.write(VolumeMapper.buildInventoryStringFromItemList(items));
									invsWriter.newLine();
								}
								noOfSavedBlocks++;
							} catch (Exception e) {
								War.war.log("Unexpected error while saving a block to " + " file for zone " + zoneName + ". Blocks saved so far: " + noOfSavedBlocks + "Position: x:" + x + " y:" + y + " z:" + z + ". " + e.getClass().getName() + " " + e.getMessage(), Level.WARNING);
								e.printStackTrace();
							} finally {
								z++;
							}
						}
						y++;
					}
					x++;
				}
			} catch (IOException e) {
				War.war.log("Failed to write volume file " + zoneName + " for warzone " + volume.getName() + ". " + e.getClass().getName() + " " + e.getMessage(), Level.WARNING);
				e.printStackTrace();
			} catch (Exception e) {
				War.war.log("Unexpected error caused failure to write volume file " + zoneName + " for warzone " + volume.getName() + ". " + e.getClass().getName() + " " + e.getMessage(), Level.WARNING);
				e.printStackTrace();
			} finally {
				try {
					if (cornersWriter != null) {
						cornersWriter.close();
					}
					if (blocksOutput != null) {
						blocksOutput.close();
					}
					if (signsWriter != null) {
						signsWriter.close();
					}
					if (invsWriter != null) {
						invsWriter.close();
					}
				} catch (IOException e) {
					War.war.log("Failed to close volume file " + volume.getName() + " for warzone " + zoneName + ". " + e.getClass().getName() + " " + e.getMessage(), Level.WARNING);
					e.printStackTrace();
				}
			}
		}
		return noOfSavedBlocks;
	}
	


	/**
	 * Saves the Volume as a background-job
	 *
	 * @param ZoneVolme
	 *                volume volume to save
	 * @param String
	 *                zoneName The zone the volume is located
	 * @param War
	 *                war Instance of war
	 * @param long tickDelay delay before beginning the task
	 */
	private static void saveAsJob(ZoneVolume volume, String zoneName, long tickDelay) {
		ZoneVolumeSaveJob job = new ZoneVolumeSaveJob(volume, zoneName);
		War.war.getServer().getScheduler().scheduleSyncDelayedTask(War.war, job, tickDelay);
	}

	/**
	 * Deletes the given volume
	 *
	 * @param Volume
	 *                volume volume to delete
	 * @param War
	 *                war Instance of war
	 */
	public static void delete(Volume volume) {
		PreNimitzZoneVolumeMapper.deleteFile(War.war.getDataFolder().getPath() + "/dat/volume-" + volume.getName() + ".dat");
		PreNimitzZoneVolumeMapper.deleteFile(War.war.getDataFolder().getPath() + "/dat/volume-" + volume.getName() + ".corners");
		PreNimitzZoneVolumeMapper.deleteFile(War.war.getDataFolder().getPath() + "/dat/volume-" + volume.getName() + ".blocks");
		PreNimitzZoneVolumeMapper.deleteFile(War.war.getDataFolder().getPath() + "/dat/volume-" + volume.getName() + ".signs");
		PreNimitzZoneVolumeMapper.deleteFile(War.war.getDataFolder().getPath() + "/dat/volume-" + volume.getName() + ".invs");
	}

	/**
	 * Deletes a volume file
	 *
	 * @param String
	 *                path path of file
	 * @param War
	 *                war Instance of war
	 */
	private static void deleteFile(String path) {
		File volFile = new File(path);
		if (volFile.exists()) {
			boolean deletedData = volFile.delete();
			if (!deletedData) {
				War.war.log("Failed to delete file " + volFile.getName(), Level.WARNING);
			}
		}
	}
}
