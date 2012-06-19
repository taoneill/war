package com.tommytony.war.command;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.tommytony.war.War;
import com.tommytony.war.Warzone;
import com.tommytony.war.config.WarConfig;
import com.tommytony.war.mapper.WarzoneYmlMapper;
import com.tommytony.war.structure.ZoneLobby;

public class SaveZoneCommand extends AbstractZoneMakerCommand {

	public SaveZoneCommand(WarCommandHandler handler, CommandSender sender, String[] args) throws NotZoneMakerException {
		super(handler, sender, args);
	}

	@Override
	public boolean handle() {
		Warzone zone = null;
		CommandSender commandSender = this.getSender();
		boolean isFirstParamWarzone = false;

		if (this.args.length > 0 && !this.args[0].contains(":")) {
			// warzone name maybe in first place
			Warzone zoneByName = Warzone.getZoneByName(this.args[0]);
			if (zoneByName != null) {
				zone = zoneByName;
				isFirstParamWarzone = true;
			}
		}

		if (this.getSender() instanceof Player) {
			Player player = (Player) commandSender;

			Warzone zoneByLoc = Warzone.getZoneByLocation(player);
			ZoneLobby lobbyByLoc = ZoneLobby.getLobbyByLocation(player);
			if (zoneByLoc == null && lobbyByLoc != null) {
				zoneByLoc = lobbyByLoc.getZone();
			}
			if (zoneByLoc != null) {
				zone = zoneByLoc;
			}
		}

		if (zone == null) {
			// No warzone found, whatever the mean, escape
			return false;
		} else if (!this.isSenderAuthorOfZone(zone)) {
			return true;
		}

		if (isFirstParamWarzone) {
			if (this.args.length > 1) {
				// More than one param: the arguments need to be shifted
				String[] newargs = new String[this.args.length - 1];
				for (int i = 1; i < this.args.length; i++) {
					newargs[i - 1] = this.args[i];
				}
				this.args = newargs;
			}
		}

		// We have a warzone and indexed-from-0 arguments
		if (War.war.getWarConfig().getBoolean(WarConfig.KEEPOLDZONEVERSIONS)) {
			// Keep a copy of the old version, just in case. First, find the version number
			File oldVersionsFolder = new File(War.war.getDataFolder().getPath() + "/temp/oldversions/warzone-" + zone.getName());
			oldVersionsFolder.mkdirs();
			
			File[] versionFolders = oldVersionsFolder.listFiles();
			int newVersion = versionFolders.length + 1;
						
			SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss"); 
			String newVersionString = format.format(new Date()) + "-" + newVersion;
			String newVersionPath = War.war.getDataFolder().getPath() + "/temp/oldversions/warzone-" + zone.getName() + "/" + newVersionString;
			File newVersionFolder = new File(newVersionPath);
			newVersionFolder.mkdir();
			
			// Copy all warzone files to new version folder before they get overwritten
			try {
				copyFile(new File(War.war.getDataFolder().getPath() + "/warzone-" + zone.getName() + ".yml"), new File(newVersionPath + "/warzone-" + zone.getName() + ".yml"));
				(new File(newVersionPath + "/dat/warzone-" + zone.getName())).mkdirs();
				String oldPath = War.war.getDataFolder().getPath() + "/dat/warzone-" + zone.getName() + "/";
				File currentZoneFolder = new File(oldPath);
				
				File[] currentZoneFiles = currentZoneFolder.listFiles();
				for (File file : currentZoneFiles) {
					copyFile(file, new File(newVersionPath + "/dat/warzone-" + zone.getName() + "/" + file.getName()));
				}
		    } catch (IOException badCopy) {
		    	War.war.log("Failed to make backup copy version " + newVersion + " of warzone " + zone.getName(), Level.WARNING);
		    }
			
			int currentVersion = newVersion + 1;
			this.msg("Saving version " + currentVersion + " of warzone " + zone.getName());
			War.war.log(this.getSender().getName() + " is saving version " + currentVersion + " of warzone " + zone.getName(), Level.INFO);
		} else {
			this.msg("Saving new permanent version of warzone " + zone.getName());
			War.war.log(this.getSender().getName() + " is saving new permanent version of warzone " + zone.getName(), Level.INFO);
		}
		
		// Let's save the new version update
		int savedBlocks = zone.saveState(true);

		// changed settings: must reinitialize with new settings
		String namedParamResult = War.war.updateZoneFromNamedParams(zone, commandSender, this.args);
		WarzoneYmlMapper.save(zone);
		if (this.args.length > 0) {
			// the config may have changed, requiring a reset for spawn styles etc.
			zone.getVolume().resetBlocks();
		}
		if (zone.getLobby() != null) {
			zone.getLobby().getVolume().resetBlocks();
		}
		zone.initializeZone(); // bring back team spawns etc

		if (War.war.getWarHub() != null) { // maybe the zone was disabled/enabled
			War.war.getWarHub().getVolume().resetBlocks();
			War.war.getWarHub().initialize();
		}

		this.msg("Saved " + savedBlocks + " blocks in warzone " + zone.getName() + "." + namedParamResult);
		if (namedParamResult != null && namedParamResult.length() > 0) {
			War.war.log(this.getSender().getName() + " also updated warzone " + zone.getName() + " configuration." + namedParamResult, Level.INFO);
		}

		return true;
	}
	
	public static void copyFile(File sourceFile, File destFile) throws IOException {
	    if(!destFile.exists()) {
	        destFile.createNewFile();
	    }

	    FileChannel source = null;
	    FileChannel destination = null;

	    try {
	        source = new FileInputStream(sourceFile).getChannel();
	        destination = new FileOutputStream(destFile).getChannel();
	        destination.transferFrom(source, 0, source.size());
	    }
	    finally {
	        if(source != null) {
	            source.close();
	        }
	        if(destination != null) {
	            destination.close();
	        }
	    }
	}
}
