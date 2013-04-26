package com.tommytony.war.utility;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.entity.Player;

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
		PlayerStatsYmlMapper.save(p, PlayerStatsYmlMapper.KILL, stats.get(p).getKills());
		PlayerStatsYmlMapper.save(p, PlayerStatsYmlMapper.DEATH, stats.get(p).getDeaths());
		PlayerStatsYmlMapper.save(p, PlayerStatsYmlMapper.WIN, stats.get(p).getWins());
		PlayerStatsYmlMapper.save(p, PlayerStatsYmlMapper.LOSS, stats.get(p).getLosses());
		//and remove player from memory
		stats.remove(p);
	}
	
	/*Object Section*/
	
	private int kills;
	private int deaths;
	private int wins;
	private int losses;
	
	public PlayerStatTracker(Player p) {
		this.kills = 0;
		this.deaths = 0;
		this.wins = 0;
		this.losses = 0;
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
