package com.tommytony.war;

import com.tommytony.war.command.WarCommandHandler;
import com.tommytony.war.config.*;
import com.tommytony.war.event.*;
import com.tommytony.war.job.CapturePointTimer;
import com.tommytony.war.job.HelmetProtectionTask;
import com.tommytony.war.job.ScoreboardSwitchTimer;
import com.tommytony.war.job.SpoutFadeOutMessageJob;
import com.tommytony.war.mapper.WarYmlMapper;
import com.tommytony.war.mapper.WarzoneYmlMapper;
import com.tommytony.war.spout.SpoutDisplayer;
import com.tommytony.war.structure.*;
import com.tommytony.war.ui.UIManager;
import com.tommytony.war.utility.Loadout;
import com.tommytony.war.utility.PlayerState;
import com.tommytony.war.utility.SizeCounter;
import com.tommytony.war.utility.WarLogFormatter;
import com.tommytony.war.volume.Volume;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;

/**
 * Main class of War
 *
 * @author tommytony, Tim Düsterhus
 * @package bukkit.tommytony.war
 */
public class War extends JavaPlugin {
	static final boolean HIDE_BLANK_MESSAGES = true;
	public static War war;
	private static ResourceBundle messages = ResourceBundle.getBundle("messages");
	private final List<String> zoneMakerNames = new ArrayList<String>();
	private final List<String> commandWhitelist = new ArrayList<String>();
	private final List<Warzone> incompleteZones = new ArrayList<Warzone>();
	private final List<String> zoneMakersImpersonatingPlayers = new ArrayList<String>();
	private final HashMap<String, String> wandBearers = new HashMap<String, String>(); // playername to zonename
	private final List<String> deadlyAdjectives = new ArrayList<String>();
	private final List<String> killerVerbs = new ArrayList<String>();
	private final InventoryBag defaultInventories = new InventoryBag();
	private final WarConfigBag warConfig = new WarConfigBag();
	private final WarzoneConfigBag warzoneDefaultConfig = new WarzoneConfigBag();
	private final TeamConfigBag teamDefaultConfig = new TeamConfigBag();
	// general
	private WarPlayerListener playerListener = new WarPlayerListener();
	private WarEntityListener entityListener = new WarEntityListener();
	private WarBlockListener blockListener = new WarBlockListener();
	private WarServerListener serverListener = new WarServerListener();
	private WarCommandHandler commandHandler = new WarCommandHandler();
	private PluginDescriptionFile desc = null;
	private boolean loaded = false;
	private boolean isSpoutServer = false;
	private boolean tagServer = false;
	// Zones and hub
	private List<Warzone> warzones = new ArrayList<Warzone>();
	private WarHub warHub;
	private HashMap<String, PlayerState> disconnected = new HashMap<String, PlayerState>();
	private KillstreakReward killstreakReward;
	private MySQLConfig mysqlConfig;
	private Economy econ = null;
	private SpoutDisplayer spoutMessenger = null;
	private HubLobbyMaterials warhubMaterials = new HubLobbyMaterials(
			new ItemStack(Material.GLASS), new ItemStack(Material.WOOD),
			new ItemStack(Material.OBSIDIAN), new ItemStack(Material.GLOWSTONE));
	private UIManager UIManager;

	public War() {
		super();
		War.war = this;
	}

	public static void reloadLanguage() {
		String[] parts = War.war.getWarConfig().getString(WarConfig.LANGUAGE).replace("-", "_").split("_");
		Locale lang = new Locale(parts[0]);
		if (parts.length >= 2) {
			lang = new Locale(parts[0], parts[1]);
		}
		War.messages = ResourceBundle.getBundle("messages", lang);
	}

	/**
	 * @see JavaPlugin#onEnable()
	 * @see War#loadWar()
	 */
	public void onEnable() {
		this.loadWar();
	}

