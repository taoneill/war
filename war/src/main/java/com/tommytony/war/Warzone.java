package com.tommytony.war;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.getspout.spoutapi.SpoutManager;
import org.getspout.spoutapi.player.SpoutPlayer;

import com.tommytony.war.config.InventoryBag;
import com.tommytony.war.config.TeamConfig;
import com.tommytony.war.config.TeamConfigBag;
import com.tommytony.war.config.TeamKind;
import com.tommytony.war.config.WarzoneConfig;
import com.tommytony.war.config.WarzoneConfigBag;
import com.tommytony.war.job.InitZoneJob;
import com.tommytony.war.job.LoadoutResetJob;
import com.tommytony.war.job.ScoreCapReachedJob;
import com.tommytony.war.mapper.LoadoutYmlMapper;
import com.tommytony.war.spout.SpoutDisplayer;
import com.tommytony.war.structure.Bomb;
import com.tommytony.war.structure.Cake;
import com.tommytony.war.structure.Monument;
import com.tommytony.war.structure.HubLobbyMaterials;
import com.tommytony.war.structure.WarzoneMaterials;
import com.tommytony.war.structure.ZoneLobby;
import com.tommytony.war.structure.ZoneWallGuard;
import com.tommytony.war.utility.Direction;
import com.tommytony.war.utility.Loadout;
import com.tommytony.war.utility.LoadoutSelection;
import com.tommytony.war.utility.PlayerState;
import com.tommytony.war.utility.PotionEffectHelper;
import com.tommytony.war.volume.BlockInfo;
import com.tommytony.war.volume.Volume;
import com.tommytony.war.volume.ZoneVolume;
import org.bukkit.inventory.meta.LeatherArmorMeta;

/**
 *
 * @author tommytony
 * @package com.tommytony.war
 */
public class Warzone {
	private String name;
	private ZoneVolume volume;
	private World world;
	private final List<Team> teams = new ArrayList<Team>();
	private final List<Monument> monuments = new ArrayList<Monument>();
	private final List<Bomb> bombs = new ArrayList<Bomb>();
	private final List<Cake> cakes = new ArrayList<Cake>();
	private Location teleport;
	private ZoneLobby lobby;
	private Location rallyPoint;
	
	private final List<String> authors = new ArrayList<String>();
	
	private final int minSafeDistanceFromWall = 6;
	private List<ZoneWallGuard> zoneWallGuards = new ArrayList<ZoneWallGuard>();
	private HashMap<String, PlayerState> playerStates = new HashMap<String, PlayerState>();
	private HashMap<String, Team> flagThieves = new HashMap<String, Team>();
	private HashMap<String, Bomb> bombThieves = new HashMap<String, Bomb>();
	private HashMap<String, Cake> cakeThieves = new HashMap<String, Cake>();
	private HashMap<String, LoadoutSelection> loadoutSelections = new HashMap<String, LoadoutSelection>();
	private HashMap<String, PlayerState> deadMenInventories = new HashMap<String, PlayerState>();
	private final List<Player> respawn = new ArrayList<Player>();
	private final List<String> reallyDeadFighters = new ArrayList<String>();
	
	private final WarzoneConfigBag warzoneConfig;
	private final TeamConfigBag teamDefaultConfig;
	private InventoryBag defaultInventories = new InventoryBag();
	
	private HubLobbyMaterials lobbyMaterials = null;
	private WarzoneMaterials warzoneMaterials = new WarzoneMaterials(49, (byte)0, 85, (byte)0, 89, (byte)0);	// default main obsidian, stand ladder, light glowstone
	
	private boolean isEndOfGame = false;
	private boolean isReinitializing = false;
	//private final Object gameEndLock = new Object();

	public Warzone(World world, String name) {
		this.world = world;
		this.name = name;
		this.warzoneConfig = new WarzoneConfigBag(this);
		this.teamDefaultConfig = new TeamConfigBag();	// don't use ctor with Warzone, as this changes config resolution
		this.volume = new ZoneVolume(name, this.getWorld(), this);
		this.lobbyMaterials = new HubLobbyMaterials(
				War.war.getWarhubMaterials().getFloorId(),
				War.war.getWarhubMaterials().getFloorData(),
				War.war.getWarhubMaterials().getOutlineId(),
				War.war.getWarhubMaterials().getOutlineData(),
				War.war.getWarhubMaterials().getGateId(),
				War.war.getWarhubMaterials().getGateData(),
				War.war.getWarhubMaterials().getLightId(),
				War.war.getWarhubMaterials().getLightData()
			);
	}

	public static Warzone getZoneByName(String name) {
		Warzone bestGuess = null;
		for (Warzone warzone : War.war.getWarzones()) {
			if (warzone.getName().toLowerCase().equals(name.toLowerCase())) {
				// perfect match, return right away
				return warzone;
			} else if (warzone.getName().toLowerCase().startsWith(name.toLowerCase())) {
				// perhaps there's a perfect match in the remaining zones, let's take this one aside
				bestGuess = warzone;
			}
		}
		return bestGuess;
	}

	public static Warzone getZoneByLocation(Location location) {
		for (Warzone warzone : War.war.getWarzones()) {
			if (location.getWorld().getName().equals(warzone.getWorld().getName()) && warzone.getVolume() != null && warzone.getVolume().contains(location)) {
				return warzone;
			}
		}
		return null;
	}

	public static Warzone getZoneByLocation(Player player) {
		return Warzone.getZoneByLocation(player.getLocation());
	}

	public static Warzone getZoneByPlayerName(String playerName) {
		for (Warzone warzone : War.war.getWarzones()) {
			Team team = warzone.getPlayerTeam(playerName);
			if (team != null) {
				return warzone;
			}
		}
		return null;
	}

	public static Warzone getZoneByTeam(Team team) {
		for (Warzone warzone : War.war.getWarzones()) {
			for (Team teamToCheck : warzone.getTeams()) {
				if (teamToCheck.getName().equals(team.getName())) {
					return warzone;
				}				
			}

		}
		return null;
	}

	public boolean ready() {
		if (this.volume.hasTwoCorners() && !this.volume.tooSmall() && !this.volume.tooBig()) {
			return true;
		}
		return false;
	}

	public List<Team> getTeams() {
		return this.teams;
	}

	public Team getPlayerTeam(String playerName) {
		for (Team team : this.teams) {
			for (Player player : team.getPlayers()) {
				if (player.getName().equals(playerName)) {
					return team;
				}
			}
		}
		return null;
	}

	public String getTeamInformation() {
		String teamsMessage = "Teams: ";
		if (this.getTeams().isEmpty()) {
			teamsMessage += "none.";
		} else {
			for (Team team : this.getTeams()) {
				teamsMessage += team.getName() + " (" + team.getPoints() + " points, " + team.getRemainingLifes() + "/" 
					+ team.getTeamConfig().resolveInt(TeamConfig.LIFEPOOL) + " lives left. ";
				for (Player member : team.getPlayers()) {
					teamsMessage += member.getName() + " ";
				}
				teamsMessage += ")  ";
			}
		}
		return teamsMessage;
	}

	public String getName() {
		return this.name;
	}

	@Override
	public String toString() {
		return this.getName();
	}

