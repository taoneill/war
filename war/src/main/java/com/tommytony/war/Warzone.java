package com.tommytony.war;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import com.tommytony.war.mapper.VolumeMapper;
import com.tommytony.war.mapper.ZoneVolumeMapper;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.permissions.Permissible;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.getspout.spoutapi.SpoutManager;
import org.getspout.spoutapi.player.SpoutPlayer;

import com.google.common.collect.ImmutableList;
import com.tommytony.war.config.InventoryBag;
import com.tommytony.war.config.ScoreboardType;
import com.tommytony.war.config.TeamConfig;
import com.tommytony.war.config.TeamConfigBag;
import com.tommytony.war.config.TeamKind;
import com.tommytony.war.config.WarzoneConfig;
import com.tommytony.war.config.WarzoneConfigBag;
import com.tommytony.war.event.WarBattleWinEvent;
import com.tommytony.war.event.WarPlayerLeaveEvent;
import com.tommytony.war.event.WarPlayerThiefEvent;
import com.tommytony.war.event.WarScoreCapEvent;
import com.tommytony.war.job.InitZoneJob;
import com.tommytony.war.job.LoadoutResetJob;
import com.tommytony.war.job.LogKillsDeathsJob;
import com.tommytony.war.job.LogKillsDeathsJob.KillsDeathsRecord;
import com.tommytony.war.mapper.LoadoutYmlMapper;
import com.tommytony.war.spout.SpoutDisplayer;
import com.tommytony.war.structure.Bomb;
import com.tommytony.war.structure.Cake;
import com.tommytony.war.structure.HubLobbyMaterials;
import com.tommytony.war.structure.Monument;
import com.tommytony.war.structure.WarzoneMaterials;
import com.tommytony.war.structure.ZoneLobby;
import com.tommytony.war.structure.ZoneWallGuard;
import com.tommytony.war.utility.Direction;
import com.tommytony.war.utility.Loadout;
import com.tommytony.war.utility.LoadoutSelection;
import com.tommytony.war.utility.PlayerState;
import com.tommytony.war.utility.PotionEffectHelper;
import com.tommytony.war.volume.Volume;
import com.tommytony.war.volume.ZoneVolume;

/**
 *
 * @author tommytony
 * @package com.tommytony.war
 */
public class Warzone {
	public enum LeaveCause {
		COMMAND, DISCONNECT, SCORECAP, RESET;
		public boolean useRallyPoint() {
			return this == SCORECAP ? true : false;
		}
	}

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
	private HashMap<String, Integer> killCount = new HashMap<String, Integer>();
	private final List<Player> respawn = new ArrayList<Player>();
	private final List<String> reallyDeadFighters = new ArrayList<String>();

	private List<LogKillsDeathsJob.KillsDeathsRecord> killsDeathsTracker = new ArrayList<KillsDeathsRecord>();
	
	private final WarzoneConfigBag warzoneConfig;
	private final TeamConfigBag teamDefaultConfig;
	private InventoryBag defaultInventories = new InventoryBag();

	private Scoreboard scoreboard;
	
	private HubLobbyMaterials lobbyMaterials = null;
	private WarzoneMaterials warzoneMaterials = new WarzoneMaterials(
			new ItemStack(Material.OBSIDIAN), new ItemStack(Material.FENCE),
			new ItemStack(Material.GLOWSTONE));
	
	private boolean isEndOfGame = false;
	private boolean isReinitializing = false;
	//private final Object gameEndLock = new Object();

