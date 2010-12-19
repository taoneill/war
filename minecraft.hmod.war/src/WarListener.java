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
		
		// Player commands: /warzones, /warzone, /teams, /join
		
		// warzones
		if(command.equals("/warzones")){
			
			String warzonesMessage = "Warzones: ";
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
					"teleport to warzone, " +
					"then use /teams and /join <team-name>."));
			return true;
		}
		
		// warzone
		else if(command.equals("/warzone")) {
			if(split.length < 2) {
				player.sendMessage(war.str("Usage: /warzone <warzone-name>."));
			} else {
				for(Warzone warzone : war.getWarzones()) {
					if(warzone.getName().equals(split[2])){
						player.teleportTo(warzone.getTeleport());
						player.sendMessage(war.str("You've landed in the " + warzone.getName() +
								" warzone. Use the /join command. " + getAllTeamsMsg(player)));
						return true;
					}
				}
				player.sendMessage("So such warzone.");
			}
			return true;
		}
		
		// /teams
		if(command.equals("/teams")){
			if(split.length < 2 || !war.inAnyWarzone(player.getLocation())) {
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
				String name = split[1];
				List<Team> teams = war.warzone(player.getLocation()).getTeams();
				boolean foundTeam = false;
				for(Team team : teams) {
					if(team.getName().equals(name)) {
						team.addPlayer(player);
						foundTeam = true;
					}
				}
				if(foundTeam) {
					etc.getServer().messageAll(war.str("" + player.getName() + " joined " + name));
				} else {
					player.sendMessage(war.str("No such team. Try /teams."));
				}
			}
			return true;
		}
		
		
		// /team <msg>
		else if(command.equals("/team")) {
			if(split.length < 2) {
				player.sendMessage(war.str("Usage: /team <message>. " +
						"Sends a message only to your teammates."));
			} else {
				Team playerTeam = war.getPlayerTeam(player.getName());
				String teamMessage = "<"+ player.getName() + ":> ";
				for(int j = 1 ; j<split.length; j++) {
					String part = split[j];
					teamMessage += part + " ";
				}
				playerTeam.teamcast(war.str(Colors.LightBlue, teamMessage));
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
					team.teamcast(war.str("Resetting warzone."));
				}
				warzone.resetState();
				player.sendMessage(war.str("Warzone reset."));
			}
			return true;
		}
		
		
		// Warzone maker commands: /setwarzone, /setwarzonestart, /resetwarzone, /newteam, /setteamspawn, .. /setmonument
		
		// /newteam <teamname>
		else if(command.equals("/newteam")) {
			if(split.length < 2 || !war.inAnyWarzone(player.getLocation())) {
				player.sendMessage(war.str("Usage: /newteam <team-name>." +
						" Sets the team spawn to the current location. " +
						"Must be in a warzone (try /warzones and /warzone). "));
			} else {
				String name = split[1];
				war.warzone(player.getLocation()).getTeams().add(new Team(name, player.getLocation()));
				player.sendMessage(war.str("Team created with spawn here."));
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
				player.sendMessage(war.str("Team spawn relocated."));
			}
			return true;
		}
		
		// /setwarzone
		else if(command.equals("/setwarzone")) {
			if(split.length < 3 || (split.length == 3 && (!split[2].equals("southeast") && !split[2].equals("northwest")))) {
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
					if(split[2].equals("northwest")) {
						newZone.setNorthwest(player.getLocation());
						player.sendMessage(war.str("Warzone added. Northwesternmost point set."));
					} else {
						newZone.setSoutheast(player.getLocation());
						player.sendMessage(war.str("Warzone added. Southeasternmost point set."));
					}
				} else {
					String message = "";
					if(split[2].equals("northwest")) {
						warzone.setNorthwest(player.getLocation());
						message += "Northwesternmost point set." ;
					} else {
						warzone.setSoutheast(player.getLocation());
						message += "Southeasternmost point set.";
					}
					
					if(warzone.getNorthwest() == null) {
						message += " Still missing northwesternmost point.";
					}
					if(warzone.getSoutheast() == null) {
						message += " Still missing southeasternmost point.";
					}
					if(warzone.getNorthwest() != null && warzone.getSoutheast() != null) {
						if(warzone.ready()) {
							message += " Warzone ready. Use /newteam while inside the warzone to create new teams. Make sure to use /setwarzonestart to " +
									"set the warzone teleport point and initial state.";
						} else if (warzone.tooSmall()) {
							message += " Warzone is too small. Min north-south size: 20. Min east-west size: 20.";
						} else if (warzone.tooBig()) {
							message += " Warzone is too Big. Max north-south size: 1000. Max east-west size: 1000.";
						}
					}
					player.sendMessage(war.str(message));
				}
				
			}
			return true;
		}
		

		// /setwarzonestate
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
				warzone.saveState();
				warzone.setTeleport(player.getLocation());
				player.sendMessage(war.str("Warzone initial state and teleport location changed."));
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
					team.teamcast(war.str("Resetting warzone..."));
				}
				warzone.resetState();
				player.sendMessage(war.str("Warzone reset."));
			}
			return true;
		}
		
        return false;
    }
	
	public boolean onDamage(PluginLoader.DamageType damageType, BaseEntity attacker, BaseEntity defender, int damageAmount) {
		if(attacker != null && defender != null && attacker.isPlayer() && defender.isPlayer()) {
			// only let adversaries (same warzone, different team) attack each other
			Player a = (Player) attacker;
			Player d = (Player) defender;
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
				a.sendMessage(war.str("Your attack was blocked!" +
						" You must join a team " +
						", then you'll be able to damage people " +
						"in the other teams in that warzone."));
				return true; // no pvp outside of the war battles, no friendly fire either
			}
		}
		// mobs are always dangerous
		return false;
	}

	public boolean onHealthChange(Player player, int before, int after) {
		
		if(after < 0) {
			Team team = war.getPlayerTeam(player.getName());
			if(team != null){
				// teleport to team spawn upon death
				player.setHealth(20);
				player.teleportTo(team.getTeamSpawn());
				war.getLogger().log(Level.INFO, player.getName() + " died and was tp'd back to team " + team.getName() + "'s spawn");
				return true;
			}
		}
		return false;
	}
	
	private String getAllTeamsMsg(Player player){
		String teamsMessage = "Teams: ";
		for(Team team : war.warzone(player.getLocation()).getTeams()) {
			teamsMessage += team.getName() + " (";
			for(Player member : team.getPlayers()) {
				teamsMessage += member.getName() + " ";
			}
			teamsMessage += ")  ";
		}
		return teamsMessage;
	}
}
