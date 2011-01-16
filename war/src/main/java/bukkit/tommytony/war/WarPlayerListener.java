package bukkit.tommytony.war;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerMoveEvent;

import com.tommytony.war.Monument;
import com.tommytony.war.Team;
import com.tommytony.war.TeamMaterials;
import com.tommytony.war.WarHub;
import com.tommytony.war.Warzone;
import com.tommytony.war.ZoneLobby;
import com.tommytony.war.mappers.WarMapper;
import com.tommytony.war.mappers.WarzoneMapper;


/**
 * 
 * @author tommytony
 *
 */
public class WarPlayerListener extends PlayerListener {

	private final War war;
	private Random random = null;

	public WarPlayerListener(War war) {
		this.war = war;
		random = new Random();
	}
	
	public void onPlayerJoin(PlayerEvent event) {
		event.getPlayer().sendMessage(war.str("War is on! Pick your battle (try /warzones)."));
    }
	
	public void onPlayerQuit(PlayerEvent event) {
		Player player = event.getPlayer();
		Team team = war.getPlayerTeam(player.getName());
		if(team != null) {
			team.removePlayer(player.getName());
		}
	}

	public void onPlayerCommand(PlayerChatEvent event) {
		Player player = event.getPlayer();
		String[] split = event.getMessage().split(" ");
		String command = split[0];
		if(command.startsWith("/")) {
			
			// Handle both /war <command> and /<war command>. I.e. "/war zone temple" == "/zone temple"
			String[] arguments = null;
			if(command.equals("/war")) {
				command = split[1];
				arguments = new String[split.length - 2];
				for(int i = 1; i <= arguments.length; i++) {
					arguments[i-1] = split[i];
				}
			} else {
				command = command.substring(1, command.length());
				arguments = new String[split.length - 1];
				for(int i = 1; i <= arguments.length; i++) {
					arguments[i-1] = split[i];
				}
			}
		
			// Player commands: /warzones, /warzone, /teams, /join, /leave
			
			// warzones
			if(command.equals("zones") || command.equals("warzones")){
				
				String warzonesMessage = "Warzones: ";
				if(war.getWarzones().isEmpty()){
					warzonesMessage += "none.";
				}
				for(Warzone warzone : war.getWarzones()) {
					
					warzonesMessage += warzone.getName() + " ("
					+ warzone.getTeams().size() + " teams, ";
					int playerTotal = 0;
					for(Team team : warzone.getTeams()) {
						playerTotal += team.getPlayers().size();
					}
					warzonesMessage += playerTotal + " players)  ";
				}
				player.sendMessage(war.str(warzonesMessage + "  Use /zone <zone-name> to " +
						"teleport to a warzone. "));
				event.setCancelled(true); 
			}
			
			// warzone
			else if(command.equals("zone") || command.equals("warzone")) {
				if(arguments.length < 1) {
					player.sendMessage(war.str("Usage: /zone <warzone-name>."));
				} else {
					boolean warped = false;
					for(Warzone warzone : war.getWarzones()) {
						if(warzone.getName().equals(arguments[0]) && warzone.getTeleport() != null){
							player.teleportTo(warzone.getTeleport());
							warped = true;
						}
					}
					if(!warped) {
						player.sendMessage("No such warzone.");
					}
				}
				event.setCancelled(true); 
			}
			
			// /teams
			else if(command.equals("teams")){
				if(!war.inAnyWarzone(player.getLocation())) {
					player.sendMessage(war.str("Usage: /teams. " +
							"Must be in a warzone (try /warzones and /warzone)."));
				} else {
					player.sendMessage(war.str("" + getAllTeamsMsg(player)));
				}
				event.setCancelled(true); 
			}
			
			// /join <teamname>
			else if(command.equals("join")) {
				if(arguments.length < 1 || !war.inAnyWarzone(player.getLocation())
						|| (arguments.length > 0 && TeamMaterials.teamMaterialFromString(arguments[0]) == null)) {
					player.sendMessage(war.str("Usage: /join <diamond/iron/gold/d/i/g>." +
							" Teams are warzone specific." +
							" You must be inside a warzone to join a team."));
				} else {				
					// drop from old team if any
					Team previousTeam = war.getPlayerTeam(player.getName());
					if(previousTeam != null) {
						if(!previousTeam.removePlayer(player.getName())){
							war.getLogger().log(Level.WARNING, "Could not remove player " + player.getName() + " from team " + previousTeam.getName());
						}
							
					}
					
					// join new team
					String name = TeamMaterials.teamMaterialToString(TeamMaterials.teamMaterialFromString(arguments[0]));
					Warzone warzone = war.warzone(player.getLocation());
					List<Team> teams = warzone.getTeams();
					boolean foundTeam = false;
					for(Team team : teams) {
						if(team.getName().equals(name)) {
							if(!warzone.hasPlayerInventory(player.getName())) {
								warzone.keepPlayerInventory(player);
								player.sendMessage(war.str("Your inventory is is storage until you /leave."));
							}
							team.addPlayer(player);
							Warzone zone = war.warzone(player.getLocation());
							zone.respawnPlayer(team, player);
							foundTeam = true;
						}
					}
					if(foundTeam) {
						for(Team team : teams){
							team.teamcast(war.str("" + player.getName() + " joined " + name));
						}
					} else {
						player.sendMessage(war.str("No such team. Try /teams."));
					}
				}
				event.setCancelled(true); 
			}
			
			// /leave
			else if(command.equals("leave")) {
				if(!war.inAnyWarzone(player.getLocation()) || war.getPlayerTeam(player.getName()) == null) {
					player.sendMessage(war.str("Usage: /leave. " +
							"Must be in a team already."));
				} else {
					Team playerTeam = war.getPlayerTeam(player.getName());
					playerTeam.removePlayer(player.getName());
					
					Warzone zone = war.warzone(player.getLocation());
					player.teleportTo(zone.getTeleport());
					player.sendMessage(war.str("Left the zone."));
					zone.restorePlayerInventory(player);
					player.sendMessage(war.str("Your inventory has (hopefully) been restored."));
				}
				event.setCancelled(true); 
			}
			
			
			// /team <msg>
			else if(command.equals("team")) {
				if(!war.inAnyWarzone(player.getLocation())) {
					player.sendMessage(war.str("Usage: /team <message>. " +
							"Sends a message only to your teammates."));
				} else {
					Team playerTeam = war.getPlayerTeam(player.getName());
					String teamMessage = player.getName();
					for(int j = 0 ; j<arguments.length; j++) {
						String part = arguments[j];
						teamMessage += part + " ";
					}
					playerTeam.teamcast(war.str(teamMessage));
				}
				event.setCancelled(true); 
			}
			
			if(war.isZoneMaker(player.getName())) {			
			// Mod commands : /nextbattle
			
				// /restartbattle
				if(command.equals("nextbattle") || command.equals("restartbattle")) {
					if(!war.inAnyWarzone(player.getLocation())) {
						player.sendMessage(war.str("Usage: /nextbattle. Resets the zone blocks and all teams' life pools. Must be in warzone."));
					} else {
						Warzone warzone = war.warzone(player.getLocation());
						for(Team team: warzone.getTeams()) {
							team.teamcast(war.str("The battle was interrupted. " + getAllTeamsMsg(player) + " Resetting warzone " + warzone.getName() + " and life pools..."));
						}
						int resetBlocks = warzone.getVolume().resetBlocks();
						warzone.initializeZone();
						player.sendMessage(war.str("Warzone reset. " + resetBlocks + " blocks reset."));
						war.getLogger().info(resetBlocks + " blocks reset in warzone " + warzone.getName() + ".");
					}
					event.setCancelled(true);
				}
				
				// Warzone maker commands: /setzone, /savezone, /setteam, /setmonument, /resetzone
				
				// /warhub
				else if(command.equals("warhub")) {
					WarHub hub = war.getWarHub();
					if(hub != null) {
						// reset existing hub
						hub.getVolume().resetBlocks();
						hub.setLocation(player.getLocation());
						hub.initialize();
					} else {
						hub = new WarHub(war, player.getLocation());
						hub.initialize();
					}
					WarMapper.save(war);
				}
				
				// /setzone
				else if(command.equals("setzone") || command.equals("setwarzone")) {
					if(arguments.length < 2 || arguments.length > 2 
							|| (arguments.length == 2 && (!arguments[1].equals("southeast") && !arguments[1].equals("northwest")
																	&& !arguments[1].equals("se") && !arguments[1].equals("nw")))) {
						player.sendMessage(war.str("Usage: /setzone <warzone-name> <'southeast'/'northwest'/'se'/'nw'>. " +
								"Defines the battleground boundary. " +
								"The warzone is reset at the start of every battle. " +
								"This command overwrites any previously saved blocks " +
								"(i.e. make sure you reset with /restartbattle " +
								"or /resetwarzone before changing the boundary). "));
					} else {
						Warzone warzone = war.findWarzone(arguments[0]);
						String message = "";
						if(warzone == null) {
							// create the warzone
							warzone = new Warzone(war, player.getLocation().getWorld(), arguments[0]);
							war.addWarzone(warzone);
							WarMapper.save(war);
							if(arguments[1].equals("northwest") || arguments[1].equals("nw")) {
								warzone.setNorthwest(player.getLocation());
								player.sendMessage(war.str("Warzone " + warzone.getName() + " added. Northwesternmost point set at x=" 
										+ (int)warzone.getNorthwest().getBlockX() + " z=" + (int)warzone.getNorthwest().getBlockZ() + "."));
							} else {
								warzone.setSoutheast(player.getLocation());
								player.sendMessage(war.str("Warzone " + warzone.getName() + " added. Southeasternmost point set at x=" 
										+ (int)warzone.getSoutheast().getBlockX() + " z=" + (int)warzone.getSoutheast().getBlockZ() + "."));
							}
							WarzoneMapper.save(war, warzone, false);
						} else {
							// change existing warzone
							if(arguments[1].equals("northwest") || arguments[1].equals("nw")) {
								if(warzone.getSoutheast() != null 
										&& (player.getLocation().getBlockX() >= warzone.getSoutheast().getBlockX()
												|| player.getLocation().getBlockZ() <= warzone.getSoutheast().getBlockZ())) {
									player.sendMessage(war.str("You must place that corner northwest relative to the existing southeast corner!"));
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
									player.sendMessage(war.str("You must place that corner southeast relative to the existing northwest corner!"));
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
							WarzoneMapper.save(war, warzone, true);
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
						player.sendMessage(war.str(message));
					}
					event.setCancelled(true); 
				}
				
				else if(command.equals("setzonelobby")) {
					if((!war.inAnyWarzone(player.getLocation())
							&& !war.inAnyWarzoneLobby(player.getLocation()))
							|| arguments.length < 1 || arguments.length > 1 
							|| (arguments.length == 1 && !arguments[0].equals("north") && !arguments[0].equals("n")
																	&& !arguments[0].equals("east") && !arguments[0].equals("e")
																	&& !arguments[0].equals("south") && !arguments[0].equals("s")
																	&& !arguments[0].equals("west") && !arguments[0].equals("w"))) {
						player.sendMessage(war.str("Usage: /setzonelobby <north/n/east/e/south/s/west/w>. Must be in warzone." +
								"Defines on which side the zone lobby lies. " +
								"Removes any previously set lobby."));
					} else {
						Warzone warzone = war.warzone(player.getLocation());
						ZoneLobby lobby = war.lobby(player.getLocation());
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
							player.sendMessage(war.str("Warzone lobby moved to " + wallStr + " side of zone."));
						} else {
							// new lobby
							lobby = new ZoneLobby(war, warzone, wall);
							warzone.setLobby(lobby);
							lobby.initialize();
							player.sendMessage(war.str("Warzone lobby created on " + wallStr + "side of zone."));
						}
						WarzoneMapper.save(war, warzone, false);
					}
				}
		
				// /savewarzone
				else if(command.equals("savezone") || command.equals("savewarzone")) {
					if(!war.inAnyWarzone(player.getLocation()) && !war.inAnyWarzoneLobby(player.getLocation())) {
						player.sendMessage(war.str("Usage: /savezone. Must be in warzone. " +
								"Changes the warzone state loaded at the beginning of every battle. " +
								"Also sets the teleport point for this warzone where you're standing." +
								"(i.e. make sure to use /zone or the warzone tp point will change). " +
								"Just like /setzone, this command overwrites any previously saved blocks " +
								"(i.e. make sure you reset with /restartbattle " +
								"or /resetzone before changing start state). "));
					} else {
						Warzone warzone = war.warzone(player.getLocation());
						ZoneLobby lobby = war.lobby(player.getLocation());
						if(warzone == null && lobby != null) {
							warzone = lobby.getZone();
						} else {
							lobby = warzone.getLobby();
						}
						int savedBlocks = warzone.saveState();
						if(warzone.getLobby() == null) {
							// Set default lobby on south side
							lobby = new ZoneLobby(war, warzone, BlockFace.SOUTH);
							warzone.setLobby(lobby);
							lobby.initialize();
							player.sendMessage(war.str("Default lobby created on south side of zone."));
						}
						WarzoneMapper.save(war, warzone, true);
						warzone.initializeZone();	// bring back team spawns etc
						player.sendMessage(war.str("Warzone " + warzone.getName() + " initial state changed. Saved " + savedBlocks + " blocks."));
					}
					event.setCancelled(true); 
				}
				
				// /resetwarzone
				else if(command.equals("resetzone") || command.equals("resetwarzone")) {
					if(!war.inAnyWarzone(player.getLocation()) && !war.inAnyWarzoneLobby(player.getLocation())) {
						player.sendMessage(war.str("Usage: /resetzone pool=10. Reloads the zone. All named parameter are optional. Defaults: pool=7 maxScore=-1 (infinite). Must be in warzone."));
					} else {
						Warzone warzone = war.warzone(player.getLocation());
						ZoneLobby lobby = war.lobby(player.getLocation());
						if(warzone == null && lobby != null) {
							warzone = lobby.getZone();
						} else {
							lobby = warzone.getLobby();
						}
						int resetBlocks = warzone.getVolume().resetBlocks();
						warzone.initializeZone();
						for(Team team: warzone.getTeams()) {
							team.teamcast(war.str("The war has ended. " + getAllTeamsMsg(player) + " Resetting warzone " + warzone.getName() + " and teams..."));
							for(Player p : team.getPlayers()) {
								p.teleportTo(warzone.getTeleport());
								warzone.restorePlayerInventory(p);
								player.sendMessage(war.str("You have left the warzone. Your inventory has (hopefully) been restored."));
							}
						}
						war.getWarzones().remove(warzone);
						Warzone resetWarzone = WarzoneMapper.load(war, warzone.getName(), true);
						war.getWarzones().add(resetWarzone);
						if(arguments.length > 0) {
							for(String arg : arguments) {
								if(arg.startsWith("pool=")){
									int overrideLifepool = Integer.parseInt(arg.substring(5));
									resetWarzone.setLifePool(overrideLifepool);
								}
							}
						}
						resetWarzone.initializeZone();
						player.sendMessage(war.str("Warzone and teams reset. " + resetBlocks + " blocks reset."));
						war.getLogger().info(resetBlocks + " blocks reset in warzone " + warzone.getName() + ".");
					}
					event.setCancelled(true); 
				}
				
				// /deletewarzone
				else if(command.equals("deletezone") || command.equals("deletewarzone")) {
					if(!war.inAnyWarzone(player.getLocation())) {
						player.sendMessage(war.str("Usage: /deletewarzone." +
								" Deletes the warzone. " +
								"Must be in the warzone (try /warzones and /warzone). "));
					} else {
						Warzone warzone = war.warzone(player.getLocation());
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
						war.getWarzones().remove(warzone);
						WarMapper.save(war);
						WarzoneMapper.delete(war, warzone.getName());
						player.sendMessage(war.str("Warzone " + warzone.getName() + " removed."));
					}
					event.setCancelled(true); 
				}
				
				// /setteam <diamond/iron/gold/d/i/g>
				else if(command.equals("setteam") || command.equals("newteam") || command.equals("teamspawn")) {
					if(arguments.length < 1 || !war.inAnyWarzone(player.getLocation()) 
							|| (arguments.length > 0 && TeamMaterials.teamMaterialFromString(arguments[0]) == null)) {
						player.sendMessage(war.str("Usage: /setteam <diamond/iron/gold/d/i/g>." +
								" Sets the team spawn to the current location. " +
								"Must be in a warzone (try /zones and /zone). "));
					} else {
						Material teamMaterial = TeamMaterials.teamMaterialFromString(arguments[0]);
						String name = TeamMaterials.teamMaterialToString(teamMaterial);					
						Warzone warzone = war.warzone(player.getLocation());
						Team existingTeam = warzone.getTeamByMaterial(teamMaterial);
						if(existingTeam != null) {
							// relocate
							existingTeam.setTeamSpawn(player.getLocation());
							player.sendMessage(war.str("Team " + existingTeam.getName() + " spawn relocated."));
						} else {
							// new team
							Team newTeam = new Team(name, teamMaterial, player.getLocation(), war, warzone);
							newTeam.setRemainingTickets(warzone.getLifePool());
							warzone.getTeams().add(newTeam);
							newTeam.setTeamSpawn(player.getLocation());
							player.sendMessage(war.str("Team " + name + " created with spawn here."));
						}
						
						WarzoneMapper.save(war, warzone, false);
					}
					event.setCancelled(true); 
				}
				
				// /deleteteam <teamname>
				else if(command.equals("deleteteam")) {
					if(arguments.length < 1 || !war.inAnyWarzone(player.getLocation())) {
						player.sendMessage(war.str("Usage: /deleteteam <team-name>." +
								" Deletes the team and its spawn. " +
								"Must be in a warzone (try /zones and /zone). "));
					} else {
						String name = TeamMaterials.teamMaterialToString(TeamMaterials.teamMaterialFromString(arguments[1]));
						Warzone warzone = war.warzone(player.getLocation());
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
							WarzoneMapper.save(war, warzone, false);
							player.sendMessage(war.str("Team " + name + " removed."));
						} else {
							player.sendMessage(war.str("No such team."));
						}
					}
					event.setCancelled(true); 
				}
				
				// /monument
				else if(command.equals("setmonument")) {
					if(!war.inAnyWarzone(player.getLocation())) {
						player.sendMessage(war.str("Usage: /setmonument <name>. Creates or moves a monument. Must be in warzone."));
					} else {
						Warzone warzone = war.warzone(player.getLocation());
						String monumentName = arguments[0];
						if(warzone.hasMonument(monumentName)) {
							// move the existing monument
							Monument monument = warzone.getMonument(monumentName);
							monument.getVolume().resetBlocks();
							monument.setLocation(player.getLocation());
							player.sendMessage(war.str("Monument " + monument.getName() + " was moved."));
						} else {
							// create a new monument
							Monument monument = new Monument(arguments[0], war, warzone, player.getLocation());
							warzone.getMonuments().add(monument);
							player.sendMessage(war.str("Monument " + monument.getName() + " created."));
						}
						WarzoneMapper.save(war, warzone, false);
					}
					event.setCancelled(true); 
				}
				
				// /deletemonument <name>
				else if(command.equals("deletemonument")) {
					if(arguments.length < 1 || !war.inAnyWarzone(player.getLocation())) {
						player.sendMessage(war.str("Usage: /deletemonument <team-name>." +
								" Deletes the monument. " +
								"Must be in a warzone (try /warzones and /warzone). "));
					} else {
						String name = arguments[0];
						Warzone warzone = war.warzone(player.getLocation());
						Monument monument = warzone.getMonument(name);
						if(monument != null) {
							monument.getVolume().resetBlocks();
							warzone.getMonuments().remove(monument);
							WarzoneMapper.save(war, warzone, false);
							player.sendMessage(war.str("Monument " + name + " removed."));
						} else {
							player.sendMessage(war.str("No such monument."));
						}
					}
					event.setCancelled(true); 
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
				player.sendMessage(war.str("You can't do this if you are not a warzone maker."));
			}
		}
    }
	
	public void onPlayerMove(PlayerMoveEvent event) {
		Player player = event.getPlayer();
		Location from = event.getFrom();
		Location to = event.getTo();
		
		// Zone walls
		if(to != null) {
			Warzone nearbyZone = war.zoneOfZoneWallAtProximity(to);
			if(nearbyZone != null && !war.isZoneMaker(player.getName())) {	// zone makers don't get bothered with guard walls
				nearbyZone.protectZoneWallAgainstPlayer(player);
			} else {
				// make sure to delete any wall guards as you leave
				for(Warzone zone : war.getWarzones()) {
					zone.dropZoneWallGuardIfAny(player);
				}
			}
		}
		
		Warzone playerWarzone = war.getPlayerWarzone(player.getName());	// this uses the teams, so it asks: get the player's team's warzone, to be clearer
		if(playerWarzone != null) {
			Team team = war.getPlayerTeam(player.getName());
			
			// Player belongs to a warzone team but is outside: he just died! Handle death! Exempt the zone maker.
			if(from != null && war.warzone(player.getLocation()) == null && team != null && !war.isZoneMaker(player.getName())) {
				// teleport to team spawn upon death
				
				boolean roundOver = false;
				synchronized(playerWarzone) {
					int remaining = team.getRemainingTickets();
					if(remaining == 0) { // your death caused your team to lose
						List<Team> teams = playerWarzone.getTeams();
						for(Team t : teams) {
							t.teamcast(war.str("The battle is over. Team " + team.getName() + " lost: " 
									+ player.getName() + " hit the bottom of their life pool." ));
							t.teamcast(war.str("A new battle begins. The warzone is being reset..."));
							if(!t.getName().equals(team.getName())) {
								// all other teams get a point
								t.addPoint();
								t.resetSign();
							}
						}
						playerWarzone.getVolume().resetBlocks();
						playerWarzone.initializeZone();
						roundOver = true;
					} else {
						team.setRemainingTickets(remaining - 1);
					}
				}
				synchronized(player) {
					if(!roundOver && !war.inAnyWarzone(player.getLocation())) {	// only respawn him if he isnt back at zone yet
						playerWarzone.respawnPlayer(event, team, player);
						player.sendMessage(war.str("You died!"));
						team.resetSign();
						war.getLogger().log(Level.INFO, player.getName() + " died and was tp'd back to team " + team.getName() + "'s spawn");
					} else {
						war.getLogger().log(Level.INFO, player.getName() + " died and battle ended in team " + team.getName() + "'s disfavor");
					}
				}
			}
			
			// Monuments
			if(to != null && team != null
					&& playerWarzone.nearAnyOwnedMonument(to, team) 
					&& player.getHealth() < 20
					&& random.nextInt(42) == 3 ) {	// one chance out of many of getting healed
				player.setHealth(20);
				player.sendMessage(war.str("Your dance pleases the monument's voodoo. You gain full health!"));
			}
		} else if (war.inAnyWarzone(player.getLocation()) && !war.isZoneMaker(player.getName())) { // player is not in any team, but inside warzone boundaries, get him out
			Warzone zone = war.warzone(player.getLocation());
			event.setTo(zone.getTeleport());
			player.sendMessage(war.str("You can't be inside a warzone without a team."));
		}
		
		if(to != null) {
			// Warzone lobby gates
			for(Warzone zone : war.getWarzones()){
				if(zone.getLobby() != null) {
					synchronized(player) {
						Team oldTeam = war.getPlayerTeam(player.getName());
						if(oldTeam == null) { // trying to counter spammy player move
							if(zone.getLobby().isAutoAssignGate(to)) {
								dropFromOldTeamIfAny(player);
								zone.autoAssign(event, player);
							} else if (zone.getLobby().isInTeamGate(TeamMaterials.TEAMDIAMOND, to)){
								dropFromOldTeamIfAny(player);
								Team diamondTeam = zone.getTeamByMaterial(TeamMaterials.TEAMDIAMOND);
								diamondTeam.addPlayer(player);
								zone.keepPlayerInventory(player);
								player.sendMessage(war.str("Your inventory is is storage until you /leave."));
								zone.respawnPlayer(event, diamondTeam, player);
								for(Team team : zone.getTeams()){
									team.teamcast(war.str("" + player.getName() + " joined team diamond."));
								}
							} else if (zone.getLobby().isInTeamGate(TeamMaterials.TEAMIRON, to)){
								dropFromOldTeamIfAny(player);
								Team ironTeam = zone.getTeamByMaterial(TeamMaterials.TEAMIRON);
								ironTeam.addPlayer(player);
								zone.keepPlayerInventory(player);
								player.sendMessage(war.str("Your inventory is is storage until you /leave."));
								zone.respawnPlayer(event, ironTeam, player);
								for(Team team : zone.getTeams()){
									team.teamcast(war.str("" + player.getName() + " joined team iron."));
								}
							} else if (zone.getLobby().isInTeamGate(TeamMaterials.TEAMGOLD, to)){
								dropFromOldTeamIfAny(player);
								Team goldTeam = zone.getTeamByMaterial(TeamMaterials.TEAMGOLD);
								goldTeam.addPlayer(player);
								zone.keepPlayerInventory(player);
								player.sendMessage(war.str("Your inventory is is storage until you /leave."));
								zone.respawnPlayer(event, goldTeam, player);
								for(Team team : zone.getTeams()){
									team.teamcast(war.str("" + player.getName() + " joined team gold."));
								}
							} else if (zone.getLobby().isInWarHubLinkGate(to)){
								dropFromOldTeamIfAny(player);
								event.setTo(to);
								player.teleportTo(war.getWarHub().getLocation());
							}
						} else if(war.inAnyWarzone(event.getFrom())) { // already in a team and in warzone, leaving
							if(zone.getLobby().isAutoAssignGate(to)
									|| zone.getLobby().isInTeamGate(TeamMaterials.TEAMDIAMOND, to)
									|| zone.getLobby().isInTeamGate(TeamMaterials.TEAMIRON, to)
									|| zone.getLobby().isInTeamGate(TeamMaterials.TEAMGOLD, to)) {
								// same as leave, except event.setTo
								Team playerTeam = war.getPlayerTeam(player.getName());
								playerTeam.removePlayer(player.getName());
								event.setTo(playerWarzone.getTeleport());
								player.sendMessage(war.str("Left the zone."));
								playerWarzone.restorePlayerInventory(player);
								player.sendMessage(war.str("Your inventory has (hopefully) been restored."));
							}
						}
					}
				}
			}
			
			// Warhub zone gates
			WarHub hub = war.getWarHub();
			if(hub != null) {
				Warzone zone = hub.getDestinationWarzoneForLocation(player.getLocation());
				synchronized(player) {
					
					if(zone != null
							&& (zone.getTeleport().getBlockX() - player.getLocation().getBlockX() > 10
									|| zone.getTeleport().getBlockZ() - player.getLocation().getBlockZ() > 10)	// trying to prevent effects of spammy player move 
									) {
						event.setTo(zone.getTeleport());
						//player.teleportTo(zone.getTeleport());
						player.sendMessage(war.str("Welcome to warzone " + zone.getName() + "."));
					}
				}
			}
		}
    }
	
	private void dropFromOldTeamIfAny(Player player) {
		// drop from old team if any
		Team previousTeam = war.getPlayerTeam(player.getName());
		if(previousTeam != null) {
			if(!previousTeam.removePlayer(player.getName())){
				war.getLogger().log(Level.WARNING, "Could not remove player " + player.getName() + " from team " + previousTeam.getName());
			}
		}
	}

	private String getAllTeamsMsg(Player player){
		String teamsMessage = "Teams: ";
		if(war.warzone(player.getLocation()).getTeams().isEmpty()){
			teamsMessage += "none.";
		}
		Warzone warzone = war.warzone(player.getLocation());
		for(Team team :warzone.getTeams()) {
			teamsMessage += team.getName() + " (" + team.getPoints() + " points, "+ team.getRemainingTickets() + "/" + warzone.getLifePool() + " lives left. ";
			for(Player member : team.getPlayers()) {
				teamsMessage += member.getName() + " ";
			}
			teamsMessage += ")  ";
		}
		return teamsMessage;
	}

	
	
}