	/**
	 * @see JavaPlugin#onDisable()
	 * @see War#unloadWar()
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

		// Spout server detection
		try {
			Class.forName("org.getspout.spoutapi.player.SpoutPlayer");
			isSpoutServer = true;
			spoutMessenger = new SpoutDisplayer();
		} catch (ClassNotFoundException e) {
			isSpoutServer = false;
		}
		try {
			Class.forName("org.sqlite.JDBC").newInstance();
		} catch (Exception e) {
			this.log("SQLite3 driver not found!", Level.SEVERE);
			this.getServer().getPluginManager().disablePlugin(this);
			return;
		}
		this.UIManager = new UIManager(this);

		// Register events
		PluginManager pm = this.getServer().getPluginManager();
		pm.registerEvents(this.playerListener, this);
		pm.registerEvents(this.entityListener, this);
		pm.registerEvents(this.blockListener, this);
		pm.registerEvents(this.serverListener, this);
		pm.registerEvents(this.UIManager, this);
		if (pm.isPluginEnabled("TagAPI")) {
			try {
				Class.forName("org.kitteh.tag.TagAPI");
				pm.registerEvents(new WarTagListener(), this);
				this.tagServer = true;
			} catch (ClassNotFoundException e) {
				this.tagServer = false;
			}
		}

		// Add defaults
		warConfig.put(WarConfig.BUILDINZONESONLY, false);
		warConfig.put(WarConfig.DISABLEBUILDMESSAGE, false);
		warConfig.put(WarConfig.DISABLEPVPMESSAGE, false);
		warConfig.put(WarConfig.KEEPOLDZONEVERSIONS, true);
		warConfig.put(WarConfig.MAXZONES, 12);
		warConfig.put(WarConfig.PVPINZONESONLY, false);
		warConfig.put(WarConfig.TNTINZONESONLY, false);
		warConfig.put(WarConfig.RESETSPEED, 5000);
		warConfig.put(WarConfig.MAXSIZE, 750);
		warConfig.put(WarConfig.LANGUAGE, Locale.getDefault().toString());
		warConfig.put(WarConfig.AUTOJOIN, "");
		warConfig.put(WarConfig.TPWARMUP, 0);

		warzoneDefaultConfig.put(WarzoneConfig.AUTOASSIGN, false);
		warzoneDefaultConfig.put(WarzoneConfig.BLOCKHEADS, true);
		warzoneDefaultConfig.put(WarzoneConfig.DISABLED, false);
		warzoneDefaultConfig.put(WarzoneConfig.FRIENDLYFIRE, false);
		warzoneDefaultConfig.put(WarzoneConfig.GLASSWALLS, true);
		warzoneDefaultConfig.put(WarzoneConfig.INSTABREAK, false);
		warzoneDefaultConfig.put(WarzoneConfig.MINPLAYERS, 1);
		warzoneDefaultConfig.put(WarzoneConfig.MINTEAMS, 1);
		warzoneDefaultConfig.put(WarzoneConfig.MONUMENTHEAL, 5);
		warzoneDefaultConfig.put(WarzoneConfig.NOCREATURES, false);
		warzoneDefaultConfig.put(WarzoneConfig.NODROPS, false);
		warzoneDefaultConfig.put(WarzoneConfig.PVPINZONE, true);
		warzoneDefaultConfig.put(WarzoneConfig.REALDEATHS, false);
		warzoneDefaultConfig.put(WarzoneConfig.RESETONEMPTY, false);
		warzoneDefaultConfig.put(WarzoneConfig.RESETONCONFIGCHANGE, false);
		warzoneDefaultConfig.put(WarzoneConfig.RESETONLOAD, false);
		warzoneDefaultConfig.put(WarzoneConfig.RESETONUNLOAD, false);
		warzoneDefaultConfig.put(WarzoneConfig.UNBREAKABLE, false);
		warzoneDefaultConfig.put(WarzoneConfig.DEATHMESSAGES, true);
		warzoneDefaultConfig.put(WarzoneConfig.JOINMIDBATTLE, true);
		warzoneDefaultConfig.put(WarzoneConfig.AUTOJOIN, false);
		warzoneDefaultConfig.put(WarzoneConfig.SCOREBOARD, ScoreboardType.NONE);
		warzoneDefaultConfig.put(WarzoneConfig.SOUPHEALING, false);
		warzoneDefaultConfig.put(WarzoneConfig.ALLOWENDER, true);
		warzoneDefaultConfig.put(WarzoneConfig.RESETBLOCKS, true);
		warzoneDefaultConfig.put(WarzoneConfig.CAPTUREPOINTTIME, 15);
		warzoneDefaultConfig.put(WarzoneConfig.PREPTIME, 0);

		teamDefaultConfig.put(TeamConfig.FLAGMUSTBEHOME, true);
		teamDefaultConfig.put(TeamConfig.FLAGPOINTSONLY, false);
		teamDefaultConfig.put(TeamConfig.FLAGRETURN, FlagReturn.BOTH);
		teamDefaultConfig.put(TeamConfig.LIFEPOOL, 7);
		teamDefaultConfig.put(TeamConfig.MAXSCORE, 10);
		teamDefaultConfig.put(TeamConfig.NOHUNGER, false);
		teamDefaultConfig.put(TeamConfig.PLAYERLOADOUTASDEFAULT, false);
		teamDefaultConfig.put(TeamConfig.RESPAWNTIMER, 0);
		teamDefaultConfig.put(TeamConfig.SATURATION, 10);
		teamDefaultConfig.put(TeamConfig.SPAWNSTYLE, TeamSpawnStyle.SMALL);
		teamDefaultConfig.put(TeamConfig.TEAMSIZE, 10);
		teamDefaultConfig.put(TeamConfig.PERMISSION, "war.player");
		teamDefaultConfig.put(TeamConfig.XPKILLMETER, false);
		teamDefaultConfig.put(TeamConfig.KILLSTREAK, false);
		teamDefaultConfig.put(TeamConfig.BLOCKWHITELIST, "all");
		teamDefaultConfig.put(TeamConfig.PLACEBLOCK, true);
		teamDefaultConfig.put(TeamConfig.APPLYPOTION, "");
		teamDefaultConfig.put(TeamConfig.ECOREWARD, 0.0);
		teamDefaultConfig.put(TeamConfig.INVENTORYDROP, false);
		teamDefaultConfig.put(TeamConfig.BORDERDROP, false);

		this.getDefaultInventories().clearLoadouts();
		HashMap<Integer, ItemStack> defaultLoadout = new HashMap<Integer, ItemStack>();

		ItemStack stoneSword = new ItemStack(Material.STONE_SWORD, 1, (byte) 8);
		stoneSword.setDurability((short) 8);
		defaultLoadout.put(0, stoneSword);

		ItemStack bow = new ItemStack(Material.BOW, 1, (byte) 8);
		bow.setDurability((short) 8);
		defaultLoadout.put(1, bow);

		ItemStack arrows = new ItemStack(Material.ARROW, 7);
		defaultLoadout.put(2, arrows);

		ItemStack stonePick = new ItemStack(Material.IRON_PICKAXE, 1, (byte) 8);
		stonePick.setDurability((short) 8);
		defaultLoadout.put(3, stonePick);

		ItemStack stoneSpade = new ItemStack(Material.STONE_SPADE, 1, (byte) 8);
		stoneSword.setDurability((short) 8);
		defaultLoadout.put(4, stoneSpade);

		this.getDefaultInventories().addLoadout("default", defaultLoadout);

		HashMap<Integer, ItemStack> reward = new HashMap<Integer, ItemStack>();
		reward.put(0, new ItemStack(Material.CAKE, 1));
		this.getDefaultInventories().setReward(reward);

		this.getCommandWhitelist().add("who");
		this.getZoneMakerNames().add("tommytony");
		this.setKillstreakReward(new KillstreakReward());
		this.setMysqlConfig(new MySQLConfig());

		// Add constants
		this.getDeadlyAdjectives().clear();
		for (String adjective : this.getString("pvp.kill.adjectives").split(";")) {
			this.getDeadlyAdjectives().add(adjective);
		}
		this.getKillerVerbs().clear();
		for (String verb : this.getString("pvp.kill.verbs").split(";")) {
			this.getKillerVerbs().add(verb);
		}

		// Load files
		WarYmlMapper.load();

		// Start tasks
		HelmetProtectionTask helmetProtectionTask = new HelmetProtectionTask();
		this.getServer().getScheduler().scheduleSyncRepeatingTask(this, helmetProtectionTask, 250, 100);

		CapturePointTimer cpt = new CapturePointTimer();
		cpt.runTaskTimer(this, 100, 20);
		ScoreboardSwitchTimer sst = new ScoreboardSwitchTimer();
		sst.runTaskTimer(this, 500, 20 * 60);

		if (this.isSpoutServer) {
			SpoutFadeOutMessageJob fadeOutMessagesTask = new SpoutFadeOutMessageJob();
			this.getServer().getScheduler().scheduleSyncRepeatingTask(this, fadeOutMessagesTask, 100, 100);
		}
		if (this.mysqlConfig.isEnabled()) {
			try {
				Class.forName("com.mysql.jdbc.Driver").newInstance();
			} catch (Exception ex) {
				this.log("MySQL driver not found!", Level.SEVERE);
				this.getServer().getPluginManager().disablePlugin(this);
			}
		}
		if (this.getServer().getPluginManager().isPluginEnabled("Vault")) {
			RegisteredServiceProvider<Economy> rsp = this.getServer().getServicesManager()
				.getRegistration(Economy.class);
			if (rsp != null) {
				this.econ = rsp.getProvider();
			}
		}

		War.reloadLanguage();

		// Get own log file
		try {
			// Create an appending file handler
			new File(this.getDataFolder() + "/temp/").mkdir();
			FileHandler handler = new FileHandler(this.getDataFolder() + "/temp/war.log", true);

			// Add to War-specific logger
			Formatter formatter = new WarLogFormatter();
			handler.setFormatter(formatter);
			this.getLogger().addHandler(handler);
		} catch (IOException e) {
			this.getLogger().log(Level.WARNING, "Failed to create War log file");
		}

		// Size check
		long datSize = SizeCounter.getFileOrDirectorySize(new File(this.getDataFolder() + "/dat/")) / 1024 / 1024;
		long tempSize = SizeCounter.getFileOrDirectorySize(new File(this.getDataFolder() + "/temp/")) / 1024 / 1024;

		if (datSize + tempSize > 100) {
			this.log("War data files are taking " + datSize + "MB and its temp files " + tempSize + "MB. Consider permanently deleting old warzone versions and backups in /plugins/War/temp/.", Level.WARNING);
		}

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

		HandlerList.unregisterAll(this);
		this.log("War v" + this.desc.getVersion() + " is off.", Level.INFO);
		this.setLoaded(false);
	}

	/**
	 * @see org.bukkit.command.CommandExecutor#onCommand(org.bukkit.command.CommandSender, org.bukkit.command.Command, String, String[])
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
		for (ItemStack stack : inv.getStorageContents()) {
			if (stack != null && stack.getType() != Material.AIR) {
				loadout.put(i, stack.clone());
				i++;
			}
		}
		if (inv.getBoots() != null && inv.getBoots().getType() != Material.AIR) {
			loadout.put(100, inv.getBoots().clone());
		}
		if (inv.getLeggings() != null && inv.getLeggings().getType() != Material.AIR) {
			loadout.put(101, inv.getLeggings().clone());
		}
		if (inv.getChestplate() != null && inv.getChestplate().getType() != Material.AIR) {
			loadout.put(102, inv.getChestplate().clone());
		}
		if (inv.getHelmet() != null && inv.getHelmet().getType() != Material.AIR) {
			loadout.put(103, inv.getHelmet().clone());
		}
	}

	public void safelyEnchant(ItemStack target, Enchantment enchantment, int level) {
		if (level > enchantment.getMaxLevel()) {
			target.addUnsafeEnchantment(enchantment, level);
		} else {
			target.addEnchantment(enchantment, level);
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

	public String updateTeamFromNamedParams(Team team, CommandSender commandSender, String[] arguments) {
		try {
			Map<String, String> namedParams = new HashMap<String, String>();
			Map<String, String> thirdParameter = new HashMap<String, String>();
			for (String namedPair : arguments) {
				String[] pairSplit = namedPair.split(":");
				if (pairSplit.length == 2) {
					namedParams.put(pairSplit[0].toLowerCase(), pairSplit[1]);
				} else if (pairSplit.length == 3) {
					namedParams.put(pairSplit[0].toLowerCase(), pairSplit[1]);
					thirdParameter.put(pairSplit[0].toLowerCase(), pairSplit[2]);
				}
			}

			StringBuilder returnMessage = new StringBuilder();
			returnMessage.append(team.getTeamConfig().updateFromNamedParams(namedParams));

			if (commandSender instanceof Player) {
				Player player = (Player) commandSender;
				if (namedParams.containsKey("loadout")) {
					String loadoutName = namedParams.get("loadout");
					HashMap<Integer, ItemStack> loadout = team.getInventories().getLoadout(loadoutName);
					if (loadout == null) {
						// Check if any loadouts exist, if not gotta use the default inventories then add the newly created one
						if(!team.getInventories().hasLoadouts()) {
							Warzone warzone = team.getZone();
							for (String key : warzone.getDefaultInventories().resolveLoadouts().keySet()) {
								HashMap<Integer, ItemStack> transferredLoadout = warzone.getDefaultInventories().resolveLoadouts().get(key);
								if (transferredLoadout != null) {
									team.getInventories().setLoadout(key, transferredLoadout);
								} else {
									War.war.log("Failed to transfer loadout " + key + " down to team " + team.getName() + " in warzone " + warzone.getName(), Level.WARNING);
								}
							}
						}

						loadout = new HashMap<Integer, ItemStack>();
						team.getInventories().setLoadout(loadoutName, loadout);
						returnMessage.append(loadoutName + " respawn loadout added.");
					} else {
						returnMessage.append(loadoutName + " respawn loadout updated.");
					}
					this.inventoryToLoadout(player, loadout);
					Loadout ldt = team.getInventories().getNewLoadout(loadoutName);
					if (thirdParameter.containsKey("loadout")) {
						String permission = thirdParameter.get("loadout");
						ldt.setPermission(permission);
						returnMessage.append(' ').append(loadoutName).append(" respawn loadout permission set to ").append(permission).append('.');
					} else if (ldt.requiresPermission()) {
						ldt.setPermission(null);
						returnMessage.append(' ').append(loadoutName).append(" respawn loadout permission deleted.");
					}
				}
				if (namedParams.containsKey("deleteloadout")) {
					String loadoutName = namedParams.get("deleteloadout");
					if (team.getInventories().containsLoadout(loadoutName)) {
						team.getInventories().removeLoadout(loadoutName);
						returnMessage.append(" " + loadoutName + " loadout removed.");
					} else {
						returnMessage.append(" " + loadoutName + " loadout not found.");
					}
				}
				if (namedParams.containsKey("reward")) {
					HashMap<Integer, ItemStack> reward = new HashMap<Integer, ItemStack>();
					this.inventoryToLoadout(player, reward);
					team.getInventories().setReward(reward);
					returnMessage.append(" game end reward updated.");
				}
			}

			return returnMessage.toString();
		} catch (Exception e) {
			return "PARSE-ERROR";
		}
	}

	public String updateZoneFromNamedParams(Warzone warzone, CommandSender commandSender, String[] arguments) {
		try {
			Map<String, String> namedParams = new HashMap<String, String>();
			Map<String, String> thirdParameter = new HashMap<String, String>();
			for (String namedPair : arguments) {
				String[] pairSplit = namedPair.split(":");
				if (pairSplit.length == 2) {
					namedParams.put(pairSplit[0].toLowerCase(), pairSplit[1]);
				} else if (pairSplit.length == 3) {
					namedParams.put(pairSplit[0].toLowerCase(), pairSplit[1]);
					thirdParameter.put(pairSplit[0].toLowerCase(), pairSplit[2]);
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

			returnMessage.append(warzone.getWarzoneConfig().updateFromNamedParams(namedParams));
			returnMessage.append(warzone.getTeamDefaultConfig().updateFromNamedParams(namedParams));

			if (commandSender instanceof Player) {
				Player player = (Player) commandSender;
				if (namedParams.containsKey("loadout")) {
					String loadoutName = namedParams.get("loadout");
					HashMap<Integer, ItemStack> loadout = warzone.getDefaultInventories().getLoadout(loadoutName);
					if (loadout == null) {
						loadout = new HashMap<Integer, ItemStack>();

						// Check if any loadouts exist, if not gotta use the default inventories then add the newly created one
						if(!warzone.getDefaultInventories().hasLoadouts()) {
							for (String key : warzone.getDefaultInventories().resolveLoadouts().keySet()) {
								HashMap<Integer, ItemStack> transferredLoadout = warzone.getDefaultInventories().resolveLoadouts().get(key);
								if (transferredLoadout != null) {
									warzone.getDefaultInventories().setLoadout(key, transferredLoadout);
								} else {
									War.war.log("Failed to transfer loadout " + key + " down to warzone " + warzone.getName(), Level.WARNING);
								}
							}
						}

						warzone.getDefaultInventories().setLoadout(loadoutName, loadout);
						returnMessage.append(loadoutName).append(" respawn loadout added.");
					} else {
						returnMessage.append(loadoutName).append(" respawn loadout updated.");
					}
					this.inventoryToLoadout(player, loadout);
					Loadout ldt = warzone.getDefaultInventories().getNewLoadout(loadoutName);
					if (thirdParameter.containsKey("loadout")) {
						String permission = thirdParameter.get("loadout");
						ldt.setPermission(permission);
						returnMessage.append(' ').append(loadoutName).append(" respawn loadout permission set to ").append(permission).append('.');
					} else if (ldt.requiresPermission()) {
						ldt.setPermission(null);
						returnMessage.append(' ').append(loadoutName).append(" respawn loadout permission deleted.");
					}
				}
				if (namedParams.containsKey("deleteloadout")) {
					String loadoutName = namedParams.get("deleteloadout");
					if (warzone.getDefaultInventories().containsLoadout(loadoutName)) {
						warzone.getDefaultInventories().removeLoadout(loadoutName);
						returnMessage.append(" " + loadoutName + " loadout removed.");
					} else {
						returnMessage.append(" " + loadoutName + " loadout not found.");
					}
				}
				if (namedParams.containsKey("reward")) {
					HashMap<Integer, ItemStack> reward = new HashMap<Integer, ItemStack>();
					this.inventoryToLoadout(player, reward);
					warzone.getDefaultInventories().setReward(reward);
					returnMessage.append(" game end reward updated.");
				}
				if (namedParams.containsKey("lobbymaterial")) {
					String whichBlocks = namedParams.get("lobbymaterial");
					ItemStack blockInHand = player.getInventory().getItemInMainHand();
					boolean updatedLobbyMaterials = false;

					if (!blockInHand.getType().isBlock() && !blockInHand.getType().equals(Material.AIR)) {
						this.badMsg(player, "Can only use blocks or air as lobby material.");
					} else {
						if (whichBlocks.equals("floor")) {
							warzone.getLobbyMaterials().setFloorBlock(blockInHand);
							returnMessage.append(" lobby floor material set to ").append(blockInHand.getType());
							updatedLobbyMaterials = true;
						} else if (whichBlocks.equals("outline")) {
							warzone.getLobbyMaterials().setOutlineBlock(blockInHand);
							returnMessage.append(" lobby outline material set to ").append(blockInHand.getType());
							updatedLobbyMaterials = true;
						} else if (whichBlocks.equals("gate")) {
							warzone.getLobbyMaterials().setGateBlock(blockInHand);
							returnMessage.append(" lobby gate material set to ").append(blockInHand.getType());
							updatedLobbyMaterials = true;
						} else if (whichBlocks.equals("light")) {
							warzone.getLobbyMaterials().setLightBlock(blockInHand);
							returnMessage.append(" lobby light material set to ").append(blockInHand.getType());
							updatedLobbyMaterials = true;
						}

						if (updatedLobbyMaterials && warzone.getLobby() != null) {
							warzone.getLobby().getVolume().resetBlocks();
							warzone.getLobby().initialize();
						}
					}
				}
				if (namedParams.containsKey("material")) {
					String whichBlocks = namedParams.get("material");
					ItemStack blockInHand = player.getInventory().getItemInMainHand();
					boolean updatedMaterials = false;

					if (!blockInHand.getType().isBlock()) {
						this.badMsg(player, "Can only use blocks as material.");
					} else {
						if (whichBlocks.equals("main")) {
							warzone.getWarzoneMaterials().setMainBlock(blockInHand);
							returnMessage.append(" main material set to ").append(blockInHand.getType());
							updatedMaterials = true;
						} else if (whichBlocks.equals("stand")) {
							warzone.getWarzoneMaterials().setStandBlock(blockInHand);
							returnMessage.append(" stand material set to ").append(blockInHand.getType());
							updatedMaterials = true;
						} else if (whichBlocks.equals("light")) {
							warzone.getWarzoneMaterials().setLightBlock(blockInHand);
							returnMessage.append(" light material set to ").append(blockInHand.getType());
							updatedMaterials = true;
						}

						if (updatedMaterials) {
							// reset all structures
							for (Monument monument : warzone.getMonuments()) {
								monument.getVolume().resetBlocks();
								monument.addMonumentBlocks();
							}
							for (Cake cake : warzone.getCakes()) {
								cake.getVolume().resetBlocks();
								cake.addCakeBlocks();
							}
							for (Bomb bomb : warzone.getBombs()) {
								bomb.getVolume().resetBlocks();
								bomb.addBombBlocks();
							}
							for (Team team : warzone.getTeams()) {
								for (Volume spawnVolume : team.getSpawnVolumes().values()) {
									spawnVolume.resetBlocks();
								}
								team.initializeTeamSpawns();
								if (team.getTeamFlag() != null) {
									team.getFlagVolume().resetBlocks();
									team.initializeTeamFlag();
								}
							}
						}
					}
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
			Map<String, String> thirdParameter = new HashMap<String, String>();
			for (String namedPair : arguments) {
				String[] pairSplit = namedPair.split(":");
				if (pairSplit.length == 2) {
					namedParams.put(pairSplit[0].toLowerCase(), pairSplit[1]);
				} else if (pairSplit.length == 3) {
					namedParams.put(pairSplit[0].toLowerCase(), pairSplit[1]);
					thirdParameter.put(pairSplit[0].toLowerCase(), pairSplit[2]);
				}
			}

			StringBuilder returnMessage = new StringBuilder();

			returnMessage.append(this.getWarConfig().updateFromNamedParams(namedParams));
			returnMessage.append(this.getWarzoneDefaultConfig().updateFromNamedParams(namedParams));
			returnMessage.append(this.getTeamDefaultConfig().updateFromNamedParams(namedParams));

			if (commandSender instanceof Player) {
				Player player = (Player) commandSender;
				if (namedParams.containsKey("loadout")) {
					String loadoutName = namedParams.get("loadout");
					HashMap<Integer, ItemStack> loadout = this.getDefaultInventories().getLoadout(loadoutName);
					if (loadout == null) {
						loadout = new HashMap<Integer, ItemStack>();
						this.getDefaultInventories().addLoadout(loadoutName, loadout);
						returnMessage.append(loadoutName + " respawn loadout added.");
					} else {
						returnMessage.append(loadoutName + " respawn loadout updated.");
					}
					this.inventoryToLoadout(player, loadout);
					Loadout ldt = this.getDefaultInventories().getNewLoadout(loadoutName);
					if (thirdParameter.containsKey("loadout")) {
						String permission = thirdParameter.get("loadout");
						ldt.setPermission(permission);
						returnMessage.append(' ').append(loadoutName).append(" respawn loadout permission set to ").append(permission).append('.');
					} else if (ldt.requiresPermission()) {
						ldt.setPermission(null);
						returnMessage.append(' ').append(loadoutName).append(" respawn loadout permission deleted.");
					}
				}
				if (namedParams.containsKey("deleteloadout")) {
					String loadoutName = namedParams.get("deleteloadout");
					if (this.getDefaultInventories().containsLoadout(loadoutName)) {
						if (this.getDefaultInventories().getNewLoadouts().size() > 1) {
							this.getDefaultInventories().removeLoadout(loadoutName);
							returnMessage.append(" " + loadoutName + " loadout removed.");
						} else {
							returnMessage.append(" Can't remove only loadout.");
						}
					} else {
						returnMessage.append(" " + loadoutName + " loadout not found.");
					}
				}
				if (namedParams.containsKey("reward")) {
					HashMap<Integer, ItemStack> reward = new HashMap<Integer, ItemStack>();
					this.inventoryToLoadout(player, reward);
					this.getDefaultInventories().setReward(reward);
					returnMessage.append(" game end reward updated.");
				}
				if (namedParams.containsKey("rallypoint")) {
					String zoneName = namedParams.get("rallypoint");
					this.setZoneRallyPoint(zoneName, player);
					returnMessage.append(" rallypoint set for zone " + zoneName + ".");
				}
				if (namedParams.containsKey("warhubmaterial")) {
					String whichBlocks = namedParams.get("warhubmaterial");
					ItemStack blockInHand = player.getInventory().getItemInMainHand();
					boolean updatedWarhubMaterials = false;

					if (!blockInHand.getType().isBlock() && !blockInHand.getType().equals(Material.AIR)) {
						this.badMsg(player, "Can only use blocks or air as warhub material.");
					} else {
						if (whichBlocks.equals("floor")) {
							this.warhubMaterials.setFloorBlock(blockInHand);
							returnMessage.append(" warhub floor material set to " + blockInHand.getType());
							updatedWarhubMaterials = true;
						} else if (whichBlocks.equals("outline")) {
							this.warhubMaterials.setOutlineBlock(blockInHand);
							returnMessage.append(" warhub outline material set to " + blockInHand.getType());
							updatedWarhubMaterials = true;
						} else if (whichBlocks.equals("gate")) {
							this.warhubMaterials.setGateBlock(blockInHand);
							returnMessage.append(" warhub gate material set to " + blockInHand.getType());
							updatedWarhubMaterials = true;
						} else if (whichBlocks.equals("light")) {
							this.warhubMaterials.setLightBlock(blockInHand);
							returnMessage.append(" warhub light material set to " + blockInHand.getType());
							updatedWarhubMaterials = true;
						}

						if (updatedWarhubMaterials && War.war.getWarHub() != null) {
							War.war.getWarHub().getVolume().resetBlocks();
							War.war.getWarHub().initialize();
						}
					}
				}
			}

			return returnMessage.toString();
		} catch (Exception e) {
			return "PARSE-ERROR";
		}
	}

	public String printConfig(Team team) {
		ChatColor teamColor = ChatColor.AQUA;

		ChatColor normalColor = ChatColor.WHITE;

		String teamConfigStr = "";
		InventoryBag invs = team.getInventories();
		teamConfigStr += getLoadoutsString(invs);

		for (TeamConfig teamConfig : TeamConfig.values()) {
			Object value = team.getTeamConfig().getValue(teamConfig);
			if (value != null) {
				teamConfigStr += " " + teamConfig.toStringWithValue(value).replace(":", ":" + teamColor) + normalColor;
			}
		}

		return " ::" + teamColor + "Team " + team.getName() + teamColor + " config" + normalColor + "::"
			+ ifEmptyInheritedForTeam(teamConfigStr);
	}

	private String getLoadoutsString(InventoryBag invs) {
		StringBuilder loadoutsString = new StringBuilder();
		ChatColor loadoutColor = ChatColor.GREEN;
		ChatColor normalColor = ChatColor.WHITE;

		if (invs.hasLoadouts()) {
			StringBuilder loadouts = new StringBuilder();
			for (Loadout ldt : invs.getNewLoadouts()) {
				if (ldt.requiresPermission()) {
					loadouts.append(ldt.getName()).append(":").append(ldt.getPermission()).append(",");
				} else {
					loadouts.append(ldt.getName()).append(",");
				}
			}
			loadoutsString.append(" loadout:").append(loadoutColor).append(loadouts.toString()).append(normalColor);
		}

		if (invs.hasReward()) {
			loadoutsString.append(" reward:").append(loadoutColor).append("default").append(normalColor);
		}

		return loadoutsString.toString();
	}

	public String printConfig(Warzone zone) {
		ChatColor teamColor = ChatColor.AQUA;
		ChatColor zoneColor = ChatColor.DARK_AQUA;
		ChatColor authorColor = ChatColor.GREEN;
		ChatColor normalColor = ChatColor.WHITE;

		String warzoneConfigStr = "";
		for (WarzoneConfig warzoneConfig : WarzoneConfig.values()) {
			Object value = zone.getWarzoneConfig().getValue(warzoneConfig);
			if (value != null) {
				warzoneConfigStr += " " + warzoneConfig.toStringWithValue(value).replace(":", ":" + zoneColor) + normalColor;
			}
		}

		String teamDefaultsStr = "";
		teamDefaultsStr += getLoadoutsString( zone.getDefaultInventories());
		for (TeamConfig teamConfig : TeamConfig.values()) {
			Object value = zone.getTeamDefaultConfig().getValue(teamConfig);
			if (value != null) {
				teamDefaultsStr += " " + teamConfig.toStringWithValue(value).replace(":", ":" + teamColor) + normalColor;
			}
		}

		return "::" + zoneColor + "Warzone " + authorColor + zone.getName() + zoneColor + " config" + normalColor + "::"
		 + " author:" + authorColor + ifEmptyEveryone(zone.getAuthorsString()) + normalColor
		 + ifEmptyInheritedForWarzone(warzoneConfigStr)
		 + " ::" + teamColor + "Team defaults" + normalColor + "::"
		 + ifEmptyInheritedForWarzone(teamDefaultsStr);
	}
	
	private String ifEmptyInheritedForWarzone(String maybeEmpty) {
		if (maybeEmpty.equals("")) {
			maybeEmpty = " all values inherited (see " + ChatColor.GREEN + "/warcfg -p)" + ChatColor.WHITE;
		}
		return maybeEmpty;
	}

	private String ifEmptyInheritedForTeam(String maybeEmpty) {
		if (maybeEmpty.equals("")) {
			maybeEmpty = " all values inherited (see " + ChatColor.GREEN + "/warcfg -p" + ChatColor.WHITE
				+ " and " + ChatColor.GREEN + "/zonecfg -p" + ChatColor.WHITE + ")";
		}
		return maybeEmpty;
	}

	private String ifEmptyEveryone(String maybeEmpty) {
		if (maybeEmpty.equals("")) {
			maybeEmpty = "*";
		}
		return maybeEmpty;
	}

	public String printConfig() {
		ChatColor teamColor = ChatColor.AQUA;
		ChatColor zoneColor = ChatColor.DARK_AQUA;
		ChatColor globalColor = ChatColor.DARK_GREEN;
		ChatColor normalColor = ChatColor.WHITE;

		String warConfigStr = "";
		for (WarConfig warConfig : WarConfig.values()) {
			warConfigStr += " " + warConfig.toStringWithValue(this.getWarConfig().getValue(warConfig)).replace(":", ":" + globalColor) + normalColor;
		}

		String warzoneDefaultsStr = "";
		for (WarzoneConfig warzoneConfig : WarzoneConfig.values()) {
			warzoneDefaultsStr += " " + warzoneConfig.toStringWithValue(this.getWarzoneDefaultConfig().getValue(warzoneConfig)).replace(":", ":" + zoneColor) + normalColor;
		}

		String teamDefaultsStr = "";
		teamDefaultsStr += getLoadoutsString(this.getDefaultInventories());
		for (TeamConfig teamConfig : TeamConfig.values()) {
			teamDefaultsStr += " " + teamConfig.toStringWithValue(this.getTeamDefaultConfig().getValue(teamConfig)).replace(":", ":" + teamColor) + normalColor;
		}

		return normalColor + "::" + globalColor + "War config" + normalColor + "::" + warConfigStr
			+ normalColor + " ::" + zoneColor + "Warzone defaults" + normalColor + "::" + warzoneDefaultsStr
				+ normalColor + " ::" + teamColor + "Team defaults" + normalColor + "::" + teamDefaultsStr;
	}

	private void setZoneRallyPoint(String warzoneName, Player player) {
		Warzone zone = this.findWarzone(warzoneName);
		if (zone == null) {
			this.badMsg(player, "Can't set rally point. No such warzone.");
		} else {
			zone.setRallyPoint(player.getLocation());
			WarzoneYmlMapper.save(zone);
		}
	}

	public void addWarzone(Warzone zone) {
		this.warzones.add(zone);
	}

	public List<Warzone> getWarzones() {
		return this.warzones;
	}

	/**
	 * Get a list of warzones that are not disabled.
	 * @return List of enabled warzones.
	 */
	public List<Warzone> getEnabledWarzones() {
		List<Warzone> enabledZones = new ArrayList<Warzone>(this.warzones.size());
		for (Warzone zone : this.warzones) {
			if (zone.getWarzoneConfig().getBoolean(WarzoneConfig.DISABLED) == false) {
				enabledZones.add(zone);
			}
		}
		return enabledZones;
	}

