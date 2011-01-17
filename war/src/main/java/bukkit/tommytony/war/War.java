package bukkit.tommytony.war;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.tommytony.war.Monument;
import com.tommytony.war.Team;
import com.tommytony.war.TeamMaterials;
import com.tommytony.war.WarHub;
import com.tommytony.war.Warzone;
import com.tommytony.war.ZoneLobby;
import com.tommytony.war.mappers.WarMapper;
import com.tommytony.war.mappers.WarzoneMapper;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 
 * @author tommytony
 *
 */
public class War extends JavaPlugin {
	
	public War(PluginLoader pluginLoader, Server instance,
			PluginDescriptionFile desc, File folder, File plugin, ClassLoader cLoader) {
		super(pluginLoader, instance, desc, folder, plugin, cLoader);
		// TODO: switch to bukkit config file
	}
	

	private WarPlayerListener playerListener = new WarPlayerListener(this);
	private WarEntityListener entityListener = new WarEntityListener(this);
	private WarBlockListener blockListener = new WarBlockListener(this);
    private Logger log;
    String name = "War";
    String version = "0.3";
    
    private final List<Warzone> warzones = new ArrayList<Warzone>();
    private final List<String> zoneMakerNames = new ArrayList<String>();
    private final HashMap<Integer, ItemStack> defaultLoadout = new HashMap<Integer, ItemStack>();
    private int defaultLifepool = 15;
    private boolean defaultFriendlyFire = false;
	private boolean defaultDrawZoneOutline = true;
	private boolean defaultAutoAssignOnly = false;
	private int defaultTeamCap = 5;
	private int defaultScoreCap = 5;
	private boolean pvpInZonesOnly = false;
	private WarHub warHub;
	
	public void onDisable() {
		Logger.getLogger("Minecraft").info(name + " " + version + " disabled.");
	}

	public void onEnable() {
		this.log = Logger.getLogger("Minecraft");
		
		// Register hooks		
		PluginManager pm = getServer().getPluginManager();
		
		pm.registerEvent(Event.Type.PLAYER_LOGIN, playerListener, Priority.Normal, this);
		pm.registerEvent(Event.Type.PLAYER_QUIT, playerListener, Priority.Normal, this);
		//pm.registerEvent(Event.Type.PLAYER_COMMAND, playerListener, Priority.Normal, this);
		pm.registerEvent(Event.Type.PLAYER_MOVE, playerListener, Priority.Normal, this);
		
		//pm.registerEvent(Event.Type.ENTITY_DAMAGED, entityListener, Priority.Normal, this); 	// done thru Move at respawn
		pm.registerEvent(Event.Type.ENTITY_DAMAGEDBY_ENTITY, entityListener, Priority.Normal, this);
		
		pm.registerEvent(Event.Type.BLOCK_PLACED, blockListener, Priority.Normal, this);
		//pm.registerEvent(Event.Type.BLOCK_CANBUILD, blockListener, Priority.Normal, this);
		pm.registerEvent(Event.Type.BLOCK_DAMAGED, blockListener, Priority.Normal, this);
		
		// Load files from disk or create them (using these defaults)
		this.defaultLoadout.put(0, new ItemStack(Material.STONE_SWORD, 1,  (byte) 8));
		this.defaultLoadout.put(1, new ItemStack(Material.BOW, 1, (byte) 8));
		this.defaultLoadout.put(2, new ItemStack(Material.ARROW, 7));
		this.defaultLoadout.put(3, new ItemStack(Material.IRON_PICKAXE, 1, (byte) 8));
		this.defaultLifepool = 7;
		this.defaultFriendlyFire = false;
		this.defaultAutoAssignOnly = false;
		getLogger().info("Loading War files...");
		WarMapper.load(this, this.getServer().getWorlds()[0]);		
		getLogger().info("War v" + version + " is on.");
	}
	
