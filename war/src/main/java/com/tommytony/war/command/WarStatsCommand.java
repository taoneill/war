package com.tommytony.war.command;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import com.tommytony.war.War;
import com.tommytony.war.utility.PlayerStatTracker;

public class WarStatsCommand extends AbstractWarCommand {

	public WarStatsCommand(WarCommandHandler handler, CommandSender sender, String[] args) {
		super(handler, sender, args);
	}

	@Override
	public boolean handle() {
		//we can do this if we are not in game :D
		if(this.args.length == 0) {
			return false;
		} else if(this.args.length == 1) {
			//they want every stat for a certain player
			PlayerStatTracker stat = PlayerStatTracker.getStats(args[0]);  //we shall multithread this if it gets laggy
			this.msg("Stats for " + ChatColor.GOLD + this.args[0] + "\n"
					+ ChatColor.WHITE + "Kills: " + ChatColor.DARK_PURPLE + stat.getKills() + "\n"
				    + ChatColor.WHITE + "Deaths: " + ChatColor.DARK_PURPLE + stat.getDeaths() + "\n"
				    + ChatColor.WHITE + "Wins: " + ChatColor.DARK_PURPLE + stat.getWins() + "\n"
				    + ChatColor.WHITE + "Losses: " + ChatColor.DARK_PURPLE + stat.getLosses());
			return true;
		} 
		return false;
    }
}
