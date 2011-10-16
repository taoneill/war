package bukkit.tommytony.war;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.nijiko.permissions.PermissionHandler;
import com.nijikokun.bukkit.Permissions.Permissions;
import com.tommytony.war.*;
import com.tommytony.war.jobs.HelmetProtectionTask;
import com.tommytony.war.mappers.*;
import com.tommytony.war.utils.*;

/**
 * Main class of War
 *
 * @author tommytony, Tim Düsterhus
 * @package bukkit.tommytony.war
 */
public class War extends JavaPlugin {
	public static PermissionHandler permissionHandler;
	public static War war;
	private static boolean loadedOnce = false;

	// general
	private WarPlayerListener playerListener = new WarPlayerListener();
	private WarEntityListener entityListener = new WarEntityListener();
	private WarBlockListener blockListener = new WarBlockListener();
	private WarCommandHandler commandHandler = new WarCommandHandler();
	private Logger logger;
	private PluginDescriptionFile desc = null;
	private boolean loaded = false;

	// Zones and hub
	private List<Warzone> warzones = new ArrayList<Warzone>();
	private WarHub warHub;
	private final List<Warzone> incompleteZones = new ArrayList<Warzone>();
	private final List<String> zoneMakerNames = new ArrayList<String>();
	private final List<String> commandWhitelist = new ArrayList<String>();
	private final List<String> zoneMakersImpersonatingPlayers = new ArrayList<String>();
	private HashMap<String, PlayerState> disconnected = new HashMap<String, PlayerState>();
	private final HashMap<String, String> wandBearers = new HashMap<String, String>(); // playername to zonename

	// Global settings
	private boolean pvpInZonesOnly = false;
	private boolean disablePvpMessage = false;
	private boolean buildInZonesOnly = false;
	private boolean tntInZonesOnly = false;
	private final List<String> deadlyAdjectives = new ArrayList<String>();
	private final List<String> killerVerbs = new ArrayList<String>();

	// Default warzone settings
	private final HashMap<Integer, ItemStack> defaultLoadout = new HashMap<Integer, ItemStack>();
	private final HashMap<String, HashMap<Integer, ItemStack>> defaultExtraLoadouts = new HashMap<String, HashMap<Integer, ItemStack>>();
	private int defaultLifepool = 7;
	private int defaultTeamCap = 7;
	private int defaultScoreCap = 10;
	private int defaultMonumentHeal = 5;
	private boolean defaultBlockHeads = true;
	private boolean defaultFriendlyFire = false;
	private boolean defaultAutoAssignOnly = false;
	private boolean defaultFlagPointsOnly = false;
	private boolean defaultUnbreakableZoneBlocks = false;
	private boolean defaultNoCreatures = false;
	private boolean defaultGlassWalls = true;
	private boolean defaultPvpInZone = true;
	private boolean defaultInstaBreak = false;
	private boolean defaultNoDrops = false;
	private boolean defaultNoHunger = false;
	private int defaultSaturation = 10;
	private int defaultMinPlayers = 1;	// By default, 1 player on 1 team is enough for unlocking the cant-exit-spawn guard
	private int defaultMinTeams = 1;
	private FlagReturn defaultFlagReturn = FlagReturn.BOTH;
	private boolean defaultResetOnEmpty = false, defaultResetOnLoad = false, defaultResetOnUnload = false;
	private TeamSpawnStyle defaultSpawnStyle = TeamSpawnStyle.BIG;
	private final HashMap<Integer, ItemStack> defaultReward = new HashMap<Integer, ItemStack>();

	public War() {
		super();
		War.war = this;
	}

	/**
	 * @see JavaPlugin.onEnable()
	 * @see War.loadWar()
	 */
	public void onEnable() {
		this.loadWar();
	}

	/**
	 * @see JavaPlugin.onDisable()
	 * @see War.unloadWar()
	 */
	public void onDisable() {
		this.unloadWar();
	}

	/**
	 * Initializes war
	 */
	public void loadWar() {
		this.setLoaded(true);
		this.desc = this.getDescription();
		this.logger = this.getServer().getLogger();
		this.setupPermissions();

		if (!War.loadedOnce) {
			War.loadedOnce = true; // This prevented multiple hookups of the same listener

			// Register hooks
			PluginManager pm = this.getServer().getPluginManager();

			pm.registerEvent(Event.Type.PLAYER_QUIT, this.playerListener, Priority.Normal, this);
			pm.registerEvent(Event.Type.PLAYER_KICK, this.playerListener, Priority.Normal, this);
			pm.registerEvent(Event.Type.PLAYER_MOVE, this.playerListener, Priority.Normal, this);
			pm.registerEvent(Event.Type.PLAYER_PICKUP_ITEM, this.playerListener, Priority.Normal, this);
			pm.registerEvent(Event.Type.INVENTORY_OPEN, this.playerListener, Priority.Normal, this);
			pm.registerEvent(Event.Type.PLAYER_DROP_ITEM, this.playerListener, Priority.Normal, this);
			pm.registerEvent(Event.Type.PLAYER_COMMAND_PREPROCESS, this.playerListener, Priority.Normal, this);
			pm.registerEvent(Event.Type.PLAYER_INTERACT, this.playerListener, Priority.Normal, this);
			pm.registerEvent(Event.Type.PLAYER_TOGGLE_SNEAK, this.playerListener, Priority.Normal, this);

			pm.registerEvent(Event.Type.ENTITY_EXPLODE, this.entityListener, Priority.Normal, this);
			pm.registerEvent(Event.Type.ENTITY_DAMAGE, this.entityListener, Priority.High, this);
			pm.registerEvent(Event.Type.ENTITY_COMBUST, this.entityListener, Priority.Normal, this);
			pm.registerEvent(Event.Type.CREATURE_SPAWN, this.entityListener, Priority.Normal, this);
			pm.registerEvent(Event.Type.ENTITY_REGAIN_HEALTH, this.entityListener, Priority.Normal, this);
			pm.registerEvent(Event.Type.FOOD_LEVEL_CHANGE, this.entityListener, Priority.Normal, this);

			pm.registerEvent(Event.Type.BLOCK_PLACE, this.blockListener, Priority.Normal, this);
			pm.registerEvent(Event.Type.BLOCK_DAMAGE, this.blockListener, Priority.Normal, this);
			pm.registerEvent(Event.Type.BLOCK_BREAK, this.blockListener, Priority.Normal, this);
		}

		// Load files from disk or create them (using these defaults)
		this.getDefaultLoadout().clear();
		this.getDefaultLoadout().put(0, new ItemStack(Material.STONE_SWORD, 1, (byte) 8));
		this.getDefaultLoadout().put(1, new ItemStack(Material.BOW, 1, (byte) 8));
		this.getDefaultLoadout().put(2, new ItemStack(Material.ARROW, 7));
		this.getDefaultLoadout().put(3, new ItemStack(Material.IRON_PICKAXE, 1, (byte) 8));
		this.getDefaultLoadout().put(4, new ItemStack(Material.STONE_SPADE, 1, (byte) 8));
		this.getDefaultReward().clear();
		this.getDefaultReward().put(0, new ItemStack(Material.CAKE, 1));
		
		this.getDeadlyAdjectives().clear();
		this.getDeadlyAdjectives().add("");
		this.getDeadlyAdjectives().add("");
		this.getDeadlyAdjectives().add("mighty ");
		this.getDeadlyAdjectives().add("deadly ");
		this.getDeadlyAdjectives().add("fine ");
		this.getDeadlyAdjectives().add("precise ");
		this.getDeadlyAdjectives().add("brutal ");
		this.getDeadlyAdjectives().add("powerful ");
		
		this.getKillerVerbs().clear();
		this.getKillerVerbs().add("killed");
		this.getKillerVerbs().add("killed");
		this.getKillerVerbs().add("killed");
		this.getKillerVerbs().add("finished");
		this.getKillerVerbs().add("annihilated");
		this.getKillerVerbs().add("murdered");
		this.getKillerVerbs().add("obliterated");
		this.getKillerVerbs().add("exterminated");
		
		WarMapper.load();
		HelmetProtectionTask helmetProtectionTask = new HelmetProtectionTask();
		this.getServer().getScheduler().scheduleSyncRepeatingTask(this, helmetProtectionTask, 250, 100);
		this.log("War v" + this.desc.getVersion() + " is on.", Level.INFO);
	}