	/**
	 * Get a list of warzones that have players in them.
	 * @return List of enabled warzones with players.
	 */
	public List<Warzone> getActiveWarzones() {
		List<Warzone> activeZones = new ArrayList<Warzone>(this.warzones.size());
		for (Warzone zone : this.warzones) {
			if (zone.getWarzoneConfig().getBoolean(WarzoneConfig.DISABLED) == false
				&& zone.getPlayerCount() > 0) {
				activeZones.add(zone);
			}
		}
		return activeZones;
	}

	public void msg(CommandSender sender, String str) {
		if (messages.containsKey(str)) str = this.getString(str);
		if (HIDE_BLANK_MESSAGES && (str == null || str.isEmpty())) return;
		if (sender instanceof Player) {
			StringBuilder output = new StringBuilder(ChatColor.GRAY.toString())
					.append(this.getString("war.prefix")).append(ChatColor.WHITE).append(' ');
			output.append(this.colorKnownTokens(str, ChatColor.WHITE));
			sender.sendMessage(output.toString());
		} else {
			sender.sendMessage(str);
		}
	}

	public void badMsg(CommandSender sender, String str) {
		if (messages.containsKey(str)) str = this.getString(str);
		if (HIDE_BLANK_MESSAGES && (str == null || str.isEmpty())) return;
		if (sender instanceof Player) {
			StringBuilder output = new StringBuilder(ChatColor.GRAY.toString())
					.append(this.getString("war.prefix")).append(ChatColor.RED).append(' ');
			output.append(this.colorKnownTokens(str, ChatColor.RED));
			sender.sendMessage(output.toString());
		} else {
			sender.sendMessage(str);
		}
	}

