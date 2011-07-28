package bukkit.tommytony.war.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.tommytony.war.WarHub;
import com.tommytony.war.Warzone;
import com.tommytony.war.mappers.WarMapper;

import bukkit.tommytony.war.NoZoneMakerException;
import bukkit.tommytony.war.War;
import bukkit.tommytony.war.WarCommandHandler;

public class SetWarHubCommand extends AbstractZoneMakerCommand {
	public SetWarHubCommand(WarCommandHandler handler, CommandSender sender, String[] args) throws NoZoneMakerException {
		super(handler, sender, args);
	}

	@Override
	public boolean handle() {
		if (!(this.sender instanceof Player)) {
			return true;
		}

		if (this.args.length != 0) {
			return false;
		}
		Player player = (Player) this.sender;

		if (War.war.getWarzones().size() > 0) {
			if (War.war.getWarHub() != null) {
				// reset existing hub
				War.war.getWarHub().getVolume().resetBlocks();
				War.war.getWarHub().setLocation(player.getLocation());
				War.war.getWarHub().initialize();
				this.msg("War hub moved.");
			} else {
				War.war.setWarHub(new WarHub(player.getLocation()));
				War.war.getWarHub().initialize();
				for (Warzone zone : War.war.getWarzones()) {
					if (zone.getLobby() != null) {
						zone.getLobby().getVolume().resetBlocks();
						zone.getLobby().initialize();
					}
				}
				this.msg("War hub created.");
			}
			WarMapper.save();
		} else {
			this.msg("No warzones yet.");
		}

		return true;
	}
}
