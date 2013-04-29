package com.tommytony.war.utility;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.entity.Player;

import com.tommytony.war.War;
import com.tommytony.war.mapper.PlayerStatsYmlMapper;

/*
 * A class to keep track of the Player stats during the current session and hold them in memory,
 * then serialize them to the disk when its appropriate
 */

public class PlayerStatTracker {

	/*Static Section*/
	private static Map<Player, PlayerStatTracker> stats = new HashMap<Player, PlayerStatTracker>();
	
	public static void addTracking(Player p, PlayerStatTracker t) {
		stats.put(p, t);
	}
	
	public static PlayerStatTracker getStats(Player p) {
		return stats.get(p);
	}
	
	public static void serialize(Player p) {
		//write to disk
		PlayerStatTracker statTrack = stats.get(p);
		War.war.getStatMapper().save(p, new int[] {statTrack.getKills(), statTrack.getDeaths(),
				statTrack.getWins(), statTrack.getLosses()});
		//and remove player from memory
		stats.remove(p);
	}
	
	/*Object Section*/
	
	private int kills;
	private int deaths;
	private int wins;
	private int losses;
	
	public PlayerStatTracker(Player p) {
		int[] stats = War.war.getStatMapper().load(p);
		this.kills = stats[0];
		this.deaths = stats[0];
		this.wins = stats[0];
		this.losses = stats[0];
		addTracking(p, this);
	}
	
	public void incKills() {
		this.kills++;
	}
	
	public void incDeaths() {
		this.deaths++;
	}
	
	public void incWins() {
		this.wins++;
	}
	
	public void incLosses() {
		this.losses++;
	}
	
	public int getKills() {
		return this.kills;
	}
	
	public int getDeaths() {
		return this.deaths;
	}
	
	public int getWins() {
		return this.wins;
	}
	
	public int getLosses() {
		return this.losses;
	}
}