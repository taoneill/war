package com.tommytony.war.command;

import com.tommytony.war.War;
import com.tommytony.war.mapper.WarYmlMapper;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.logging.Level;

/**
 * Makes a player zonemaker and other way round.
 *
 * @author Tim Düsterhus
 */
public class ZoneMakerCommand extends AbstractWarCommand {

	public ZoneMakerCommand(WarCommandHandler handler, CommandSender sender, String[] args) throws NotZoneMakerException {
		super(handler, sender, args);

		if (sender instanceof Player) { // i hate java for this.
			if (!War.war.isZoneMaker((Player) sender)) {
				for (OfflinePlayer offlinePlayer : War.war.getZoneMakersImpersonatingPlayers()) {
					if (offlinePlayer.isOnline() && sender.equals(offlinePlayer.getPlayer())) {
						return;
					}
				}
				throw new NotZoneMakerException();
			}
		}
	}

	@Override
	public boolean handle() {
		if (!(this.getSender() instanceof Player)) {
			this.badMsg("You can't do this if you are not in-game.");
			return true;
		}
		Player player = (Player) this.getSender();

		if (War.war.isZoneMaker(player)) {
			if (this.args.length == 0) {
				War.war.getZoneMakersImpersonatingPlayers().add(player);
				this.msg("You are now impersonating a regular player. Type /zonemaker again to toggle back to war maker mode.");
			} else if (this.args.length == 1) {
				OfflinePlayer other = Bukkit.getOfflinePlayer(this.args[0]);
				// make someone zonemaker or remove the right
				if (War.war.getZoneMakerNames().contains(other)) {
					// kick
					War.war.getZoneMakerNames().remove(other);
					this.msg(this.args[0] + " is not a zone maker anymore.");
					if (other.isOnline()) {
						War.war.msg(other.getPlayer(), player.getName() + " took away your warzone maker priviledges.");
						War.war.log(player.getName() + " took away zonemaker rights from " + other.getName(), Level.INFO);
					}
				} else {
					// add
					War.war.getZoneMakerNames().add(other);
					this.msg(this.args[0] + " is now a zone maker.");
					if (other.isOnline()) {
						War.war.msg(other.getPlayer(), player.getName() + " made you warzone maker.");
						War.war.log(player.getName() + " made " + other.getName() + " a zonemaker", Level.INFO);
					}
				}
				WarYmlMapper.save();
			} else {
				return false;
			}
		} else {
			if (this.args.length != 0) {
				return false;
			}

			War.war.getZoneMakersImpersonatingPlayers().remove(player);
			this.msg("You are back as a zone maker.");
			WarYmlMapper.save();
		}

		return true;
	}
}