	public Warzone(World world, String name) {
		this.world = world;
		this.name = name;
		this.warzoneConfig = new WarzoneConfigBag(this);
		this.teamDefaultConfig = new TeamConfigBag();	// don't use ctor with Warzone, as this changes config resolution
		this.volume = new ZoneVolume(name, this.getWorld(), this);
		this.lobbyMaterials = War.war.getWarhubMaterials().clone();
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
		StringBuilder teamsMessage = new StringBuilder(War.war.getString("zone.teaminfo.prefix"));
		if (this.getTeams().isEmpty()) {
			teamsMessage.append(War.war.getString("zone.teaminfo.none"));
		} else {
			for (Team team : this.getTeams()) {
				teamsMessage.append('\n');
				teamsMessage.append(MessageFormat.format(War.war.getString("zone.teaminfo.format"),
						team.getName(), team.getPoints(), team.getRemainingLifes(),
						team.getTeamConfig().resolveInt(TeamConfig.LIFEPOOL), StringUtils.join(team.getPlayerNames())));
			}
		}
		return teamsMessage.toString();
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
					for (Volume teamVolume : team.getSpawnVolumes().values()) {
						teamVolume.resetBlocks();
					}
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

			this.volume.saveBlocks();
			if (clearArtifacts) {
				this.initializeZone(); // bring back stuff
			}
			return this.volume.size();
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
			if (this.scoreboard != null) {
				for (OfflinePlayer opl : this.scoreboard.getPlayers()) {
					this.scoreboard.resetScores(opl);
				}
				this.scoreboard.clearSlot(DisplaySlot.SIDEBAR);
				for (Objective obj : this.scoreboard.getObjectives()) {
					obj.unregister();
				}
				for (Player player : Bukkit.getOnlinePlayers()) {
					if (player.getScoreboard() == this.scoreboard) {
						player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
					}
				}
				this.scoreboard = null;
			}
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
				team.initializeTeamSpawns();
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
		if (this.getScoreboardType() != ScoreboardType.NONE) {
			this.scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
			scoreboard.registerNewObjective(this.getScoreboardType().getDisplayName(), "dummy");
			Objective obj = scoreboard.getObjective(this.getScoreboardType().getDisplayName());
			Validate.isTrue(obj.isModifiable(), "Cannot modify players' scores on the " + this.name + " scoreboard.");
			for (Team team : this.getTeams()) {
				String teamName = team.getKind().getColor() + team.getName() + ChatColor.RESET;
				if (this.getScoreboardType() == ScoreboardType.POINTS) {
					obj.getScore(Bukkit.getOfflinePlayer(teamName)).setScore(team.getPoints());
				} else if (this.getScoreboardType() == ScoreboardType.LIFEPOOL) {
					obj.getScore(Bukkit.getOfflinePlayer(teamName)).setScore(team.getRemainingLifes());
				}
			}
			obj.setDisplaySlot(DisplaySlot.SIDEBAR);
			for (Team team : this.getTeams()) {
				for (Player player : team.getPlayers()) {
					player.setScoreboard(scoreboard);
				}
			}
		}
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
		player.teleport(team.getRandomSpawn());
	}

