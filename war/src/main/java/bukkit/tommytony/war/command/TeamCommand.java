package bukkit.tommytony.war.command;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import bukkit.tommytony.war.WarCommandHandler;

import com.tommytony.war.Team;

/**
 * Sends a message to all team-members
 * @author das-schaf
 *
 */
public class TeamCommand extends AbstractWarCommand {
	public TeamCommand(WarCommandHandler handler, CommandSender sender, String[] args) {
		super(handler, sender, args);
	}

	@Override
	public boolean handle() {
		if (!(this.sender instanceof Player)) return true;

		Player player = (Player) this.sender;
		Team playerTeam = Team.getTeamByPlayerName(player.getName());
		if (playerTeam == null) {
			return false;
		}

		ChatColor color = playerTeam.getKind().getColor();
		String teamMessage = color + player.getName() + ": " + ChatColor.WHITE;
		for (String part : this.args) {
			teamMessage += part + " ";
		}
		playerTeam.teamcast(teamMessage);

		return true;
	}
}
