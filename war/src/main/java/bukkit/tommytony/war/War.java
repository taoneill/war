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
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
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
import com.tommytony.war.mappers.*;
import com.tommytony.war.utils.*;

/**
 * Main class of War
 *
 * @author 	tommytony, Tim Düsterhus
 * @package 	bukkit.tommytony.war
 */
public class War extends JavaPlugin {
	public static PermissionHandler permissionHandler;
	public static War war;

	// general
	private WarPlayerListener playerListener = new WarPlayerListener();
	private WarEntityListener entityListener = new WarEntityListener();
	private WarBlockListener blockListener = new WarBlockListener();
	private WarCommandHandler commandHandler = new WarCommandHandler();
	private Logger log;
	private PluginDescriptionFile desc = null;
	private boolean loaded = false;

	// Zones and hub
	private List<Warzone> warzones;
	private WarHub warHub;
	private final List<Warzone> incompleteZones = new ArrayList<Warzone>();
	private final List<String> zoneMakerNames = new ArrayList<String>();
	private final List<String> commandWhitelist = new ArrayList<String>();
	private final List<String> zoneMakersImpersonatingPlayers = new ArrayList<String>();
	private HashMap<String, InventoryStash> disconnected = new HashMap<String, InventoryStash>();
	private final HashMap<String, String> wandBearers = new HashMap<String, String>(); // playername to zonename

	// Default warzone settings
	private final HashMap<Integer, ItemStack> defaultLoadout = new HashMap<Integer, ItemStack>();
	private int defaultLifepool = 7;
	private boolean defaultFriendlyFire = false;
	private boolean defaultAutoAssignOnly = false;
	private int defaultTeamCap = 7;
	private int defaultScoreCap = 10;
	private int defaultMonumentHeal = 5;
	private boolean defaultBlockHeads = true;
	private boolean defaultDropLootOnDeath = false;
	private String defaultSpawnStyle = TeamSpawnStyles.BIG;
	private final HashMap<Integer, ItemStack> defaultReward = new HashMap<Integer, ItemStack>();
	private boolean defaultUnbreakableZoneBlocks = false;
	private boolean defaultNoCreatures = false;
	private boolean defaultResetOnEmpty = false;
	private boolean defaultResetOnLoad = false;
	private boolean defaultResetOnUnload = false;

	// Global settings
	private boolean pvpInZonesOnly = false;
	private boolean disablePvpMessage = false;
	private boolean buildInZonesOnly = false;

	public War() {
		super();

		War.war = this;
	}

	public void onEnable() {
		this.loadWar();
	}

	public void onDisable() {
		this.unloadWar();
	}

	/**
	 * Initializes war
	 */
	public void loadWar() {
		this.setLoaded(true);
		this.warzones = new ArrayList<Warzone>();
		this.desc = this.getDescription();
		this.log = this.getServer().getLogger();
		this.setupPermissions();

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

		pm.registerEvent(Event.Type.ENTITY_EXPLODE, this.entityListener, Priority.Normal, this);
		pm.registerEvent(Event.Type.ENTITY_DAMAGE, this.entityListener, Priority.High, this);
		pm.registerEvent(Event.Type.ENTITY_COMBUST, this.entityListener, Priority.Normal, this);
		pm.registerEvent(Event.Type.CREATURE_SPAWN, this.entityListener, Priority.Normal, this);
		pm.registerEvent(Event.Type.ENTITY_REGAIN_HEALTH, this.entityListener, Priority.Normal, this);

		pm.registerEvent(Event.Type.BLOCK_PLACE, this.blockListener, Priority.Normal, this);
		pm.registerEvent(Event.Type.BLOCK_BREAK, this.blockListener, Priority.Normal, this);


		// Load files from disk or create them (using these defaults)
		this.getDefaultLoadout().put(0, new ItemStack(Material.STONE_SWORD, 1, (byte) 8));
		this.getDefaultLoadout().put(1, new ItemStack(Material.BOW, 1, (byte) 8));
		this.getDefaultLoadout().put(2, new ItemStack(Material.ARROW, 7));
		this.getDefaultLoadout().put(3, new ItemStack(Material.IRON_PICKAXE, 1, (byte) 8));
		this.getDefaultLoadout().put(4, new ItemStack(Material.STONE_SPADE, 1, (byte) 8));
		this.getDefaultReward().put( 0, new ItemStack(Material.CAKE, 1));

		WarMapper.load();
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

		this.setLoaded(false);
		this.log("War v" + this.desc.getVersion() + " is off.", Level.INFO);
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
	 * Handles war commands
	 */
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		return this.commandHandler.handle(sender, cmd, commandLabel, args);

		if (sender instanceof Player) {
			if (command.equals("teams")) {
				this.performTeams(player);
			} else if (command.equals("join") && this.canPlayWar(player)) {
				this.performJoin(player, arguments);
			} else if (command.equals("leave")) {
				this.performLeave(player);
			} else if (command.equals("team")) {
				this.performTeam(player, arguments);
			} else if (command.equals("warhub")) {
				this.performWarhub(player);
			} else if (this.isZoneMaker(player)) {
				// Mod commands : /nextbattle
				if (command.equals("nextbattle")) {
					this.performNextBattle(player);
				}
				// Warzone maker commands: /setzone, /savezone, /setteam, /setmonument, /resetzone
				else if (command.equals("setzone")) {
					this.performSetZone(player, arguments);
				} else if (command.equals("setzonelobby")) {
					this.performSetZoneLobby(player, arguments);
				} else if (command.equals("savezone")) {
					this.performSaveZone(player, arguments);
				} else if (command.equals("setzoneconfig") || command.equals("zonecfg")) {
					this.performSetZoneConfig(player, arguments);
				} else if (command.equals("resetzone")) {
					this.performResetZone(player, arguments);
				} else if (command.equals("deletezone")) {
					this.performDeleteZone(player, arguments);
				} else if (command.equals("setteam")) {
					this.performSetTeam(player, arguments);
				} else if (command.equals("setteamflag")) {
					this.performSetTeamFlag(player, arguments);
				} else if (command.equals("deleteteam")) {
					this.performDeleteTeam(player, arguments);
				} else if (command.equals("setmonument")) {
					this.performSetMonument(player, arguments);
				} else if (command.equals("deletemonument")) {
					this.performDeleteMonument(player, arguments);
				} else if (command.equals("setwarhub")) {
					this.performSetWarhub(player);
				} else if (command.equals("deletewarhub")) {
					this.performDeleteWarhub(player);
				} else if (command.equals("setwarconfig") || command.equals("warcfg")) {
					this.performSetWarConfig(player, arguments);
				} else if (command.equals("zonemaker") || command.equals("zm")) {
					this.performZonemakerAsZonemaker(player, arguments);
				} else if (command.equals("unloadwar")) {
					this.unloadWar();
				} else if (command.equals("loadwar")) {
					this.loadWar();
				}
			} else if (command.equals("setzone") // Not a zone maker but War command.
					|| command.equals("nextbattle") || command.equals("setzonelobby") || command.equals("savezone") || command.equals("setzoneconfig") || command.equals("resetzone") || command.equals("deletezone") || command.equals("setteam") || command.equals("deleteteam") || command.equals("setmonument") || command.equals("deletemonument") || command.equals("setwarhub") || command.equals("deletewarhub") || command.equals("setwarconfig") || command.equals("unloadwar")) {
				this.badMsg(player, "You can't do this if you are not a warzone maker.");
			} else if (command.equals("zonemaker") || command.equals("zm")) {
				this.performZonemakerAsPlayer(player);
			}
		}
		return true;
	}

