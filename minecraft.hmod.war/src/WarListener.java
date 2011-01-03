import java.util.List;
import java.util.logging.Level;



public class WarListener extends PluginListener {

	private final War war;

	public WarListener(War war) {
		this.war = war;
	}
	
	public void onLogin(Player player) {    
		player.sendMessage(war.str("War is on! Pick your battle (try /warzones)."));
    }
	
	public boolean onCommand(Player player, java.lang.String[] split) {
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
			return true;
		}
		
		// warzone
		else if(command.equals("/warzone")) {
			if(split.length < 2) {
				player.sendMessage(war.str("Usage: /warzone <warzone-name>."));
			} else {
				for(Warzone warzone : war.getWarzones()) {
					if(warzone.getName().equals(split[1])){
						player.teleportTo(warzone.getTeleport());
						player.sendMessage(war.str("You've landed in warzone " + warzone.getName() +
								". Use the /join command. " + getAllTeamsMsg(player)));
						return true;
					}
				}
				player.sendMessage("No such warzone.");
			}
			return true;
		}
		
		// /teams
		else if(command.equals("/teams")){
			if(!war.inAnyWarzone(player.getLocation())) {
				player.sendMessage(war.str("Usage: /teams. " +
						"Must be in a warzone (try /warzones and /warzone)."));
			} else {
				player.sendMessage(war.str("" + getAllTeamsMsg(player)));
			}
			return true;
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
				List<Team> teams = war.warzone(player.getLocation()).getTeams();
				boolean foundTeam = false;
				for(Team team : teams) {
					if(team.getName().equals(name)) {
						team.addPlayer(player);
						player.teleportTo(team.getTeamSpawn());
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
			return true;
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
			}
			return true;
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
			return true;
		}
		
		// Mod commands : /restartbattle
		
		// /restartbattle
		else if(command.equals("/restartbattle")) {
			if(!war.inAnyWarzone(player.getLocation())) {
				player.sendMessage(war.str("Usage: /restartbattle. Must be in warzone."));
			} else {
				Warzone warzone = war.warzone(player.getLocation());
				for(Team team: warzone.getTeams()) {
					team.teamcast(war.str("The battle has ended. " + getAllTeamsMsg(player) + " Resetting warzone " + warzone.getName() + "..."));
				}
				int resetBlocks = warzone.resetState();
				player.sendMessage(war.str("Warzone reset. " + resetBlocks + " blocks reset."));
			}
			return true;
		}
		
		
		// Warzone maker commands: /setwarzone, /setwarzonestart, /resetwarzone, /newteam, /setteamspawn, .. /setmonument?
		
		// /newteam <teamname>
		else if(command.equals("/newteam")) {
			if(split.length < 2 || !war.inAnyWarzone(player.getLocation())) {
				player.sendMessage(war.str("Usage: /newteam <team-name>." +
						" Sets the team spawn to the current location. " +
						"Must be in a warzone (try /warzones and /warzone). "));
			} else {
				String name = split[1];
				war.warzone(player.getLocation()).getTeams().add(new Team(name, player.getLocation()));
				player.sendMessage(war.str("Team " + name + " created with spawn here."));
			}
			return true;
		}
				
		// /setteamspawn 
		else if(command.equals("/setteamspawn")) {
			if(split.length < 2 || war.getPlayerTeam(player.getName()) == null) {
				player.sendMessage(war.str("Usage: /setteamspawn <team-name>. " +
						"Sets the team spawn. " +
						"Must be in warzone and team must already exist."));
			} else {
				Team playerTeam = war.getPlayerTeam(player.getName());
				playerTeam.setTeamSpawn(player.getLocation());
				player.sendMessage(war.str("Team " + playerTeam + " spawn relocated."));
			}
			return true;
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
					Warzone newZone = new Warzone(war.getServer(), split[1]);
					war.addWarzone(newZone);
					if(split[2].equals("northwest") || split[2].equals("nw")) {
						newZone.setNorthwest(player.getLocation());
						player.sendMessage(war.str("Warzone " + newZone.getName() + " added. Northwesternmost point set at x=" + (int)newZone.getNorthwest().x + " z=" + (int)newZone.getNorthwest().z + "."));
					} else {
						newZone.setSoutheast(player.getLocation());
						player.sendMessage(war.str("Warzone " + newZone.getName() + " added. Southeasternmost point set at x=" + (int)newZone.getSoutheast().x + " z=" + (int)newZone.getSoutheast().z + "."));
					}
				} else {
					String message = "";
					if(split[2].equals("northwest") || split[2].equals("nw")) {
						warzone.setNorthwest(player.getLocation());
						message += "Northwesternmost point set at x=" + (int)warzone.getNorthwest().x + " z=" + (int)warzone.getNorthwest().z + " on warzone " + warzone.getName() + ".";
					} else {
						warzone.setSoutheast(player.getLocation());
						message += "Southeasternmost point set at x=" + (int)warzone.getSoutheast().x + " z=" + (int)warzone.getSoutheast().z + " on warzone " + warzone.getName() + ".";
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
				
			}
			return true;
		}
		

		// /setwarzonestart
		else if(command.equals("/setwarzonestart")) {
			if(!war.inAnyWarzone(player.getLocation())) {
				player.sendMessage(war.str("Usage: /setwarzonestart. Must be in warzone. " +
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
			}
			return true;
		}
		
		// /resetwarzone
		else if(command.equals("/resetwarzone")) {
			if(!war.inAnyWarzone(player.getLocation())) {
				player.sendMessage(war.str("Usage: /resetwarzone. Must be in warzone."));
			} else {
				Warzone warzone = war.warzone(player.getLocation());
				for(Team team: warzone.getTeams()) {
					team.teamcast(war.str("Resetting warzone " + warzone.getName() + "..."));
				}
				int resetBlocks = warzone.resetState();
				Location playerLoc = player.getLocation();
				player.sendMessage(war.str("Warzone reset. " + resetBlocks + " blocks reset."));
			}
			return true;
		}
		
        return false;
    }
	
	public boolean onDamage(PluginLoader.DamageType damageType, BaseEntity attacker, BaseEntity defender, int damageAmount) {
		if(attacker != null && defender != null && attacker.isPlayer() && defender.isPlayer()) {
			// only let adversaries (same warzone, different team) attack each other
			Player a = attacker.getPlayer();
			Player d = defender.getPlayer();
			Warzone attackerWarzone = war.warzone(a.getLocation());
			Team attackerTeam = war.getPlayerTeam(a.getName());
			Warzone defenderWarzone = war.warzone(d.getLocation());
			Team defenderTeam = war.getPlayerTeam(d.getName());
			if(attackerTeam != null && defenderTeam != null 
					&& attackerTeam != defenderTeam 			
					&& attackerWarzone == defenderWarzone) {
				war.getLogger().log(Level.INFO, a.getName() + " hit " + d.getName() + " for " + damageAmount);
				return false;	// adversaries!
			} else {
				a.sendMessage(war.str("Your attack missed!"));
				if(attackerTeam == null) {
					a.sendMessage(war.str(" You must join a team " +
						", then you'll be able to damage people " +
						"in the other teams in that warzone."));
				} else if (defenderTeam == null) {
					a.sendMessage(war.str("Your target is not in a team."));
				} else if (attackerTeam == defenderTeam) {
					a.sendMessage(war.str("Your target is on your team."));
				} else if (attackerWarzone != defenderWarzone) {
					a.sendMessage(war.str("Your target is playing in another warzone."));
				}
				return true; // no pvp outside of the war battles, no friendly fire either
			}
		}
		// mobs are always dangerous
		return false;
	}

	public boolean onHealthChange(Player player, int before, int after) {
		
		if(after <= 0) {
			Team team = war.getPlayerTeam(player.getName());
			if(team != null){
				// teleport to team spawn upon death
				Warzone zone = war.warzone(player.getLocation());
				boolean roundOver = false;
				synchronized(zone) {
					int remaining = team.getRemainingTickets();
					if(remaining == 0) { // your death caused your team to lose
						player.sendMessage(war.str("You died and your team's life pool was empty!"));
						List<Team> teams = zone.getTeams();
						for(Team t : teams) {
							t.teamcast(war.str(player.getName() + " died but team " + team.getName() + "'s lifepool was empty. The battle is over."));
						}
						zone.resetState();
						roundOver = true;
					} else {
						team.setRemainingTickets(remaining - 1);
					}
				}
				if(!roundOver) {
					player.teleportTo(team.getTeamSpawn());
					player.setHealth(20);
					player.sendMessage(war.str("You died!"));
					war.getLogger().log(Level.INFO, player.getName() + " died and was tp'd back to team " + team.getName() + "'s spawn");
				} else {
					war.getLogger().log(Level.INFO, player.getName() + " died and battle ended in team " + team.getName() + "'s disfavor");
				}
				
				
				
				
				return true;
			}
		}
		return false;
	}
	
	public void onPlayerMove(Player player, Location from, Location to) {
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
    }
	
	private String getAllTeamsMsg(Player player){
		String teamsMessage = "Teams: ";
		if(war.warzone(player.getLocation()).getTeams().isEmpty()){
			teamsMessage += "none.";
		}
		for(Team team : war.warzone(player.getLocation()).getTeams()) {
			teamsMessage += team.getName() + " (" + team.getRemainingTickets() + "/" + War.LIFEPOOL + " lives left. ";
			for(Player member : team.getPlayers()) {
				teamsMessage += member.getName() + " ";
			}
			teamsMessage += ")  ";
		}
		return teamsMessage;
	}
}
