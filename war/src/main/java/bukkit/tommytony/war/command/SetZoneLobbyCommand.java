package bukkit.tommytony.war.command;

import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.tommytony.war.Warzone;
import com.tommytony.war.ZoneLobby;
import com.tommytony.war.mappers.WarzoneMapper;

import bukkit.tommytony.war.War;
import bukkit.tommytony.war.WarCommandHandler;

/**
 * Places the zonelobby
 *
 * @author Tim Düsterhus
 */
public class SetZoneLobbyCommand extends AbstractZoneMakerCommand {

	public SetZoneLobbyCommand(WarCommandHandler handler, CommandSender sender, String[] args) throws NotZoneMakerException {
		super(handler, sender, args);
	}

	@Override
	public boolean handle() {
		if (!(this.getSender() instanceof Player)) {
			this.badMsg("You can't do this if you are not in-game.");
			return true;
		}
		if (this.args.length != 1) {
			return false;
		}
		Player player = (Player) this.getSender();
		Warzone zone = Warzone.getZoneByLocation((Player) this.getSender());
		if (zone == null) {
			ZoneLobby lobby = ZoneLobby.getLobbyByLocation((Player) this.getSender());
			if (lobby != null) {
				zone = lobby.getZone();
			}
		}

		if (zone == null) {
			Warzone givenWarzone = Warzone.getZoneByName(this.args[0]);
			if (givenWarzone == null) {
				return false;
			} else {
				// Move the warzone lobby
				ZoneLobby lobby = givenWarzone.getLobby();
				if (lobby != null) {
					// reset existing lobby
					lobby.getVolume().resetBlocks();
					lobby.setLocation(player.getLocation());
					lobby.initialize();
					this.msg("Warzone lobby moved to your location.");
				} else {
					// new lobby
					lobby = new ZoneLobby(givenWarzone, player.getLocation());
					givenWarzone.setLobby(lobby);
					lobby.initialize();
					if (War.war.getWarHub() != null) { // warhub has to change
						War.war.getWarHub().getVolume().resetBlocks();
						War.war.getWarHub().initialize();
					}
					this.msg("Warzone lobby moved to your location.");
				}
				WarzoneMapper.save(givenWarzone, false);
			}
		} else {
			// Inside a warzone: use the classic n/s/e/w mode
			if (!this.args[0].equals("north") && !this.args[0].equals("n") && !this.args[0].equals("east") && !this.args[0].equals("e") && !this.args[0].equals("south") && !this.args[0].equals("s") && !this.args[0].equals("west") && !this.args[0].equals("w")) {
				return false;
			}
			ZoneLobby lobby = zone.getLobby();

			BlockFace wall = BlockFace.WEST;
			String wallStr = "";
			if (this.args[0].equals("north") || this.args[0].equals("n")) {
				wall = BlockFace.NORTH;
				wallStr = "north";
			} else if (this.args[0].equals("east") || this.args[0].equals("e")) {
				wall = BlockFace.EAST;
				wallStr = "east";
			} else if (this.args[0].equals("south") || this.args[0].equals("s")) {
				wall = BlockFace.SOUTH;
				wallStr = "south";
			} else if (this.args[0].equals("west") || this.args[0].equals("w")) {
				wall = BlockFace.WEST;
				wallStr = "west";
			}

			if (lobby != null) {
				// reset existing lobby
				lobby.getVolume().resetBlocks();
				lobby.setWall(wall);
				lobby.initialize();
				this.msg("Warzone lobby moved to " + wallStr + " side of zone.");
			} else {
				// new lobby
				lobby = new ZoneLobby(zone, wall);
				zone.setLobby(lobby);
				lobby.initialize();
				if (War.war.getWarHub() != null) { // warhub has to change
					War.war.getWarHub().getVolume().resetBlocks();
					War.war.getWarHub().initialize();
				}
				this.msg("Warzone lobby created on " + wallStr + "side of zone.");
			}
			WarzoneMapper.save(zone, false);
		}

		return true;
	}

}
