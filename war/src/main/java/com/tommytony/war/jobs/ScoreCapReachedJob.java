package com.tommytony.war.jobs;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.getspout.spoutapi.SpoutManager;
import org.getspout.spoutapi.player.SpoutPlayer;

import bukkit.tommytony.war.War;
import bukkit.tommytony.war.WarSpoutListener;

import com.tommytony.war.Team;
import com.tommytony.war.Warzone;
import com.tommytony.war.spout.SpoutMessenger;

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
		                		SpoutMessenger.cleanForNotification("Match won! " + ChatColor.WHITE + "Winners:"),
		                		SpoutMessenger.cleanForNotification(SpoutMessenger.addMissingColor(winnersStr, zone)),
		                		Material.CAKE,
		                		(short)0,
		                		10000);
					}
				}
			}
			String winnersStrAndExtra = "Score cap reached. Game is over! Winning team(s): " + this.winnersStr;
			winnersStrAndExtra += ". Resetting warzone and your inventory...";
			t.teamcast(winnersStrAndExtra);
			boolean isSpoutServer = War.war.isSpoutServer();
			for (Player tp : t.getPlayers()) {
				// Send everyone to rally point (or zone lobby if not rally point)
				if (this.zone.getRallyPoint() != null) {
					tp.teleport(this.zone.getRallyPoint());
				} else {
					tp.teleport(this.zone.getTeleport());
				}
				tp.setFireTicks(0);
				tp.setRemainingAir(300);
				if (this.zone.hasPlayerState(tp.getName())) {
					this.zone.restorePlayerState(tp);
				}
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