	public void setTeleport(Location location) {
		this.teleport = location;
	}

	public Location getTeleport() {
		return this.teleport;
	}

	public int saveState(boolean clearArtifacts) {
		if (this.ready()) {
			if (clearArtifacts) {
				// removed everything to keep save clean
				for (ZoneWallGuard guard : this.zoneWallGuards) {
					guard.deactivate();
				}
				this.zoneWallGuards.clear();

				for (Team team : this.teams) {
					team.getSpawnVolume().resetBlocks();
					if (team.getTeamFlag() != null) {
						team.getFlagVolume().resetBlocks();
					}
				}

				for (Monument monument : this.monuments) {
					monument.getVolume().resetBlocks();
				}

				if (this.lobby != null) {
					this.lobby.getVolume().resetBlocks();
				}
			}

			int saved = this.volume.saveBlocks();
			if (clearArtifacts) {
				this.initializeZone(); // bring back stuff
			}
			return saved;
		}
		return 0;
	}

	/**
	 * Goes back to the saved state of the warzone (resets only block types, not physics). Also teleports all players back to their respective spawns.
	 *
	 * @return
	 */
	public void initializeZone() {
		this.initializeZone(null);
	}

	public void initializeZone(Player respawnExempted) {
		if (this.ready() && this.volume.isSaved()) {
			// everyone back to team spawn with full health
			for (Team team : this.teams) {
				for (Player player : team.getPlayers()) {
					if (respawnExempted == null 
							|| (respawnExempted != null
									&& !player.getName().equals(respawnExempted.getName()))) {
						this.respawnPlayer(team, player);
					}
				}
				team.setRemainingLives(team.getTeamConfig().resolveInt(TeamConfig.LIFEPOOL));
				team.initializeTeamSpawn();
				if (team.getTeamFlag() != null) {
					team.setTeamFlag(team.getTeamFlag());
				}
			}

			this.initZone();
			
			if (War.war.getWarHub() != null) {
				War.war.getWarHub().resetZoneSign(this);
			}
		}
		
		// Don't forget to reset these to false, or we won't be able to score or empty lifepools anymore
		this.isReinitializing = false;
		this.isEndOfGame = false;
	}

	public void initializeZoneAsJob(Player respawnExempted) {
		InitZoneJob job = new InitZoneJob(this, respawnExempted);
		War.war.getServer().getScheduler().scheduleSyncDelayedTask(War.war, job);
	}

	public void initializeZoneAsJob() {
		InitZoneJob job = new InitZoneJob(this);
		War.war.getServer().getScheduler().scheduleSyncDelayedTask(War.war, job);
	}

	private void initZone() {
		// reset monuments
		for (Monument monument : this.monuments) {
			monument.getVolume().resetBlocks();
			monument.addMonumentBlocks();
		}
		
		// reset bombs
		for (Bomb bomb : this.bombs) {
			bomb.getVolume().resetBlocks();
			bomb.addBombBlocks();
		}
		
		// reset cakes
		for (Cake cake : this.cakes) {
			cake.getVolume().resetBlocks();
			cake.addCakeBlocks();
		}

		// reset lobby (here be demons)
		if (this.lobby != null) {
			if (this.lobby.getVolume() != null) {
				this.lobby.getVolume().resetBlocks();
			}
			this.lobby.initialize();
		}

		this.flagThieves.clear();
		this.bombThieves.clear();
		this.cakeThieves.clear();
		this.reallyDeadFighters.clear();

		// nom drops
		for(Entity entity : (this.getWorld().getEntities())) {
			if (!(entity instanceof Item)) {
				continue;
			}
			
			// validate position
			if (!this.getVolume().contains(entity.getLocation())) {
				continue;
			}

			// omnomnomnom
			entity.remove();
		}
	}

	public void endRound() {

	}

	public void respawnPlayer(Team team, Player player) {
		this.handleRespawn(team, player);
		// Teleport the player back to spawn
		player.teleport(team.getTeamSpawn());
	}

	public void respawnPlayer(PlayerMoveEvent event, Team team, Player player) {
		this.handleRespawn(team, player);
		// Teleport the player back to spawn
		event.setTo(team.getTeamSpawn());
	}
	
	public boolean isRespawning(Player p) {
		return respawn.contains(p);
	}

	private void handleRespawn(final Team team, final Player player) {
		// Fill hp
		player.setRemainingAir(300);
		player.setHealth(20);
		player.setFoodLevel(20);
		player.setSaturation(team.getTeamConfig().resolveInt(TeamConfig.SATURATION));
		player.setExhaustion(0);
		player.setFireTicks(0);		//this works fine here, why put it in LoudoutResetJob...? I'll keep it over there though
		
		player.getOpenInventory().close();
		player.getInventory().clear();
		
		if (player.getGameMode() == GameMode.CREATIVE) {
			// Players are always in survival mode in warzones
			player.setGameMode(GameMode.SURVIVAL);
		}
		
		// clear potion effects
		PotionEffectHelper.clearPotionEffects(player);
		
		boolean isFirstRespawn = false;
		if (!this.getLoadoutSelections().keySet().contains(player.getName())) {
			isFirstRespawn = true;
			this.getLoadoutSelections().put(player.getName(), new LoadoutSelection(true, 0));
		} else if (this.isReinitializing) {
			isFirstRespawn = true;
			this.getLoadoutSelections().get(player.getName()).setStillInSpawn(true);
		} else {
			this.getLoadoutSelections().get(player.getName()).setStillInSpawn(true);
		}
		
		// Spout
		if (War.war.isSpoutServer()) {
			SpoutManager.getPlayer(player).setTitle(team.getKind().getColor() + player.getName());
		}
		
		final LoadoutResetJob job = new LoadoutResetJob(this, team, player, isFirstRespawn, false);
		if (team.getTeamConfig().resolveInt(TeamConfig.RESPAWNTIMER) == 0 || isFirstRespawn) {
			job.run();
		}			
		else {
			// "Respawn" Timer - player will not be able to leave spawn for a few seconds
			respawn.add(player);
			
			War.war.getServer().getScheduler().scheduleSyncDelayedTask(War.war, new Runnable() {
				public void run() {
				    respawn.remove(player);
					War.war.getServer().getScheduler().scheduleSyncDelayedTask(War.war, job);
				}
			}, team.getTeamConfig().resolveInt(TeamConfig.RESPAWNTIMER) * 20L); // 20 ticks = 1 second
		}
	}

