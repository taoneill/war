package bukkit.tommytony.war.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import bukkit.tommytony.war.War;
import bukkit.tommytony.war.WarCommandHandler;

import com.tommytony.war.Warzone;

public class WarhubCommand extends AbstractWarCommand {
	public WarhubCommand(WarCommandHandler handler, CommandSender sender, String[] args) {
		super(handler, sender, args);
	}

	@Override
	public boolean handle() {
		if (!(this.sender instanceof Player)) {
			return true;
		}
		if (this.args.length != 0) {
			return false;
		}
		Player player = (Player) this.sender;
		if (War.war.getWarHub() == null) {
			this.msg("No warhub on this War server. Try /zones and /zone.");
		} else if (!War.war.canWarp(player)) {
			this.msg("Can't warp to warhub. You need the 'war.warp' permission.");
		} else {
			Warzone playerWarzone = Warzone.getZoneByPlayerName(player.getName());
			if (playerWarzone != null) { // was in zone
				playerWarzone.handlePlayerLeave(player, War.war.getWarHub().getLocation(), true);
			}
			player.teleport(War.war.getWarHub().getLocation());
		}
		return true;
	}
}
