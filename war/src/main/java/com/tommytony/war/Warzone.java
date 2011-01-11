package com.tommytony.war;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Block;
import org.bukkit.BlockFace;
import org.bukkit.ItemStack;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Player;
import org.bukkit.PlayerInventory;
import org.bukkit.World;

import bukkit.tommytony.war.War;

import com.tommytony.war.volumes.VerticalVolume;
import com.tommytony.war.volumes.Volume;

/**
 * 
 * @author tommytony
 *
 */
public class Warzone {
	private String name;
	private VerticalVolume volume;
	private Location northwest;
	private Location southeast;
	private final List<Team> teams = new ArrayList<Team>();
	private final List<Monument> monuments = new ArrayList<Monument>();
	
	private Location teleport;
	private boolean friendlyFire;
	private int lifePool;
	private HashMap<Integer, ItemStack> loadout; 
	
	private HashMap<String, List<ItemStack>> inventories = new HashMap<String, List<ItemStack>>();
	private World world;
	
	public Warzone(War war, World world, String name) {
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
		this.northwest = northwest;
		this.volume.setCornerOne(world.getBlockAt(northwest.getBlockX(), northwest.getBlockY(), northwest.getBlockZ()));
	}


	public Location getNorthwest() {
		return northwest;
	}

	public void setSoutheast(Location southeast) {
		this.southeast = southeast;
		this.volume.setCornerTwo(world.getBlockAt(southeast.getBlockX(), southeast.getBlockY(), southeast.getBlockZ()));		
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
	public void initializeZone() {
		if(ready() && volume.isSaved()){			
			// get surface corners
			volume.getMinX();
			volume.getMinZ();
			volume.getMinY();
			int c1maxY = world.getHighestBlockYAt(volume.getMinX(), volume.getMinZ());
			int c2maxY = world.getHighestBlockYAt(volume.getMaxX(), volume.getMaxZ());
			Block ne = world.getBlockAt(volume.getMinX(), c1maxY, volume.getMinZ());
			Block nw = world.getBlockAt(volume.getMinX(), c2maxY, volume.getMaxZ());
			Block sw = world.getBlockAt(volume.getMinX(), c1maxY, volume.getMaxZ());
			Block se = world.getBlockAt(volume.getMaxX(), c2maxY, volume.getMinZ());
			
			// add north wall, ne - nw
			Block lastBlock = null;
			for(int z = volume.getMinZ(); z < volume.getMaxZ(); z++) {
				lastBlock = highestBlockToGlass(ne.getX(), z, lastBlock);
			}
			
			// add east, ne - se
			lastBlock = null;
			for(int x = volume.getMinX(); x < volume.getMaxX(); x++) {
				lastBlock = highestBlockToGlass(x, ne.getZ(), lastBlock);
			}
			
			// add south, se - sw
			lastBlock = null;
			for(int z = volume.getMinZ(); z < volume.getMaxZ(); z++) {
				lastBlock = highestBlockToGlass(se.getX(), z, lastBlock);
			}
			
			// add west, nw - sw
			for(int x = volume.getMinX(); x < volume.getMaxX(); x++) {
				lastBlock = highestBlockToGlass(x, nw.getZ(), lastBlock);
			}
			
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
		}
	}

	private Block highestBlockToGlass(int x, int z, Block lastBlock) {
		int highest = world.getHighestBlockYAt(x, z);
		Block block = world.getBlockAt(x, highest -1 , z);
		
		if(block.getType() == Material.Leaves) { // top of tree, lets find some dirt
			Block over = block.getFace(BlockFace.Down);
			Block under = over.getFace(BlockFace.Down);
			int treeHeight = 0;
			while(!((over.getType() == Material.Air || over.getType() == Material.Leaves || over.getType() == Material.Wood)
					&& (under.getType() == Material.Grass || under.getType() == Material.Dirt || under.getType() == Material.Stone))
				  && treeHeight < 40) {
				over = under;
				under = over.getFace(BlockFace.Down);
				treeHeight++;
			}
			block = under; // found the ground
		}
		
		block.setType(Material.Glass);

		if(lastBlock != null) {
			// link the new block and the old vertically if there's a big drop or rise
			if(block.getY() - lastBlock.getY() > 2) {  // new block too high 
				Block under = block.getFace(BlockFace.Down);
				while(under.getY() != lastBlock.getY() - 1) {
					block.setType(Material.Glass);
					under = under.getFace(BlockFace.Down);
				}
			} else if (block.getY() - lastBlock.getY() < -2) { // new block too low
				Block over = block.getFace(BlockFace.Up);
				while(over.getY() != lastBlock.getY() + 1) {
					block.setType(Material.Glass);
					over = over.getFace(BlockFace.Up);
				}
			}
		}

		return block;
	}

	public void endRound() {
		
	}

	public void respawnPlayer(Team team, Player player) {
		player.teleportTo(team.getTeamSpawn());
		
		// Reset inventory to loadout
		PlayerInventory playerInv = player.getInventory();
		for(int i = 0; i < playerInv.getSize(); i++){
			playerInv.setItem(i, null);	
		}
		for(Integer slot : loadout.keySet()) {
			if(slot == 100) {
				playerInv.setBoots(loadout.get(slot));
			} else if(slot == 101) {
				playerInv.setLeggings(loadout.get(slot));
			} else if(slot == 102) {
				playerInv.setChestplate(loadout.get(slot));
			} else if(slot == 103) {
				playerInv.setHelmet(loadout.get(slot));
			} else { 
				playerInv.setItem(slot, loadout.get(slot));
			}
		}
		
		player.setHealth(20);
		
	}

	public boolean isMonumentCenterBlock(Block block) {
		for(Monument monument : monuments) {
			int x = monument.getLocation().getBlockX();
			int y = monument.getLocation().getBlockY() - 1;
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
			invToStore.add(i, inventory.getItem(i));
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
		if(teleport != null) {
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