	/**
	 * Converts the player-inventory to a loadout hashmap
	 *
	 * @param PlayerInventory		inv		inventory to get the items from
	 * @param HashMap<Integer, ItemStack>	loadout		the hashmap to save to
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
	 * @param Player			player		player to get the inventory to get the items from
	 * @param HashMap<Integer, ItemStack>	loadout		the hashmap to save to
	 */
	private void inventoryToLoadout(Player player, HashMap<Integer, ItemStack> loadout) {
		this.inventoryToLoadout(player.getInventory(), loadout);
	}

	public void performZonemakerAsPlayer(Player player) {
		boolean wasImpersonating = false;
		for (String name : this.getZoneMakersImpersonatingPlayers()) {
			if (player.getName().equals(name)) {
				wasImpersonating = true;
			}
		}
		if (wasImpersonating) {
			this.getZoneMakersImpersonatingPlayers().remove(player.getName());
			this.msg(player, "You are back as a zone maker.");
		}
		WarMapper.save();
	}

	public void performZonemakerAsZonemaker(Player player, String[] arguments) {
		if (arguments.length > 2) {
			this.badMsg(player, "Usage: /zonemaker <player-name>, /zonemaker" + "Elevates the player to zone maker or removes his rights. " + "If you are already a zonemaker, you can toggle between player and zone maker modes by using the command without arguments.");
		} else {
			if (arguments.length == 1) {
				// make someone zonemaker or remove the right
				if (this.zoneMakerNames.contains(arguments[0])) {
					// kick
					this.zoneMakerNames.remove(arguments[0]);
					this.msg(player, arguments[0] + " is not a zone maker anymore.");
					Player kickedMaker = this.getServer().getPlayer(arguments[0]);
					if (kickedMaker != null) {
						this.msg(kickedMaker, player.getName() + " took away your warzone maker priviledges.");
					}
				} else {
					// add
					this.zoneMakerNames.add(arguments[0]);
					this.msg(player, arguments[0] + " is now a zone maker.");
					Player newMaker = this.getServer().getPlayer(arguments[0]);
					if (newMaker != null) {
						this.msg(newMaker, player.getName() + " made you warzone maker.");
					}
				}
			} else {
				// toggle to player mode
				if (this.isZoneMaker(player)) {
					this.getZoneMakersImpersonatingPlayers().add(player.getName());
				}
				this.msg(player, "You are now impersonating a regular player. Type /zonemaker again to toggle back to war maker mode.");
			}

			WarMapper.save();
		}
	}

	public void performSetWarConfig(Player player, String[] arguments) {
		if (arguments.length == 0) {
			this.badMsg(player, "Usage: /setwarconfig pvpinzonesonly:on lifepool:8 teamsize:5 maxscore:7 autoassign:on outline:off ff:on  " + "Changes the server defaults for new warzones. Please give at leaset one named parameter.");
		} else {
			if (this.updateFromNamedParams(player, arguments)) {
				WarMapper.save();
				this.msg(player, "War config saved.");
			} else {
				this.badMsg(player, "Failed to read named parameters.");
			}
		}
	}

	public void performDeleteWarhub(Player player) {
		if (this.warHub != null) {
			// reset existing hub
			this.warHub.getVolume().resetBlocks();
			VolumeMapper.delete(this.warHub.getVolume(), this);
			this.warHub = null;
			for (Warzone zone : this.warzones) {
				if (zone.getLobby() != null) {
					zone.getLobby().getVolume().resetBlocks();
					zone.getLobby().initialize();
				}
			}

			this.msg(player, "War hub removed.");
		} else {
			this.badMsg(player, "No War hub to delete.");
		}
		WarMapper.save();
	}

	public void performSetWarhub(Player player) {
		if (this.warzones.size() > 0) {
			if (this.warHub != null) {
				// reset existing hub
				this.warHub.getVolume().resetBlocks();
				this.warHub.setLocation(player.getLocation());
				this.warHub.initialize();
				this.msg(player, "War hub moved.");
			} else {
				this.warHub = new WarHub(player.getLocation());
				this.warHub.initialize();
				for (Warzone zone : this.warzones) {
					if (zone.getLobby() != null) {
						zone.getLobby().getVolume().resetBlocks();
						zone.getLobby().initialize();
					}
				}
				this.msg(player, "War hub created.");
			}
			WarMapper.save();
		} else {
			this.badMsg(player, "No warzones yet.");
		}
	}

	public void performDeleteMonument(Player player, String[] arguments) {
		if (arguments.length < 1 || (!this.inAnyWarzone(player.getLocation()) && !this.inAnyWarzoneLobby(player.getLocation()))) {
			this.badMsg(player, "Usage: /deletemonument <name>." + " Deletes the monument. " + "Must be in a warzone or lobby (try /warzones and /warzone). ");
		} else {
			String name = arguments[0];
			Warzone warzone = this.warzone(player.getLocation());
			ZoneLobby lobby = this.lobby(player.getLocation());
			if (warzone == null && lobby != null) {
				warzone = lobby.getZone();
			} else {
				lobby = warzone.getLobby();
			}
			Monument monument = warzone.getMonument(name);
			if (monument != null) {
				monument.getVolume().resetBlocks();
				warzone.getMonuments().remove(monument);
				WarzoneMapper.save(warzone, false);
				this.msg(player, "Monument " + monument.getName() + " removed.");
			} else {
				this.badMsg(player, "No such monument.");
			}
		}
	}

	public void performSetMonument(Player player, String[] arguments) {
		if (!this.inAnyWarzone(player.getLocation()) || arguments.length < 1 || arguments.length > 1 || (arguments.length == 1 && this.warzone(player.getLocation()) != null && arguments[0].equals(this.warzone(player.getLocation()).getName()))) {
			this.badMsg(player, "Usage: /setmonument <name>. Creates or moves a monument. Monument can't have same name as zone. Must be in warzone.");
		} else {
			Warzone warzone = this.warzone(player.getLocation());
			String monumentName = arguments[0];
			if (warzone.hasMonument(monumentName)) {
				// move the existing monument
				Monument monument = warzone.getMonument(monumentName);
				monument.getVolume().resetBlocks();
				monument.setLocation(player.getLocation());
				this.msg(player, "Monument " + monument.getName() + " was moved.");
			} else {
				// create a new monument
				Monument monument = new Monument(arguments[0], warzone, player.getLocation());
				warzone.getMonuments().add(monument);
				this.msg(player, "Monument " + monument.getName() + " created.");
			}
			WarzoneMapper.save(warzone, false);
		}
	}

