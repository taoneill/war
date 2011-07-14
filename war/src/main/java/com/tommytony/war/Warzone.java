package com.tommytony.war;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;

import bukkit.tommytony.war.War;

import com.tommytony.war.jobs.InitZoneJob;
import com.tommytony.war.jobs.LoadoutResetJob;
import com.tommytony.war.jobs.ScoreCapReachedJob;
import com.tommytony.war.utils.InventoryStash;
import com.tommytony.war.volumes.ZoneVolume;

/**
 * 
 * @author tommytony
 *
 */
public class Warzone {
	private String name;
	private ZoneVolume volume;
	private final List<Team> teams = new ArrayList<Team>();
	private final List<Monument> monuments = new ArrayList<Monument>();
	
	private Location teleport;
	private boolean friendlyFire;
	private int lifePool;
	private HashMap<Integer, ItemStack> loadout = new HashMap<Integer, ItemStack>();
	private boolean drawZoneOutline;
	private int teamCap = 5;
	private int scoreCap = 5;
	private int monumentHeal = 5;
	private String spawnStyle = TeamSpawnStyles.BIG;
	private HashMap<Integer, ItemStack> reward = new HashMap<Integer, ItemStack>();
	
	private HashMap<String, InventoryStash> inventories = new HashMap<String, InventoryStash>();
	private HashMap<String, Team> flagThieves = new HashMap<String, Team>();
	private World world;
	private final int minSafeDistanceFromWall = 6;
	private List<ZoneWallGuard> zoneWallGuards = new ArrayList<ZoneWallGuard>();
	private War war;
	private ZoneLobby lobby;
	private boolean autoAssignOnly;
	private boolean blockHeads;
	private boolean dropLootOnDeath;
	private boolean unbreakableZoneBlocks;
	private boolean disabled = false;
	private boolean noCreatures;
	private boolean resetOnEmpty = false;
	private HashMap<String, InventoryStash> deadMenInventories = new HashMap<String, InventoryStash>();
	private Location rallyPoint;
	
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
		this.monumentHeal = war.getDefaultMonumentHeal();
		this.setBlockHeads(war.isDefaultBlockHeads());
		this.setDropLootOnDeath(war.isDefaultDropLootOnDeath());
		this.setUnbreakableZoneBlocks(war.isDefaultUnbreakableZoneBlocks());
		this.setNoCreatures(war.getDefaultNoCreatures());
		this.volume = new ZoneVolume(name, war, this.getWorld(), this);
	}
	
	public boolean ready() {
		if(volume.hasTwoCorners() && !volume.tooSmall() && !volume.tooBig()) return true;
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
				for(ZoneWallGuard guard : zoneWallGuards) {
					guard.deactivate();
				}
				zoneWallGuards.clear();
//				volume.resetWallBlocks(BlockFace.EAST);
//				volume.resetWallBlocks(BlockFace.WEST);
//				volume.resetWallBlocks(BlockFace.NORTH);
//				volume.resetWallBlocks(BlockFace.SOUTH);
				
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
		initializeZone(null);
	}
	
	public void initializeZone(Player respawnExempted) {
		if(ready() && volume.isSaved()){			
			// everyone back to team spawn with full health
			for(Team team : teams) {
				for(Player player : team.getPlayers()) {
					if(player != respawnExempted) respawnPlayer(team, player);
				}
				team.setRemainingLives(lifePool);
				team.initializeTeamSpawn();
				if(team.getTeamFlag() != null) team.setTeamFlag(team.getTeamFlag());
				//team.resetSign();
			}
			
			initZone();
		}
	}

	
	public void initializeZoneAsJob(Player respawnExempted) {
		InitZoneJob job = new InitZoneJob(this, respawnExempted);		
		war.getServer().getScheduler().scheduleSyncDelayedTask(war, job);
	}
	
	public void initializeZoneAsJob() {
		InitZoneJob job = new InitZoneJob(this);		
		war.getServer().getScheduler().scheduleSyncDelayedTask(war, job);
	}
		
	private void initZone() {		
		// reset monuments
		for(Monument monument : monuments) {
			monument.getVolume().resetBlocks();
			monument.addMonumentBlocks();
		}

		// reset lobby (here be demons)
		if(lobby != null) {
			lobby.initialize();
		}
		
		this.flagThieves.clear();
	}

	public void endRound() {
		
	}

	public void respawnPlayer(Team team, Player player) {
		handleRespawn(team, player);
		// Teleport the player back to spawn
		player.teleport(team.getTeamSpawn());
	}
	
	public void respawnPlayer(PlayerMoveEvent event, Team team, Player player) {
		handleRespawn(team, player);
		// Teleport the player back to spawn
		event.setTo(team.getTeamSpawn());
		//player.teleport(team.getTeamSpawn());
	}
	
	private void handleRespawn(Team team, Player player){		
		// Fill hp
		player.setFireTicks(0);
		player.setRemainingAir(300);
		player.setHealth(20);

		LoadoutResetJob job = new LoadoutResetJob(this, team, player);
		war.getServer().getScheduler().scheduleSyncDelayedTask(war, job);
	}

	public void resetInventory(Team team, Player player) {
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
			} else {
				ItemStack item = loadout.get(slot);
				if(item != null) {
					playerInv.addItem(item);
				}
			}
		}
		if(isBlockHeads()) {
			playerInv.setHelmet(new ItemStack(team.getKind().getMaterial(), 1, (short)1, new Byte(team.getKind().getData())));
		} else {
			if(team.getKind() == TeamKinds.teamKindFromString("gold")) {
				playerInv.setHelmet(new ItemStack(Material.GOLD_HELMET));
			} else if (team.getKind() == TeamKinds.teamKindFromString("diamond")) {
				playerInv.setHelmet(new ItemStack(Material.DIAMOND_HELMET));
			} else if (team.getKind() == TeamKinds.teamKindFromString("iron")) {
				playerInv.setHelmet(new ItemStack(Material.IRON_HELMET));
			} else {
				playerInv.setHelmet(new ItemStack(Material.LEATHER_HELMET));
			}
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
			if(stack != null) {
				this.loadout.put(slot, stack);
			}
		}
	}

	public HashMap<Integer, ItemStack> getLoadout() {
		return loadout;
	}

	public void setLifePool(int lifePool) {
		this.lifePool = lifePool;
		for(Team team : teams) {
			team.setRemainingLives(lifePool);
		}
	}

	public int getLifePool() {
		return lifePool;
	}
	
	public void setMonumentHeal(int monumentHeal) {
		this.monumentHeal = monumentHeal;
	}

	public int getMonumentHeal() {
		return monumentHeal;
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
			playerInvFromInventoryStash(playerInv, originalContents);
		}
	}
	
	private void playerInvFromInventoryStash(PlayerInventory playerInv,
			InventoryStash originalContents) {
		playerInv.clear();
		playerInv.clear(playerInv.getSize() + 0);
		playerInv.clear(playerInv.getSize() + 1);
		playerInv.clear(playerInv.getSize() + 2);
		playerInv.clear(playerInv.getSize() + 3);	// helmet/blockHead
		for(ItemStack item : originalContents.getContents()) {
			if(item != null && item.getTypeId() != 0) {
				playerInv.addItem(item);
			}
		}
		if(originalContents.getHelmet() != null && originalContents.getHelmet().getType() != Material.AIR) {
			playerInv.setHelmet(originalContents.getHelmet());
		}
		if(originalContents.getChest() != null && originalContents.getChest().getType() != Material.AIR) {
			playerInv.setChestplate(originalContents.getChest());
		}
		if(originalContents.getLegs() != null && originalContents.getLegs().getType() != Material.AIR) {
			playerInv.setLeggings(originalContents.getLegs());
		}
		if(originalContents.getFeet() != null && originalContents.getFeet().getType() != Material.AIR) {
			playerInv.setBoots(originalContents.getFeet());
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
			if(monument.getName().startsWith(monumentName)) {
				return monument;
			}
		}
		return null;
	}
	
	public boolean isImportantBlock(Block block) {
		if(ready()) {
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
		}
		return false;
	}

	public World getWorld() {
		
		return world;
	}

	public void setWorld(World world) {
		this.world = world;
	}

	public ZoneVolume getVolume() {
		return volume;
	}

	public void setVolume(ZoneVolume zoneVolume) {
		this.volume = zoneVolume;
	}
	
	public Team getTeamByKind(TeamKind kind) {
		for(Team t : teams) {
			if(t.getKind() == kind) {
				return t;
			}
		}
		return null;
	}

	public boolean isNearWall(Location latestPlayerLocation) {
		if(volume.hasTwoCorners()) {
			if(Math.abs(volume.getSoutheastZ() - latestPlayerLocation.getBlockZ()) < minSafeDistanceFromWall 
					&& latestPlayerLocation.getBlockX() <= volume.getSoutheastX()
					&& latestPlayerLocation.getBlockX() >= volume.getNorthwestX()
					&& latestPlayerLocation.getBlockY() >= volume.getMinY()
					&& latestPlayerLocation.getBlockY() <= volume.getMaxY()) {
				return true; 	// near east wall
			} else if (Math.abs(volume.getSoutheastX() - latestPlayerLocation.getBlockX()) < minSafeDistanceFromWall
					&& latestPlayerLocation.getBlockZ() <= volume.getNorthwestZ()
					&& latestPlayerLocation.getBlockZ() >= volume.getSoutheastZ()
					&& latestPlayerLocation.getBlockY() >= volume.getMinY()
					&& latestPlayerLocation.getBlockY() <= volume.getMaxY()) {
				return true;	// near south wall
			} else if (Math.abs(volume.getNorthwestX() - latestPlayerLocation.getBlockX()) < minSafeDistanceFromWall
					&& latestPlayerLocation.getBlockZ() <= volume.getNorthwestZ()
					&& latestPlayerLocation.getBlockZ() >= volume.getSoutheastZ()
					&& latestPlayerLocation.getBlockY() >= volume.getMinY()
					&& latestPlayerLocation.getBlockY() <= volume.getMaxY()) {
				return true;	// near north wall
			} else if (Math.abs(volume.getNorthwestZ() - latestPlayerLocation.getBlockZ()) < minSafeDistanceFromWall
					&& latestPlayerLocation.getBlockX() <= volume.getSoutheastX()
					&& latestPlayerLocation.getBlockX() >= volume.getNorthwestX()
					&& latestPlayerLocation.getBlockY() >= volume.getMinY()
					&& latestPlayerLocation.getBlockY() <= volume.getMaxY()) {
				return true;	// near west wall
			} else if (Math.abs(volume.getMaxY() - latestPlayerLocation.getBlockY()) < minSafeDistanceFromWall
					&& latestPlayerLocation.getBlockX() <= volume.getMaxX()
					&& latestPlayerLocation.getBlockX() >= volume.getMinX()
					&& latestPlayerLocation.getBlockZ() <= volume.getMaxZ()
					&& latestPlayerLocation.getBlockZ() >= volume.getMinZ()) {
				return true;	// near up wall
			} else if (Math.abs(volume.getMinY() - latestPlayerLocation.getBlockY()) < minSafeDistanceFromWall
					&& latestPlayerLocation.getBlockX() <= volume.getMaxX()
					&& latestPlayerLocation.getBlockX() >= volume.getMinX()
					&& latestPlayerLocation.getBlockZ() <= volume.getMaxZ()
					&& latestPlayerLocation.getBlockZ() >= volume.getMinZ()) {
				return true;	// near down wall
			}
		}
		return false;
	}
	
	public List<Block> getNearestWallBlocks(Location latestPlayerLocation) {
		List<Block> nearestWallBlocks = new ArrayList<Block>();
		if(Math.abs(volume.getSoutheastZ() - latestPlayerLocation.getBlockZ()) < minSafeDistanceFromWall 
				&& latestPlayerLocation.getBlockX() <= volume.getSoutheastX()
				&& latestPlayerLocation.getBlockX() >= volume.getNorthwestX()
				&& latestPlayerLocation.getBlockY() >= volume.getMinY()
				&& latestPlayerLocation.getBlockY() <= volume.getMaxY()) {
			// near east wall
			Block eastWallBlock = world.getBlockAt(latestPlayerLocation.getBlockX() + 1, latestPlayerLocation.getBlockY() + 1, volume.getSoutheastZ());
			nearestWallBlocks.add(eastWallBlock);
		}
		
		if (Math.abs(volume.getSoutheastX() - latestPlayerLocation.getBlockX()) < minSafeDistanceFromWall
				&& latestPlayerLocation.getBlockZ() <= volume.getNorthwestZ()
				&& latestPlayerLocation.getBlockZ() >= volume.getSoutheastZ()
				&& latestPlayerLocation.getBlockY() >= volume.getMinY()
				&& latestPlayerLocation.getBlockY() <= volume.getMaxY()) {
			// near south wall
			Block southWallBlock = world.getBlockAt(volume.getSoutheastX(), latestPlayerLocation.getBlockY() + 1, latestPlayerLocation.getBlockZ());
			nearestWallBlocks.add(southWallBlock);
		}
		
		if (Math.abs(volume.getNorthwestX() - latestPlayerLocation.getBlockX()) < minSafeDistanceFromWall
				&& latestPlayerLocation.getBlockZ() <= volume.getNorthwestZ()
				&& latestPlayerLocation.getBlockZ() >= volume.getSoutheastZ()
				&& latestPlayerLocation.getBlockY() >= volume.getMinY()
				&& latestPlayerLocation.getBlockY() <= volume.getMaxY()) {
			// near north wall
			Block northWallBlock = world.getBlockAt(volume.getNorthwestX(), latestPlayerLocation.getBlockY() + 1, latestPlayerLocation.getBlockZ());
			nearestWallBlocks.add(northWallBlock);
		}
		
		if (Math.abs(volume.getNorthwestZ() - latestPlayerLocation.getBlockZ()) < minSafeDistanceFromWall
				&& latestPlayerLocation.getBlockX() <= volume.getSoutheastX()
				&& latestPlayerLocation.getBlockX() >= volume.getNorthwestX()
				&& latestPlayerLocation.getBlockY() >= volume.getMinY()
				&& latestPlayerLocation.getBlockY() <= volume.getMaxY()) {
			// near west wall
			Block westWallBlock = world.getBlockAt(latestPlayerLocation.getBlockX(), latestPlayerLocation.getBlockY() + 1, volume.getNorthwestZ());
			nearestWallBlocks.add(westWallBlock);
		}
		
		if (Math.abs(volume.getMaxY() - latestPlayerLocation.getBlockY()) < minSafeDistanceFromWall
				&& latestPlayerLocation.getBlockX() <= volume.getMaxX()
				&& latestPlayerLocation.getBlockX() >= volume.getMinX()
				&& latestPlayerLocation.getBlockZ() <= volume.getMaxZ()
				&& latestPlayerLocation.getBlockZ() >= volume.getMinZ()) {
			// near up wall
			Block upWallBlock = world.getBlockAt(latestPlayerLocation.getBlockX(), volume.getMaxY(), latestPlayerLocation.getBlockZ());
			nearestWallBlocks.add(upWallBlock);
		}
		
		if (Math.abs(volume.getMinY() - latestPlayerLocation.getBlockY()) < minSafeDistanceFromWall
				&& latestPlayerLocation.getBlockX() <= volume.getMaxX()
				&& latestPlayerLocation.getBlockX() >= volume.getMinX()
				&& latestPlayerLocation.getBlockZ() <= volume.getMaxZ()
				&& latestPlayerLocation.getBlockZ() >= volume.getMinZ()) {
			// near down wall
			Block downWallBlock = world.getBlockAt(latestPlayerLocation.getBlockX(), volume.getMinY(), latestPlayerLocation.getBlockZ());
			nearestWallBlocks.add(downWallBlock);
		}
		return nearestWallBlocks;
		// note: y + 1 to line up 3 sided square with player eyes
	}
	
	public List<BlockFace> getNearestWalls(Location latestPlayerLocation) {
		List<BlockFace> walls = new ArrayList<BlockFace>();
		if(Math.abs(volume.getSoutheastZ() - latestPlayerLocation.getBlockZ()) < minSafeDistanceFromWall 
				&& latestPlayerLocation.getBlockX() <= volume.getSoutheastX()
				&& latestPlayerLocation.getBlockX() >= volume.getNorthwestX()
				&& latestPlayerLocation.getBlockY() >= volume.getMinY()
				&& latestPlayerLocation.getBlockY() <= volume.getMaxY()) {
			// near east wall
			walls.add(BlockFace.EAST);
		} 
		
		if (Math.abs(volume.getSoutheastX() - latestPlayerLocation.getBlockX()) < minSafeDistanceFromWall
				&& latestPlayerLocation.getBlockZ() <= volume.getNorthwestZ()
				&& latestPlayerLocation.getBlockZ() >= volume.getSoutheastZ()
				&& latestPlayerLocation.getBlockY() >= volume.getMinY()
				&& latestPlayerLocation.getBlockY() <= volume.getMaxY()) {
			// near south wall
			walls.add(BlockFace.SOUTH); 	
		}

		if (Math.abs(volume.getNorthwestX() - latestPlayerLocation.getBlockX()) < minSafeDistanceFromWall
				&& latestPlayerLocation.getBlockZ() <= volume.getNorthwestZ()
				&& latestPlayerLocation.getBlockZ() >= volume.getSoutheastZ()
				&& latestPlayerLocation.getBlockY() >= volume.getMinY()
				&& latestPlayerLocation.getBlockY() <= volume.getMaxY()) {
			// near north wall
			walls.add(BlockFace.NORTH);
		} 

		if (Math.abs(volume.getNorthwestZ() - latestPlayerLocation.getBlockZ()) < minSafeDistanceFromWall
				&& latestPlayerLocation.getBlockX() <= volume.getSoutheastX()
				&& latestPlayerLocation.getBlockX() >= volume.getNorthwestX()
				&& latestPlayerLocation.getBlockY() >= volume.getMinY()
				&& latestPlayerLocation.getBlockY() <= volume.getMaxY()) {
			// near west wall
			walls.add(BlockFace.WEST);
		}
		
		if (Math.abs(volume.getMaxY() - latestPlayerLocation.getBlockY()) < minSafeDistanceFromWall
				&& latestPlayerLocation.getBlockX() <= volume.getMaxX()
				&& latestPlayerLocation.getBlockX() >= volume.getMinX()
				&& latestPlayerLocation.getBlockZ() <= volume.getMaxZ()
				&& latestPlayerLocation.getBlockZ() >= volume.getMinZ()) {
			// near up wall
			walls.add(BlockFace.UP);
		}
		
		if (Math.abs(volume.getMinY() - latestPlayerLocation.getBlockY()) < minSafeDistanceFromWall
				&& latestPlayerLocation.getBlockX() <= volume.getMaxX()
				&& latestPlayerLocation.getBlockX() >= volume.getMinX()
				&& latestPlayerLocation.getBlockZ() <= volume.getMaxZ()
				&& latestPlayerLocation.getBlockZ() >= volume.getMinZ()) {
			// near down wall
			walls.add(BlockFace.DOWN);
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

	public boolean protectZoneWallAgainstPlayer(Player player) {
		List<BlockFace> nearestWalls = getNearestWalls(player.getLocation());
		boolean protecting = false;
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
			protecting = true;
		}
		return protecting;
	}
	
	public void dropZoneWallGuardIfAny(Player player) {
		List<ZoneWallGuard> playerGuards = new ArrayList<ZoneWallGuard>();
		for(ZoneWallGuard guard : zoneWallGuards) {
			if(guard.getPlayer().getName().equals(player.getName())){
				playerGuards.add(guard);
				guard.deactivate();
//				BlockFace guardWall = guard.getWall();
//				getVolume().resetWallBlocks(guardWall);
//				if(isDrawZoneOutline()) {
//					addZoneOutline(guard.getWall());
//				}
//				if(lobby != null) {
//					lobby.getVolume().resetBlocks(); // always reset the lobby even if the guard is on another wall
//													// because player can go around corner
//					lobby.initialize();
//				}
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
	
	public Team autoAssign(Player player) {
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
			war.msg(player, "Your inventory is in storage until you /leave.");
			respawnPlayer(lowestNoOfPlayers, player);
			for(Team team : teams){
				team.teamcast("" + player.getName() + " joined team " + lowestNoOfPlayers.getName() + ".");
			}
		}
		return lowestNoOfPlayers;
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
	
	public void handleDeath(Player player) {
		Team playerTeam = war.getPlayerTeam(player.getName());
		Warzone playerWarzone = war.getPlayerTeamWarzone(player.getName());
		if(playerTeam != null && playerWarzone != null) {
			// teleport to team spawn upon death
			war.msg(player, "You died.");
			playerWarzone.respawnPlayer(playerTeam, player);
			int remaining = playerTeam.getRemainingLifes();
			if(remaining == 0) { // your death caused your team to lose
				List<Team> teams = playerWarzone.getTeams();
				String scores = "";
				for(Team t : teams) {
					t.teamcast("The battle is over. Team " + playerTeam.getName() + " lost: " 
							+ player.getName() + " died and there were no lives left in their life pool.");
					
					if (t.getPlayers().size() != 0) {
						if(!t.getName().equals(playerTeam.getName())) {
							// all other teams get a point
							t.addPoint();
							t.resetSign();
						}
						scores += t.getName() + "(" + t.getPoints() + ") " ;
					}
				}
				if(!scores.equals("")){
					for(Team t : teams) {
						t.teamcast("New scores - " + scores + " (/" + getScoreCap() + ")" );
					}
				}
				// detect score cap
				List<Team> scoreCapTeams = new ArrayList<Team>();
				for(Team t : teams) {
					if(t.getPoints() == playerWarzone.getScoreCap()) {
						scoreCapTeams.add(t);
					}
				}
				if(!scoreCapTeams.isEmpty()) {
					String winnersStr = "";
					for(Team winner : scoreCapTeams) {
						if (winner.getPlayers().size() != 0) winnersStr += winner.getName() + " ";
					}
					
					playerWarzone.handleScoreCapReached(player, winnersStr);
					//player.teleport(playerWarzone.getTeleport());
					// player will die because it took too long :(
					// we dont restore his inventory in handleScoreCapReached
					// check out PLAYER_MOVE for the rest of the fix
					
				} else {
					// A new battle starts. Reset the zone but not the teams.
					for(Team t : teams) {
						t.teamcast("A new battle begins. Resetting warzone...");
					}
					playerWarzone.getVolume().resetBlocksAsJob();
					playerWarzone.initializeZoneAsJob(player);
				}
			} else {
				// player died without causing his team's demise
				if(playerWarzone.isFlagThief(player.getName())) {
					// died while carrying flag.. dropped it
					Team victim = playerWarzone.getVictimTeamForThief(player.getName());
					victim.getFlagVolume().resetBlocks();
					victim.initializeTeamFlag();
					playerWarzone.removeThief(player.getName());
					for(Team t : playerWarzone.getTeams()) {
						t.teamcast(player.getName() + " died and dropped team " + victim.getName() + "'s flag.");
					}
				}
				playerTeam.setRemainingLives(remaining - 1);
				if(remaining - 1 == 0) {
					for(Team t : playerWarzone.getTeams()) {
						t.teamcast("Team " + playerTeam.getName() + "'s life pool is empty. One more death and they lose the battle!");
					}
				}
			}
			playerTeam.resetSign();
			Plugin heroicDeath = war.getServer().getPluginManager().getPlugin("HeroicDeath");
			if(heroicDeath != null) {
				
			}
		}
	}
	
	public void handlePlayerLeave(Player player, Location destination, PlayerMoveEvent event, boolean removeFromTeam) {
		handlePlayerLeave(player, removeFromTeam);
		event.setTo(destination);
	}
	
	public void handlePlayerLeave(Player player, Location destination, boolean removeFromTeam) {
		handlePlayerLeave(player, removeFromTeam);
		player.teleport(destination);
	}

	private void handlePlayerLeave(Player player, boolean removeFromTeam) {
		Team playerTeam = war.getPlayerTeam(player.getName());
		if(playerTeam !=null) {
			if(removeFromTeam) playerTeam.removePlayer(player.getName());
			for(Team t : this.getTeams()) {
				t.teamcast(player.getName() + " left the zone.");
			}
			playerTeam.resetSign();	
			if(this.isFlagThief(player.getName())) {
				Team victim = this.getVictimTeamForThief(player.getName());
				victim.getFlagVolume().resetBlocks();
				victim.initializeTeamFlag();
				this.removeThief(player.getName());
				for(Team t : this.getTeams()) {
					t.teamcast("Team " + victim.getName() + " flag was returned.");
				}
			}
			if(this.getLobby() != null) {
				this.getLobby().resetTeamGateSign(playerTeam);
			}
			if(this.hasPlayerInventory(player.getName())) {
				this.restorePlayerInventory(player);
			}
			player.setHealth(20);
			player.setFireTicks(0);
			player.setRemainingAir(300);
			
			war.msg(player, "Left the zone. Your inventory has been restored.");
			if(war.getWarHub() != null) {
				war.getWarHub().resetZoneSign(this);
			}

			boolean zoneEmpty = true;
			for(Team team : this.getTeams()) {
				if(team.getPlayers().size() > 0) {
					zoneEmpty = false;
					break;
				}
			}
			if(zoneEmpty && resetOnEmpty) {
				// reset the zone for a new game when the last player leaves
				for(Team team : this.getTeams()) {
					team.resetPoints();
					team.setRemainingLives(this.getLifePool());
				}
				this.getVolume().resetBlocksAsJob();
				this.initializeZoneAsJob();
				war.logInfo("Last player left warzone " + this.getName() + ". Warzone blocks resetting automatically...");
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
		winnersStr = "Score cap reached. Game is over! Winning team(s): " + winnersStr;		
		winnersStr += ". Resetting warzone and your inventory...";
		// Score cap reached. Reset everything.
		ScoreCapReachedJob job = new ScoreCapReachedJob(this, winnersStr);		
		war.getServer().getScheduler().scheduleSyncDelayedTask(war, job);
		if(this.getLobby() != null) {
			this.getLobby().getVolume().resetBlocksAsJob();
		}
		this.getVolume().resetBlocksAsJob();
		this.initializeZoneAsJob(player);
		if(war.getWarHub() != null) {
			// TODO: test if warhub sign give the correct info despite the jobs
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

	public void setSpawnStyle(String spawnStyle) {
		this.spawnStyle = spawnStyle;
	}

	public String getSpawnStyle() {
		return spawnStyle;
	}

	public void setReward(HashMap<Integer, ItemStack> reward) {
		this.reward = reward;
	}

	public HashMap<Integer, ItemStack> getReward() {
		return reward;
	}

	public void setUnbreakableZoneBlocks(boolean unbreakableZoneBlocks) {
		this.unbreakableZoneBlocks = unbreakableZoneBlocks;
	}

	public boolean isUnbreakableZoneBlocks() {
		return unbreakableZoneBlocks;
	}

	public void setDisabled(boolean disabled) {
		this.disabled = disabled;
	}

	public boolean isDisabled() {
		return disabled;
	}

	public boolean isNoCreatures() {
		return noCreatures;
	}

	public void setNoCreatures(boolean noCreatures) {
		this.noCreatures = noCreatures;
	}

	public boolean isDeadMan(String playerName) {
		if(deadMenInventories.containsKey(playerName)) {
			return true;
		}
		return false;
	}

	public void restoreDeadmanInventory(Player player) {
		if(isDeadMan(player.getName())) {
			playerInvFromInventoryStash(player.getInventory(), deadMenInventories.get(player.getName()));
			deadMenInventories.remove(player.getName());
		}
		
	}

	public void setRallyPoint(Location location) {
		this.rallyPoint = location;
	}
	
	public Location getRallyPoint() {
		return this.rallyPoint;
	}

	public void unload() {
		war.logInfo("Clearing zone " + this.getName() + "...");
		for(Team team : this.getTeams()) {
			for(Player player : team.getPlayers()) {
				this.handlePlayerLeave(player, this.getTeleport(), false);
			}
			team.getPlayers().clear();
		}
		if(this.getLobby() != null)
		{
			this.getLobby().getVolume().resetBlocks();
			this.getLobby().getVolume().finalize();
		}
		//this.getVolume().resetBlocks();
		this.getVolume().finalize();
	}
}
