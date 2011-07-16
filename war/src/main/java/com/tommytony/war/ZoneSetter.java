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
		Warzone warzone = this.war.findWarzone(this.zoneName);
		Block northwestBlock = this.player.getLocation().getWorld().getBlockAt(this.player.getLocation());
		StringBuilder msgString = new StringBuilder();
		try {
			if (warzone == null) {
				// create the warzone
				warzone = new Warzone(this.war, this.player.getLocation().getWorld(), this.zoneName);
				this.war.getIncompleteZones().add(warzone);
				warzone.getVolume().setNorthwest(northwestBlock);
				this.war.msg(this.player, "Warzone " + warzone.getName() + " created. Northwesternmost point set to x:" + warzone.getVolume().getNorthwestX() + " z:" + warzone.getVolume().getNorthwestZ() + ". ");
			} else {
				// change existing warzone
				this.resetWarzone(warzone, msgString);
				warzone.getVolume().setNorthwest(northwestBlock);
				msgString.append("Warzone " + warzone.getName() + " modified. Northwesternmost point set to x:" + warzone.getVolume().getNorthwestX() + " z:" + warzone.getVolume().getNorthwestZ() + ". ");
			}
			this.saveIfReady(warzone, msgString);
		} catch (NotNorthwestException e) {
			this.war.badMsg(this.player, "The block you selected is not to the northwest of the existing southeasternmost block.");
			if (warzone.getVolume().isSaved()) {
				warzone.initializeZone(); // was reset before changing
			}
		} catch (TooSmallException e) {
			this.handleTooSmall();
			if (warzone.getVolume().isSaved()) {
				warzone.initializeZone();
			}
		} catch (TooBigException e) {
			this.handleTooBig();
			if (warzone.getVolume().isSaved()) {
				warzone.initializeZone();
			}
		}
	}

	public void placeSoutheast() {
		Warzone warzone = this.war.findWarzone(this.zoneName);
		Block southeastBlock = this.player.getLocation().getWorld().getBlockAt(this.player.getLocation());
		StringBuilder msgString = new StringBuilder();
		try {
			if (warzone == null) {
				// create the warzone
				warzone = new Warzone(this.war, this.player.getLocation().getWorld(), this.zoneName);
				this.war.getIncompleteZones().add(warzone);
				warzone.getVolume().setSoutheast(southeastBlock);
				this.war.msg(this.player, "Warzone " + warzone.getName() + " created. Southeasternmost point set to x:" + warzone.getVolume().getSoutheastX() + " z:" + warzone.getVolume().getSoutheastZ() + ". ");
			} else {
				// change existing warzone
				this.resetWarzone(warzone, msgString);
				warzone.getVolume().setSoutheast(southeastBlock);
				msgString.append("Warzone " + warzone.getName() + " modified. Southeasternmost point set to x:" + warzone.getVolume().getSoutheastX() + " z:" + warzone.getVolume().getSoutheastZ() + ". ");
			}
			this.saveIfReady(warzone, msgString);
		} catch (NotSoutheastException e) {
			this.war.badMsg(this.player, "The block you selected is not to the southeast of the existing northwestnmost block.");
			if (warzone.getVolume().isSaved()) {
				warzone.initializeZone(); // was reset before changing
			}
		} catch (TooSmallException e) {
			this.handleTooSmall();
			if (warzone.getVolume().isSaved()) {
				warzone.initializeZone();
			}
		} catch (TooBigException e) {
			this.handleTooBig();
			if (warzone.getVolume().isSaved()) {
				warzone.initializeZone();
			}
		}
	}

	public void placeCorner1() {
		Block corner1Block = this.player.getLocation().getWorld().getBlockAt(this.player.getLocation());
		this.placeCorner1(corner1Block);
	}

	public void placeCorner1(Block corner1Block) {
		Warzone warzone = this.war.findWarzone(this.zoneName);
		StringBuilder msgString = new StringBuilder();
		try {
			if (warzone == null) {
				// create the warzone
				warzone = new Warzone(this.war, this.player.getLocation().getWorld(), this.zoneName);
				this.war.getIncompleteZones().add(warzone);
				warzone.getVolume().setZoneCornerOne(corner1Block);
				this.war.msg(this.player, "Warzone " + warzone.getName() + " created. Corner 1 set to x:" + corner1Block.getX() + " y:" + corner1Block.getY() + " z:" + corner1Block.getZ() + ". ");
			} else {
				// change existing warzone
				this.resetWarzone(warzone, msgString);
				warzone.getVolume().setZoneCornerOne(corner1Block);
				msgString.append("Warzone " + warzone.getName() + " modified. Corner 1 set to x:" + corner1Block.getX() + " y:" + corner1Block.getY() + " z:" + corner1Block.getZ() + ". ");
			}
			this.saveIfReady(warzone, msgString);
		} catch (TooSmallException e) {
			this.handleTooSmall();
			if (warzone.getVolume().isSaved()) {
				warzone.initializeZone();
			}
		} catch (TooBigException e) {
			this.handleTooBig();
			if (warzone.getVolume().isSaved()) {
				warzone.initializeZone();
			}
		}
	}

	public void placeCorner2() {
		Block corner2Block = this.player.getLocation().getWorld().getBlockAt(this.player.getLocation());
		this.placeCorner2(corner2Block);
	}

	public void placeCorner2(Block corner2Block) {
		Warzone warzone = this.war.findWarzone(this.zoneName);
		StringBuilder msgString = new StringBuilder();
		try {
			if (warzone == null) {
				// create the warzone
				warzone = new Warzone(this.war, this.player.getLocation().getWorld(), this.zoneName);
				this.war.getIncompleteZones().add(warzone);
				warzone.getVolume().setZoneCornerTwo(corner2Block);
				this.war.msg(this.player, "Warzone " + warzone.getName() + " created. Corner 2 set to x:" + corner2Block.getX() + " y:" + corner2Block.getY() + " z:" + corner2Block.getZ() + ". ");
			} else {
				// change existing warzone
				this.resetWarzone(warzone, msgString);
				warzone.getVolume().setZoneCornerTwo(corner2Block);
				msgString.append("Warzone " + warzone.getName() + " modified. Corner 2 set to x:" + corner2Block.getX() + " y:" + corner2Block.getY() + " z:" + corner2Block.getZ() + ". ");
			}
			this.saveIfReady(warzone, msgString);
		} catch (TooSmallException e) {
			this.handleTooSmall();
			if (warzone.getVolume().isSaved()) {
				warzone.initializeZone();
			}
		} catch (TooBigException e) {
			this.handleTooBig();
			if (warzone.getVolume().isSaved()) {
				warzone.initializeZone();
			}
		}
	}

	private void resetWarzone(Warzone warzone, StringBuilder msgString) {
		if (warzone.getVolume().isSaved()) {
			this.war.msg(this.player, "Resetting " + warzone.getName() + " blocks.");
			if (warzone.getLobby() != null && warzone.getLobby().getVolume() != null) {
				warzone.getLobby().getVolume().resetBlocks();
			}
			int reset = warzone.getVolume().resetBlocks();
			msgString.append(reset + " blocks reset. ");
		}
	}

	private void handleTooSmall() {
		this.war.badMsg(this.player, "That would make the " + this.zoneName + " warzone too small. Sides must be at least 10 blocks and all existing structures (spawns, flags, etc) must fit inside.");
	}

	private void handleTooBig() {
		this.war.badMsg(this.player, "That would make the " + this.zoneName + " warzone too big. Sides must be less than 750 blocks.");
	}

	private void saveIfReady(Warzone warzone, StringBuilder msgString) {
		if (warzone.ready()) {
			if (!this.war.getWarzones().contains(warzone)) {
				this.war.addWarzone(warzone);
			}
			if (this.war.getIncompleteZones().contains(warzone)) {
				this.war.getIncompleteZones().remove(warzone);
			}
			WarMapper.save(this.war);
			msgString.append("Saving new warzone blocks...");
			this.war.msg(this.player, msgString.toString());
			warzone.saveState(false); // we just changed the volume, cant reset walls
			if (warzone.getLobby() == null) {
				// Set default lobby on south side
				ZoneLobby lobby = new ZoneLobby(this.war, warzone, BlockFace.SOUTH);
				warzone.setLobby(lobby);
				if (this.war.getWarHub() != null) { // warhub has to change
					this.war.getWarHub().getVolume().resetBlocks();
					this.war.getWarHub().initialize();
				}
				this.war.msg(this.player, "Default lobby created on south side of zone. Use /setzonelobby <n/s/e/w> to change its position.");
			} // else {
				// gotta move the lobby (or dont because zone.initzon does it for you)
				// warzone.getLobby().changeWall(warzone.getLobby().getWall());
			// }
			warzone.initializeZone();
			WarzoneMapper.save(this.war, warzone, true);
			this.war.msg(this.player, "Warzone saved.");
		} else {
			if (warzone.getVolume().getCornerOne() == null) {
				msgString.append("Still missing corner 1.");
			} else if (warzone.getVolume().getCornerTwo() == null) {
				msgString.append("Still missing corner 2.");
			}
			this.war.msg(this.player, msgString.toString());
		}
	}

}