	public void performDeleteTeam(Player player, String[] arguments) {
		if (arguments.length < 1 || (!this.inAnyWarzone(player.getLocation()) && !this.inAnyWarzoneLobby(player.getLocation()))) {
			this.badMsg(player, "Usage: /deleteteam <team-name/color>." + " Deletes the team and its spawn. " + "Must be in a warzone or lobby (try /zones and /zone). ");
		} else {
			String name = arguments[0];
			Warzone warzone = this.warzone(player.getLocation());
			ZoneLobby lobby = this.lobby(player.getLocation());
			if (warzone == null && lobby != null) {
				warzone = lobby.getZone();
			} else {
				lobby = warzone.getLobby();
			}
			Team team = warzone.getTeamByKind(TeamKinds.teamKindFromString(name));
			if (team != null) {
				if (team.getFlagVolume() != null) {
					team.getFlagVolume().resetBlocks();
				}
				team.getSpawnVolume().resetBlocks();
				warzone.getTeams().remove(team);
				if (warzone.getLobby() != null) {
					warzone.getLobby().getVolume().resetBlocks();
					// warzone.getVolume().resetWallBlocks(warzone.getLobby().getWall());
					// warzone.addZoneOutline(warzone.getLobby().getWall());
					warzone.getLobby().initialize();
				}
				WarzoneMapper.save(warzone, false);
				this.msg(player, "Team " + team.getName() + " removed.");
			} else {
				this.badMsg(player, "No such team.");
			}
		}
	}

	public void performSetTeamFlag(Player player, String[] arguments) {
		if (arguments.length < 1 || !this.inAnyWarzone(player.getLocation()) || (arguments.length > 0 && TeamKinds.teamKindFromString(arguments[0]) == null)) {
			this.badMsg(player, "Usage: /setteamflag <team-name/color>, e.g. /setteamflag diamond. " + "Sets the team flag post to the current location. " + "Must be in a warzone (try /zones and /zone). ");
		} else {
			TeamKind kind = TeamKinds.teamKindFromString(arguments[0]);
			Warzone warzone = this.warzone(player.getLocation());
			Team team = warzone.getTeamByKind(kind);
			if (team == null) {
				// no such team yet
				this.badMsg(player, "Place the team spawn first.");
			} else if (team.getFlagVolume() == null) {
				// new team flag
				team.setTeamFlag(player.getLocation());
				Location playerLoc = player.getLocation();
				player.teleport(new Location(playerLoc.getWorld(), playerLoc.getBlockX() + 1, playerLoc.getBlockY(), playerLoc.getBlockZ()));
				this.msg(player, "Team " + team.getName() + " flag added here.");
				WarzoneMapper.save(warzone, false);
			} else {
				// relocate flag
				team.getFlagVolume().resetBlocks();
				team.setTeamFlag(player.getLocation());
				Location playerLoc = player.getLocation();
				player.teleport(new Location(playerLoc.getWorld(), playerLoc.getBlockX() + 1, playerLoc.getBlockY(), playerLoc.getBlockZ() + 1));
				this.msg(player, "Team " + team.getName() + " flag moved.");
				WarzoneMapper.save(warzone, false);
			}
		}
	}

	public void performSetTeam(Player player, String[] arguments) {
		if (arguments.length < 1 || !this.inAnyWarzone(player.getLocation()) || (arguments.length > 0 && TeamKinds.teamKindFromString(arguments[0]) == null)) {
			this.badMsg(player, "Usage: /setteam <team-kind/color>, e.g. /setteam red." + "Sets the team spawn to the current location. " + "Must be in a warzone (try /zones and /zone). ");
		} else {
			TeamKind teamKind = TeamKinds.teamKindFromString(arguments[0]);
			Warzone warzone = this.warzone(player.getLocation());
			Team existingTeam = warzone.getTeamByKind(teamKind);
			if (existingTeam != null) {
				// relocate
				existingTeam.setTeamSpawn(player.getLocation());
				this.msg(player, "Team " + existingTeam.getName() + " spawn relocated.");
			} else {
				// new team (use default TeamKind name for now)
				Team newTeam = new Team(teamKind.getDefaultName(), teamKind, player.getLocation(), warzone);
				newTeam.setRemainingLives(warzone.getLifePool());
				warzone.getTeams().add(newTeam);
				if (warzone.getLobby() != null) {
					warzone.getLobby().getVolume().resetBlocks();
					// warzone.getVolume().resetWallBlocks(warzone.getLobby().getWall());
					// warzone.addZoneOutline(warzone.getLobby().getWall());
					warzone.getLobby().initialize();
				}
				newTeam.setTeamSpawn(player.getLocation());
				this.msg(player, "Team " + newTeam.getName() + " created with spawn here.");
			}

			WarzoneMapper.save(warzone, false);
		}
	}

	public void performDeleteZone(Player player, String[] arguments) {
		if (arguments.length == 0 && !this.inAnyWarzone(player.getLocation()) && !this.inAnyWarzoneLobby(player.getLocation())) {
			this.badMsg(player, "Usage: /deletezone [warzone-name]. " + "Deletes the warzone. " + "Must be in the warzone or name must be provided (try /zones and /zone). ");
		} else {
			ZoneLobby lobby = null;
			Warzone warzone = null;
			if (arguments.length == 1) { // get zone by name
				for (Warzone tmp : this.getWarzones()) {
					if (tmp.getName().toLowerCase().startsWith(arguments[0].toLowerCase())) {
						warzone = tmp;
						break;
					}
				}
				if (warzone == null) {
					this.badMsg(player, "No such warzone.");
					return;
				}
			} else { // get zone by position
				warzone = this.warzone(player.getLocation());
				lobby = this.lobby(player.getLocation());
			}

			if (warzone == null && lobby != null) {
				warzone = lobby.getZone();
			} else {
				lobby = warzone.getLobby();
			}

			for (Team t : warzone.getTeams()) {
				if (t.getTeamFlag() != null) {
					t.getFlagVolume().resetBlocks();
				}
				t.getSpawnVolume().resetBlocks();

				// reset inventory
				for (Player p : t.getPlayers()) {
					warzone.restorePlayerInventory(p);
				}
			}
			for (Monument m : warzone.getMonuments()) {
				m.getVolume().resetBlocks();
			}
			if (warzone.getLobby() != null) {
				warzone.getLobby().getVolume().resetBlocks();
			}
			warzone.getVolume().resetBlocks();
			this.getWarzones().remove(warzone);
			WarMapper.save();
			WarzoneMapper.delete(warzone.getName());
			if (this.warHub != null) { // warhub has to change
				this.warHub.getVolume().resetBlocks();
				this.warHub.initialize();
			}
			this.msg(player, "Warzone " + warzone.getName() + " removed.");
		}
	}

