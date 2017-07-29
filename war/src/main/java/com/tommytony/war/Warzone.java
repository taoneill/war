package com.tommytony.war;

import com.google.common.collect.ImmutableList;
import com.tommytony.war.config.*;
import com.tommytony.war.event.WarBattleWinEvent;
import com.tommytony.war.event.WarPlayerLeaveEvent;
import com.tommytony.war.event.WarPlayerThiefEvent;
import com.tommytony.war.event.WarScoreCapEvent;
import com.tommytony.war.job.InitZoneJob;
import com.tommytony.war.job.LoadoutResetJob;
import com.tommytony.war.job.LogKillsDeathsJob;
import com.tommytony.war.job.LogKillsDeathsJob.KillsDeathsRecord;
import com.tommytony.war.job.ZoneTimeJob;
import com.tommytony.war.mapper.LoadoutYmlMapper;
import com.tommytony.war.mapper.VolumeMapper;
import com.tommytony.war.mapper.ZoneVolumeMapper;
import com.tommytony.war.spout.SpoutDisplayer;
import com.tommytony.war.structure.*;
import com.tommytony.war.utility.*;
import com.tommytony.war.volume.Volume;
import com.tommytony.war.volume.ZoneVolume;
import net.milkbowl.vault.economy.EconomyResponse;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.getspout.spoutapi.SpoutManager;
import org.getspout.spoutapi.player.SpoutPlayer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;

/**
 *
 * @author tommytony
 * @package com.tommytony.war
 */
public class Warzone {


	static final Comparator<Team> LEAST_PLAYER_COUNT_ORDER = new Comparator<Team>() {
		@Override
		public int compare(Team arg0, Team arg1) {
			return arg0.getPlayers().size() - arg1.getPlayers().size();
		}
	};
	private final List<Team> teams = new ArrayList<Team>();
	private final List<Monument> monuments = new ArrayList<Monument>();
	private final List<CapturePoint> capturePoints = new ArrayList<CapturePoint>();
	private final List<Bomb> bombs = new ArrayList<Bomb>();
	private final List<Cake> cakes = new ArrayList<Cake>();
	private final List<String> authors = new ArrayList<String>();
	private final int minSafeDistanceFromWall = 6;
	private final List<Player> respawn = new ArrayList<Player>();
	private final List<String> reallyDeadFighters = new ArrayList<String>();
	private final WarzoneConfigBag warzoneConfig;
	private final TeamConfigBag teamDefaultConfig;
	private String name;
	private ZoneVolume volume;
	private World world;
	private Location teleport;
	private ZoneLobby lobby;
	private Location rallyPoint;
	private List<ZoneWallGuard> zoneWallGuards = new ArrayList<ZoneWallGuard>();
	private HashMap<String, PlayerState> playerStates = new HashMap<String, PlayerState>();
	private HashMap<UUID, Team> flagThieves = new HashMap<UUID, Team>();
	private HashMap<UUID, Bomb> bombThieves = new HashMap<UUID, Bomb>();
	private HashMap<UUID, Cake> cakeThieves = new HashMap<UUID, Cake>();
	private HashMap<String, LoadoutSelection> loadoutSelections = new HashMap<String, LoadoutSelection>();
	private HashMap<String, PlayerState> deadMenInventories = new HashMap<String, PlayerState>();
	private HashMap<String, Integer> killCount = new HashMap<String, Integer>();
	private HashMap<Player, PermissionAttachment> attachments = new HashMap<Player, PermissionAttachment>();
	private HashMap<Player, Team> delayedJoinPlayers = new HashMap<Player, Team>();
	private List<LogKillsDeathsJob.KillsDeathsRecord> killsDeathsTracker = new ArrayList<KillsDeathsRecord>();
	private InventoryBag defaultInventories = new InventoryBag();

	private Scoreboard scoreboard;
	
	private HubLobbyMaterials lobbyMaterials = null;
	private WarzoneMaterials warzoneMaterials = new WarzoneMaterials(
			new ItemStack(Material.OBSIDIAN), new ItemStack(Material.FENCE),
			new ItemStack(Material.GLOWSTONE));
	
	private boolean isEndOfGame = false;
	private boolean isReinitializing = false;
	//private final Object gameEndLock = new Object();

    private boolean pvpReady = true;
	private Random killSeed = new Random();
	// prevent tryCallDelayedPlayers from being recursively called by Warzone#assign
	private boolean activeDelayedCall = false;
	private ScoreboardType scoreboardType;