	public void msg(CommandSender sender, String str, Object... obj) {
		if (messages.containsKey(str)) str = this.getString(str);
		if (HIDE_BLANK_MESSAGES && (str == null || str.isEmpty())) return;
		if (sender instanceof Player) {
			StringBuilder output = new StringBuilder(ChatColor.GRAY.toString())
					.append(this.getString("war.prefix")).append(ChatColor.WHITE).append(' ');
			output.append(MessageFormat.format(
						this.colorKnownTokens(str, ChatColor.WHITE), obj));
			sender.sendMessage(output.toString());
		} else {
			StringBuilder output = new StringBuilder();
			output.append(MessageFormat.format(str, obj));
			sender.sendMessage(output.toString());
		}
	}

	public void badMsg(CommandSender sender, String str, Object... obj) {
		if (messages.containsKey(str)) str = this.getString(str);
		if (HIDE_BLANK_MESSAGES && (str == null || str.isEmpty())) return;
		if (sender instanceof Player) {
			StringBuilder output = new StringBuilder(ChatColor.GRAY.toString())
					.append(this.getString("war.prefix")).append(ChatColor.RED).append(' ');
			output.append(MessageFormat.format(
						this.colorKnownTokens(str, ChatColor.RED), obj));
			sender.sendMessage(output.toString());
		} else {
			StringBuilder output = new StringBuilder();
			output.append(MessageFormat.format(str, obj));
			sender.sendMessage(output.toString());
		}
	}

