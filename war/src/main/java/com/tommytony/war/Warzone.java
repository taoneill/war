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
	private boolean drawZoneOutline;
	
	private HashMap<String, List<ItemStack>> inventories = new HashMap<String, List<ItemStack>>();
	private World world;
	private Material originalSouthEastBlock;
	private Material originalNorthWestBlock;
	private final int minSafeDistanceFromWall = 5;
	private List<ZoneWallGuard> zoneWallGuards = new ArrayList<ZoneWallGuard>();
	private War war;
	private ZoneLobby lobby;
	private boolean autoAssignOnly;
	
	
	public Warzone(War war, World world, String name) {
		this.world = world;
		this.war = war;
		this.name = name;
		this.friendlyFire = war.getDefaultFriendlyFire();
		this.setLifePool(war.getDefaultLifepool());
		this.setLoadout(war.getDefaultLoadout());
		this.drawZoneOutline = war.getDefaultDrawZoneOutline();
		this.autoAssignOnly = war.getDefaultAutoAssignOnly();
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
		if(this.northwest != null && originalNorthWestBlock != null) {
			// reset old corner
			int highest = this.world.getHighestBlockYAt(this.northwest.getBlockX(), this.northwest.getBlockZ()) - 1;
			Block oldTopNWBlock = this.world.getBlockAt(this.northwest.getBlockX(), highest, this.northwest.getBlockZ());
			oldTopNWBlock.setType(originalNorthWestBlock);
		}
		this.northwest = northwest;
		int newHighest = this.world.getHighestBlockYAt(this.northwest.getBlockX(), this.northwest.getBlockZ()) - 1;
		Block topNWBlock = this.world.getBlockAt(this.northwest.getBlockX(), newHighest, this.northwest.getBlockZ());
		originalNorthWestBlock = topNWBlock.getType();	// save block for reset
		topNWBlock.setType(Material.GLASS);
		this.volume.setCornerOne(world.getBlockAt(northwest.getBlockX(), northwest.getBlockY(), northwest.getBlockZ()));
	}


	public Location getNorthwest() {
		return northwest;
	}

	public void setSoutheast(Location southeast) {
		if(this.southeast != null && originalSouthEastBlock != null) {
			// reset old corner
			int highest = this.world.getHighestBlockYAt(this.southeast.getBlockX(), this.southeast.getBlockZ()) - 1;
			Block oldTopSEBlock = this.world.getBlockAt(this.southeast.getBlockX(), highest, this.southeast.getBlockZ());
			oldTopSEBlock.setType(originalSouthEastBlock);
		}
		// change corner
		this.southeast = southeast;
		int newHighest = this.world.getHighestBlockYAt(this.southeast.getBlockX(), this.southeast.getBlockZ()) - 1;
		Block topSEBlock = this.world.getBlockAt(this.southeast.getBlockX(), newHighest, this.southeast.getBlockZ());
		originalSouthEastBlock = topSEBlock.getType();	// save block for reset
		topSEBlock.setType(Material.GLASS);
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
			// removed everything to keep save clean
			volume.resetWallBlocks(BlockFace.East);
			volume.resetWallBlocks(BlockFace.West);
			volume.resetWallBlocks(BlockFace.North);
			volume.resetWallBlocks(BlockFace.South);
			
			for(Team team : teams) {
				team.getVolume().resetBlocks();
			}
			
			for(Monument monument : monuments) {
				monument.remove();
			}
			int saved = volume.saveBlocks();
			initializeZone();	// bring back stuff
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

			// add wall outlines
			if(drawZoneOutline) {
				addZoneOutline(BlockFace.North);
				addZoneOutline(BlockFace.East);
				addZoneOutline(BlockFace.South);
				addZoneOutline(BlockFace.West);
			}
			
			// everyone back to team spawn with full health
			for(Team team : teams) {
				for(Player player : team.getPlayers()) {
					respawnPlayer(team, player);
				}
				team.setRemainingTickets(lifePool);
				team.setTeamSpawn(team.getTeamSpawn());
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

	private void addZoneOutline(BlockFace wall) {
		int c1maxY = world.getHighestBlockYAt(volume.getMinX(), volume.getMinZ());
		int c2maxY = world.getHighestBlockYAt(volume.getMaxX(), volume.getMaxZ());
		Block ne = world.getBlockAt(volume.getMinX(), c1maxY, volume.getMinZ());
		Block nw = world.getBlockAt(volume.getMinX(), c2maxY, volume.getMaxZ());
		Block se = world.getBlockAt(volume.getMaxX(), c2maxY, volume.getMinZ());
		Block lastBlock = null;
		if(BlockFace.North == wall) {
			for(int z = volume.getMinZ(); z < volume.getMaxZ(); z++) {
				lastBlock = highestBlockToGlass(ne.getX(), z, lastBlock);
			}
		} else if (BlockFace.East == wall) {
			for(int x = volume.getMinX(); x < volume.getMaxX(); x++) {
				lastBlock = highestBlockToGlass(x, ne.getZ(), lastBlock);
			}
		} else if (BlockFace.South == wall) {
			for(int z = volume.getMinZ(); z < volume.getMaxZ(); z++) {
				lastBlock = highestBlockToGlass(se.getX(), z, lastBlock);
			}
		} else if (BlockFace.West == wall) {
			for(int x = volume.getMinX(); x < volume.getMaxX(); x++) {
				lastBlock = highestBlockToGlass(x, nw.getZ(), lastBlock);
			}
		}
	}

	private Block highestBlockToGlass(int x, int z, Block lastBlock) {
		int highest = world.getHighestBlockYAt(x, z);
		Block block = world.getBlockAt(x, highest -1 , z);
		
		if(block.getType() == Material.LEAVES) { // top of tree, lets find some dirt
			Block over = block.getFace(BlockFace.Down);
			Block under = over.getFace(BlockFace.Down);
			int treeHeight = 0;
			while(!((over.getType() == Material.AIR || over.getType() == Material.LEAVES || over.getType() == Material.WOOD)
					&& (under.getType() != Material.AIR || under.getType() == Material.LEAVES || under.getType() == Material.LEAVES))
				  && treeHeight < 40) {
				over = under;
				under = over.getFace(BlockFace.Down);
				treeHeight++;
			}
			block = under; // found the ground
		}
		
		block.setType(Material.GLASS);

		if(lastBlock != null) {
			// link the new block and the old vertically if there's a big drop or rise
			if(block.getY() - lastBlock.getY() > 1) {  // new block too high 
				Block under = block.getFace(BlockFace.Down);
				while(under.getY() != lastBlock.getY() - 1) {
					under.setType(Material.GLASS);
					under = under.getFace(BlockFace.Down);
				}
			} else if (lastBlock.getY() - block.getY() > 1) { // new block too low
				Block over = block.getFace(BlockFace.Up);
				while(over.getY() != lastBlock.getY() + 1) {
					over.setType(Material.GLASS);
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
		// BUKKIT
//		for(int i = 0; i < playerInv.getSize(); i++){
//			playerInv.setItem(index, new ItemStack(Material.))
//			playerInv.setItem(i, null);	
//		}
		for(Integer slot : loadout.keySet()) {
//			if(slot == 101) {
//				playerInv.setLeggings(loadout.get(slot));
//			} else if(slot == 102) {
//				playerInv.setChestplate(loadout.get(slot));
//			} else if(slot == 103) {
//				playerInv.setHelmet(loadout.get(slot));
//			} else { 
				playerInv.setItem(slot, loadout.get(slot));
			//}
		}
		
		if(team.getMaterial() == Material.GOLD_BLOCK) {
			playerInv.setHelmet(new ItemStack(Material.GOLD_BOOTS));
		} else if (team.getMaterial() == Material.DIAMOND_BLOCK) {
			playerInv.setHelmet(new ItemStack(Material.DIAMOND_BOOTS));
		} else if (team.getMaterial() == Material.IRON_BLOCK) {
			playerInv.setHelmet(new ItemStack(Material.IRON_BOOTS));
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
			playerInv.setItem(i, new ItemStack(Material.AIR));
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

	public VerticalVolume getVolume() {
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
			walls.add(BlockFace.East);
		} 
		
		if (Math.abs(southeast.getBlockX() - latestPlayerLocation.getBlockX()) < minSafeDistanceFromWall
				&& latestPlayerLocation.getBlockZ() <= northwest.getBlockZ()
				&& latestPlayerLocation.getBlockZ() >= southeast.getBlockZ()) {
			// near south wall
			walls.add(BlockFace.South); 	
		}

		if (Math.abs(northwest.getBlockX() - latestPlayerLocation.getBlockX()) < minSafeDistanceFromWall
				&& latestPlayerLocation.getBlockZ() <= northwest.getBlockZ()
				&& latestPlayerLocation.getBlockZ() >= southeast.getBlockZ()) {
			// near north wall
			walls.add(BlockFace.North);
		} 

		if (Math.abs(northwest.getBlockZ() - latestPlayerLocation.getBlockZ()) < minSafeDistanceFromWall
				&& latestPlayerLocation.getBlockX() <= southeast.getBlockX()
				&& latestPlayerLocation.getBlockX() >= northwest.getBlockX()) {
			// near west wall
			walls.add(BlockFace.West);
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
				if(drawZoneOutline) {
					addZoneOutline(guard.getWall());
				}
				war.getLogger().info("Reset " + reset + " blocks in " + guard.getWall() + "wall of warzone " + name);
			}
		}
		// now remove those zone guards
		for(ZoneWallGuard playerGuard : playerGuards) {
			zoneWallGuards.remove(playerGuard);
		}
		playerGuards.clear();
	}

	public boolean getAutoAssignOnly() {
		
		return autoAssignOnly;
	}

	public void setLobby(ZoneLobby lobby) {
		this.lobby = lobby;
	}

	public ZoneLobby getLobby() {
		return lobby;
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

	

}