	public void resetInventory(Team team, Player player, HashMap<Integer, ItemStack> loadout) {
		// Reset inventory to loadout
		PlayerInventory playerInv = player.getInventory();
		playerInv.clear();
		playerInv.clear(playerInv.getSize() + 0);
		playerInv.clear(playerInv.getSize() + 1);
		playerInv.clear(playerInv.getSize() + 2);
		playerInv.clear(playerInv.getSize() + 3); // helmet/blockHead
		boolean helmetIsInLoadout = false;
		for (Integer slot : loadout.keySet()) {
			if (slot == 100) {
				playerInv.setBoots(War.war.copyStack(loadout.get(slot)));
			} else if (slot == 101) {
				playerInv.setLeggings(War.war.copyStack(loadout.get(slot)));
			} else if (slot == 102) {
				playerInv.setChestplate(War.war.copyStack(loadout.get(slot)));
			} else if (slot == 103) {
				playerInv.setHelmet(War.war.copyStack(loadout.get(slot)));
				helmetIsInLoadout = true;
			} else {
				ItemStack item = loadout.get(slot);
				if (item != null) {
					playerInv.addItem(War.war.copyStack(item));
				}
			}
		}
		if (this.getWarzoneConfig().getBoolean(WarzoneConfig.BLOCKHEADS)) {
			playerInv.setHelmet(new ItemStack(team.getKind().getMaterial(), 1, (short) 1, new Byte(team.getKind().getData())));
		} else {
			if (!helmetIsInLoadout) {
				ItemStack helmet = new ItemStack(Material.LEATHER_HELMET);
				LeatherArmorMeta meta = (LeatherArmorMeta) helmet.getItemMeta();
				meta.setColor(team.getKind().getBukkitColor());
				helmet.setItemMeta(meta);
				playerInv.setHelmet(helmet);
			}
		}
	}

	public boolean isMonumentCenterBlock(Block block) {
		for (Monument monument : this.monuments) {
			int x = monument.getLocation().getBlockX();
			int y = monument.getLocation().getBlockY() + 1;
			int z = monument.getLocation().getBlockZ();
			if (x == block.getX() && y == block.getY() && z == block.getZ()) {
				return true;
			}
		}
		return false;
	}

	public Monument getMonumentFromCenterBlock(Block block) {
		for (Monument monument : this.monuments) {
			int x = monument.getLocation().getBlockX();
			int y = monument.getLocation().getBlockY() + 1;
			int z = monument.getLocation().getBlockZ();
			if (x == block.getX() && y == block.getY() && z == block.getZ()) {
				return monument;
			}
		}
		return null;
	}

	public boolean nearAnyOwnedMonument(Location to, Team team) {
		for (Monument monument : this.monuments) {
			if (monument.isNear(to) && monument.isOwner(team)) {
				return true;
			}
		}
		return false;
	}

	public List<Monument> getMonuments() {
		return this.monuments;
	}

	public boolean hasPlayerState(String playerName) {
		return this.playerStates.containsKey(playerName);
	}

	public void keepPlayerState(Player player) {
		PlayerInventory inventory = player.getInventory();
		ItemStack[] contents = inventory.getContents();
		
		String playerTitle = player.getName();
		if (War.war.isSpoutServer()) {
			playerTitle = SpoutManager.getPlayer(player).getTitle();
		}
		
		this.playerStates.put(player.getName(), new PlayerState(player.getGameMode(), 
																contents, 
																inventory.getHelmet(), 
																inventory.getChestplate(), 
																inventory.getLeggings(), 
																inventory.getBoots(), 
																player.getHealth(), 
																player.getExhaustion(), 
																player.getSaturation(), 
																player.getFoodLevel(), 
																player.getActivePotionEffects(),
																playerTitle,
																player.getLevel(),
																player.getExp()));
	}

	public void restorePlayerState(Player player) {
		PlayerState originalState = this.playerStates.remove(player.getName());
		PlayerInventory playerInv = player.getInventory();
		if (originalState != null) {
			player.getOpenInventory().close();
			this.playerInvFromInventoryStash(playerInv, originalState);
			player.setGameMode(originalState.getGamemode());
			player.setHealth(originalState.getHealth());
			player.setExhaustion(originalState.getExhaustion());
			player.setSaturation(originalState.getSaturation());
			player.setFoodLevel(originalState.getFoodLevel());
			PotionEffectHelper.restorePotionEffects(player, originalState.getPotionEffects());
			player.setLevel(originalState.getLevel());
			player.setExp(originalState.getExp());
			
			if (War.war.isSpoutServer()) {
				SpoutManager.getPlayer(player).setTitle(originalState.getPlayerTitle());
			}
		}
	}

	private void playerInvFromInventoryStash(PlayerInventory playerInv, PlayerState originalContents) {
		playerInv.clear();
		
		playerInv.clear(playerInv.getSize() + 0);
		playerInv.clear(playerInv.getSize() + 1);
		playerInv.clear(playerInv.getSize() + 2);
		playerInv.clear(playerInv.getSize() + 3); // helmet/blockHead
		
		int invIndex = 0;
		for (ItemStack item : originalContents.getContents()) {
			if (item != null && item.getTypeId() != 0) {
				playerInv.setItem(invIndex, item);
			}
			invIndex++;
		}
		
		if (originalContents.getHelmet() != null) {
			playerInv.setHelmet(originalContents.getHelmet());
		}
		if (originalContents.getChest() != null) {
			playerInv.setChestplate(originalContents.getChest());
		}
		if (originalContents.getLegs() != null) {
			playerInv.setLeggings(originalContents.getLegs());
		}
		if (originalContents.getFeet() != null) {
			playerInv.setBoots(originalContents.getFeet());
		}
	}

	public boolean hasMonument(String monumentName) {
		for (Monument monument : this.monuments) {
			if (monument.getName().startsWith(monumentName)) {
				return true;
			}
		}
		return false;
	}

	public Monument getMonument(String monumentName) {
		for (Monument monument : this.monuments) {
			if (monument.getName().startsWith(monumentName)) {
				return monument;
			}
		}
		return null;
	}
	
	public boolean hasBomb(String bombName) {
		for (Bomb bomb : this.bombs) {
			if (bomb.getName().equals(bombName)) {
				return true;
			}
		}
		return false;
	}

	public Bomb getBomb(String bombName) {
		for (Bomb bomb : this.bombs) {
			if (bomb.getName().startsWith(bombName)) {
				return bomb;
			}
		}
		return null;
	}
	
	public boolean hasCake(String cakeName) {
		for (Cake cake : this.cakes) {
			if (cake.getName().equals(cakeName)) {
				return true;
			}
		}
		return false;
	}

	public Cake getCake(String cakeName) {
		for (Cake cake : this.cakes) {
			if (cake.getName().startsWith(cakeName)) {
				return cake;
			}
		}
		return null;
	}

	public boolean isImportantBlock(Block block) {
		if (this.ready()) {
			for (Monument m : this.monuments) {
				if (m.getVolume().contains(block)) {
					return true;
				}
			}
			for (Bomb b : this.bombs) {
				if (b.getVolume().contains(block)) {
					return true;
				}
			}
			for (Cake c : this.cakes) {
				if (c.getVolume().contains(block)) {
					return true;
				}
			}
			for (Team t : this.teams) {
				if (t.getSpawnVolume().contains(block)) {
					return true;
				} else if (t.getFlagVolume() != null && t.getFlagVolume().contains(block)) {
					return true;
				}
			}
			if (this.volume.isWallBlock(block)) {
				return true;
			}
		}
		return false;
	}

	public World getWorld() {

		return this.world;
	}

	public void setWorld(World world) {
		this.world = world;
	}

	public ZoneVolume getVolume() {
		return this.volume;
	}

	public void setVolume(ZoneVolume zoneVolume) {
		this.volume = zoneVolume;
	}

