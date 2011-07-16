package com.tommytony.war;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import com.tommytony.war.mappers.WarMapper;
import com.tommytony.war.mappers.WarzoneMapper;
import com.tommytony.war.volumes.NotNorthwestException;
import com.tommytony.war.volumes.NotSoutheastException;
import com.tommytony.war.volumes.TooBigException;
import com.tommytony.war.volumes.TooSmallException;

import bukkit.tommytony.war.War;

public class ZoneSetter {

	private final War war;
	private final Player player;
	private final String zoneName;

	public ZoneSetter(War war, Player player, String zoneName) {
		this.war = war;
		this.player = player;
		this.zoneName = zoneName;
	}
	
	public void placeNorthwest() {
		Warzone warzone = war.findWarzone(zoneName);
		Block northwestBlock = player.getLocation().getWorld().getBlockAt(player.getLocation());
		StringBuilder msgString = new StringBuilder();
		try
		{
			if (warzone == null) {
				// create the warzone
				warzone = new Warzone(war, player.getLocation().getWorld(), zoneName);
				war.getIncompleteZones().add(warzone);
				warzone.getVolume().setNorthwest(northwestBlock);
				war.msg(player, "Warzone " + warzone.getName() + " created. Northwesternmost point set to x:" 
						+ (int)warzone.getVolume().getNorthwestX() + " z:" + (int)warzone.getVolume().getNorthwestZ() + ". ");
			} else {
				// change existing warzone
				resetWarzone(warzone, msgString);
				warzone.getVolume().setNorthwest(northwestBlock);
				msgString.append("Warzone " + warzone.getName() + " modified. Northwesternmost point set to x:" 
						+ (int)warzone.getVolume().getNorthwestX() + " z:" + (int)warzone.getVolume().getNorthwestZ() + ". ");
			}
			saveIfReady(warzone, msgString);
		} catch (NotNorthwestException e) {
			war.badMsg(player, "The block you selected is not to the northwest of the existing southeasternmost block.");
			if (warzone.getVolume().isSaved()) warzone.initializeZone();	// was reset before changing
		} catch (TooSmallException e) {
			handleTooSmall();
			if (warzone.getVolume().isSaved()) warzone.initializeZone();
		} catch (TooBigException e) {
			handleTooBig();
			if (warzone.getVolume().isSaved()) warzone.initializeZone();
		}
	}
	
	public void placeSoutheast() {
		Warzone warzone = war.findWarzone(zoneName);
		Block southeastBlock = player.getLocation().getWorld().getBlockAt(player.getLocation());
		StringBuilder msgString = new StringBuilder();
		try
		{
			if (warzone == null) {
				// create the warzone
				warzone = new Warzone(war, player.getLocation().getWorld(), zoneName);
				war.getIncompleteZones().add(warzone);
				warzone.getVolume().setSoutheast(southeastBlock);
				war.msg(player, "Warzone " + warzone.getName() + " created. Southeasternmost point set to x:" 
						+ (int)warzone.getVolume().getSoutheastX() + " z:" + (int)warzone.getVolume().getSoutheastZ() + ". ");
			} else {
				// change existing warzone
				resetWarzone(warzone, msgString);				
				warzone.getVolume().setSoutheast(southeastBlock);
				msgString.append("Warzone " + warzone.getName() + " modified. Southeasternmost point set to x:" 
						+ (int)warzone.getVolume().getSoutheastX() + " z:" + (int)warzone.getVolume().getSoutheastZ() + ". ");
			}
			saveIfReady(warzone, msgString);
		} catch (NotSoutheastException e) {
			war.badMsg(player, "The block you selected is not to the southeast of the existing northwestnmost block.");
			if (warzone.getVolume().isSaved()) warzone.initializeZone();	// was reset before changing
		} catch (TooSmallException e) {
			handleTooSmall();
			if (warzone.getVolume().isSaved()) warzone.initializeZone();
		} catch (TooBigException e) {
			handleTooBig();
			if (warzone.getVolume().isSaved()) warzone.initializeZone();
		}
	}
	
	public void placeCorner1() {
		Block corner1Block = player.getLocation().getWorld().getBlockAt(player.getLocation());
		placeCorner1(corner1Block);
	}
	