	/**
	 * Cleans up war
	 */
	public void unloadWar() {
		for (Warzone warzone : this.warzones) {
			warzone.unload();
		}
		this.warzones.clear();

		if (this.warHub != null) {
			this.warHub.getVolume().resetBlocks();
		}

		this.getServer().getScheduler().cancelTasks(this);
		this.playerListener.purgeLatestPositions();

		this.log("War v" + this.desc.getVersion() + " is off.", Level.INFO);
		this.setLoaded(false);
	}

	/**
	 * Initializes Permissions
	 */
	public void setupPermissions() {
		Plugin permissionsPlugin = this.getServer().getPluginManager().getPlugin("Permissions");
		if (War.permissionHandler == null) {
			if (permissionsPlugin != null) {
				War.permissionHandler = ((Permissions) permissionsPlugin).getHandler();
			} else {
				this.log("Permissions system not enabled. Defaulting to regular War config.", Level.INFO);
			}
		}
	}

	/**
	 * @see JavaPlugin.onCommand()
	 */
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		return this.commandHandler.handle(sender, cmd, args);
	}

	/**
	 * Converts the player-inventory to a loadout hashmap
	 *
	 * @param inv
	 *                inventory to get the items from
	 * @param loadout
	 *                the hashmap to save to
	 */
	private void inventoryToLoadout(PlayerInventory inv, HashMap<Integer, ItemStack> loadout) {
		loadout.clear();
		int i = 0;
		for (ItemStack stack : inv.getContents()) {
			if (stack != null && stack.getType() != Material.AIR) {
				loadout.put(i, stack);
				i++;
			}

		}
		if (inv.getBoots() != null && inv.getBoots().getType() != Material.AIR) {
			loadout.put(100, inv.getBoots());
		}
		if (inv.getLeggings() != null && inv.getLeggings().getType() != Material.AIR) {
			loadout.put(101, inv.getLeggings());
		}
		if (inv.getChestplate() != null && inv.getChestplate().getType() != Material.AIR) {
			loadout.put(102, inv.getChestplate());
		}
	}

	/**
	 * Converts the player-inventory to a loadout hashmap
	 *
	 * @param player
	 *                player to get the inventory to get the items from
	 * @param loadout
	 *                the hashmap to save to
	 */
	private void inventoryToLoadout(Player player, HashMap<Integer, ItemStack> loadout) {
		this.inventoryToLoadout(player.getInventory(), loadout);
	}

	public String updateZoneFromNamedParams(Warzone warzone, CommandSender commandSender, String[] arguments) {
		try {
			Map<String, String> namedParams = new HashMap<String, String>();
			for (String namedPair : arguments) {
				String[] pairSplit = namedPair.split(":");
				if (pairSplit.length == 2) {
					namedParams.put(pairSplit[0].toLowerCase(), pairSplit[1]);
				}
			}
			
			StringBuilder returnMessage = new StringBuilder();
			if (namedParams.containsKey("author")) {
				for(String author : namedParams.get("author").split(",")) {
					if (!author.equals("") && !warzone.getAuthors().contains(author)) {
						warzone.addAuthor(author);
						returnMessage.append(" author " + author + " added.");
					}
				}
			}
			if (namedParams.containsKey("deleteauthor")) {
				for(String author : namedParams.get("deleteauthor").split(",")) {
					if (warzone.getAuthors().contains(author)) {
						warzone.getAuthors().remove(author);
						returnMessage.append(" " + author + " removed from zone authors.");
					}
				}
			}
			if (namedParams.containsKey("lifepool")) {
				warzone.setLifePool(Integer.parseInt(namedParams.get("lifepool")));
				returnMessage.append(" lifepool set to " + warzone.getLifePool() + ".");
			}
			if (namedParams.containsKey("monumentheal")) {
				warzone.setMonumentHeal(Integer.parseInt(namedParams.get("monumentheal")));
				returnMessage.append(" monumentheal set to " + warzone.getMonumentHeal() + ".");
			}
			if (namedParams.containsKey("teamsize")) {
				warzone.setTeamCap(Integer.parseInt(namedParams.get("teamsize")));
				returnMessage.append(" teamsize set to " + warzone.getTeamCap() + ".");
			}
			if (namedParams.containsKey("maxscore")) {
				warzone.setScoreCap(Integer.parseInt(namedParams.get("maxscore")));
				returnMessage.append(" maxscore set to " + warzone.getScoreCap() + ".");
			}
			if (namedParams.containsKey("ff")) {
				String onOff = namedParams.get("ff");
				warzone.setFriendlyFire(onOff.equals("on") || onOff.equals("true"));
				returnMessage.append(" ff set to " + String.valueOf(warzone.getFriendlyFire()) + ".");
			}
			if (namedParams.containsKey("autoassign")) {
				String onOff = namedParams.get("autoassign");
				warzone.setAutoAssignOnlyAndResetLobby(onOff.equals("on") || onOff.equals("true"));
				returnMessage.append(" autoassign set to " + String.valueOf(warzone.isAutoAssignOnly()) + ".");
			}
			if (namedParams.containsKey("flagpointsonly")) {
				String onOff = namedParams.get("flagpointsonly");
				warzone.setFlagPointsOnly(onOff.equals("on") || onOff.equals("true"));
				returnMessage.append(" flagpointsonly set to " + String.valueOf(warzone.isFlagPointsOnly()) + ".");
			}
			if (namedParams.containsKey("blockheads")) {
				String onOff = namedParams.get("blockheads");
				warzone.setBlockHeads(onOff.equals("on") || onOff.equals("true"));
				returnMessage.append(" blockheads set to " + String.valueOf(warzone.isBlockHeads()) + ".");
			}
			if (namedParams.containsKey("spawnstyle")) {
				String spawnStyle = namedParams.get("spawnstyle");
				warzone.setSpawnStyle(TeamSpawnStyle.getStyleFromString(spawnStyle));
				returnMessage.append(" spawnstyle set to " + warzone.getSpawnStyle().toString() + ".");
			}
			if (namedParams.containsKey("flagreturn")) {
				String flagReturn = namedParams.get("flagreturn").toLowerCase();
				if (flagReturn.equals("flag")) {
					warzone.setFlagReturn(FlagReturn.FLAG);
				} else if (flagReturn.equals("spawn")) {
					warzone.setFlagReturn(FlagReturn.SPAWN);
				} else {
					warzone.setFlagReturn(FlagReturn.BOTH);
				}
				returnMessage.append(" flagreturn set to " + warzone.getFlagReturn().toString() + ".");
			}
			if (namedParams.containsKey("unbreakable")) {
				String onOff = namedParams.get("unbreakable");
				warzone.setUnbreakableZoneBlocks(onOff.equals("on") || onOff.equals("true"));
				returnMessage.append(" unbreakable set to " + String.valueOf(warzone.isUnbreakableZoneBlocks()) + ".");
			}
			if (namedParams.containsKey("disabled")) {
				String onOff = namedParams.get("disabled");
				warzone.setDisabled(onOff.equals("on") || onOff.equals("true"));
				returnMessage.append(" disabled set to " + String.valueOf(warzone.isDisabled()) + ".");
			}
			if (namedParams.containsKey("nocreatures")) {
				String onOff = namedParams.get("nocreatures");
				warzone.setNoCreatures(onOff.equals("on") || onOff.equals("true"));
				returnMessage.append(" nocreatures set to " + String.valueOf(warzone.isNoCreatures()) + ".");
			}
			if (namedParams.containsKey("glasswalls")) {
				String onOff = namedParams.get("glasswalls");
				warzone.setGlassWalls(onOff.equals("on") || onOff.equals("true"));
				returnMessage.append(" glasswalls set to " + String.valueOf(warzone.isGlassWalls()) + ".");
			}
			if (namedParams.containsKey("pvpinzone")) {
				String onOff = namedParams.get("pvpinzone");
				warzone.setPvpInZone(onOff.equals("on") || onOff.equals("true"));
				returnMessage.append(" pvpinzone set to " + String.valueOf(warzone.isPvpInZone()) + ".");
			}
			if (namedParams.containsKey("instabreak")) {
				String onOff = namedParams.get("instabreak");
				warzone.setInstaBreak(onOff.equals("on") || onOff.equals("true"));
				returnMessage.append(" instabreak set to " + String.valueOf(warzone.isInstaBreak()) + ".");
			}
			if (namedParams.containsKey("nodrops")) {
				String onOff = namedParams.get("nodrops");
				warzone.setNoDrops(onOff.equals("on") || onOff.equals("true"));
				returnMessage.append(" nodrops set to " + String.valueOf(warzone.isNoDrops()) + ".");
			}
			if (namedParams.containsKey("nohunger")) {
				String onOff = namedParams.get("nohunger");
				warzone.setNoHunger(onOff.equals("on") || onOff.equals("true"));
				returnMessage.append(" nohunger set to " + String.valueOf(warzone.isNoHunger()) + ".");
			}
			if (namedParams.containsKey("saturation")) {
				int sat = Integer.parseInt(namedParams.get("saturation"));
				if (sat > 20) {
					sat = 20;
				} else if (sat < 0) {
					sat = 0;
				}
				warzone.setSaturation(sat);
				returnMessage.append(" saturation set to " + warzone.getSaturation() + ".");
			}
			if (namedParams.containsKey("minplayers")) {
				int val = Integer.parseInt(namedParams.get("minplayers"));
				if (val > warzone.getTeamCap()) {
					returnMessage.append(" minplayers can't be greater than teamsize.");
				} else {
					warzone.setMinPlayers(val);
					returnMessage.append(" minplayers set to " + warzone.getMinPlayers() + ".");	
				}				
			}
			if (namedParams.containsKey("minteams")) {
				int val = Integer.parseInt(namedParams.get("minteams"));
				if (val > warzone.getTeams().size()) {
					returnMessage.append(" minteams can't be higher than number of teams.");
				} else {
					warzone.setMinTeams(val);
					returnMessage.append(" minteams set to " + warzone.getMinTeams() + ".");
				}
			}			

			if (namedParams.containsKey("resetonempty")) {
				String onOff = namedParams.get("resetonempty");
				warzone.setResetOnEmpty(onOff.equals("on") || onOff.equals("true"));
				returnMessage.append(" resetonempty set to " + String.valueOf(warzone.isResetOnEmpty()) + ".");
			}
			if (namedParams.containsKey("resetonload")) {
				String onOff = namedParams.get("resetonload");
				warzone.setResetOnLoad(onOff.equals("on") || onOff.equals("true"));
				returnMessage.append(" resetonload set to " + String.valueOf(warzone.isResetOnLoad()) + ".");
			}
			if (namedParams.containsKey("resetonunload")) {
				String onOff = namedParams.get("resetonunload");
				warzone.setResetOnUnload(onOff.equals("on") || onOff.equals("true"));
				returnMessage.append(" resetonunload set to " + String.valueOf(warzone.isResetOnUnload()) + ".");
			}
			if (commandSender instanceof Player) {
				Player player = (Player) commandSender;
				if (namedParams.containsKey("loadout")) {
					String loadoutName = namedParams.get("loadout");
					if (loadoutName.equals("default")) {
						this.inventoryToLoadout(player, warzone.getLoadout());
					} else {
						HashMap<Integer, ItemStack> extraLoadout = warzone.getExtraLoadouts().get(loadoutName);
						if (extraLoadout == null) {
							extraLoadout = new HashMap<Integer, ItemStack>();
							warzone.getExtraLoadouts().put(loadoutName, extraLoadout);
						}
						this.inventoryToLoadout(player, extraLoadout);
					}
					returnMessage.append(" " + loadoutName + " respawn loadout updated.");
				}
				if (namedParams.containsKey("deleteloadout")) {
					String loadoutName = namedParams.get("deleteloadout");
					if (loadoutName.equals("default")) {
						returnMessage.append(" Can't remove default loadout.");
					} else {
						HashMap<Integer, ItemStack> extraLoadout = warzone.getExtraLoadouts().get(loadoutName);
						if (warzone.getExtraLoadouts().keySet().contains(loadoutName)) {
							warzone.getExtraLoadouts().remove(loadoutName);
							returnMessage.append(" " + loadoutName + " loadout removed.");
						} else {
							returnMessage.append(" " + loadoutName + " loadout not found.");
						}
					}
				}
				if (namedParams.containsKey("reward")) {
					this.inventoryToLoadout(player, warzone.getReward());
					returnMessage.append(" game end reward updated.");
				}
			}

			return returnMessage.toString();
		} catch (Exception e) {
			return "PARSE-ERROR";
		}
	}

	public String updateFromNamedParams(CommandSender commandSender, String[] arguments) {
		try {
			Map<String, String> namedParams = new HashMap<String, String>();
			for (String namedPair : arguments) {
				String[] pairSplit = namedPair.split(":");
				if (pairSplit.length == 2) {
					namedParams.put(pairSplit[0].toLowerCase(), pairSplit[1]);
				}
			}
			
			StringBuilder returnMessage = new StringBuilder();
			if (namedParams.containsKey("pvpinzonesonly")) {
				String onOff = namedParams.get("pvpinzonesonly");
				this.setPvpInZonesOnly(onOff.equals("on") || onOff.equals("true"));
				returnMessage.append(" flagpointsonly set to " + String.valueOf(war.isDefaultFlagPointsOnly()) + ".");
			}
			if (namedParams.containsKey("disablepvpmessage")) {
				String onOff = namedParams.get("disablepvpmessage");
				this.setDisablePvpMessage(onOff.equals("on") || onOff.equals("true"));
				returnMessage.append(" disablepvpmessage set to " + String.valueOf(war.isDisablePvpMessage()) + ".");
			}
			if (namedParams.containsKey("buildinzonesonly")) {
				String onOff = namedParams.get("buildinzonesonly");
				this.setBuildInZonesOnly(onOff.equals("on") || onOff.equals("true"));
				returnMessage.append(" buildinzonesonly set to " + String.valueOf(war.isBuildInZonesOnly()) + ".");
			}
			if (namedParams.containsKey("tntinzonesonly")) {
				String onOff = namedParams.get("tntinzonesonly");
				this.setTntInZonesOnly(onOff.equals("on") || onOff.equals("true"));
				returnMessage.append(" tntinzonesonly set to " + String.valueOf(war.isTntInZonesOnly()) + ".");
			}
			
			if (namedParams.containsKey("lifepool")) {
				this.setDefaultLifepool(Integer.parseInt(namedParams.get("lifepool")));
				returnMessage.append(" lifepool set to " + war.getDefaultLifepool() + ".");
			}
			if (namedParams.containsKey("monumentheal")) {
				this.setDefaultMonumentHeal(Integer.parseInt(namedParams.get("monumentheal")));
				returnMessage.append(" monumentheal set to " + war.getDefaultMonumentHeal() + ".");
			}
			if (namedParams.containsKey("teamsize")) {
				this.setDefaultTeamCap(Integer.parseInt(namedParams.get("teamsize")));
				returnMessage.append(" teamsize set to " + war.getDefaultTeamCap() + ".");
			}
			if (namedParams.containsKey("maxscore")) {
				this.setDefaultScoreCap(Integer.parseInt(namedParams.get("maxscore")));
				returnMessage.append(" maxscore set to " + war.getDefaultScoreCap() + ".");
			}
			if (namedParams.containsKey("ff")) {
				String onOff = namedParams.get("ff");
				this.setDefaultFriendlyFire(onOff.equals("on") || onOff.equals("true"));
				returnMessage.append(" ff set to " + String.valueOf(war.isDefaultFriendlyFire()) + ".");
			}
			if (namedParams.containsKey("autoassign")) {
				String onOff = namedParams.get("autoassign");
				this.setDefaultAutoAssignOnly(onOff.equals("on") || onOff.equals("true"));
				returnMessage.append(" autoassign set to " + String.valueOf(war.isDefaultAutoAssignOnly()) + ".");
			}
			if (namedParams.containsKey("flagpointsonly")) {
				String onOff = namedParams.get("flagpointsonly");
				this.setDefaultFlagPointsOnly(onOff.equals("on") || onOff.equals("true"));
				returnMessage.append(" flagpointsonly set to " + String.valueOf(war.isDefaultFlagPointsOnly()) + ".");
			}			
			if (namedParams.containsKey("blockheads")) {
				String onOff = namedParams.get("blockheads");
				this.setDefaultBlockHeads(onOff.equals("on") || onOff.equals("true"));
				returnMessage.append(" blockheads set to " + String.valueOf(war.isDefaultBlockHeads()) + ".");
			}
			if (namedParams.containsKey("spawnstyle")) {
				String spawnStyle = namedParams.get("spawnstyle");
				this.setDefaultSpawnStyle(TeamSpawnStyle.getStyleFromString(spawnStyle));
				returnMessage.append(" spawnstyle set to " + war.getDefaultSpawnStyle().toString() + ".");
			}
			if (namedParams.containsKey("flagreturn")) {
				String flagreturn = namedParams.get("flagreturn").toLowerCase();
				if (flagreturn.equals("flag")) {
					this.setDefaultFlagReturn(FlagReturn.FLAG);
				} else if (flagreturn.equals("spawn")) {
					this.setDefaultFlagReturn(FlagReturn.SPAWN);
				} else {
					this.setDefaultFlagReturn(FlagReturn.BOTH);
				}
				returnMessage.append(" flagreturn set to " + war.getDefaultFlagReturn().toString() + ".");
			}
			
			if (namedParams.containsKey("unbreakable")) {
				String onOff = namedParams.get("unbreakable");
				this.setDefaultUnbreakableZoneBlocks(onOff.equals("on") || onOff.equals("true"));
				returnMessage.append(" unbreakable set to " + String.valueOf(war.isDefaultUnbreakableZoneBlocks()) + ".");
			}
			if (namedParams.containsKey("nocreatures")) {
				String onOff = namedParams.get("nocreatures");
				this.setDefaultNoCreatures(onOff.equals("on") || onOff.equals("true"));
				returnMessage.append(" nocreatures set to " + String.valueOf(war.isDefaultNoCreatures()) + ".");
			}
			if (namedParams.containsKey("glasswalls")) {
				String onOff = namedParams.get("glasswalls");
				this.setDefaultGlassWalls(onOff.equals("on") || onOff.equals("true"));
				returnMessage.append(" glasswalls set to " + String.valueOf(war.isDefaultGlassWalls()) + ".");
			}
			if (namedParams.containsKey("pvpinzone")) {
				String onOff = namedParams.get("pvpinzone");
				this.setDefaultPvpInZone(onOff.equals("on") || onOff.equals("true"));
				returnMessage.append(" pvpinzone set to " + String.valueOf(war.isDefaultPvpInZone()) + ".");
			}
			if (namedParams.containsKey("instabreak")) {
				String onOff = namedParams.get("instabreak");
				this.setDefaultInstaBreak(onOff.equals("on") || onOff.equals("true"));
				returnMessage.append(" instabreak set to " + String.valueOf(war.isDefaultInstaBreak()) + ".");
			}
			if (namedParams.containsKey("nodrops")) {
				String onOff = namedParams.get("nodrops");
				war.setDefaultNoDrops(onOff.equals("on") || onOff.equals("true"));
				returnMessage.append(" nodrops set to " + String.valueOf(war.isDefaultNoDrops()) + ".");
			}
			if (namedParams.containsKey("nohunger")) {
				String onOff = namedParams.get("nohunger");
				this.setDefaultNoHunger(onOff.equals("on") || onOff.equals("true"));
				returnMessage.append(" nohunger set to " + String.valueOf(this.isDefaultNoHunger()) + ".");
			}
			if (namedParams.containsKey("saturation")) {
				int sat = Integer.parseInt(namedParams.get("saturation"));
				if (sat > 20) {
					sat = 20;
				} else if (sat < 0) {
					sat = 0;
				}
				this.setDefaultSaturation(sat);
				returnMessage.append(" saturation set to " + this.getDefaultSaturation() + ".");
			}
			if (namedParams.containsKey("minplayers")) {
				int val = Integer.parseInt(namedParams.get("minplayers"));
				if (val > this.getDefaultTeamCap()) {
					returnMessage.append(" minplayers can't be greater than teamsize.");
				} else {
					this.setDefaultMinPlayers(val);
					returnMessage.append(" minplayers set to " + this.getDefaultMinPlayers() + ".");	
				}				
			}
			if (namedParams.containsKey("minteams")) {
				this.setDefaultMinTeams(Integer.parseInt(namedParams.get("minteams")));
				returnMessage.append(" minteams set to " + this.getDefaultMinTeams() + ".");
			}	

			if (namedParams.containsKey("resetonempty")) {
				String onOff = namedParams.get("resetonempty");
				this.setDefaultResetOnEmpty(onOff.equals("on") || onOff.equals("true"));
				returnMessage.append(" resetonempty set to " + String.valueOf(war.isDefaultResetOnEmpty()) + ".");
			}
			if (namedParams.containsKey("resetonload")) {
				String onOff = namedParams.get("resetonload");
				this.setDefaultResetOnLoad(onOff.equals("on") || onOff.equals("true"));
				returnMessage.append(" resetonload set to " + String.valueOf(war.isDefaultResetOnLoad()) + ".");
			}
			if (namedParams.containsKey("resetonunload")) {
				String onOff = namedParams.get("resetonunload");
				this.setDefaultResetOnUnload(onOff.equals("on") || onOff.equals("true"));
				returnMessage.append(" resetonunload set to " + String.valueOf(war.isDefaultResetOnUnload()) + ".");
			}
			if (commandSender instanceof Player) {
				Player player = (Player) commandSender;
				if (namedParams.containsKey("loadout")) {
					String loadoutName = namedParams.get("loadout");
					if (loadoutName.equals("default")) {
						this.inventoryToLoadout(player, this.getDefaultLoadout());
					} else {
						HashMap<Integer, ItemStack> extraLoadout = this.getDefaultExtraLoadouts().get(loadoutName);
						if (extraLoadout == null) {
							HashMap<Integer, ItemStack> newLoadout = new HashMap<Integer, ItemStack>();
							this.getDefaultExtraLoadouts().put(loadoutName, newLoadout);
						}
						this.inventoryToLoadout(player, extraLoadout);
					}
					returnMessage.append(loadoutName + " respawn loadout updated.");
				} 
				if (namedParams.containsKey("deleteloadout")) {
					String loadoutName = namedParams.get("deleteloadout");
					if (loadoutName.equals("default")) {
						returnMessage.append(" Can't remove default loadout.");
					} else {
						HashMap<Integer, ItemStack> extraLoadout = this.getDefaultExtraLoadouts().get(loadoutName);
						if (this.getDefaultExtraLoadouts().keySet().contains(loadoutName)) {
							this.getDefaultExtraLoadouts().remove(loadoutName);
							returnMessage.append(" " + loadoutName + " loadout removed.");
						} else {
							returnMessage.append(" " + loadoutName + " loadout not found.");
						}
					}
				}
				if (namedParams.containsKey("reward")) {
					this.inventoryToLoadout(player, this.getDefaultReward());
					returnMessage.append(" game end reward updated.");
				}
				if (namedParams.containsKey("rallypoint")) {
					String zoneName = namedParams.get("rallypoint");
					this.setZoneRallyPoint(zoneName, player);
					returnMessage.append(" rallypoint set for zone " + zoneName + ".");
				}
			}

			return returnMessage.toString();
		} catch (Exception e) {
			return "PARSE-ERROR";
		}
	}

	public String printConfig(Warzone zone) {
		ChatColor color = ChatColor.RED;
		ChatColor alt = ChatColor.GREEN;
		ChatColor normal = ChatColor.WHITE;
		return "Warzone " + zone.getName() + " config -"
		 + " author:" + alt + ifEmptyEveryone(zone.getAuthorsString()) + normal
		 + " lifepool:" + color + zone.getLifePool() + normal
		 + " teamsize:" + color + zone.getTeamCap() + normal
		 + " maxscore:" + color + zone.getScoreCap() + normal
		 + " ff:" + color + String.valueOf(zone.getFriendlyFire()) + normal
		 + " autoassign:" + color + String.valueOf(zone.isAutoAssignOnly()) + normal
		 + " flagpointsonly:" + color + String.valueOf(zone.isFlagPointsOnly()) + normal
		 + " blockheads:" + color + String.valueOf(zone.isBlockHeads()) + normal
		 + " spawnstyle:" + color + zone.getSpawnStyle() + normal
		 + " flagreturn:" + color + zone.getFlagReturn() + normal
		 + " monumentheal:" + color + zone.getMonumentHeal() + normal
		 + " unbreakable:" + color + String.valueOf(zone.isUnbreakableZoneBlocks()) + normal
		 + " disabled:" + color + String.valueOf(zone.isDisabled()) + normal
		 + " nocreatures:" + color + String.valueOf(zone.isNoCreatures()) + normal
		 + " glasswalls:" + color + String.valueOf(zone.isGlassWalls()) + normal
		 + " pvpinzone:" + color + String.valueOf(zone.isPvpInZone()) + normal
		 + " instabreak:" + color + String.valueOf(zone.isInstaBreak()) + normal
		 + " nodrops:" + color + String.valueOf(zone.isNoDrops()) + normal
		 + " nohunger:" + color + String.valueOf(zone.isNoHunger()) + normal
		 + " saturation:" + color + zone.getSaturation() + normal
		 + " minplayers:" + color + zone.getMinPlayers() + normal
		 + " minteams:" + color + zone.getMinTeams() + normal
		 + " resetonempty:" + color + String.valueOf(zone.isResetOnEmpty()) + normal
		 + " resetonload:" + color + String.valueOf(zone.isResetOnLoad()) + normal
		 + " resetonunload:" + color + String.valueOf(zone.isResetOnUnload());
	}
	
	private String ifEmptyEveryone(String maybeEmpty) {
		if (maybeEmpty.equals("")) {
			maybeEmpty = "*";
		}
		return maybeEmpty;
	}

	public String printConfig() {
		ChatColor color = ChatColor.RED;
		ChatColor global = ChatColor.GREEN;
		ChatColor normal = ChatColor.WHITE;
		return "War config -"
		 + " pvpinzonesonly:" + global + String.valueOf(this.isPvpInZonesOnly()) + normal
		 + " disablepvpmessage:" + global + String.valueOf(this.isDisablePvpMessage()) + normal
		 + " buildinzonesonly:" + global + String.valueOf(this.isBuildInZonesOnly()) + normal
		 + " tntinzonesonly:" + global + String.valueOf(this.isTntInZonesOnly()) + normal
		 + " - Warzone defaults -"
		 + " lifepool:" + color + this.getDefaultLifepool() + normal
		 + " teamsize:" + color + this.getDefaultTeamCap() + normal
		 + " maxscore:" + color + this.getDefaultScoreCap() + normal
		 + " ff:" + color + String.valueOf(this.isDefaultFriendlyFire()) + normal
		 + " autoassign:" + color + String.valueOf(this.isDefaultAutoAssignOnly()) + normal
		 + " flagpointsonly:" + color + String.valueOf(this.isDefaultFlagPointsOnly()) + normal
		 + " blockheads:" + color + String.valueOf(this.isDefaultBlockHeads()) + normal
		 + " spawnstyle:" + color + this.getDefaultSpawnStyle() + normal
		 + " flagreturn:" + color + this.getDefaultFlagReturn() + normal
		 + " monumentheal:" + color + this.getDefaultMonumentHeal() + normal
		 + " unbreakable:" + color + String.valueOf(this.isDefaultUnbreakableZoneBlocks()) + normal
		 + " nocreatures:" + color + String.valueOf(this.isDefaultNoCreatures()) + normal
		 + " glasswalls:" + color + String.valueOf(this.isDefaultGlassWalls()) + normal
		 + " pvpinzone:" + color + String.valueOf(this.isDefaultPvpInZone()) + normal
		 + " instabreak:" + color + String.valueOf(this.isDefaultInstaBreak()) + normal
		 + " nodrops:" + color + String.valueOf(this.isDefaultNoDrops()) + normal
		 + " nohunger:" + color + String.valueOf(this.isDefaultNoHunger()) + normal
		 + " saturation:" + color + this.getDefaultSaturation() + normal
		 + " minplayers:" + color + this.getDefaultMinPlayers() + normal
		 + " minteams:" + color + this.getDefaultMinTeams() + normal
		 + " resetonempty:" + color + String.valueOf(this.isDefaultResetOnEmpty()) + normal
		 + " resetonload:" + color + String.valueOf(this.isDefaultResetOnLoad()) + normal
		 + " resetonunload:" + color + String.valueOf(this.isDefaultResetOnUnload());
	}

	private void setZoneRallyPoint(String warzoneName, Player player) {
		Warzone zone = this.findWarzone(warzoneName);
		if (zone == null) {
			this.badMsg(player, "Can't set rally point. No such warzone.");
		} else {
			zone.setRallyPoint(player.getLocation());
			WarzoneMapper.save(zone, false);
		}
	}

	public void addWarzone(Warzone zone) {
		this.warzones.add(zone);
	}

	public List<Warzone> getWarzones() {
		return this.warzones;
	}

	public void msg(CommandSender sender, String str) {
		if (sender instanceof Player) {
			String out = ChatColor.GRAY + "War> " + ChatColor.WHITE + this.colorKnownTokens(str, ChatColor.WHITE) + " ";
			ChatFixUtil.sendMessage(sender, out);
		} else {
			sender.sendMessage("War> " + str);
		}
	}

	public void badMsg(CommandSender sender, String str) {
		if (sender instanceof Player) {
			String out = ChatColor.GRAY + "War> " + ChatColor.RED + this.colorKnownTokens(str, ChatColor.RED) + " ";
			ChatFixUtil.sendMessage(sender, out);
		} else {
			sender.sendMessage("War> " + str);
		}
	}

	/**
	 * Colors the teams and examples in messages
	 *
	 * @param String
	 *                str message-string
	 * @param String
	 *                msgColor current message-color
	 * @return String Message with colored teams
	 */
	private String colorKnownTokens(String str, ChatColor msgColor) {
		for (TeamKind kind : TeamKind.values()) {
			str = str.replaceAll(" " + kind.toString(), " " + kind.getColor() + kind.toString() + msgColor);
		}
		str = str.replaceAll("Ex -", ChatColor.GRAY + "Ex -");
		return str;
	}

	/**
	 * Logs a specified message with a specified level
	 *
	 * @param String
	 *                str message to log
	 * @param Level
	 *                lvl level to use
	 */
	public void log(String str, Level lvl) {
		this.getLogger().log(lvl, "War> " + str);
	}

	// the only way to find a zone that has only one corner
	public Warzone findWarzone(String warzoneName) {
		for (Warzone warzone : this.warzones) {
			if (warzone.getName().toLowerCase().equals(warzoneName.toLowerCase())) {
				return warzone;
			}
		}
		for (Warzone warzone : this.incompleteZones) {
			if (warzone.getName().equals(warzoneName)) {
				return warzone;
			}
		}
		return null;
	}

	/**
	 * Checks whether the given player is allowed to play war.
	 *
	 * @param 	player	Player to check
	 * @return		true if the player may play war
	 */
	public boolean canPlayWar(Player player) {
		if (War.permissionHandler != null) {
			if (War.permissionHandler.has(player, "war.player") || War.permissionHandler.has(player, "War.player")) {
				return true;
			} else {
				return false;
			}
		} else {
			// w/o Permissions, everyone can play
			return player.hasPermission("war.player");
		}
	}

	/**
	 * Checks whether the given player is allowed to warp.
	 *
	 * @param 	player	Player to check
	 * @return		true if the player may warp
	 */
	public boolean canWarp(Player player) {
		if (War.permissionHandler != null) {
			if (War.permissionHandler.has(player, "war.warp") || War.permissionHandler.has(player, "War.warp")) {
				return true; 
			} else {
				return false;
			}
			
		} else {
			// w/o Permissions, everyone can warp
			return player.hasPermission("war.warp");
		}
	}

	/**
	 * Checks whether the given player is allowed to build outside zones
	 *
	 * @param 	player	Player to check
	 * @return		true if the player may build outside zones
	 */
	public boolean canBuildOutsideZone(Player player) {
		if (this.isBuildInZonesOnly()) {
			if (War.permissionHandler != null) {
				if (War.permissionHandler.has(player, "war.build") || War.permissionHandler.has(player, "War.build")) {
					return true;
				} else {
					return false;
				}
			} else {
				// w/o Permissions, if buildInZonesOnly, no one can build outside the zone except Zonemakers
				return player.hasPermission("war.build");
			}
		} else {
			return true;
		}
	}

	/**
	 * Checks whether the given player is allowed to pvp outside zones
	 *
	 * @param 	player	Player to check
	 * @return		true if the player may pvp outside zones
	 */
	public boolean canPvpOutsideZones(Player player) {
		if (this.isPvpInZonesOnly()) {
			if (War.permissionHandler != null) {
				if (War.permissionHandler.has(player, "war.pvp") || War.permissionHandler.has(player, "War.pvp")) {
					return true;
				} else {
					return false;
				}
			} else {
				// w/o Permissions, if pvpInZoneOnly, no one can pvp outside the zone
				return player.hasPermission("war.pvp");
			}
		} else {
			return true;
		}
	}

	/**
	 * Checks whether the given player is a zone maker
	 *
	 * @param 	player	Player to check
	 * @return		true if the player is a zone maker
	 */
	public boolean isZoneMaker(Player player) {
		// sort out disguised first
		for (String disguised : this.zoneMakersImpersonatingPlayers) {
			if (disguised.equals(player.getName())) {
				return false;
			}
		}

		for (String zoneMaker : this.zoneMakerNames) {
			if (zoneMaker.equals(player.getName())) {
				return true;
			}
		}
		if (War.permissionHandler != null) {
			if (War.permissionHandler.has(player, "war.*") || War.permissionHandler.has(player, "War.*")
					|| War.permissionHandler.has(player, "war.zonemaker") || War.permissionHandler.has(player, "War.zonemaker")) {
				// War admins are zonemakers
				return true;
			} else {
				return false;
			}
		} else {
			// default to op, if no permissions are found
			return player.hasPermission("war.zonemaker");
		}
	}
	
	/**
	 * Checks whether the given player is a War admin
	 *
	 * @param 	player	Player to check
	 * @return		true if the player is a War admin
	 */
	public boolean isWarAdmin(Player player) {
		if (War.permissionHandler != null) {
			if (War.permissionHandler.has(player, "war.*") || War.permissionHandler.has(player, "War.*")) {
				return true;
			} else {
				return false;
			}
		} else {
			// default to op, if no permissions are found
			return player.hasPermission("war.*");
		}
	}

	public void addWandBearer(Player player, String zoneName) {
		if (this.wandBearers.containsKey(player.getName())) {
			String alreadyHaveWand = this.wandBearers.get(player.getName());
			if (player.getInventory().first(Material.WOOD_SWORD) != -1) {
				if (zoneName.equals(alreadyHaveWand)) {
					this.badMsg(player, "You already have a wand for zone " + alreadyHaveWand + ". Drop the wooden sword first.");
				} else {
					// new zone, already have sword
					this.wandBearers.remove(player.getName());
					this.wandBearers.put(player.getName(), zoneName);
					this.msg(player, "Switched wand to zone " + zoneName + ".");
				}
			} else {
				// lost his sword, or new warzone
				if (zoneName.equals(alreadyHaveWand)) {
					// same zone, give him a new sword
					player.getInventory().addItem(new ItemStack(Material.WOOD_SWORD, 1, (byte) 8));
					this.msg(player, "Here's a new sword for zone " + zoneName + ".");
				}
			}
		} else {
			if (player.getInventory().firstEmpty() == -1) {
				this.badMsg(player, "Your inventory is full. Please drop an item and try again.");
			} else {
				this.wandBearers.put(player.getName(), zoneName);
				player.getInventory().addItem(new ItemStack(Material.WOOD_SWORD, 1, (byte) 8));
				// player.getWorld().dropItem(player.getLocation(), new ItemStack(Material.WOOD_SWORD));
				this.msg(player, "You now have a wand for zone " + zoneName + ". Left-click with wodden sword for corner 1. Right-click for corner 2.");
			}
		}
	}

	public boolean isWandBearer(Player player) {
		return this.wandBearers.containsKey(player.getName());
	}

	public String getWandBearerZone(Player player) {
		if (this.isWandBearer(player)) {
			return this.wandBearers.get(player.getName());
		}
		return "";
	}

	public void removeWandBearer(Player player) {
		if (this.wandBearers.containsKey(player.getName())) {
			this.wandBearers.remove(player.getName());
		}
	}

	public HashMap<Integer, ItemStack> getDefaultLoadout() {
		return this.defaultLoadout;
	}

	public Logger getLogger() {
		return this.logger;
	}

	public Warzone zoneOfZoneWallAtProximity(Location location) {
		for (Warzone zone : this.warzones) {
			if (zone.getWorld() == location.getWorld() && zone.isNearWall(location)) {
				return zone;
			}
		}
		return null;
	}

	public List<String> getZoneMakerNames() {
		return this.zoneMakerNames;
	}

	public List<String> getCommandWhitelist() {
		return this.commandWhitelist;
	}

	public boolean inAnyWarzoneLobby(Location location) {
		if (ZoneLobby.getLobbyByLocation(location) == null) {
			return false;
		}
		return true;
	}

	public List<String> getZoneMakersImpersonatingPlayers() {
		return this.zoneMakersImpersonatingPlayers;
	}

	public HashMap<Integer, ItemStack> getDefaultReward() {
		return this.defaultReward;
	}

	public List<Warzone> getIncompleteZones() {
		return this.incompleteZones;
	}

	public WarHub getWarHub() {
		return this.warHub;
	}

	public void setWarHub(WarHub warHub) {
		this.warHub = warHub;
	}

	public boolean isLoaded() {
		return this.loaded;
	}

	public void setLoaded(boolean loaded) {
		this.loaded = loaded;
	}

	public boolean isPvpInZonesOnly() {
		return this.pvpInZonesOnly;
	}

	public void setPvpInZonesOnly(boolean pvpInZonesOnly) {
		this.pvpInZonesOnly = pvpInZonesOnly;
	}

	public boolean isDisablePvpMessage() {
		return this.disablePvpMessage;
	}

	public void setDisablePvpMessage(boolean disablePvpMessage) {
		this.disablePvpMessage = disablePvpMessage;
	}

	public boolean isBuildInZonesOnly() {
		return this.buildInZonesOnly;
	}

	public void setBuildInZonesOnly(boolean buildInZonesOnly) {
		this.buildInZonesOnly = buildInZonesOnly;
	}

	public int getDefaultLifepool() {
		return this.defaultLifepool;
	}

	public void setDefaultLifepool(int defaultLifepool) {
		this.defaultLifepool = defaultLifepool;
	}

	public int getDefaultTeamCap() {
		return this.defaultTeamCap;
	}

	public void setDefaultTeamCap(int defaultTeamCap) {
		this.defaultTeamCap = defaultTeamCap;
	}

	public int getDefaultScoreCap() {
		return this.defaultScoreCap;
	}

	public void setDefaultScoreCap(int defaultScoreCap) {
		this.defaultScoreCap = defaultScoreCap;
	}

	public int getDefaultMonumentHeal() {
		return this.defaultMonumentHeal;
	}

	public void setDefaultMonumentHeal(int defaultMonumentHeal) {
		this.defaultMonumentHeal = defaultMonumentHeal;
	}

	public boolean isDefaultBlockHeads() {
		return this.defaultBlockHeads;
	}

	public void setDefaultBlockHeads(boolean defaultBlockHeads) {
		this.defaultBlockHeads = defaultBlockHeads;
	}

	public boolean isDefaultFriendlyFire() {
		return this.defaultFriendlyFire;
	}

	public void setDefaultFriendlyFire(boolean defaultFriendlyFire) {
		this.defaultFriendlyFire = defaultFriendlyFire;
	}

	public boolean isDefaultAutoAssignOnly() {
		return this.defaultAutoAssignOnly;
	}

	public void setDefaultAutoAssignOnly(boolean defaultAutoAssignOnly) {
		this.defaultAutoAssignOnly = defaultAutoAssignOnly;
	}

	public boolean isDefaultUnbreakableZoneBlocks() {
		return this.defaultUnbreakableZoneBlocks;
	}

	public void setDefaultUnbreakableZoneBlocks(boolean defaultUnbreakableZoneBlocks) {
		this.defaultUnbreakableZoneBlocks = defaultUnbreakableZoneBlocks;
	}

	public boolean isDefaultNoCreatures() {
		return this.defaultNoCreatures;
	}

	public void setDefaultNoCreatures(boolean defaultNoCreatures) {
		this.defaultNoCreatures = defaultNoCreatures;
	}

	public boolean isDefaultResetOnEmpty() {
		return this.defaultResetOnEmpty;
	}

	public void setDefaultResetOnEmpty(boolean defaultResetOnEmpty) {
		this.defaultResetOnEmpty = defaultResetOnEmpty;
	}

	public boolean isDefaultResetOnLoad() {
		return this.defaultResetOnLoad;
	}

	public void setDefaultResetOnLoad(boolean defaultResetOnLoad) {
		this.defaultResetOnLoad = defaultResetOnLoad;
	}

	public boolean isDefaultResetOnUnload() {
		return this.defaultResetOnUnload;
	}

	public void setDefaultResetOnUnload(boolean defaultResetOnUnload) {
		this.defaultResetOnUnload = defaultResetOnUnload;
	}

	public TeamSpawnStyle getDefaultSpawnStyle() {
		return this.defaultSpawnStyle;
	}

	public void setDefaultSpawnStyle(TeamSpawnStyle defaultSpawnStyle) {
		this.defaultSpawnStyle = defaultSpawnStyle;
	}

	public void setDefaultFlagReturn(FlagReturn defaultFlagReturn) {
		this.defaultFlagReturn = defaultFlagReturn;
	}

	public FlagReturn getDefaultFlagReturn() {
		return this.defaultFlagReturn;
	}

	public HashMap<String, PlayerState> getDisconnected() {
		return this.disconnected;
	}

	public void setDisconnected(HashMap<String, PlayerState> disconnected) {
		this.disconnected = disconnected;
	}

	public void setDefaultGlassWalls(boolean defaultGlassWalls) {
		this.defaultGlassWalls = defaultGlassWalls;
	}

	public boolean isDefaultGlassWalls() {
		return this.defaultGlassWalls;
	}
	
	public void setDefaultFlagPointsOnly(boolean defaultFlagPointsOnly) {
		this.defaultFlagPointsOnly = defaultFlagPointsOnly;
	}

	public boolean isDefaultFlagPointsOnly() {
		return this.defaultFlagPointsOnly;
	}

	public void setDefaultMinPlayers(int defaultMinPlayers) {
		this.defaultMinPlayers = defaultMinPlayers;
	}

	public int getDefaultMinPlayers() {
		return defaultMinPlayers;
	}

	public void setDefaultMinTeams(int defaultMinTeams) {
		this.defaultMinTeams = defaultMinTeams;
	}

	public int getDefaultMinTeams() {
		return defaultMinTeams;
	}

	public HashMap<String, HashMap<Integer, ItemStack>> getDefaultExtraLoadouts() {
		return defaultExtraLoadouts;
	}

	public void setDefaultPvpInZone(boolean defaultPvpInZone) {
		this.defaultPvpInZone = defaultPvpInZone;
	}

	public boolean isDefaultPvpInZone() {
		return defaultPvpInZone;
	}

	public void setDefaultInstaBreak(boolean defaultInstaBreak) {
		this.defaultInstaBreak = defaultInstaBreak;
	}

	public boolean isDefaultInstaBreak() {
		return defaultInstaBreak;
	}

	public List<String> getDeadlyAdjectives() {
		return deadlyAdjectives;
	}

	public List<String> getKillerVerbs() {
		return killerVerbs;
	}

	public boolean isTntInZonesOnly() {
		return tntInZonesOnly;
	}

	public void setTntInZonesOnly(boolean tntInZonesOnly) {
		this.tntInZonesOnly = tntInZonesOnly;
	}

	public boolean isDefaultNoDrops() {
		return defaultNoDrops;
	}

	public void setDefaultNoDrops(boolean defaultNoDrops) {
		this.defaultNoDrops = defaultNoDrops;
	}

	public boolean isDefaultNoHunger() {
		return defaultNoHunger;
	}

	public void setDefaultNoHunger(boolean defaultNoHunger) {
		this.defaultNoHunger = defaultNoHunger;
	}
	
	public int getDefaultSaturation() {
		return defaultSaturation;
	}

	public void setDefaultSaturation(int defaultSaturation) {
		this.defaultSaturation = defaultSaturation;
	}

}