	public Team getTeamByKind(TeamKind kind) {
		for (Team t : this.teams) {
			if (t.getKind() == kind) {
				return t;
			}
		}
		return null;
	}
    public boolean soupHealing() {
		return (this.getWarzoneConfig().getBoolean(WarzoneConfig.SOUPHEALING));   	
    }
	public boolean isNearWall(Location latestPlayerLocation) {
		if (this.volume.hasTwoCorners()) {
			if (Math.abs(this.volume.getSoutheastZ() - latestPlayerLocation.getBlockZ()) < this.minSafeDistanceFromWall && latestPlayerLocation.getBlockX() <= this.volume.getSoutheastX() && latestPlayerLocation.getBlockX() >= this.volume.getNorthwestX() && latestPlayerLocation.getBlockY() >= this.volume.getMinY() && latestPlayerLocation.getBlockY() <= this.volume.getMaxY()) {
				return true; // near east wall
			} else if (Math.abs(this.volume.getSoutheastX() - latestPlayerLocation.getBlockX()) < this.minSafeDistanceFromWall && latestPlayerLocation.getBlockZ() <= this.volume.getNorthwestZ() && latestPlayerLocation.getBlockZ() >= this.volume.getSoutheastZ() && latestPlayerLocation.getBlockY() >= this.volume.getMinY() && latestPlayerLocation.getBlockY() <= this.volume.getMaxY()) {
				return true; // near south wall
			} else if (Math.abs(this.volume.getNorthwestX() - latestPlayerLocation.getBlockX()) < this.minSafeDistanceFromWall && latestPlayerLocation.getBlockZ() <= this.volume.getNorthwestZ() && latestPlayerLocation.getBlockZ() >= this.volume.getSoutheastZ() && latestPlayerLocation.getBlockY() >= this.volume.getMinY() && latestPlayerLocation.getBlockY() <= this.volume.getMaxY()) {
				return true; // near north wall
			} else if (Math.abs(this.volume.getNorthwestZ() - latestPlayerLocation.getBlockZ()) < this.minSafeDistanceFromWall && latestPlayerLocation.getBlockX() <= this.volume.getSoutheastX() && latestPlayerLocation.getBlockX() >= this.volume.getNorthwestX() && latestPlayerLocation.getBlockY() >= this.volume.getMinY() && latestPlayerLocation.getBlockY() <= this.volume.getMaxY()) {
				return true; // near west wall
			} else if (Math.abs(this.volume.getMaxY() - latestPlayerLocation.getBlockY()) < this.minSafeDistanceFromWall && latestPlayerLocation.getBlockX() <= this.volume.getMaxX() && latestPlayerLocation.getBlockX() >= this.volume.getMinX() && latestPlayerLocation.getBlockZ() <= this.volume.getMaxZ() && latestPlayerLocation.getBlockZ() >= this.volume.getMinZ()) {
				return true; // near up wall
			} else if (Math.abs(this.volume.getMinY() - latestPlayerLocation.getBlockY()) < this.minSafeDistanceFromWall && latestPlayerLocation.getBlockX() <= this.volume.getMaxX() && latestPlayerLocation.getBlockX() >= this.volume.getMinX() && latestPlayerLocation.getBlockZ() <= this.volume.getMaxZ() && latestPlayerLocation.getBlockZ() >= this.volume.getMinZ()) {
				return true; // near down wall
			}
		}
		return false;
	}

	public List<Block> getNearestWallBlocks(Location latestPlayerLocation) {
		List<Block> nearestWallBlocks = new ArrayList<Block>();
		if (Math.abs(this.volume.getSoutheastZ() - latestPlayerLocation.getBlockZ()) < this.minSafeDistanceFromWall && latestPlayerLocation.getBlockX() <= this.volume.getSoutheastX() && latestPlayerLocation.getBlockX() >= this.volume.getNorthwestX() && latestPlayerLocation.getBlockY() >= this.volume.getMinY() && latestPlayerLocation.getBlockY() <= this.volume.getMaxY()) {
			// near east wall
			Block eastWallBlock = this.world.getBlockAt(latestPlayerLocation.getBlockX() + 1, latestPlayerLocation.getBlockY() + 1, this.volume.getSoutheastZ());
			nearestWallBlocks.add(eastWallBlock);
		}

		if (Math.abs(this.volume.getSoutheastX() - latestPlayerLocation.getBlockX()) < this.minSafeDistanceFromWall && latestPlayerLocation.getBlockZ() <= this.volume.getNorthwestZ() && latestPlayerLocation.getBlockZ() >= this.volume.getSoutheastZ() && latestPlayerLocation.getBlockY() >= this.volume.getMinY() && latestPlayerLocation.getBlockY() <= this.volume.getMaxY()) {
			// near south wall
			Block southWallBlock = this.world.getBlockAt(this.volume.getSoutheastX(), latestPlayerLocation.getBlockY() + 1, latestPlayerLocation.getBlockZ());
			nearestWallBlocks.add(southWallBlock);
		}

		if (Math.abs(this.volume.getNorthwestX() - latestPlayerLocation.getBlockX()) < this.minSafeDistanceFromWall && latestPlayerLocation.getBlockZ() <= this.volume.getNorthwestZ() && latestPlayerLocation.getBlockZ() >= this.volume.getSoutheastZ() && latestPlayerLocation.getBlockY() >= this.volume.getMinY() && latestPlayerLocation.getBlockY() <= this.volume.getMaxY()) {
			// near north wall
			Block northWallBlock = this.world.getBlockAt(this.volume.getNorthwestX(), latestPlayerLocation.getBlockY() + 1, latestPlayerLocation.getBlockZ());
			nearestWallBlocks.add(northWallBlock);
		}

		if (Math.abs(this.volume.getNorthwestZ() - latestPlayerLocation.getBlockZ()) < this.minSafeDistanceFromWall && latestPlayerLocation.getBlockX() <= this.volume.getSoutheastX() && latestPlayerLocation.getBlockX() >= this.volume.getNorthwestX() && latestPlayerLocation.getBlockY() >= this.volume.getMinY() && latestPlayerLocation.getBlockY() <= this.volume.getMaxY()) {
			// near west wall
			Block westWallBlock = this.world.getBlockAt(latestPlayerLocation.getBlockX(), latestPlayerLocation.getBlockY() + 1, this.volume.getNorthwestZ());
			nearestWallBlocks.add(westWallBlock);
		}

		if (Math.abs(this.volume.getMaxY() - latestPlayerLocation.getBlockY()) < this.minSafeDistanceFromWall && latestPlayerLocation.getBlockX() <= this.volume.getMaxX() && latestPlayerLocation.getBlockX() >= this.volume.getMinX() && latestPlayerLocation.getBlockZ() <= this.volume.getMaxZ() && latestPlayerLocation.getBlockZ() >= this.volume.getMinZ()) {
			// near up wall
			Block upWallBlock = this.world.getBlockAt(latestPlayerLocation.getBlockX(), this.volume.getMaxY(), latestPlayerLocation.getBlockZ());
			nearestWallBlocks.add(upWallBlock);
		}

		if (Math.abs(this.volume.getMinY() - latestPlayerLocation.getBlockY()) < this.minSafeDistanceFromWall && latestPlayerLocation.getBlockX() <= this.volume.getMaxX() && latestPlayerLocation.getBlockX() >= this.volume.getMinX() && latestPlayerLocation.getBlockZ() <= this.volume.getMaxZ() && latestPlayerLocation.getBlockZ() >= this.volume.getMinZ()) {
			// near down wall
			Block downWallBlock = this.world.getBlockAt(latestPlayerLocation.getBlockX(), this.volume.getMinY(), latestPlayerLocation.getBlockZ());
			nearestWallBlocks.add(downWallBlock);
		}
		return nearestWallBlocks;
		// note: y + 1 to line up 3 sided square with player eyes
	}