	public void placeCorner1(Block corner1Block) {
		Warzone warzone = war.findWarzone(zoneName);
		StringBuilder msgString = new StringBuilder();
		try
		{
			if (warzone == null) {
				// create the warzone
				warzone = new Warzone(war, player.getLocation().getWorld(), zoneName);
				war.getIncompleteZones().add(warzone);
				warzone.getVolume().setZoneCornerOne(corner1Block);
				war.msg(player, "Warzone " + warzone.getName() + " created. Corner 1 set to x:" 
						+ (int)corner1Block.getX() + " y:" + (int)corner1Block.getY() + " z:" + (int)corner1Block.getZ() + ". ");
			} else {
				// change existing warzone
				resetWarzone(warzone, msgString);				
				warzone.getVolume().setZoneCornerOne(corner1Block);
				msgString.append("Warzone " + warzone.getName() + " modified. Corner 1 set to x:" 
						+ (int)corner1Block.getX() + " y:" + (int)corner1Block.getY() + " z:" + (int)corner1Block.getZ() + ". ");
			}
			saveIfReady(warzone, msgString);
		} catch (TooSmallException e) {
			handleTooSmall();
			if (warzone.getVolume().isSaved()) warzone.initializeZone();
		} catch (TooBigException e) {
			handleTooBig();
			if (warzone.getVolume().isSaved()) warzone.initializeZone();
		}
	}
	
	public void placeCorner2() {
		Block corner2Block = player.getLocation().getWorld().getBlockAt(player.getLocation());
		placeCorner2(corner2Block);
	}
	
	public void placeCorner2(Block corner2Block) {
		Warzone warzone = war.findWarzone(zoneName);
		StringBuilder msgString = new StringBuilder();
		try
		{
			if (warzone == null) {
				// create the warzone
				warzone = new Warzone(war, player.getLocation().getWorld(), zoneName);
				war.getIncompleteZones().add(warzone);
				warzone.getVolume().setZoneCornerTwo(corner2Block);
				war.msg(player, "Warzone " + warzone.getName() + " created. Corner 2 set to x:" 
						+ (int)corner2Block.getX() + " y:" + (int)corner2Block.getY() + " z:" + (int)corner2Block.getZ() + ". ");
			} else {
				// change existing warzone
				resetWarzone(warzone, msgString);				
				warzone.getVolume().setZoneCornerTwo(corner2Block);
				msgString.append("Warzone " + warzone.getName() + " modified. Corner 2 set to x:" 
						+ (int)corner2Block.getX() + " y:" + (int)corner2Block.getY() + " z:" + (int)corner2Block.getZ() + ". ");
			}
			saveIfReady(warzone, msgString);
		} catch (TooSmallException e) {
			handleTooSmall();
			if (warzone.getVolume().isSaved()) warzone.initializeZone();
		} catch (TooBigException e) {
			handleTooBig();
			if (warzone.getVolume().isSaved()) warzone.initializeZone();
		}
	}
	
	private void resetWarzone(Warzone warzone, StringBuilder msgString) {
		if (warzone.getVolume().isSaved()) {
			war.msg(player, "Resetting " + warzone.getName() + " blocks.");
			if (warzone.getLobby() != null && warzone.getLobby().getVolume() != null) {
				warzone.getLobby().getVolume().resetBlocks();
			}
			int reset = warzone.getVolume().resetBlocks();
			msgString.append(reset + " blocks reset. ");
		}
	}
	
	private void handleTooSmall() {
		war.badMsg(player, "That would make the " + zoneName + " warzone too small. Sides must be at least 10 blocks and all existing structures (spawns, flags, etc) must fit inside.");
	}
	
	private void handleTooBig() {
		war.badMsg(player, "That would make the " + zoneName + " warzone too big. Sides must be less than 750 blocks.");
	}
	
	private void saveIfReady(Warzone warzone, StringBuilder msgString) {
		if (warzone.ready()) {
			if (!war.getWarzones().contains(warzone)) {
				war.addWarzone(warzone);
			}
			if (war.getIncompleteZones().contains(warzone)) {
				war.getIncompleteZones().remove(warzone);
			}
			WarMapper.save(war);
			msgString.append("Saving new warzone blocks...");
			war.msg(player, msgString.toString());
			warzone.saveState(false); // we just changed the volume, cant reset walls 
			if (warzone.getLobby() == null) {
				// Set default lobby on south side
				ZoneLobby lobby = new ZoneLobby(war, warzone, BlockFace.SOUTH);
				warzone.setLobby(lobby);
				if (war.getWarHub() != null) {	// warhub has to change
					war.getWarHub().getVolume().resetBlocks();
					war.getWarHub().initialize();
				}
				war.msg(player, "Default lobby created on south side of zone. Use /setzonelobby <n/s/e/w> to change its position.");
			} //else {
				// gotta move the lobby (or dont because zone.initzon does it for you)
				//warzone.getLobby().changeWall(warzone.getLobby().getWall());
			//}
			warzone.initializeZone();
			WarzoneMapper.save(war, warzone, true);
			war.msg(player, "Warzone saved.");
		} else {
			if (warzone.getVolume().getCornerOne() == null) {
				msgString.append("Still missing corner 1.");
			} else if (warzone.getVolume().getCornerTwo() == null) {
				msgString.append("Still missing corner 2.");
			}
			war.msg(player, msgString.toString());
		}
	}
	
}
