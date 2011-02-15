package bukkit.tommytony.war;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.nijikokun.bukkit.Permissions.Permissions;
import com.tommytony.war.ChatFixUtil;
import com.tommytony.war.Monument;
import com.tommytony.war.Team;
import com.tommytony.war.TeamChatColors;
import com.tommytony.war.TeamMaterials;
import com.tommytony.war.TeamSpawnStyles;
import com.tommytony.war.WarHub;
import com.tommytony.war.Warzone;
import com.tommytony.war.ZoneLobby;
import com.tommytony.war.mappers.VolumeMapper;
import com.tommytony.war.mappers.WarMapper;
import com.tommytony.war.mappers.WarzoneMapper;

/**
 * 
 * @author tommytony
 *
 */
public class War extends JavaPlugin {
	public static Permissions Permissions = null;
	
	public War(PluginLoader pluginLoader, Server instance,
			PluginDescriptionFile desc, File folder, File plugin, ClassLoader cLoader) {
		super(pluginLoader, instance, desc, folder, plugin, cLoader);
		this.desc = desc;
	}

	private WarPlayerListener playerListener = new WarPlayerListener(this);
	private WarEntityListener entityListener = new WarEntityListener(this);
	private WarBlockListener blockListener = new WarBlockListener(this);
    private Logger log;
    private PluginDescriptionFile desc = null;
    
    private final List<Warzone> warzones = new ArrayList<Warzone>();
    private final List<Warzone> incompleteZones = new ArrayList<Warzone>();
    private final List<String> zoneMakerNames = new ArrayList<String>();
    private final List<String> zoneMakersImpersonatingPlayers = new ArrayList<String>();
    private final HashMap<Integer, ItemStack> defaultLoadout = new HashMap<Integer, ItemStack>();
    private int defaultLifepool = 42;
    private boolean defaultFriendlyFire = false;
	private boolean defaultDrawZoneOutline = true;
	private boolean defaultAutoAssignOnly = false;
	private int defaultTeamCap = 7;
	private int defaultScoreCap = 10;
	private boolean defaultBlockHeads = false;
	private boolean defaultDropLootOnDeath = false;
	private String defaultSpawnStyle = TeamSpawnStyles.BIG;
	private final HashMap<Integer, ItemStack> defaultReward = new HashMap<Integer, ItemStack>();
	private boolean defaultUnbreakableZoneBlocks = false;
	
	private boolean pvpInZonesOnly = false;
	private boolean buildInZonesOnly = false;
	
	
	private WarHub warHub;
	
	public void onDisable() {
		for(Warzone warzone : warzones) {
			this.logInfo("Clearing zone " + warzone.getName() + "...");
			for(Team team : warzone.getTeams()) {
				for(Player player : team.getPlayers()) {
					warzone.handlePlayerLeave(player, warzone.getTeleport());
				}
			}
			if(warzone.getLobby() != null) {
				warzone.getLobby().getVolume().resetBlocks();
			}
			warzone.getVolume().resetBlocks();
		}
		if(warHub != null) {
			warHub.getVolume().resetBlocks();
		}
		this.logInfo("Done. War v" + desc.getVersion() + " is off.");
	}

