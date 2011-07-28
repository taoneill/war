package bukkit.tommytony.war.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.tommytony.war.mappers.WarMapper;

import bukkit.tommytony.war.NoZoneMakerException;
import bukkit.tommytony.war.War;
import bukkit.tommytony.war.WarCommandHandler;

public class SetWarConfigCommand extends AbstractZoneMakerCommand {

	public SetWarConfigCommand(WarCommandHandler handler, CommandSender sender, String[] args) throws NoZoneMakerException {
		super(handler, sender, args);
	}

	@Override
	public boolean handle() {
		if (this.args.length == 0) {
			return false;
		}
		if (!(this.sender instanceof Player))
		 {
			return true;
			// TODO: Maybe move rallypoint to warzone setting
			// TODO: The rallypoint is the only thing that prevents this from being used from cli
		}

		if (War.war.updateFromNamedParams((Player) this.sender, this.args)) {
			WarMapper.save();
			this.msg("War config saved.");
		} else {
			this.msg("Failed to read named parameters.");
		}

		return true;
	}

}
