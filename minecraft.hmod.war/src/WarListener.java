import java.util.List;

public class WarListener extends PluginListener {

	private final War war;

	public WarListener(War war) {
		this.war = war;
	}
	
	public void onLogin(Player player) {    
		player.sendMessage("<war> War is on! You must pick sides (try /teams and /join).");
    }
	
	public boolean onCommand(Player player, java.lang.String[] split) {
		String command = split[0];
		// /teams
		if(command.equals("/teams")){
			String teamsMessage = "<war> Teams: ";
			for(Team team : war.getTeams()) {
				teamsMessage += team.getName() + " (";
				for(Player member : team.getPlayers()) {
					teamsMessage += member.getName() + " ";
				}
				teamsMessage += ")  ";
			}
			player.sendMessage(teamsMessage);
			return true;
		}
		
		// /newteam <teamname>
		else if(command.equals("/newteam")) {
			if(split.length < 2) {
				player.sendMessage("<war> Usage: /newteam <teamname>");
			} else {
				String name = split[1];
				war.getTeams().add(new Team(name, player.getLocation()));
			}
			return true;
		}
		
		// /join <teamname>
		else if(command.equals("/join")) {
			if(split.length < 2) {
				player.sendMessage("<war> Usage: /join <teamname>");
			} else {
				String name = split[1];
				List<Team> teams = war.getTeams();
				boolean foundTeam = false;
				for(Team team : teams) {
					if(team.getName().equals(name)) {
						team.addPlayer(player);
						foundTeam = true;
					}
				}
				if(foundTeam) {
					player.sendMessage("<war> Joined " + name);
					etc.getServer().messageAll("<war> " + player.getName() + " joined " + name);
				} else {
					player.sendMessage("<war> No such team. Try /teams.");
				}
			}
			return true;
		}
		
		// /team <msg>
		else if(command.equals("/team")) {
			if(split.length < 2) {
				player.sendMessage("<war> Usage: /team <team-only message>");
			} else {
				Team playerTeam = war.getPlayerTeam(player.getName());
				String teamMessage = "<" + playerTeam.getName() + " - " + player.getName() + ":> ";
				for(int j = 1 ; j<split.length; j++) {
					String part = split[j];
					teamMessage += part + " ";
				}
				playerTeam.teamcast(teamMessage);
			}
			return true;
		}
		
        return false;
    }

}