	public void performResetZone(Player player, String[] arguments) {
		if (!this.inAnyWarzone(player.getLocation()) && !this.inAnyWarzoneLobby(player.getLocation())) {
			this.badMsg(player, "Usage: /resetzone. Reloads the zone. Must be in warzone or lobby.");
		} else {
			Warzone warzone = this.warzone(player.getLocation());
			ZoneLobby lobby = this.lobby(player.getLocation());
			if (warzone == null && lobby != null) {
				warzone = lobby.getZone();
			} else {
				lobby = warzone.getLobby();
			}
			warzone.clearFlagThieves();
			for (Team team : warzone.getTeams()) {
				team.teamcast("The war has ended. " + this.playerListener.getAllTeamsMsg(player) + " Resetting warzone " + warzone.getName() + " and teams...");
				for (Player p : team.getPlayers()) {
					warzone.restorePlayerInventory(p);
					p.teleport(warzone.getTeleport());
					this.msg(player, "You have left the warzone. Your inventory has been restored.");
				}
				team.resetPoints();
				team.getPlayers().clear();
			}

			this.msg(player, "Reloading warzone " + warzone.getName() + ".");
			warzone.getVolume().resetBlocksAsJob();
			if (lobby != null) {
				lobby.getVolume().resetBlocksAsJob();
			}
			warzone.initializeZoneAsJob();
		}
	}

	public void performSetZoneConfig(Player player, String[] arguments) {
		if ((!this.inAnyWarzone(player.getLocation()) && !this.inAnyWarzoneLobby(player.getLocation())) || arguments.length == 0) {
			this.badMsg(player, "Usage: /setzoneconfig lifepool:8 teamsize:5 maxscore:7 autoassign:on outline:off ff:on  " + "Please give at leaset one named parameter. Does not save the blocks of the warzone. Resets the zone with the new config. Must be in warzone.");
		} else {
			Warzone warzone = this.warzone(player.getLocation());
			ZoneLobby lobby = this.lobby(player.getLocation());
			if (warzone == null && lobby != null) {
				warzone = lobby.getZone();
			} else {
				lobby = warzone.getLobby();
			}
			if (this.updateZoneFromNamedParams(warzone, player, arguments)) {
				this.msg(player, "Saving config and resetting warzone " + warzone.getName() + ".");
				WarzoneMapper.save(warzone, false);
				warzone.getVolume().resetBlocks();
				if (lobby != null) {
					lobby.getVolume().resetBlocks();
				}
				warzone.initializeZone(); // bring back team spawns etc
				this.msg(player, "Warzone config saved. Zone reset.");

				if (this.warHub != null) { // maybe the zone was disabled/enabled
					this.warHub.getVolume().resetBlocks();
					this.warHub.initialize();
				}
			} else {
				this.badMsg(player, "Failed to read named parameters.");
			}
		}
	}

	public void performSaveZone(Player player, String[] arguments) {
		if (!this.inAnyWarzone(player.getLocation()) && !this.inAnyWarzoneLobby(player.getLocation())) {
			this.badMsg(player, "Usage: /savezone lifepool:8 teamsize:5 maxscore:7 autoassign:on outline:off ff:on " + "All named params optional. Saves the blocks of the warzone (i.e. the current zone state will be reloaded at each battle start). Must be in warzone.");
		} else {
			Warzone warzone = this.warzone(player.getLocation());
			ZoneLobby lobby = this.lobby(player.getLocation());
			if (warzone == null && lobby != null) {
				warzone = lobby.getZone();
			} else {
				lobby = warzone.getLobby();
			}
			this.msg(player, "Saving warzone " + warzone.getName() + ".");
			int savedBlocks = warzone.saveState(true);
			if (arguments.length > 0) {
				// changed settings: must reinitialize with new settings
				this.updateZoneFromNamedParams(warzone, player, arguments);
				WarzoneMapper.save(warzone, true);
				warzone.getVolume().resetBlocks();
				if (lobby != null) {
					lobby.getVolume().resetBlocks();
				}
				warzone.initializeZone(); // bring back team spawns etc

				if (this.warHub != null) { // maybe the zone was disabled/enabled
					this.warHub.getVolume().resetBlocks();
					this.warHub.initialize();
				}
			}

			this.msg(player, "Warzone " + warzone.getName() + " initial state changed. Saved " + savedBlocks + " blocks.");
		}
	}

	public void performSetZoneLobby(Player player, String[] arguments) {
		String usageStr = "Usage: When inside a warzone - /setzonelobby <north/n/east/e/south/s/west/w>." + "Attaches the lobby to the specified zone wall. When outside a warzone - /setzonelobby <zonename>. " + "Moves the lobby to your current position.";
		if (arguments.length < 1 || arguments.length > 1) {
			this.badMsg(player, usageStr);
		} else if (this.inAnyWarzone(player.getLocation()) || this.inAnyWarzoneLobby(player.getLocation())) {
			// Inside a warzone: use the classic n/s/e/w mode
			if (!arguments[0].equals("north") && !arguments[0].equals("n") && !arguments[0].equals("east") && !arguments[0].equals("e") && !arguments[0].equals("south") && !arguments[0].equals("s") && !arguments[0].equals("west") && !arguments[0].equals("w")) {
				this.badMsg(player, usageStr);
				return;
			}
			Warzone warzone = this.warzone(player.getLocation());
			ZoneLobby lobby = this.lobby(player.getLocation());
			if (warzone == null && lobby != null) {
				warzone = lobby.getZone();
			} else {
				lobby = warzone.getLobby();
			}
			BlockFace wall = BlockFace.WEST;
			String wallStr = "";
			if (arguments[0].equals("north") || arguments[0].equals("n")) {
				wall = BlockFace.NORTH;
				wallStr = "north";
			} else if (arguments[0].equals("east") || arguments[0].equals("e")) {
				wall = BlockFace.EAST;
				wallStr = "east";
			} else if (arguments[0].equals("south") || arguments[0].equals("s")) {
				wall = BlockFace.SOUTH;
				wallStr = "south";
			} else if (arguments[0].equals("west") || arguments[0].equals("w")) {
				wall = BlockFace.WEST;
				wallStr = "west";
			}
			if (lobby != null) {
				// reset existing lobby
				lobby.getVolume().resetBlocks();
				lobby.setWall(wall);
				lobby.initialize();
				this.msg(player, "Warzone lobby moved to " + wallStr + " side of zone.");
			} else {
				// new lobby
				lobby = new ZoneLobby(warzone, wall);
				warzone.setLobby(lobby);
				lobby.initialize();
				if (this.warHub != null) { // warhub has to change
					this.warHub.getVolume().resetBlocks();
					this.warHub.initialize();
				}
				this.msg(player, "Warzone lobby created on " + wallStr + "side of zone.");
			}
			WarzoneMapper.save(warzone, false);
		} else {
			// Not in a warzone: set the lobby position to where the player is standing
			Warzone warzone = this.matchWarzone(arguments[0]);
			if (warzone == null) {
				this.badMsg(player, "No warzone matches " + arguments[0] + ".");
			} else {
				// Move the warzone lobby
				ZoneLobby lobby = warzone.getLobby();
				if (lobby != null) {
					// reset existing lobby
					lobby.getVolume().resetBlocks();
					lobby.setLocation(player.getLocation());
					lobby.initialize();
					this.msg(player, "Warzone lobby moved to your location.");
				} else {
					// new lobby
					lobby = new ZoneLobby(warzone, player.getLocation());
					warzone.setLobby(lobby);
					lobby.initialize();
					if (this.warHub != null) { // warhub has to change
						this.warHub.getVolume().resetBlocks();
						this.warHub.initialize();
					}
					this.msg(player, "Warzone lobby moved to your location.");
				}
				WarzoneMapper.save(warzone, false);
			}
		}
	}