	/**
	 * Colors the teams and examples in messages
	 *
	 * @param str message-string
	 * @param msgColor current message-color
	 * @return String Message with colored teams
	 */
	private String colorKnownTokens(String str, ChatColor msgColor) {
		str = str.replaceAll("Ex -", ChatColor.BLUE + "Ex -" + ChatColor.GRAY);
		str = str.replaceAll("\\\\", ChatColor.BLUE + "\\\\" + ChatColor.GRAY);
		str = str.replaceAll("->", ChatColor.LIGHT_PURPLE + "->" + ChatColor.GRAY);
		str = str.replaceAll("/teamcfg", ChatColor.AQUA + "/teamcfg" + ChatColor.GRAY);
		str = str.replaceAll("Team defaults", ChatColor.AQUA + "Team defaults" + ChatColor.GRAY);
		str = str.replaceAll("Team config", ChatColor.AQUA + "Team config" + ChatColor.GRAY);
		str = str.replaceAll("/zonecfg", ChatColor.DARK_AQUA + "/zonecfg" + ChatColor.GRAY);
		str = str.replaceAll("Warzone defaults", ChatColor.DARK_AQUA + "Warzone defaults" + ChatColor.GRAY);
		str = str.replaceAll("Warzone config", ChatColor.DARK_AQUA + "Warzone config" + ChatColor.GRAY);
		str = str.replaceAll("/warcfg", ChatColor.DARK_GREEN + "/warcfg" + ChatColor.GRAY);
		str = str.replaceAll("War config", ChatColor.DARK_GREEN + "War config" + ChatColor.GRAY);
		str = str.replaceAll("Print config", ChatColor.WHITE + "Print config" + ChatColor.GREEN);
		
		for (TeamKind kind : TeamKind.values()) {
			str = str.replaceAll(" " + kind.toString(), " " + kind.getColor() + kind.toString() + msgColor);
			str = str.replaceAll(kind.toString() + "/", kind.getColor() + kind.toString() + ChatColor.GRAY + "/");
		}
		
		return str;
	}