	public void onEnable() {
		this.log = Logger.getLogger("Minecraft");
		this.setupPermissions();
		
		// Register hooks		
		PluginManager pm = getServer().getPluginManager();
		
		pm.registerEvent(Event.Type.PLAYER_JOIN, playerListener, Priority.Normal, this);
		pm.registerEvent(Event.Type.PLAYER_QUIT, playerListener, Priority.Normal, this);
		pm.registerEvent(Event.Type.PLAYER_MOVE, playerListener, Priority.Normal, this);
		
		pm.registerEvent(Event.Type.ENTITY_DAMAGEDBY_ENTITY, entityListener, Priority.Normal, this);
		pm.registerEvent(Event.Type.ENTITY_DAMAGEDBY_PROJECTILE, entityListener, Priority.Normal, this);
		pm.registerEvent(Event.Type.ENTITY_EXPLODE, entityListener, Priority.Normal, this);
		pm.registerEvent(Event.Type.ENTITY_DEATH, entityListener, Priority.Normal, this);
		pm.registerEvent(Event.Type.ENTITY_DAMAGED, entityListener, Priority.Normal, this);
		pm.registerEvent(Event.Type.ENTITY_COMBUST, entityListener, Priority.Normal, this);
		
		pm.registerEvent(Event.Type.BLOCK_PLACED, blockListener, Priority.Normal, this);
		//pm.registerEvent(Event.Type.BLOCK_BREAK, blockListener, Priority.Normal, this);
		pm.registerEvent(Event.Type.BLOCK_DAMAGED, blockListener, Priority.Normal, this);
		
		// Load files from disk or create them (using these defaults)
		this.defaultLoadout.put(0, new ItemStack(Material.STONE_SWORD, 1,  (byte) 8));
		this.defaultLoadout.put(1, new ItemStack(Material.BOW, 1, (byte) 8));
		this.defaultLoadout.put(2, new ItemStack(Material.ARROW, 7));
		this.defaultLoadout.put(3, new ItemStack(Material.IRON_PICKAXE, 1, (byte) 8));
		this.defaultLoadout.put(4, new ItemStack(Material.STONE_SPADE, 1, (byte) 8));
		this.defaultLifepool = 7;
		this.defaultFriendlyFire = false;
		this.defaultAutoAssignOnly = false;
		this.getDefaultReward().put(0, new ItemStack(Material.CAKE, 1));
		WarMapper.load(this);
		this.logInfo("Done. War v"+ desc.getVersion() + " is on.");
	}
	
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		if(sender instanceof Player) {
			Player player = (Player) sender;
			String command = cmd.getName();
			String[] arguments = null;
			// Handle both /war <command> and /<war command>. I.e. "/war zone temple" == "/zone temple"
			String helpMessage =  "War is on. Please pick your battle. " +
									"Try /warhub, /zones and /zone. Further instructions at war.tommytony.com/instructions.";
			if((command.equals("war") || command.equals("War")) && args.length > 1) {
				command = args[1];
				arguments = new String[args.length - 1];
				for(int i = 2; i <= arguments.length; i++) {
					arguments[i-1] = args[i];
				}
				if(arguments.length == 1 && (arguments[0].equals("help") || arguments[0].equals("h"))) {
					this.msg(player, helpMessage);
				}
			} else if (command.equals("war") || command.equals("War")) {
				this.msg(player, helpMessage);
			} else {
				arguments = args;
			}
		
			// Player commands: /warzones, /warzone, /teams, /join, /leave
			if(command.equals("zones") || command.equals("warzones")){
				performZones(player);
			} else if(command.equals("zone") || command.equals("warzone")) {
				performZone(player, arguments);
			} else if(command.equals("teams")){
				performTeams(player);
			} else if(command.equals("join") && canPlayWar(player)) {
				performJoin(player, arguments);
			} else if(command.equals("leave")) {
				performLeave(player);
			} else if(command.equals("team")) {
				performTeam(player, arguments);
			} else if(command.equals("warhub")) {
				performWarhub(player);
			} else if(this.isZoneMaker(player)) {			
				// Mod commands : /nextbattle
				if(command.equals("nextbattle")) {
					performNextBattle(player);
				}				
				// Warzone maker commands: /setzone, /savezone, /setteam, /setmonument, /resetzone
				else if(command.equals("setzone")) {
					performSetZone(player, arguments);
				} else if(command.equals("setzonelobby")) {
					performSetZoneLobby(player, arguments);
				} else if(command.equals("savezone")) {
					performSaveZone(player, arguments);
				} else if(command.equals("setzoneconfig")) {
					performSetZoneConfig(player, arguments);
				} else if(command.equals("resetzone")) {
					performResetZone(player, arguments);
				} else if(command.equals("deletezone")) {
					performDeleteZone(player);
				} else if(command.equals("setteam")) {
					performSetTeam(player, arguments);
				} else if(command.equals("setteamflag")) {
					performSetTeamFlag(player, arguments);
				} else if(command.equals("deleteteam")) {
					performDeleteTeam(player, arguments);
				} else if(command.equals("setmonument")) {
					performSetMonument(player, arguments);
				} else if(command.equals("deletemonument")) {
					performDeleteMonument(player, arguments);
				} else if(command.equals("setwarhub")) {
					performSetWarhub(player);
				} else if(command.equals("deletewarhub")) {
					performDeleteWarhub(player);
				} else if(command.equals("setwarconfig")) {
					performSetWarConfig(player, arguments);
				} else if(command.equals("zonemaker")) {
					performZonemakerAsZonemaker(player, arguments);
				}
			} else if (command.equals("setzone")		// Not a zone maker but War command.
						|| command.equals("nextbattle")
						|| command.equals("setzonelobby")
						|| command.equals("savezone")
						|| command.equals("setzoneconfig")
						|| command.equals("resetzone")
						|| command.equals("deletezone")
						|| command.equals("setteam")
						|| command.equals("deleteteam")
						|| command.equals("setmonument")
						|| command.equals("deletemonument")
						|| command.equals("setwarhub")
						|| command.equals("deletewarhub")
						|| command.equals("setwarconfig")) {
				this.badMsg(player, "You can't do this if you are not a warzone maker.");
			} else if (command.equals("zonemaker")) {
				performZonemakerAsPlayer(player);
			}
		}
		return true;
	}

	public void performZonemakerAsPlayer(Player player) {
		boolean wasImpersonating = false;
		for(String name : getZoneMakersImpersonatingPlayers()) {
			if(player.getName().equals(name)) {
				wasImpersonating = true;
			}
		}
		if(wasImpersonating) {
			getZoneMakersImpersonatingPlayers().remove(player.getName());
			msg(player, "You are back as a zone maker.");
		}
		WarMapper.save(this);
	}

	public void performZonemakerAsZonemaker(Player player, String[] arguments) {
		if(arguments.length > 2) {
			this.badMsg(player, "Usage: /zonemaker <player-name>, /zonemaker" +
					"Elevates the player to zone maker or removes his rights. " +
					"If you are already a zonemaker, you can toggle between player and zone maker modes by using the command without arguments.");
		} else {
			if(arguments.length == 1) {
				// make someone zonemaker or remove the right
				if(zoneMakerNames.contains(arguments[0])) {
					// kick
					zoneMakerNames.remove(arguments[0]);
					msg(player, arguments[0] + " is not a zone maker anymore.");
					Player kickedMaker = getServer().getPlayer(arguments[0]);
					if(kickedMaker != null) {
						msg(kickedMaker, player.getName() + " took away your warzone maker priviledges.");
					}
				} else {
					// add
					zoneMakerNames.add(arguments[0]);
					msg(player, arguments[0] + " is now a zone maker.");
					Player newMaker = getServer().getPlayer(arguments[0]);
					if(newMaker != null) {
						msg(newMaker, player.getName() + " made you warzone maker.");
					}
				}
			} else {
				// toggle to player mode
				if(isZoneMaker(player)) {
					getZoneMakersImpersonatingPlayers().add(player.getName());
				}
				msg(player, "You are now impersonating a regular player. Type /zonemaker again to toggle back to war maker mode.");
			}
			
			WarMapper.save(this);
		}
	}

	public void performSetWarConfig(Player player, String[] arguments) {
		if(arguments.length == 0) {
			this.badMsg(player, "Usage: /setwarconfig pvpinzonesonly:on lifepool:8 teamsize:5 maxscore:7 autoassign:on outline:off ff:on  " +
					"Changes the server defaults for new warzones. Please give at leaset one named parameter. Must be in warzone.");
		} else {
			if(updateFromNamedParams(arguments)) {
				WarMapper.save(this);
				this.msg(player, "War config saved.");
			} else {
				this.badMsg(player, "Failed to read named parameters.");
			}
		}
	}

	public void performDeleteWarhub(Player player) {
		if(warHub != null) {
			// reset existing hub
			warHub.getVolume().resetBlocks();
			VolumeMapper.delete(warHub.getVolume(), this);
			this.warHub = null;
			for(Warzone zone : warzones) {
				zone.getLobby().getVolume().resetBlocks();
				zone.getLobby().initialize();
			}
			
			this.msg(player, "War hub removed.");
		} else {
			this.badMsg(player, "No War hub to delete.");
		}
		WarMapper.save(this);
	}

	public void performSetWarhub(Player player) {
		if(warzones.size() > 0) {
			if(warHub != null) {
				// reset existing hub
				warHub.getVolume().resetBlocks();
				warHub.setLocation(player.getLocation());
				warHub.initialize();
				msg(player, "War hub moved.");
			} else {
				warHub = new WarHub(this, player.getLocation());
				warHub.initialize();
				for(Warzone zone : warzones) {
					zone.getLobby().getVolume().resetBlocks();
					zone.getLobby().initialize();
				}
				msg(player, "War hub created.");
			}
			WarMapper.save(this);
		} else {
			badMsg(player, "No warzones yet.");
		}
	}

	public void performDeleteMonument(Player player, String[] arguments) {
		if(arguments.length < 1 || (!this.inAnyWarzone(player.getLocation()) 
										&& !this.inAnyWarzoneLobby(player.getLocation()))) {
			this.badMsg(player, "Usage: /deletemonument <name>." +
					" Deletes the monument. " +
					"Must be in a warzone or lobby (try /warzones and /warzone). ");
		} else {
			String name = arguments[0];
			Warzone warzone = this.warzone(player.getLocation());
			ZoneLobby lobby = this.lobby(player.getLocation());
			if(warzone == null && lobby != null) {
				warzone = lobby.getZone();
			} else {
				lobby = warzone.getLobby();
			}
			Monument monument = warzone.getMonument(name);
			if(monument != null) {
				monument.getVolume().resetBlocks();
				warzone.getMonuments().remove(monument);
				WarzoneMapper.save(this, warzone, false);
				this.msg(player, "Monument " + name + " removed.");
			} else {
				this.badMsg(player, "No such monument.");
			}
		}
	}

	public void performSetMonument(Player player, String[] arguments) {
		if(!this.inAnyWarzone(player.getLocation()) || arguments.length < 1 || arguments.length > 1 
				|| (arguments.length == 1 && this.warzone(player.getLocation()) != null
						&& arguments[0].equals(this.warzone(player.getLocation()).getName()))) {
			this.badMsg(player, "Usage: /setmonument <name>. Creates or moves a monument. Monument can't have same name as zone. Must be in warzone.");
		} else {
			Warzone warzone = this.warzone(player.getLocation());
			String monumentName = arguments[0];
			if(warzone.hasMonument(monumentName)) {
				// move the existing monument
				Monument monument = warzone.getMonument(monumentName);
				monument.getVolume().resetBlocks();
				monument.setLocation(player.getLocation());
				this.msg(player, "Monument " + monument.getName() + " was moved.");
			} else {
				// create a new monument
				Monument monument = new Monument(arguments[0], this, warzone, player.getLocation());
				warzone.getMonuments().add(monument);
				this.msg(player, "Monument " + monument.getName() + " created.");
			}
			WarzoneMapper.save(this, warzone, false);
		}
	}

	public void performDeleteTeam(Player player, String[] arguments) {
		if(arguments.length < 1 || (!this.inAnyWarzone(player.getLocation()) 
										&& !this.inAnyWarzoneLobby(player.getLocation()))) {
			this.badMsg(player, "Usage: /deleteteam <team-name>." +
					" Deletes the team and its spawn. " +
					"Must be in a warzone or lobby (try /zones and /zone). ");
		} else {
			String name = TeamMaterials.teamMaterialToString(TeamMaterials.teamMaterialFromString(arguments[0]));
			Warzone warzone = this.warzone(player.getLocation());
			ZoneLobby lobby = this.lobby(player.getLocation());
			if(warzone == null && lobby != null) {
				warzone = lobby.getZone();
			} else {
				lobby = warzone.getLobby();
			}
			List<Team> teams = warzone.getTeams();
			Team team = null;
			for(Team t : teams) {
				if(name.equals(t.getName())) {
					team = t;
				}
			}
			if(team != null) {
				if(team.getFlagVolume() != null) team.getFlagVolume().resetBlocks();
				team.getSpawnVolume().resetBlocks();	
				warzone.getTeams().remove(team);
				if(warzone.getLobby() != null) {
					warzone.getLobby().getVolume().resetBlocks();
					warzone.getVolume().resetWallBlocks(warzone.getLobby().getWall());
					warzone.addZoneOutline(warzone.getLobby().getWall());
					warzone.getLobby().initialize();
				}
				WarzoneMapper.save(this, warzone, false);
				this.msg(player, "Team " + name + " removed.");
			} else {
				this.badMsg(player, "No such team.");
			}
		}
	}

	public void performSetTeamFlag(Player player, String[] arguments) {
		if(arguments.length < 1 || !this.inAnyWarzone(player.getLocation()) 
				|| (arguments.length > 0 && TeamMaterials.teamMaterialFromString(arguments[0]) == null)) {
			this.badMsg(player, "Usage: /setteamflag <diamond/iron/gold/d/i/g>. " +
					"Sets the team flag post to the current location. " +
					"Must be in a warzone (try /zones and /zone). ");
		} else {
			Material teamMaterial = TeamMaterials.teamMaterialFromString(arguments[0]);
			String name = TeamMaterials.teamMaterialToString(teamMaterial);					
			Warzone warzone = this.warzone(player.getLocation());
			Team team = warzone.getTeamByMaterial(teamMaterial);
			if(team == null) {
				// no such team yet			
				this.badMsg(player, "Place the team spawn first.");
			} else if (team.getFlagVolume() == null){
				// new team flag
				team.setTeamFlag(player.getLocation());
				Location playerLoc = player.getLocation();
				player.teleportTo(new Location(playerLoc.getWorld(), 
						playerLoc.getBlockX()+1, playerLoc.getBlockY(), playerLoc.getBlockZ()));
				this.msg(player, "Team " + name + " flag added here.");
				WarzoneMapper.save(this, warzone, false);
			} else {
				// relocate flag
				team.getFlagVolume().resetBlocks();
				team.setTeamFlag(player.getLocation());
				Location playerLoc = player.getLocation();
				player.teleportTo(new Location(playerLoc.getWorld(), 
						playerLoc.getBlockX()+1, playerLoc.getBlockY(), playerLoc.getBlockZ()+1));
				this.msg(player, "Team " + name + " flag moved.");
				WarzoneMapper.save(this, warzone, false);
			}
		}
	}

	public void performSetTeam(Player player, String[] arguments) {
		if(arguments.length < 1 || !this.inAnyWarzone(player.getLocation()) 
				|| (arguments.length > 0 && TeamMaterials.teamMaterialFromString(arguments[0]) == null)) {
			this.badMsg(player, "Usage: /setteam <diamond/iron/gold/d/i/g>. " +
					"Sets the team spawn to the current location. " +
					"Must be in a warzone (try /zones and /zone). ");
		} else {
			Material teamMaterial = TeamMaterials.teamMaterialFromString(arguments[0]);
			String name = TeamMaterials.teamMaterialToString(teamMaterial);					
			Warzone warzone = this.warzone(player.getLocation());
			Team existingTeam = warzone.getTeamByMaterial(teamMaterial);
			if(existingTeam != null) {
				// relocate
				existingTeam.setTeamSpawn(player.getLocation());
				this.msg(player, "Team " + existingTeam.getName() + " spawn relocated.");
			} else {
				// new team
				Team newTeam = new Team(name, teamMaterial, player.getLocation(), this, warzone);
				newTeam.setRemainingLives(warzone.getLifePool());
				warzone.getTeams().add(newTeam);
				if(warzone.getLobby() != null) {
					warzone.getLobby().getVolume().resetBlocks();
					warzone.getVolume().resetWallBlocks(warzone.getLobby().getWall());
					warzone.addZoneOutline(warzone.getLobby().getWall());
					warzone.getLobby().initialize();
				}
				newTeam.setTeamSpawn(player.getLocation());
				this.msg(player, "Team " + name + " created with spawn here.");
			}
			
			WarzoneMapper.save(this, warzone, false);
		}
	}

	public void performDeleteZone(Player player) {
		if(!this.inAnyWarzone(player.getLocation()) && !this.inAnyWarzoneLobby(player.getLocation())) {
			this.badMsg(player, "Usage: /deletezone. " +
					"Deletes the warzone. " +
					"Must be in the warzone (try /zones and /zone). ");
		} else {
			Warzone warzone = this.warzone(player.getLocation());
			ZoneLobby lobby = this.lobby(player.getLocation());
			if(warzone == null && lobby != null) {
				warzone = lobby.getZone();
			} else {
				lobby = warzone.getLobby();
			}
			for(Team t : warzone.getTeams()) {
				if(t.getTeamFlag() != null) t.getFlagVolume().resetBlocks();
				t.getSpawnVolume().resetBlocks();
			}
			for(Monument m : warzone.getMonuments()) {
				m.getVolume().resetBlocks();
			}
			if(warzone.getLobby() != null) {
				warzone.getLobby().getVolume().resetBlocks();
			}
			warzone.getVolume().resetBlocks();
			this.getWarzones().remove(warzone);
			WarMapper.save(this);
			WarzoneMapper.delete(this, warzone.getName());
			if(warHub != null) {	// warhub has to change
				warHub.getVolume().resetBlocks();
				warHub.initialize();
			}
			this.msg(player, "Warzone " + warzone.getName() + " removed.");
		}
	}

	public void performResetZone(Player player, String[] arguments) {
		if(!this.inAnyWarzone(player.getLocation()) && !this.inAnyWarzoneLobby(player.getLocation())) {
			this.badMsg(player, "Usage: /resetzone <hard/h>. Reloads the zone (from disk if the hard option is specified). Must be in warzone or lobby.");
		} else {
			Warzone warzone = this.warzone(player.getLocation());
			ZoneLobby lobby = this.lobby(player.getLocation());
			if(warzone == null && lobby != null) {
				warzone = lobby.getZone();
			} else {
				lobby = warzone.getLobby();
			}
			int resetBlocks = 0;
			warzone.clearFlagThieves();
			for(Team team: warzone.getTeams()) {
				team.teamcast("The war has ended. " + playerListener.getAllTeamsMsg(player) + " Resetting warzone " + warzone.getName() + " and teams...");
				for(Player p : team.getPlayers()) {
					warzone.restorePlayerInventory(p);
					p.teleportTo(warzone.getTeleport());
					this.msg(player, "You have left the warzone. Your inventory has (hopefully) been restored.");
				}
				team.setPoints(0);
				team.getPlayers().clear();
			}
			
			Warzone resetWarzone = null;
			this.msg(player, "Reloading warzone " + warzone.getName() + ".");
			if(arguments.length == 1 && (arguments[0].equals("hard") || arguments[0].equals("h"))) {
				// reset from disk
				this.getWarzones().remove(warzone);
				resetWarzone = WarzoneMapper.load(this, warzone.getName(), true);
				this.getWarzones().add(resetWarzone);
				resetBlocks = warzone.getVolume().resetBlocks();
				if(lobby!=null) {
					lobby.getVolume().resetBlocks();
				}
				resetWarzone.initializeZone();
			} else {
				resetBlocks = warzone.getVolume().resetBlocks();
				if(lobby!=null) {
					lobby.getVolume().resetBlocks();
				}
				warzone.initializeZone();
				
			}
			
			this.msg(player, "Warzone and teams reset. " + resetBlocks + " blocks reset.");
			logInfo(resetBlocks + " blocks reset in warzone " + warzone.getName() + ".");
		}
	}

	public void performSetZoneConfig(Player player, String[] arguments) {
		if((!this.inAnyWarzone(player.getLocation()) && !this.inAnyWarzoneLobby(player.getLocation()))
			|| arguments.length == 0) {
			this.badMsg(player, "Usage: /setzoneconfig lifepool:8 teamsize:5 maxscore:7 autoassign:on outline:off ff:on  " +
					"Please give at leaset one named parameter. Does not save the blocks of the warzone. Resets the zone with the new config. Must be in warzone.");
		} else {
			Warzone warzone = this.warzone(player.getLocation());
			ZoneLobby lobby = this.lobby(player.getLocation());
			if(warzone == null && lobby != null) {
				warzone = lobby.getZone();
			} else {
				lobby = warzone.getLobby();
			}
			if(updateZoneFromNamedParams(warzone, arguments)) {
				this.msg(player, "Saving config and resetting warzone " + warzone.getName() + ".");
				WarzoneMapper.save(this, warzone, false);
				warzone.getVolume().resetBlocks();
				if(lobby != null) {
					lobby.getVolume().resetBlocks();
				}
				warzone.initializeZone();	// bring back team spawns etc
				this.msg(player, "Warzone config saved. Zone reset.");
				
				if(warHub != null) { // maybe the zone was disabled/enabled
					warHub.getVolume().resetBlocks();
					warHub.initialize();
				}
			} else {
				this.badMsg(player, "Failed to read named parameters.");
			}
		}
	}

	public void performSaveZone(Player player, String[] arguments) {
		if(!this.inAnyWarzone(player.getLocation()) && !this.inAnyWarzoneLobby(player.getLocation())) {
			this.badMsg(player, "Usage: /savezone lifepool:8 teamsize:5 maxscore:7 autoassign:on outline:off ff:on " +
					"All named params optional. Saves the blocks of the warzone (i.e. the current zone state will be reloaded at each battle start). Must be in warzone.");
		} else {
			Warzone warzone = this.warzone(player.getLocation());
			ZoneLobby lobby = this.lobby(player.getLocation());
			if(warzone == null && lobby != null) {
				warzone = lobby.getZone();
			} else {
				lobby = warzone.getLobby();
			}
			this.msg(player, "Saving warzone " + warzone.getName() + ".");
			int savedBlocks = warzone.saveState(true);
			updateZoneFromNamedParams(warzone, arguments);
			WarzoneMapper.save(this, warzone, true);
			warzone.getVolume().resetBlocks();
			if(lobby != null) {
				lobby.getVolume().resetBlocks();
			}
			warzone.initializeZone();	// bring back team spawns etc
			
			if(warHub != null) { // maybe the zone was disabled/enabled
				warHub.getVolume().resetBlocks();
				warHub.initialize();
			}
			
			this.msg(player, "Warzone " + warzone.getName() + " initial state changed. Saved " + savedBlocks + " blocks.");
		}
	}

	public void performSetZoneLobby(Player player, String[] arguments) {
		if((!this.inAnyWarzone(player.getLocation())
				&& !this.inAnyWarzoneLobby(player.getLocation()))
				|| arguments.length < 1 || arguments.length > 1 
				|| (arguments.length == 1 && !arguments[0].equals("north") && !arguments[0].equals("n")
														&& !arguments[0].equals("east") && !arguments[0].equals("e")
														&& !arguments[0].equals("south") && !arguments[0].equals("s")
														&& !arguments[0].equals("west") && !arguments[0].equals("w"))) {
			this.badMsg(player, "Usage: /setzonelobby <north/n/east/e/south/s/west/w>. Must be in warzone." +
					"Defines on which side the zone lobby lies. " +
					"Removes any previously set lobby.");
		} else {
			Warzone warzone = this.warzone(player.getLocation());
			ZoneLobby lobby = this.lobby(player.getLocation());
			if(warzone == null && lobby != null) {
				warzone = lobby.getZone();
			} else {
				lobby = warzone.getLobby();
			}
			BlockFace wall = BlockFace.WEST;
			String wallStr = "";
			if(arguments[0].equals("north") || arguments[0].equals("n")) {
				wall = BlockFace.NORTH;
				wallStr = "north";
			} else if(arguments[0].equals("east") || arguments[0].equals("e")) {
				wall = BlockFace.EAST;
				wallStr = "east";
			} else if(arguments[0].equals("south") || arguments[0].equals("s")) {
				wall = BlockFace.SOUTH;
				wallStr = "south";
			} else if(arguments[0].equals("west") || arguments[0].equals("w")) {
				wall = BlockFace.WEST;
				wallStr = "west";
			}				
			if(lobby != null) {
				// reset existing lobby
				lobby.getVolume().resetBlocks();
				lobby.changeWall(wall);
				lobby.initialize();
				this.msg(player, "Warzone lobby moved to " + wallStr + " side of zone.");
			} else {
				// new lobby
				lobby = new ZoneLobby(this, warzone, wall);
				warzone.setLobby(lobby);
				lobby.initialize();
				if(warHub != null) { // warhub has to change
					warHub.getVolume().resetBlocks();
					warHub.initialize();
				}
				this.msg(player, "Warzone lobby created on " + wallStr + "side of zone.");
			}
			WarzoneMapper.save(this, warzone, false);
		}
	}

	public void performSetZone(Player player, String[] arguments) {
		if(arguments.length < 2 || arguments.length > 2 
				|| (arguments.length == 2 && (!arguments[1].equals("southeast") && !arguments[1].equals("northwest")
														&& !arguments[1].equals("se") && !arguments[1].equals("nw")))) {
			this.badMsg(player, "Usage: /setzone <warzone-name> <'southeast'/'northwest'/'se'/'nw'>. " +
					"Set one corner, then the next. Defines the outline of the warzone, which will be reset at the start of every battle. " +
					"Saves the zone blocks if the zone if the outline is correct.");
		} else {
			Warzone warzone = this.findWarzone(arguments[0]);
			if(warzone == null) {
				// create the warzone
				warzone = new Warzone(this, player.getLocation().getWorld(), arguments[0]);
				this.getIncompleteZones().add(warzone);
				//WarMapper.save(this);
				if(arguments[1].equals("northwest") || arguments[1].equals("nw")) {
					warzone.setNorthwest(player.getLocation());
					this.msg(player, "Warzone " + warzone.getName() + " created. Northwesternmost point set to x:" 
							+ (int)warzone.getNorthwest().getBlockX() + " z:" + (int)warzone.getNorthwest().getBlockZ() + ".");
				} else {
					warzone.setSoutheast(player.getLocation());
					this.msg(player, "Warzone " + warzone.getName() + " created. Southeasternmost point set to x:" 
							+ (int)warzone.getSoutheast().getBlockX() + " z:" + (int)warzone.getSoutheast().getBlockZ() + ".");
				}
				//WarzoneMapper.save(this, warzone, false);
			} else {
				// change existing warzone
				if(arguments[1].equals("northwest") || arguments[1].equals("nw")) {
					if(warzone.getSoutheast() != null 
							&& (player.getLocation().getBlockX() >= warzone.getSoutheast().getBlockX()
									|| player.getLocation().getBlockZ() <= warzone.getSoutheast().getBlockZ())) {
						this.badMsg(player, "You must place that corner northwest relative to the existing southeast corner!");
					} else if (warzone.getSoutheast() == null){
						// just moving the single nw corner we've placed so far
						warzone.setNorthwest(player.getLocation());
					}else {
						String msgString = "";
						if(warzone.getVolume().isSaved()) {
							msg(player, "Resetting " + warzone.getName() + " blocks.");
							if(warzone.getLobby() != null) {
								warzone.getLobby().getVolume().resetBlocks();
							}
							int reset = warzone.getVolume().resetBlocks();
							
							msgString = reset + " blocks reset. ";
						} else {
							this.addWarzone(warzone);
							this.incompleteZones.remove(warzone);
							WarMapper.save(this);
						}
						warzone.setNorthwest(player.getLocation());
						if(warzone.tooSmall()) {
							badMsg(player, "Warzone " + warzone.getName() + " is too small. Min north-south size: 20. Min east-west size: 20.");
						} else if (warzone.tooBig()) {
							badMsg(player, "Warzone " + warzone.getName() + " is too Big. Max north-south size: 500. Max east-west size: 500.");
						}
						else {
							msgString += "New zone outline ok. Northwesternmost point of zone " + warzone.getName() + " set to x:" + (int)warzone.getNorthwest().getBlockX()
								+ " z:" + (int)warzone.getNorthwest().getBlockZ()+ ". Saving new warzone blocks...";
							msg(player, msgString);
							WarzoneMapper.save(this, warzone, false);
						}
					} 
				} else if(arguments[1].equals("southeast") || arguments[1].equals("se")) {
					if(warzone.getNorthwest() != null 
							&& (player.getLocation().getBlockX() <= warzone.getNorthwest().getBlockX()
									|| player.getLocation().getBlockZ() >= warzone.getNorthwest().getBlockZ())) {
						this.badMsg(player, "You must place that corner southeast relative to the existing northwest corner! ");
					} else if (warzone.getNorthwest() == null){
						// just moving the single se corner we've placed so far
						warzone.setSoutheast(player.getLocation());
					} else {
						String msgString = "";
						if(warzone.getVolume().isSaved()) {
							msg(player, "Resetting zone " + warzone.getName() + " blocks.");
							if(warzone.getLobby() != null) {
								warzone.getLobby().getVolume().resetBlocks();
							}
							int reset = warzone.getVolume().resetBlocks();
							
							msgString = reset + " blocks reset. ";
						} else {
							this.addWarzone(warzone);
							this.incompleteZones.remove(warzone);
							WarMapper.save(this);
						}
						warzone.setSoutheast(player.getLocation());
						if(warzone.tooSmall()) {
							badMsg(player, "Warzone " + warzone.getName() + " is too small. Min north-south size: 20. Min east-west size: 20.");
						} else if (warzone.tooBig()) {
							badMsg(player, "Warzone " + warzone.getName() + " is too Big. Max north-south size: 500. Max east-west size: 500.");
						}
						else {
							msgString += "New zone outline ok. Southeasternmost point of zone " + warzone.getName() + " set to x:" + (int)warzone.getSoutheast().getBlockX()
								+ " z:" + (int)warzone.getSoutheast().getBlockZ()+ ". Saving new warzone blocks...";
							msg(player, msgString);
							WarzoneMapper.save(this, warzone, false);
						}
					}
				}
			}
			if(warzone.getNorthwest() == null) {
				msg(player, "Still missing northwesternmost point.");
			}
			if(warzone.getSoutheast() == null) {
				msg(player, "Still missing southeasternmost point.");
			}
			if(warzone.getNorthwest() != null && warzone.getSoutheast() != null) {
				if(warzone.ready()) {
					warzone.saveState(false); // we just changed the volume, cant reset walls 
					if(warzone.getLobby() == null) {
						// Set default lobby on south side
						ZoneLobby lobby = new ZoneLobby(this, warzone, BlockFace.SOUTH);
						warzone.setLobby(lobby);
						//lobby.initialize();
						if(warHub != null) {	// warhub has to change
							warHub.getVolume().resetBlocks();
							warHub.initialize();
						}
						this.msg(player, "Default lobby created on south side of zone. Use /setzonelobby <n/s/e/w> to change which zone wall it is attached to.");
					} else {
						// gotta move the lobby
						warzone.getLobby().changeWall(warzone.getLobby().getWall());
					}
					warzone.initializeZone();
					WarzoneMapper.save(this, warzone, true);
					this.msg(player, "Warzone saved. Use /setteam, /setmonument and /savezone to configure the zone.");
				}
			}
		}
	}

	public void performNextBattle(Player player) {
		if(!this.inAnyWarzone(player.getLocation())) {
			this.badMsg(player, "Usage: /nextbattle. Resets the zone blocks and all teams' life pools. Must be in warzone.");
		} else {
			Warzone warzone = this.warzone(player.getLocation());
			warzone.clearFlagThieves();
			for(Team team: warzone.getTeams()) {
				team.teamcast("The battle was interrupted. " + playerListener.getAllTeamsMsg(player) + " Resetting warzone " + warzone.getName() + " and life pools...");
			}
			int resetBlocks = warzone.getVolume().resetBlocks();
			warzone.initializeZone();
			this.msg(player, "Warzone reset. " + resetBlocks + " blocks reset.");
			logInfo(resetBlocks + " blocks reset in warzone " + warzone.getName() + ".");
		}
	}

	public void performWarhub(Player player) {
		if(this.getWarHub() == null) {
			this.badMsg(player, "No warhub on this War server. Try /zones and /zone.");
		} else if(!canWarp(player)) {
			this.badMsg(player, "Can't warp to warhub. You need the 'war.warp' permission.");
		} else {
			Team playerTeam = this.getPlayerTeam(player.getName());
			Warzone playerWarzone = getPlayerTeamWarzone(player.getName());
			if(playerTeam != null) { // was in zone
				playerWarzone.handlePlayerLeave(player, this.getWarHub().getLocation());
			}
			player.teleportTo(this.getWarHub().getLocation());
		}
	}

	public void performTeam(Player player, String[] arguments) {
		Team playerTeam = this.getPlayerTeam(player.getName());
		if(playerTeam == null) {
			this.badMsg(player, "Usage: /team <message>. " +
					"Sends a message only to your teammates.");
		} else {
			ChatColor color = null;
			if(playerTeam.getMaterial() == TeamMaterials.TEAMDIAMOND) {
				color = TeamChatColors.TEAMDIAMOND;
			} else if(playerTeam.getMaterial() == TeamMaterials.TEAMGOLD) {
				color = TeamChatColors.TEAMGOLD;
			} else if(playerTeam.getMaterial() ==  TeamMaterials.TEAMIRON) {
				color = TeamChatColors.TEAMIRON;
			}
			String teamMessage = color + player.getName() + ": " + ChatColor.WHITE;
			for(int j = 0 ; j<arguments.length; j++) {
				String part = arguments[j];
				teamMessage += part + " ";
			}
			playerTeam.teamcast(teamMessage);
		}
	}

	public void performLeave(Player player) {
		if(!this.inAnyWarzone(player.getLocation()) || this.getPlayerTeam(player.getName()) == null) {
			this.badMsg(player, "Usage: /leave. " +
					"Must be in a team already.");
		} else {
			Warzone zone = getPlayerTeamWarzone(player.getName());
			zone.handlePlayerLeave(player, zone.getTeleport());
		}
	}

	public void performJoin(Player player, String[] arguments) {
		if(arguments.length < 1 || (!this.inAnyWarzone(player.getLocation()) && !this.inAnyWarzoneLobby(player.getLocation()))
				|| (arguments.length > 0 && TeamMaterials.teamMaterialFromString(arguments[0]) == null)) {
			this.badMsg(player, "Usage: /join <diamond/iron/gold/d/i/g>." +
					" Teams are warzone specific." +
					" You must be inside a warzone or zone lobby to join a team." +
					" Use as an alternative to walking through the team gate.");
		} else {				
			// drop from old team if any
			Team previousTeam = this.getPlayerTeam(player.getName());
			if(previousTeam != null) {
				Warzone zone = this.getPlayerTeamWarzone(player.getName());
				if(!previousTeam.removePlayer(player.getName())){
					logWarn("Could not remove player " + player.getName() + " from team " + previousTeam.getName());
				}						
				if(zone.isFlagThief(player.getName())) {
					Team victim = zone.getVictimTeamForThief(player.getName());
					victim.getFlagVolume().resetBlocks();
					victim.initializeTeamFlag();
					zone.removeThief(player.getName());
					for(Team t : zone.getTeams()) {
						t.teamcast("Team " + victim.getName() + " flag was returned.");
					}
				}
				previousTeam.resetSign();
			}
			
			// join new team
			String name = TeamMaterials.teamMaterialToString(TeamMaterials.teamMaterialFromString(arguments[0]));
			Warzone warzone = this.warzone(player.getLocation());
			ZoneLobby lobby = this.lobby(player.getLocation());
			if(warzone == null && lobby != null) {
				warzone = lobby.getZone();
			} else {
				lobby = warzone.getLobby();
			}
			if(warzone.isDisabled()) {
				badMsg(player, "This warzone is disabled.");
			} else {
				List<Team> teams = warzone.getTeams();
				boolean foundTeam = false;
				for(Team team : teams) {
					if(team.getName().equals(name)) {
						if(!warzone.hasPlayerInventory(player.getName())) {
							warzone.keepPlayerInventory(player);
							this.msg(player, "Your inventory is is storage until you /leave.");
						}
						if(team.getPlayers().size() < warzone.getTeamCap()) {
							team.addPlayer(player);
							team.resetSign();
							warzone.respawnPlayer(team, player);
							if(warHub != null) {
								warHub.resetZoneSign(warzone);
							}
							foundTeam = true;
						} else {
							this.badMsg(player, "Team " + name + " is full.");
							foundTeam = true;
						}
					}
				}
				if(foundTeam) {
					for(Team team : teams){
						team.teamcast("" + player.getName() + " joined " + name);
					}
				} else {
					this.badMsg(player, "No such team. Try /teams.");
				}
			}
		}
	}

	public void performTeams(Player player) {
		if(!this.inAnyWarzone(player.getLocation()) && !this.inAnyWarzoneLobby(player.getLocation())) {
			this.badMsg(player, "Usage: /teams. " +
					"Must be in a warzone or zone lobby (try /war, /zones and /zone).");
		} else {
			this.msg(player, "" + playerListener.getAllTeamsMsg(player));
		}
	}

	public void performZone(Player player, String[] arguments) {
		if(arguments.length < 1) {
			this.badMsg(player, "Usage: /zone <warzone-name>.");
		} else if(!canWarp(player)) {
			this.badMsg(player, "Can't warp to zone. You need the 'war.warp' permission.");
		} else {
			boolean warped = false;
			for(Warzone warzone : this.getWarzones()) {
				if(warzone.getName().equals(arguments[0]) && warzone.getTeleport() != null){
					Team playerTeam = getPlayerTeam(player.getName());
					if(playerTeam != null) {
						Warzone playerWarzone = getPlayerTeamWarzone(player.getName());
						playerWarzone.handlePlayerLeave(player, warzone.getTeleport());
					} else {					
						player.teleportTo(warzone.getTeleport());
					}
					warped = true;
				}
			}
			if(!warped) {
				this.badMsg(player, "No such warzone.");
			}
		}
	}

	private void performZones(Player player) {
		String warzonesMessage = "Warzones: ";
		if(this.getWarzones().isEmpty()){
			warzonesMessage += "none.";
		}
		for(Warzone warzone : this.getWarzones()) {
			
			warzonesMessage += warzone.getName() + " ("
			+ warzone.getTeams().size() + " teams, ";
			int playerTotal = 0;
			for(Team team : warzone.getTeams()) {
				playerTotal += team.getPlayers().size();
			}
			warzonesMessage += playerTotal + " players)  ";
		}
		this.msg(player, warzonesMessage + "  Use /zone <zone-name> to " +
				"teleport to a warzone. ");
	}

	private boolean updateZoneFromNamedParams(Warzone warzone, String[] arguments) {
		try {
			Map<String,String> namedParams = new HashMap<String,String>();
			for(String namedPair : arguments) {
				String[] pairSplit = namedPair.split(":");
				if(pairSplit.length == 2) {
					namedParams.put(pairSplit[0].toLowerCase(), pairSplit[1]);
				}
			}
			if(namedParams.containsKey("lifepool")){
				warzone.setLifePool(Integer.parseInt(namedParams.get("lifepool")));
			}
			if(namedParams.containsKey("teamsize")){
				warzone.setTeamCap(Integer.parseInt(namedParams.get("teamsize")));
			}
			if(namedParams.containsKey("maxscore")){
				warzone.setScoreCap(Integer.parseInt(namedParams.get("maxscore")));
			}
			if(namedParams.containsKey("ff")){
				String onOff = namedParams.get("ff");
				warzone.setFriendlyFire(onOff.equals("on") || onOff.equals("true"));
			}
			if(namedParams.containsKey("autoassign")){
				String onOff = namedParams.get("autoassign");
				warzone.setAutoAssignOnly(onOff.equals("on") || onOff.equals("true"));
			}
			if(namedParams.containsKey("outline")){
				String onOff = namedParams.get("outline");
				warzone.setDrawZoneOutline(onOff.equals("on") || onOff.equals("true"));
			}
			if(namedParams.containsKey("blockheads")){
				String onOff = namedParams.get("blockheads");
				warzone.setBlockHeads(onOff.equals("on") || onOff.equals("true"));
			}
			if(namedParams.containsKey("spawnstyle")) {
				String spawnStyle = namedParams.get("spawnstyle").toLowerCase();
				if(spawnStyle.equals(TeamSpawnStyles.SMALL)) {
					warzone.setSpawnStyle(spawnStyle);
				} else if (spawnStyle.equals(TeamSpawnStyles.FLAT)){
					warzone.setSpawnStyle(spawnStyle);
				} else {
					warzone.setSpawnStyle(TeamSpawnStyles.BIG);
				}
			}
			if(namedParams.containsKey("unbreakable")) {
				String onOff = namedParams.get("unbreakable");
				warzone.setUnbreakableZoneBlocks(onOff.equals("on") || onOff.equals("true"));
			}
			if(namedParams.containsKey("disabled")) {
				String onOff = namedParams.get("disabled");
				warzone.setDisabled(onOff.equals("on") || onOff.equals("true"));
			}
//			if(namedParams.containsKey("dropLootOnDeath")){
//				String onOff = namedParams.get("dropLootOnDeath");
//				warzone.setDropLootOnDeath(onOff.equals("on") || onOff.equals("true"));
//			}
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	
	private boolean updateFromNamedParams(String[] arguments) {
		try {
			Map<String,String> namedParams = new HashMap<String,String>();
			for(String namedPair : arguments) {
				String[] pairSplit = namedPair.split(":");
				if(pairSplit.length == 2) {
					namedParams.put(pairSplit[0].toLowerCase(), pairSplit[1]);
				}
			}
			if(namedParams.containsKey("lifepool")){
				setDefaultLifepool(Integer.parseInt(namedParams.get("lifepool")));
			} 
			if(namedParams.containsKey("teamsize")){
				setDefaultTeamCap(Integer.parseInt(namedParams.get("teamsize")));
			}
			if(namedParams.containsKey("maxscore")){
				setDefaultScoreCap(Integer.parseInt(namedParams.get("maxscore")));
			}
			if(namedParams.containsKey("ff")){
				String onOff = namedParams.get("ff");
				setDefaultFriendlyFire(onOff.equals("on"));
			} 
			if(namedParams.containsKey("autoassign")){
				String onOff = namedParams.get("autoassign");
				setDefaultAutoAssignOnly(onOff.equals("on") || onOff.equals("true"));
			} 
			if(namedParams.containsKey("outline")){
				String onOff = namedParams.get("outline");
				setDefaultDrawZoneOutline(onOff.equals("on") || onOff.equals("true"));
			} 
			if(namedParams.containsKey("pvpinzonesonly")){
				String onOff = namedParams.get("pvpinzonesonly");
				setPvpInZonesOnly(onOff.equals("on") || onOff.equals("true"));
			} 
			if(namedParams.containsKey("blockheads")){
				String onOff = namedParams.get("blockheads");
				setDefaultBlockHeads(onOff.equals("on") || onOff.equals("true"));
			}
			if(namedParams.containsKey("spawnstyle")){
				String spawnStyle = namedParams.get("spawnstyle").toLowerCase();
				if(spawnStyle.equals(TeamSpawnStyles.SMALL)) {
					setDefaultSpawnStyle(spawnStyle);
				} else if (spawnStyle.equals(TeamSpawnStyles.FLAT)){
					setDefaultSpawnStyle(spawnStyle);
				} else {
					setDefaultSpawnStyle(TeamSpawnStyles.BIG);
				}
			}
			if(namedParams.containsKey("buildinzonesonly")) {
				String onOff = namedParams.get("buildinzonesonly");
				setBuildInZonesOnly(onOff.equals("on") || onOff.equals("true"));
			}
			if(namedParams.containsKey("unbreakable")) {
				String onOff = namedParams.get("unbreakable");
				setDefaultUnbreakableZoneBlocks(onOff.equals("on") || onOff.equals("true"));
			}
//			if(namedParams.containsKey("dropLootOnDeath")){
//				String onOff = namedParams.get("dropLootOnDeath");
//				setDefaultDropLootOnDeath(onOff.equals("on") || onOff.equals("true"));
//			}
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public Team getPlayerTeam(String playerName) {
		for(Warzone warzone : warzones) {
			Team team = warzone.getPlayerTeam(playerName);
			if(team != null) return team;
		}
		return null;
	}
	
	public Warzone getPlayerTeamWarzone(String playerName) {
		for(Warzone warzone : warzones) {
			Team team = warzone.getPlayerTeam(playerName);
			if(team != null) return warzone;
		}
		return null;
	}	

	public Logger getLogger() {
		return log;
	}
	
	public Warzone warzone(Location location) {
		for(Warzone warzone : warzones) {
			if(warzone.getVolume() != null && warzone.getVolume().contains(location)) return warzone;
		}
		return null;
	}

	public boolean inAnyWarzone(Location location) {
		Block locBlock = location.getWorld().getBlockAt(location.getBlockX(), location.getBlockY(), location.getBlockZ());
		Warzone currentZone = warzone(location);
		if(currentZone == null) {
			return false; 
		} else if (currentZone.getVolume().isWallBlock(locBlock)) {
			return false;	// wall block doesnt count. this lets people in at the lobby side wall because wall gates overlap with the zone.
		}
		return true;
	}
	
	public boolean inWarzone(String warzoneName, Location location) {
		Warzone currentZone = warzone(location);
		if(currentZone == null) {
			return false;
		} else if (warzoneName.equals(currentZone.getName())){
			return true;
		}
		return false;
	}

	public void addWarzone(Warzone zone) {
		warzones.add(zone);
	}

	public List<Warzone> getWarzones() {
		return warzones;
	}
	
	public void msg(Player player, String str) {
		String out = ChatColor.GRAY + "[War] " + ChatColor.WHITE + colorTeams(str, ChatColor.WHITE) + " ";
		ChatFixUtil.sendMessage(player, out);
	}
	
	public void badMsg(Player player, String str) {
		String out = ChatColor.GRAY + "[War] " + ChatColor.RED + colorTeams(str, ChatColor.RED) + " ";
		ChatFixUtil.sendMessage(player, out);
	}
	
	private String colorTeams(String str, ChatColor msgColor) {
		String out = str.replaceAll("iron", TeamChatColors.TEAMIRON + "iron" + msgColor);
		out = out.replaceAll("Iron", TeamChatColors.TEAMIRON + "Iron" + msgColor);
		out = out.replaceAll("gold", TeamChatColors.TEAMGOLD + "gold" + msgColor);
		out = out.replaceAll("Gold", TeamChatColors.TEAMGOLD + "Gold" + msgColor);
		out = out.replaceAll("diamond", TeamChatColors.TEAMDIAMOND + "diamond" + msgColor);
		out = out.replaceAll("Diamond", TeamChatColors.TEAMDIAMOND + "Diamond" + msgColor);
		return out;
	}
	
	public void logInfo(String str) {
		this.getLogger().log(Level.INFO, "[War] " + str);
	}
	
	public void logWarn(String str) {
		this.getLogger().log(Level.WARNING, "[War] " + str);
	}
	
	// the only way to find a zone that has only one corner
	public Warzone findWarzone(String warzoneName) {
		for(Warzone warzone : warzones) {
			if(warzone.getName().equals(warzoneName)) {
				return warzone;
			}
		}
		for(Warzone warzone : incompleteZones) {	
			if(warzone.getName().equals(warzoneName)) {
				return warzone;
			}
		}
		return null;
	}

	public HashMap<Integer, ItemStack> getDefaultLoadout() {
		return defaultLoadout;
	}

	public void setDefaultLifepool(int defaultLifepool) {
		this.defaultLifepool = defaultLifepool;
	}

	public int getDefaultLifepool() {
		return defaultLifepool;
	}

	public void setDefaultFriendlyFire(boolean defaultFriendlyFire) {
		this.defaultFriendlyFire = defaultFriendlyFire;
	}

	public boolean getDefaultFriendlyFire() {
		return defaultFriendlyFire;
	}

	public String getName() {
		return desc.getName();
	}

	public Warzone zoneOfZoneWallAtProximity(Location location) {
		for(Warzone zone : warzones) {
			if(zone.isNearWall(location)) return zone;
		}
		return null;
	}

	public List<String> getZoneMakerNames() {
		return zoneMakerNames;
	}
	
	public boolean canPlayWar(Player player) {
		if(Permissions != null 
				&& (Permissions.Security.permission(player, "war.player")
						|| Permissions.Security.permission(player, "War.player"))) {
			return true;
		}
		if(Permissions == null) {
			// w/o Permissions, everyone can play
			return true;
		}
		return false;
	}
	
	public boolean canWarp(Player player) {
		if(Permissions != null 
				&& (Permissions.Security.permission(player, "war.warp")
						|| Permissions.Security.permission(player, "War.warp"))) {
			return true;
		}
		if(Permissions == null) {
			// w/o Permissions, everyone can warp
			return true;
		}
		return false;
	}
	
	public boolean canBuildOutsideZone(Player player) {
		if(isBuildInZonesOnly()) {
			if(Permissions != null 
					&& (Permissions.Security.permission(player, "war.build")
							|| Permissions.Security.permission(player, "War.build"))) {
				return true;
			}
			// w/o Permissions, if buildInZonesOnly, no one can build outside the zone
			return false;
		} else {
			return true;
		}
	}
	
	public boolean isZoneMaker(Player player) {
		boolean isPlayerImpersonator = false;
		for(String disguised : zoneMakersImpersonatingPlayers) {
			if(disguised.equals(player.getName())) isPlayerImpersonator = true;
		}
		if(!isPlayerImpersonator) {
			for(String zoneMaker : zoneMakerNames) {
				if(zoneMaker.equals(player.getName())) return true;
			}
			if(Permissions != null 
					&& (Permissions.Security.permission(player, "war.*")
							|| Permissions.Security.permission(player, "War.*"))) {
				return true;
			}
		}
		return false;			
	}

	public boolean getDefaultDrawZoneOutline() {
		return isDefaultDrawZoneOutline() ;
	}

	public boolean getDefaultAutoAssignOnly() {
		
		return defaultAutoAssignOnly;
	}

	public void setDefaultAutoAssignOnly(boolean autoAssign) {
		this.defaultAutoAssignOnly = autoAssign;
	}

	public WarHub getWarHub() {
		return warHub;
	}

	public void setWarHub(WarHub warHub) {
		this.warHub = warHub;
	}

	public ZoneLobby lobby(Location location) {
		for(Warzone warzone : warzones) {
			if(warzone.getLobby() != null 
					&& warzone.getLobby().getVolume() != null 
					&& warzone.getLobby().getVolume().contains(location)) 
				return warzone.getLobby();
		}
		return null;
	}

	public boolean inAnyWarzoneLobby(Location location) {
		if(lobby(location) == null) {
			return false;
		}
		return true;
	}
	
	public boolean inWarzoneLobby(String warzoneName, Location location) {
		ZoneLobby currentLobby = lobby(location);
		if(currentLobby == null) {
			return false;
		} else if (warzoneName.equals(currentLobby.getZone().getName())){
			return true;
		}
		return false;
	}

	public void setDefaultTeamCap(int defaultTeamCap) {
		this.defaultTeamCap = defaultTeamCap;
	}

	public int getDefaultTeamCap() {
		return defaultTeamCap;
	}

	public void setPvpInZonesOnly(boolean pvpInZonesOnly) {
		this.pvpInZonesOnly = pvpInZonesOnly;
	}

	public boolean isPvpInZonesOnly() {
		return pvpInZonesOnly;
	}

	public void setDefaultScoreCap(int defaultScoreCap) {
		this.defaultScoreCap = defaultScoreCap;
	}

	public int getDefaultScoreCap() {
		return defaultScoreCap;
	}

	public void setDefaultDrawZoneOutline(boolean defaultDrawZoneOutline) {
		this.defaultDrawZoneOutline = defaultDrawZoneOutline;
	}

	public boolean isDefaultDrawZoneOutline() {
		return defaultDrawZoneOutline;
	}

	public List<String> getZoneMakersImpersonatingPlayers() {
		return zoneMakersImpersonatingPlayers;
	}
	
	public void setupPermissions() {
		Plugin test = this.getServer().getPluginManager().getPlugin("Permissions");

		if(this.Permissions == null) {
		    if(test != null) {
		    	this.Permissions = (Permissions)test;
		    } else {
		    	logInfo("Permissions system not enabled. Defaulting to regular War config.");
		    }
		}
	    }

	public BlockState refetchStateForBlock(World world, Block block) {
		Block again = world.getBlockAt(block.getX(), block.getY(), block.getZ());
		return again.getState();
	}

	public void setDefaultBlockHeads(boolean defaultBlockHeads) {
		this.defaultBlockHeads = defaultBlockHeads;
	}

	public boolean isDefaultBlockHeads() {
		return defaultBlockHeads;
	}

	public void setDefaultDropLootOnDeath(boolean defaultDropLootOnDeath) {
		this.defaultDropLootOnDeath = defaultDropLootOnDeath;
	}

	public boolean isDefaultDropLootOnDeath() {
		return defaultDropLootOnDeath;
	}

	public void setDefaultSpawnStyle(String defaultSpawnStyle) {
		this.defaultSpawnStyle = defaultSpawnStyle;
	}

	public String getDefaultSpawnStyle() {
		return defaultSpawnStyle;
	}

	public HashMap<Integer, ItemStack> getDefaultReward() {
		return defaultReward;
	}

	public List<Warzone> getIncompleteZones() {
		return incompleteZones;
	}

	public void setBuildInZonesOnly(boolean buildInZonesOnly) {
		this.buildInZonesOnly = buildInZonesOnly;
	}

	public boolean isBuildInZonesOnly() {
		return buildInZonesOnly;
	}

	public void setDefaultUnbreakableZoneBlocks(boolean defaultUnbreakableZoneBlocks) {
		this.defaultUnbreakableZoneBlocks = defaultUnbreakableZoneBlocks;
	}

	public boolean isDefaultUnbreakableZoneBlocks() {
		return defaultUnbreakableZoneBlocks;
	}
	
}