	public void performSetZone(Player player, String[] arguments) {
		if (arguments.length < 2 || arguments.length > 2 || (arguments.length == 2 && (!arguments[1].equals("southeast") && !arguments[1].equals("northwest") && !arguments[1].equals("se") && !arguments[1].equals("nw") && !arguments[1].equals("corner1") && !arguments[1].equals("corner2") && !arguments[1].equals("c1") && !arguments[1].equals("c2") && !arguments[1].equals("pos1") && !arguments[1].equals("pos2") && !arguments[1].equals("wand")))) {
			if (arguments.length == 1) {
				// we only have a zone name, default to wand mode
				this.addWandBearer(player, arguments[0]);
			} else {
				this.badMsg(player, "Usage: =<Classic mode>= /setzone <warzone-name> <'northwest'/'southeast'/'nw'/'se'> (NW defaults to top block, SE to bottom). " + "=<Wand Cuboid mode>= /setzone <warzone-name> wand (gives you a wooden sword to right and left click, drop to disable). " + "=<Wandless Cuboid mode>= /setzone <warzone-name> <'corner1'/'corner2'/'c1'/'c2'/'pos1'/'pos2'> (block where you're standing). " + "Set one corner, then the next. Defines the outline of the warzone, which will be reset at the start of every battle. " + "Saves the zone blocks if the outline is valid.");
			}
		} else {
			ZoneSetter setter = new ZoneSetter(player, arguments[0]);
			if (arguments[1].equals("northwest") || arguments[1].equals("nw")) {
				setter.placeNorthwest();
			} else if (arguments[1].equals("southeast") || arguments[1].equals("se")) {
				setter.placeSoutheast();
			} else if (arguments[1].equals("corner1") || arguments[1].equals("c1") || arguments[1].equals("pos1")) {
				setter.placeCorner1();
			} else if (arguments[1].equals("corner2") || arguments[1].equals("c2") || arguments[1].equals("pos2")) {
				setter.placeCorner2();
			} else if (arguments[1].equals("wand")) {
				this.addWandBearer(player, arguments[0]);
			}
		}
	}

	public void performNextBattle(Player player) {
		if (!this.inAnyWarzone(player.getLocation())) {
			this.badMsg(player, "Usage: /nextbattle. Resets the zone blocks and all teams' life pools. Must be in warzone.");
		} else {
			Warzone warzone = this.warzone(player.getLocation());
			warzone.clearFlagThieves();
			for (Team team : warzone.getTeams()) {
				team.teamcast("The battle was interrupted. " + this.playerListener.getAllTeamsMsg(player) + " Resetting warzone " + warzone.getName() + " and life pools...");
			}
			warzone.getVolume().resetBlocksAsJob();
			warzone.initializeZoneAsJob();
		}
	}

	public void performWarhub(Player player) {
		if (this.getWarHub() == null) {
			this.badMsg(player, "No warhub on this War server. Try /zones and /zone.");
		} else if (!this.canWarp(player)) {
			this.badMsg(player, "Can't warp to warhub. You need the 'war.warp' permission.");
		} else {
			Team playerTeam = this.getPlayerTeam(player.getName());
			Warzone playerWarzone = this.getPlayerTeamWarzone(player.getName());
			if (playerTeam != null) { // was in zone
				playerWarzone.handlePlayerLeave(player, this.getWarHub().getLocation(), true);
			}
			player.teleport(this.getWarHub().getLocation());
		}
	}

	public void performTeam(Player player, String[] arguments) {
		Team playerTeam = this.getPlayerTeam(player.getName());
		if (playerTeam == null) {
			this.badMsg(player, "Usage: /team <message>. " + "Sends a message only to your teammates.");
		} else {
			ChatColor color = playerTeam.getKind().getColor();
			String teamMessage = color + player.getName() + ": " + ChatColor.WHITE;
			for (String part : arguments) {
				teamMessage += part + " ";
			}
			playerTeam.teamcast(teamMessage);
		}
	}

	public void performLeave(Player player) {
		if (!this.inAnyWarzone(player.getLocation()) || this.getPlayerTeam(player.getName()) == null) {
			this.badMsg(player, "Usage: /leave. " + "Must be in a team already.");
		} else {
			Warzone zone = this.getPlayerTeamWarzone(player.getName());
			zone.handlePlayerLeave(player, zone.getTeleport(), true);
		}
	}

	public void performJoin(Player player, String[] arguments) {
		if (arguments.length < 1 || (!this.inAnyWarzone(player.getLocation()) && !this.inAnyWarzoneLobby(player.getLocation())) || (arguments.length > 0 && TeamKinds.teamKindFromString(arguments[0]) == null)) {
			this.badMsg(player, "Usage: /join <diamond/iron/gold/red/blue/green/etc.>." + " Teams are warzone specific." + " You must be inside a warzone or zone lobby to join a team." + " Use as an alternative to walking through the team gate.");
		} else {
			// drop from old team if any
			Team previousTeam = this.getPlayerTeam(player.getName());
			if (previousTeam != null) {
				Warzone zone = this.getPlayerTeamWarzone(player.getName());
				if (!previousTeam.removePlayer(player.getName())) {
					this.logWarn("Could not remove player " + player.getName() + " from team " + previousTeam.getName());
				}
				if (zone.isFlagThief(player.getName())) {
					Team victim = zone.getVictimTeamForThief(player.getName());
					victim.getFlagVolume().resetBlocks();
					victim.initializeTeamFlag();
					zone.removeThief(player.getName());
					for (Team t : zone.getTeams()) {
						t.teamcast("Team " + victim.getName() + " flag was returned.");
					}
				}
				previousTeam.resetSign();
			}

			// join new team
			String name = arguments[0];
			TeamKind kind = TeamKinds.teamKindFromString(arguments[0]);
			Warzone warzone = this.warzone(player.getLocation());
			ZoneLobby lobby = this.lobby(player.getLocation());
			if (warzone == null && lobby != null) {
				warzone = lobby.getZone();
			} else {
				lobby = warzone.getLobby();
			}
			if (warzone.isDisabled()) {
				this.badMsg(player, "This warzone is disabled.");
			} else {
				List<Team> teams = warzone.getTeams();
				boolean foundTeam = false;
				for (Team team : teams) {
					if (team.getName().startsWith(name) || team.getKind() == kind) {
						if (!warzone.hasPlayerInventory(player.getName())) {
							warzone.keepPlayerInventory(player);
							this.msg(player, "Your inventory is in storage until you /leave.");
						}
						if (team.getPlayers().size() < warzone.getTeamCap()) {
							team.addPlayer(player);
							team.resetSign();
							warzone.respawnPlayer(team, player);
							if (this.warHub != null) {
								this.warHub.resetZoneSign(warzone);
							}
							foundTeam = true;
						} else {
							this.badMsg(player, "Team " + team.getName() + " is full.");
							foundTeam = true;
						}
					}
				}
				if (foundTeam) {
					for (Team team : teams) {
						team.teamcast("" + player.getName() + " joined " + team.getName());
					}
				} else {
					this.badMsg(player, "No such team. Try /teams.");
				}
			}
		}
	}

