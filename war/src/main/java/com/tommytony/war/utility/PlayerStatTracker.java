package com.tommytony.war.utility;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.entity.Player;

import com.tommytony.war.War;

/*
 * A class to keep track of the Player stats during the current session and hold them in memory,
 * then serialize them to the disk when its appropriate
 */

public class PlayerStatTracker {

	/*Static Section*/
	private static Map<String, PlayerStatTracker> stats = new HashMap<String, PlayerStatTracker>();
	
	public static void addTracking(Player p, PlayerStatTracker t) {
		stats.put(p.getName(), t);
	}
	
	public static PlayerStatTracker getStats(Player p) {
		return stats.get(p.getName());
	}
	
	public static void serialize(Player p) {
		//write to disk
		PlayerStatTracker statTrack = stats.get(p.getName());
		War.war.getStatMapper().save(p.getName(), new int[] {statTrack.getKills(), statTrack.getDeaths(),
				statTrack.getWins(), statTrack.getLosses()});
		//and remove player from memory
		stats.remove(p.getName());
	}
	
	public static PlayerStatTracker getStats(String player) {
		PlayerStatTracker stat = null;
		//check memory cache first
		stat = stats.get(player);
		if(stat == null) {
			//we gotta hit the disk
			stat = new PlayerStatTracker(War.war.getStatMapper().load(player));
		}
		return stat;
	}
	
	/*Object Section*/
	
	private int kills;
	private int deaths;
	private int wins;
	private int losses;
	
	public PlayerStatTracker(Player p) {
		int[] stats = War.war.getStatMapper().load(p.getName());
		this.kills = stats[0];
		this.deaths = stats[1];
		this.wins = stats[2];
		this.losses = stats[3];
		addTracking(p, this);
	}
	
	public PlayerStatTracker(int[] stats) {
		this.kills = stats[0];
		this.deaths = stats[1];
		this.wins = stats[2];
		this.losses = stats[3];
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
