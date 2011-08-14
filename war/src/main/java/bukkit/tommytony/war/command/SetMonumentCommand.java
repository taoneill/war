package bukkit.tommytony.war.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.tommytony.war.Monument;
import com.tommytony.war.Warzone;
import com.tommytony.war.mappers.WarzoneMapper;

import bukkit.tommytony.war.WarCommandHandler;

/**
 * Places a monument
 *
 * @author Tim Düsterhus
 */
public class SetMonumentCommand extends AbstractZoneMakerCommand {
	public SetMonumentCommand(WarCommandHandler handler, CommandSender sender, String[] args) throws NotZoneMakerException {
		super(handler, sender, args);
	}

	@Override
	public boolean handle() {
		if (!(this.getSender() instanceof Player)) {
			this.badMsg("You can't do this if you are not in-game.");
			return true;
		}

		Player player = (Player) this.getSender();

		if (this.args.length != 1) {
			return false;
		}

		Warzone zone = Warzone.getZoneByLocation(player);

		if (zone == null) {
			return false;
		}
		if (this.args[0].equals(zone.getName())) {
			return false;
		}

		Warzone warzone = Warzone.getZoneByLocation(player);
		if (warzone.hasMonument(this.args[0])) {
			// move the existing monument
			Monument monument = warzone.getMonument(this.args[0]);
			monument.getVolume().resetBlocks();
			monument.setLocation(player.getLocation());
			this.msg("Monument " + monument.getName() + " was moved.");
		} else {
			// create a new monument
			Monument monument = new Monument(this.args[0], warzone, player.getLocation());
			warzone.getMonuments().add(monument);
			this.msg("Monument " + monument.getName() + " created.");
		}
		WarzoneMapper.save(warzone, false);

		WarzoneMapper.save(zone, false);

		return true;
	}
}