	public void performTeams(Player player) {
		if (!this.inAnyWarzone(player.getLocation()) && !this.inAnyWarzoneLobby(player.getLocation())) {
			this.badMsg(player, "Usage: /teams. " + "Must be in a warzone or zone lobby (try /war, /zones and /zone).");
		} else {
			this.msg(player, "" + this.playerListener.getAllTeamsMsg(player));
		}
	}

	private boolean updateZoneFromNamedParams(Warzone warzone, Player player, String[] arguments) {
		try {
			Map<String, String> namedParams = new HashMap<String, String>();
			for (String namedPair : arguments) {
				String[] pairSplit = namedPair.split(":");
				if (pairSplit.length == 2) {
					namedParams.put(pairSplit[0].toLowerCase(), pairSplit[1]);
				}
			}
			if (namedParams.containsKey("lifepool")) {
				warzone.setLifePool(Integer.parseInt(namedParams.get("lifepool")));
			}
			if (namedParams.containsKey("monumentheal")) {
				warzone.setMonumentHeal(Integer.parseInt(namedParams.get("monumentheal")));
			}
			if (namedParams.containsKey("teamsize")) {
				warzone.setTeamCap(Integer.parseInt(namedParams.get("teamsize")));
			}
			if (namedParams.containsKey("maxscore")) {
				warzone.setScoreCap(Integer.parseInt(namedParams.get("maxscore")));
			}
			if (namedParams.containsKey("ff")) {
				String onOff = namedParams.get("ff");
				warzone.setFriendlyFire(onOff.equals("on") || onOff.equals("true"));
			}
			if (namedParams.containsKey("autoassign")) {
				String onOff = namedParams.get("autoassign");
				warzone.setAutoAssignOnly(onOff.equals("on") || onOff.equals("true"));
			}
			if (namedParams.containsKey("blockheads")) {
				String onOff = namedParams.get("blockheads");
				warzone.setBlockHeads(onOff.equals("on") || onOff.equals("true"));
			}
			if (namedParams.containsKey("spawnstyle")) {
				String spawnStyle = namedParams.get("spawnstyle").toLowerCase();
				if (spawnStyle.equals(TeamSpawnStyles.SMALL)) {
					warzone.setSpawnStyle(spawnStyle);
				} else if (spawnStyle.equals(TeamSpawnStyles.FLAT)) {
					warzone.setSpawnStyle(spawnStyle);
				} else if (spawnStyle.equals(TeamSpawnStyles.INVISIBLE)) {
					warzone.setSpawnStyle(spawnStyle);
				} else {
					warzone.setSpawnStyle(TeamSpawnStyles.BIG);
				}
			}
			if (namedParams.containsKey("unbreakable")) {
				String onOff = namedParams.get("unbreakable");
				warzone.setUnbreakableZoneBlocks(onOff.equals("on") || onOff.equals("true"));
			}
			if (namedParams.containsKey("disabled")) {
				String onOff = namedParams.get("disabled");
				warzone.setDisabled(onOff.equals("on") || onOff.equals("true"));
			}
			if (namedParams.containsKey("nocreatures")) {
				String onOff = namedParams.get("nocreatures");
				warzone.setNoCreatures(onOff.equals("on") || onOff.equals("true"));
			}
			if (namedParams.containsKey("loadout")) {
				// String loadoutType = namedParams.get("loadout");
				this.inventoryToLoadout(player, warzone.getLoadout());
			}
			if (namedParams.containsKey("reward")) {
				// String rewardType = namedParams.get("reward");
				this.inventoryToLoadout(player, warzone.getReward());
			}
			if (namedParams.containsKey("resetonempty")) {
				String onOff = namedParams.get("resetonempty");
				warzone.setResetOnEmpty(onOff.equals("on") || onOff.equals("true"));
			}
			if (namedParams.containsKey("resetonload")) {
				String onOff = namedParams.get("resetonload");
				warzone.setResetOnLoad(onOff.equals("on") || onOff.equals("true"));
			}
			if (namedParams.containsKey("resetonunload")) {
				String onOff = namedParams.get("resetonunload");
				warzone.setResetOnUnload(onOff.equals("on") || onOff.equals("true"));
			}

			return true;
		} catch (Exception e) {
			return false;
		}
	}