	public List<BlockFace> getNearestWalls(Location latestPlayerLocation) {
		List<BlockFace> walls = new ArrayList<BlockFace>();
		if (Math.abs(this.volume.getSoutheastZ() - latestPlayerLocation.getBlockZ()) < this.minSafeDistanceFromWall && latestPlayerLocation.getBlockX() <= this.volume.getSoutheastX() && latestPlayerLocation.getBlockX() >= this.volume.getNorthwestX() && latestPlayerLocation.getBlockY() >= this.volume.getMinY() && latestPlayerLocation.getBlockY() <= this.volume.getMaxY()) {
			// near east wall
			walls.add(Direction.EAST());
		}

		if (Math.abs(this.volume.getSoutheastX() - latestPlayerLocation.getBlockX()) < this.minSafeDistanceFromWall && latestPlayerLocation.getBlockZ() <= this.volume.getNorthwestZ() && latestPlayerLocation.getBlockZ() >= this.volume.getSoutheastZ() && latestPlayerLocation.getBlockY() >= this.volume.getMinY() && latestPlayerLocation.getBlockY() <= this.volume.getMaxY()) {
			// near south wall
			walls.add(Direction.SOUTH());
		}

		if (Math.abs(this.volume.getNorthwestX() - latestPlayerLocation.getBlockX()) < this.minSafeDistanceFromWall && latestPlayerLocation.getBlockZ() <= this.volume.getNorthwestZ() && latestPlayerLocation.getBlockZ() >= this.volume.getSoutheastZ() && latestPlayerLocation.getBlockY() >= this.volume.getMinY() && latestPlayerLocation.getBlockY() <= this.volume.getMaxY()) {
			// near north wall
			walls.add(Direction.NORTH());
		}

		if (Math.abs(this.volume.getNorthwestZ() - latestPlayerLocation.getBlockZ()) < this.minSafeDistanceFromWall && latestPlayerLocation.getBlockX() <= this.volume.getSoutheastX() && latestPlayerLocation.getBlockX() >= this.volume.getNorthwestX() && latestPlayerLocation.getBlockY() >= this.volume.getMinY() && latestPlayerLocation.getBlockY() <= this.volume.getMaxY()) {
			// near west wall
			walls.add(Direction.WEST());
		}

		if (Math.abs(this.volume.getMaxY() - latestPlayerLocation.getBlockY()) < this.minSafeDistanceFromWall && latestPlayerLocation.getBlockX() <= this.volume.getMaxX() && latestPlayerLocation.getBlockX() >= this.volume.getMinX() && latestPlayerLocation.getBlockZ() <= this.volume.getMaxZ() && latestPlayerLocation.getBlockZ() >= this.volume.getMinZ()) {
			// near up wall
			walls.add(BlockFace.UP);
		}

		if (Math.abs(this.volume.getMinY() - latestPlayerLocation.getBlockY()) < this.minSafeDistanceFromWall && latestPlayerLocation.getBlockX() <= this.volume.getMaxX() && latestPlayerLocation.getBlockX() >= this.volume.getMinX() && latestPlayerLocation.getBlockZ() <= this.volume.getMaxZ() && latestPlayerLocation.getBlockZ() >= this.volume.getMinZ()) {
			// near down wall
			walls.add(BlockFace.DOWN);
		}
		return walls;
	}

	public ZoneWallGuard getPlayerZoneWallGuard(String name, BlockFace wall) {
		for (ZoneWallGuard guard : this.zoneWallGuards) {
			if (guard.getPlayer().getName().equals(name) && wall == guard.getWall()) {
				return guard;
			}
		}
		return null;
	}

	public boolean protectZoneWallAgainstPlayer(Player player) {
		List<BlockFace> nearestWalls = this.getNearestWalls(player.getLocation());
		boolean protecting = false;
		for (BlockFace wall : nearestWalls) {
			ZoneWallGuard guard = this.getPlayerZoneWallGuard(player.getName(), wall);
			if (guard != null) {
				// already protected, need to move the guard
				guard.updatePlayerPosition(player.getLocation());
			} else {
				// new guard
				guard = new ZoneWallGuard(player, War.war, this, wall);
				this.zoneWallGuards.add(guard);
			}
			protecting = true;
		}
		return protecting;
	}

	public void dropZoneWallGuardIfAny(Player player) {
		List<ZoneWallGuard> playerGuards = new ArrayList<ZoneWallGuard>();
		for (ZoneWallGuard guard : this.zoneWallGuards) {
			if (guard.getPlayer().getName().equals(player.getName())) {
				playerGuards.add(guard);
				guard.deactivate();
			}
		}
		// now remove those zone guards
		for (ZoneWallGuard playerGuard : playerGuards) {
			this.zoneWallGuards.remove(playerGuard);
		}
		playerGuards.clear();
	}

	public void setLobby(ZoneLobby lobby) {
		this.lobby = lobby;
	}

	public ZoneLobby getLobby() {
		return this.lobby;
	}

	public Team autoAssign(Player player) {
		Team lowestNoOfPlayers = null;
		for (Team t : this.teams) {
			if (lowestNoOfPlayers == null || (lowestNoOfPlayers != null && lowestNoOfPlayers.getPlayers().size() > t.getPlayers().size())) {
				if (War.war.canPlayWar(player, t)) {
						lowestNoOfPlayers = t;
				}
			}
		}
		if (lowestNoOfPlayers != null) {
			if (player.getWorld() != this.getWorld()) {
				player.teleport(this.getWorld().getSpawnLocation());
			}
			lowestNoOfPlayers.addPlayer(player);
			lowestNoOfPlayers.resetSign();
			if (!this.hasPlayerState(player.getName())) {
				this.keepPlayerState(player);
			}
			War.war.msg(player, "Your inventory is in storage until you use '/war leave'.");
			this.respawnPlayer(lowestNoOfPlayers, player);
			for (Team team : this.teams) {
				team.teamcast("" + player.getName() + " joined team " + lowestNoOfPlayers.getName() + ".");
			}
		}
		return lowestNoOfPlayers;
	}

