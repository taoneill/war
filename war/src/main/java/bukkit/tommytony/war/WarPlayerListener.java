package bukkit.tommytony.war;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerMoveEvent;

import com.tommytony.war.Team;
import com.tommytony.war.TeamMaterials;
import com.tommytony.war.WarHub;
import com.tommytony.war.Warzone;
import com.tommytony.war.ZoneLobby;


/**
 * 
 * @author tommytony
 *
 */
public class WarPlayerListener extends PlayerListener {

	private final War war;
	private Random random = null;

	public WarPlayerListener(War war) {
		this.war = war;
		random = new Random();
	}
	
	public void onPlayerJoin(PlayerEvent event) {
		event.getPlayer().sendMessage(war.str("War is on! Pick your battle (try /warhub, /zones and /zone)."));
    }
	
	public void onPlayerQuit(PlayerEvent event) {
		Player player = event.getPlayer();
		Team team = war.getPlayerTeam(player.getName());
		if(team != null) {
			Warzone zone = war.getPlayerTeamWarzone(player.getName());
			if(zone != null && zone.hasPlayerInventory(player.getName())) {
				player.teleportTo(zone.getTeleport());
				zone.restorePlayerInventory(player);
				if(zone.getLobby() != null) {
					zone.getLobby().resetTeamGateSign(team);
				}
			}
			team.removePlayer(player.getName());
			team.resetSign();
			if(war.getWarHub() != null) {
				war.getWarHub().resetZoneSign(zone);
			}
			player.sendMessage(war.str("You have left the warzone. Your inventory has (hopefully) been restored."));  
		}
	}

	
	public void onPlayerMove(PlayerMoveEvent event) {
		Player player = event.getPlayer();
		Location playerLoc = event.getFrom(); // same as player.getLoc. Don't call again we need same result.
		//Location from = event.getFrom();
		Warzone locZone = null;
		ZoneLobby locLobby = null;
		//if(from != null) {
			locZone = war.warzone(playerLoc);
			locLobby = war.lobby(playerLoc);
		//}
//		synchronized(player) {
		//Location to = event.getTo();
		boolean canPlay = war.canPlayWar(player);
		boolean isMaker = war.isZoneMaker(player);
		
		// Zone walls
		if(!isMaker) { // zone makers don't get bothered with guard walls
			Warzone nearbyZone = war.zoneOfZoneWallAtProximity(playerLoc);
			if(nearbyZone != null) {	
				nearbyZone.protectZoneWallAgainstPlayer(player);
			} else {
				// make sure to delete any wall guards as you leave
				for(Warzone zone : war.getWarzones()) {
					zone.dropZoneWallGuardIfAny(player);
				}
			}
		}
		
		Warzone playerWarzone = war.getPlayerTeamWarzone(player.getName());	
			// this uses the teams, so it asks: get the player's team's warzone, to be clearer
		
		boolean enteredGate = false;
		// Warzone lobby gates
		if(locLobby != null) {
			Warzone zone = locLobby.getZone(); 
			Team oldTeam = war.getPlayerTeam(player.getName());
			boolean isAutoAssignGate = false;
			boolean isDiamondGate = false;
			boolean isIronGate = false;
			boolean isGoldGate = false;
			if(oldTeam == null && canPlay) { // trying to counter spammy player move
				isAutoAssignGate = zone.getLobby().isAutoAssignGate(playerLoc);
				if(isAutoAssignGate) {
					enteredGate = true;
					dropFromOldTeamIfAny(player);
					int noOfPlayers = 0;
					for(Team t : zone.getTeams()) {
						noOfPlayers += t.getPlayers().size();
					}
					if(noOfPlayers < zone.getTeams().size() * zone.getTeamCap()) {
						zone.autoAssign(event, player);
						//zone.autoAssign(player);
						//event.setCancelled(true);
					} else {
						event.setFrom(zone.getTeleport());
						player.teleportTo(zone.getTeleport());
						event.setCancelled(true);
						player.sendMessage("All teams are full.");
					}
					return;
				} 
				
				isDiamondGate = zone.getLobby().isInTeamGate(TeamMaterials.TEAMDIAMOND, playerLoc);	
				if (isDiamondGate){
					enteredGate = true;
					dropFromOldTeamIfAny(player);
					Team diamondTeam = zone.getTeamByMaterial(TeamMaterials.TEAMDIAMOND);
					if(diamondTeam.getPlayers().size() < zone.getTeamCap()) {
						diamondTeam.addPlayer(player);
						diamondTeam.resetSign();
						zone.keepPlayerInventory(player);
						player.sendMessage(war.str("Your inventory is is storage until you /leave."));
						zone.respawnPlayer(event, diamondTeam, player);
						//zone.respawnPlayer(diamondTeam, player);
						//event.setCancelled(true);
						for(Team team : zone.getTeams()){
							team.teamcast(war.str("" + player.getName() + " joined team diamond."));
						}
					} else {
						event.setFrom(zone.getTeleport());
						player.teleportTo(zone.getTeleport());
						event.setCancelled(true);
						player.sendMessage("Team diamond is full.");
					}
					return;
				}
				
				isIronGate = zone.getLobby().isInTeamGate(TeamMaterials.TEAMIRON, playerLoc);	
				if (isIronGate){
					enteredGate = true;
					dropFromOldTeamIfAny(player);
					Team ironTeam = zone.getTeamByMaterial(TeamMaterials.TEAMIRON);
					if(ironTeam.getPlayers().size() < zone.getTeamCap()) {
						ironTeam.addPlayer(player);
						ironTeam.resetSign();
						zone.keepPlayerInventory(player);
						player.sendMessage(war.str("Your inventory is is storage until you /leave."));
						zone.respawnPlayer(event, ironTeam, player);
//										zone.respawnPlayer(ironTeam, player);
//										event.setCancelled(true);
						for(Team team : zone.getTeams()){
							team.teamcast(war.str("" + player.getName() + " joined team iron."));
						}
					} else {
						event.setFrom(zone.getTeleport());
						player.teleportTo(zone.getTeleport());
						event.setCancelled(true);
						player.sendMessage("Team iron is full.");
					}
					return;
				}
				
				isGoldGate = zone.getLobby().isInTeamGate(TeamMaterials.TEAMGOLD, playerLoc);	
				if (isGoldGate){
					enteredGate = true;
					dropFromOldTeamIfAny(player);
					Team goldTeam = zone.getTeamByMaterial(TeamMaterials.TEAMGOLD);
					if(goldTeam.getPlayers().size() < zone.getTeamCap()) {
						goldTeam.addPlayer(player);
						goldTeam.resetSign();
						zone.keepPlayerInventory(player);
						player.sendMessage(war.str("Your inventory is is storage until you /leave."));
						zone.respawnPlayer(event, goldTeam, player);
						//zone.respawnPlayer(goldTeam, player);
						//event.setCancelled(true);
						for(Team team : zone.getTeams()){
							team.teamcast(war.str("" + player.getName() + " joined team gold."));
						}
					} else {
						event.setFrom(zone.getTeleport());
						player.teleportTo(zone.getTeleport());
						event.setCancelled(true);
						player.sendMessage("Team gold is full.");
					}
					return;
				} 
					
				if (zone.getLobby().isInWarHubLinkGate(playerLoc)){
					enteredGate = true;
					dropFromOldTeamIfAny(player);
					event.setFrom(war.getWarHub().getLocation());
					player.teleportTo(war.getWarHub().getLocation());
					event.setCancelled(true);
					player.sendMessage(war.str("Welcome to the War hub."));
					return;
				}
			} else if ((isAutoAssignGate || isDiamondGate || isGoldGate || isIronGate)  &&
					    !canPlay) {
				event.setFrom(zone.getTeleport());
				player.teleportTo(zone.getTeleport());
				event.setCancelled(true);
				player.sendMessage(war.str("You don't have permission to play War. Ask a mod, please."));
				return;
			}
			
		}
		
		// Warhub zone gates
		WarHub hub = war.getWarHub();
		if(hub != null && hub.getVolume().contains(playerLoc)) {
			Warzone zone = hub.getDestinationWarzoneForLocation(playerLoc);
			//synchronized(player) {
				if(zone != null) {
					enteredGate = true;
					event.setFrom(zone.getTeleport());
							player.teleportTo(zone.getTeleport());
//							
					event.setCancelled(true);
					player.sendMessage(war.str("Welcome to warzone " + zone.getName() + "."));
					return;
				}
			//}
		}
		
		if(locZone != null && locZone.getLobby() != null
				&& locZone.getLobby().isLeavingZone(playerLoc)) { // already in a team and in warzone, leaving
			enteredGate = true;
			// same as leave, except using event.setFrom and cancelling even .. don't ask me, see NetServerHandler code 
			Team playerTeam = war.getPlayerTeam(player.getName());
			if(playerTeam != null) {
				playerTeam.removePlayer(player.getName());
				playerTeam.resetSign();
				event.setFrom(playerWarzone.getTeleport());
				player.teleportTo(playerWarzone.getTeleport());
				event.setCancelled(true);
				playerWarzone.restorePlayerInventory(player);
				if(playerWarzone.getLobby() != null) {
					playerWarzone.getLobby().resetTeamGateSign(playerTeam);
				}
				player.sendMessage(war.str("Left the zone. Your inventory has (hopefully) been restored."));
				if(war.getWarHub() != null) {
					war.getWarHub().resetZoneSign(locZone);
				}
				return;
			}
		}
	
		if(playerWarzone != null && !enteredGate) {
			Team team = war.getPlayerTeam(player.getName());
			
			// Player belongs to a warzone team but is outside: he snuck out!.
			if(locZone == null && team != null) {
				//player.sendMessage(war.str("Teleporting you back to spawn. Please /leave your team if you want to exit the zone."));
				event.setFrom(team.getTeamSpawn());
				//playerWarzone.respawnPlayer(team, player);
				player.teleportTo(team.getTeamSpawn());
				event.setCancelled(true);
			}

			// Monuments
			if(team != null
					&& playerWarzone.nearAnyOwnedMonument(playerLoc, team) 
					&& player.getHealth() < 20
					&& random.nextInt(42) == 3 ) {	// one chance out of many of getting healed
				player.setHealth(20);
				player.sendMessage(war.str("Your dance pleases the monument's voodoo. You gain full health!"));
			}
		} else if (locZone != null && locZone.getLobby() != null 
				&&  locZone.getLobby().isLeavingZone(playerLoc) && !isMaker) { 
			// player is not in any team, but inside warzone boundaries, get him out
			Warzone zone = war.warzone(playerLoc);
			event.setFrom(zone.getTeleport());
			player.teleportTo(zone.getTeleport());
			event.setCancelled(true);
			//player.sendMessage(war.str("You can't be inside a warzone without a team."));
		}
	
    }
	
	private void dropFromOldTeamIfAny(Player player) {
		// drop from old team if any
		Team previousTeam = war.getPlayerTeam(player.getName());
		if(previousTeam != null) {
			if(!previousTeam.removePlayer(player.getName())){
				war.warn("Could not remove player " + player.getName() + " from team " + previousTeam.getName());
			}
		}
	}

	public String getAllTeamsMsg(Player player){
		String teamsMessage = "Teams: ";
		Warzone warzone = war.warzone(player.getLocation());
		ZoneLobby lobby = war.lobby(player.getLocation());
		if(warzone == null && lobby != null) {
			warzone = lobby.getZone();
		} else {
			lobby = warzone.getLobby();
		}
		if(warzone.getTeams().isEmpty()){
			teamsMessage += "none.";
		}
		for(Team team :warzone.getTeams()) {
			teamsMessage += team.getName() + " (" + team.getPoints() + " points, "+ team.getRemainingTickets() + "/" + warzone.getLifePool() + " lives left. ";
			for(Player member : team.getPlayers()) {
				teamsMessage += member.getName() + " ";
			}
			teamsMessage += ")  ";
		}
		return teamsMessage;
	}

	
	
}
