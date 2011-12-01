package bukkit.tommytony.war;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
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
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.tommytony.war.FlagReturn;
import com.tommytony.war.Team;
import com.tommytony.war.WarHub;
import com.tommytony.war.Warzone;
import com.tommytony.war.ZoneLobby;
import com.tommytony.war.ZoneSetter;

/**
 * @author tommytony, Tim Düsterhus
 * @package bukkit.tommytony.war
 */
public class WarPlayerListener extends PlayerListener {
	private java.util.Random random = new java.util.Random();
	private HashMap<String, Location> latestLocations = new HashMap<String, Location>(); 

	/**
	 * Correctly removes quitting players from warzones
	 *
	 * @see PlayerListener.onPlayerQuit()
	 */
	@Override
	public void onPlayerQuit(PlayerQuitEvent event) {
		if (War.war.isLoaded()) {
			Player player = event.getPlayer();
			Warzone zone = Warzone.getZoneByPlayerName(player.getName());
			if (zone != null) {
				zone.handlePlayerLeave(player, zone.getTeleport(), true);
			}

			if (War.war.isWandBearer(player)) {
				War.war.removeWandBearer(player);
			}
		}
	}

	@Override
	public void onPlayerDropItem(PlayerDropItemEvent event) {
		if (War.war.isLoaded()) {
			Player player = event.getPlayer();
			Team team = Team.getTeamByPlayerName(player.getName());
			if (team != null) {
				Warzone zone = Warzone.getZoneByPlayerName(player.getName());

				if (zone.isFlagThief(player.getName())) {
					// a flag thief can't drop his flag
					War.war.badMsg(player, "Can't drop items while stealing flag. What are you doing?! Run!");
					event.setCancelled(true);
				} else if (zone.isNoDrops()) {
					War.war.badMsg(player, "Can't drop items in this warzone.");
					event.setCancelled(true);
				} else {
					Item item = event.getItemDrop();
					if (item != null) {
						ItemStack itemStack = item.getItemStack();
						if (itemStack != null && itemStack.getType() == team.getKind().getMaterial() && itemStack.getData().getData() == team.getKind().getData()) {
							// Can't drop your team's kind block
							War.war.badMsg(player, "Can't drop " + team.getName() + " blocks.");
							event.setCancelled(true);
							return;
						}

						if (zone.isNearWall(player.getLocation()) && itemStack != null) {
							War.war.badMsg(player, "Can't drop items near the zone border!");
							event.setCancelled(true);
							return;
						}
						
						if (zone.getNewlyRespawned().keySet().contains(player.getName())) {
							// still at spawn
							War.war.badMsg(player, "Can't drop items while still in spawn.");
							event.setCancelled(true);
							return;
						}
					}
				}
			}

			if (War.war.isWandBearer(player)) {
				Item item = event.getItemDrop();
				if (item.getItemStack().getType() == Material.WOOD_SWORD) {
					String zoneName = War.war.getWandBearerZone(player);
					War.war.removeWandBearer(player);
					War.war.msg(player, "You dropped the zone " + zoneName + " wand.");
				}
			}
		}
	}

	@Override
	public void onPlayerPickupItem(PlayerPickupItemEvent event) {
		if (War.war.isLoaded()) {
			Player player = event.getPlayer();
			Team team = Team.getTeamByPlayerName(player.getName());
			if (team != null) {
				Warzone zone = Warzone.getZoneByPlayerName(player.getName());

				if (zone.isFlagThief(player.getName())) {
					// a flag thief can't pick up anything
					event.setCancelled(true);
				} else {
					Item item = event.getItem();
					if (item != null && item instanceof CraftItem) {
						CraftItem cItem = (CraftItem) item;
						if (cItem != null) {
							ItemStack itemStack = cItem.getItemStack();
							if (itemStack != null && itemStack.getType() == team.getKind().getMaterial() && player.getInventory().contains(new ItemStack(team.getKind().getMaterial(), team.getKind().getData()))) {
								// Can't pick up a second precious block
								event.setCancelled(true);
								return;
							}
						}
					}
				}
			}
		}
	}

