package com.tommytony.war.command;

import java.io.File;
import java.sql.SQLException;
import java.util.logging.Level;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.tommytony.war.War;
import com.tommytony.war.Warzone;
import com.tommytony.war.config.WarzoneConfig;
import com.tommytony.war.mapper.WarYmlMapper;
import com.tommytony.war.mapper.WarzoneYmlMapper;
import com.tommytony.war.structure.ZoneLobby;

public class RenameZoneCommand extends AbstractZoneMakerCommand {
	public RenameZoneCommand(WarCommandHandler handler, CommandSender sender, String[] args) throws NotZoneMakerException {
		super(handler, sender, args);
	}

	@Override
	public boolean handle() {
		Warzone zone;
		
		if (this.args.length == 2) {
			zone = Warzone.getZoneByName(this.args[0]);
			this.args[0] = this.args[1];
		} else if (this.args.length == 1) {
			if (!(this.getSender() instanceof Player)) {
				return false;
			}
			zone = Warzone.getZoneByLocation((Player) this.getSender());
			if (zone == null) {
				ZoneLobby lobby = ZoneLobby.getLobbyByLocation((Player) this.getSender());
				if (lobby == null) {
					return false;
				}
				zone = lobby.getZone();
			}
		} else {
			return false;
		}
		
		if (zone == null) {
			return false;
		} else if (!this.isSenderAuthorOfZone(zone)) {
			return true;
		}
		
		// Kill old warzone, but use it to create the renamed copy
		zone.unload();
		zone.getVolume().resetBlocks();	// We're going to use the blocks to save the new copy, reset to base state.
		
		String newName = this.args[0];
		String oldName = zone.getName();
		
		// Update the name
		zone.setName(newName);
		zone.saveState(false); // Save new volume files. Don't clear anything, we already unloaded.
		WarzoneYmlMapper.save(zone);	// Save new config files for warzone.
				
		// Get rid of old unloaded zone instance
		War.war.getWarzones().remove(zone);
				
		// Move old files
		(new File(War.war.getDataFolder().getPath() + "/temp/renamed/")).mkdir();
		(new File(War.war.getDataFolder().getPath() + "/warzone-" + oldName + ".yml")).renameTo(new File(War.war.getDataFolder().getPath() + "/temp/renamed/warzone-" + oldName + ".yml"));
		(new File(War.war.getDataFolder().getPath() + "/temp/renamed/dat/warzone-" + oldName)).mkdirs();

		String oldPath = War.war.getDataFolder().getPath() + "/dat/warzone-" + oldName + "/";
		File oldZoneFolder = new File(oldPath);
		File[] oldZoneFiles = oldZoneFolder.listFiles();
		for (File file : oldZoneFiles) {
			file.renameTo(new File(War.war.getDataFolder().getPath() + "/temp/renamed/dat/warzone-" + oldName + "/" + file.getName()));
		}
		oldZoneFolder.delete();

		// Load new warzone
		War.war.log("Loading zone " + newName + "...", Level.INFO);
		Warzone newZone = WarzoneYmlMapper.load(newName);
		War.war.getWarzones().add(newZone);
		try {
			newZone.getVolume().loadCorners();
		} catch (SQLException ex) {
			War.war.log("Failed to load warzone " + newZone.getName() + ": " + ex.getMessage(), Level.WARNING);
			throw new RuntimeException(ex);
		}
		try {
			zone.getVolume().loadCorners();
		} catch (SQLException ex) {
			War.war.log("Failed to load warzone " + zone.getName() + ": " + ex.getMessage(), Level.WARNING);
			throw new RuntimeException(ex);
		}
		if (zone.getLobby() != null) {
			zone.getLobby().getVolume().resetBlocks();
		}
		if (zone.getWarzoneConfig().getBoolean(WarzoneConfig.RESETONLOAD)) {
			zone.getVolume().resetBlocks();
		}

		newZone.initializeZone();

		// Update war config
		WarYmlMapper.save();

		if (War.war.getWarHub() != null) { // warhub has to change
			War.war.getWarHub().getVolume().resetBlocks();
			War.war.getWarHub().initialize();
		}

		War.war.log(this.getSender().getName() + " renamed warzone " + oldName + " to " + newName, Level.INFO);
		this.msg("Warzone " + oldName + " renamed to " + newName + ".");

		return true;
	}
}
