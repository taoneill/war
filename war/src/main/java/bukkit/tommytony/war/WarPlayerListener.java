package bukkit.tommytony.war;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Player;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerMoveEvent;

import com.tommytony.war.Monument;
import com.tommytony.war.Team;
import com.tommytony.war.TeamMaterials;
import com.tommytony.war.Warzone;
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
					player.sendMessage(war.str("Left the team. You can now exit the warzone."));
					Warzone zone = war.warzone(player.getLocation());
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
			
			// Mod commands : /nextbattle
			
			// /restartbattle
			else if(command.equals("nextbattle") || command.equals("restartbattle")) {
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
					} else {
						String message = "";
						if(arguments[1].equals("northwest") || arguments[1].equals("nw")) {
							int reset = warzone.getVolume().resetBlocks();
							warzone.setNorthwest(player.getLocation());
							warzone.saveState();
							warzone.initializeZone();
							message += "Northwesternmost point set at x=" + (int)warzone.getNorthwest().getBlockX() 
											+ " z=" + (int)warzone.getNorthwest().getBlockZ() + " on warzone " + warzone.getName() + ". " +
											reset + " blocks reset. New zone saved.";
						} else {
							int reset = warzone.getVolume().resetBlocks();
							warzone.setSoutheast(player.getLocation());
							warzone.saveState();
							warzone.initializeZone();
							message += "Southeasternmost point set at x=" + (int)warzone.getSoutheast().getBlockX()
											+ " z=" + (int)warzone.getSoutheast().getBlockZ() + " on warzone " + warzone.getName() + ". " +
											reset + " blocks reset. New zone saved.";
						}
						
						if(warzone.getNorthwest() == null) {
							message += " Still missing northwesternmost point.";
						}
						if(warzone.getSoutheast() == null) {
							message += " Still missing southeasternmost point.";
						}
						if(warzone.getNorthwest() != null && warzone.getSoutheast() != null) {
							if(warzone.ready()) {
								message += " Warzone " + warzone.getName() + " almost ready. Use /setteam while inside the warzone to create new teams. Make sure to use /savezone to " +
										"set the warzone teleport point and initial state.";
							} else if (warzone.tooSmall()) {
								message += " Warzone " + warzone.getName() + " is too small. Min north-south size: 20. Min east-west size: 20.";
							} else if (warzone.tooBig()) {
								message += " Warzone " + warzone.getName() + " is too Big. Max north-south size: 1000. Max east-west size: 1000.";
							}
						}
						player.sendMessage(war.str(message));
					}
					WarzoneMapper.save(war, warzone, false);
					
				}
				event.setCancelled(true); 
			}		
	
			// /savewarzone
			else if(command.equals("savezone") || command.equals("savewarzone")) {
				if(!war.inAnyWarzone(player.getLocation())) {
					player.sendMessage(war.str("Usage: /savezone. Must be in warzone. " +
							"Changes the warzone state loaded at the beginning of every battle. " +
							"Also sets the teleport point for this warzone where you're standing." +
							"(i.e. make sure to use /zone or the warzone tp point will change). " +
							"Just like /setzone, this command overwrites any previously saved blocks " +
							"(i.e. make sure you reset with /restartbattle " +
							"or /resetzone before changing start state). "));
				} else {
					Warzone warzone = war.warzone(player.getLocation());
					int savedBlocks = warzone.saveState();
					warzone.setTeleport(player.getLocation());
					player.sendMessage(war.str("Warzone " + warzone.getName() + " initial state and teleport location changed. Saved " + savedBlocks + " blocks."));
					WarzoneMapper.save(war, warzone, true);
				}
				event.setCancelled(true); 
			}
			
			// /resetwarzone
			else if(command.equals("resetzone") || command.equals("resetwarzone")) {
				if(!war.inAnyWarzone(player.getLocation())) {
					player.sendMessage(war.str("Usage: /resetzone pool=10. Reloads the zone. All named parameter are optional. Defaults: pool=7 maxScore=-1 (infinite). Must be in warzone."));
				} else {
					Warzone warzone = war.warzone(player.getLocation());
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
						m.remove();
					}
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
						monument.remove();
						warzone.getMonuments().remove(monument);
						WarzoneMapper.save(war, warzone, false);
						player.sendMessage(war.str("Monument " + name + " removed."));
					} else {
						player.sendMessage(war.str("No such monument."));
					}
				}
				event.setCancelled(true); 
			}
		}
    }
	
	public void onPlayerMove(PlayerMoveEvent event) {
		Player player = event.getPlayer();
		Location from = event.getFrom();
		Location to = event.getTo();
		
		// Zone walls
		if(to != null) {
			Warzone nearbyZone = war.zoneOfTooCloseZoneWall(to);
			if(nearbyZone != null) {
				nearbyZone.protectZoneWallAgainstPlayer(player);
			} else {
				// make sure to delete any wall guards as you leave
				for(Warzone zone : war.getWarzones()) {
					zone.dropZoneWallGuardIfAny(player);
				}
			}
		}
		
		Warzone playerWarzone = war.getPlayerWarzone(player.getName());
		if(playerWarzone != null) {
			Team playerTeam = war.getPlayerTeam(player.getName());
			
			
			// Monuments
			if(to != null && playerTeam != null
					&& playerWarzone.nearAnyOwnedMonument(to, playerTeam) 
					&& player.getHealth() < 20
					&& random.nextInt(42) == 3 ) {	// one chance out of many of getting healed
				player.setHealth(20);
				player.sendMessage(war.str("Your dance pleases the monument's voodoo. You gain full health!"));
			}
					
//			if(player != null && from != null && to != null && 
//					playerTeam != null && !playerWarzone.getVolume().contains(to)) {
//				player.sendMessage(war.str("Can't go outside the warzone boundary! Use /leave to exit the battle."));
//				if(playerWarzone.getVolume().contains(from)){
//					player.teleportTo(from);
//				} else {
//					// somehow the player made it out of the zone
//					player.teleportTo(playerTeam.getTeamSpawn());
//					player.sendMessage(war.str("Brought you back to your team spawn. Use /leave to exit the battle."));
//				}
//			}
//			
//			if(player != null && from != null && to != null && 
//					playerTeam == null 
//					&& war.inAnyWarzone(from) 
//					&& !war.inAnyWarzone(to)) {
//				// leaving
//				Warzone zone = war.warzone(from);
//				player.sendMessage(war.str("Leaving warzone " + zone.getName() + "."));
//			}
//			
//			if(player != null && from != null && to != null && 
//					playerTeam == null 
//					&& !war.inAnyWarzone(from) 
//					&& war.inAnyWarzone(to)) {
//				// entering
//				Warzone zone = war.warzone(to);
//				player.sendMessage(war.str("Entering warzone " + zone.getName() + ". Tip: use /teams."));
//			}
			
			
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