	public void handleDeath(Player player) {
		// THIS ISN'T THREAD SAFE
		// Every death and player movement should ideally occur in sequence because
		// 1) a death can cause the end of the game by emptying a lifepool causing the max score to be reached
		// 2) a player movement from one block to the next (getting a flag home or causing a bomb to go off perhaps) could win the game
		//
		// Concurrent execution of these events could cause the inventory reset of the last players to die to fail as
		// they get tp'ed back to the lobby, or perhaps kills to bleed into the next round.

		Team playerTeam = Team.getTeamByPlayerName(player.getName());

		// Make sure the player that died is still part of a team, game may have ended while waiting.
		// Ignore dying players that essentially just got tp'ed to lobby and got their state restored.
		// Gotta take care of restoring ReallyDeadFighters' game-end state when in onRespawn as well.
		if (playerTeam != null) {
			// teleport to team spawn upon fast respawn death, but not for real deaths
			if (!this.getWarzoneConfig().getBoolean(WarzoneConfig.REALDEATHS)) {
				this.respawnPlayer(playerTeam, player);
			} else {
				// onPlayerRespawn takes care of real deaths
				//player.setHealth(0);
				this.getReallyDeadFighters().add(player.getName());
			}
			
			int remaining = playerTeam.getRemainingLifes();
			if (remaining == 0) { // your death caused your team to lose
				if (this.isReinitializing()) {
					// Prevent duplicate battle end. You died just after the battle ending death.
					this.respawnPlayer(playerTeam, player);
				} else {
					// Handle team loss
					List<Team> teams = this.getTeams();
					String scores = "";
					for (Team t : teams) {
						if (War.war.isSpoutServer()) {
							for (Player p : t.getPlayers()) {
								SpoutPlayer sp = SpoutManager.getPlayer(p);
								if (sp.isSpoutCraftEnabled()) {
					                sp.sendNotification(
					                		SpoutDisplayer.cleanForNotification("Round over! " + playerTeam.getKind().getColor() + playerTeam.getName()),
					                		SpoutDisplayer.cleanForNotification("ran out of lives."),
					                		playerTeam.getKind().getMaterial(),
					                		playerTeam.getKind().getData(),
					                		10000);
								}
							}
						}
						
						t.teamcast("The battle is over. Team " + playerTeam.getName() + " lost: " + player.getName() + " died and there were no lives left in their life pool.");
	
						if (t.getPlayers().size() != 0 && !t.getTeamConfig().resolveBoolean(TeamConfig.FLAGPOINTSONLY)) {
							if (!t.getName().equals(playerTeam.getName())) {
								// all other teams get a point
								t.addPoint();
								t.resetSign();
							}
							scores += t.getName() + "(" + t.getPoints() + "/" + t.getTeamConfig().resolveInt(TeamConfig.MAXSCORE) + ") ";
						}
					}
					
					if (!scores.equals("")) {
						for (Team t : teams) {
							t.teamcast("New scores - " + scores);
						}
					}
					
					// detect score cap
					List<Team> scoreCapTeams = new ArrayList<Team>();
					for (Team t : teams) {
						if (t.getPoints() == t.getTeamConfig().resolveInt(TeamConfig.MAXSCORE)) {
							scoreCapTeams.add(t);
						}
					}
					if (!scoreCapTeams.isEmpty()) {
						String winnersStr = "";
						for (Team winner : scoreCapTeams) {
							if (winner.getPlayers().size() != 0) {
								winnersStr += winner.getName() + " ";
							}
						}
	
						this.handleScoreCapReached(winnersStr);
					} else {
						// A new battle starts. Reset the zone but not the teams.
						for (Team t : teams) {
							t.teamcast("A new battle begins. Resetting warzone...");
						}
						
						this.reinitialize();
					}
				}
			} else {
				// player died without causing his team's demise
				if (this.isFlagThief(player.getName())) {
					// died while carrying flag.. dropped it
					Team victim = this.getVictimTeamForFlagThief(player.getName());
					victim.getFlagVolume().resetBlocks();
					victim.initializeTeamFlag();
					this.removeFlagThief(player.getName());
					
					if (War.war.isSpoutServer()) {
						for (Player p : victim.getPlayers()) {
							SpoutPlayer sp = SpoutManager.getPlayer(p);
							if (sp.isSpoutCraftEnabled()) {
				                sp.sendNotification(
			                		SpoutDisplayer.cleanForNotification(playerTeam.getKind().getColor() + player.getName() + ChatColor.YELLOW + " dropped"),
			                		SpoutDisplayer.cleanForNotification(ChatColor.YELLOW + "your flag."),
			                		playerTeam.getKind().getMaterial(),
			                		playerTeam.getKind().getData(),
			                		5000);
							}
						}
					}
					
					for (Team t : this.getTeams()) {
						t.teamcast(player.getName() + " died and dropped team " + victim.getName() + "'s flag.");
					}
				}
				
				// Bomb thieves
				if (this.isBombThief(player.getName())) {
					// died while carrying bomb.. dropped it
					Bomb bomb = this.getBombForThief(player.getName());
					bomb.getVolume().resetBlocks();
					bomb.addBombBlocks();
					this.removeBombThief(player.getName());
					
					for (Team t : this.getTeams()) {
						t.teamcast(player.getName() + " died and dropped bomb " + ChatColor.GREEN + bomb.getName() + ChatColor.WHITE + ".");
						if (War.war.isSpoutServer()) {
							for (Player p : t.getPlayers()) {
								SpoutPlayer sp = SpoutManager.getPlayer(p);
								if (sp.isSpoutCraftEnabled()) {
					                sp.sendNotification(
				                		SpoutDisplayer.cleanForNotification(playerTeam.getKind().getColor() + player.getName() + ChatColor.YELLOW + " dropped"),
				                		SpoutDisplayer.cleanForNotification(ChatColor.YELLOW + "bomb " + ChatColor.GREEN + bomb.getName() + ChatColor.YELLOW + "."),
				                		Material.TNT,
				                		(short)0,
				                		5000);
								}
							}
						}
					}
				}
				
				if (this.isCakeThief(player.getName())) {
					// died while carrying cake.. dropped it
					Cake cake = this.getCakeForThief(player.getName());
					cake.getVolume().resetBlocks();
					cake.addCakeBlocks();
					this.removeCakeThief(player.getName());
					
					for (Team t : this.getTeams()) {
						t.teamcast(player.getName() + " died and dropped cake " + ChatColor.GREEN + cake.getName() + ChatColor.WHITE + ".");
						if (War.war.isSpoutServer()) {
							for (Player p : t.getPlayers()) {
								SpoutPlayer sp = SpoutManager.getPlayer(p);
								if (sp.isSpoutCraftEnabled()) {
					                sp.sendNotification(
				                		SpoutDisplayer.cleanForNotification(playerTeam.getKind().getColor() + player.getName() + ChatColor.YELLOW + " dropped"),
				                		SpoutDisplayer.cleanForNotification(ChatColor.YELLOW + "cake " + ChatColor.GREEN + cake.getName() + ChatColor.YELLOW + "."),
				                		Material.CAKE,
				                		(short)0,
				                		5000);
								}
							}
						}
					}
				}
				
				// Decrement lifepool
				playerTeam.setRemainingLives(remaining - 1);
				
				// Lifepool empty warning
				if (remaining - 1 == 0) {
					for (Team t : this.getTeams()) {
						t.teamcast("Team " + playerTeam.getName() + "'s life pool is empty. One more death and they lose the battle!");
					}
				}
			}
			playerTeam.resetSign();
		}
	}

	public void reinitialize() {
		this.isReinitializing = true;
		this.getVolume().resetBlocksAsJob();
		this.initializeZoneAsJob();
	}

	public void handlePlayerLeave(Player player, Location destination, PlayerMoveEvent event, boolean removeFromTeam) {
		this.handlePlayerLeave(player, removeFromTeam);
		event.setTo(destination);
	}

