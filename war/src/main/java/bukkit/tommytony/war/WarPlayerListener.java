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
			Warzone zone = war.getPlayerWarzone(player.getName());
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
		Location from = event.getFrom();
		Location to = event.getTo();
		
		// Zone walls
		if(to != null && !war.isZoneMaker(player.getName())) { // zone makers don't get bothered with guard walls
			Warzone nearbyZone = war.zoneOfZoneWallAtProximity(to);
			if(nearbyZone != null) {	
				nearbyZone.protectZoneWallAgainstPlayer(player);
			} else {
				// make sure to delete any wall guards as you leave
				for(Warzone zone : war.getWarzones()) {
					zone.dropZoneWallGuardIfAny(player);
				}
			}
		}
		
		Warzone playerWarzone = war.getPlayerWarzone(player.getName());	// this uses the teams, so it asks: get the player's team's warzone, to be clearer
		if(playerWarzone != null) {
			Team team = war.getPlayerTeam(player.getName());
			
			// Player belongs to a warzone team but is outside: he just died! Handle death! Don't exempt the zone maker.
			if(from != null && war.warzone(player.getLocation()) == null && team != null) {
				// teleport to team spawn upon death
				player.sendMessage(war.str("You died."));
				boolean newBattle = false;
				boolean scoreCapReached = false;
				synchronized(playerWarzone) {
					synchronized(player) {
						int remaining = team.getRemainingTickets();
						if(remaining == 0) { // your death caused your team to lose
							List<Team> teams = playerWarzone.getTeams();
							for(Team t : teams) {
								t.teamcast(war.str("The battle is over. Team " + team.getName() + " lost: " 
										+ player.getName() + " died and there were no lives left in their life pool." ));
								
								if(!t.getName().equals(team.getName())) {
									// all other teams get a point
									t.addPoint();
									t.resetSign();
								}
							}
							// detect score cap
							List<Team> scoreCapTeams = new ArrayList<Team>();
							for(Team t : teams) {
								if(t.getPoints() == playerWarzone.getScoreCap()) {
									scoreCapTeams.add(t);
								}
							}
							if(!scoreCapTeams.isEmpty()) {
								String winnersStr = "Score cap reached! Winning team(s): ";
								for(Team winner : scoreCapTeams) {
									winnersStr += winner.getName() + " ";
								}
								winnersStr += ". The warzone is being reset... Please choose a new team.";
								// Score cap reached. Reset everything.
								for(Team t : teams) {
									t.teamcast(war.str(winnersStr));
									for(Player tp : t.getPlayers()) {
										if(tp.getName() != player.getName()) {
											tp.teleportTo(playerWarzone.getTeleport());
										}
									}
									t.setPoints(0);
									t.getPlayers().clear();	// empty the team
								}
								playerWarzone.getVolume().resetBlocks();
								playerWarzone.initializeZone();
								scoreCapReached = true;
							} else {
								// We can keep going
								for(Team t : teams) {
									t.teamcast(war.str("A new battle begins. The warzone is being reset..."));
								}
								playerWarzone.getVolume().resetBlocks();
								playerWarzone.initializeZone();
								newBattle = true;
								team.setRemainingTickets(team.getRemainingTickets()+1); // TODO get rid of this dirty workaround for the twice move-on-death bug
							}
						} else if(!team.getVolume().contains(player.getLocation())){
							// only decrement points if the player is not already back at spawn
							team.setRemainingTickets(remaining - 1);
						}
					}
				}
				synchronized(player) {
					if(!newBattle && !scoreCapReached && !war.inAnyWarzone(player.getLocation())) {	// only respawn him if he isnt back at zone yet
						playerWarzone.respawnPlayer(event, team, player);
						team.resetSign();
						war.info(player.getName() + " died and was tp'd back to team " + team.getName() + "'s spawn");
					} else if (scoreCapReached) { 
						event.setTo(playerWarzone.getTeleport());
						team.resetSign();
						war.info(player.getName() + " died and enemy team reached score cap");
					} else if (newBattle){
						war.info(player.getName() + " died and battle ended in team " + team.getName() + "'s disfavor");
					}
				}
			}
			
			// Monuments
			if(to != null && team != null
					&& playerWarzone.nearAnyOwnedMonument(to, team) 
					&& player.getHealth() < 20
					&& random.nextInt(42) == 3 ) {	// one chance out of many of getting healed
				player.setHealth(20);
				player.sendMessage(war.str("Your dance pleases the monument's voodoo. You gain full health!"));
			}
		} else if (war.inAnyWarzone(to) && !war.isZoneMaker(player.getName())) { // player is not in any team, but inside warzone boundaries, get him out
			Warzone zone = war.warzone(to);
			event.setTo(zone.getTeleport());
			player.sendMessage(war.str("You can't be inside a warzone without a team."));
		}
		
		if(to != null) {
			// Warzone lobby gates
			for(Warzone zone : war.getWarzones()){
				if(zone.getLobby() != null) {
					synchronized(player) {
						Team oldTeam = war.getPlayerTeam(player.getName());
						if(oldTeam == null) { // trying to counter spammy player move
							if(zone.getLobby().isAutoAssignGate(to)) {
								dropFromOldTeamIfAny(player);
								int noOfPlayers = 0;
								for(Team t : zone.getTeams()) {
									noOfPlayers += t.getPlayers().size();
								}
								if(noOfPlayers < zone.getTeams().size() * zone.getTeamCap()) {
									zone.autoAssign(event, player);
								} else {
									event.setTo(zone.getTeleport());
									player.sendMessage("All teams are full.");
								}
							} else if (zone.getLobby().isInTeamGate(TeamMaterials.TEAMDIAMOND, to)){
								dropFromOldTeamIfAny(player);
								Team diamondTeam = zone.getTeamByMaterial(TeamMaterials.TEAMDIAMOND);
								if(diamondTeam.getPlayers().size() < zone.getTeamCap()) {
									diamondTeam.addPlayer(player);
									diamondTeam.resetSign();
									zone.keepPlayerInventory(player);
									player.sendMessage(war.str("Your inventory is is storage until you /leave."));
									zone.respawnPlayer(event, diamondTeam, player);
									for(Team team : zone.getTeams()){
										team.teamcast(war.str("" + player.getName() + " joined team diamond."));
									}
								} else {
									event.setTo(zone.getTeleport());
									player.sendMessage("Team diamond is full.");
								}
							} else if (zone.getLobby().isInTeamGate(TeamMaterials.TEAMIRON, to)){
								dropFromOldTeamIfAny(player);
								Team ironTeam = zone.getTeamByMaterial(TeamMaterials.TEAMIRON);
								if(ironTeam.getPlayers().size() < zone.getTeamCap()) {
									ironTeam.addPlayer(player);
									ironTeam.resetSign();
									zone.keepPlayerInventory(player);
									player.sendMessage(war.str("Your inventory is is storage until you /leave."));
									zone.respawnPlayer(event, ironTeam, player);
									for(Team team : zone.getTeams()){
										team.teamcast(war.str("" + player.getName() + " joined team iron."));
									}
								} else {
									event.setTo(zone.getTeleport());
									player.sendMessage("Team iron is full.");
								}
							} else if (zone.getLobby().isInTeamGate(TeamMaterials.TEAMGOLD, to)){
								dropFromOldTeamIfAny(player);
								Team goldTeam = zone.getTeamByMaterial(TeamMaterials.TEAMGOLD);
								if(goldTeam.getPlayers().size() < zone.getTeamCap()) {
									goldTeam.addPlayer(player);
									goldTeam.resetSign();
									zone.keepPlayerInventory(player);
									player.sendMessage(war.str("Your inventory is is storage until you /leave."));
									zone.respawnPlayer(event, goldTeam, player);
									for(Team team : zone.getTeams()){
										team.teamcast(war.str("" + player.getName() + " joined team gold."));
									}
								} else {
									event.setTo(zone.getTeleport());
									player.sendMessage("Team gold is full.");
								}
							} else if (zone.getLobby().isInWarHubLinkGate(to)){
								dropFromOldTeamIfAny(player);
								event.setTo(war.getWarHub().getLocation());
								player.teleportTo(war.getWarHub().getLocation());
								player.sendMessage(war.str("Welcome to the War hub."));
							}
						} else if(zone.getLobby().isLeavingZone(to)) { // already in a team and in warzone, leaving
								// same as leave, except event.setTo
								Team playerTeam = war.getPlayerTeam(player.getName());
								playerTeam.removePlayer(player.getName());
								playerTeam.resetSign();
								event.setTo(playerWarzone.getTeleport());
								playerWarzone.restorePlayerInventory(player);
								if(playerWarzone.getLobby() != null) {
									playerWarzone.getLobby().resetTeamGateSign(playerTeam);
								}
								player.sendMessage(war.str("Left the zone. Your inventory has (hopefully) been restored."));
								if(war.getWarHub() != null) {
									war.getWarHub().resetZoneSign(zone);
								}
						}
					}
				}
			}
			
			// Warhub zone gates
			WarHub hub = war.getWarHub();
			if(hub != null) {
				Warzone zone = hub.getDestinationWarzoneForLocation(player.getLocation());
				synchronized(player) {
					if(zone != null) {
						event.setTo(zone.getTeleport());
						//player.teleportTo(zone.getTeleport());
						player.sendMessage(war.str("Welcome to warzone " + zone.getName() + "."));
					}
				}
			}
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
