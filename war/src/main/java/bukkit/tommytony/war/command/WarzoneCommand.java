package bukkit.tommytony.war.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import bukkit.tommytony.war.War;
import bukkit.tommytony.war.WarCommandHandler;

import com.tommytony.war.Team;
import com.tommytony.war.Warzone;

public class WarzoneCommand extends AbstractWarCommand {
	public WarzoneCommand(WarCommandHandler handler, CommandSender sender, String[] args) {
		super(handler, sender, args);
	}

	public boolean handle() {
		if (!(this.sender instanceof Player)) return true;

		Player player = (Player) this.sender;
		if (this.args.length < 1) {
			return false;
		} else if (!War.war.canWarp(player)) {
			War.war.badMsg(player, "Can't warp to zone. You need the 'war.warp' permission.");
		} else {
			for (Warzone warzone : War.war.getWarzones()) {
				if (warzone.getName().toLowerCase().startsWith(this.args[0].toLowerCase()) && warzone.getTeleport() != null) {
					Team playerTeam = War.war.getPlayerTeam(player.getName());
					if (playerTeam != null) {
						Warzone playerWarzone = War.war.getPlayerTeamWarzone(player.getName());
						playerWarzone.handlePlayerLeave(player, warzone.getTeleport(), true);
					} else {
						player.teleport(warzone.getTeleport());
					}
					return true;
				}
			}
			War.war.badMsg(player, "No such warzone.");
		}
		return true;
	}
}
