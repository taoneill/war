package bukkit.tommytony.war.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import bukkit.tommytony.war.War;
import bukkit.tommytony.war.WarCommandHandler;

import com.tommytony.war.Team;
import com.tommytony.war.Warzone;

public class WarhubCommand extends AbstractWarCommand {
	public WarhubCommand(WarCommandHandler handler, CommandSender sender, String[] args) {
		super(handler, sender, args);
	}

	public boolean handle() {
		if (!(this.sender instanceof Player)) return true;

		Player player = (Player) this.sender;
		if (War.war.getWarHub() == null) {
			War.war.badMsg(player, "No warhub on this War server. Try /zones and /zone.");
		} else if (!War.war.canWarp(player)) {
			War.war.badMsg(player, "Can't warp to warhub. You need the 'war.warp' permission.");
		} else {
			Team playerTeam = War.war.getPlayerTeam(player.getName());
			Warzone playerWarzone = War.war.getPlayerTeamWarzone(player.getName());
			if (playerTeam != null) { // was in zone
				playerWarzone.handlePlayerLeave(player, War.war.getWarHub().getLocation(), true);
			}
			player.teleport(War.war.getWarHub().getLocation());
		}
		return true;
	}
}
