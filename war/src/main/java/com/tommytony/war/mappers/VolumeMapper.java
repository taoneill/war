package com.tommytony.war.mappers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Chest;
import org.bukkit.block.Dispenser;
import org.bukkit.block.Sign;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;

import bukkit.tommytony.war.War;

import com.tommytony.war.volumes.BlockInfo;
import com.tommytony.war.volumes.VerticalVolume;
import com.tommytony.war.volumes.Volume;

/**
 * 
 * @author tommytony
 *
 */
public class VolumeMapper {
	
	public static Volume loadVolume(String volumeName, String zoneName,
			War war, World world) {
		Volume volume = new Volume(volumeName, war, world);
		load(volume, zoneName, war, world);
		return volume;
	}

	public static VerticalVolume loadVerticalVolume(String volumeName, String zoneName,
			War war, World world) {
		VerticalVolume volume = new VerticalVolume(volumeName, war, world);
		load(volume, zoneName, war, world);
		return volume;
	}
	
	public static void load(Volume volume, String zoneName, War war, World world) {
		BufferedReader in = null;
		try {
			if(zoneName.equals("")) in = new BufferedReader(new FileReader(new File(war.getDataFolder().getPath() + 
												"/dat/volume-" + volume.getName() + ".dat"))); // for the warhub
			else in = new BufferedReader(new FileReader(new File(war.getDataFolder().getPath() + "/dat/warzone-" + zoneName + "/volume-" + volume.getName() + ".dat")));
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
				
				volume.setBlockTypes(new int[volume.getSizeX()][volume.getSizeY()][volume.getSizeZ()]);
				volume.setBlockDatas(new byte[volume.getSizeX()][volume.getSizeY()][volume.getSizeZ()]);
				for(int i = 0; i < volume.getSizeX(); i++){
					for(int j = 0; j < volume.getSizeY(); j++) {
						for(int k = 0; k < volume.getSizeZ(); k++) {
							String blockLine = in.readLine();
							if(blockLine != null && !blockLine.equals("")) {
								String[] blockSplit = blockLine.split(",");
								if(blockLine != null && !blockLine.equals("") && blockSplit.length > 1) {
									int typeID = Integer.parseInt(blockSplit[0]);
									byte data = Byte.parseByte(blockSplit[1]);
									
									volume.getBlockTypes()[i][j][k] = typeID;
									volume.getBlockDatas()[i][j][k] = data;
									
									if(typeID == Material.SIGN.getId() 
											|| typeID == Material.SIGN_POST.getId()) {
										// Signs
										String linesStr = "";
										if(blockSplit.length > 2) {
											for(int o = 2; o < blockSplit.length; o++) {
												linesStr += blockSplit[o];
											}
											String[] lines = linesStr.split(";;");
											volume.getSignLines().put("sign-" + i + "-" + j + "-" + k, lines);
										}
									} else if(typeID == Material.CHEST.getId()) {
										// Chests
										List<ItemStack> items = new ArrayList<ItemStack>();
										if(blockSplit.length > 2) {
											String itemsStr = blockSplit[2];
											String[] itemsStrSplit = itemsStr.split(";;");
											for(String itemStr : itemsStrSplit) {
												String[] itemStrSplit = itemStr.split(";");
												items.add(new ItemStack(Integer.parseInt(itemStrSplit[0]),
														Integer.parseInt(itemStrSplit[1])));
											}
										}
										volume.getInvBlockContents().put("chest-" + i + "-" + j + "-" + k, items);
									} else if(typeID == Material.DISPENSER.getId()) {
										// Dispensers
										List<ItemStack> items = new ArrayList<ItemStack>();
										if(blockSplit.length > 2) {
											String itemsStr = blockSplit[2];
											String[] itemsStrSplit = itemsStr.split(";;");
											for(String itemStr : itemsStrSplit) {
												String[] itemStrSplit = itemStr.split(";");
												items.add(new ItemStack(Integer.parseInt(itemStrSplit[0]),
														Integer.parseInt(itemStrSplit[1])));
											}
										}
										volume.getInvBlockContents().put("dispenser-" + i + "-" + j + "-" + k, items);
										
									} 
								}
							}
						}
						if(height129Fix && j == volume.getSizeY() - 1) {
							for(int skip = 0; skip < volume.getSizeZ(); skip++) {
								in.readLine();	// throw away the extra vertical block I used to save pre 0.8
							}
						}
					}
				}
			}
		} catch (IOException e) {
			war.logWarn("Failed to read volume file " + volume.getName() + 
					" for warzone " + zoneName + ". " + e.getClass().getName() + " " + e.getMessage());
			e.printStackTrace();
		} finally {
			if(in != null)
				try {
					in.close();
				} catch (IOException e) {
					war.logWarn("Failed to close file reader for volume " + volume.getName() +
							" for warzone " + zoneName + ". " + e.getClass().getName() + " " + e.getMessage());
					e.printStackTrace();
				}
		}
	}
	
	public static void save(Volume volume, String zoneName, War war) {
		if(volume.isSaved() && volume.getBlockTypes() != null) {
			BufferedWriter out = null;
			try {
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
				
				for(int i = 0; i < volume.getSizeX(); i++){
					for(int j = 0; j < volume.getSizeY(); j++) {
						for(int k = 0; k < volume.getSizeZ(); k++) {
							int typeId = volume.getBlockTypes()[i][j][k];
							byte data = volume.getBlockDatas()[i][j][k];
							out.write(typeId + "," + data + ",");
							if(typeId == Material.SIGN.getId() 
									|| typeId == Material.SIGN_POST.getId()) {
								// Signs
								String extra = "";
								String[] lines = volume.getSignLines().get("sign-" + i + "-" + j + "-" + k);
								if(lines != null) {
									for(String line : lines) {
										extra += line + ";;";
									}
									out.write(extra);
								}
							} else if(typeId == Material.CHEST.getId()) {
								// Chests
								String extra = "";
								List<ItemStack> contents = volume.getInvBlockContents().get("chest-" + i + "-" + j + "-" + k);
								if(contents != null) {
									for(ItemStack item : contents) {
										extra += item.getTypeId() + ";" + item.getAmount() + ";;";
									}
									out.write(extra);
								}
							} else if(typeId == Material.DISPENSER.getId()) {
								// Dispensers
								String extra = "";
								List<ItemStack> contents = volume.getInvBlockContents().get("dispenser-" + i + "-" + j + "-" + k);
								if(contents != null) {
									for(ItemStack item : contents) {
										extra += item.getTypeId() + ";" + item.getAmount() + ";;";
									}
									out.write(extra);
								}
							}
							out.newLine();
						}
					}
				}
			} catch (IOException e) {
				war.logWarn("Failed to write volume file " + zoneName + 
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
	}

	public static void delete(Volume volume, War war) {
		File volFile= new File("War/dat/volume-" + volume.getName());
		boolean deletedData = volFile.delete();
		if(!deletedData) {
			war.logWarn("Failed to delete file " + volFile.getName());
		}
	}


}
