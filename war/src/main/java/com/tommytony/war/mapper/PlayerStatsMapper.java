package com.tommytony.war.mapper;

/**
 * @author grinning
 * @package com.tommytony.war.mapper
 * @description Interface for any stat mapping data source
 */

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
	public int load(String player, int stat);
	
	public int[] load(String player);
	
	//saves the stats for the given player
	public void save(String player, int stat, int amt);
	
	public void save(String player, int[] stats);
	
	public void save(String player, int stats[], boolean makeNewPlayer);
}
