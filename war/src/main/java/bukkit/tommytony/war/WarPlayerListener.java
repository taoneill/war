package bukkit.tommytony.war;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.tommytony.war.InventoryStash;
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
	private HashMap<String, InventoryStash> disconnected = new HashMap<String, InventoryStash>();

	public WarPlayerListener(War war) {
		this.war = war;
		random = new Random();
	}
	
	public void onPlayerJoin(PlayerEvent event) {
		Player player = event.getPlayer();
		// Disconnected
		if(disconnected.containsKey(player.getName())) {
			// restore the disconnected player's inventory
			InventoryStash originalContents = disconnected.remove(player.getName());
			PlayerInventory playerInv = player.getInventory();
			playerInv.clear(playerInv.getSize() + 0);
			playerInv.clear(playerInv.getSize() + 1);
			playerInv.clear(playerInv.getSize() + 2);
			playerInv.clear(playerInv.getSize() + 3);	// helmet/blockHead
			if(originalContents != null) {
				playerInv.clear();
				
				for(ItemStack item : originalContents.getContents()) {
					if(item.getTypeId() != 0) {
						playerInv.addItem(item);
					}
				}
				
				if(originalContents.getHelmet() != null) {
					playerInv.setHelmet(originalContents.getHelmet());
				}
				if(originalContents.getChest() != null) {
					playerInv.setChestplate(originalContents.getChest());
				}
				if(originalContents.getLegs() != null) {
					playerInv.setLeggings(originalContents.getLegs());
				}
				if(originalContents.getFeet() != null) {
					playerInv.setBoots(originalContents.getFeet());
				}
			}
			
			player.sendMessage(war.str("You were disconnected. Here's your inventory from last time."));
		}
    }
	
	public void onPlayerQuit(PlayerEvent event) {
		Player player = event.getPlayer();
		Team team = war.getPlayerTeam(player.getName());
		if(team != null) {
			Warzone zone = war.getPlayerTeamWarzone(player.getName());
			if(zone != null) {
				if(zone.hasPlayerInventory(player.getName())) {
					disconnected.put(player.getName(), zone.getPlayerInventory(player.getName()));
				}
				zone.handlePlayerLeave(player, zone.getTeleport());
			}
		}
	}

	
	public void onPlayerMove(PlayerMoveEvent event) {
		Player player = event.getPlayer();
		Location playerLoc = event.getFrom(); // same as player.getLoc. Don't call again we need same result.
		Warzone locZone = null;
		ZoneLobby locLobby = null;
		locZone = war.warzone(playerLoc);
		locLobby = war.lobby(playerLoc);
		boolean canPlay = war.canPlayWar(player);
		boolean isMaker = war.isZoneMaker(player);
		
		// Zone walls
		Warzone nearbyZone = war.zoneOfZoneWallAtProximity(playerLoc);
		if(nearbyZone != null && !isMaker) { // zone makers don't get bothered with guard walls
			nearbyZone.protectZoneWallAgainstPlayer(player);
		} else { // zone makers still need to delete their walls
			// make sure to delete any wall guards as you leave
			for(Warzone zone : war.getWarzones()) {
				zone.dropZoneWallGuardIfAny(player);
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
						if(war.getWarHub() != null) {
							war.getWarHub().resetZoneSign(zone);
						}
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
						if(war.getWarHub() != null) {
							war.getWarHub().resetZoneSign(zone);
						}
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
						if(war.getWarHub() != null) {
							war.getWarHub().resetZoneSign(zone);
						}
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
						if(war.getWarHub() != null) {
							war.getWarHub().resetZoneSign(zone);
						}
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
				player.sendMessage(war.str("You don't have permission to play War. Ask a mod for the 'war.player' permission, please."));
				return;
			}
			
		}
		
		// Warhub zone gates
		WarHub hub = war.getWarHub();
		if(hub != null && hub.getVolume().contains(playerLoc)) {
			Warzone zone = hub.getDestinationWarzoneForLocation(playerLoc);
				if(zone != null) {
					enteredGate = true;
					event.setFrom(zone.getTeleport());
							player.teleportTo(zone.getTeleport());
					event.setCancelled(true);
					player.sendMessage(war.str("Welcome to warzone " + zone.getName() + "."));
					return;
				}
		}
		
		if(locZone != null && locZone.getLobby() != null
				&& locZone.getLobby().isLeavingZone(playerLoc)) { // already in a team and in warzone, leaving
			enteredGate = true;
			// same as leave
			Team playerTeam = war.getPlayerTeam(player.getName());
			if(playerTeam != null) {
				event.setFrom(playerWarzone.getTeleport());
				playerWarzone.handlePlayerLeave(player, playerWarzone.getTeleport());
				event.setCancelled(true);
				return;
			}
		}
	
		if(playerWarzone != null && !enteredGate) {
			Team team = war.getPlayerTeam(player.getName());
			
			// Player belongs to a warzone team but is outside: he snuck out or is at spawn and died
//			if(locZone == null && team != null) {
//				handleDeath(event, player, playerWarzone, team);
//				event.setFrom(team.getTeamSpawn());
//				//playerWarzone.respawnPlayer(team, player);
//				player.teleportTo(team.getTeamSpawn());
//				event.setCancelled(true);
//				return;
//			}

			// Monuments
			if(team != null
					&& playerWarzone.nearAnyOwnedMonument(playerLoc, team) 
					&& player.getHealth() < 20
					&& player.getHealth() > 0	// don't heal the dead
					&& random.nextInt(77) == 3 ) {	// one chance out of many of getting healed
				int currentHp = player.getHealth();
				int newHp = currentHp + 5;
				if(newHp > 20) newHp = 20;
				player.setHealth(newHp);
				player.sendMessage(war.str("Your dance pleases the monument's voodoo. You gain health!"));
				return;
			}
			
			// Flag capture
			if(playerWarzone.isFlagThief(player.getName()) 
					&& (team.getSpawnVolume().contains(player.getLocation())
							|| (team.getFlagVolume() != null && team.getFlagVolume().contains(player.getLocation())))) {
				if(playerWarzone.isTeamFlagStolen(team)) {
					player.sendMessage(war.str("You can't capture the enemy flag until your team flag is returned."));
				} else {
					synchronized(playerWarzone) {
						// flags can be captured at own spawn or own flag pole
						team.addPoint();
						if(team.getPoints() >= playerWarzone.getScoreCap()) {
							if(playerWarzone.hasPlayerInventory(player.getName())){
								playerWarzone.restorePlayerInventory(player);
							}
							playerWarzone.handleScoreCapReached(player, team.getName());
							event.setFrom(playerWarzone.getTeleport());
							player.teleportTo(playerWarzone.getTeleport());
							event.setCancelled(true);
						} else {
							// added a point
							Team victim = playerWarzone.getVictimTeamForThief(player.getName());
							victim.getFlagVolume().resetBlocks();	// bring back flag to team that lost it
							victim.initializeTeamFlag();
							for(Team t : playerWarzone.getTeams()) {
								t.teamcast(war.str(player.getName() + " captured team " + victim.getName()
										+ "'s flag. Team " + team.getName() + " scores one point." ));
							}
							playerWarzone.respawnPlayer(event, team, player);
							team.resetSign();
							playerWarzone.getLobby().resetTeamGateSign(team);
						}				
						playerWarzone.removeThief(player.getName());
					}
				}
			}
		} else if (locZone != null && locZone.getLobby() != null 
				&&  !locZone.getLobby().isLeavingZone(playerLoc) && !isMaker) { 
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
			teamsMessage += team.getName() + " (" + team.getPoints() + " points, "+ team.getRemainingLifes() + "/" + warzone.getLifePool() + " lives left. ";
			for(Player member : team.getPlayers()) {
				teamsMessage += member.getName() + " ";
			}
			teamsMessage += ")  ";
		}
		return teamsMessage;
	}

	private void handleDeath(PlayerMoveEvent event, Player player, Warzone playerWarzone, Team playerTeam) {
    	// teleport to team spawn upon death
		player.sendMessage(war.str("You died."));
		boolean newBattle = false;
		boolean scoreCapReached = false;
		//synchronized(playerWarzone) {
			//synchronized(player) {
				int remaining = playerTeam.getRemainingLifes();
				if(remaining == 0) { // your death caused your team to lose
					List<Team> teams = playerWarzone.getTeams();
					for(Team t : teams) {
						t.teamcast(war.str("The battle is over. Team " + playerTeam.getName() + " lost: " 
								+ player.getName() + " died and there were no lives left in their life pool." ));
						
						if(!t.getName().equals(playerTeam.getName())) {
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
						String winnersStr = "";
						for(Team winner : scoreCapTeams) {
							winnersStr += winner.getName() + " ";
						}
						if(playerWarzone.hasPlayerInventory(player.getName())){
							playerWarzone.restorePlayerInventory(player);
						}
						
						playerWarzone.handleScoreCapReached(player, winnersStr);
						event.setFrom(playerWarzone.getTeleport());
						player.teleportTo(playerWarzone.getTeleport());
						event.setCancelled(true);
						scoreCapReached = true;
					} else {
						// A new battle starts. Reset the zone but not the teams.
						for(Team t : teams) {
							t.teamcast(war.str("A new battle begins. The warzone is being reset..."));
						}
						playerWarzone.getVolume().resetBlocks();
						playerWarzone.initializeZone(event);
						newBattle = true;
					}
				} else {
					// player died without causing his team's demise
					if(playerWarzone.isFlagThief(player.getName())) {
						// died while carrying flag.. dropped it
						Team victim = playerWarzone.getVictimTeamForThief(player.getName());
						victim.getFlagVolume().resetBlocks();
						victim.initializeTeamFlag();
						playerWarzone.removeThief(player.getName());
						for(Team t : playerWarzone.getTeams()) {
							t.teamcast(war.str(player.getName() + " died and dropped team " + victim.getName() + "'s flag."));
						}
					}
					playerTeam.setRemainingLives(remaining - 1);
				}
			//}
		//}
		//synchronized(player) {
			if(!newBattle && !scoreCapReached) {
				playerTeam.resetSign();
				playerWarzone.respawnPlayer(event, playerTeam, player);
			} 
//			else if (scoreCapReached) {
//				
//				war.info(player.getName() + " died and enemy team reached score cap");
//			} else if (newBattle){
//				war.info(player.getName() + " died and battle ended in team " + playerTeam.getName() + "'s disfavor");
//			}
		//}
	}
}
