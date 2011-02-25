package com.tommytony.war.jobs;

import org.bukkit.entity.Player;

import com.tommytony.war.Warzone;

public class ScoreCapReachedJob implements Runnable {

	private final Player player;
	private final Warzone zone;
	private boolean giveReward;

	public ScoreCapReachedJob(Player player, Warzone zone) {
		this.player = player;
		this.zone = zone;
	}

	public void run() {
		player.teleportTo(zone.getTeleport());
		// don't reset inv of dead guy who caused this, he's gonna die becasue this takes too long so we'll restore inv at PLAYER_MOVE 
		if(zone.hasPlayerInventory(player.getName())){
			zone.restorePlayerInventory(player);
		}
		if(giveReward) {
			// give reward
			for(Integer slot : zone.getReward().keySet()){
				player.getInventory().addItem(zone.getReward().get(slot));
			}
		}
	}

	public void giveReward(boolean giveReward) {
		this.giveReward = giveReward;
		// TODO Auto-generated method stub
		
	}

}
