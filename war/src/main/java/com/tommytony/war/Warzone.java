package com.tommytony.war;
import org.bukkit.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Warzone {
	private String name;
	private Location northwest;
	private Location southeast;
	private final List<Team> teams = new ArrayList<Team>();
	private final List<Monument> monuments = new ArrayList<Monument>();
	
	private int[][][] initialState = null;
	private Location teleport;
	private boolean friendlyFire;
	private War war;
	private int lifePool;
	private HashMap<Integer, ItemStack> loadout; 
	
	private HashMap<String, HashMap<Integer, ItemStack>> inventories = new HashMap<String, HashMap<Integer, ItemStack>>();
	private World world;
	
	public Warzone(War war, World world, String name) {
		this.war = war;
		this.world = world;
		this.name = name;
		this.friendlyFire = war.getDefaultFriendlyFire();
		this.setLifePool(war.getDefaultLifepool());
		this.setLoadout(war.getDefaultLoadout());
	}
	
	public boolean ready() {
		if(getNorthwest() != null && getSoutheast() != null 
				&& !tooSmall() && !tooBig()) return true;
		return false;
	}
	
	public boolean tooSmall() {
		if((getSoutheast().getBlockX() - getNorthwest().getBlockX() < 20)
				|| (getNorthwest().getBlockZ() - getSoutheast().getBlockZ() < 20)) return true;
		return false;
	}
	
	public boolean tooBig() {
		if((getSoutheast().getBlockX() - getNorthwest().getBlockX() > 1000)
				|| (getNorthwest().getBlockZ() - getSoutheast().getBlockZ() > 1000)) return true;
		return false;
	}
	
	public boolean contains(Location point) {
		return ready() && point.getBlockX() <= getSoutheast().getBlockX() && point.getBlockX() >= getNorthwest().getBlockX() 
				&& point.getBlockZ() <= getNorthwest().getBlockZ() && point.getBlockZ() >= getSoutheast().getBlockZ();
	}
	

	public List<Team> getTeams() {
		return teams;
	}
	
	public Team getPlayerTeam(String playerName) {
		for(Team team : teams) {
			for(Player player : team.getPlayers()) {
				if(player.getName().equals(playerName)) {
					return team;
				}
			}
		}
		return null;
	}

	public String getName() {
		return name;
	}

	public void setNorthwest(Location northwest) {
		// remove old nw sign, if any (replace with air)
		if(this.northwest != null) {
			removeNorthwest();
		}
		this.northwest = northwest;
		// add sign
		int x = northwest.getBlockX();
		int y = northwest.getBlockY();
		int z = northwest.getBlockZ();
		
		Block block = world.getBlockAt(x, y, z); 
		block.setType(Material.SignPost);
		block.setData((byte)10); // towards southeast
		
//		BUKKIT
//			SignPost sign = (SignPost)complexBlock;
//			sign.setText(0, "Northwest");
//			sign.setText(1, "corner of");
//			sign.setText(2, "warzone");
//			sign.setText(3, name);
		
		saveState();
	}
	
	public void removeNorthwest() {
		int x = northwest.getBlockX();
		int y = northwest.getBlockY();
		int z = northwest.getBlockZ();
		world.getBlockAt(x, y, z).setTypeID(0);
	}

	public Location getNorthwest() {
		return northwest;
	}

	public void setSoutheast(Location southeast) {
		// remove old se sign, if any (replace with air)
		if(this.southeast != null) {
			removeSoutheast();
		}
		this.southeast = southeast;
		// add sign
		int x = southeast.getBlockX();
		int y = southeast.getBlockY();
		int z = southeast.getBlockZ();
		Block block = world.getBlockAt(x, y, z);	// towards northwest
		block.setType(Material.SignPost);
		block.setData((byte)2);;
	
//		BUKKIT
//		SignPost sign = (SignPostblockk;
//		sign.setText(0, "Southeast");
//		sign.setText(1, "corner of");
//		sign.setText(2, "warzone");
//		sign.setText(3, name);
		
		saveState();
	}
	
	public void removeSoutheast() {
		int x = southeast.getBlockX();
		int y = southeast.getBlockY();
		int z = southeast.getBlockZ();
		world.getBlockAt(x, y, z).setTypeID(0);
	}

	public Location getSoutheast() {
		return southeast;
	}

	public void setTeleport(Location location) {
		this.teleport = location;
	}

	public Location getTeleport() {
		return this.teleport;
	}
	
	public int saveState() {
		if(ready()){
			int northSouth = ((int)(southeast.getBlockX())) - ((int)(northwest.getBlockX()));
			int eastWest = ((int)(northwest.getBlockZ())) - ((int)(southeast.getBlockZ()));
			setInitialState(new int[northSouth + 6][128][eastWest + 6]);
			int noOfSavedBlocks = 0;
			int x = (int)northwest.getBlockX() - 2;
			int minY = 0;
			int maxY = 128;
			for(int i = 0; i < northSouth + 3; i++){
				int y = minY;
				for(int j = 0; j < 128; j++) {
					int z = (int)southeast.getBlockZ() - 2;
					for(int k = 0; k < eastWest + 3; k++) {
						getInitialState()[i][j][k] = world.getBlockAt(x, y, z).getTypeID();
						noOfSavedBlocks++;
						z++;
					}
					y++;
				}
				x++;
			}
			return noOfSavedBlocks;
		}
		return 0;
	}
	
	/**
	 * Goes back to the saved state of the warzone (resets only block types, not physics).
	 * Also teleports all players back to their respective spawns.
	 * @return
	 */
	public int resetState() {
		if(ready() && getInitialState() != null){
			
			// reset blocks
			int northSouth = ((int)(southeast.getBlockX())) - ((int)(northwest.getBlockX()));
			int eastWest = ((int)(northwest.getBlockZ())) - ((int)(southeast.getBlockZ()));
			int noOfResetBlocks = 0;
			int noOfFailures = 0;
			int x = northwest.getBlockX() - 2;
			int minY = 0;
			int maxY = 128;
			for(int i = 0; i < northSouth + 3; i++){
				int y = minY;
				for(int j = 0; j < 128; j++) {
					int z = (int)southeast.getBlockZ() - 2;
					for(int k = 0; k < eastWest + 3; k++) {
						Block currentBlock = world.getBlockAt(x, y, z);
						int currentType = currentBlock.getTypeID();
						int initialType = getInitialState()[i][j][k];
						if(currentType != initialType) {	// skip block if nothing changed
							currentBlock.setTypeID(initialType);
							noOfResetBlocks++;
						}
						z++;
					}
					y++;					
				}
				x++;
			}
			
			// everyone back to team spawn with full health
			for(Team team : teams) {
				for(Player player : team.getPlayers()) {
					respawnPlayer(team, player);
				}
				team.setRemainingTickets(lifePool);
				resetSign(team);
			}
			
			// reset monuments
			for(Monument monument : monuments) {
				monument.reset();
			}
			
			this.setNorthwest(this.getNorthwest());
			this.setSoutheast(this.getSoutheast());
			
			return noOfResetBlocks;
		}
		return 0;
	}

	public void endRound() {
		
	}

	public void respawnPlayer(Team team, Player player) {
//		BUKKIT
//		Inventory playerInv = player.getInventory();
//		playerInv.getContents().clear();
//		for(Integer slot : loadout.keySet()) {
//			playerInv.getContents().add(loadout.get(slot));
//			// TODO set the proper slot index
//		}
		
		player.setHealth(20);
		
//		BUKKIT
//		player.setFireTicks(0);
		player.teleportTo(team.getTeamSpawn());
	}

	public boolean isMonumentCenterBlock(Block block) {
		for(Monument monument : monuments) {
			int x = monument.getLocation().getBlockX();
			int y = monument.getLocation().getBlockY();
			int z = monument.getLocation().getBlockZ();
			if(x == block.getX() && y == block.getY() && z == block.getZ()) {
				return true;
			}
		}
		return false;
	}
	
	public Monument getMonumentFromCenterBlock(Block block) {
		for(Monument monument : monuments) {
			int x = monument.getLocation().getBlockX();
			int y = monument.getLocation().getBlockY();
			int z = monument.getLocation().getBlockZ();
			if(x == block.getX() && y == block.getY() && z == block.getZ()) {
				return monument;
			}
		}
		return null;
	}

	public boolean nearAnyOwnedMonument(Location to, Team team) {
		for(Monument monument : monuments) {
			if(monument.isNear(to) && monument.isOwner(team)) {
				return true;
			}
		}
		return false;
	}
	
	public void removeSpawnArea(Team team) {
		// Reset spawn to what it was before the gold blocks
		int[] spawnState = team.getOldSpawnState();
		int x = (int)team.getTeamSpawn().getBlockX();
		int y = (int)team.getTeamSpawn().getBlockY();
		int z = (int)team.getTeamSpawn().getBlockZ();
		
		// center
		world.getBlockAt(x, y, z).setTypeID(spawnState[0]);
		world.getBlockAt(x, y-1, z).setTypeID(spawnState[1]);
		
		// inner ring
		world.getBlockAt(x+1, y-1, z+1).setTypeID(spawnState[2]);
		world.getBlockAt(x+1, y-1, z).setTypeID(spawnState[3]);
		world.getBlockAt(x+1, y-1, z-1).setTypeID(spawnState[4]);
		
		world.getBlockAt(x, y-1, z+1).setTypeID(spawnState[5]);		
		world.getBlockAt(x, y-1, z-1).setTypeID(spawnState[6]);
		
		world.getBlockAt(x-1, y-1, z+1).setTypeID(spawnState[7]);
		world.getBlockAt(x-1, y-1, z).setTypeID(spawnState[8]);
		world.getBlockAt(x-1, y-1, z-1).setTypeID(spawnState[9]);
		
		// outer ring 
		world.getBlockAt(x+2, y-1, z+2).setTypeID(spawnState[10]);
		world.getBlockAt(x+2, y-1, z+1).setTypeID(spawnState[11]);
		world.getBlockAt(x+2, y-1, z).setTypeID(spawnState[12]);
		world.getBlockAt(x+2, y-1, z-1).setTypeID(spawnState[13]);
		world.getBlockAt(x+2, y-1, z-2).setTypeID(spawnState[14]);
		
		world.getBlockAt(x-1, y-1, z+2).setTypeID(spawnState[15]);
		world.getBlockAt(x-1, y-1, z-2).setTypeID(spawnState[16]);
		
		world.getBlockAt(x, y-1, z+2).setTypeID(spawnState[17]);
		world.getBlockAt(x, y-1, z-2).setTypeID(spawnState[18]);
		
		world.getBlockAt(x+1, y-1, z+2).setTypeID(spawnState[19]);
		world.getBlockAt(x+1, y-1, z-2).setTypeID(spawnState[20]);
		
		world.getBlockAt(x-2, y-1, z+2).setTypeID(spawnState[21]);
		world.getBlockAt(x-2, y-1, z+1).setTypeID(spawnState[22]);
		world.getBlockAt(x-2, y-1, z).setTypeID(spawnState[23]);
		world.getBlockAt(x-2, y-1, z-1).setTypeID(spawnState[24]);
		world.getBlockAt(x-2, y-1, z-2).setTypeID(spawnState[25]);
		
	}

	public void addSpawnArea(Team team, Location location, int blockType) {
		// Save the spawn state (i.e. the nine block under the player spawn)
		int[] spawnState = new int[26];
		int x = location.getBlockX();
		int y = location.getBlockY();
		int z = location.getBlockZ();
		// center
		spawnState[0] = world.getBlockAt(x, y, z).getTypeID();
		spawnState[1] = world.getBlockAt(x, y-1, z).getTypeID();
		
		// inner ring
		spawnState[2] = world.getBlockAt(x+1, y-1, z+1).getTypeID();
		spawnState[3] = world.getBlockAt(x+1, y-1, z).getTypeID();
		spawnState[4] = world.getBlockAt(x+1, y-1, z-1).getTypeID();
		
		spawnState[5] = world.getBlockAt(x, y-1, z+1).getTypeID();		
		spawnState[6] = world.getBlockAt(x, y-1, z-1).getTypeID();
		
		spawnState[7] = world.getBlockAt(x-1, y-1, z+1).getTypeID();
		spawnState[8] = world.getBlockAt(x-1, y-1, z).getTypeID();
		spawnState[9] = world.getBlockAt(x-1, y-1, z-1).getTypeID();
		
		// outer ring
		spawnState[10] = world.getBlockAt(x+2, y-1, z+2).getTypeID();
		spawnState[11] = world.getBlockAt(x+2, y-1, z+1).getTypeID();
		spawnState[12] = world.getBlockAt(x+2, y-1, z).getTypeID();
		spawnState[13] = world.getBlockAt(x+2, y-1, z-1).getTypeID();
		spawnState[14] = world.getBlockAt(x+2, y-1, z-2).getTypeID();
		
		spawnState[15] = world.getBlockAt(x-1, y-1, z+2).getTypeID();
		spawnState[16] = world.getBlockAt(x-1, y-1, z-2).getTypeID();
		
		spawnState[17] = world.getBlockAt(x, y-1, z+2).getTypeID();
		spawnState[18] = world.getBlockAt(x, y-1, z-2).getTypeID();
		
		spawnState[19] = world.getBlockAt(x+1, y-1, z+2).getTypeID();
		spawnState[20] = world.getBlockAt(x+1, y-1, z-2).getTypeID();
		
		spawnState[21] = world.getBlockAt(x-2, y-1, z+2).getTypeID();
		spawnState[22] = world.getBlockAt(x-2, y-1, z+1).getTypeID();
		spawnState[23] = world.getBlockAt(x-2, y-1, z).getTypeID();
		spawnState[24] = world.getBlockAt(x-2, y-1, z-1).getTypeID();
		spawnState[25] = world.getBlockAt(x-2, y-1, z-2).getTypeID();
		
		team.setTeamSpawn(location);
		team.setOldSpawnState(spawnState);
		
		// Set the spawn 
		
		// first ring
		world.getBlockAt(x+1, y-1, z+1).setType(Material.LightStone);
		world.getBlockAt(x+1, y-1, z).setType(Material.LightStone);
		world.getBlockAt(x+1, y-1, z-1).setType(Material.LightStone);
		world.getBlockAt(x, y-1, z+1).setType(Material.LightStone);
		world.getBlockAt(x, y-1, z).setType(Material.Stone);
		world.getBlockAt(x, y-1, z-1).setType(Material.LightStone);
		world.getBlockAt(x-1, y-1, z+1).setType(Material.LightStone);
		world.getBlockAt(x-1, y-1, z).setType(Material.LightStone);
		world.getBlockAt(x-1, y-1, z-1).setType(Material.LightStone);
		
		// outer ring
		//world.getBlockAt(x+2, y-1, z+2).setType(Material.Stone);
		world.getBlockAt(x+2, y-1, z+1).setType(Material.Stone);
		world.getBlockAt(x+2, y-1, z).setType(Material.Stone);
		world.getBlockAt(x+2, y-1, z-1).setType(Material.Stone);
		//world.getBlockAt(x+2, y-1, z-2).setType(Material.Stone);
		
		world.getBlockAt(x-1, y-1, z+2).setType(Material.Stone);
		world.getBlockAt(x-1, y-1, z-2).setType(Material.Stone);
		
		world.getBlockAt(x, y-1, z+2).setType(Material.Stone);
		world.getBlockAt(x, y-1, z-2).setType(Material.Stone);
		
		world.getBlockAt(x+1, y-1, z+2).setType(Material.Stone);
		world.getBlockAt(x+1, y-1, z-2).setType(Material.Stone);
		
		//world.getBlockAt(x-2, y-1, z+2).setType(Material.Stone);
		world.getBlockAt(x-2, y-1, z+1).setType(Material.Stone);
		world.getBlockAt(x-2, y-1, z).setType(Material.Stone);
		world.getBlockAt(x-2, y-1, z-1).setType(Material.Stone);
		//world.getBlockAt(x-2, y-1, z-2).setType(Material.Stone);
		
		resetSign(team);
	}
	
	public void resetSign(Team team){
		int x = team.getTeamSpawn().getBlockX();
		int y = team.getTeamSpawn().getBlockY();
		int z = team.getTeamSpawn().getBlockZ();
		
		Block block = world.getBlockAt(x, y, z);
		block.setType(Material.SignPost);
		
//		BUKKIT
//		SignPost sign = (SignPost) block; 
//		sign.setText(0, "Team");
//		sign.setText(1, team.getName());
//		sign.setText(2, team.getPoints() + " pts");
//		sign.setText(3, team.getRemainingTickets() + "/" + lifePool + " lives left");

	}

	public List<Monument> getMonuments() {
		return monuments;
	}

	public boolean getFriendlyFire() {
		// TODO Auto-generated method stub
		return this.friendlyFire;
	}

	public void setLoadout(HashMap<Integer, ItemStack> loadout) {
		this.loadout = loadout;
	}

	public HashMap<Integer, ItemStack> getLoadout() {
		return loadout;
	}

	public void setLifePool(int lifePool) {
		this.lifePool = lifePool;
	}

	public int getLifePool() {
		return lifePool;
	}

	public void setFriendlyFire(boolean ffOn) {
		this.friendlyFire = ffOn;
	}

	public void setInitialState(int[][][] initialState) {
		this.initialState = initialState;
	}

	public int[][][] getInitialState() {
		return initialState;
	}

	public boolean hasPlayerInventory(String playerName) {
		return inventories.containsKey(playerName);
	}

	public void keepPlayerInventory(Player player) {
		// BUKKIT
//		Inventory inventory = player.getInventory();
//		
//		inventories.put(player.getName(), inventory.getContents());
	}

	public void restorePlayerInventory(Player player) {
//		HashMap<Integer,ItemStack> originalContents = inventories.remove((player.getName());
//		Inventory playerInv = player.getInventory(); 
//		playerInv.clearContents();
//		playerInv.update();
//		for(Item item : originalContents) {
//			playerInv.addItem(item);
//		}
//		playerInv.update();
//		player.getInventory().update();
	}

	public boolean hasMonument(String monumentName) {
		boolean hasIt = false;
		for(Monument monument: monuments) {
			if(monument.getName().equals(monumentName)) {
				return true;
			}
		}
		return false;
	}
	
	public Monument getMonument(String monumentName) {
		boolean hasIt = false;
		for(Monument monument: monuments) {
			if(monument.getName().equals(monumentName)) {
				return monument;
			}
		}
		return null;
	}
	
	public boolean isImportantBlock(Block block) {
		block.getX();
		for(Monument m : monuments) {
			if(m.contains(block)){
				return true;
			}
		}
		for(Team t : teams) {
			if(t.contains(block)){
				return true;
			}
		}
		if(teleportNear(block)) {
			return true;
		}
		return false;
	}

	private boolean teleportNear(Block block) {
		int x = (int)this.teleport.getBlockX();
		int y = (int)this.teleport.getBlockY();
		int z = (int)this.teleport.getBlockZ();
		int bx = block.getX();
		int by = block.getY();
		int bz = block.getZ();
		if((bx == x && by == y && bz == z) || 
				(bx == x+1 && by == y-1 && bz == z+1) ||
				(bx == x+1 && by == y-1 && bz == z) ||
				(bx == x+1 && by == y-1 && bz == z-1) ||
				(bx == x && by == y-1 && bz == z+1) ||
				(bx == x && by == y-1 && bz == z) ||
				(bx == x && by == y-1 && bz == z-1) ||
				(bx == x-1 && by == y-1 && bz == z+1) ||
				(bx == x-1 && by == y-1 && bz == z) ||
				(bx == x-1 && by == y-1 && bz == z-1) ) {
			return true;
		}
		return false;
	}

	public World getWorld() {
		
		return world;
	}

	public void setWorld(World world) {
		this.world = world;
	}

	

}
