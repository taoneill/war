package com.tommytony.war.mappers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.Dispenser;
import org.bukkit.block.Sign;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;

import bukkit.tommytony.war.War;

import com.tommytony.war.jobs.DeferredBlockResetsJob;
import com.tommytony.war.jobs.ZoneVolumeSaveJob;
import com.tommytony.war.utils.DeferredBlockReset;
import com.tommytony.war.volumes.Volume;
import com.tommytony.war.volumes.ZoneVolume;

/**
 * The ZoneVolumeMapper take the blocks from disk and sets them in the worlds, since
 * the ZoneVolume doesn't hold its blocks in memory like regular Volumes.
 * 
 * @author tommytony
 *
 */
public class PreDeGaulleZoneVolumeMapper {
	
	private static List<ItemStack> readInventoryString(String invString) {
		List<ItemStack> items = new ArrayList<ItemStack>();
		String[] itemsStrSplit = invString.split(";;");
		for(String itemStr : itemsStrSplit) {
			String[] itemStrSplit = itemStr.split(";");
			if(itemStrSplit.length == 4) {
				ItemStack stack = new ItemStack(Integer.parseInt(itemStrSplit[0]),
						Integer.parseInt(itemStrSplit[1]));
				stack.setData(new MaterialData(stack.getTypeId(),Byte.parseByte(itemStrSplit[3])));
				short durability = (short)Integer.parseInt(itemStrSplit[2]);
				stack.setDurability(durability);
				items.add(stack);
			} else if(itemStrSplit.length == 3) {
				ItemStack stack = new ItemStack(Integer.parseInt(itemStrSplit[0]),
						Integer.parseInt(itemStrSplit[1]));
				short durability = (short)Integer.parseInt(itemStrSplit[2]);
				stack.setDurability(durability);
				items.add(stack);
			} else {
				items.add(new ItemStack(Integer.parseInt(itemStrSplit[0]),
						Integer.parseInt(itemStrSplit[1])));
			}
		}
		return items;
	}