	public void respawnPlayer(PlayerMoveEvent event, Team team, Player player) {
		this.handleRespawn(team, player);
		// Teleport the player back to spawn
		event.setTo(team.getRandomSpawn());
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
		player.setLevel(0);
		player.setExp(0);
		player.setAllowFlight(false);
		player.setFlying(false);

		player.getInventory().clear();
		
		this.setKillCount(player.getName(), 0);

		if (player.getGameMode() != GameMode.SURVIVAL) {
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

		War.war.getKillstreakReward().getAirstrikePlayers().remove(player.getName());

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

	public void resetInventory(Team team, Player player, Map<Integer, ItemStack> loadout) {
		// Reset inventory to loadout
		PlayerInventory playerInv = player.getInventory();
		playerInv.clear();
		playerInv.clear(playerInv.getSize() + 0);
		playerInv.clear(playerInv.getSize() + 1);
		playerInv.clear(playerInv.getSize() + 2);
		playerInv.clear(playerInv.getSize() + 3); // helmet/blockHead
		for (Integer slot : loadout.keySet()) {
			if (slot == 100) {
				playerInv.setBoots(loadout.get(slot).clone());
			} else if (slot == 101) {
				playerInv.setLeggings(loadout.get(slot).clone());
			} else if (slot == 102) {
				playerInv.setChestplate(loadout.get(slot).clone());
			} else if (slot == 103) {
				playerInv.setHelmet(loadout.get(slot).clone());
			} else {
				ItemStack item = loadout.get(slot);
				if (item != null) {
					playerInv.addItem(item.clone());
				}
			}
		}
		if (this.getWarzoneConfig().getBoolean(WarzoneConfig.BLOCKHEADS)) {
			ItemStack helmet = new ItemStack(Material.LEATHER_HELMET);
			LeatherArmorMeta meta = (LeatherArmorMeta) helmet.getItemMeta();
			meta.setColor(team.getKind().getBukkitColor());
			helmet.setItemMeta(meta);
			playerInv.setHelmet(helmet);
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
		
		this.playerStates.put(
				player.getName(),
				new PlayerState(player.getGameMode(), contents, inventory
						.getHelmet(), inventory.getChestplate(), inventory
						.getLeggings(), inventory.getBoots(), player
						.getHealth(), player.getExhaustion(), player
						.getSaturation(), player.getFoodLevel(), player
						.getActivePotionEffects(), playerTitle, player
						.getLevel(), player.getExp(), player.getAllowFlight()));
	}

	public void restorePlayerState(Player player) {
		PlayerState originalState = this.playerStates.remove(player.getName());
		PlayerInventory playerInv = player.getInventory();
		if (originalState != null) {
			player.getOpenInventory().close();
			this.playerInvFromInventoryStash(playerInv, originalState);
			player.setGameMode(originalState.getGamemode());
			player.setHealth(Math.max(Math.min(originalState.getHealth(), 20.0D), 0.0D));
			player.setExhaustion(originalState.getExhaustion());
			player.setSaturation(originalState.getSaturation());
			player.setFoodLevel(originalState.getFoodLevel());
			PotionEffectHelper.restorePotionEffects(player, originalState.getPotionEffects());
			player.setLevel(originalState.getLevel());
			player.setExp(originalState.getExp());
			player.setAllowFlight(originalState.canFly());
			
			if (War.war.isSpoutServer()) {
				SpoutManager.getPlayer(player).setTitle(originalState.getPlayerTitle());
			}
		}
		player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
	}

	private void playerInvFromInventoryStash(PlayerInventory playerInv, PlayerState originalContents) {
		playerInv.clear();
		
		playerInv.clear(playerInv.getSize() + 0);
		playerInv.clear(playerInv.getSize() + 1);
		playerInv.clear(playerInv.getSize() + 2);
		playerInv.clear(playerInv.getSize() + 3); // helmet/blockHead
		
		int invIndex = 0;
		for (ItemStack item : originalContents.getContents()) {
			if (item != null && item.getType() != Material.AIR) {
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
				for (Volume tVolume : t.getSpawnVolumes().values()) {
					if (tVolume.contains(block)) {
						return true;
					}
				}
				if (t.getFlagVolume() != null && t.getFlagVolume().contains(block)) {
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

	static final Comparator<Team> LEAST_PLAYER_COUNT_ORDER = new Comparator<Team>() {
		@Override
		public int compare(Team arg0, Team arg1) {
			return arg0.getPlayers().size() - arg1.getPlayers().size();
		}
	};

	public Team autoAssign(Player player) {
		Collections.sort(teams, LEAST_PLAYER_COUNT_ORDER);
		Team lowestNoOfPlayers = null;
		for (Team team : this.teams) {
			if (War.war.canPlayWar(player, team)) {
				lowestNoOfPlayers = team;
				break;
			}
		}
		if (lowestNoOfPlayers != null) {
			this.assign(player, lowestNoOfPlayers);
		}
		return lowestNoOfPlayers;
	}

	/**
	 * Assign a player to a specific team.
	 * 
	 * @param player
	 *            Player to assign to team.
	 * @param team
	 *            Team to add the player to.
	 * @return false if player does not have permission to join this team.
	 */
	public boolean assign(Player player, Team team) {
		if (!War.war.canPlayWar(player, team)) {
			War.war.badMsg(player, "join.permission.single");
			return false;
		}
		if (player.getWorld() != this.getWorld()) {
			player.teleport(this.getWorld().getSpawnLocation());
		}
		team.addPlayer(player);
		team.resetSign();
		if (!this.hasPlayerState(player.getName())) {
			this.keepPlayerState(player);
			War.war.msg(player, "join.inventorystored");
		}
		this.respawnPlayer(team, player);
		this.broadcast("join.broadcast", player.getName(), team.getKind().getFormattedName());
		return true;
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
											playerTeam.getKind().getBlockHead(),
											10000);
								}
							}
						}
						
						t.teamcast("zone.battle.end", playerTeam.getName(), player.getName());
	
						if (t.getPlayers().size() != 0 && !t.getTeamConfig().resolveBoolean(TeamConfig.FLAGPOINTSONLY)) {
							if (!t.getName().equals(playerTeam.getName())) {
								// all other teams get a point
								t.addPoint();
								t.resetSign();
							}
							scores += '\n' + t.getName() + "(" + t.getPoints() + "/" + t.getTeamConfig().resolveInt(TeamConfig.MAXSCORE) + ") ";
							
						}
					}
					
					// whoever didn't lose, reward them
					//
					List<Team> winningTeams = new ArrayList<Team>(teams.size());
					for( Team t : teams ) {
						if( !t.getPlayers().contains(player)) {
							winningTeams.add(t);
						}
					}
					WarBattleWinEvent event1 = new WarBattleWinEvent(this, winningTeams);
					War.war.getServer().getPluginManager().callEvent(event1);

					if (!scores.equals("")) {
						this.broadcast("zone.battle.newscores", scores);
					}
					if (War.war.getMysqlConfig().isEnabled() && War.war.getMysqlConfig().isLoggingEnabled()) {
						LogKillsDeathsJob logKillsDeathsJob = new LogKillsDeathsJob(ImmutableList.copyOf(this.getKillsDeathsTracker()));
						War.war.getServer().getScheduler().runTaskAsynchronously(War.war, logKillsDeathsJob);
					}
					this.getKillsDeathsTracker().clear();
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
						this.broadcast("zone.battle.reset");
						if (this.getWarzoneConfig().getBoolean(WarzoneConfig.RESETBLOCKS)) {
							this.reinitialize();
						} else {
							this.initializeZone();
						}
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
										playerTeam.getKind().getBlockHead(),
										5000);
							}
						}
					}
					
					this.broadcast("drop.flag.broadcast", player.getName(), victim.getName());
				}
				
				// Bomb thieves
				if (this.isBombThief(player.getName())) {
					// died while carrying bomb.. dropped it
					Bomb bomb = this.getBombForThief(player.getName());
					bomb.getVolume().resetBlocks();
					bomb.addBombBlocks();
					this.removeBombThief(player.getName());
					
					for (Team t : this.getTeams()) {
						t.teamcast("drop.bomb.broadcast", player.getName(), ChatColor.GREEN + bomb.getName() + ChatColor.WHITE);
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
						t.teamcast("drop.cake.broadcast", player.getName(), ChatColor.GREEN + cake.getName() + ChatColor.WHITE);
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
					this.broadcast("zone.lifepool.empty", playerTeam.getName());
				}
			}
			playerTeam.resetSign();
		}
	}

	public void reinitialize() {
		this.isReinitializing = true;
		this.getVolume().resetBlocksAsJob();
	}

	public void handlePlayerLeave(Player player, Location destination, PlayerMoveEvent event, boolean removeFromTeam) {
		this.handlePlayerLeave(player);
		event.setTo(destination);
	}

	public void handlePlayerLeave(Player player, Location destination, boolean removeFromTeam) {
		this.handlePlayerLeave(player);
		player.teleport(destination);
	}

	private void handlePlayerLeave(Player player) {
		Team playerTeam = Team.getTeamByPlayerName(player.getName());
		if (playerTeam != null) {
			playerTeam.removePlayer(player);
			this.broadcast("leave.broadcast", playerTeam.getKind().getColor() + player.getName() + ChatColor.WHITE);
			playerTeam.resetSign();
			if (this.getPlayerCount() == 0 && this.getWarzoneConfig().getBoolean(WarzoneConfig.RESETONEMPTY)) {
				// reset the zone for a new game when the last player leaves
				for (Team team : this.getTeams()) {
					team.resetPoints();
					team.setRemainingLives(team.getTeamConfig().resolveInt(TeamConfig.LIFEPOOL));
				}
				if (!this.isReinitializing()) {
					this.reinitialize();
					War.war.getLogger().log(Level.INFO, "Last player left warzone {0}. Warzone blocks resetting automatically...", new Object[] {this.getName()});
				}
			}
			
			WarPlayerLeaveEvent event1 = new WarPlayerLeaveEvent(player.getName());
			War.war.getServer().getPluginManager().callEvent(event1);
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
		WarPlayerThiefEvent event1 = new WarPlayerThiefEvent(Bukkit.getPlayerExact(flagThief), WarPlayerThiefEvent.StolenObject.FLAG);
		War.war.getServer().getPluginManager().callEvent(event1);
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
		WarPlayerThiefEvent event1 = new WarPlayerThiefEvent(Bukkit.getPlayerExact(bombThief), WarPlayerThiefEvent.StolenObject.BOMB);
		War.war.getServer().getPluginManager().callEvent(event1);
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
		WarPlayerThiefEvent event1 = new WarPlayerThiefEvent(Bukkit.getPlayerExact(cakeThief), WarPlayerThiefEvent.StolenObject.CAKE);
		War.war.getServer().getPluginManager().callEvent(event1);
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
		List<Team> winningTeams = new ArrayList<Team>(teams.size());
		for (String team : winnersStr.split(" ")) {
			winningTeams.add(this.getTeamByKind(TeamKind.getTeam(team)));
		}
		WarScoreCapEvent event1 = new WarScoreCapEvent(winningTeams);
		War.war.getServer().getPluginManager().callEvent(event1);
		
		for (Team t : this.getTeams()) {
			if (War.war.isSpoutServer()) {
				for (Player p : t.getPlayers()) {
					SpoutPlayer sp = SpoutManager.getPlayer(p);
					if (sp.isSpoutCraftEnabled()) {
		                sp.sendNotification(
		                		SpoutDisplayer.cleanForNotification("Match won! " + ChatColor.WHITE + "Winners:"),
		                		SpoutDisplayer.cleanForNotification(SpoutDisplayer.addMissingColor(winnersStr, this)),
		                		Material.CAKE,
		                		(short)0,
		                		10000);
					}
				}
			}
			String winnersStrAndExtra = "Score cap reached. Game is over! Winning team(s): " + winnersStr;
			winnersStrAndExtra += ". Resetting warzone and your inventory...";
			t.teamcast(winnersStrAndExtra);
			for (Iterator<Player> it = t.getPlayers().iterator(); it.hasNext();) {
				Player tp = it.next();
				it.remove(); // Remove player from team first to prevent anti-tp
				t.removePlayer(tp);
				tp.teleport(this.getEndTeleport(LeaveCause.SCORECAP));
				if (winnersStr.contains(t.getName())) {
					// give reward
					rewardPlayer(tp, t.getInventories().resolveReward());
				}
			}
			t.resetPoints();
			t.getPlayers().clear(); // empty the team
			t.resetSign();
		}
		if (this.getWarzoneConfig().getBoolean(WarzoneConfig.RESETBLOCKS)) {
			this.reinitialize();
		} else {
			this.initializeZone();
		}
	}

	public void rewardPlayer(Player player, Map<Integer, ItemStack> reward) {
		for (Integer slot : reward.keySet()) {
			ItemStack item = reward.get(slot);
			if (item != null) {
				player.getInventory().addItem(item);
			}
		}
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
			for (Iterator<Player> it = team.getPlayers().iterator(); it.hasNext(); ) {
				final Player player = it.next();
				it.remove();
				team.removePlayer(player);
				player.teleport(this.getTeleport());
			}
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
				this.resetInventory(playerTeam, player, Collections.<Integer, ItemStack>emptyMap());
				War.war.msg(player, "404 No loadouts found");
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
					if (isFirstRespawn && playerTeam.getInventories().resolveLoadouts().keySet().size() > 1 || isToggle) {
						War.war.msg(player, "zone.loadout.equip", name);
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
				if (item != null && item.getType() != Material.AIR) {
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
				for (Volume teamSpawnVolume : maybeOpponent.getSpawnVolumes().values()) {
					Volume periphery = new Volume(new Location(
							teamSpawnVolume.getWorld(),
							teamSpawnVolume.getMinX() - 1,
							teamSpawnVolume.getMinY() - 1,
							teamSpawnVolume.getMinZ() - 1), new Location(
							teamSpawnVolume.getWorld(),
							teamSpawnVolume.getMaxX() + 1,
							teamSpawnVolume.getMaxY() + 1,
							teamSpawnVolume.getMaxZ() + 1));
					if (periphery.contains(block)) {
						return true;
					}
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

	public Scoreboard getScoreboard() {
		return scoreboard;
	}

	public ScoreboardType getScoreboardType() {
		return this.getWarzoneConfig().getScoreboardType(WarzoneConfig.SCOREBOARD);
	}
	public boolean hasKillCount(String player) {
		return killCount.containsKey(player);
	}

	public int getKillCount(String player) {
		return killCount.get(player);
	}

	public void setKillCount(String player, int totalKills) {
		if (totalKills < 0) {
			throw new IllegalArgumentException("Amount of kills to set cannot be a negative number.");
		}
		killCount.put(player, totalKills);
	}

	public void addKillCount(String player, int amount) {
		if (amount < 0) {
			throw new IllegalArgumentException("Amount of kills to add cannot be a negative number.");
		}
		killCount.put(player, killCount.get(player) + amount);
	}

	public void addKillDeathRecord(OfflinePlayer player, int kills, int deaths) {
		for (Iterator<KillsDeathsRecord> it = this.killsDeathsTracker.iterator(); it.hasNext();) {
			LogKillsDeathsJob.KillsDeathsRecord kdr = it.next();
			if (kdr.getPlayer().equals(player)) {
				kills += kdr.getKills();
				deaths += kdr.getDeaths();
				it.remove();
			}
		}
		LogKillsDeathsJob.KillsDeathsRecord kdr = new LogKillsDeathsJob.KillsDeathsRecord(player, kills, deaths);
		this.killsDeathsTracker.add(kdr);
	}

	public List<LogKillsDeathsJob.KillsDeathsRecord> getKillsDeathsTracker() {
		return killsDeathsTracker;
	}

	/**
	 * Send a message to all teams.
	 * @param message Message or key to translate.
	 */
	public void broadcast(String message) {
		for (Team team : this.teams) {
			team.teamcast(message);
		}
	}

	/**
	 * Send a message to all teams.
	 * @param message Message or key to translate.
	 * @param args Arguments for the formatter.
	 */
	public void broadcast(String message, Object... args) {
		for (Team team : this.teams) {
			team.teamcast(message, args);
		}
	}

	/**
	 * Get a list of all players in the warzone. The list is immutable. If you
	 * need to modify the player list, you must use the per-team lists
	 * 
	 * @return list containing all team players.
	 */
	public List<Player> getPlayers() {
		List<Player> players = new ArrayList<Player>();
		for (Team team : this.teams) {
			players.addAll(team.getPlayers());
		}
		return players;
	}

	/**
	 * Get the amount of players in all teams in this warzone.
	 * 
	 * @return total player count
	 */
	public int getPlayerCount() {
		int count = 0;
		for (Team team : this.teams) {
			count += team.getPlayers().size();
		}
		return count;
	}

	/**
	 * Get the amount of players in all teams in this warzone. Same as
	 * {@link #getPlayerCount()}, except only checks teams that the specified
	 * player has permission to join.
	 * 
	 * @param target
	 *            Player to check for permissions.
	 * @return total player count in teams the player has access to.
	 */
	public int getPlayerCount(Permissible target) {
		int playerCount = 0;
		for (Team team : this.teams) {
			if (target.hasPermission(team.getTeamConfig().resolveString(
					TeamConfig.PERMISSION))) {
				playerCount += team.getPlayers().size();
			}
		}
		return playerCount;
	}

	/**
	 * Get the total capacity of all teams in this zone. This should be
	 * preferred over {@link TeamConfig#TEAMSIZE} as that can differ per team.
	 * 
	 * @return capacity of all teams in this zone
	 */
	public int getTotalCapacity() {
		int capacity = 0;
		for (Team team : this.teams) {
			capacity += team.getTeamConfig().resolveInt(TeamConfig.TEAMSIZE);
		}
		return capacity;
	}

	/**
	 * Get the total capacity of all teams in this zone. Same as
	 * {@link #getTotalCapacity()}, except only checks teams that the specified
	 * player has permission to join.
	 * 
	 * @param target
	 *            Player to check for permissions.
	 * @return capacity of teams the player has access to.
	 */
	public int getTotalCapacity(Permissible target) {
		int capacity = 0;
		for (Team team : this.teams) {
			if (target.hasPermission(team.getTeamConfig().resolveString(
					TeamConfig.PERMISSION))) {
				capacity += team.getTeamConfig()
						.resolveInt(TeamConfig.TEAMSIZE);
			}
		}
		return capacity;
	}

	/**
	 * Check if all teams are full.
	 * 
	 * @return true if all teams are full, false otherwise.
	 */
	public boolean isFull() {
		return this.getPlayerCount() == this.getTotalCapacity();
	}

	/**
	 * Check if all teams are full. Same as {@link #isFull()}, except only
	 * checks teams that the specified player has permission to join.
	 * 
	 * @param target
	 *            Player to check for permissions.
	 * @return true if all teams are full, false otherwise.
	 */
	public boolean isFull(Permissible target) {
		return this.getPlayerCount(target) == this.getTotalCapacity(target);
	}

	public void dropAllStolenObjects(Player player, boolean quiet) {
		if (this.isFlagThief(player.getName())) {
			Team victimTeam = this.getVictimTeamForFlagThief(player.getName());

			this.removeFlagThief(player.getName());

			// Bring back flag of victim team
			victimTeam.getFlagVolume().resetBlocks();
			victimTeam.initializeTeamFlag();

			if (!quiet) {
				this.broadcast("drop.flag.broadcast", player.getName(), ChatColor.GREEN + victimTeam.getName() + ChatColor.WHITE);
			}
		} else if (this.isCakeThief(player.getName())) {
			Cake cake = this.getCakeForThief(player.getName());

			this.removeCakeThief(player.getName());

			// Bring back cake
			cake.getVolume().resetBlocks();
			cake.addCakeBlocks();

			if (!quiet) {
				this.broadcast("drop.cake.broadcast", player.getName(), ChatColor.GREEN + cake.getName() + ChatColor.WHITE);
			}
		} else if (this.isBombThief(player.getName())) {
			Bomb bomb = this.getBombForThief(player.getName());

			this.removeBombThief(player.getName());

			// Bring back bomb
			bomb.getVolume().resetBlocks();
			bomb.addBombBlocks();

			if (!quiet) {
				this.broadcast("drop.bomb.broadcast", player.getName(), ChatColor.GREEN + bomb.getName() + ChatColor.WHITE);
			}
		}
	}

	/**
	 * Get the proper ending teleport location for players leaving the warzone.
	 * <p>
	 * Specifically, it gets teleports in this order:
	 * <ul>
	 * <li>Rally point (if scorecap)
	 * <li>Warhub (if autojoin)
	 * <li>Lobby
	 * </ul>
	 * </p>
	 * @param reason Reason for leaving zone
	 * @return
	 */
	public Location getEndTeleport(LeaveCause reason) {
		if (reason.useRallyPoint() && this.getRallyPoint() != null) {
			return this.getRallyPoint();
		}
		if (this.getWarzoneConfig().getBoolean(WarzoneConfig.AUTOJOIN)
				&& War.war.getWarHub() != null) {
			return War.war.getWarHub().getLocation();
		}
		return this.getTeleport();
	}

	public Volume loadStructure(String volName, World world) throws SQLException {
		return loadStructure(volName, world, ZoneVolumeMapper.getZoneConnection(volume, name, world));
	}

	public Volume loadStructure(String volName, Connection zoneConnection) throws SQLException {
		return loadStructure(volName, world, zoneConnection);
	}

	public Volume loadStructure(String volName, World world, Connection zoneConnection) throws SQLException {
		Volume volume = new Volume(volName, world);
		if (!containsTable(String.format("structure_%d_corners", volName.hashCode() & Integer.MAX_VALUE), zoneConnection)) {
			volume = VolumeMapper.loadVolume(volName, name, world);
			ZoneVolumeMapper.saveStructure(volume, zoneConnection);
			War.war.getLogger().log(Level.INFO, "Stuffed structure {0} into database for warzone {1}", new Object[] {volName, name});
			return volume;
		}
		ZoneVolumeMapper.loadStructure(volume, zoneConnection);
		return volume;
	}

	private boolean containsTable(String table, Connection connection) throws SQLException {
		PreparedStatement stmt = connection.prepareStatement("SELECT COUNT(*) AS ct FROM sqlite_master WHERE type = ? AND name = ?");
		stmt.setString(1, "table");
		stmt.setString(2, table);
		ResultSet resultSet = stmt.executeQuery();
		try {
			return resultSet.next() && resultSet.getInt("ct") > 0;
		} finally {
			resultSet.close();
			stmt.close();
		}
	}

	/**
	 * Check if a player has stolen from a warzone flag, bomb, or cake.
	 * @param suspect Player to check.
	 * @return true if suspect has stolen a structure.
	 */
	public boolean isThief(String suspect) {
		return this.isFlagThief(suspect) || this.isBombThief(suspect) || this.isCakeThief(suspect);
	}
}
