package com.tommytony.war.mapper;

import org.bukkit.entity.Player;

public interface PlayerStatsMapper {

	//stat numbers
	public final static int KILL = 4;
	public final static int LOSS = 5;
	public final static int WIN = 6;
	public final static int DEATH = 7;
	
	//initializes data source
	public void init();
	
	//shuts down data source/cleans up resources
	public void close();
	
	//loads the stats for the given player
	public int load(Player player, int stat);
	
	public int[] load(Player player);
	
	//saves the stats for the given player
	public void save(Player player, int stat, int amt);
	
	public void save(Player player, int[] stats);
}
