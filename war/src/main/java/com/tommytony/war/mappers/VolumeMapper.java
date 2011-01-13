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
import com.tommytony.war.volumes.Volume;

public class VolumeMapper {
	public static Volume load(String zoneName, String volumeName, War war, World world) {
		BufferedReader in = null;
		Volume volume = null;
		try {
			in = new BufferedReader(new FileReader(new File("War/warzone-" + zoneName + "/volume-" + volumeName)));
			String firstLine = in.readLine();
			if(firstLine != null && !firstLine.equals("")) {
				int x1 = Integer.parseInt(in.readLine());
				int y1 = Integer.parseInt(in.readLine());
				int z1 = Integer.parseInt(in.readLine());
				int x2 = Integer.parseInt(in.readLine());
				int y2 = Integer.parseInt(in.readLine());
				int z2 = Integer.parseInt(in.readLine());
				
				volume = new Volume(volumeName, war, world);
				volume.setCornerOne(world.getBlockAt(x1, y1, z1));
				volume.setCornerTwo(world.getBlockAt(x2, y2, z2));
				
				volume.setBlockInfos(new BlockInfo[volume.getSizeX()][volume.getSizeY()][volume.getSizeZ()]);
				for(int i = 0; i < volume.getSizeX(); i++){
					for(int j = 0; j < volume.getSizeY(); j++) {
						for(int k = 0; k < volume.getSizeZ(); k++) {
							String blockLine = in.readLine();
							String[] blockSplit = blockLine.split(",");
							
							int typeID = Integer.parseInt(blockSplit[0]);
							byte data = Byte.parseByte(blockSplit[1]);
							String[] lines = null;
							if(typeID == Material.Sign.getID() || typeID == Material.SignPost.getID()) {
								String signLines = blockSplit[2];
								if(blockSplit.length > 3) {
									// sign includes commas
									for(int splitI = 3; splitI < blockSplit.length; splitI++) {
										signLines.concat(blockSplit[splitI]);
									}
								}
								String[] signLinesSplit = signLines.split("[line]");
								lines = new String[4];
								lines[0] = signLinesSplit[0];
								lines[1] = signLinesSplit[1];
								lines[2] = signLinesSplit[2];
								lines[3] = signLinesSplit[3];
							}
							volume.getBlockInfos()[i][j][k] = new BlockInfo(typeID, data, lines);
						}
					}
				}
			}
		} catch (IOException e) {
			war.getLogger().warning("Failed to read volume file " + volumeName + 
					" for warzone " + zoneName + ". " + e.getClass().getName() + " " + e.getMessage());
			e.printStackTrace();
		} finally {
			if(in != null)
				try {
					in.close();
				} catch (IOException e) {
					war.getLogger().warning("Failed to close file reader for volume " + volumeName +
							" for warzone " + zoneName + ". " + e.getClass().getName() + " " + e.getMessage());
					e.printStackTrace();
				}
		}
		return volume;
	}
	
	public static void save(Volume volume, String zoneName, War war) {
		if(volume.isSaved() && volume.getBlockInfos() != null) {
			BufferedWriter out = null;
			try {
				out = new BufferedWriter(new FileWriter(new File("War/warzone-" + zoneName + "/volume-" + volume.getName() + ".dat")));
				out.write("corner1"); out.newLine();
				out.write(volume.getCornerOne().getX()); out.newLine();
				out.write(volume.getCornerOne().getY()); out.newLine();
				out.write(volume.getCornerOne().getZ()); out.newLine();
				out.write("corner2"); out.newLine();
				out.write(volume.getCornerTwo().getX()); out.newLine();
				out.write(volume.getCornerTwo().getY()); out.newLine();
				out.write(volume.getCornerTwo().getZ()); out.newLine();
				
				for(int i = 0; i < volume.getSizeX(); i++){
					for(int j = 0; j < volume.getSizeY(); j++) {
						for(int k = 0; k < volume.getSizeZ(); k++) {
							BlockInfo info = volume.getBlockInfos()[i][j][k];
							if(info == null) {
								out.write("0,0,"); out.newLine();
							} else {
								if(info.getType() == Material.Sign || info.getType() == Material.SignPost) {
									String[] lines = info.getSignLines();
									out.write(info.getTypeID() + "," + info.getData() + "," + lines[0] + "[line]" + lines[1] 
									          + "[line]" + lines[2] + "[line]"+ lines[3]);
									
								} else {
									out.write(info.getTypeID() + "," + info.getData() + ","); 
								}
							}
						}
					}
				}
			} catch (IOException e) {
				war.getLogger().warning("Failed to write volume file " + zoneName + 
						" for warzone " + volume.getName() + ". " + e.getClass().getName() + " " + e.getMessage());
				e.printStackTrace();
			} 
			finally {
				if(out != null)
					try {
						out.close();
					} catch (IOException e) {
						war.getLogger().warning("Failed to close file writer for volume " + volume.getName() +
								" for warzone " + zoneName + ". " + e.getClass().getName() + " " + e.getMessage());
						e.printStackTrace();
					}	
			}
		}
	}

}
