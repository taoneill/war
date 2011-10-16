package bukkit.tommytony.war.command;

import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import com.tommytony.war.Warzone;

import bukkit.tommytony.war.War;
import bukkit.tommytony.war.WarCommandHandler;

/**
 * Represents a command that may only be used by zone makers
 *
 * @author Tim Düsterhus
 */
public abstract class AbstractZoneMakerCommand extends AbstractWarCommand {

	public AbstractZoneMakerCommand(WarCommandHandler handler, CommandSender sender, String[] args) throws NotZoneMakerException {
		super(handler, sender, args);

		if (sender instanceof Player) {
			// for players check War.isZoneMaker()
			if (!War.war.isZoneMaker((Player) sender)) {
				throw new NotZoneMakerException();
			}
		} else if (!(sender instanceof ConsoleCommandSender)) {
			throw new NotZoneMakerException();
		}
	}
	
	public boolean isSenderAuthorOfZone(Warzone zone) {
		if (this.getSender() instanceof Player) {
			if (War.war.isWarAdmin((Player) this.getSender())) {
				// War admin has rights over all warzones
				return true;
			}
			
			// Not War admin, is he author?
			boolean isAuthor = zone.isAuthor((Player) this.getSender());
			if (!isAuthor) {
				War.war.badMsg(this.getSender(), "You can't do this because you are not an author of the " + zone.getName() + " warzone." );
			}
			return isAuthor;
		} else {
			// From console, you can do anything
			return true;
		}
	}
}
