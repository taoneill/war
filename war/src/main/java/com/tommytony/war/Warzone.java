package com.tommytony.war;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Block;
import org.bukkit.ItemStack;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Player;
import org.bukkit.PlayerInventory;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;

import com.tommytony.war.volumes.VerticalVolume;
import com.tommytony.war.volumes.Volume;

public class Warzone {
	private String name;
	private VerticalVolume volume;
	private Location northwest;
	private Location southeast;
	private final List<Team> teams = new ArrayList<Team>();
	private final List<Monument> monuments = new ArrayList<Monument>();
	
	private Location teleport;
	private boolean friendlyFire;
	private War war;
	private int lifePool;
	private HashMap<Integer, ItemStack> loadout; 
	
	private HashMap<String, List<ItemStack>> inventories = new HashMap<String, List<ItemStack>>();
	private World world;
	
	public Warzone(War war, World world, String name) {
		this.war = war;
		this.world = world;
		this.name = name;
		this.friendlyFire = war.getDefaultFriendlyFire();
		this.setLifePool(war.getDefaultLifepool());
		this.setLoadout(war.getDefaultLoadout());
		this.volume = new VerticalVolume(name, war, this);
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
		this.volume.setCornerOne(world.getBlockAt(northwest.getBlockX(), northwest.getBlockY(), northwest.getBlockZ()));
		
		// add sign
		int x = northwest.getBlockX();
		int y = northwest.getBlockY();
		int z = northwest.getBlockZ();
		
		Block block = world.getBlockAt(x, y, z); 
		block.setType(Material.SignPost);
		block.setData((byte)10); // towards southeast
		
		BlockState state = block.getState();
		Sign sign = (Sign)state;
		sign.setLine(0, "Northwest");
		sign.setLine(1, "corner of");
		sign.setLine(2, "warzone");
		sign.setLine(3, name);
		state.update();
	
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
		this.volume.setCornerTwo(world.getBlockAt(southeast.getBlockX(), southeast.getBlockY(), southeast.getBlockZ()));
		// add sign
		int x = southeast.getBlockX();
		int y = southeast.getBlockY();
		int z = southeast.getBlockZ();
		Block block = world.getBlockAt(x, y, z);	// towards northwest
		block.setType(Material.SignPost);
		block.setData((byte)2);;
	
		BlockState state = block.getState();
		Sign sign = (Sign)state;
		sign.setLine(0, "Southeast");
		sign.setLine(1, "corner of");
		sign.setLine(2, "warzone");
		sign.setLine(3, name);
		state.update();
		
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
			return volume.saveBlocks();
		}
		return 0;
	}
	
	/**
	 * Goes back to the saved state of the warzone (resets only block types, not physics).
	 * Also teleports all players back to their respective spawns.
	 * @return
	 */
	public int resetState() {
		if(ready() && volume.isSaved()){
			int reset = volume.resetBlocks();
			
			// everyone back to team spawn with full health
			for(Team team : teams) {
				for(Player player : team.getPlayers()) {
					respawnPlayer(team, player);
				}
				team.setRemainingTickets(lifePool);
				team.getVolume().resetBlocks();
				team.resetSign();
			}
			
			// reset monuments
			for(Monument monument : monuments) {
				monument.remove();
				monument.addMonumentBlocks();
			}
			
			this.setNorthwest(this.getNorthwest());
			this.setSoutheast(this.getSoutheast());
			
			return reset;
		}
		return 0;
	}

	public void endRound() {
		
	}

	public void respawnPlayer(Team team, Player player) {
		// Reset inventory to loadout
		PlayerInventory playerInv = player.getInventory();
		for(int i = 0; i < playerInv.getSize(); i++){
			playerInv.setItem(i, new ItemStack(Material.Air));	
		}
		for(Integer slot : loadout.keySet()) {
			playerInv.setItem(slot, loadout.get(slot));
		}
		
		player.setHealth(20);
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
	
	public List<Monument> getMonuments() {
		return monuments;
	}

	public boolean getFriendlyFire() {
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

	public boolean hasPlayerInventory(String playerName) {
		return inventories.containsKey(playerName);
	}

	public void keepPlayerInventory(Player player) {
		PlayerInventory inventory = player.getInventory();
		List<ItemStack> invToStore = new ArrayList<ItemStack>();
		for(int i=0; i < inventory.getSize(); i++) {
			invToStore.set(i, inventory.getItem(i));
		}
		inventories.put(player.getName(), invToStore);
	}

	public void restorePlayerInventory(Player player) {
		List<ItemStack> originalContents = inventories.remove(player.getName());
		PlayerInventory playerInv = player.getInventory();
		for(int i = 0; i < playerInv.getSize(); i++) {
			playerInv.setItem(i, new ItemStack(Material.Air));
		}
		for(int i = 0; i < playerInv.getSize(); i++) {
			playerInv.setItem(i, originalContents.get(i));
		}
	}

	public boolean hasMonument(String monumentName) {
		for(Monument monument: monuments) {
			if(monument.getName().equals(monumentName)) {
				return true;
			}
		}
		return false;
	}
	
	public Monument getMonument(String monumentName) {
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
			if(m.getVolume().contains(block)){
				return true;
			}
		}
		for(Team t : teams) {
			if(t.getVolume().contains(block)){
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

	public Volume getVolume() {
		return volume;
	}

	public void setVolume(VerticalVolume zoneVolume) {
		this.volume = zoneVolume;
	}
	
	public Team getTeamByMaterial(Material material) {
		for(Team t : teams) {
			if(t.getMaterial().getID() == material.getID()) {
				return t;
			}
		}
		return null;
	}

	

}