	public void onCommand(Player player, String cmd, String[] args) {
		String command = cmd.replace("/", "");
		String[] arguments = null;
		// Handle both /war <command> and /<war command>. I.e. "/war zone temple" == "/zone temple"
		if((command.equals("war") || command.equals("War")) && args.length > 1) {
			command = args[1];
			arguments = new String[args.length - 2];
			for(int i = 2; i <= arguments.length; i++) {
				arguments[i-2] = args[i];
			}
		} else if (command.equals("war") || command.equals("War")) {
			player.sendMessage(this.str("War is on. Please pick your battle. " +
					"Use /warhub, /zones and /zone."));
		} else {
			arguments = new String[args.length - 1];
			for(int i = 1; i <= arguments.length; i++) {
				arguments[i-1] = args[i];
			}
		}
	
		// Player commands: /warzones, /warzone, /teams, /join, /leave
		
		// warzones
		if(command.equals("zones") || command.equals("warzones")){
			
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
			player.sendMessage(this.str(warzonesMessage + "  Use /zone <zone-name> to " +
					"teleport to a warzone. "));
		}
		
		// warzone
		else if(command.equals("zone") || command.equals("warzone")) {
			if(arguments.length < 1) {
				player.sendMessage(this.str("Usage: /zone <warzone-name>."));
			} else {
				boolean warped = false;
				for(Warzone warzone : this.getWarzones()) {
					if(warzone.getName().equals(arguments[0]) && warzone.getTeleport() != null){
						player.teleportTo(warzone.getTeleport());
						warped = true;
					}
				}
				if(!warped) {
					player.sendMessage("No such warzone.");
				}
			}
		}
		
		// /teams
		else if(command.equals("teams")){
			if(!this.inAnyWarzone(player.getLocation()) && !this.inAnyWarzoneLobby(player.getLocation())) {
				player.sendMessage(this.str("Usage: /teams. " +
						"Must be in a warzone or zone lobby (try /war, /zones and /zone)."));
			} else {
				player.sendMessage(this.str("" + playerListener.getAllTeamsMsg(player)));
			}
		}
		
		// /join <teamname>
		else if(command.equals("join")) {
			if(arguments.length < 1 || (!this.inAnyWarzone(player.getLocation()) && !this.inAnyWarzoneLobby(player.getLocation()))
					|| (arguments.length > 0 && TeamMaterials.teamMaterialFromString(arguments[0]) == null)) {
				player.sendMessage(this.str("Usage: /join <diamond/iron/gold/d/i/g>." +
						" Teams are warzone specific." +
						" You must be inside a warzone or zone lobby to join a team." +
						" Use as an alternative to walking through the team gate."));
			} else {				
				// drop from old team if any
				Team previousTeam = this.getPlayerTeam(player.getName());
				if(previousTeam != null) {
					if(!previousTeam.removePlayer(player.getName())){
						this.getLogger().log(Level.WARNING, "Could not remove player " + player.getName() + " from team " + previousTeam.getName());
					}
						
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
				List<Team> teams = warzone.getTeams();
				boolean foundTeam = false;
				for(Team team : teams) {
					if(team.getName().equals(name)) {
						if(!warzone.hasPlayerInventory(player.getName())) {
							warzone.keepPlayerInventory(player);
							player.sendMessage(this.str("Your inventory is is storage until you /leave."));
						}
						if(team.getPlayers().size() < warzone.getTeamCap()) {
							team.addPlayer(player);
							Warzone zone = this.warzone(player.getLocation());
							zone.respawnPlayer(team, player);
							foundTeam = true;
						} else {
							player.sendMessage(this.str("Team " + name + " is full."));
							foundTeam = true;
						}
					}
				}
				if(foundTeam) {
					for(Team team : teams){
						team.teamcast(this.str("" + player.getName() + " joined " + name));
					}
				} else {
					player.sendMessage(this.str("No such team. Try /teams."));
				}
			}
		}
		
		// /leave
		else if(command.equals("leave")) {
			if(!this.inAnyWarzone(player.getLocation()) || this.getPlayerTeam(player.getName()) == null) {
				player.sendMessage(this.str("Usage: /leave. " +
						"Must be in a team already."));
			} else {
				Team playerTeam = this.getPlayerTeam(player.getName());
				playerTeam.removePlayer(player.getName());
				
				Warzone zone = this.warzone(player.getLocation());
				player.teleportTo(zone.getTeleport());
				player.sendMessage(this.str("Left the zone."));
				zone.restorePlayerInventory(player);
				player.sendMessage(this.str("Your inventory has (hopefully) been restored."));
			}
		}
		
		// /team <msg>
		else if(command.equals("team")) {
			if(!this.inAnyWarzone(player.getLocation())) {
				player.sendMessage(this.str("Usage: /team <message>. " +
						"Sends a message only to your teammates."));
			} else {
				Team playerTeam = this.getPlayerTeam(player.getName());
				String teamMessage = player.getName();
				for(int j = 0 ; j<arguments.length; j++) {
					String part = arguments[j];
					teamMessage += part + " ";
				}
				playerTeam.teamcast(this.str(teamMessage));
			}
		}
		
		// /warhub
		else if(command.equals("warhub")) {
			if(this.getWarHub() == null) {
				player.sendMessage("No warhub on this War server. Try /zones and /zone.");
			} else {
				Team playerTeam = this.getPlayerTeam(player.getName());
				if(playerTeam != null) { // was in zone
					playerTeam.removePlayer(player.getName());
				}
				
				player.teleportTo(this.getWarHub().getLocation());
			}
			
		}
		
		if(this.isZoneMaker(player.getName())) {			
		// Mod commands : /nextbattle
		
			// /nextbattle
			if(command.equals("nextbattle")) {
				if(!this.inAnyWarzone(player.getLocation())) {
					player.sendMessage(this.str("Usage: /nextbattle. Resets the zone blocks and all teams' life pools. Must be in warzone."));
				} else {
					Warzone warzone = this.warzone(player.getLocation());
					for(Team team: warzone.getTeams()) {
						team.teamcast(this.str("The battle was interrupted. " + playerListener.getAllTeamsMsg(player) + " Resetting warzone " + warzone.getName() + " and life pools..."));
					}
					int resetBlocks = warzone.getVolume().resetBlocks();
					warzone.initializeZone();
					player.sendMessage(this.str("Warzone reset. " + resetBlocks + " blocks reset."));
					this.getLogger().info(resetBlocks + " blocks reset in warzone " + warzone.getName() + ".");
				}
			}
			
			// Warzone maker commands: /setzone, /savezone, /setteam, /setmonument, /resetzone
			
			// /setwarhub
			else if(command.equals("setwarhub")) {
				if(warHub != null) {
					// reset existing hub
					warHub.getVolume().resetBlocks();
					warHub.setLocation(player.getLocation());
					warHub.initialize();
				} else {
					warHub = new WarHub(this, player.getLocation());
					warHub.initialize();
					for(Warzone zone : warzones) {
						zone.getLobby().getVolume().resetBlocks();
						zone.getLobby().initialize();
					}
				}
				WarMapper.save(this);
			}
			
			// /setzone
			else if(command.equals("setzone")) {
				if(arguments.length < 2 || arguments.length > 2 
						|| (arguments.length == 2 && (!arguments[1].equals("southeast") && !arguments[1].equals("northwest")
																&& !arguments[1].equals("se") && !arguments[1].equals("nw")))) {
					player.sendMessage(this.str("Usage: /setzone <warzone-name> <'southeast'/'northwest'/'se'/'nw'>. " +
							"Defines the battleground boundary. " +
							"The warzone is reset at the start of every battle. " +
							"This command overwrites any previously saved blocks " +
							"(i.e. make sure you reset with /restartbattle " +
							"or /resetwarzone before changing the boundary). "));
				} else {
					Warzone warzone = this.findWarzone(arguments[0]);
					String message = "";
					if(warzone == null) {
						// create the warzone
						warzone = new Warzone(this, player.getLocation().getWorld(), arguments[0]);
						this.addWarzone(warzone);
						WarMapper.save(this);
						if(arguments[1].equals("northwest") || arguments[1].equals("nw")) {
							warzone.setNorthwest(player.getLocation());
							player.sendMessage(this.str("Warzone " + warzone.getName() + " added. Northwesternmost point set at x=" 
									+ (int)warzone.getNorthwest().getBlockX() + " z=" + (int)warzone.getNorthwest().getBlockZ() + "."));
						} else {
							warzone.setSoutheast(player.getLocation());
							player.sendMessage(this.str("Warzone " + warzone.getName() + " added. Southeasternmost point set at x=" 
									+ (int)warzone.getSoutheast().getBlockX() + " z=" + (int)warzone.getSoutheast().getBlockZ() + "."));
						}
						WarzoneMapper.save(this, warzone, false);
					} else {
						// change existing warzone
						if(arguments[1].equals("northwest") || arguments[1].equals("nw")) {
							if(warzone.getSoutheast() != null 
									&& (player.getLocation().getBlockX() >= warzone.getSoutheast().getBlockX()
											|| player.getLocation().getBlockZ() <= warzone.getSoutheast().getBlockZ())) {
								player.sendMessage(this.str("You must place that corner northwest relative to the existing southeast corner!"));
							} else {
								int reset = warzone.getVolume().resetBlocks();
								warzone.setNorthwest(player.getLocation());
								warzone.saveState();
								warzone.initializeZone();
								message += "Northwesternmost point set at x=" + (int)warzone.getNorthwest().getBlockX() 
												+ " z=" + (int)warzone.getNorthwest().getBlockZ() + " on warzone " + warzone.getName() + ". " +
												reset + " blocks reset. Zone saved.";
							} 
						} else {
							if(warzone.getNorthwest() != null 
									&& (player.getLocation().getBlockX() <= warzone.getNorthwest().getBlockX()
											|| player.getLocation().getBlockZ() >= warzone.getNorthwest().getBlockZ())) {
								player.sendMessage(this.str("You must place that corner southeast relative to the existing northwest corner!"));
							} else {
								int reset = warzone.getVolume().resetBlocks();
								warzone.setSoutheast(player.getLocation());
								warzone.saveState();
								warzone.initializeZone();
								
								message += "Southeasternmost point set at x=" + (int)warzone.getSoutheast().getBlockX()
												+ " z=" + (int)warzone.getSoutheast().getBlockZ() + " on warzone " + warzone.getName() + ". " +
												reset + " blocks reset. Zone saved.";
							}
						}
						WarzoneMapper.save(this, warzone, true);
					}
					if(warzone.getNorthwest() == null) {
						message += " Still missing northwesternmost point.";
					}
					if(warzone.getSoutheast() == null) {
						message += " Still missing southeasternmost point.";
					}
					if(warzone.getNorthwest() != null && warzone.getSoutheast() != null) {
						if(warzone.ready()) {
							message += " Warzone " + warzone.getName() + " outline done. Use /setteam, /setmonument and /savezone to complete the zone.";
						} else if (warzone.tooSmall()) {
							message += " Warzone " + warzone.getName() + " is too small. Min north-south size: 20. Min east-west size: 20.";
						} else if (warzone.tooBig()) {
							message += " Warzone " + warzone.getName() + " is too Big. Max north-south size: 1000. Max east-west size: 1000.";
						}
					}
					player.sendMessage(this.str(message));
				}
			}
			
			else if(command.equals("setzonelobby")) {
				if((!this.inAnyWarzone(player.getLocation())
						&& !this.inAnyWarzoneLobby(player.getLocation()))
						|| arguments.length < 1 || arguments.length > 1 
						|| (arguments.length == 1 && !arguments[0].equals("north") && !arguments[0].equals("n")
																&& !arguments[0].equals("east") && !arguments[0].equals("e")
																&& !arguments[0].equals("south") && !arguments[0].equals("s")
																&& !arguments[0].equals("west") && !arguments[0].equals("w"))) {
					player.sendMessage(this.str("Usage: /setzonelobby <north/n/east/e/south/s/west/w>. Must be in warzone." +
							"Defines on which side the zone lobby lies. " +
							"Removes any previously set lobby."));
				} else {
					Warzone warzone = this.warzone(player.getLocation());
					ZoneLobby lobby = this.lobby(player.getLocation());
					if(warzone == null && lobby != null) {
						warzone = lobby.getZone();
					} else {
						lobby = warzone.getLobby();
					}
					BlockFace wall = null;
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
						player.sendMessage(this.str("Warzone lobby moved to " + wallStr + " side of zone."));
					} else {
						// new lobby
						lobby = new ZoneLobby(this, warzone, wall);
						warzone.setLobby(lobby);
						lobby.initialize();
						player.sendMessage(this.str("Warzone lobby created on " + wallStr + "side of zone."));
					}
					WarzoneMapper.save(this, warzone, false);
				}
			}
	
			// /savewarzone
			else if(command.equals("savezone")) {
				if(!this.inAnyWarzone(player.getLocation()) && !this.inAnyWarzoneLobby(player.getLocation())) {
					player.sendMessage(this.str("Usage: /savezone. Must be in warzone. " +
							"Changes the warzone state loaded at the beginning of every battle. " +
							"Also sets the teleport point for this warzone where you're standing." +
							"(i.e. make sure to use /zone or the warzone tp point will change). " +
							"Just like /setzone, this command overwrites any previously saved blocks " +
							"(i.e. make sure you reset with /restartbattle " +
							"or /resetzone before changing start state). "));
				} else {
					Warzone warzone = this.warzone(player.getLocation());
					ZoneLobby lobby = this.lobby(player.getLocation());
					if(warzone == null && lobby != null) {
						warzone = lobby.getZone();
					} else {
						lobby = warzone.getLobby();
					}
					int savedBlocks = warzone.saveState();
					if(warzone.getLobby() == null) {
						// Set default lobby on south side
						lobby = new ZoneLobby(this, warzone, BlockFace.SOUTH);
						warzone.setLobby(lobby);
						lobby.initialize();
						player.sendMessage(this.str("Default lobby created on south side of zone."));
					}
					WarzoneMapper.save(this, warzone, true);
					warzone.initializeZone();	// bring back team spawns etc
					player.sendMessage(this.str("Warzone " + warzone.getName() + " initial state changed. Saved " + savedBlocks + " blocks."));
				}
			}
			
			// /resetwarzone
			else if(command.equals("resetzone")) {
				if(!this.inAnyWarzone(player.getLocation()) && !this.inAnyWarzoneLobby(player.getLocation())) {
					player.sendMessage(this.str("Usage: /resetzone pool=10. Reloads the zone. All named parameter are optional. Defaults: pool=7 maxScore=-1 (infinite). Must be in warzone."));
				} else {
					Warzone warzone = this.warzone(player.getLocation());
					ZoneLobby lobby = this.lobby(player.getLocation());
					if(warzone == null && lobby != null) {
						warzone = lobby.getZone();
					} else {
						lobby = warzone.getLobby();
					}
					int resetBlocks = warzone.getVolume().resetBlocks();
					warzone.initializeZone();
					for(Team team: warzone.getTeams()) {
						team.teamcast(this.str("The war has ended. " + playerListener.getAllTeamsMsg(player) + " Resetting warzone " + warzone.getName() + " and teams..."));
						for(Player p : team.getPlayers()) {
							p.teleportTo(warzone.getTeleport());
							warzone.restorePlayerInventory(p);
							player.sendMessage(this.str("You have left the warzone. Your inventory has (hopefully) been restored."));
						}
					}
					this.getWarzones().remove(warzone);
					Warzone resetWarzone = WarzoneMapper.load(this, warzone.getName(), true);
					this.getWarzones().add(resetWarzone);
					if(arguments.length > 0) {
						for(String arg : arguments) {
							if(arg.startsWith("pool=")){
								int overrideLifepool = Integer.parseInt(arg.substring(5));
								resetWarzone.setLifePool(overrideLifepool);
							}
						}
					}
					resetWarzone.initializeZone();
					player.sendMessage(this.str("Warzone and teams reset. " + resetBlocks + " blocks reset."));
					this.getLogger().info(resetBlocks + " blocks reset in warzone " + warzone.getName() + ".");
				}
			}
			
			// /deletezone
			else if(command.equals("deletezone")) {
				if(!this.inAnyWarzone(player.getLocation()) && !this.inAnyWarzoneLobby(player.getLocation())) {
					player.sendMessage(this.str("Usage: /deletewarzone." +
							" Deletes the warzone. " +
							"Must be in the warzone (try /warzones and /warzone). "));
				} else {
					Warzone warzone = this.warzone(player.getLocation());
					ZoneLobby lobby = this.lobby(player.getLocation());
					if(warzone == null && lobby != null) {
						warzone = lobby.getZone();
					} else {
						lobby = warzone.getLobby();
					}
					for(Team t : warzone.getTeams()) {
						t.getVolume().resetBlocks();
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
					player.sendMessage(this.str("Warzone " + warzone.getName() + " removed."));
				}
			}
			
			// /setteam <diamond/iron/gold/d/i/g>
			else if(command.equals("setteam")) {
				if(arguments.length < 1 || !this.inAnyWarzone(player.getLocation()) 
						|| (arguments.length > 0 && TeamMaterials.teamMaterialFromString(arguments[0]) == null)) {
					player.sendMessage(this.str("Usage: /setteam <diamond/iron/gold/d/i/g>." +
							" Sets the team spawn to the current location. " +
							"Must be in a warzone (try /zones and /zone). "));
				} else {
					Material teamMaterial = TeamMaterials.teamMaterialFromString(arguments[0]);
					String name = TeamMaterials.teamMaterialToString(teamMaterial);					
					Warzone warzone = this.warzone(player.getLocation());
					Team existingTeam = warzone.getTeamByMaterial(teamMaterial);
					if(existingTeam != null) {
						// relocate
						existingTeam.setTeamSpawn(player.getLocation());
						player.sendMessage(this.str("Team " + existingTeam.getName() + " spawn relocated."));
					} else {
						// new team
						Team newTeam = new Team(name, teamMaterial, player.getLocation(), this, warzone);
						newTeam.setRemainingTickets(warzone.getLifePool());
						warzone.getTeams().add(newTeam);
						newTeam.setTeamSpawn(player.getLocation());
						if(warzone.getLobby() != null) {
							warzone.getLobby().getVolume().resetBlocks();
							warzone.getVolume().resetWallBlocks(warzone.getLobby().getWall());
							warzone.addZoneOutline(warzone.getLobby().getWall());
							warzone.getLobby().initialize();
						}
						player.sendMessage(this.str("Team " + name + " created with spawn here."));
					}
					
					WarzoneMapper.save(this, warzone, false);
				}
			}
			
			// /deleteteam <teamname>
			else if(command.equals("deleteteam")) {
				if(arguments.length < 1 || (!this.inAnyWarzone(player.getLocation()) 
												&& !this.inAnyWarzoneLobby(player.getLocation()))) {
					player.sendMessage(this.str("Usage: /deleteteam <team-name>." +
							" Deletes the team and its spawn. " +
							"Must be in a warzone or lobby (try /zones and /zone). "));
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
						team.getVolume().resetBlocks();	
						warzone.getTeams().remove(team);
						if(warzone.getLobby() != null) {
							warzone.getLobby().getVolume().resetBlocks();
							warzone.getVolume().resetWallBlocks(warzone.getLobby().getWall());
							warzone.addZoneOutline(warzone.getLobby().getWall());
							warzone.getLobby().initialize();
						}
						WarzoneMapper.save(this, warzone, false);
						player.sendMessage(this.str("Team " + name + " removed."));
					} else {
						player.sendMessage(this.str("No such team."));
					}
				}
			}
			
			// /setmonument
			else if(command.equals("setmonument")) {
				if(!this.inAnyWarzone(player.getLocation())) {
					player.sendMessage(this.str("Usage: /setmonument <name>. Creates or moves a monument. Must be in warzone."));
				} else {
					Warzone warzone = this.warzone(player.getLocation());
					String monumentName = arguments[0];
					if(warzone.hasMonument(monumentName)) {
						// move the existing monument
						Monument monument = warzone.getMonument(monumentName);
						monument.getVolume().resetBlocks();
						monument.setLocation(player.getLocation());
						player.sendMessage(this.str("Monument " + monument.getName() + " was moved."));
					} else {
						// create a new monument
						Monument monument = new Monument(arguments[0], this, warzone, player.getLocation());
						warzone.getMonuments().add(monument);
						player.sendMessage(this.str("Monument " + monument.getName() + " created."));
					}
					WarzoneMapper.save(this, warzone, false);
				}
			}
			
			// /deletemonument <name>
			else if(command.equals("deletemonument")) {
				if(arguments.length < 1 || (!this.inAnyWarzone(player.getLocation()) 
												&& !this.inAnyWarzoneLobby(player.getLocation()))) {
					player.sendMessage(this.str("Usage: /deletemonument <name>." +
							" Deletes the monument. " +
							"Must be in a warzone or lobby (try /warzones and /warzone). "));
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
						player.sendMessage(this.str("Monument " + name + " removed."));
					} else {
						player.sendMessage(this.str("No such monument."));
					}
				}
			}
		} else if (command.equals("setzone")		// Not a zone maker but War command.
						|| command.equals("nextbattle")
						|| command.equals("setzonelobby")
						|| command.equals("savezone")
						|| command.equals("deletezone")
						|| command.equals("setteam")
						|| command.equals("deleteteam")
						|| command.equals("setmonument")
						|| command.equals("deletemonument")) {
			player.sendMessage(this.str("You can't do this if you are not a warzone maker."));
		}
		
	}

	public Team getPlayerTeam(String playerName) {
		for(Warzone warzone : warzones) {
			Team team = warzone.getPlayerTeam(playerName);
			if(team != null) return team;
		}
		return null;
	}
	
	public Warzone getPlayerWarzone(String playerName) {
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
	
	public String str(String str) {
		String out = ChatColor.GRAY + "[war] " + ChatColor.WHITE + str;
		return out;
	}
	
	public Warzone findWarzone(String warzoneName) {
		for(Warzone warzone : warzones) {
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
		return name;
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
	
	public boolean isZoneMaker(String playerName) {
		for(String zoneMaker : zoneMakerNames) {
			if(zoneMaker.equals(playerName)) return true;
		}
		return false;			
	}

	public boolean getDefaultDrawZoneOutline() {
		return defaultDrawZoneOutline ;
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
	
}
