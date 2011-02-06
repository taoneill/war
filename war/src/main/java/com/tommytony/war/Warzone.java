package com.tommytony.war;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.minecraft.server.Entity;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import bukkit.tommytony.war.War;

import com.tommytony.war.volumes.VerticalVolume;

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
	private HashMap<Integer, ItemStack> loadout = new HashMap<Integer, ItemStack>(); 
	private boolean drawZoneOutline;
	private int teamCap = 5;
	private int scoreCap = 5;
	
	private HashMap<String, InventoryStash> inventories = new HashMap<String, InventoryStash>();
	private HashMap<String, Team> flagThieves = new HashMap<String, Team>();
	private World world;
	private Material[] originalSoutheastBlocks;
	private Material[] originalNorthwestBlocks;
	private final int minSafeDistanceFromWall = 5;
	private List<ZoneWallGuard> zoneWallGuards = new ArrayList<ZoneWallGuard>();
	private War war;
	private ZoneLobby lobby;
	private boolean autoAssignOnly;
	private boolean blockHeads;
	private boolean dropLootOnDeath;
	
	
	public Warzone(War war, World world, String name) {
		this.world = world;
		this.war = war;
		this.name = name;
		this.friendlyFire = war.getDefaultFriendlyFire();
		this.setLifePool(war.getDefaultLifepool());
		this.setLoadout(war.getDefaultLoadout());
		this.setDrawZoneOutline(war.getDefaultDrawZoneOutline());
		this.setAutoAssignOnly(war.getDefaultAutoAssignOnly());
		this.teamCap = war.getDefaultTeamCap();
		this.scoreCap = war.getDefaultScoreCap();
		this.setBlockHeads(war.isDefaultBlockHeads());
		this.setDropLootOnDeath(war.isDefaultDropLootOnDeath());
		this.volume = new VerticalVolume(name, war, this.getWorld());
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
		if((getSoutheast().getBlockX() - getNorthwest().getBlockX() > 500)
				|| (getNorthwest().getBlockZ() - getSoutheast().getBlockZ() > 500)) return true;
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
		resetNorthwestCursorBlocks();
		this.northwest = northwest;
		this.volume.setCornerOne(world.getBlockAt(northwest.getBlockX(), northwest.getBlockY(), northwest.getBlockZ()));
		addNorthwestCursorBlocks();
	}
	
	private void addNorthwestCursorBlocks() {
		int newHighest = this.world.getHighestBlockYAt(this.northwest.getBlockX(), this.northwest.getBlockZ()) - 1;
		Block topNWBlock = this.world.getBlockAt(this.northwest.getBlockX(), newHighest, this.northwest.getBlockZ());
		originalNorthwestBlocks = new Material[3];
		originalNorthwestBlocks[0] = topNWBlock.getType();	// save blocks for reset
		originalNorthwestBlocks[1] = topNWBlock.getFace(BlockFace.EAST).getType();
		originalNorthwestBlocks[2] = topNWBlock.getFace(BlockFace.SOUTH).getType();
		topNWBlock.setType(Material.GLASS);
		topNWBlock.getFace(BlockFace.EAST).setType(Material.GLASS);
		topNWBlock.getFace(BlockFace.SOUTH).setType(Material.GLASS);
	}
	
	private void resetNorthwestCursorBlocks() {
		if(this.northwest != null && originalNorthwestBlocks != null) {
			// reset old corner
			Block oldTopNWBlock = this.world.getBlockAt(this.northwest.getBlockX(), this.northwest.getBlockY() - 1, this.northwest.getBlockZ());
			oldTopNWBlock.setType(originalNorthwestBlocks[0]);
			oldTopNWBlock.getFace(BlockFace.EAST).setType(originalNorthwestBlocks[1]);
			oldTopNWBlock.getFace(BlockFace.SOUTH).setType(originalNorthwestBlocks[2]);
		}
	}

	public Location getNorthwest() {
		return northwest;
	}

	public void setSoutheast(Location southeast) {
		resetSoutheastCursorBlocks();
		this.southeast = southeast;
		this.volume.setCornerTwo(world.getBlockAt(southeast.getBlockX(), southeast.getBlockY(), southeast.getBlockZ()));
		addSoutheastCursorBlocks();
	}
	
	private void addSoutheastCursorBlocks() {
		int newHighest = this.world.getHighestBlockYAt(this.southeast.getBlockX(), this.southeast.getBlockZ()) - 1;
		Block topSEBlock = this.world.getBlockAt(this.southeast.getBlockX(), newHighest, this.southeast.getBlockZ());
		originalSoutheastBlocks = new Material[3];
		originalSoutheastBlocks[0] = topSEBlock.getType();	// save block for reset
		originalSoutheastBlocks[1] = topSEBlock.getFace(BlockFace.WEST).getType();
		originalSoutheastBlocks[2] = topSEBlock.getFace(BlockFace.NORTH).getType();
		topSEBlock.setType(Material.GLASS);
		topSEBlock.getFace(BlockFace.WEST).setType(Material.GLASS);
		topSEBlock.getFace(BlockFace.NORTH).setType(Material.GLASS);
	}
	
	private void resetSoutheastCursorBlocks() {
		if(this.southeast != null && originalSoutheastBlocks != null) {
			// reset old corner
			Block oldTopSEBlock = this.world.getBlockAt(this.southeast.getBlockX(), this.southeast.getBlockY() - 1, this.southeast.getBlockZ());
			oldTopSEBlock.setType(originalSoutheastBlocks[0]);
			oldTopSEBlock.getFace(BlockFace.WEST).setType(originalSoutheastBlocks[1]);
			oldTopSEBlock.getFace(BlockFace.NORTH).setType(originalSoutheastBlocks[2]);
		}
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
	
	public int saveState(boolean clearArtifacts) {
		if(ready()){
			if(clearArtifacts) {
				// removed everything to keep save clean
				volume.resetWallBlocks(BlockFace.EAST);
				volume.resetWallBlocks(BlockFace.WEST);
				volume.resetWallBlocks(BlockFace.NORTH);
				volume.resetWallBlocks(BlockFace.SOUTH);
				
				for(Team team : teams) {
					team.getSpawnVolume().resetBlocks();
					if(team.getTeamFlag() != null) team.getFlagVolume().resetBlocks();
				}
				
				for(Monument monument : monuments) {
					monument.getVolume().resetBlocks();
				}
				
				if(lobby != null) {
					lobby.getVolume().resetBlocks();
				}
			}
			
			
			
			int saved = volume.saveBlocks();
			if(clearArtifacts) {
				initializeZone();	// bring back stuff
			}
			return saved;
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
			// everyone back to team spawn with full health
			for(Team team : teams) {
				for(Player player : team.getPlayers()) {
					respawnPlayer(team, player);
				}
				team.setRemainingLives(lifePool);
				team.setTeamSpawn(team.getTeamSpawn());
				if(team.getTeamFlag() != null) team.setTeamFlag(team.getTeamFlag());
				//team.resetSign();
			}
			
			initZone();
		}
	}
	
	public void initializeZone(PlayerMoveEvent event) {
		if(ready() && volume.isSaved()){			
			// everyone back to team spawn with full health
			for(Team team : teams) {
				for(Player p : team.getPlayers()) {
					if(p.getName().equals(event.getPlayer().getName())) { 
						respawnPlayer(event, team, event.getPlayer());
					} else {
						respawnPlayer(team, p);
					}
				}
				team.setRemainingLives(lifePool);
				team.setTeamSpawn(team.getTeamSpawn());
				if(team.getTeamFlag() != null) team.setTeamFlag(team.getTeamFlag());
				team.resetSign();
			}
			
			initZone();
		}
	}
	
	private void initZone() {
		// add wall outlines
		if(isDrawZoneOutline()) {
			addZoneOutline(BlockFace.NORTH);
			addZoneOutline(BlockFace.EAST);
			addZoneOutline(BlockFace.SOUTH);
			addZoneOutline(BlockFace.WEST);
		}
		
		// reset monuments
		for(Monument monument : monuments) {
			monument.getVolume().resetBlocks();
			monument.addMonumentBlocks();
		}

		// reset lobby
		if(lobby != null) {
			lobby.initialize();
		}
		
		this.setNorthwest(this.getNorthwest());
		this.setSoutheast(this.getSoutheast());
		
		this.flagThieves.clear();
	}

	public void addZoneOutline(BlockFace wall) {
		int c1maxY = world.getHighestBlockYAt(volume.getMinX(), volume.getMinZ());
		int c2maxY = world.getHighestBlockYAt(volume.getMaxX(), volume.getMaxZ());
		Block ne = world.getBlockAt(volume.getMinX(), c1maxY, volume.getMinZ());
		Block nw = world.getBlockAt(volume.getMinX(), c2maxY, volume.getMaxZ());
		Block se = world.getBlockAt(volume.getMaxX(), c2maxY, volume.getMinZ());
		Block lastBlock = null;
		if(BlockFace.NORTH == wall) {
			for(int z = volume.getMinZ(); z < volume.getMaxZ(); z++) {
				lastBlock = highestBlockToGlass(ne.getX(), z, lastBlock);
			}
		} else if (BlockFace.EAST == wall) {
			for(int x = volume.getMinX(); x < volume.getMaxX(); x++) {
				lastBlock = highestBlockToGlass(x, ne.getZ(), lastBlock);
			}
		} else if (BlockFace.SOUTH == wall) {
			for(int z = volume.getMinZ(); z < volume.getMaxZ(); z++) {
				lastBlock = highestBlockToGlass(se.getX(), z, lastBlock);
			}
		} else if (BlockFace.WEST == wall) {
			for(int x = volume.getMinX(); x < volume.getMaxX(); x++) {
				lastBlock = highestBlockToGlass(x, nw.getZ(), lastBlock);
			}
		}
	}

	private Block highestBlockToGlass(int x, int z, Block lastBlock) {
		int highest = world.getHighestBlockYAt(x, z);
		Block block = world.getBlockAt(x, highest -1 , z);
		
		if(block.getType() == Material.LEAVES) { // top of tree, lets find some dirt/ground
			Block over = block.getFace(BlockFace.DOWN);
			Block under = over.getFace(BlockFace.DOWN);
			int treeHeight = 0;
			while(!((over.getType() == Material.AIR && under.getType() != Material.AIR && under.getType() != Material.LEAVES) 
					|| (over.getType() == Material.LEAVES && under.getType() != Material.LEAVES && under.getType() != Material.AIR)
					|| (over.getType() == Material.WOOD && under.getType() != Material.WOOD && under.getType() != Material.AIR))
				  && treeHeight < 40) {
				over = under;
				if(over.getY() <= 0) break; 	// reached bottom
				under = over.getFace(BlockFace.DOWN);
				treeHeight++;
			}
			block = under; // found the ground
		}
		
		block.setType(Material.GLASS);

		if(lastBlock != null) {
			// link the new block and the old vertically if there's a big drop or rise
			if(block.getY() - lastBlock.getY() > 1) {  // new block too high 
				Block under = block.getFace(BlockFace.DOWN);
				while(under.getY() != lastBlock.getY() - 1) {
					under.setType(Material.GLASS);
					if(under.getY() <= 0) break;	// reached bottom
					under = under.getFace(BlockFace.DOWN);
				}
			} else if (lastBlock.getY() - block.getY() > 1) { // new block too low
				Block over = block.getFace(BlockFace.UP);
				while(over.getY() != lastBlock.getY() + 1) {
					over.setType(Material.GLASS);
					if(over.getY() >= 127) break;
					over = over.getFace(BlockFace.UP);
				}
			}
		}

		return block;
	}

	public void endRound() {
		
	}

	public void respawnPlayer(Team team, Player player) {
		handleRespawn(team, player);
		
	}
	
	public void respawnPlayer(PlayerMoveEvent event, Team team, Player player) {
		event.setFrom(team.getTeamSpawn());
		handleRespawn(team, player);
		
		event.setCancelled(true);
	}
	
	private void handleRespawn(Team team, Player player){
		// Reset inventory to loadout
		PlayerInventory playerInv = player.getInventory();
		playerInv.clear();
		playerInv.clear(playerInv.getSize() + 0);
		playerInv.clear(playerInv.getSize() + 1);
		playerInv.clear(playerInv.getSize() + 2);
		playerInv.clear(playerInv.getSize() + 3);	// helmet/blockHead
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
				playerInv.addItem(loadout.get(slot));
			}
		}
		if(isBlockHeads()) {
			playerInv.setHelmet(new ItemStack(team.getMaterial()));
		} else {
			if(team.getMaterial() == Material.GOLD_BLOCK) {
				playerInv.setHelmet(new ItemStack(Material.GOLD_HELMET));
			} else if (team.getMaterial() == Material.DIAMOND_BLOCK) {
				playerInv.setHelmet(new ItemStack(Material.DIAMOND_HELMET));
			} else if (team.getMaterial() == Material.IRON_BLOCK) {
				playerInv.setHelmet(new ItemStack(Material.IRON_HELMET));
			}
		}
		
		// Fill hp
		player.setHealth(20);

		// Teleport the player back to spawn
		Location playerLoc = player.getLocation();
		int x = playerLoc.getBlockX();
		int y = playerLoc.getBlockY();
		int z = playerLoc.getBlockZ();
		Block playerBlock = world.getBlockAt(x, y, z).getFace(BlockFace.UP);
		Material playerBlockType = playerBlock.getType();
		if(playerBlockType.getId() == Material.WATER.getId()
				|| playerBlockType.getId() == Material.STATIONARY_WATER.getId()) {
			// If in water, make arbitrary adjustments to fix drowning deaths causing "Player moved wrongly!" error
			player.teleportTo(new Location(playerLoc.getWorld(),
					team.getTeamSpawn().getX(), team.getTeamSpawn().getY() + 3, team.getTeamSpawn().getZ()));
		} else {
			player.teleportTo(team.getTeamSpawn());
		}
	}

	public boolean isMonumentCenterBlock(Block block) {
		for(Monument monument : monuments) {
			int x = monument.getLocation().getBlockX();
			int y = monument.getLocation().getBlockY() + 1;
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
			int y = monument.getLocation().getBlockY() + 1;
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

	public void setLoadout(HashMap<Integer, ItemStack> newLoadout) {
		this.loadout.clear();
		for(Integer slot : newLoadout.keySet()) {
			ItemStack stack = newLoadout.get(slot);
			this.loadout.put(slot, stack);
		}
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
		ItemStack[] contents = inventory.getContents();
		inventories.put(player.getName(), new InventoryStash(contents, inventory.getHelmet(), inventory.getChestplate(), 
																inventory.getLeggings(), inventory.getBoots()));	
	}

	public void restorePlayerInventory(Player player) {
		InventoryStash originalContents = inventories.remove(player.getName());
		PlayerInventory playerInv = player.getInventory();
		if(originalContents != null) {
			playerInv.clear();
			playerInv.clear(playerInv.getSize() + 0);
			playerInv.clear(playerInv.getSize() + 1);
			playerInv.clear(playerInv.getSize() + 2);
			playerInv.clear(playerInv.getSize() + 3);	// helmet/blockHead
			for(ItemStack item : originalContents.getContents()) {
				if(item.getTypeId() != 0) {
					playerInv.addItem(item);
				}
			}
			if(originalContents.getHelmet() != null) {
				playerInv.setHelmet(originalContents.getHelmet());
			}
			if(originalContents.getChest() != null) {
				playerInv.setChestplate(originalContents.getChest());
			}
			if(originalContents.getLegs() != null) {
				playerInv.setLeggings(originalContents.getLegs());
			}
			if(originalContents.getFeet() != null) {
				playerInv.setBoots(originalContents.getFeet());
			}
		}
		
	}
	
	public InventoryStash getPlayerInventory(String playerName) {
		if(inventories.containsKey(playerName)) return inventories.get(playerName);
		return null;
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
			if(t.getSpawnVolume().contains(block)){
				return true;
			} else if (t.getFlagVolume() != null 
					&& t.getFlagVolume().contains(block)) {
				return true;
			}
		}
		if(volume.isWallBlock(block)){
			return true;
		}
//		if(lobby != null) {
//			lobby.getVolume().contains(block);
//		}
		return false;
	}

//	private boolean teleportNear(Block block) {
//		if(teleport != null) {
//			int x = (int)this.teleport.getBlockX();
//			int y = (int)this.teleport.getBlockY();
//			int z = (int)this.teleport.getBlockZ();
//			int bx = block.getX();
//			int by = block.getY();
//			int bz = block.getZ();
//			if((bx == x && by == y && bz == z) || 
//					(bx == x+1 && by == y-1 && bz == z+1) ||
//					(bx == x+1 && by == y-1 && bz == z) ||
//					(bx == x+1 && by == y-1 && bz == z-1) ||
//					(bx == x && by == y-1 && bz == z+1) ||
//					(bx == x && by == y-1 && bz == z) ||
//					(bx == x && by == y-1 && bz == z-1) ||
//					(bx == x-1 && by == y-1 && bz == z+1) ||
//					(bx == x-1 && by == y-1 && bz == z) ||
//					(bx == x-1 && by == y-1 && bz == z-1) ) {
//				return true;
//			}
//		}
//		return false;
//	}

	public World getWorld() {
		
		return world;
	}

	public void setWorld(World world) {
		this.world = world;
	}

	public VerticalVolume getVolume() {
		return volume;
	}

	public void setVolume(VerticalVolume zoneVolume) {
		this.volume = zoneVolume;
	}
	
	public Team getTeamByMaterial(Material material) {
		for(Team t : teams) {
			if(t.getMaterial().getId() == material.getId()) {
				return t;
			}
		}
		return null;
	}

	public boolean isNearWall(Location latestPlayerLocation) {
		if(volume.hasTwoCorners()) {
			if(Math.abs(southeast.getBlockZ() - latestPlayerLocation.getBlockZ()) < minSafeDistanceFromWall 
					&& latestPlayerLocation.getBlockX() <= southeast.getBlockX()
					&& latestPlayerLocation.getBlockX() >= northwest.getBlockX()) {
				return true; 	// near east wall
			} else if (Math.abs(southeast.getBlockX() - latestPlayerLocation.getBlockX()) < minSafeDistanceFromWall
					&& latestPlayerLocation.getBlockZ() <= northwest.getBlockZ()
					&& latestPlayerLocation.getBlockZ() >= southeast.getBlockZ()) {
				return true;	// near south wall
			} else if (Math.abs(northwest.getBlockX() - latestPlayerLocation.getBlockX()) < minSafeDistanceFromWall
					&& latestPlayerLocation.getBlockZ() <= northwest.getBlockZ()
					&& latestPlayerLocation.getBlockZ() >= southeast.getBlockZ()) {
				return true;	// near north wall
			} else if (Math.abs(northwest.getBlockZ() - latestPlayerLocation.getBlockZ()) < minSafeDistanceFromWall
					&& latestPlayerLocation.getBlockX() <= southeast.getBlockX()
					&& latestPlayerLocation.getBlockX() >= northwest.getBlockX()) {
				return true;	// near west wall
			}
		}
		return false;
	}
	
	public List<Block> getNearestWallBlocks(Location latestPlayerLocation) {
		List<Block> nearestWallBlocks = new ArrayList<Block>();
		if(Math.abs(southeast.getBlockZ() - latestPlayerLocation.getBlockZ()) < minSafeDistanceFromWall 
				&& latestPlayerLocation.getBlockX() <= southeast.getBlockX()
				&& latestPlayerLocation.getBlockX() >= northwest.getBlockX()) {
			// near east wall
			Block eastWallBlock = world.getBlockAt(latestPlayerLocation.getBlockX() + 1, latestPlayerLocation.getBlockY(), southeast.getBlockZ());
			nearestWallBlocks.add(eastWallBlock);
		}
		
		if (Math.abs(southeast.getBlockX() - latestPlayerLocation.getBlockX()) < minSafeDistanceFromWall
				&& latestPlayerLocation.getBlockZ() <= northwest.getBlockZ()
				&& latestPlayerLocation.getBlockZ() >= southeast.getBlockZ()) {
			// near south wall
			Block southWallBlock = world.getBlockAt(southeast.getBlockX(), latestPlayerLocation.getBlockY() + 1, latestPlayerLocation.getBlockZ());
			nearestWallBlocks.add(southWallBlock);
		}
		
		if (Math.abs(northwest.getBlockX() - latestPlayerLocation.getBlockX()) < minSafeDistanceFromWall
				&& latestPlayerLocation.getBlockZ() <= northwest.getBlockZ()
				&& latestPlayerLocation.getBlockZ() >= southeast.getBlockZ()) {
			// near north wall
			Block northWallBlock = world.getBlockAt(northwest.getBlockX(), latestPlayerLocation.getBlockY() + 1, latestPlayerLocation.getBlockZ());
			nearestWallBlocks.add(northWallBlock);
		}
		
		if (Math.abs(northwest.getBlockZ() - latestPlayerLocation.getBlockZ()) < minSafeDistanceFromWall
				&& latestPlayerLocation.getBlockX() <= southeast.getBlockX()
				&& latestPlayerLocation.getBlockX() >= northwest.getBlockX()) {
			// near west wall
			Block westWallBlock = world.getBlockAt(latestPlayerLocation.getBlockX(), latestPlayerLocation.getBlockY() + 1, northwest.getBlockZ());
			nearestWallBlocks.add(westWallBlock);
		}
		return nearestWallBlocks;
		// note: y + 1 to line up 3 sided square with player eyes
	}
	
	public List<BlockFace> getNearestWalls(Location latestPlayerLocation) {
		List<BlockFace> walls = new ArrayList<BlockFace>();
		if(Math.abs(southeast.getBlockZ() - latestPlayerLocation.getBlockZ()) < minSafeDistanceFromWall 
				&& latestPlayerLocation.getBlockX() <= southeast.getBlockX()
				&& latestPlayerLocation.getBlockX() >= northwest.getBlockX()) {
			// near east wall
			walls.add(BlockFace.EAST);
		} 
		
		if (Math.abs(southeast.getBlockX() - latestPlayerLocation.getBlockX()) < minSafeDistanceFromWall
				&& latestPlayerLocation.getBlockZ() <= northwest.getBlockZ()
				&& latestPlayerLocation.getBlockZ() >= southeast.getBlockZ()) {
			// near south wall
			walls.add(BlockFace.SOUTH); 	
		}

		if (Math.abs(northwest.getBlockX() - latestPlayerLocation.getBlockX()) < minSafeDistanceFromWall
				&& latestPlayerLocation.getBlockZ() <= northwest.getBlockZ()
				&& latestPlayerLocation.getBlockZ() >= southeast.getBlockZ()) {
			// near north wall
			walls.add(BlockFace.NORTH);
		} 

		if (Math.abs(northwest.getBlockZ() - latestPlayerLocation.getBlockZ()) < minSafeDistanceFromWall
				&& latestPlayerLocation.getBlockX() <= southeast.getBlockX()
				&& latestPlayerLocation.getBlockX() >= northwest.getBlockX()) {
			// near west wall
			walls.add(BlockFace.WEST);
		}
		return walls;
	}
	
	public ZoneWallGuard getPlayerZoneWallGuard(String name, BlockFace wall) {
		for(ZoneWallGuard guard : zoneWallGuards) {
			if(guard.getPlayer().getName().equals(name) && wall == guard.getWall()) {
				return guard;
			}
		}
		return null;
		
	}

	public void protectZoneWallAgainstPlayer(Player player) {
		List<BlockFace> nearestWalls = getNearestWalls(player.getLocation());
		for(BlockFace wall : nearestWalls) {
			ZoneWallGuard guard = getPlayerZoneWallGuard(player.getName(), wall);
			if(guard != null) { 
				// already protected, need to move the guard
				guard.updatePlayerPosition(player.getLocation());
			} else {
				// new guard
				guard = new ZoneWallGuard(player, war, this, wall);
				zoneWallGuards.add(guard);
			}
		}
	}
	
	public void dropZoneWallGuardIfAny(Player player) {
		List<ZoneWallGuard> playerGuards = new ArrayList<ZoneWallGuard>();
		for(ZoneWallGuard guard : zoneWallGuards) {
			if(guard.getPlayer().getName().equals(player.getName())){
				playerGuards.add(guard);
				int reset = volume.resetWallBlocks(guard.getWall()); // this should restore old blocks
				if(isDrawZoneOutline()) {
					addZoneOutline(guard.getWall());
				}
				if(lobby != null) {
					lobby.getVolume().resetBlocks(); // always reset the lobby even if the guard is on another wall
													// because player can go around corner
					lobby.initialize();
				}
				//war.getLogger().info("Reset " + reset + " blocks in " + guard.getWall() + " wall of warzone " + name);
			}
		}
		// now remove those zone guards
		for(ZoneWallGuard playerGuard : playerGuards) {
			zoneWallGuards.remove(playerGuard);
		}
		playerGuards.clear();
	}

	public boolean getAutoAssignOnly() {
		
		return isAutoAssignOnly();
	}

	public void setLobby(ZoneLobby lobby) {
		this.lobby = lobby;
	}

	public ZoneLobby getLobby() {
		return lobby;
	}

	public void autoAssign(PlayerMoveEvent event, Player player) {
		Team lowestNoOfPlayers = null;
		for(Team t : teams) {
			if(lowestNoOfPlayers == null
					|| (lowestNoOfPlayers != null && lowestNoOfPlayers.getPlayers().size() > t.getPlayers().size())) {
				lowestNoOfPlayers = t;
			}
		}
		if(lowestNoOfPlayers != null) {
			lowestNoOfPlayers.addPlayer(player);
			lowestNoOfPlayers.resetSign();
			if(!hasPlayerInventory(player.getName())) {
				keepPlayerInventory(player);
			}
			player.sendMessage(war.str("Your inventory is is storage until you /leave."));
			respawnPlayer(event, lowestNoOfPlayers, player);
			for(Team team : teams){
				team.teamcast(war.str("" + player.getName() + " joined team " + lowestNoOfPlayers.getName() + "."));
			}
		}
	}
	
	public void autoAssign(Player player) {
		Team lowestNoOfPlayers = null;
		for(Team t : teams) {
			if(lowestNoOfPlayers == null
					|| (lowestNoOfPlayers != null && lowestNoOfPlayers.getPlayers().size() > t.getPlayers().size())) {
				lowestNoOfPlayers = t;
			}
		}
		if(lowestNoOfPlayers != null) {
			lowestNoOfPlayers.addPlayer(player);
			lowestNoOfPlayers.resetSign();
			if(!hasPlayerInventory(player.getName())) {
				keepPlayerInventory(player);
			}
			player.sendMessage(war.str("Your inventory is is storage until you /leave."));
			respawnPlayer(lowestNoOfPlayers, player);
			for(Team team : teams){
				team.teamcast(war.str("" + player.getName() + " joined team " + team.getName() + "."));
			}
		}
	}

	public void setTeamCap(int teamCap) {
		this.teamCap = teamCap;
	}

	public int getTeamCap() {
		return teamCap;
	}

	public void setScoreCap(int scoreCap) {
		this.scoreCap = scoreCap;
	}

	public int getScoreCap() {
		return scoreCap;
	}

	public void setAutoAssignOnly(boolean autoAssignOnly) {
		this.autoAssignOnly = autoAssignOnly;
	}

	public boolean isAutoAssignOnly() {
		return autoAssignOnly;
	}

	public void setDrawZoneOutline(boolean drawZoneOutline) {
		this.drawZoneOutline = drawZoneOutline;
	}

	public boolean isDrawZoneOutline() {
		return drawZoneOutline;
	}

	public void handlePlayerLeave(Player player, Location destination) {
		Team playerTeam = war.getPlayerTeam(player.getName());
		if(playerTeam !=null) {
			Warzone zone = war.getPlayerTeamWarzone(player.getName());
			playerTeam.removePlayer(player.getName());
			playerTeam.resetSign();
			if(zone != null) {	
				if(zone.isFlagThief(player.getName())) {
					Team victim = zone.getVictimTeamForThief(player.getName());
					victim.getFlagVolume().resetBlocks();
					victim.initializeTeamFlag();
					zone.removeThief(player.getName());
					for(Team t : zone.getTeams()) {
						t.teamcast(war.str("Team " + victim.getName() + " flag was returned."));
					}
				}
				if(zone.getLobby() != null) {
					zone.getLobby().resetTeamGateSign(playerTeam);
				}
				if(zone.hasPlayerInventory(player.getName())) {
					zone.restorePlayerInventory(player);
				}
				player.teleportTo(destination);
				player.sendMessage(war.str("Left the zone. Your inventory has (hopefully) been restored."));
				if(war.getWarHub() != null) {
					war.getWarHub().resetZoneSign(zone);
				}
			}
		}
		
	}

	public boolean isEnemyTeamFlagBlock(Team playerTeam, Block block) {
		for(Team team : teams) {
			if(!team.getName().equals(playerTeam.getName())
					&& team.isTeamFlagBlock(block)) {
				return true;
			}
		}
		return false;
	}

	public Team getTeamForFlagBlock(Block block) {
		for(Team team : teams) {
			if(team.isTeamFlagBlock(block)) {
				return team;
			}
		}
		return null;
	}

	public void addFlagThief(Team lostFlagTeam, String flagThief) {
		flagThieves.put(flagThief, lostFlagTeam);
	}

	public boolean isFlagThief(String suspect) {
		if(flagThieves.containsKey(suspect)) return true;
		return false;
	}
	
	public Team getVictimTeamForThief(String thief) {
		return flagThieves.get(thief);
	}

	public void removeThief(String thief) {
		flagThieves.remove(thief);		
	}

	public void clearFlagThieves() {
		flagThieves.clear();
	}
	
	public boolean isTeamFlagStolen(Team team) {
		for(String playerKey : flagThieves.keySet()) {
			if(flagThieves.get(playerKey).getName().equals(team.getName())) {
				return true;
			}
		}
		return false;
	}

	public void handleScoreCapReached(Player player, String winnersStr) {
		winnersStr = "Score cap reached! Winning team(s): " + winnersStr;		
		winnersStr += ". Your inventory has (hopefully) been reset. The warzone is being reset... Please choose a new team.";
		// Score cap reached. Reset everything.
		for(Team t : this.getTeams()) {
			t.teamcast(war.str(winnersStr));
			for(Player tp : t.getPlayers()) {
				if(!tp.getName().equals(player.getName())) {
					if(this.hasPlayerInventory(tp.getName())){
						this.restorePlayerInventory(tp);
					}
					tp.teleportTo(this.getTeleport());
				}
				
			}
			t.setPoints(0);
			t.getPlayers().clear();	// empty the team
		}
		if(this.getLobby() != null) {
			this.getLobby().getVolume().resetBlocks();
		}
		this.getVolume().resetBlocks();
		this.initializeZone();
		if(war.getWarHub() != null) {
			war.getWarHub().resetZoneSign(this);
		}
	}

	public void setBlockHeads(boolean blockHeads) {
		this.blockHeads = blockHeads;
	}

	public boolean isBlockHeads() {
		return blockHeads;
	}

	public void setDropLootOnDeath(boolean dropLootOnDeath) {
		this.dropLootOnDeath = dropLootOnDeath;
	}

	public boolean isDropLootOnDeath() {
		return dropLootOnDeath;
	}

	
}
