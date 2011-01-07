package com.tommytony.war;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;

import org.bukkit.Location;
import org.bukkit.Player;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerMoveEvent;



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
		
		// Player commands: /warzones, /warzone, /teams, /join, /leave
		
		// warzones
		if(command.equals("/warzones")){
			
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
			player.sendMessage(war.str(warzonesMessage + "  Use /warzone <zone-name> to " +
					"teleport to a warzone, " +
					"then use /teams and /join <team-name>."));
			event.setCancelled(true); // do i need this?
		}
		
		// warzone
		else if(command.equals("/warzone")) {
			if(split.length < 2) {
				player.sendMessage(war.str("Usage: /warzone <warzone-name>."));
			} else {
				boolean warped = false;
				for(Warzone warzone : war.getWarzones()) {
					if(warzone.getName().equals(split[1])){
						player.teleportTo(warzone.getTeleport());
						warped = true;
						player.sendMessage(war.str("You've landed in warzone " + warzone.getName() +
								". Use the /join command. " + getAllTeamsMsg(player)));
					}
				}
				if(!warped) {
					player.sendMessage("No such warzone.");
				}
			}
			event.setCancelled(true); // do i need this?
		}
		
		// /teams
		else if(command.equals("/teams")){
			if(!war.inAnyWarzone(player.getLocation())) {
				player.sendMessage(war.str("Usage: /teams. " +
						"Must be in a warzone (try /warzones and /warzone)."));
			} else {
				player.sendMessage(war.str("" + getAllTeamsMsg(player)));
			}
			event.setCancelled(true); // do i need this?
		}
		
		// /join <teamname>
		else if(command.equals("/join")) {
			if(split.length < 2 || !war.inAnyWarzone(player.getLocation())) {
				player.sendMessage(war.str("Usage: /join <team-name>." +
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
				String name = split[1];
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
			event.setCancelled(true); // do i need this?
		}
		
		// /leave
		else if(command.equals("/leave")) {
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
			event.setCancelled(true); // do i need this?
		}
		
		
		// /team <msg>
		else if(command.equals("/team")) {
			if(!war.inAnyWarzone(player.getLocation())) {
				player.sendMessage(war.str("Usage: /team <message>. " +
						"Sends a message only to your teammates."));
			} else {
				Team playerTeam = war.getPlayerTeam(player.getName());
				String teamMessage = player.getName();
				for(int j = 1 ; j<split.length; j++) {
					String part = split[j];
					teamMessage += part + " ";
				}
				playerTeam.teamcast(war.str(teamMessage));
			}
			event.setCancelled(true); // do i need this?
		}
		
		// Mod commands : /restartbattle
		
		// /restartbattle
		else if(command.equals("/restartbattle")) {
			if(!war.inAnyWarzone(player.getLocation())) {
				player.sendMessage(war.str("Usage: /restartbattle. Must be in warzone."));
			} else {
				Warzone warzone = war.warzone(player.getLocation());
				for(Team team: warzone.getTeams()) {
					team.teamcast(war.str("The battle has ended. " + getAllTeamsMsg(player) + " Resetting warzone " + warzone.getName() + " and life pools..."));
				}
				int resetBlocks = warzone.resetState();
				player.sendMessage(war.str("Warzone reset. " + resetBlocks + " blocks reset."));
				war.getLogger().info(resetBlocks + " blocks reset in warzone " + warzone.getName() + ".");
			}
			event.setCancelled(true); // do i need this?
		}
		
		// Warzone maker commands: /setwarzone, /savewarzone, /newteam, /setteamspawn, .. /monument?
		
		// /newteam <teamname>
		else if(command.equals("/newteam")) {
			if(split.length < 2 || !war.inAnyWarzone(player.getLocation())) {
				player.sendMessage(war.str("Usage: /newteam <team-name>." +
						" Sets the team spawn to the current location. " +
						"Must be in a warzone (try /warzones and /warzone). "));
			} else {
				String name = split[1];
				Team newTeam = new Team(name, player.getLocation());
				Warzone warzone = war.warzone(player.getLocation());
				newTeam.setRemainingTickets(warzone.getLifePool());
				warzone.getTeams().add(newTeam);
				warzone.addSpawnArea(newTeam, player.getLocation(), 41);				
				player.sendMessage(war.str("Team " + name + " created with spawn here."));
				WarzoneMapper.save(war, warzone, false);
			}
			event.setCancelled(true); // do i need this?
		}
				
		// /setteamspawn 
		else if(command.equals("/teamspawn")) {
			if(split.length < 2 || !war.inAnyWarzone(player.getLocation())) {
				player.sendMessage(war.str("Usage: /setteamspawn <team-name>. " +
						"Sets the team spawn. " +
						"Must be in warzone and team must already exist."));
			} else {
				Warzone warzone = war.warzone(player.getLocation());
				List<Team> teams = warzone.getTeams();
				Team team = null;
				for(Team t : teams) {
					if(t.getName().equals(split[1])) {
						team = t;
					}
				}
				if(team != null) {
					warzone.removeSpawnArea(team);
					warzone.addSpawnArea(team, player.getLocation(), 41);
					team.setTeamSpawn(player.getLocation());
					player.sendMessage(war.str("Team " + team.getName() + " spawn relocated."));
				} else {
					player.sendMessage(war.str("Usage: /setteamspawn <team-name>. " +
							"Sets the team spawn. " +
							"Must be in warzone and team must already exist."));
				}
				
				WarzoneMapper.save(war, warzone, false);
			}
			event.setCancelled(true); // do i need this?
		}
		
		// /deleteteam <teamname>
		else if(command.equals("/deleteteam")) {
			if(split.length < 2 || !war.inAnyWarzone(player.getLocation())) {
				player.sendMessage(war.str("Usage: /deleteteam <team-name>." +
						" Deletes the team and its spawn. " +
						"Must be in a warzone (try /warzones and /warzone). "));
			} else {
				String name = split[1];
				Warzone warzone = war.warzone(player.getLocation());
				List<Team> teams = warzone.getTeams();
				Team team = null;
				for(Team t : teams) {
					if(name.equals(t.getName())) {
						team = t;
					}
				}
				if(team != null) {
					warzone.removeSpawnArea(team);	
					warzone.getTeams().remove(team);
					WarzoneMapper.save(war, warzone, false);
					player.sendMessage(war.str("Team " + name + " removed."));
				} else {
					player.sendMessage(war.str("No such team."));
				}
			}
			event.setCancelled(true); // do i need this?
		}
		
		// /setwarzone
		else if(command.equals("/setwarzone")) {
			if(split.length < 3 || (split.length == 3 && (!split[2].equals("southeast") && !split[2].equals("northwest")
															&& !split[2].equals("se") && !split[2].equals("nw")))) {
				player.sendMessage(war.str("Usage: /setwarzone <warzone-name> <'southeast'/'northwest'>. " +
						"Defines the battleground boundary. " +
						"The warzone is reset at the start of every battle. " +
						"This command overwrites any previously saved blocks " +
						"(i.e. make sure you reset with /restartbattle " +
						"or /resetwarzone before changing the boundary). "));
			} else {
				Warzone warzone = war.findWarzone(split[1]);
				if(warzone == null) {
					// create the warzone
					warzone = new Warzone(war, player.getLocation().getWorld(), split[1]);
					war.addWarzone(warzone);
					WarMapper.save(war);
					if(split[2].equals("northwest") || split[2].equals("nw")) {
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
					if(split[2].equals("northwest") || split[2].equals("nw")) {
						warzone.setNorthwest(player.getLocation());
						message += "Northwesternmost point set at x=" + (int)warzone.getNorthwest().getBlockX() 
										+ " z=" + (int)warzone.getNorthwest().getBlockZ() + " on warzone " + warzone.getName() + ".";
					} else {
						warzone.setSoutheast(player.getLocation());
						message += "Southeasternmost point set at x=" + (int)warzone.getSoutheast().getBlockX()
										+ " z=" + (int)warzone.getSoutheast().getBlockZ() + " on warzone " + warzone.getName() + ".";
					}
					
					if(warzone.getNorthwest() == null) {
						message += " Still missing northwesternmost point.";
					}
					if(warzone.getSoutheast() == null) {
						message += " Still missing southeasternmost point.";
					}
					if(warzone.getNorthwest() != null && warzone.getSoutheast() != null) {
						if(warzone.ready()) {
							message += " Warzone " + warzone.getName() + " almost ready. Use /newteam while inside the warzone to create new teams. Make sure to use /setwarzonestart to " +
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
			event.setCancelled(true); // do i need this?
		}		

		// /savewarzone
		else if(command.equals("/savewarzone")) {
			if(!war.inAnyWarzone(player.getLocation())) {
				player.sendMessage(war.str("Usage: /savewarzone. Must be in warzone. " +
						"Changes the warzone state at the beginning of every battle. " +
						"Also sets the teleport point for this warzone " +
						"(i.e. make sure to use /warzone or the warzone tp point will change). " +
						"Just like /setwarzone, this command overwrites any previously saved blocks " +
						"(i.e. make sure you reset with /restartbattle " +
						"or /resetwarzone before changing start state). "));
			} else {
				Warzone warzone = war.warzone(player.getLocation());
				int savedBlocks = warzone.saveState();
				warzone.setTeleport(player.getLocation());
				player.sendMessage(war.str("Warzone " + warzone.getName() + " initial state and teleport location changed. Saved " + savedBlocks + " blocks."));
				WarzoneMapper.save(war, warzone, true);
			}
			event.setCancelled(true); // do i need this?
		}
		
		// /resetwarzone
		else if(command.equals("/resetwarzone")) {
			if(!war.inAnyWarzone(player.getLocation())) {
				player.sendMessage(war.str("Usage: /resetwarzone <life pool size (optional)>. Must be in warzone."));
			} else {
				Warzone warzone = war.warzone(player.getLocation());
				int resetBlocks = warzone.resetState();
				for(Team team: warzone.getTeams()) {
					team.teamcast(war.str("The war has ended. " + getAllTeamsMsg(player) + " Resetting warzone " + warzone.getName() + " and teams..."));
					for(Player p : team.getPlayers()) {
						p.teleportTo(warzone.getTeleport());
						warzone.restorePlayerInventory(p);
						player.sendMessage(war.str("You are now teamless. Your inventory has (hopefully) been restored."));
					}
				}
				war.getWarzones().remove(warzone);
				Warzone resetWarzone = WarzoneMapper.load(war, warzone.getName(), true);
				war.getWarzones().add(resetWarzone);
				if(split.length > 1) {
					int overrideLifepool = Integer.parseInt(split[1]);
					resetWarzone.setLifePool(overrideLifepool);
				}
				resetWarzone.resetState();
				player.sendMessage(war.str("Warzone and teams reset. " + resetBlocks + " blocks reset."));
				war.getLogger().info(resetBlocks + " blocks reset in warzone " + warzone.getName() + ".");
			}
			event.setCancelled(true); // do i need this?
		}
		
		// /deletewarzone
		else if(command.equals("/deletewarzone")) {
			if(!war.inAnyWarzone(player.getLocation())) {
				player.sendMessage(war.str("Usage: /deletewarzone." +
						" Deletes the warzone. " +
						"Must be in the warzone (try /warzones and /warzone). "));
			} else {
				Warzone warzone = war.warzone(player.getLocation());
				warzone.removeSoutheast();
				warzone.removeNorthwest();
				for(Team t : warzone.getTeams()) {
					warzone.removeSpawnArea(t);
				}
				for(Monument m : warzone.getMonuments()) {
					m.remove();
				}
				war.getWarzones().remove(warzone);
				WarMapper.save(war);
				WarzoneMapper.delete(war, warzone.getName());
				player.sendMessage(war.str("Warzone " + warzone.getName() + " removed."));
			}
			event.setCancelled(true); // do i need this?
		}
		
		// /monument
		else if(command.equals("/monument")) {
			if(!war.inAnyWarzone(player.getLocation())) {
				player.sendMessage(war.str("Usage: /monument <name>. Must be in warzone."));
			} else {
				Warzone warzone = war.warzone(player.getLocation());
				String monumentName = split[1];
				if(warzone.hasMonument(monumentName)) {
					// move the existing monument
					Monument monument = warzone.getMonument(monumentName);
					monument.remove();
					monument.setLocation(player.getLocation());
					player.sendMessage(war.str("Monument " + monument.getName() + " was moved."));
				} else {
					// create a new monument
					Monument monument = new Monument(split[1], warzone.getWorld(), player.getLocation());
					warzone.getMonuments().add(monument);
					player.sendMessage(war.str("Monument " + monument.getName() + " created."));
				}
				WarzoneMapper.save(war, warzone, false);
			}
			event.setCancelled(true); // do i need this?
		}
		
		// /deletemonument <name>
		else if(command.equals("/deletemonument")) {
			if(split.length < 2 || !war.inAnyWarzone(player.getLocation())) {
				player.sendMessage(war.str("Usage: /deletemonument <team-name>." +
						" Deletes the monument. " +
						"Must be in a warzone (try /warzones and /warzone). "));
			} else {
				String name = split[1];
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
			event.setCancelled(true); // do i need this?
		}
    }
	
	public void onPlayerMove(PlayerMoveEvent event) {
		Player player = event.getPlayer();
		Location from = event.getFrom();
		Location to = event.getTo();
		
		Warzone playerWarzone = war.getPlayerWarzone(player.getName());
		Team playerTeam = war.getPlayerTeam(player.getName());
		if(player != null && from != null && to != null && 
				playerTeam != null && !playerWarzone.contains(to)) {
			player.sendMessage(war.str("Can't go outside the warzone boundary! Use /leave to exit the battle."));
			if(playerWarzone.contains(from)){
				player.teleportTo(from);
			} else {
				// somehow the player made it out of the zone
				player.teleportTo(playerTeam.getTeamSpawn());
				player.sendMessage(war.str("Brought you back to your team spawn. Use /leave to exit the battle."));
			}
		}
		
		if(player != null && from != null && to != null && 
				playerTeam == null 
				&& war.inAnyWarzone(from) 
				&& !war.inAnyWarzone(to)) {
			// leaving
			Warzone zone = war.warzone(from);
			player.sendMessage(war.str("Leaving warzone " + zone.getName() + "."));
		}
		
		if(player != null && from != null && to != null && 
				playerTeam == null 
				&& !war.inAnyWarzone(from) 
				&& war.inAnyWarzone(to)) {
			// entering
			Warzone zone = war.warzone(to);
			player.sendMessage(war.str("Entering warzone " + zone.getName() + ". Tip: use /teams."));
		}
		
		if(to != null && playerTeam != null
				&& playerWarzone.nearAnyOwnedMonument(to, playerTeam) 
				&& player.getHealth() < 20
				&& random.nextInt(42) == 3 ) {	// one chance out of many of getting healed
			player.setHealth(20);
			player.sendMessage(war.str("Your dance pleases the monument's voodoo. You gain full health!"));
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