	private boolean updateFromNamedParams(Player player, String[] arguments) {
		try {
			Map<String, String> namedParams = new HashMap<String, String>();
			for (String namedPair : arguments) {
				String[] pairSplit = namedPair.split(":");
				if (pairSplit.length == 2) {
					namedParams.put(pairSplit[0].toLowerCase(), pairSplit[1]);
				}
			}
			if (namedParams.containsKey("lifepool")) {
				this.setDefaultLifepool(Integer.parseInt(namedParams.get("lifepool")));
			}
			if (namedParams.containsKey("monumentheal")) {
				this.setDefaultMonumentHeal(Integer.parseInt(namedParams.get("monumentheal")));
			}
			if (namedParams.containsKey("teamsize")) {
				this.setDefaultTeamCap(Integer.parseInt(namedParams.get("teamsize")));
			}
			if (namedParams.containsKey("maxscore")) {
				this.setDefaultScoreCap(Integer.parseInt(namedParams.get("maxscore")));
			}
			if (namedParams.containsKey("ff")) {
				String onOff = namedParams.get("ff");
				this.setDefaultFriendlyFire(onOff.equals("on"));
			}
			if (namedParams.containsKey("autoassign")) {
				String onOff = namedParams.get("autoassign");
				this.setDefaultAutoAssignOnly(onOff.equals("on") || onOff.equals("true"));
			}
			if (namedParams.containsKey("pvpinzonesonly")) {
				String onOff = namedParams.get("pvpinzonesonly");
				this.setPvpInZonesOnly(onOff.equals("on") || onOff.equals("true"));
			}
			if (namedParams.containsKey("disablepvpmessage")) {
				String onOff = namedParams.get("disablepvpmessage");
				this.setDisablePvpMessage(onOff.equals("on") || onOff.equals("true"));
			}
			if (namedParams.containsKey("blockheads")) {
				String onOff = namedParams.get("blockheads");
				this.setDefaultBlockHeads(onOff.equals("on") || onOff.equals("true"));
			}
			if (namedParams.containsKey("spawnstyle")) {
				String spawnStyle = namedParams.get("spawnstyle").toLowerCase();
				if (spawnStyle.equals(TeamSpawnStyles.SMALL)) {
					this.setDefaultSpawnStyle(spawnStyle);
				} else if (spawnStyle.equals(TeamSpawnStyles.FLAT)) {
					this.setDefaultSpawnStyle(spawnStyle);
				} else {
					this.setDefaultSpawnStyle(TeamSpawnStyles.BIG);
				}
			}
			if (namedParams.containsKey("buildinzonesonly")) {
				String onOff = namedParams.get("buildinzonesonly");
				this.setBuildInZonesOnly(onOff.equals("on") || onOff.equals("true"));
			}
			if (namedParams.containsKey("unbreakable")) {
				String onOff = namedParams.get("unbreakable");
				this.setDefaultUnbreakableZoneBlocks(onOff.equals("on") || onOff.equals("true"));
			}
			if (namedParams.containsKey("nocreatures")) {
				String onOff = namedParams.get("nocreatures");
				this.setDefaultNoCreatures(onOff.equals("on") || onOff.equals("true"));
			}
			if (namedParams.containsKey("loadout")) {
				// String loadoutType = namedParams.get("loadout");
				this.inventoryToLoadout(player, this.getDefaultLoadout());
			}
			if (namedParams.containsKey("reward")) {
				// String rewardType = namedParams.get("reward");
				this.inventoryToLoadout(player, this.getDefaultReward());
			}
			if (namedParams.containsKey("resetonempty")) {
				String onOff = namedParams.get("resetonempty");
				this.setDefaultResetOnEmpty(onOff.equals("on") || onOff.equals("true"));
			}
			if (namedParams.containsKey("resetonload")) {
				String onOff = namedParams.get("resetonload");
				this.setDefaultResetOnLoad(onOff.equals("on") || onOff.equals("true"));
			}
			if (namedParams.containsKey("resetonunload")) {
				String onOff = namedParams.get("resetonunload");
				this.setDefaultResetOnUnload(onOff.equals("on") || onOff.equals("true"));
			}
			if (namedParams.containsKey("rallypoint")) {
				// String rewardType = namedParams.get("reward");
				this.setZoneRallyPoint(namedParams.get("rallypoint"), player);
			}

			return true;
		} catch (Exception e) {
			return false;
		}
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

	public Team getPlayerTeam(String playerName) {
		for (Warzone warzone : this.warzones) {
			Team team = warzone.getPlayerTeam(playerName);
			if (team != null) {
				return team;
			}
		}
		return null;
	}

	public Warzone getPlayerTeamWarzone(String playerName) {
		for (Warzone warzone : this.warzones) {
			Team team = warzone.getPlayerTeam(playerName);
			if (team != null) {
				return warzone;
			}
		}
		return null;
	}

	public Logger getLogger() {
		return this.log;
	}

	public Warzone warzone(Location location) {
		for (Warzone warzone : this.warzones) {
			if (location.getWorld().getName().equals(warzone.getWorld().getName()) && warzone.getVolume() != null && warzone.getVolume().contains(location)) {
				return warzone;
			}
		}
		return null;
	}

	public boolean inAnyWarzone(Location location) {
		Block locBlock = location.getWorld().getBlockAt(location.getBlockX(), location.getBlockY(), location.getBlockZ());
		Warzone currentZone = this.warzone(location);
		if (currentZone == null) {
			return false;
		} else if (currentZone.getVolume().isWallBlock(locBlock)) {
			return false; // wall block doesnt count. this lets people in at the lobby side wall because wall gates overlap with the zone.
		}
		return true;
	}

	public boolean inWarzone(String warzoneName, Location location) {
		Warzone currentZone = this.warzone(location);
		if (currentZone == null) {
			return false;
		} else if (warzoneName.toLowerCase().equals(currentZone.getName().toLowerCase())) {
			return true;
		}
		return false;
	}

	public void addWarzone(Warzone zone) {
		this.warzones.add(zone);
	}

	public List<Warzone> getWarzones() {
		return this.warzones;
	}

	public void msg(Player player, String str) {
		String out = ChatColor.GRAY + "War> " + ChatColor.WHITE + this.colorTeams(str, ChatColor.WHITE) + " ";
		ChatFixUtil.sendMessage(player, out);
	}

	public void badMsg(Player player, String str) {
		String out = ChatColor.GRAY + "War> " + ChatColor.RED + this.colorTeams(str, ChatColor.RED) + " ";
		ChatFixUtil.sendMessage(player, out);
	}

	/**
	 * Colors the teams in messages
	 *
	 * @param 	String	str		message-string
	 * @param 	String	msgColor	current message-color
	 * @return	String			Message with colored teams
	 */
	private String colorTeams(String str, ChatColor msgColor) {
		for (TeamKind kind : TeamKinds.getTeamkinds()) {
			str = str.replaceAll(" " + kind.getDefaultName(), " " + kind.getColor() + kind.getDefaultName() + msgColor);
		}
		return str;
	}

	/**
	 * Sends a message of Level Info to the logger
	 *
	 * @param 	String	str	message to send
	 * @deprecated	Use War.log() now
	 */
	@Deprecated
	public void logInfo(String str) {
		this.log(str, Level.INFO);
	}

	/**
	 * Sends a message of Level Warning to the logger
	 *
	 * @param 	String	str	message to send
	 * @deprecated	Use War.log() now
	 */
	@Deprecated
	public void logWarn(String str) {
		this.log(str, Level.WARNING);
	}

	/**
	 * Logs a specified message with a specified level
	 *
	 * @param 	String	str	message to log
	 * @param 	Level	lvl	level to use
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

	public Warzone matchWarzone(String warzoneSubString) {
		for (Warzone warzone : this.warzones) {
			if (warzone.getName().toLowerCase().startsWith(warzoneSubString.toLowerCase())) {
				return warzone;
			}
		}
		return null;
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

	public void setDefaultLifepool(int defaultLifepool) {
		this.defaultLifepool = defaultLifepool;
	}

	public int getDefaultLifepool() {
		return this.defaultLifepool;
	}

	public void setDefaultMonumentHeal(int defaultMonumentHeal) {
		this.defaultMonumentHeal = defaultMonumentHeal;
	}

	public int getDefaultMonumentHeal() {
		return this.defaultMonumentHeal;
	}

	public void setDefaultFriendlyFire(boolean defaultFriendlyFire) {
		this.defaultFriendlyFire = defaultFriendlyFire;
	}

	public boolean getDefaultFriendlyFire() {
		return this.defaultFriendlyFire;
	}

	public String getName() {
		return this.desc.getName();
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

	public boolean canPlayWar(Player player) {
		if (War.permissionHandler != null && (War.permissionHandler.has(player, "war.player") || War.permissionHandler.has(player, "War.player"))) {
			return true;
		}
		if (War.permissionHandler == null) {
			// w/o Permissions, everyone can play
			return true;
		}
		return false;
	}

	public boolean canWarp(Player player) {
		if (War.permissionHandler != null && (War.permissionHandler.has(player, "war.warp") || War.permissionHandler.has(player, "War.warp"))) {
			return true;
		}
		if (War.permissionHandler == null) {
			// w/o Permissions, everyone can warp
			return true;
		}
		return false;
	}

	public boolean canBuildOutsideZone(Player player) {
		if (this.isBuildInZonesOnly()) {
			if (War.permissionHandler != null && (War.permissionHandler.has(player, "war.build") || War.permissionHandler.has(player, "War.build"))) {
				return true;
			}
			// w/o Permissions, if buildInZonesOnly, no one can build outside the zone except Zonemakers
			return this.isZoneMaker(player);
		} else {
			return true;
		}
	}

	public boolean canPvpOutsideZones(Player player) {
		if (this.isPvpInZonesOnly()) {
			if (War.permissionHandler != null && (War.permissionHandler.has(player, "war.pvp") || War.permissionHandler.has(player, "War.pvp"))) {
				return true;
			}
			// w/o Permissions, if pvpInZoneOnly, no one can pvp outside the zone
			return false;
		} else {
			return true;
		}
	}

	public boolean isZoneMaker(Player player) {
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
		if (War.permissionHandler != null && (War.permissionHandler.has(player, "war.*") || War.permissionHandler.has(player, "War.*"))) {
			return true;
		} else {
			return player.isOp();
		}
	}

	public boolean getDefaultAutoAssignOnly() {

		return this.defaultAutoAssignOnly;
	}

	public void setDefaultAutoAssignOnly(boolean autoAssign) {
		this.defaultAutoAssignOnly = autoAssign;
	}

	public WarHub getWarHub() {
		return this.warHub;
	}

	public void setWarHub(WarHub warHub) {
		this.warHub = warHub;
	}

	public ZoneLobby lobby(Location location) {
		for (Warzone warzone : this.warzones) {
			if (warzone.getLobby() != null && warzone.getLobby().getVolume() != null && warzone.getLobby().getVolume().contains(location)) {
				return warzone.getLobby();
			}
		}
		return null;
	}

	public boolean inAnyWarzoneLobby(Location location) {
		if (this.lobby(location) == null) {
			return false;
		}
		return true;
	}

	public boolean inWarzoneLobby(String warzoneName, Location location) {
		ZoneLobby currentLobby = this.lobby(location);
		if (currentLobby == null) {
			return false;
		} else if (warzoneName.toLowerCase().equals(currentLobby.getZone().getName().toLowerCase())) {
			return true;
		}
		return false;
	}

	public void setDefaultTeamCap(int defaultTeamCap) {
		this.defaultTeamCap = defaultTeamCap;
	}

	public int getDefaultTeamCap() {
		return this.defaultTeamCap;
	}

	public void setPvpInZonesOnly(boolean pvpInZonesOnly) {
		this.pvpInZonesOnly = pvpInZonesOnly;
	}

	public boolean isPvpInZonesOnly() {
		return this.pvpInZonesOnly;
	}

	public void setDefaultScoreCap(int defaultScoreCap) {
		this.defaultScoreCap = defaultScoreCap;
	}

	public int getDefaultScoreCap() {
		return this.defaultScoreCap;
	}

	public List<String> getZoneMakersImpersonatingPlayers() {
		return this.zoneMakersImpersonatingPlayers;
	}

	public void setDefaultBlockHeads(boolean defaultBlockHeads) {
		this.defaultBlockHeads = defaultBlockHeads;
	}

	public boolean isDefaultBlockHeads() {
		return this.defaultBlockHeads;
	}

	public void setDefaultDropLootOnDeath(boolean defaultDropLootOnDeath) {
		this.defaultDropLootOnDeath = defaultDropLootOnDeath;
	}

	public boolean isDefaultDropLootOnDeath() {
		return this.defaultDropLootOnDeath;
	}

	public void setDefaultSpawnStyle(String defaultSpawnStyle) {
		this.defaultSpawnStyle = defaultSpawnStyle;
	}

	public String getDefaultSpawnStyle() {
		return this.defaultSpawnStyle;
	}

	public HashMap<Integer, ItemStack> getDefaultReward() {
		return this.defaultReward;
	}

	public List<Warzone> getIncompleteZones() {
		return this.incompleteZones;
	}

	public void setBuildInZonesOnly(boolean buildInZonesOnly) {
		this.buildInZonesOnly = buildInZonesOnly;
	}

	public boolean isBuildInZonesOnly() {
		return this.buildInZonesOnly;
	}

	public void setDisablePvpMessage(boolean disablePvpMessage) {
		this.disablePvpMessage = disablePvpMessage;
	}

	public boolean isDisablePvpMessage() {
		return this.disablePvpMessage;
	}

	public void setDefaultUnbreakableZoneBlocks(boolean defaultUnbreakableZoneBlocks) {
		this.defaultUnbreakableZoneBlocks = defaultUnbreakableZoneBlocks;
	}

	public boolean isDefaultUnbreakableZoneBlocks() {
		return this.defaultUnbreakableZoneBlocks;
	}

	public boolean getDefaultNoCreatures() {
		return this.isDefaultNoCreatures();
	}

	public void setDefaultNoCreatures(boolean defaultNoCreatures) {
		this.defaultNoCreatures = defaultNoCreatures;
	}

	public boolean isDefaultNoCreatures() {
		return this.defaultNoCreatures;
	}

	public void setDisconnected(HashMap<String, InventoryStash> disconnected) {
		this.disconnected = disconnected;
	}

	public HashMap<String, InventoryStash> getDisconnected() {
		return this.disconnected;
	}

	public void setLoaded(boolean loaded) {
		this.loaded = loaded;
	}

	public boolean isLoaded() {
		return this.loaded;
	}

	public void setDefaultResetOnEmpty(boolean defaultResetOnEmpty) {
		this.defaultResetOnEmpty = defaultResetOnEmpty;
	}

	public boolean isDefaultResetOnEmpty() {
		return this.defaultResetOnEmpty;
	}

	public void setDefaultResetOnLoad(boolean defaultResetOnLoad) {
		this.defaultResetOnLoad = defaultResetOnLoad;
	}

	public boolean isDefaultResetOnLoad() {
		return this.defaultResetOnLoad;
	}

	public void setDefaultResetOnUnload(boolean defaultResetOnUnload) {
		this.defaultResetOnUnload = defaultResetOnUnload;
	}

	public boolean isDefaultResetOnUnload() {
		return this.defaultResetOnUnload;
	}
}
