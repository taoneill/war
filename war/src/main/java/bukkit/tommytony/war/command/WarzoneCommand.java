package bukkit.tommytony.war.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import bukkit.tommytony.war.War;
import bukkit.tommytony.war.WarCommandHandler;

import com.tommytony.war.Warzone;

public class WarzoneCommand extends AbstractWarCommand {
	public WarzoneCommand(WarCommandHandler handler, CommandSender sender, String[] args) {
		super(handler, sender, args);
	}

	@Override
	public boolean handle() {
		if (!(this.sender instanceof Player)) {
			return true;
		}

		if (this.args.length != 1) {
			return false;
		}
		Player player = (Player) this.sender;

		if (!War.war.canWarp(player)) {
			this.msg("Can't warp to zone. You need the 'war.warp' permission.");
		} else {
			Warzone warzone = Warzone.getZoneByName(this.args[0]);
			if (warzone.getTeleport() != null) {
				Warzone playerWarzone = Warzone.getZoneByPlayerName(player.getName());
				if (playerWarzone != null) {
					playerWarzone.handlePlayerLeave(player, warzone.getTeleport(), true);
				} else {
					player.teleport(warzone.getTeleport());
				}
				return true;
			}

			this.msg("No such warzone.");
		}
		return true;
	}
}
