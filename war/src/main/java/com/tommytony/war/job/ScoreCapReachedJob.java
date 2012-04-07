package com.tommytony.war.job;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.getspout.spoutapi.SpoutManager;
import org.getspout.spoutapi.player.SpoutPlayer;


import com.tommytony.war.Team;
import com.tommytony.war.War;
import com.tommytony.war.Warzone;
import com.tommytony.war.spout.SpoutDisplayer;

public class ScoreCapReachedJob implements Runnable {

	private final Warzone zone;
	private final String winnersStr;

	public ScoreCapReachedJob(Warzone zone, String winnersStr) {
		this.zone = zone;
		this.winnersStr = winnersStr;
	}

	public void run() {
		for (Team t : this.zone.getTeams()) {
			if (War.war.isSpoutServer()) {
				for (Player p : t.getPlayers()) {
					SpoutPlayer sp = SpoutManager.getPlayer(p);
					if (sp.isSpoutCraftEnabled()) {
		                sp.sendNotification(
		                		SpoutDisplayer.cleanForNotification("Match won! " + ChatColor.WHITE + "Winners:"),
		                		SpoutDisplayer.cleanForNotification(SpoutDisplayer.addMissingColor(winnersStr, zone)),
		                		Material.CAKE,
		                		(short)0,
		                		10000);
					}
				}
			}
			String winnersStrAndExtra = "Score cap reached. Game is over! Winning team(s): " + this.winnersStr;
			winnersStrAndExtra += ". Resetting warzone and your inventory...";
			t.teamcast(winnersStrAndExtra);
			for (Player tp : t.getPlayers()) {
				// Send everyone to rally point (or zone lobby if not rally point)
				this.zone.gameEndTeleport(tp);
				
				if (this.winnersStr.contains(t.getName())) {
					// give reward
					for (Integer slot : t.getInventories().resolveReward().keySet()) {
						ItemStack item = t.getInventories().resolveReward().get(slot);
						if (item != null) {
							tp.getInventory().addItem(item);
						}
					}
				}
			}
			t.resetPoints();
			t.getPlayers().clear(); // empty the team
		}
	}
}
