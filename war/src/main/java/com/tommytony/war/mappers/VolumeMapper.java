package com.tommytony.war.mappers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.bukkit.Material;
import org.bukkit.World;

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
							String[] blockSplit = blockLine.split(",");
							if(blockLine != null && !blockLine.equals("")) {
								int typeID = Integer.parseInt(blockSplit[0]);
								byte data = Byte.parseByte(blockSplit[1]);
								String[] lines = null;
//								if(typeID == Material.SIGN.getId() || typeID == Material.SIGN_POST.getId()) {
//									String signLines = blockSplit[2];
//									if(blockSplit.length > 3) {
//										// sign includes commas
//										for(int splitI = 3; splitI < blockSplit.length; splitI++) {
//											signLines.concat(blockSplit[splitI]);
//										}
//									}
//									String[] signLinesSplit = signLines.split("[line]");
//									lines = new String[4];
//									lines[0] = signLinesSplit[0];
//									lines[1] = signLinesSplit[1];
//									lines[2] = signLinesSplit[2];
//									lines[3] = signLinesSplit[3];
//								}
								//volume.getBlockTypes()[i][j][k] = new BlockInfo(typeID, data, lines);
								volume.getBlockTypes()[i][j][k] = typeID;
								volume.getBlockDatas()[i][j][k] = data;
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
			war.warn("Failed to read volume file " + volume.getName() + 
					" for warzone " + zoneName + ". " + e.getClass().getName() + " " + e.getMessage());
			e.printStackTrace();
		} finally {
			if(in != null)
				try {
					in.close();
				} catch (IOException e) {
					war.warn("Failed to close file reader for volume " + volume.getName() +
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
							//BlockInfo info = volume.getBlockTypes()[i][j][k];
//							if(info == null) {
//								out.write("0,0,");
//							} else {
//								if(info.getType() == Material.SIGN || info.getType() == Material.SIGN_POST) {
//									String[] lines = info.getSignLines();
//									out.write(info.getTypeId() + "," + info.getData() + "," + lines[0] + "[line]" + lines[1] 
//									          + "[line]" + lines[2] + "[line]"+ lines[3]);
//									
//								} else {
//									out.write(info.getTypeId() + "," + info.getData() + ","); 
								//}
//							}
							out.newLine();
						}
					}
				}
			} catch (IOException e) {
				war.warn("Failed to write volume file " + zoneName + 
						" for warzone " + volume.getName() + ". " + e.getClass().getName() + " " + e.getMessage());
				e.printStackTrace();
			} 
			finally {
				if(out != null)
					try {
						out.close();
					} catch (IOException e) {
						war.warn("Failed to close file writer for volume " + volume.getName() +
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
			war.warn("Failed to delete file " + volFile.getName());
		}
	}


}