	/**
	 * Logs a specified message with a specified level
	 *
	 * @param str message to log
	 * @param lvl level to use
	 */
	public void log(String str, Level lvl) {
		this.getLogger().log(lvl, str);
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
	 * Checks whether the given player is allowed to play in a certain team
	 *
	 * @param 	player	Player to check
         * @param         team  Team to check  
	 * @return		true if the player may play in the team
	 */
	public boolean canPlayWar(Player player, Team team) {
		return player.hasPermission(team.getTeamConfig().resolveString(TeamConfig.PERMISSION));
	}

	/**
	 * Checks whether the given player is allowed to warp.
	 *
	 * @param 	player	Player to check
	 * @return		true if the player may warp
	 */
	public boolean canWarp(Player player) {
		return player.hasPermission("war.warp");
	}

	/**
	 * Checks whether the given player is allowed to build outside zones
	 *
	 * @param 	player	Player to check
	 * @return		true if the player may build outside zones
	 */
	public boolean canBuildOutsideZone(Player player) {
		if (this.getWarConfig().getBoolean(WarConfig.BUILDINZONESONLY)) {
			return player.hasPermission("war.build");
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
		if (this.getWarConfig().getBoolean(WarConfig.PVPINZONESONLY)) {
			return player.hasPermission("war.pvp");
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
		
		return player.hasPermission("war.zonemaker");
	}
	
	/**
	 * Checks whether the given player is a War admin
	 *
	 * @param 	player	Player to check
	 * @return		true if the player is a War admin
	 */
	public boolean isWarAdmin(Player player) {
		return player.hasPermission("war.admin");
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
				this.msg(player, "You now have a wand for zone " + zoneName + ". Left-click with wooden sword for corner 1. Right-click for corner 2.");
				War.war.log(player.getName() + " now has a wand for warzone " + zoneName, Level.INFO);
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
	
	public boolean isSpoutServer() {
		return this.isSpoutServer;
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
		return ZoneLobby.getLobbyByLocation(location) != null;
	}

	public List<String> getZoneMakersImpersonatingPlayers() {
		return this.zoneMakersImpersonatingPlayers;
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

	public HashMap<String, PlayerState> getDisconnected() {
		return this.disconnected;
	}

	public void setDisconnected(HashMap<String, PlayerState> disconnected) {
		this.disconnected = disconnected;
	}

	public InventoryBag getDefaultInventories() {
		return defaultInventories;
	}

	public List<String> getDeadlyAdjectives() {
		return deadlyAdjectives;
	}

	public List<String> getKillerVerbs() {
		return killerVerbs;
	}

	public TeamConfigBag getTeamDefaultConfig() {
		return this.teamDefaultConfig ;
	}

	public WarzoneConfigBag getWarzoneDefaultConfig() {
		return this.warzoneDefaultConfig;
	}
	
	public WarConfigBag getWarConfig() {
		return this.warConfig;
	}

	public SpoutDisplayer getSpoutDisplayer() {
		return this.spoutMessenger ;
	}

	public HubLobbyMaterials getWarhubMaterials() {
		return this.warhubMaterials;
	}

	public void setWarhubMaterials(HubLobbyMaterials warhubMaterials) {
		this.warhubMaterials = warhubMaterials;
	}

	public boolean isTagServer() {
		return tagServer;
	}

	public KillstreakReward getKillstreakReward() {
		return killstreakReward;
	}

	public void setKillstreakReward(KillstreakReward killstreakReward) {
		this.killstreakReward = killstreakReward;
	}

	public MySQLConfig getMysqlConfig() {
		return mysqlConfig;
	}

	public void setMysqlConfig(MySQLConfig mysqlConfig) {
		this.mysqlConfig = mysqlConfig;
	}

	public String getString(String key) {
		return messages.getString(key);
	}

	public Locale getLoadedLocale() {
		return messages.getLocale();
	}

	/**
	 * Convert serialized effect to actual effect.
	 * @param serializedEffect String stored in configuration.
	 *                         Format: TYPE;DURATION;AMPLIFY
	 * @return Potion effect or null otherwise
	 */
	public PotionEffect getPotionEffect(String serializedEffect) {
		String[] arr = serializedEffect.split(";");
		if (arr.length != 3) return null;
		try {
			PotionEffectType type = PotionEffectType.getByName(arr[0]);
			int duration = Integer.parseInt(arr[1]);
			int amplification = Integer.parseInt(arr[2]);
			return new PotionEffect(type, duration, amplification);
		} catch (RuntimeException ex) {
			return null;
		}
	}

	public Economy getEconomy() {
		return econ;
	}

	public UIManager getUIManager() {
		return UIManager;
	}

	public void setUIManager(UIManager UIManager) {
		this.UIManager = UIManager;
	}
}