	public static int load(ZoneVolume volume, String zoneName, War war,
			World world, boolean onlyLoadCorners) {
		BufferedReader in = null;
		int noOfResetBlocks = 0;
		try {
			in = new BufferedReader(new FileReader(new File(war.getDataFolder().getPath() + "/dat/warzone-" + zoneName + "/volume-" + volume.getName() + ".dat")));
			String firstLine = in.readLine();
			
			if(firstLine != null && !firstLine.equals("")) {
				boolean height129Fix = false;
				int x1 = Integer.parseInt(in.readLine());
				int y1 = Integer.parseInt(in.readLine());
				if(y1 == 128) {
					height129Fix = true;
					y1 = 127;
				}
				int z1 = Integer.parseInt(in.readLine());
				in.readLine();
				int x2 = Integer.parseInt(in.readLine());
				int y2 = Integer.parseInt(in.readLine());
				if(y2 == 128) {
					height129Fix = true;
					y2 = 127;
				}
				int z2 = Integer.parseInt(in.readLine());
				
				volume.setCornerOne(world.getBlockAt(x1, y1, z1));
				volume.setCornerTwo(world.getBlockAt(x2, y2, z2));	
				
				if(!onlyLoadCorners) {
					DeferredBlockResetsJob deferred = new DeferredBlockResetsJob(world);
					int blockReads = 0, visitedBlocks = 0, x = 0, y = 0, z = 0, i = 0, j = 0 , k = 0;
					int diskBlockType;
					byte diskBlockData;
					Block worldBlock;
					int worldBlockId;
					volume.clearBlocksThatDontFloat();
					x = volume.getMinX();
					String blockLine;
					String[] blockSplit;
					for(i = 0; i < volume.getSizeX(); i++){
						y = volume.getMinY();					
						for(j = 0; j < volume.getSizeY(); j++) {
							z = volume.getMinZ();
							for(k = 0; k < volume.getSizeZ(); k++) {
								try {
									blockLine = in.readLine();
							
									if(blockLine != null && !blockLine.equals("")) {
										blockSplit = blockLine.split(",");
										if(blockLine != null && !blockLine.equals("") && blockSplit.length > 1) {
											diskBlockType = Integer.parseInt(blockSplit[0]);
											diskBlockData = Byte.parseByte(blockSplit[1]);
											worldBlock = volume.getWorld().getBlockAt(x, y, z);
											worldBlockId = worldBlock.getTypeId();
											if(worldBlockId != diskBlockType ||
												(worldBlockId == diskBlockType && worldBlock.getData() != diskBlockData ) ||
												(worldBlockId == diskBlockType && worldBlock.getData() == diskBlockData &&
														(diskBlockType == Material.WALL_SIGN.getId() || diskBlockType == Material.SIGN_POST.getId() 
																|| diskBlockType == Material.CHEST.getId() || diskBlockType == Material.DISPENSER.getId())
												)
											) {
												if(diskBlockType == Material.WALL_SIGN.getId() 
														|| diskBlockType == Material.SIGN_POST.getId()) {
													// Signs read
													String linesStr = "";
													if(blockSplit.length > 2) {
														for(int o = 2; o < blockSplit.length; o++) {
															linesStr += blockSplit[o];
														}
														String[] lines = linesStr.split(";;");

														// Signs set
														// A sign post hanging on a wall south of here will 
														if(diskBlockType == Material.SIGN_POST.getId() && ((diskBlockData & 0x04) == 0x04)
																&& i+1 != volume.getSizeX()) {
															deferred.add(new DeferredBlockReset(x, y, z, diskBlockType, diskBlockData, lines));
														} else {
															worldBlock.setType(Material.getMaterial(diskBlockType));
															BlockState state = worldBlock.getState();
															state.setData(new org.bukkit.material.Sign(diskBlockType, diskBlockData));
															if(state instanceof Sign) {
																Sign sign = (Sign)state;
																if(lines != null && sign.getLines() != null) {
																	if(lines.length>0)sign.setLine(0, lines[0]);
																	if(lines.length>1)sign.setLine(1, lines[1]);
																	if(lines.length>2)sign.setLine(2, lines[2]);
																	if(lines.length>3)sign.setLine(3, lines[3]);
																	sign.update(true);
																}
															}
														}
													}
												} else if(diskBlockType == Material.CHEST.getId()) {
													// Chests read
													List<ItemStack> items = null;
													if(blockSplit.length > 2) {
														String itemsStr = blockSplit[2];
														items = readInventoryString(itemsStr);
													}
													
													// Chests set
													worldBlock.setType(Material.getMaterial(diskBlockType));
													worldBlock.setData(diskBlockData);
													BlockState state = worldBlock.getState();
													if(state instanceof Chest) {
														Chest chest = (Chest)state;
														if(items != null) {
															int ii = 0;
															chest.getInventory().clear();
															for(ItemStack item : items) {
																if(item != null) {
																	chest.getInventory().setItem(ii, item);
																	ii++;
																}
															}
															chest.update(true);
														}
													}
												} else if(diskBlockType == Material.DISPENSER.getId()) {
													// Dispensers read
													List<ItemStack> items = null;
													if(blockSplit.length > 2) {
														String itemsStr = blockSplit[2];
														//String itemsStr = lineScanner.nextLine();
														items = readInventoryString(itemsStr);
													}
													
													// Dispensers set
													worldBlock.setType(Material.getMaterial(diskBlockType));
													worldBlock.setData(diskBlockData);
													BlockState state = worldBlock.getState();
													if(state instanceof Dispenser) {
														Dispenser dispenser = (Dispenser)state;
														if(items != null) {
															int ii = 0;
															dispenser.getInventory().clear();
															for(ItemStack item : items) {
																if(item != null) {
																	dispenser.getInventory().setItem(ii, item);
																	ii++;
																}
															}
															dispenser.update(true);
														}
													}
												} else if(diskBlockType == Material.WOODEN_DOOR.getId() || diskBlockType == Material.IRON_DOOR_BLOCK.getId()){
													// Door blocks
													
													if(j-1 > 0) {
														Block blockBelow = world.getBlockAt(x, y-1, z);
														boolean belowIsGlass = blockBelow.getTypeId() == Material.GLASS.getId();
														// Set current block to glass if block below isn't glass.
														// Having a glass block below means the current block is a door top.
														if(belowIsGlass) {
															// Top door block. Set both it and the block below as door.
															blockBelow.setType(Material.getMaterial(diskBlockType));
															blockBelow.setData(diskBlockData);
															worldBlock.setType(Material.getMaterial(diskBlockType));
															worldBlock.setData(diskBlockData);
														} else {
															worldBlock.setType(Material.GLASS);
														}
													}
													
												} else if(((diskBlockType == Material.TORCH.getId() && ((diskBlockData & 0x02) == 0x02)) 
														|| (diskBlockType == Material.REDSTONE_TORCH_OFF.getId() && ((diskBlockData & 0x02) == 0x02))
														|| (diskBlockType == Material.REDSTONE_TORCH_ON.getId()  && ((diskBlockData & 0x02) == 0x02))
														|| (diskBlockType == Material.LEVER.getId()  && ((diskBlockData & 0x02) == 0x02))
														|| (diskBlockType == Material.STONE_BUTTON.getId() && ((diskBlockData & 0x02) == 0x02))
														|| (diskBlockType == Material.LADDER.getId() && ((diskBlockData & 0x04) == 0x04))
														|| (diskBlockType == Material.RAILS.getId() && ((diskBlockData & 0x02) == 0x02)))
														&& i+1 != volume.getSizeX()){
													// Blocks that hang on a block south of themselves need to make sure that block is there before placing themselves... lol
													// Change the block itself later on:
													deferred.add(new DeferredBlockReset(x, y, z, diskBlockType, diskBlockData));
												} else {
													// regular block
													worldBlock.setType(Material.getMaterial(diskBlockType));
													worldBlock.setData(diskBlockData);
												}
												noOfResetBlocks++;
											}
											visitedBlocks++;
										}
										blockReads++;
									}
								
								} catch (Exception e) {
									volume.getWar().getLogger().warning("Failed to reset block in zone volume " + volume.getName() + ". " 
											+ "Blocks read: " + blockReads 
											+ ". Visited blocks so far:" + visitedBlocks 
											+ ". Blocks reset: "+ noOfResetBlocks + 
											". Error at x:" + x + " y:" + y + " z:" + z + ". Exception:" + e.getClass().toString() + " " + e.getMessage());
									e.printStackTrace();
								} finally {
									z++;
								}
							}
							if(height129Fix && j == volume.getSizeY() - 1) {
								for(int skip = 0; skip < volume.getSizeZ(); skip++) {
									in.readLine();	// throw away the extra vertical block I used to save pre 0.8
									//scanner.nextLine();
								}
							}
							y++;
						}
						x++;
					}
					if(!deferred.isEmpty()) {
						war.getServer().getScheduler().scheduleSyncDelayedTask(war, deferred, 1);
					}
				}
			}
		} catch (IOException e) {
			war.logWarn("Failed to read volume file " + volume.getName() + 
					" for warzone " + zoneName + ". " + e.getClass().getName() + " " + e.getMessage());
			e.printStackTrace();
		} catch (Exception e) {
			war.logWarn("Unexpected error caused failure to read volume file " + zoneName + 
					" for warzone " + volume.getName() + ". " + e.getClass().getName() + " " + e.getMessage());
			e.printStackTrace();
		}  finally {
			if(in != null)
			//if(scanner != null)
				try {
					in.close();
					in = null;
					//scanner.close();
					//scanner = null;
				} catch (IOException e) {
					war.logWarn("Failed to close file reader for volume " + volume.getName() +
							" for warzone " + zoneName + ". " + e.getClass().getName() + " " + e.getMessage());
					e.printStackTrace();
				}
		}
		return noOfResetBlocks;
	}

