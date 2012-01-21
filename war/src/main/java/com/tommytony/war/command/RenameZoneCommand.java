package com.tommytony.war.command;

import java.io.File;
import java.util.logging.Level;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;


import com.tommytony.war.War;
import com.tommytony.war.WarCommandHandler;
import com.tommytony.war.Warzone;
import com.tommytony.war.config.WarzoneConfig;
import com.tommytony.war.mapper.PropertiesFile;
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

		// kill old reference
		zone.unload();
		War.war.getWarzones().remove(zone);

		// rename zone file
		(new File(War.war.getDataFolder().getPath() + "/warzone-" + zone.getName() + ".yml")).renameTo(new File(War.war.getDataFolder().getPath() + "/warzone-" + this.args[0] + ".yml"));
		// rename zone folder
		(new File(War.war.getDataFolder().getPath() + "/dat/warzone-" + zone.getName())).renameTo(new File(War.war.getDataFolder().getPath() + "/dat/warzone-" + this.args[0]));

		// TODO: Move renaming into ZoneVolumeMapper?
		// rename volume files
		String oldStart = War.war.getDataFolder().getPath() + "/dat/warzone-" + this.args[0] + "/volume-" + zone.getName() + ".";
		String newStart = War.war.getDataFolder().getPath() + "/dat/warzone-" + this.args[0] + "/volume-" + this.args[0] + ".";
		(new File(oldStart + "corners")).renameTo(new File(newStart + "corners"));
		(new File(oldStart + "blocks")).renameTo(new File(newStart + "blocks"));
		(new File(oldStart + "signs")).renameTo(new File(newStart + "signs"));
		(new File(oldStart + "invs")).renameTo(new File(newStart + "invs"));

		// set new name
		PropertiesFile warzoneConfig = new PropertiesFile(War.war.getDataFolder().getPath() + "/warzone-" + this.args[0] + ".yml");
		warzoneConfig.setString("name", this.args[0]);
		warzoneConfig.save();
		warzoneConfig.close();

		War.war.log("Loading zone " + this.args[0] + "...", Level.INFO);
		Warzone newZone = WarzoneYmlMapper.load(this.args[0], false);
		War.war.getWarzones().add(newZone);
		// zone.getVolume().loadCorners();
		newZone.getVolume().loadCorners();
		if (newZone.getLobby() != null) {
			newZone.getLobby().getVolume().resetBlocks();
		}
		if (newZone.getWarzoneConfig().getBoolean(WarzoneConfig.RESETONLOAD)) {
			newZone.getVolume().resetBlocks();
		}
		newZone.initializeZone();

		// save war config
		WarYmlMapper.save();

		if (War.war.getWarHub() != null) { // warhub has to change
			War.war.getWarHub().getVolume().resetBlocks();
			War.war.getWarHub().initialize();
		}

		this.msg("Warzone " + zone.getName() + " renamed to " + this.args[0] + ".");

		return true;
	}
}
