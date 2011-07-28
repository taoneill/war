package bukkit.tommytony.war.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.tommytony.war.mappers.WarMapper;

import bukkit.tommytony.war.NoZoneMakerException;
import bukkit.tommytony.war.War;
import bukkit.tommytony.war.WarCommandHandler;

public class ZoneMakerCommand extends AbstractWarCommand {

	public ZoneMakerCommand(WarCommandHandler handler, CommandSender sender, String[] args) throws NoZoneMakerException {
		super(handler, sender, args);
		// ffffuuuu java, fffuuu!
		// Just because you want the invoking of super-constructor as the very first statement
		// I had to rewrite the complete NoZoneMakerException thingy :(
		if (sender instanceof Player) {
			if (!War.war.isZoneMaker((Player) sender)) {
				for (String name : War.war.getZoneMakersImpersonatingPlayers()) {
					if (((Player) sender).getName().equals(name)) {
						return;
					}
				}
				throw new NoZoneMakerException();
			}
		}
	}

	@Override
	public boolean handle() {
		if (!(this.sender instanceof Player)) {
			return true;
		}
		Player player = (Player) this.sender;

		if (War.war.isZoneMaker(player)) {
			if (this.args.length == 0) {
				War.war.getZoneMakersImpersonatingPlayers().add(player.getName());
				this.msg("You are now impersonating a regular player. Type /zonemaker again to toggle back to war maker mode.");
			} else if (this.args.length == 1) {
				// make someone zonemaker or remove the right
				if (War.war.getZoneMakerNames().contains(this.args[0])) {
					// kick
					War.war.getZoneMakerNames().remove(this.args[0]);
					this.msg(this.args[0] + " is not a zone maker anymore.");
					Player kickedMaker = War.war.getServer().getPlayer(this.args[0]);
					if (kickedMaker != null) {
						War.war.msg(kickedMaker, player.getName() + " took away your warzone maker priviledges.");
					}
				} else {
					// add
					War.war.getZoneMakerNames().add(this.args[0]);
					this.msg(this.args[0] + " is now a zone maker.");
					Player newMaker = War.war.getServer().getPlayer(this.args[0]);
					if (newMaker != null) {
						War.war.msg(newMaker, player.getName() + " made you warzone maker.");
					}
				}
			} else {
				return false;
			}
		} else {
			if (this.args.length != 0) {
				return false;
			}

			War.war.getZoneMakersImpersonatingPlayers().remove(player.getName());
			this.msg("You are back as a zone maker.");
			WarMapper.save();
		}

		return true;
	}
}
