package com.tommytony.war.command;

import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.bukkit.selections.CuboidSelection;
import com.sk89q.worldedit.bukkit.selections.Selection;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.Location;

import com.tommytony.war.War;


public class SetZoneCommand extends AbstractZoneMakerCommand {

	public SetZoneCommand(WarCommandHandler handler, CommandSender sender, String[] args) throws NotZoneMakerException {
		super(handler, sender, args);
	}

	@Override
	public boolean handle() {
		if (!(this.getSender() instanceof Player)) {
			this.badMsg("command.console");
			return true;
		}

		Player player = (Player) this.getSender();

		if (this.args.length == 0) {
			return false;
		} else if (this.args.length > 2) {
			return false;
		} else if (this.args.length == 1) {
			if (War.war.getServer().getPluginManager().isPluginEnabled("WorldEdit")) {
				WorldEditPlugin worldEdit = (WorldEditPlugin) War.war.getServer().getPluginManager().getPlugin("WorldEdit");
				Selection selection = worldEdit.getSelection(player);
				if (selection != null && selection instanceof CuboidSelection) {
					Location min = selection.getMinimumPoint();
					Location max = selection.getMaximumPoint();
					ZoneSetter setter = new ZoneSetter(player, this.args[0]);
					setter.placeCorner1(min.getBlock());
					setter.placeCorner2(max.getBlock());
					return true;
				}
			}
			War.war.addWandBearer(player, this.args[0]);
		} else if (this.args.length == 2) {
			if (!this.args[1].equals("southeast") && !this.args[1].equals("northwest") && !this.args[1].equals("se") && !this.args[1].equals("nw") && !this.args[1].equals("corner1") && !this.args[1].equals("corner2") && !this.args[1].equals("c1") && !this.args[1].equals("c2") && !this.args[1].equals("pos1") && !this.args[1].equals("pos2") && !this.args[1].equals("wand")) {
				return false;
			}

			ZoneSetter setter = new ZoneSetter(player, this.args[0]);
			if (this.args[1].equals("northwest") || this.args[1].equals("nw")) {
				setter.placeNorthwest();
			} else if (this.args[1].equals("southeast") || this.args[1].equals("se")) {
				setter.placeSoutheast();
			} else if (this.args[1].equals("corner1") || this.args[1].equals("c1") || this.args[1].equals("pos1")) {
				setter.placeCorner1();
			} else if (this.args[1].equals("corner2") || this.args[1].equals("c2") || this.args[1].equals("pos2")) {
				setter.placeCorner2();
			} else if (this.args[1].equals("wand")) {
				War.war.addWandBearer(player, this.args[0]);
			}
		}

		return true;
	}
}