	public static int save(Volume volume, String zoneName, War war) {
		int noOfSavedBlocks = 0;
		if(volume.hasTwoCorners()) {
			BufferedWriter out = null;
			try {
				(new File(war.getDataFolder().getPath() +"/dat/warzone-"+zoneName)).mkdir();
				if(zoneName.equals("")) out = new BufferedWriter(new FileWriter(new File(war.getDataFolder().getPath() + "/dat/volume-" + volume.getName() + ".dat")));
				else out = new BufferedWriter(new FileWriter(new File(war.getDataFolder().getPath() + 
												"/dat/warzone-" + zoneName + "/volume-" + volume.getName() + ".dat")));
				
				out.write("corner1"); out.newLine();
				out.write(Integer.toString(volume.getCornerOne().getX())); out.newLine();
				out.write(Integer.toString(volume.getCornerOne().getY())); out.newLine();
				out.write(Integer.toString(volume.getCornerOne().getZ())); out.newLine();
				out.write("corner2"); out.newLine();
				out.write(Integer.toString(volume.getCornerTwo().getX())); out.newLine();
				out.write(Integer.toString(volume.getCornerTwo().getY())); out.newLine();
				out.write(Integer.toString(volume.getCornerTwo().getZ())); out.newLine();
				
				int x = 0;
				int y = 0;
				int z = 0;
				Block block;
				int typeId;
				byte data;
				BlockState state;
				
				x = volume.getMinX();
				for(int i = 0; i < volume.getSizeX(); i++){
					y = volume.getMinY();
					for(int j = 0; j < volume.getSizeY(); j++) {
						z = volume.getMinZ();
						for(int k = 0; k < volume.getSizeZ(); k++) {
							try {
								block = volume.getWorld().getBlockAt(x, y, z);
								typeId = block.getTypeId();
								data = block.getData();
								state = block.getState();
								
								out.write(typeId + "," + data + ",");
								
								if(state instanceof Sign) {
									// Signs
									String extra = "";
									Sign sign = (Sign)state;
									if(sign.getLines() != null) {
										for(String line : sign.getLines()) {
											extra += line + ";;";
										}
										out.write(extra);
									}
									
								} else if(state instanceof Chest) {
									// Chests
									Chest chest = (Chest)state;
									Inventory inv = chest.getInventory();
									int size = inv.getSize();
									List<ItemStack> items = new ArrayList<ItemStack>();
									for(int invIndex = 0; invIndex < size; invIndex++){
										ItemStack item = inv.getItem(invIndex);
										if(item != null && item.getType().getId() != Material.AIR.getId()) {
											items.add(item);
										}
									}
									String extra = "";
									if(items != null) {
										for(ItemStack item : items) {
											if(item != null) {
												extra += item.getTypeId() + ";" 
												+ item.getAmount() + ";" 
												+ item.getDurability(); 
												if(item.getData() != null)
													extra += ";" + item.getData().getData() ;
												extra += ";;";
											}
										}
										out.write(extra);
									}
								} else if(state instanceof Dispenser) {
									// Dispensers							
									Dispenser dispenser = (Dispenser)state;
									Inventory inv = dispenser.getInventory();
									int size = inv.getSize();
									List<ItemStack> items = new ArrayList<ItemStack>();
									for(int invIndex = 0; invIndex < size; invIndex++){
										ItemStack item = inv.getItem(invIndex);
										if(item != null && item.getType().getId() != Material.AIR.getId()) {
											items.add(item);
										}
									}
									String extra = "";
									if(items != null) {
										for(ItemStack item : items) {
											if(item != null) {
												extra += item.getTypeId() + ";" 
												+ item.getAmount() + ";" 
												+ item.getDurability(); 
												if(item.getData() != null)
													extra += ";" + item.getData().getData() ;
												extra += ";;";
											}
										}
										out.write(extra);
									}
								}
								noOfSavedBlocks++;
								out.newLine();
							}
							catch (Exception e) {
								war.logWarn("Unexpected error while saving a block to " +
										" file for zone " + zoneName + ". Blocks saved so far: " + noOfSavedBlocks 
										+ "Position: x:" + x + " y:" + y + " z:" + z + ". " + e.getClass().getName() + " " + e.getMessage());
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
				war.logWarn("Failed to write volume file " + zoneName + 
						" for warzone " + volume.getName() + ". " + e.getClass().getName() + " " + e.getMessage());
				e.printStackTrace();
			} catch (Exception e) {
				war.logWarn("Unexpected error caused failure to write volume file " + zoneName + 
						" for warzone " + volume.getName() + ". " + e.getClass().getName() + " " + e.getMessage());
				e.printStackTrace();
			}  
			finally {
				if(out != null)
					try {
						out.close();
					} catch (IOException e) {
						war.logWarn("Failed to close file writer for volume " + volume.getName() +
								" for warzone " + zoneName + ". " + e.getClass().getName() + " " + e.getMessage());
						e.printStackTrace();
					}	
			}
		}
		return noOfSavedBlocks;
	}


}