	public Warzone(World world, String name) {
		this.world = world;
		this.name = name;
		this.warzoneConfig = new WarzoneConfigBag(this);
		this.teamDefaultConfig = new TeamConfigBag();	// don't use ctor with Warzone, as this changes config resolution
		this.volume = new ZoneVolume(name, this.getWorld(), this);
		this.lobbyMaterials = War.war.getWarhubMaterials().clone();
		this.pvpReady = true;
		this.scoreboardType = this.getWarzoneConfig().getScoreboardType(WarzoneConfig.SCOREBOARD);
		if (scoreboardType == ScoreboardType.SWITCHING)
			scoreboardType = ScoreboardType.LIFEPOOL;
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

	public static Warzone getZoneByNameExact(String name) {
		for (Warzone zone : War.war.getWarzones()) {
			if (zone.getName().equalsIgnoreCase(name)) return zone;
		}
		return null;
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

	public static Warzone getZoneForDeadPlayer(Player player) {
		for (Warzone warzone : War.war.getWarzones()) {
			if (warzone.getReallyDeadFighters().contains(player.getName())) {
				return warzone;
			}
		}
		return null;
	}

	public boolean ready() {
		return this.volume.hasTwoCorners() && !this.volume.tooSmall() && !this.volume.tooBig();
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
						team.getName(), team.getPoints(), team.getRemainingLives(),
						team.getTeamConfig().resolveInt(TeamConfig.LIFEPOOL), StringUtils.join(team.getPlayerNames().iterator(), ", ")));
			}
		}
		return teamsMessage.toString();
	}

	public String getName() {
		return this.name;
	}

	public void setName(String newName) {
		this.name = newName;
		this.volume.setName(newName);
	}

	@Override
	public String toString() {
		return this.getName();
	}

	public Location getTeleport() {
		return this.teleport;
	}

	public void setTeleport(Location location) {
		this.teleport = location;
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

				for (CapturePoint cp : this.capturePoints) {
					cp.getVolume().resetBlocks();
				}

				for (Bomb bomb : this.bombs) {
					bomb.getVolume().resetBlocks();
				}

				for (Cake cake : this.cakes) {
					cake.getVolume().resetBlocks();
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
				for (String entry : this.scoreboard.getEntries()) {
					this.scoreboard.resetScores(entry);
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
					if (player.equals(respawnExempted)) {
						continue;
					}
					if (this.getReallyDeadFighters().contains(player.getName())) {
						continue;
					}
					this.respawnPlayer(team, player);
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

		// reset capture points
		for (CapturePoint cp : this.capturePoints) {
			cp.getVolume().resetBlocks();
			cp.reset();
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
		if (this.getScoreboardType() != ScoreboardType.NONE) {
			this.scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
			this.updateScoreboard();
			for (Team team : this.getTeams()) {
				for (Player player : team.getPlayers()) {
					player.setScoreboard(scoreboard);
				}
			}
		}
		this.reallyDeadFighters.clear();

		//get them config (here be crazy grinning's!)
		int pvpready = warzoneConfig.getInt(WarzoneConfig.PREPTIME);

		if(pvpready != 0) { //if it is equalz to zeroz then dinosaurs will take over the earth
			this.pvpReady = false;
			ZoneTimeJob timer = new ZoneTimeJob(this);
			War.war.getServer().getScheduler().runTaskLater(War.war, timer, pvpready * 20);
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
		// first, wipe inventory to disable attribute modifications
		this.preventItemHackingThroughOpenedInventory(player);
		player.getInventory().clear();

		// clear potion effects
		PotionEffectHelper.clearPotionEffects(player);

		// Fill hp
		player.setRemainingAir(player.getMaximumAir());
		AttributeInstance ai = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
		for (AttributeModifier mod : ai.getModifiers()) {
			ai.removeModifier(mod);
		}
		ai.setBaseValue(20.0);
		player.setHealth(ai.getValue());
		player.setFoodLevel(20);
		player.setSaturation(team.getTeamConfig().resolveInt(TeamConfig.SATURATION));
		player.setExhaustion(0);
		player.setFallDistance(0);
		player.setFireTicks(0);
		player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 5, 255));
		Runnable antiFireAction = new Runnable() {

			@Override
			public void run() {
				// Stop fire here, since doing it in the same tick as death doesn't extinguish it
				player.setFireTicks(0);
			}

		};
		// ughhhhh bukkit
		War.war.getServer().getScheduler().runTaskLater(War.war, antiFireAction, 1L);
		War.war.getServer().getScheduler().runTaskLater(War.war, antiFireAction, 2L);
		War.war.getServer().getScheduler().runTaskLater(War.war, antiFireAction, 3L);
		War.war.getServer().getScheduler().runTaskLater(War.war, antiFireAction, 4L);
		War.war.getServer().getScheduler().runTaskLater(War.war, antiFireAction, 5L);


		player.setLevel(0);
		player.setExp(0);
		player.setAllowFlight(false);
		player.setFlying(false);


		this.setKillCount(player.getName(), 0);

		if (player.getGameMode() != GameMode.SURVIVAL) {
			// Players are always in survival mode in warzones
			player.setGameMode(GameMode.SURVIVAL);
		}


		String potionEffect = team.getTeamConfig().resolveString(TeamConfig.APPLYPOTION);
		if (!potionEffect.isEmpty()) {
			PotionEffect effect = War.war.getPotionEffect(potionEffect);
			if (effect != null) {
				player.addPotionEffect(effect);
			} else {
				War.war.getLogger().log(Level.WARNING,
					"Failed to apply potion effect {0} in warzone {1}.",
					new Object[] {potionEffect, name});
			}
		}

		int respawnTime = team.getTeamConfig().resolveInt(TeamConfig.RESPAWNTIMER);
		int respawnTimeTicks = respawnTime * 20;
		if (respawnTimeTicks > 0) {
			player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, respawnTimeTicks, 255));
			player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, respawnTimeTicks, 200));
			player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, respawnTimeTicks, 255));
			player.sendTitle("", ChatColor.RED + MessageFormat.format(War.war.getString("zone.spawn.timer.title"), respawnTime), 1, respawnTimeTicks, 10);
		}

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

	private void resetInventory(Team team, Player player, Map<Integer, ItemStack> loadout) {
		// Reset inventory to loadout
		PlayerInventory playerInv = player.getInventory();
		playerInv.clear();
		playerInv.clear(playerInv.getSize());
		playerInv.clear(playerInv.getSize() + 1);
		playerInv.clear(playerInv.getSize() + 2);
		playerInv.clear(playerInv.getSize() + 3); // helmet/blockHead

		Loadout banned = Loadout.getLoadout(team.getInventories().resolveNewLoadouts(), "banned");
		Set<Material> bannedMaterials = new HashSet<Material>();
		if (banned != null) {
			for (ItemStack bannedItem : banned.getContents().values()) {
				bannedMaterials.add(bannedItem.getType());
			}
		}

		for (Integer slot : loadout.keySet()) {
			ItemStack item = loadout.get(slot);
			if (item == null || item.getType() == Material.AIR) {
				continue;
			}
			if (bannedMaterials.contains(item.getType())) {
				continue;
			}
			if (slot == 100) {
				playerInv.setBoots(item.clone());
			} else if (slot == 101) {
				playerInv.setLeggings(item.clone());
			} else if (slot == 102) {
				playerInv.setChestplate(item.clone());
			} else if (slot == 103) {
				playerInv.setHelmet(item.clone());
			} else {
				playerInv.addItem(item.clone());
			}
		}
		if (this.getWarzoneConfig().getBoolean(WarzoneConfig.BLOCKHEADS)) {
			playerInv.setHelmet(team.getKind().getHat());
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
			// prevent item hacking thru CRAFTING personal inventory slots
			this.preventItemHackingThroughOpenedInventory(player);

			this.playerInvFromInventoryStash(playerInv, originalState);
			player.setGameMode(originalState.getGamemode());
			double maxH = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
			player.setHealth(Math.max(Math.min(originalState.getHealth(), maxH), 0.0D));
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

	private void preventItemHackingThroughOpenedInventory(Player player) {
		InventoryView openedInv = player.getOpenInventory();
		if (openedInv.getType() == InventoryType.CRAFTING) {
			// prevent abuse of personal crafting slots (this behavior doesn't seem to happen
			// for containers like workbench and furnace - those get closed properly)
			openedInv.getTopInventory().clear();
		}

		// Prevent player from keeping items he was transferring in his inventory
		openedInv.setCursor(null);
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

	public boolean hasCapturePoint(String capturePointName) {
		return this.getCapturePoint(capturePointName) != null;
	}

	public CapturePoint getCapturePoint(String capturePointName) {
		for (CapturePoint cp : this.capturePoints) {
			if (cp.getName().startsWith(capturePointName)) {
				return cp;
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
		if (block == null) {
			return false;
		}
		if (this.ready()) {
			for (Monument m : this.monuments) {
				if (m.getVolume().contains(block)) {
					return true;
				}
			}
			for (CapturePoint cp : this.capturePoints) {
				if (cp.getVolume().contains(block)) {
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

	public ZoneLobby getLobby() {
		return this.lobby;
	}

	public void setLobby(ZoneLobby lobby) {
		this.lobby = lobby;
	}
	
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
		PermissionAttachment attachment = player.addAttachment(War.war);
		this.attachments.put(player, attachment);
		attachment.setPermission("war.playing", true);
		attachment.setPermission("war.playing." + this.getName().toLowerCase(), true);
		team.addPlayer(player);
		team.resetSign();
		if (this.hasPlayerState(player.getName())) {
			War.war.getLogger().log(Level.WARNING, "Player {0} in warzone {1} already has a stored state - they may have lost items",
					new Object[] {player.getName(), this.getName()});
			this.playerStates.remove(player.getName());
		}
		this.getReallyDeadFighters().remove(player.getName());
		this.keepPlayerState(player);
		War.war.msg(player, "join.inventorystored");
		this.respawnPlayer(team, player);
		this.broadcast("join.broadcast", player.getName(), team.getKind().getFormattedName());
		this.tryCallDelayedPlayers();
		return true;
	}
	
	private void dropItems(Location location, ItemStack[] items) {
		for (ItemStack item : items) {
			if (item == null || item.getType() == Material.AIR) {
				continue;
			}
			location.getWorld().dropItem(location, item);
		}
	}
	
	/**
	 * Send death messages and process other records before passing off the
	 * death to the {@link #handleDeath(Player)} method.
	 * @param attacker Player who killed the defender
	 * @param defender Player who was killed
	 * @param damager Entity who caused the damage. Usually an arrow. Used for
	 * specific death messages. Can be null.
	 */
	public void handleKill(Player attacker, Player defender, Entity damager) {
		Team attackerTeam = this.getPlayerTeam(attacker.getName());
		Team defenderTeam = this.getPlayerTeam(defender.getName());
		if (this.getWarzoneConfig().getBoolean(WarzoneConfig.DEATHMESSAGES)) {
			String attackerString = attackerTeam.getKind().getColor() + attacker.getName();
			String defenderString = defenderTeam.getKind().getColor() + defender.getName();
			ItemStack weapon = attacker.getInventory().getItemInMainHand(); // Not the right way to do this, as they could kill with their other hand, but whatever
			Material killerWeapon = weapon.getType();
			String weaponString = killerWeapon.toString();
			if (weapon.hasItemMeta() && weapon.getItemMeta().hasDisplayName()) {
				weaponString = weapon.getItemMeta().getDisplayName() + ChatColor.WHITE;
			}
			if (killerWeapon == Material.AIR) {
				weaponString = War.war.getString("pvp.kill.weapon.hand");
			} else if (killerWeapon == Material.BOW || damager instanceof Arrow) {
				int rand = killSeed.nextInt(3);
				if (rand == 0) {
					weaponString = War.war.getString("pvp.kill.weapon.bow");
				} else {
					weaponString = War.war.getString("pvp.kill.weapon.aim");
				}
			} else if (damager instanceof Projectile) {
				weaponString = War.war.getString("pvp.kill.weapon.aim");
			}
			String adjectiveString = War.war.getDeadlyAdjectives().isEmpty() ? "" : War.war.getDeadlyAdjectives().get(this.killSeed.nextInt(War.war.getDeadlyAdjectives().size()));
			String verbString = War.war.getKillerVerbs().isEmpty() ? "" : War.war.getKillerVerbs().get(this.killSeed.nextInt(War.war.getKillerVerbs().size()));
			this.broadcast("pvp.kill.format", attackerString + ChatColor.WHITE, adjectiveString,
					weaponString.toLowerCase().replace('_', ' '), verbString, defenderString);
		}
		this.addKillCount(attacker.getName(), 1);
		this.addKillDeathRecord(attacker, 1, 0);
		this.addKillDeathRecord(defender, 0, 1);
		if (attackerTeam.getTeamConfig().resolveBoolean(TeamConfig.XPKILLMETER)) {
			attacker.setLevel(this.getKillCount(attacker.getName()));
		}
		if (attackerTeam.getTeamConfig().resolveBoolean(TeamConfig.KILLSTREAK)) {
			War.war.getKillstreakReward().rewardPlayer(attacker, this.getKillCount(attacker.getName()));
		}
		this.updateScoreboard();
		if (defenderTeam.getTeamConfig().resolveBoolean(TeamConfig.INVENTORYDROP)) {
			dropItems(defender.getLocation(), defender.getInventory().getContents());
			dropItems(defender.getLocation(), defender.getInventory().getArmorContents());
		}
		this.handleDeath(defender);
	}

	public void updateScoreboard() {
		if (this.getScoreboardType() == ScoreboardType.NONE)
			return;
		if (this.getScoreboard() == null)
			return;
		if (this.scoreboard.getObjective(this.getScoreboardType().getDisplayName()) == null) {
			for (String entry : this.scoreboard.getEntries()) {
				this.scoreboard.resetScores(entry);
			}
			this.scoreboard.clearSlot(DisplaySlot.SIDEBAR);
			for (Objective obj : this.scoreboard.getObjectives()) {
				obj.unregister();
			}
			scoreboard.registerNewObjective(this.getScoreboardType().getDisplayName(), "dummy");
			Objective obj = scoreboard.getObjective(this.getScoreboardType().getDisplayName());
			Validate.isTrue(obj.isModifiable(), "Cannot modify players' scores on the " + this.name + " scoreboard.");
			obj.setDisplaySlot(DisplaySlot.SIDEBAR);
		}
		switch (this.getScoreboardType()) {
			case POINTS:
				for (Team team : this.getTeams()) {
					String teamName = team.getKind().getColor() + team.getName() + ChatColor.RESET;
					this.getScoreboard().getObjective(DisplaySlot.SIDEBAR).getScore(teamName).setScore(team.getPoints());
				}
				break;
			case LIFEPOOL:
				for (Team team : this.getTeams()) {
					String teamName = team.getKind().getColor() + team.getName() + ChatColor.RESET;
					this.getScoreboard().getObjective(DisplaySlot.SIDEBAR).getScore(teamName).setScore(team.getRemainingLives());
				}
				break;
			case TOPKILLS:
				for (Player player : this.getPlayers()) {
					this.getScoreboard().getObjective(DisplaySlot.SIDEBAR).getScore(player.getName()).setScore(this.getKillCount(player.getName()));
				}
				break;
			case PLAYERCOUNT:
				for (Team team : this.getTeams()) {
					String teamName = team.getKind().getColor() + team.getName() + ChatColor.RESET;
					this.getScoreboard().getObjective(DisplaySlot.SIDEBAR).getScore(teamName).setScore(team.getPlayers().size());
				}
				break;
			default:
				break;
		}
	}
	
	/**
	 * Handle death messages before passing to {@link #handleDeath(Player)}
	 * for post-processing. It's like
	 * {@link #handleKill(Player, Player, Entity)}, but only for suicides.
	 * @param player Player who killed himself
	 */
	public void handleSuicide(Player player) {
		if (this.getWarzoneConfig().getBoolean(WarzoneConfig.DEATHMESSAGES)) {
			String defenderString = this.getPlayerTeam(player.getName()).getKind().getColor() + player.getName() + ChatColor.WHITE;
			this.broadcast("pvp.kill.self", defenderString);
		}
		this.handleDeath(player);
	}

	/**
	 * Handle a player killed naturally (like by a dispenser or explosion).
	 * @param player Player killed
	 * @param event Event causing damage
	 */
	public void handleNaturalKill(Player player, EntityDamageEvent event) {
		if (this.getWarzoneConfig().getBoolean(WarzoneConfig.DEATHMESSAGES)) {
			String defenderString = this.getPlayerTeam(player.getName()).getKind().getColor() + player.getName() + ChatColor.WHITE;
			if (event instanceof EntityDamageByEntityEvent
					&& ((EntityDamageByEntityEvent) event).getDamager() instanceof TNTPrimed) {
				this.broadcast("pvp.death.explosion", defenderString + ChatColor.WHITE);
			} else if (event.getCause() == DamageCause.FIRE || event.getCause() == DamageCause.FIRE_TICK
					|| event.getCause() == DamageCause.LAVA || event.getCause() == DamageCause.LIGHTNING) {
				this.broadcast("pvp.death.fire", defenderString);
			} else if (event.getCause() == DamageCause.DROWNING) {
				this.broadcast("pvp.death.drown", defenderString);
			} else if (event.getCause() == DamageCause.FALL) {
				this.broadcast("pvp.death.fall", defenderString);
			} else {
				this.broadcast("pvp.death.other", defenderString);
			}
		}
		this.handleDeath(player);
	}

	/**
	 * Cleanup after a player who has died. This decrements the team's
	 * remaining lifepool, drops stolen flags, and respawns the player.
	 * It also handles team lose and score cap conditions.
	 * This method is synchronized to prevent concurrent battle resets.
	 * @param player Player who died
	 */
	public synchronized void handleDeath(Player player) {
		Team playerTeam = this.getPlayerTeam(player.getName());
		Validate.notNull(playerTeam, "Can't find team for dead player " + player.getName());
		if (this.getWarzoneConfig().getBoolean(WarzoneConfig.REALDEATHS)) {
			this.getReallyDeadFighters().add(player.getName());
		} else {
			this.respawnPlayer(playerTeam, player);
		}
		if (playerTeam.getRemainingLives() <= 0) {
			handleTeamLoss(playerTeam, player);
		} else {
			this.dropAllStolenObjects(player, false);
			playerTeam.setRemainingLives(playerTeam.getRemainingLives() - 1);
			// Lifepool empty warning
			if (playerTeam.getRemainingLives() == 0) {
				this.broadcast("zone.lifepool.empty", playerTeam.getName());
			}
		}
		playerTeam.resetSign();
	}

	private void handleTeamLoss(Team losingTeam, Player player) {
		StringBuilder teamScores = new StringBuilder();
		List<Team> winningTeams = new ArrayList<Team>(teams.size());
		for (Team team : this.teams) {
			if (team.getPlayers().isEmpty())
				continue;
			if (team != losingTeam) {
				team.addPoint();
				team.resetSign();
				winningTeams.add(team);
			}
			teamScores.append(String.format("\n%s (%d/%d) ", team.getName(), team.getPoints(), team.getTeamConfig().resolveInt(TeamConfig.MAXSCORE)));
			team.sendAchievement("Round over! " + losingTeam.getKind().getFormattedName(), "ran out of lives.", losingTeam.getKind().getBlockHead(), 10000);
		}
		this.broadcast("zone.battle.end", losingTeam.getName(), player.getName());
		WarBattleWinEvent event1 = new WarBattleWinEvent(this, winningTeams);
		War.war.getServer().getPluginManager().callEvent(event1);
		if (!teamScores.toString().isEmpty()) {
			this.broadcast("zone.battle.newscores", teamScores.toString());
		}
		if (War.war.getMysqlConfig().isEnabled() && War.war.getMysqlConfig().isLoggingEnabled()) {
			LogKillsDeathsJob logKillsDeathsJob = new LogKillsDeathsJob(ImmutableList.copyOf(this.getKillsDeathsTracker()));
			logKillsDeathsJob.runTaskAsynchronously(War.war);
		}
		this.getKillsDeathsTracker().clear();
		if (!detectScoreCap()) {
			this.broadcast("zone.battle.reset");
			if (this.getWarzoneConfig().getBoolean(WarzoneConfig.RESETBLOCKS)) {
				this.reinitialize();
			} else {
				this.initializeZone();
			}
		}
	}

	/**
	 * Check if a team has achieved max score "score cap".
	 * @return true if team has achieved max score, false otherwise.
	 */
	public boolean detectScoreCap() {
		StringBuilder winnersStr = new StringBuilder();
		for (Team team : this.teams) {
			if (team.getPoints() >= team.getTeamConfig().resolveInt(TeamConfig.MAXSCORE)) {
				winnersStr.append(team.getName()).append(' ');
			}
		}
		if (!winnersStr.toString().isEmpty())
			this.handleScoreCapReached(winnersStr.toString());
		return !winnersStr.toString().isEmpty();
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
			player.removeAttachment(this.attachments.remove(player));
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
			this.autoTeamBalance();

			WarPlayerLeaveEvent event1 = new WarPlayerLeaveEvent(player.getName());
			War.war.getServer().getPluginManager().callEvent(event1);
		}
	}

	/**
	 * Moves players from team to team if the player size delta is greater than or equal to 2.
	 * Only works for autoassign zones.
	 */
	private void autoTeamBalance() {
		if (!this.getWarzoneConfig().getBoolean(WarzoneConfig.AUTOASSIGN)) {
			return;
		}
		boolean rerun = false;
		for (Team team1 : this.teams) {
			for (Team team2 : this.teams) {
				if (team1 == team2) {
					continue;
				}
				int t1p = team1.getPlayers().size();
				int t2p = team2.getPlayers().size();
				if (t1p - t2p >= 2) {
					Player eject = team1.getPlayers().get(killSeed.nextInt(t1p));
					team1.removePlayer(eject);
					team1.resetSign();
					this.assign(eject, team2);
					rerun = true;
					break;
				} else if (t2p - t1p >= 2) {
					Player eject = team2.getPlayers().get(killSeed.nextInt(t2p));
					team2.removePlayer(eject);
					team2.resetSign();
					this.assign(eject, team1);
					rerun = true;
					break;
				}
			}
		}
		if (rerun) {
			this.autoTeamBalance();
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
	public void addFlagThief(Team lostFlagTeam, Player flagThief) {
		this.flagThieves.put(flagThief.getUniqueId(), lostFlagTeam);
		WarPlayerThiefEvent event1 = new WarPlayerThiefEvent(flagThief, WarPlayerThiefEvent.StolenObject.FLAG);
		War.war.getServer().getPluginManager().callEvent(event1);
	}

	public boolean isFlagThief(Player suspect) {
		return this.flagThieves.containsKey(suspect.getUniqueId());
	}

	public Team getVictimTeamForFlagThief(Player thief) {
		return this.flagThieves.get(thief.getUniqueId());
	}

	public void removeFlagThief(Player thief) {
		this.flagThieves.remove(thief.getUniqueId());
	}

	// Bomb
	public void addBombThief(Bomb bomb, Player bombThief) {
		this.bombThieves.put(bombThief.getUniqueId(), bomb);
		WarPlayerThiefEvent event1 = new WarPlayerThiefEvent(bombThief, WarPlayerThiefEvent.StolenObject.BOMB);
		War.war.getServer().getPluginManager().callEvent(event1);
	}

	public boolean isBombThief(Player suspect) {
		return this.bombThieves.containsKey(suspect.getUniqueId());
	}

	public Bomb getBombForThief(Player thief) {
		return this.bombThieves.get(thief.getUniqueId());
	}
	
	// Cake

	public void removeBombThief(Player thief) {
		this.bombThieves.remove(thief.getUniqueId());
	}

	public void addCakeThief(Cake cake, Player cakeThief) {
		this.cakeThieves.put(cakeThief.getUniqueId(), cake);
		WarPlayerThiefEvent event1 = new WarPlayerThiefEvent(cakeThief, WarPlayerThiefEvent.StolenObject.CAKE);
		War.war.getServer().getPluginManager().callEvent(event1);
	}

	public boolean isCakeThief(Player suspect) {
		return this.cakeThieves.containsKey(suspect.getUniqueId());
	}

	public Cake getCakeForThief(Player thief) {
		return this.cakeThieves.get(thief.getUniqueId());
	}

	public void removeCakeThief(Player thief) {
		this.cakeThieves.remove(thief.getUniqueId());
	}

	public void clearThieves() {
		this.flagThieves.clear();
		this.bombThieves.clear();
		this.cakeThieves.clear();
	}

	public boolean isTeamFlagStolen(Team team) {
		for (UUID playerKey : this.flagThieves.keySet()) {
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
			double ecoReward = t.getTeamConfig().resolveDouble(TeamConfig.ECOREWARD);
			boolean doEcoReward = ecoReward != 0 && War.war.getEconomy() != null;
			for (Iterator<Player> it = t.getPlayers().iterator(); it.hasNext();) {
				Player tp = it.next();
				it.remove(); // Remove player from team first to prevent anti-tp
				t.removePlayer(tp);
				tp.teleport(this.getEndTeleport(LeaveCause.SCORECAP));
				if (winnersStr.contains(t.getName())) {
					// give reward
					rewardPlayer(tp, t.getInventories().resolveReward());
					if (doEcoReward) {
						EconomyResponse r;
						if (ecoReward > 0) {
							r = War.war.getEconomy().depositPlayer(tp.getName(), ecoReward);
						} else {
							r = War.war.getEconomy().withdrawPlayer(tp.getName(), ecoReward);
						}
						if (!r.transactionSuccess()) {
							War.war.getLogger().log(Level.WARNING,
								"Failed to reward player {0} ${1}. Error: {2}",
								new Object[] {tp.getName(), ecoReward, r.errorMessage});
						}
					}
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
		return this.deadMenInventories.containsKey(playerName);
	}

	public void restoreDeadmanInventory(Player player) {
		if (this.isDeadMan(player.getName())) {
			this.playerInvFromInventoryStash(player.getInventory(), this.deadMenInventories.get(player.getName()));
			this.deadMenInventories.remove(player.getName());
		}
	}

	public Location getRallyPoint() {
		return this.rallyPoint;
	}

	public void setRallyPoint(Location location) {
		this.rallyPoint = location;
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
		return teamsWithEnough >= this.getWarzoneConfig().getInt(WarzoneConfig.MINTEAMS);
	}

	/**
	 * Test whether there would be enough players with the addition of one more player
	 *
	 * @param plusOne Team to test
	 * @return true if there would be enough players
	 */
	public boolean testEnoughPlayers(TeamKind plusOne, boolean testDelayedJoin) {
		int teamsWithEnough = 0;
		for (Team team : teams) {
			int addl = 0;
			if (team.getKind() == plusOne) {
				addl = 1;
			}
			if (testDelayedJoin) {
				for (Iterator<Map.Entry<Player, Team>> iterator = this.delayedJoinPlayers.entrySet().iterator(); iterator.hasNext(); ) {
					Map.Entry<Player, Team> e = iterator.next();
					if (!isDelayedPlayerStillValid(e.getKey())) {
						iterator.remove();
						continue;
					}
					if (e.getValue() == team) {
						addl += 1;
					}
				}
			}
			if (team.getPlayers().size() + addl >= this.getWarzoneConfig().getInt(WarzoneConfig.MINPLAYERS)) {
				teamsWithEnough++;
			}
		}
		return teamsWithEnough >= this.getWarzoneConfig().getInt(WarzoneConfig.MINTEAMS);
	}

	public void signup(Player player, Team team) {
		War.war.msg(player, "You will be automatically sent to warzone when minplayers is reached.");
		this.delayedJoinPlayers.put(player, team);
		tryCallDelayedPlayers();
	}

	private void tryCallDelayedPlayers() {
		if (activeDelayedCall || (!isEnoughPlayers() && !testEnoughPlayers(null, true))) {
			return;
		}
		activeDelayedCall = true;
		for (Map.Entry<Player, Team> e : delayedJoinPlayers.entrySet()) {
			this.assign(e.getKey(), e.getValue());
		}
		delayedJoinPlayers.clear();
		activeDelayedCall = false;
	}

	private boolean isDelayedPlayerStillValid(Player player) {
		// Make sure they're online, they can play in this team, and they're not in another game
		return player.isOnline() && War.war.canPlayWar(player, delayedJoinPlayers.get(player))
				&& Warzone.getZoneByPlayerName(player.getName()) == null;
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
			sortedNames.remove("banned");
			for (Iterator<String> it = sortedNames.iterator(); it.hasNext();) {
				String loadoutName = it.next();
				Loadout ldt = Loadout.getLoadout(loadouts, loadoutName);
				if (ldt == null) {
					War.war.getLogger().warning("Failed to resolve loadout " + loadoutName);
					it.remove();
				}
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
				String name = it.next();
				if (i == currentIndex) {
					if (playerTeam.getTeamConfig().resolveBoolean(TeamConfig.PLAYERLOADOUTASDEFAULT) && name.equals("default")) {
						// Use player's own inventory as loadout
						this.resetInventory(playerTeam, player, this.getPlayerInventoryFromSavedState(player));
					} else if (isFirstRespawn && firstLoadout != null && name.equals("default")
							&& (!firstLoadout.requiresPermission() || player.hasPermission(firstLoadout.getPermission()))) {
						// Get the loadout for the first spawn
						this.resetInventory(playerTeam, player, firstLoadout.getContents());
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

	public List<CapturePoint> getCapturePoints() {
		return capturePoints;
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

	public WarzoneMaterials getWarzoneMaterials() {
		return warzoneMaterials;
	}

	public void setWarzoneMaterials(WarzoneMaterials warzoneMaterials) {
		this.warzoneMaterials = warzoneMaterials;
	}

	public Scoreboard getScoreboard() {
		return scoreboard;
	}

	public ScoreboardType getScoreboardType() {
		return scoreboardType;
	}

	/**
	 * Sets the TEMPORARY scoreboard type for use in this warzone.
	 * This type will NOT be persisted in the Warzone config.
	 *
	 * @param scoreboardType temporary scoreboard type
	 */
	public void setScoreboardType(ScoreboardType scoreboardType) {
		this.scoreboardType = scoreboardType;
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

	public int getMaxPlayers() {
		int zoneCap = 0;
		for (Team t : this.getTeams()) {
			zoneCap += t.getTeamConfig().resolveInt(TeamConfig.TEAMSIZE);
		}
		return zoneCap;
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
		if (this.isFlagThief(player)) {
			Team victimTeam = this.getVictimTeamForFlagThief(player);

			this.removeFlagThief(player);

			// Bring back flag of victim team
			victimTeam.getFlagVolume().resetBlocks();
			victimTeam.initializeTeamFlag();

			if (!quiet) {
				this.broadcast("drop.flag.broadcast", player.getName(), victimTeam.getKind().getColor() + victimTeam.getName() + ChatColor.WHITE);
			}
		} else if (this.isCakeThief(player)) {
			Cake cake = this.getCakeForThief(player);

			this.removeCakeThief(player);

			// Bring back cake
			cake.getVolume().resetBlocks();
			cake.addCakeBlocks();

			if (!quiet) {
				this.broadcast("drop.cake.broadcast", player.getName(), ChatColor.GREEN + cake.getName() + ChatColor.WHITE);
			}
		} else if (this.isBombThief(player)) {
			Bomb bomb = this.getBombForThief(player);

			this.removeBombThief(player);

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
	public boolean isThief(Player suspect) {
		return this.isFlagThief(suspect) || this.isBombThief(suspect) || this.isCakeThief(suspect);
	}

	public boolean getPvpReady() {
		return this.pvpReady;
	}
	
	public void setPvpReady(boolean ready) {
		this.pvpReady = ready;
	}

	public enum LeaveCause {
		COMMAND, DISCONNECT, SCORECAP, RESET;

		public boolean useRallyPoint() {
			return this == SCORECAP;
		}
	}
}