	public void handlePlayerLeave(Player player, Location destination, boolean removeFromTeam) {
		this.handlePlayerLeave(player, removeFromTeam);
		player.teleport(destination);
	}

	private void handlePlayerLeave(Player player, boolean removeFromTeam) {
		Team playerTeam = Team.getTeamByPlayerName(player.getName());
		if (playerTeam != null) {
			if (removeFromTeam) {
				playerTeam.removePlayer(player.getName());
			}
			for (Team t : this.getTeams()) {
				t.teamcast(playerTeam.getKind().getColor() + player.getName() + ChatColor.WHITE + " left the zone.");
			}
			playerTeam.resetSign();
			
			if (this.getLobby() != null) {
				this.getLobby().resetTeamGateSign(playerTeam);
			}
			if (this.hasPlayerState(player.getName())) {
				this.restorePlayerState(player);
			}
			if (this.getLoadoutSelections().containsKey(player.getName())) {
				// clear inventory selection
				this.getLoadoutSelections().remove(player.getName());
			}
			player.setFireTicks(0);
			player.setRemainingAir(300);
			
			// To hide stats
			if (War.war.isSpoutServer()) {
				War.war.getSpoutDisplayer().updateStats(player);
			}
			
			War.war.msg(player, "Your inventory is being restored.");
			if (War.war.getWarHub() != null) {
				War.war.getWarHub().resetZoneSign(this);
			}

			boolean zoneEmpty = true;
			for (Team team : this.getTeams()) {
				if (team.getPlayers().size() > 0) {
					zoneEmpty = false;
					break;
				}
			}
			if (zoneEmpty && this.getWarzoneConfig().getBoolean(WarzoneConfig.RESETONEMPTY)) {
				// reset the zone for a new game when the last player leaves
				for (Team team : this.getTeams()) {
					team.resetPoints();
					team.setRemainingLives(team.getTeamConfig().resolveInt(TeamConfig.LIFEPOOL));
				}
				this.getVolume().resetBlocksAsJob();
				this.initializeZoneAsJob();
				War.war.log("Last player left warzone " + this.getName() + ". Warzone blocks resetting automatically...", Level.INFO);
			}
		}
	}

	public boolean isEnemyTeamFlagBlock(Team playerTeam, Block block) {
		for (Team team : this.teams) {
			if (!team.getName().equals(playerTeam.getName()) && team.isTeamFlagBlock(block)) {
				return true;
			}
		}
		return false;
	}
	
	public boolean isFlagBlock(Block block) {
		for (Team team : this.teams) {
			if (team.isTeamFlagBlock(block)) {
				return true;
			}
		}
		return false;
	}

	public Team getTeamForFlagBlock(Block block) {
		for (Team team : this.teams) {
			if (team.isTeamFlagBlock(block)) {
				return team;
			}
		}
		return null;
	}
	
	public boolean isBombBlock(Block block) {
		for (Bomb bomb : this.bombs) {
			if (bomb.isBombBlock(block.getLocation())) {
				return true;
			}
		}
		return false;
	}

	public Bomb getBombForBlock(Block block) {
		for (Bomb bomb : this.bombs) {
			if (bomb.isBombBlock(block.getLocation())) {
				return bomb;
			}
		}
		return null;
	}
	
	public boolean isCakeBlock(Block block) {
		for (Cake cake : this.cakes) {
			if (cake.isCakeBlock(block.getLocation())) {
				return true;
			}
		}
		return false;
	}

	public Cake getCakeForBlock(Block block) {
		for (Cake cake : this.cakes) {
			if (cake.isCakeBlock(block.getLocation())) {
				return cake;
			}
		}
		return null;
	}

	// Flags
	public void addFlagThief(Team lostFlagTeam, String flagThief) {
		this.flagThieves.put(flagThief, lostFlagTeam);
	}

	public boolean isFlagThief(String suspect) {
		if (this.flagThieves.containsKey(suspect)) {
			return true;
		}
		return false;
	}

	public Team getVictimTeamForFlagThief(String thief) {
		return this.flagThieves.get(thief);
	}

	public void removeFlagThief(String thief) {
		this.flagThieves.remove(thief);
	}

	// Bomb
	public void addBombThief(Bomb bomb, String bombThief) {
		this.bombThieves.put(bombThief, bomb);
	}

	public boolean isBombThief(String suspect) {
		if (this.bombThieves.containsKey(suspect)) {
			return true;
		}
		return false;
	}

	public Bomb getBombForThief(String thief) {
		return this.bombThieves.get(thief);
	}

	public void removeBombThief(String thief) {
		this.bombThieves.remove(thief);
	}
	
	// Cake
	
	public void addCakeThief(Cake cake, String cakeThief) {
		this.cakeThieves.put(cakeThief, cake);
	}

	public boolean isCakeThief(String suspect) {
		if (this.cakeThieves.containsKey(suspect)) {
			return true;
		}
		return false;
	}

	public Cake getCakeForThief(String thief) {
		return this.cakeThieves.get(thief);
	}

	public void removeCakeThief(String thief) {
		this.cakeThieves.remove(thief);
	}

	public void clearThieves() {
		this.flagThieves.clear();
		this.bombThieves.clear();
		this.cakeThieves.clear();
	}

	public boolean isTeamFlagStolen(Team team) {
		for (String playerKey : this.flagThieves.keySet()) {
			if (this.flagThieves.get(playerKey).getName().equals(team.getName())) {
				return true;
			}
		}
		return false;
	}
	
	public void handleScoreCapReached(String winnersStr) {
		// Score cap reached. Reset everything.
		this.isEndOfGame = true;
		new ScoreCapReachedJob(this, winnersStr).run();	// run inventory and teleports immediately to avoid inv reset problems
		this.reinitialize();
	}

	public boolean isDeadMan(String playerName) {
		if (this.deadMenInventories.containsKey(playerName)) {
			return true;
		}
		return false;
	}

	public void restoreDeadmanInventory(Player player) {
		if (this.isDeadMan(player.getName())) {
			this.playerInvFromInventoryStash(player.getInventory(), this.deadMenInventories.get(player.getName()));
			this.deadMenInventories.remove(player.getName());
		}
	}

	public void setRallyPoint(Location location) {
		this.rallyPoint = location;
	}

	public Location getRallyPoint() {
		return this.rallyPoint;
	}

	public void unload() {
		War.war.log("Unloading zone " + this.getName() + "...", Level.INFO);
		for (Team team : this.getTeams()) {
			for (Player player : team.getPlayers()) {
				this.handlePlayerLeave(player, this.getTeleport(), false);
			}
			team.getPlayers().clear();
		}
		if (this.getLobby() != null) {
			this.getLobby().getVolume().resetBlocks();
		}
		if (this.getWarzoneConfig().getBoolean(WarzoneConfig.RESETONUNLOAD)) {
			this.getVolume().resetBlocks();
		}
	}

	public boolean isEnoughPlayers() {
		int teamsWithEnough = 0;
		for (Team team : teams) {
			if (team.getPlayers().size() >= this.getWarzoneConfig().getInt(WarzoneConfig.MINPLAYERS)) {
				teamsWithEnough++;
			}
		}
		if (teamsWithEnough >= this.getWarzoneConfig().getInt(WarzoneConfig.MINTEAMS)) {
			return true;
		}
		return false;
	}

