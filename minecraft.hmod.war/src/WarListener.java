import java.util.List;

public class WarListener extends PluginListener {

	private final War war;

	public WarListener(War war) {
		this.war = war;
	}
	
	public void onLogin(Player player) {    
		player.sendMessage(Colors.Gray + "[war] War is on! You must pick sides (try /teams and /join).");
    }
	
	public boolean onCommand(Player player, java.lang.String[] split) {
		String command = split[0];
		
		// Player commands: /warzones, /warzone, /teams, /join
		
		// warzones
		if(command.equals("/warzones")){
			
			String warzonesMessage = Colors.Gray + "[war] Warzones: ";
			for(Warzone warzone : war.getWarzones()) {
				
				warzonesMessage += warzone.getName() + " ("
				+ warzone.getTeams().size() + " teams, ";
				int playerTotal = 0;
				for(Team team : warzone.getTeams()) {
					playerTotal += team.getPlayers().size();
				}
				warzonesMessage += playerTotal + " players)  ";
			}
			player.sendMessage(warzonesMessage + "  Use /warzone <zone-name> to teleport to warzone, then use /teams and /join <team-name>.");
			return true;
		}
		
		// warzone
		else if(command.equals("/warzone")) {
			if(split.length < 2) {
				player.sendMessage(Colors.Gray + "[war] Usage: /warzone <warzone-name>.");
			} else {
				for(Warzone warzone : war.getWarzones()) {
					if(warzone.getName().equals(split[2])){
						player.teleportTo(warzone.getTeleport());
						return true;
					}
				}
				player.sendMessage(Colors.Gray + "[war] So such warzone.");
			}
			return true;
		}
		
		// /teams
		if(command.equals("/teams")){
			if(split.length < 2 || !war.inAnyWarzone(player.getLocation())) {
				player.sendMessage(Colors.Gray + "[war] Usage: /teams. Must be in a warzone (try /warzones and /warzone).");
			} else {
				String teamsMessage = Colors.Gray + "[war] Teams: ";
				for(Team team : war.warzone(player.getLocation()).getTeams()) {
					teamsMessage += team.getName() + " (";
					for(Player member : team.getPlayers()) {
						teamsMessage += member.getName() + " ";
					}
					teamsMessage += ")  ";
				}
				player.sendMessage(teamsMessage);
			}
			return true;
		}
		
		// /join <teamname>
		else if(command.equals("/join")) {
			if(split.length < 2 || !war.inAnyWarzone(player.getLocation())) {
				player.sendMessage(Colors.Gray + "[war] Usage: /join <team-name>. Teams are warzone specific. You must be inside a warzone to join a team.");
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
					player.sendMessage(Colors.Gray + Colors.Gray + "[war] Joined " + name);
					etc.getServer().messageAll(Colors.Gray + "[war] " + player.getName() + " joined " + name);
				} else {
					player.sendMessage(Colors.Gray + "[war] No such team. Try /teams.");
				}
			}
			return true;
		}
		
		
		// /team <msg>
		else if(command.equals("/team")) {
			if(split.length < 2) {
				player.sendMessage(Colors.Gray + "[war] Usage: /team <message>. Sends a message only to your teammates.");
			} else {
				Team playerTeam = war.getPlayerTeam(player.getName());
				String teamMessage = Colors.LightBlue + "<"+ player.getName() + ":> ";
				for(int j = 1 ; j<split.length; j++) {
					String part = split[j];
					teamMessage += part + " ";
				}
				playerTeam.teamcast(teamMessage);
			}
			return true;
		}
		
		// Mod commands : /restartbattle
		
		// /restartbattle
		else if(command.equals("/restartbattle")) {
			if(!war.inAnyWarzone(player.getLocation())) {
				player.sendMessage(Colors.Gray + "[war] Usage: /restartbattle. Must be in warzone.");
			} else {
				Warzone warzone = war.warzone(player.getLocation());
				for(Team team: warzone.getTeams()) {
					team.teamcast(Colors.Gray + "[war] Resetting warzone.");
				}
				warzone.resetState();
				player.sendMessage(Colors.Gray + "[war] Warzone reset.");
			}
			return true;
		}
		
		
		// Warzone maker commands: /setwarzone, /setwarzonestart, /resetwarzone, /newteam, /setteamspawn, .. /setmonument
		
		// /newteam <teamname>
		else if(command.equals("/newteam")) {
			if(split.length < 2 || !war.inAnyWarzone(player.getLocation())) {
				player.sendMessage(Colors.Gray + "[war] Usage: /newteam <team-name>. Sets the team spawn to the current location. Must be in a warzone (try /warzones and /warzone). ");
			} else {
				String name = split[1];
				war.warzone(player.getLocation()).getTeams().add(new Team(name, player.getLocation()));
				player.sendMessage(Colors.Gray + "[war] Team created with spawn here.");
			}
			return true;
		}
				
		// /setteamspawn 
		else if(command.equals("/setteamspawn")) {
			if(split.length < 2 || war.getPlayerTeam(player.getName()) == null) {
				player.sendMessage(Colors.Gray + "[war] Usage: /setteamspawn <team-name>. Sets the team spawn. Must be in warzone and team must already exist.");
			} else {
				Team playerTeam = war.getPlayerTeam(player.getName());
				playerTeam.setTeamSpawn(player.getLocation());
				player.sendMessage(Colors.Gray + "[war] Team spawn relocated.");
			}
			return true;
		}
		
		// /setwarzone
		else if(command.equals("/setwarzone")) {
			if(split.length < 3 || (split.length == 3 && (!split[2].equals("southeast") && !split[2].equals("northwest")))) {
				player.sendMessage(Colors.Gray + "[war] Usage: /setwarzone <warzone-name> <'southeast'/'northwest'>. " +
						"Defines the battleground boundary. The warzone is reset at the start of every battle. " +
						"This command overwrites any previously saved blocks (i.e. make sure you reset with /restartbattle " +
						"or /resetwarzone before changing the boundary). ");
			} else {
				Warzone zone = new Warzone(war.getServer(), split[1]);
				war.addWarzone(zone);
				player.sendMessage(Colors.Gray + "[war] Warzone added.");
			}
			return true;
		}
		

		// /setwarzonestate
		else if(command.equals("/setwarzonestart")) {
			if(!war.inAnyWarzone(player.getLocation())) {
				player.sendMessage(Colors.Gray + "[war] Usage: /setwarzonestart. Must be in warzone. Changes the warzone state at the beginning of every battle. " +
						"Also sets the teleport point for this warzone (i.e. make sure to use /warzone or the warzone tp point will change). " +
						"Just like /setwarzone, this command overwrites any previously saved blocks (i.e. make sure you reset with /restartbattle " +
						"or /resetwarzone before changing start state). ");
			} else {
				Warzone warzone = war.warzone(player.getLocation());
				warzone.resetState();
				warzone.setTeleport(player.getLocation());
				player.sendMessage(Colors.Gray + "[war] Warzone initial state and teleport location changed.");
			}
			return true;
		}
		
		// /resetwarzone
		else if(command.equals("/resetwarzone")) {
			if(!war.inAnyWarzone(player.getLocation())) {
				player.sendMessage(Colors.Gray + "[war] Usage: /resetwarzone. Must be in warzone.");
			} else {
				Warzone warzone = war.warzone(player.getLocation());
				for(Team team: warzone.getTeams()) {
					team.teamcast(Colors.Gray + "[war] Resetting warzone.");
				}
				warzone.resetState();
				player.sendMessage(Colors.Gray + "[war] Warzone reset.");
			}
			return true;
		}
		
        return false;
    }

}
