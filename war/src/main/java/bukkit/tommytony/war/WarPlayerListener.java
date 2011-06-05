package bukkit.tommytony.war;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.entity.CraftItem;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerInventoryEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.tommytony.war.Team;
import com.tommytony.war.WarHub;
import com.tommytony.war.Warzone;
import com.tommytony.war.ZoneLobby;
import com.tommytony.war.ZoneSetter;


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

	public void onPlayerQuit(PlayerQuitEvent event) {
		if(war.isLoaded()) {
			Player player = event.getPlayer();
			Team team = war.getPlayerTeam(player.getName());
			if(team != null) {
				Warzone zone = war.getPlayerTeamWarzone(player.getName());
				if(zone != null) {
					zone.handlePlayerLeave(player, zone.getTeleport(), true);
				}
			}
			if(war.isWandBearer(player)) {
				war.removeWandBearer(player);
			}
		}
	}

	public void onPlayerDropItem(PlayerDropItemEvent event) {
		if(war.isLoaded()) {
			Player player = event.getPlayer();
			Team team = war.getPlayerTeam(player.getName());
			if(team != null) {
				Warzone zone = war.getPlayerTeamWarzone(player.getName());
				
				if(zone.isFlagThief(player.getName())) {
					// a flag thief can't drop his flag
					war.badMsg(player, "Can't drop items while stealing flag. What are you doing?! Run!");
					event.setCancelled(true);
					
				} else {
					Item item = event.getItemDrop();
					if(item != null) {
						ItemStack itemStack =  item.getItemStack();
						if(itemStack != null 
								&& itemStack.getType() == team.getKind().getMaterial() 
								&& itemStack.getData().getData() == team.getKind().getData()) {
							// Can't drop your team's kind block
							war.badMsg(player, "Can't drop " + team.getName() + " block blocks.");
							event.setCancelled(true);
							return;
						}
						
						if(zone.isNearWall(player.getLocation()) && itemStack != null) {
							war.badMsg(player, "Can't drop items near the zone border!");
							event.setCancelled(true);
							return;
						}
					}
				}
			}
			if(war.isWandBearer(player)) {
				Item item = event.getItemDrop();
				if(item.getItemStack().getType() == Material.WOOD_SWORD) {
					String zoneName = war.getWandBearerZone(player);
					war.removeWandBearer(player);
					war.msg(player, "You dropped the zone " + zoneName + " wand.");
				}
			}
		}
	}

	public void onPlayerPickupItem(PlayerPickupItemEvent event) {
		if(war.isLoaded()) {
			Player player = event.getPlayer();
			Team team = war.getPlayerTeam(player.getName());
			if(team != null) {
				Warzone zone = war.getPlayerTeamWarzone(player.getName());
				
				if(zone.isFlagThief(player.getName())) {
					// a flag thief can't pick up anything
					event.setCancelled(true);
				} else {
					Item item = event.getItem();
					if(item != null && item instanceof CraftItem) {
						CraftItem cItem = (CraftItem)item;
						if(cItem != null) {
							ItemStack itemStack = cItem.getItemStack();
							if(itemStack != null && itemStack.getType() == team.getKind().getMaterial()
									&& player.getInventory().contains(new ItemStack(team.getKind().getMaterial(), team.getKind().getData()))) {
								// Can't pick up a second precious block
								//war.badMsg(player, "You already have a " + team.getName() + " block.");
								event.setCancelled(true);
								return;
							}
						}
					}
				}
			}
		}
	}
	
	public void onInventoryOpen(PlayerInventoryEvent event) {
		if(war.isLoaded()) {
			Player player = event.getPlayer();
			Inventory inventory = event.getInventory();
			Team team = war.getPlayerTeam(player.getName());
			if(team != null && inventory instanceof PlayerInventory) {
				// make sure the player doesn't have too many precious blocks
				// or illegal armor (i.e. armor not found in loadout)
				PlayerInventory playerInv = (PlayerInventory) inventory;
				ItemStack teamKindBlock = new ItemStack(team.getKind().getMaterial(), team.getKind().getData());
				if(playerInv.contains(teamKindBlock, 2)) {
					playerInv.remove(teamKindBlock);
					playerInv.addItem(teamKindBlock);
					war.badMsg(player, "All that " + team.getName() + " must have been heavy!");
				}
			}
		}
	}
	
	public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
		if(war.isLoaded()) {
			Player player = event.getPlayer();
			Team talkingPlayerTeam = war.getPlayerTeam(player.getName());
			if(talkingPlayerTeam != null) {
				String msg = event.getMessage();
				String[] split = msg.split(" ");
				if(!war.isZoneMaker(player) && split.length > 0 && split[0].startsWith("/")) {
					String command = split[0].substring(1);
					if(!command.equals("war") && !command.equals("zones") && !command.equals("warzones")
						&& !command.equals("zone") && !command.equals("warzone")
						&& !command.equals("teams")
						&& !command.equals("join")
						&& !command.equals("leave")
						&& !command.equals("team")
						&& !command.equals("warhub")
						&& !command.equals("zonemaker")) {
						war.badMsg(player, "Can't use anything but War commands (e.g. /leave, /warhub) while you're playing in a warzone.");
						event.setCancelled(true);
					}
				}
			}
		}
	}
	
	public void onPlayerKick(PlayerKickEvent event) {
		if(war.isLoaded()) {
			Player player = event.getPlayer();
			String reason = event.getReason();
			if(reason.contains("moved") || reason.contains("too quickly") || reason.contains("Hacking")) {
				boolean inWarzone = war.inAnyWarzone(player.getLocation());
				boolean inLobby = war.inAnyWarzone(player.getLocation());
				boolean inWarhub = false;
				if(war.getWarHub() != null && war.getWarHub().getVolume().contains(player.getLocation())) { 
					inWarhub = true;
				}
				if(inWarzone || inLobby || inWarhub) {
					event.setCancelled(true);
					war.logWarn("Prevented " + player.getName() + " from getting kicked.");
				}
			}
		}
	}
	
	public void onPlayerInteract(PlayerInteractEvent event) {
		if(war.isLoaded()) {
			Player player = event.getPlayer();
			if(player.getItemInHand().getType() == Material.WOOD_SWORD && war.isWandBearer(player)) {
				String zoneName = war.getWandBearerZone(player);
				ZoneSetter setter = new ZoneSetter(war, player, zoneName);
				if(event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_AIR) {
					war.badMsg(player, "Too far.");
				} else if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
					setter.placeCorner1(event.getClickedBlock());
					event.setUseItemInHand(Result.ALLOW);
				} else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
					setter.placeCorner2(event.getClickedBlock());
					event.setUseItemInHand(Result.ALLOW);
				}
				
			}
		}
	}
	
	public void onPlayerMove(PlayerMoveEvent event) {
		if(war.isLoaded()) {
			Player player = event.getPlayer();
			Location playerLoc = event.getFrom(); // same as player.getLoc. Don't call again we need same result.
			Warzone locZone = null;
			ZoneLobby locLobby = null;
			locZone = war.warzone(playerLoc);
			locLobby = war.lobby(playerLoc);
			boolean canPlay = war.canPlayWar(player);
			boolean isMaker = war.isZoneMaker(player);
			
			// Zone walls
			Team currentTeam = war.getPlayerTeam(player.getName());
			Warzone playerWarzone = war.getPlayerTeamWarzone(player.getName());	// this uses the teams, so it asks: get the player's team's warzone
			boolean protecting = false;
			if(currentTeam != null) {
				//Warzone nearbyZone = war.zoneOfZoneWallAtProximity(playerLoc);
				protecting = playerWarzone.protectZoneWallAgainstPlayer(player);
			} else {
				Warzone nearbyZone = war.zoneOfZoneWallAtProximity(playerLoc);
				if(nearbyZone != null && !isMaker) {
					protecting = nearbyZone.protectZoneWallAgainstPlayer(player);
				}
			}
			
			if(!protecting) {
				// zone makers still need to delete their walls
				// make sure to delete any wall guards as you leave
				for(Warzone zone : war.getWarzones()) {
					zone.dropZoneWallGuardIfAny(player);
				}
			}
				
			boolean enteredGate = false;
			// Warzone lobby gates
			if(locLobby != null) {
				Warzone zone = locLobby.getZone(); 
				Team oldTeam = war.getPlayerTeam(player.getName());
				boolean isAutoAssignGate = false;
				if(oldTeam == null && canPlay) { // trying to counter spammy player move
					isAutoAssignGate = zone.getLobby().isAutoAssignGate(playerLoc);
					if(isAutoAssignGate) {
						if(zone.isDisabled()){
							handleDisabledZone(event, player, zone);
						} else {
							enteredGate = true;
							dropFromOldTeamIfAny(player);
							int noOfPlayers = 0;
							for(Team t : zone.getTeams()) {
								noOfPlayers += t.getPlayers().size();
							}
							if(noOfPlayers < zone.getTeams().size() * zone.getTeamCap()) {
								Team team = zone.autoAssign(player);
								event.setFrom(team.getTeamSpawn());
								event.setCancelled(true);
								if(war.getWarHub() != null) {
									war.getWarHub().resetZoneSign(zone);
								}
							} else {
								event.setFrom(zone.getTeleport());
								player.teleport(zone.getTeleport());
								event.setCancelled(true);
								war.badMsg(player, "All teams are full.");
							}
						}
						return;
					}
					
					// go through all the team gates
					for(Team team : zone.getTeams()){
						if(zone.getLobby().isInTeamGate(team, playerLoc)) {
							enteredGate = true;
							dropFromOldTeamIfAny(player);
							if(zone.isDisabled()){
								handleDisabledZone(event, player, zone);
							} else if(team.getPlayers().size() < zone.getTeamCap()) {
								team.addPlayer(player);
								team.resetSign();
								if(war.getWarHub() != null) {
									war.getWarHub().resetZoneSign(zone);
								}
								zone.keepPlayerInventory(player);
								war.msg(player, "Your inventory is in storage until you /leave.");
								zone.respawnPlayer(event, team, player);
								for(Team t : zone.getTeams()){
									t.teamcast("" + player.getName() + " joined team " + team.getName() + ".");
								}
							} else {
								event.setFrom(zone.getTeleport());
								player.teleport(zone.getTeleport());
								event.setCancelled(true);
								war.badMsg(player, "Team " + team.getName() + " is full.");
							}
							return;
						}
					}
									
					if (war.getWarHub() != null && zone.getLobby().isInWarHubLinkGate(playerLoc)){
						enteredGate = true;
						dropFromOldTeamIfAny(player);
						event.setFrom(war.getWarHub().getLocation());
						player.teleport(war.getWarHub().getLocation());
						event.setCancelled(true);
						war.msg(player, "Welcome to the War hub.");
						return;
					}
				}
				
			}
			
			// Warhub zone gates
			WarHub hub = war.getWarHub();
			if(hub != null && hub.getVolume().contains(playerLoc)) {
				Warzone zone = hub.getDestinationWarzoneForLocation(playerLoc);
					if(zone != null && zone.getTeleport() != null) {
						enteredGate = true;
						event.setFrom(zone.getTeleport());
								player.teleport(zone.getTeleport());
						event.setCancelled(true);
						war.msg(player, "Welcome to warzone " + zone.getName() + ".");
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
					playerWarzone.handlePlayerLeave(player, playerWarzone.getTeleport(), true);
					event.setCancelled(true);
					return;
				}
			}
		
			if(playerWarzone != null && !enteredGate) {
				Team team = war.getPlayerTeam(player.getName());
				
				// Player belongs to a warzone team but is outside: he snuck out or is at spawn and died
				if(locZone == null && team != null) {;
					war.badMsg(player, "You can't sneak out of a zone while in a team. Use /leave or walk out the lobby to exit the zone, please.");
					event.setFrom(team.getTeamSpawn());
					player.teleport(team.getTeamSpawn());
					event.setCancelled(true);
					return;
				}
	
				// Monuments	//SY
				if(team != null
						&& playerWarzone.nearAnyOwnedMonument(playerLoc, team) 
						&& player.getHealth() < 20
						&& player.getHealth() > 0	// don't heal the dead
						&& random.nextInt(77) == 3 ) {	// one chance out of many of getting healed
					int currentHp = player.getHealth();
					int newHp = currentHp + locZone.getMonumentHeal();
					if(newHp > 20) newHp = 20;
					player.setHealth(newHp);
					String isS = "s";					// no 's' in 'hearts' when it's just one heart
					if (newHp-currentHp==2) isS="";
					String heartNum = "";			// since (newHp-currentHp)/2 won't give the right amount
					if (newHp-currentHp==2)
						heartNum="1 ";
					else if (newHp-currentHp%2==0)		// for some reason
						heartNum=((newHp-currentHp)/2)+" ";
					else
						heartNum=((newHp-currentHp-1)/2)+".5 ";
					war.msg(player, "Your dance pleases the monument's voodoo. You gain "+heartNum+"heart"+isS+"!");
					return;
				}
				
				// Flag capture
				if(playerWarzone.isFlagThief(player.getName()) 
						&& (team.getSpawnVolume().contains(player.getLocation())
								|| (team.getFlagVolume() != null && team.getFlagVolume().contains(player.getLocation())))) {
					if(playerWarzone.isTeamFlagStolen(team)) {
						war.badMsg(player, "You can't capture the enemy flag until your team's flag is returned.");
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
								player.teleport(playerWarzone.getTeleport());
								event.setCancelled(true);
							} else {
								// added a point
								Team victim = playerWarzone.getVictimTeamForThief(player.getName());
								victim.getFlagVolume().resetBlocks();	// bring back flag to team that lost it
								victim.initializeTeamFlag();
								for(Team t : playerWarzone.getTeams()) {
									t.teamcast(player.getName() + " captured team " + victim.getName()
											+ "'s flag. Team " + team.getName() + " scores one point." );
								}
								playerWarzone.respawnPlayer(event, team, player);
								team.resetSign();
								playerWarzone.getLobby().resetTeamGateSign(team);
							}				
							playerWarzone.removeThief(player.getName());
						}
					}
					return;
				}
			} else if (locZone != null && locZone.getLobby() != null 
					&&  !locZone.getLobby().isLeavingZone(playerLoc) && !isMaker) { 
				// player is not in any team, but inside warzone boundaries, get him out
				Warzone zone = war.warzone(playerLoc);
				event.setFrom(zone.getTeleport());
				player.teleport(zone.getTeleport());
				event.setCancelled(true);
				war.badMsg(player, "You can't be inside a warzone without a team.");
				return;
			}
		}
	}
	
	private void handleDisabledZone(PlayerMoveEvent event, Player player, Warzone zone) {
		if(zone.getLobby() != null) {
			war.badMsg(player, "This warzone is disabled.");
			event.setFrom(zone.getTeleport());
			player.teleport(zone.getTeleport());
			event.setCancelled(true);
		}
	}

	private void dropFromOldTeamIfAny(Player player) {
		// drop from old team if any
		Team previousTeam = war.getPlayerTeam(player.getName());
		if(previousTeam != null) {
			if(!previousTeam.removePlayer(player.getName())){
				war.logWarn("Could not remove player " + player.getName() + " from team " + previousTeam.getName());
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
		for(Team team : warzone.getTeams()) {
			teamsMessage += team.getName() + " (" + team.getPoints() + " points, "+ team.getRemainingLifes() + "/" + warzone.getLifePool() + " lives left. ";
			for(Player member : team.getPlayers()) {
				teamsMessage += member.getName() + " ";
			}
			teamsMessage += ")  ";
		}
		return teamsMessage;
	}
}