	@Override
	public void onInventoryOpen(PlayerInventoryEvent event) {
		if (War.war.isLoaded()) {
			Player player = event.getPlayer();
			Inventory inventory = event.getInventory();
			Team team = Team.getTeamByPlayerName(player.getName());
			if (team != null && inventory instanceof PlayerInventory) {
				// make sure the player doesn't have too many precious blocks
				// or illegal armor (i.e. armor not found in loadout)
				PlayerInventory playerInv = (PlayerInventory) inventory;
				ItemStack teamKindBlock = new ItemStack(team.getKind().getMaterial(), team.getKind().getData());
				if (playerInv.contains(teamKindBlock, 2)) {
					playerInv.remove(teamKindBlock);
					playerInv.addItem(teamKindBlock);
					War.war.badMsg(player, "All that " + team.getName() + " must have been heavy!");
				}
			}
		}
	}

	@Override
	public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
		if (War.war.isLoaded()) {
			Player player = event.getPlayer();
			Team talkingPlayerTeam = Team.getTeamByPlayerName(player.getName());
			if (talkingPlayerTeam != null) {
				String msg = event.getMessage();
				String[] split = msg.split(" ");
				if (!War.war.isZoneMaker(player) && split.length > 0 && split[0].startsWith("/")) {
					String command = split[0].substring(1);
					if (!command.equals("war") && !command.equals("zones") && !command.equals("warzones") && !command.equals("zone") && !command.equals("warzone") && !command.equals("teams") && !command.equals("join") && !command.equals("leave") && !command.equals("team") && !command.equals("warhub") && !command.equals("zonemaker")) {
						// allow white commands
						for (String whiteCommand : War.war.getCommandWhitelist()) {
							if (whiteCommand.equals(command)) {
								return;
							}
						}

						War.war.badMsg(player, "Can't use anything but War commands (e.g. /leave, /warhub) while you're playing in a warzone.");
						event.setCancelled(true);
					}
				}
			}
		}
	}

	@Override
	public void onPlayerKick(PlayerKickEvent event) {
		if (War.war.isLoaded()) {
			Player player = event.getPlayer();
			String reason = event.getReason();
			if (reason.contains("moved") || reason.contains("too quickly") || reason.contains("Hacking")) {
				boolean inWarzone = Warzone.getZoneByLocation(player) != null;
				boolean inLobby = ZoneLobby.getLobbyByLocation(player) != null;
				boolean inWarhub = false;
				if (War.war.getWarHub() != null && War.war.getWarHub().getVolume().contains(player.getLocation())) {
					inWarhub = true;
				}
				if (inWarzone || inLobby || inWarhub) {
					event.setCancelled(true);
					War.war.log("Prevented " + player.getName() + " from getting kicked.", java.util.logging.Level.WARNING);
				}
			}
		}
	}

	@Override
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (War.war.isLoaded()) {
			Player player = event.getPlayer();
			if (player.getItemInHand().getType() == Material.WOOD_SWORD && War.war.isWandBearer(player)) {
				String zoneName = War.war.getWandBearerZone(player);
				ZoneSetter setter = new ZoneSetter(player, zoneName);
				if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_AIR) {
					War.war.badMsg(player, "Too far.");
				} else if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
					setter.placeCorner1(event.getClickedBlock());
					event.setUseItemInHand(Result.ALLOW);
				} else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
					setter.placeCorner2(event.getClickedBlock());
					event.setUseItemInHand(Result.ALLOW);
				}
			} 

			Warzone zone = Warzone.getZoneByPlayerName(player.getName());
			if (zone != null && zone.getNewlyRespawned().containsKey(player.getName()) && player.getItemInHand().getType() == Material.BOW) {
				event.setUseItemInHand(Result.DENY);
				War.war.badMsg(player, "Can't shoot from inside the spawn.");
			}
		}
	}

	@Override
	public void onPlayerMove(PlayerMoveEvent event) {
		if (!War.war.isLoaded()) {
			return;
		}
		
		Player player = event.getPlayer();
		Location playerLoc = event.getFrom(); // same as player.getLoc. Don't call again we need same result.
		
		Location previousLocation = latestLocations.get(player.getName());
		if (previousLocation != null &&
				playerLoc.getBlockX() == previousLocation.getBlockX() &&
				playerLoc.getBlockY() == previousLocation.getBlockY() &&
				playerLoc.getBlockZ() == previousLocation.getBlockZ() &&
				playerLoc.getWorld() == previousLocation.getWorld()) {
			// we only care when people change location
			return;
		}
		latestLocations.put(player.getName(), playerLoc);
		
		Warzone locZone = Warzone.getZoneByLocation(playerLoc);
		ZoneLobby locLobby = ZoneLobby.getLobbyByLocation(playerLoc);

		boolean canPlay = War.war.canPlayWar(player);
		boolean isMaker = War.war.isZoneMaker(player);

		// Zone walls
		Team currentTeam = Team.getTeamByPlayerName(player.getName());
		Warzone playerWarzone = Warzone.getZoneByPlayerName(player.getName()); // this uses the teams, so it asks: get the player's team's warzone
		boolean protecting = false;
		if (currentTeam != null) {
			if (playerWarzone.isGlassWalls()) {
				protecting = playerWarzone.protectZoneWallAgainstPlayer(player);
			}
		} else {
			Warzone nearbyZone = War.war.zoneOfZoneWallAtProximity(playerLoc);
			if (nearbyZone != null && nearbyZone.isGlassWalls() && !isMaker) {
				protecting = nearbyZone.protectZoneWallAgainstPlayer(player);
			}
		}

		if (!protecting) {
			// zone makers still need to delete their walls
			// make sure to delete any wall guards as you leave
			for (Warzone zone : War.war.getWarzones()) {
				zone.dropZoneWallGuardIfAny(player);
			}
		}

		// Warzone lobby gates
		if (locLobby != null) {
			Warzone zone = locLobby.getZone();
			Team oldTeam = Team.getTeamByPlayerName(player.getName());
			boolean isAutoAssignGate = false;
			if (oldTeam == null && canPlay) { // trying to counter spammy player move
				isAutoAssignGate = zone.getLobby().isAutoAssignGate(playerLoc);
				if (isAutoAssignGate) {
					if (zone.isDisabled()) {
						this.handleDisabledZone(event, player, zone);
					} else {
						this.dropFromOldTeamIfAny(player);
						int noOfPlayers = 0;
						for (Team t : zone.getTeams()) {
							noOfPlayers += t.getPlayers().size();
						}
						if (noOfPlayers < zone.getTeams().size() * zone.getTeamCap()) {
							zone.autoAssign(player);

							if (War.war.getWarHub() != null) {
								War.war.getWarHub().resetZoneSign(zone);
							}
						} else {
							event.setTo(zone.getTeleport());
							// player.teleport(zone.getTeleport());
							War.war.badMsg(player, "All teams are full.");
						}
					}
					return;
				}

				// go through all the team gates
				for (Team team : zone.getTeams()) {
					if (zone.getLobby().isInTeamGate(team, playerLoc)) {
						this.dropFromOldTeamIfAny(player);
						if (zone.isDisabled()) {
							this.handleDisabledZone(event, player, zone);
						} else if (team.getPlayers().size() < zone.getTeamCap()) {
							team.addPlayer(player);
							team.resetSign();
							if (War.war.getWarHub() != null) {
								War.war.getWarHub().resetZoneSign(zone);
							}
							zone.keepPlayerState(player);
							War.war.msg(player, "Your inventory is in storage until you exit with '/war leave'.");
							zone.respawnPlayer(event, team, player);
							for (Team t : zone.getTeams()) {
								t.teamcast("" + player.getName() + " joined team " + team.getName() + ".");
							}
							zone.resetInventory(team, player);
						} else {
							event.setTo(zone.getTeleport());
							War.war.badMsg(player, "Team " + team.getName() + " is full.");
						}
						return;
					}
				}

				if (War.war.getWarHub() != null && zone.getLobby().isInWarHubLinkGate(playerLoc) && !War.war.getWarHub().getVolume().contains(player.getLocation())) {
					this.dropFromOldTeamIfAny(player);
					event.setTo(War.war.getWarHub().getLocation());
					// player.teleport(war.getWarHub().getLocation());
					War.war.msg(player, "Welcome to the War hub.");
					return;
				}
			}

		}

		// Warhub zone gates
		WarHub hub = War.war.getWarHub();
		if (hub != null && hub.getVolume().contains(player.getLocation())) {
			Warzone zone = hub.getDestinationWarzoneForLocation(playerLoc);
			if (zone != null && zone.getTeleport() != null) {
				event.setTo(zone.getTeleport());
				// player.teleport(zone.getTeleport());
				War.war.msg(player, "Welcome to warzone " + zone.getName() + ".");
				return;
			}
		}

		boolean isLeaving = playerWarzone != null && playerWarzone.getLobby().isLeavingZone(playerLoc);
		Team playerTeam = Team.getTeamByPlayerName(player.getName());
		if (isLeaving) { // already in a team and in warzone, leaving
			// same as leave
			if (playerTeam != null) {
				boolean atSpawnAlready = playerTeam.getTeamSpawn().getBlockX() == player.getLocation().getBlockX() && playerTeam.getTeamSpawn().getBlockY() == player.getLocation().getBlockY() && playerTeam.getTeamSpawn().getBlockZ() == player.getLocation().getBlockZ();
				if (!atSpawnAlready) {
					playerWarzone.handlePlayerLeave(player, playerWarzone.getTeleport(), event, true);
					return;
				}
			}
		}

		if (playerWarzone != null) {
			// Player belongs to a warzone team but is outside: he snuck out or is at spawn and died
			if (locZone == null && playerTeam != null && playerWarzone.getLobby() != null && !playerWarzone.getLobby().getVolume().contains(playerLoc) && !isLeaving) {
				List<BlockFace> nearestWalls = playerWarzone.getNearestWalls(playerLoc);
				War.war.badMsg(player, "Use /leave (or /war leave) to exit the zone.");
				if(nearestWalls != null && nearestWalls.size() > 0) {
					// First, try to bump the player back in
					int northSouthMove = 0;
					int eastWestMove = 0;
					int upDownMove = 0;
					int moveDistance = 1;
					
					if (nearestWalls.contains(BlockFace.NORTH)) {
						// move south
						northSouthMove += moveDistance;
					} else if (nearestWalls.contains(BlockFace.SOUTH)) {
						// move north
						northSouthMove -= moveDistance;
					} 
					
					if (nearestWalls.contains(BlockFace.EAST)) {
						// move west
						eastWestMove += moveDistance;
					} else if (nearestWalls.contains(BlockFace.WEST)) {
						// move east
						eastWestMove -= moveDistance;
					} 
					
					if (nearestWalls.contains(BlockFace.UP)) {
						upDownMove -= moveDistance;
					} else if (nearestWalls.contains(BlockFace.DOWN)) {
						// fell off the map, back to spawn
						event.setTo(playerTeam.getTeamSpawn());
						return;
					}  
					
					event.setTo(new Location(playerLoc.getWorld(), 
											playerLoc.getX() + northSouthMove, 
											playerLoc.getY() + upDownMove, 
											playerLoc.getZ() + eastWestMove, 
											playerLoc.getYaw(),
											playerLoc.getPitch()));
					return;
				} else {				
					// Otherwise, send him to spawn
					event.setTo(playerTeam.getTeamSpawn());
					return;
				}
			}
			
			if (!playerTeam.getSpawnVolume().contains(playerLoc)) {
				if (!playerWarzone.isEnoughPlayers()) {
					War.war.badMsg(player, "Can't leave spawn until there's a minimum of " + playerWarzone.getMinPlayers() +" player(s) on at least " + playerWarzone.getMinTeams() + " team(s).");
					event.setTo(playerTeam.getTeamSpawn());
					return;
				}
				if (playerWarzone.isRespawning(player)) {
					War.war.badMsg(player, "Can't leave spawn for 10 seconds after spawning!");
					event.setTo(playerTeam.getTeamSpawn());
					return;
				}
			}

			// Monuments
			if (playerTeam != null && playerWarzone.nearAnyOwnedMonument(playerLoc, playerTeam) && player.getHealth() < 20 && player.getHealth() > 0 // don't heal the dead
					&& this.random.nextInt(7) == 3) { // one chance out of many of getting healed
				int currentHp = player.getHealth();
				int newHp = Math.max(20, currentHp + locZone.getMonumentHeal());

				player.setHealth(newHp);
				String isS = "s";
				String heartNum = ""; // since (newHp-currentHp)/2 won't give the right amount
				if (newHp - currentHp == 2) { // no 's' in 'hearts' when it's just one heart
					isS = "";
					heartNum = "one ";
				} else if (newHp - currentHp % 2 == 0) {
					heartNum = ((newHp - currentHp) / 2) + " ";
				} else {
					heartNum = ((newHp - currentHp - 1) / 2) + ".5 ";
				}
				War.war.msg(player, "Your dance pleases the monument's voodoo. You gain " + heartNum + "heart" + isS + "!");
				return;
			}

			// Flag capture
			if (playerWarzone.isFlagThief(player.getName())) {
				boolean inSpawn = playerTeam.getSpawnVolume().contains(player.getLocation());
				boolean inFlag = (playerTeam.getFlagVolume() != null && playerTeam.getFlagVolume().contains(player.getLocation()));

				if (playerWarzone.getFlagReturn() == FlagReturn.BOTH) {
					if (!inSpawn && !inFlag) {
						return;
					}
				} else if (playerWarzone.getFlagReturn() == FlagReturn.SPAWN) {
					if (inFlag) {
						War.war.badMsg(player, "You have to capture the enemy flag at your team's spawn.");
						return;
					} else if (!inSpawn) {
						return;
					}
				} else if (playerWarzone.getFlagReturn() == FlagReturn.FLAG) {
					if (inSpawn) {
						War.war.badMsg(player, "You have to capture the enemy flag at your team's flag.");
						return;
					} else if (!inFlag) {
						return;
					}
				}

				if (playerWarzone.isTeamFlagStolen(playerTeam) && playerWarzone.isFlagMustBeHome()) {
					War.war.badMsg(player, "You can't capture the enemy flag until your team's flag is returned.");
				} else {
					synchronized (playerWarzone) {
						// flags can be captured at own spawn or own flag pole
						playerTeam.addPoint();
						if (playerTeam.getPoints() >= playerWarzone.getScoreCap()) {
							if (playerWarzone.hasPlayerState(player.getName())) {
								playerWarzone.restorePlayerState(player);
							}
							playerWarzone.handleScoreCapReached(player, playerTeam.getName());
							event.setTo(playerWarzone.getTeleport());
						} else {
							// added a point
							Team victim = playerWarzone.getVictimTeamForThief(player.getName());
							victim.getFlagVolume().resetBlocks(); // bring back flag to team that lost it
							victim.initializeTeamFlag();
							for (Team t : playerWarzone.getTeams()) {
								t.teamcast(playerTeam.getKind().getColor() + player.getName() + ChatColor.WHITE
										+ " captured team " + victim.getName() + "'s flag. Team " + playerTeam.getName() + " scores one point.");
							}
							playerWarzone.respawnPlayer(event, playerTeam, player);
							playerTeam.resetSign();
							playerWarzone.getLobby().resetTeamGateSign(playerTeam);
						}
						playerWarzone.removeThief(player.getName());
					}
				}
				return;
			}
			
			// Class selection lock
			if (!playerTeam.getSpawnVolume().contains(player.getLocation()) && 
					playerWarzone.getNewlyRespawned().keySet().contains(player.getName())) {
				playerWarzone.getNewlyRespawned().remove(player.getName());
			}
			
		} else if (locZone != null && locZone.getLobby() != null && !locZone.getLobby().isLeavingZone(playerLoc) && !isMaker) {
			// player is not in any team, but inside warzone boundaries, get him out
			Warzone zone = Warzone.getZoneByLocation(playerLoc);
			event.setTo(zone.getTeleport());
			War.war.badMsg(player, "You can't be inside a warzone without a team.");
			return;
		}
	}
	
	@Override
	public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
		if (War.war.isLoaded() && event.isSneaking()) {
			Warzone playerWarzone = Warzone.getZoneByLocation(event.getPlayer());
			Team playerTeam = Team.getTeamByPlayerName(event.getPlayer().getName());
			if (playerWarzone != null && playerTeam != null && playerWarzone.getExtraLoadouts().keySet().size() > 0 && playerTeam.getSpawnVolume().contains(event.getPlayer().getLocation())) {
				if (playerWarzone.getNewlyRespawned().keySet().contains(event.getPlayer().getName())) {
					Integer currentIndex = playerWarzone.getNewlyRespawned().get(event.getPlayer().getName());
					currentIndex = (currentIndex + 1) % (playerWarzone.getExtraLoadouts().keySet().size() + 1);
					playerWarzone.getNewlyRespawned().put(event.getPlayer().getName(), currentIndex);
					
					if (currentIndex == 0) {
						if (!playerWarzone.isRespawning(event.getPlayer())) playerWarzone.resetInventory(playerTeam, event.getPlayer(), playerWarzone.getLoadout());
						War.war.msg(event.getPlayer(), "Equipped default loadout.");
					} else {
						int i = 0;
						Iterator it = playerWarzone.getExtraLoadouts().entrySet().iterator();
					    while (it.hasNext()) {
					        Map.Entry pairs = (Map.Entry)it.next();
					        if (i == currentIndex - 1) {
					        	if (!playerWarzone.isRespawning(event.getPlayer())) playerWarzone.resetInventory(playerTeam, event.getPlayer(), (HashMap<Integer, ItemStack>)pairs.getValue());
								War.war.msg(event.getPlayer(), "Equipped " + pairs.getKey() + " loadout.");
					        }
					        i++;
					    }
					}
				} else {
					War.war.badMsg(event.getPlayer(), "Can't change loadout after exiting the spawn.");
				}
			}
		}
	}
	
	
	
	public void purgeLatestPositions() {
		this.latestLocations.clear();	
	}

	private void handleDisabledZone(PlayerMoveEvent event, Player player, Warzone zone) {
		if (zone.getLobby() != null) {
			War.war.badMsg(player, "This warzone is disabled.");
			event.setTo(zone.getTeleport());
		}
	}

	private void dropFromOldTeamIfAny(Player player) {
		// drop from old team if any
		Team previousTeam = Team.getTeamByPlayerName(player.getName());
		if (previousTeam != null) {
			if (!previousTeam.removePlayer(player.getName())) {
				War.war.log("Could not remove player " + player.getName() + " from team " + previousTeam.getName(), java.util.logging.Level.WARNING);
			}
		}
	}
	
	
}
