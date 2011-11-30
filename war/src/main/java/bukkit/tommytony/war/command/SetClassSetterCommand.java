package bukkit.tommytony.war.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import com.tommytony.war.ClassSetter;
import com.tommytony.war.Monument;
import com.tommytony.war.Warzone;
import com.tommytony.war.mappers.WarzoneMapper;
import bukkit.tommytony.war.WarCommandHandler;

/**
 * places a ClassSetter
 * 
 * @author grinning
 *
 */
public class SetClassSetterCommand extends AbstractZoneMakerCommand {
	public SetClassSetterCommand(WarCommandHandler handler, CommandSender sender, String[] args) throws NotZoneMakerException {
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
		} else if (!this.isSenderAuthorOfZone(zone)) {
			return true;
		}
		
		if (this.args[0].equals(zone.getName())) {
			return false;
		}

		Warzone warzone = Warzone.getZoneByLocation(player);
		if (warzone.hasClassSetter(this.args[0])) {
			// move the existing ClassSetter
			ClassSetter classsetter = warzone.getClassSetters(this.args[0]);
	        classsetter.getVolume().resetBlocks();
			classsetter.setLocation(player.getLocation());
			this.msg("Class Changer " + classsetter.getName() + " was moved.");
		} else {
			// create a new ClassSetter
			ClassSetter classsetter = new ClassSetter(this.args[0], warzone, player.getLocation());
			warzone.getClasSetters().add(classsetter);
			this.msg("Class Changer " + classsetter.getName() + " created.");
		}
		WarzoneMapper.save(warzone, false);

		WarzoneMapper.save(zone, false);

		return true;
	}
}