	public HashMap<String, LoadoutSelection> getLoadoutSelections() {
		return loadoutSelections;
	}

	public boolean isAuthor(Player player) {
		// if no authors, all zonemakers can edit the zone
		return authors.size() == 0 || authors.contains(player.getName());
	}
		
	public void addAuthor(String playerName) {
		authors.add(playerName);
	}
	
	public List<String> getAuthors() {
		return this.authors;
	}

	public String getAuthorsString() {
		String authors = "";
		for (String author : this.getAuthors()) {
			authors += author + ",";
		}
		return authors;
	}

	public void equipPlayerLoadoutSelection(Player player, Team playerTeam, boolean isFirstRespawn, boolean isToggle) {
		LoadoutSelection selection = this.getLoadoutSelections().get(player.getName());
		if (selection != null && !this.isRespawning(player) && playerTeam.getPlayers().contains(player)) {
			// Make sure that inventory resets dont occur if player has already tp'ed out (due to game end, or somesuch) 
			// - repawn timer + this method is why inventories were getting wiped as players exited the warzone. 
			List<Loadout> loadouts = playerTeam.getInventories().resolveNewLoadouts();
			List<String> sortedNames = LoadoutYmlMapper.sortNames(Loadout.toLegacyFormat(loadouts));
			sortedNames.remove("first");
			for (Iterator<String> it = sortedNames.iterator(); it.hasNext();) {
				String loadoutName = it.next();
				Loadout ldt = Loadout.getLoadout(loadouts, loadoutName);
				if (ldt.requiresPermission() && !player.hasPermission(ldt.getPermission())) {
					it.remove();
				}
			}
			if (sortedNames.isEmpty()) {
				// Fix for zones that mistakenly only specify a `first' loadout, but do not add any others.
				this.handlePlayerLeave(player, this.getTeleport(), true);
				War.war.badMsg(player, "We couldn't find a loadout for you! Please alert the warzone maker to add a `default' loadout to this warzone.");
				return;
			}
			int currentIndex = selection.getSelectedIndex();
			Loadout firstLoadout = Loadout.getLoadout(loadouts, "first");
			int i = 0;
			Iterator<String> it = sortedNames.iterator();
			while (it.hasNext()) {
				String name = (String) it.next();
				if (i == currentIndex) {
					if (playerTeam.getTeamConfig().resolveBoolean(TeamConfig.PLAYERLOADOUTASDEFAULT) && name.equals("default")) {
						// Use player's own inventory as loadout
						this.resetInventory(playerTeam, player, this.getPlayerInventoryFromSavedState(player));
					} else if (isFirstRespawn && firstLoadout != null && name.equals("default")
							&& (firstLoadout.requiresPermission() ? player.hasPermission(firstLoadout.getPermission()) : true)) {
						// Get the loadout for the first spawn
						this.resetInventory(playerTeam, player, Loadout.getLoadout(loadouts, "first").getContents());
					} else {
						// Use the loadout from the list in the settings
						this.resetInventory(playerTeam, player, Loadout.getLoadout(loadouts, name).getContents());
					}
					if (isFirstRespawn && playerTeam.getInventories().resolveLoadouts().keySet().size() > 1) {
						War.war.msg(player, "Equipped " + name + " loadout (sneak to switch).");
					} else if (isToggle) {
						War.war.msg(player, "Equipped " + name + " loadout.");
					}
				}
				i++;
			}
		}
	}

	private HashMap<Integer, ItemStack> getPlayerInventoryFromSavedState(Player player) {
		HashMap<Integer, ItemStack> playerItems = new HashMap<Integer, ItemStack>();
		PlayerState originalState = this.playerStates.get(player.getName());

		if (originalState != null) {
			int invIndex = 0;
			playerItems = new HashMap<Integer, ItemStack>();
			for (ItemStack item : originalState.getContents()) {
				if (item != null && item.getTypeId() != 0) {
					playerItems.put(invIndex, item);
				}
				invIndex++;
			}
			if (originalState.getFeet() != null) {
				playerItems.put(100, originalState.getFeet());
			}
			if (originalState.getLegs() != null) {
				playerItems.put(101, originalState.getLegs());
			}
			if (originalState.getChest() != null) {
				playerItems.put(102, originalState.getChest());
			}
			if (originalState.getHelmet() != null) {
				playerItems.put(103, originalState.getHelmet());
			}
			
			if (War.war.isSpoutServer()) {
				SpoutManager.getPlayer(player).setTitle(originalState.getPlayerTitle());
			}
		}
		
		return playerItems;
	}

	public WarzoneConfigBag getWarzoneConfig() {
		return this.warzoneConfig;
	}
	
	public TeamConfigBag getTeamDefaultConfig() {
		return this.teamDefaultConfig;
	}

	public InventoryBag getDefaultInventories() {
		return this.defaultInventories ;
	}

	public List<Bomb> getBombs() {
		return bombs;
	}

	public List<Cake> getCakes() {
		return cakes;
	}

	public List<String> getReallyDeadFighters() {
		return this.reallyDeadFighters ;
	}

	public void gameEndTeleport(Player tp) {
		if (this.getRallyPoint() != null) {
			tp.teleport(this.getRallyPoint());
		} else {
			tp.teleport(this.getTeleport());
		}
		tp.setFireTicks(0);
		tp.setRemainingAir(300);
		
		if (this.hasPlayerState(tp.getName())) {
			this.restorePlayerState(tp);
		}		
	}

	public boolean isEndOfGame() {
		return this.isEndOfGame;
	}

	public boolean isReinitializing() {
		return this.isReinitializing;
	}

//	public Object getGameEndLock() {
//		return gameEndLock;
//	}

	public void setName(String newName) {
		this.name = newName;
		this.volume.setName(newName);
	}

	public HubLobbyMaterials getLobbyMaterials() {
		return this.lobbyMaterials;
	}

	public void setLobbyMaterials(HubLobbyMaterials lobbyMaterials) {
		this.lobbyMaterials = lobbyMaterials;
	}

	public boolean isOpponentSpawnPeripheryBlock(Team team, Block block) {
		for (Team maybeOpponent : this.getTeams()) {
			if (maybeOpponent != team) {
				Volume periphery = new Volume("periphery", this.getWorld());
				periphery.setCornerOne(new BlockInfo(maybeOpponent.getSpawnVolume().getMinX()-1 , maybeOpponent.getSpawnVolume().getMinY()-1, maybeOpponent.getSpawnVolume().getMinZ()-1, 0, (byte)0));
				periphery.setCornerTwo(new BlockInfo(maybeOpponent.getSpawnVolume().getMaxX()+1, maybeOpponent.getSpawnVolume().getMaxY()+1, maybeOpponent.getSpawnVolume().getMaxZ()+1, 0, (byte)0));
				
				if (periphery.contains(block)) {
					return true;
				}
			}
		}
		return false;
	}

	public void setWarzoneMaterials(WarzoneMaterials warzoneMaterials) {
		this.warzoneMaterials = warzoneMaterials;
	}

	public WarzoneMaterials getWarzoneMaterials() {
		return warzoneMaterials;
	}
}
