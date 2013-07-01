package com.tommytony.war.event;

import java.util.HashMap;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.getspout.spoutapi.SpoutManager;
import org.getspout.spoutapi.player.SpoutPlayer;

import com.tommytony.war.Team;
import com.tommytony.war.War;
import com.tommytony.war.Warzone;
import com.tommytony.war.command.ZoneSetter;
import com.tommytony.war.config.FlagReturn;
import com.tommytony.war.config.TeamConfig;
import com.tommytony.war.config.WarzoneConfig;
import com.tommytony.war.job.CantReEnterSpawnJob;
import com.tommytony.war.spout.SpoutDisplayer;
import com.tommytony.war.structure.Bomb;
import com.tommytony.war.structure.Cake;
import com.tommytony.war.structure.WarHub;
import com.tommytony.war.structure.ZoneLobby;
import com.tommytony.war.utility.Direction;
import com.tommytony.war.utility.Loadout;
import com.tommytony.war.utility.LoadoutSelection;
import java.util.ArrayList;
import java.util.Iterator;
import org.bukkit.event.player.PlayerTeleportEvent;

/**
 * @author tommytony, Tim Düsterhus
 * @package bukkit.tommytony.war
 */
public class WarPlayerListener implements Listener {
	private java.util.Random random = new java.util.Random();
	private HashMap<String, Location> latestLocations = new HashMap<String, Location>(); 

	/**
	 * Correctly removes quitting players from warzones
	 *
	 * @see PlayerListener.onPlayerQuit()
	 */
	@EventHandler
	public void onPlayerQuit(final PlayerQuitEvent event) {
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

	@EventHandler
	public void onPlayerDropItem(final PlayerDropItemEvent event) {
		if (War.war.isLoaded()) {
			Player player = event.getPlayer();
			Team team = Team.getTeamByPlayerName(player.getName());
			if (team != null) {
				Warzone zone = Warzone.getZoneByPlayerName(player.getName());

				if (zone.isFlagThief(player.getName())) {
					// a flag thief can't drop his flag
					War.war.badMsg(player, "Can't drop items while stealing flag. What are you doing?! Run!");
					event.setCancelled(true);
				} else if (zone.isBombThief(player.getName())) {
					// a bomb thief can't drop his bomb
					War.war.badMsg(player, "Can't drop items while stealing bomb. What are you doing?! Run for your enemy's spawn!");
					event.setCancelled(true);
				} else if (zone.isCakeThief(player.getName())) {
					// a cake thief can't drop his cake
					War.war.badMsg(player, "Can't drop items while stealing cake. What are you doing?! Run!");
					event.setCancelled(true);
				} else if (zone.getWarzoneConfig().getBoolean(WarzoneConfig.NODROPS)) {
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
						
						if (zone.getLoadoutSelections().keySet().contains(player.getName())
								&& zone.getLoadoutSelections().get(player.getName()).isStillInSpawn()) {
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

	@EventHandler
	public void onPlayerPickupItem(final PlayerPickupItemEvent event) {
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
					if (item != null) {
						ItemStack itemStack = item.getItemStack();
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

	@EventHandler
	public void onPlayerCommandPreprocess(final PlayerCommandPreprocessEvent event) {
		if (War.war.isLoaded()) {
			Player player = event.getPlayer();
			Team talkingPlayerTeam = Team.getTeamByPlayerName(player.getName());
			if (talkingPlayerTeam != null) {
				String msg = event.getMessage();
				String[] split = msg.split(" ");
				if (!War.war.isWarAdmin(player) && split.length > 0 && split[0].startsWith("/")) {
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

	@EventHandler
	public void onPlayerKick(final PlayerKickEvent event) {
		if (War.war.isLoaded()) {
			Player player = event.getPlayer();
			Warzone warzone = Warzone.getZoneByLocation(player);
			
			if (warzone != null) {
				// kick player from warzone as well
				warzone.handlePlayerLeave(player, warzone.getTeleport(), true);
			}
		}
	}

	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (War.war.isLoaded()) {
			Player player = event.getPlayer();
			if (player.getItemInHand().getType() == Material.WOOD_SWORD && War.war.isWandBearer(player)) {
				String zoneName = War.war.getWandBearerZone(player);
				ZoneSetter setter = new ZoneSetter(player, zoneName);
				if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_AIR) {
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
			if (zone != null && zone.getLoadoutSelections().containsKey(player.getName()) 
					&& zone.getLoadoutSelections().get(player.getName()).isStillInSpawn()) {
				event.setUseItemInHand(Result.DENY);
				ItemStack inHand = event.getItem();
				
				if (inHand != null) {
					ItemStack newItemInHand = War.war.copyStack(inHand);
				
					event.getPlayer().setItemInHand(newItemInHand);
					event.setCancelled(true);
					
					War.war.badMsg(player, "Can't use items while still in spawn.");
				}
			}
			if (zone != null && event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock().getType() == Material.ENDER_CHEST) {
				event.setCancelled(true);
				War.war.badMsg(player, "Can't use ender chests while playing in a warzone!");
			}
		}
		     
		//Soup-PvP Stuff - nicholasntp
			if (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR) {
				Player player = event.getPlayer();
				Warzone zone = Warzone.getZoneByPlayerName(player.getName());
			      if (zone != null && zone.soupHealing()) {    
					ItemStack item = event.getItem();
			           if ((item != null) && (item.getType() == Material.MUSHROOM_SOUP)) {
			             
			             if (player.getHealth() < 20) {
			            	 int h = player.getHealth();
			            	 if (h < 14) {
			               player.setHealth((int) (h + 7));
			            	 }
			            	 else {
			            		 player.setHealth(20);
			            	 }
			               
			               item.setType(Material.BOWL);
			             } 
			             else if (player.getFoodLevel() < 20) {
			               player.setFoodLevel(20);
			               item.setType(Material.BOWL);
			             }
			           }
			      }
			}
		
	}

	@EventHandler
	public void onPlayerMove(final PlayerMoveEvent event) {
		if (!War.war.isLoaded()) {
			return;
		}
		
		Player player = event.getPlayer();
		Location playerLoc = event.getTo(); // Don't call again we need same result.
		
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

		boolean isMaker = War.war.isZoneMaker(player);

		// Zone walls
		Team currentTeam = Team.getTeamByPlayerName(player.getName());
		Warzone playerWarzone = Warzone.getZoneByPlayerName(player.getName()); // this uses the teams, so it asks: get the player's team's warzone
		boolean protecting = false;
		if (currentTeam != null) {
			if (playerWarzone.getWarzoneConfig().getBoolean(WarzoneConfig.GLASSWALLS)) {
				protecting = playerWarzone.protectZoneWallAgainstPlayer(player);
			}
		} else {
			Warzone nearbyZone = War.war.zoneOfZoneWallAtProximity(playerLoc);
			if (nearbyZone != null && nearbyZone.getWarzoneConfig().getBoolean(WarzoneConfig.GLASSWALLS) && !isMaker) {
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
			if (oldTeam == null) { // trying to counter spammy player move
				isAutoAssignGate = zone.getLobby().isAutoAssignGate(playerLoc);
				if (isAutoAssignGate) {
					if (zone.getWarzoneConfig().getBoolean(WarzoneConfig.DISABLED)) {
						this.handleDisabledZone(event, player, zone);
					} else {
						this.dropFromOldTeamIfAny(player);
						int noOfPlayers = 0;
						int totalCap = 0;
						for (Team t : zone.getTeams()) {
							noOfPlayers += t.getPlayers().size();
							totalCap += t.getTeamConfig().resolveInt(TeamConfig.TEAMSIZE);
						}
						
						if (noOfPlayers < totalCap) {
							boolean assigned = zone.autoAssign(player) != null ? true : false;
							if (!assigned) {
								event.setTo(zone.getTeleport());
								War.war.badMsg(player, "You don't have permission for any of the available teams in this warzone");
							}

							if (War.war.getWarHub() != null && assigned) {
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
						if (zone.getWarzoneConfig().getBoolean(WarzoneConfig.DISABLED)) {
							this.handleDisabledZone(event, player, zone);
						} else if (team.getPlayers().size() < team.getTeamConfig().resolveInt(TeamConfig.TEAMSIZE)
								&& War.war.canPlayWar(player, team)) {
							if (player.getWorld() != zone.getWorld()) {
								player.teleport(zone.getWorld().getSpawnLocation());
							}
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
						} else if (!War.war.canPlayWar(player, team)) {
							event.setTo(zone.getTeleport());
							War.war.badMsg(player, "You don't have permission to join team " + team.getName());
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
				if (!playerWarzone.getWarzoneConfig().getBoolean(WarzoneConfig.REALDEATHS)) {
					War.war.badMsg(player, "Use /leave (or /war leave) to exit the zone.");
				}
				if(nearestWalls != null && nearestWalls.size() > 0) {
					// First, try to bump the player back in
					int northSouthMove = 0;
					int eastWestMove = 0;
					int upDownMove = 0;
					int moveDistance = 1;
					
					if (nearestWalls.contains(Direction.NORTH())) {
						// move south
						northSouthMove += moveDistance;
					} else if (nearestWalls.contains(Direction.SOUTH())) {
						// move north
						northSouthMove -= moveDistance;
					} 
					
					if (nearestWalls.contains(Direction.EAST())) {
						// move west
						eastWestMove += moveDistance;
					} else if (nearestWalls.contains(Direction.WEST())) {
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
					
					// Otherwise, send him to spawn (first make sure he drops his flag/cake/bomb to prevent auto-cap and as punishment)
				} else if (playerWarzone.isFlagThief(player.getName())) {
					Team victimTeam = playerWarzone.getVictimTeamForFlagThief(player.getName());

					// Get player back to spawn
					playerWarzone.respawnPlayer(event, playerTeam, player);
					playerWarzone.removeFlagThief(player.getName());
					
					// Bring back flag of victim team
					victimTeam.getFlagVolume().resetBlocks();
					victimTeam.initializeTeamFlag();
					
					for (Team team : playerWarzone.getTeams()) {
						team.teamcast(player.getName() + " dropped the " + victimTeam.getName() + " flag!");
					}					
					return;
				} else if (playerWarzone.isCakeThief(player.getName())) {
					Cake cake = playerWarzone.getCakeForThief(player.getName());
					
					// Get player back to spawn
					playerWarzone.respawnPlayer(event, playerTeam, player);
					playerWarzone.removeCakeThief(player.getName());
					
					// Bring back cake
					cake.getVolume().resetBlocks();
					cake.addCakeBlocks();
					
					for (Team team : playerWarzone.getTeams()) {
						team.teamcast(player.getName() + " dropped the " + cake.getName() + " cake!");
					}		
					return;
				} else if (playerWarzone.isBombThief(player.getName())) {
					Bomb bomb = playerWarzone.getBombForThief(player.getName());
					
					// Get player back to spawn
					playerWarzone.respawnPlayer(event, playerTeam, player);
					playerWarzone.removeBombThief(player.getName());
					
					// Bring back bomb
					bomb.getVolume().resetBlocks();
					bomb.addBombBlocks();

					for (Team team : playerWarzone.getTeams()) {
						team.teamcast(player.getName() + " dropped the " + bomb.getName() + " bomb!");
					}		
					return;
				} else {
					// Get player back to spawn
					playerWarzone.respawnPlayer(event, playerTeam, player);
					return;
				}
			}
						
			LoadoutSelection loadoutSelectionState = playerWarzone.getLoadoutSelections().get(player.getName());
			FlagReturn flagReturn = playerTeam.getTeamConfig().resolveFlagReturn();
			if (!playerTeam.getSpawnVolume().contains(playerLoc)) {
				if (!playerWarzone.isEnoughPlayers() && loadoutSelectionState != null && loadoutSelectionState.isStillInSpawn()) {
					// Be sure to keep only players that just respawned locked inside the spawn for minplayer/minteams restrictions - otherwise 
					// this will conflict with the can't-renter-spawn bump just a few lines below  
					War.war.badMsg(player, "Can't leave spawn until there's a minimum of " + playerWarzone.getWarzoneConfig().getInt(WarzoneConfig.MINPLAYERS)
							+" player(s) on at least " + playerWarzone.getWarzoneConfig().getInt(WarzoneConfig.MINTEAMS) + " team(s).");
					event.setTo(playerTeam.getTeamSpawn());
					return;
				}
				if (playerWarzone.isRespawning(player)) {
					int rt = playerTeam.getTeamConfig().resolveInt(TeamConfig.RESPAWNTIMER);
					String isS = "s";
					if (rt == 1) {
						isS = "";
					}
					War.war.badMsg(player, "Can't leave spawn for " + rt + " second" + isS + " after spawning!");
					event.setTo(playerTeam.getTeamSpawn());
					return;
				}
			} else if (loadoutSelectionState != null && !loadoutSelectionState.isStillInSpawn()
					&& !playerWarzone.isCakeThief(player.getName())
					&& (flagReturn.equals(FlagReturn.BOTH) || flagReturn.equals(FlagReturn.SPAWN)) 
					&& !playerWarzone.isFlagThief(player.getName())) {
				
				// player is in spawn, but has left already: he should NOT be let back in - kick him out gently
				// if he sticks around too long.
				// (also, be sure you aren't preventing the flag or cake from being captured)
				if (!CantReEnterSpawnJob.getPlayersUnderSuspicion().contains(player.getName())) {
					CantReEnterSpawnJob job = new CantReEnterSpawnJob(player, playerTeam);
					War.war.getServer().getScheduler().scheduleSyncDelayedTask(War.war, job, 12);
				}
				return;
			}

			// Monuments
			if (playerTeam != null && playerWarzone.nearAnyOwnedMonument(playerLoc, playerTeam) && player.getHealth() < 20 && player.getHealth() > 0 // don't heal the dead
					&& this.random.nextInt(7) == 3) { // one chance out of many of getting healed
				int currentHp = player.getHealth();
				int newHp = Math.min(20, currentHp + locZone.getWarzoneConfig().getInt(WarzoneConfig.MONUMENTHEAL));

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
				
				// smoky
				if (System.currentTimeMillis() % 13 == 0) {
					playerWarzone.getWorld().playEffect(player.getLocation(), Effect.POTION_BREAK, playerTeam.getKind().getPotionEffectColor());
				}
				
				// Make sure game ends can't occur simultaneously. 
				// See Warzone.handleDeath() for details.
				boolean inSpawn = playerTeam.getSpawnVolume().contains(player.getLocation());
				boolean inFlag = (playerTeam.getFlagVolume() != null && playerTeam.getFlagVolume().contains(player.getLocation()));

				if (playerTeam.getTeamConfig().resolveFlagReturn().equals(FlagReturn.BOTH)) {
					if (!inSpawn && !inFlag) {
						return;
					}
				} else if (playerTeam.getTeamConfig().resolveFlagReturn().equals(FlagReturn.SPAWN)) {
					if (inFlag) {
						War.war.badMsg(player, "You have to capture the enemy flag at your team's spawn.");
						return;
					} else if (!inSpawn) {
						return;
					}
				} else if (playerTeam.getTeamConfig().resolveFlagReturn().equals(FlagReturn.FLAG)) {
					if (inSpawn) {
						War.war.badMsg(player, "You have to capture the enemy flag at your team's flag.");
						return;
					} else if (!inFlag) {
						return;
					}
				}
				
				if (!playerTeam.getPlayers().contains(player)) {
					// Make sure player is still part of team, game may have ended while waiting)
					// Ignore the scorers that happened immediately after the game end.
					return;
				}

				if (playerWarzone.isTeamFlagStolen(playerTeam) && playerTeam.getTeamConfig().resolveBoolean(TeamConfig.FLAGMUSTBEHOME)) {
					War.war.badMsg(player, "You can't capture the enemy flag until your team's flag is returned.");
				} else {
					// flags can be captured at own spawn or own flag pole
					if (playerWarzone.isReinitializing()) {
						// Battle already ended or interrupted
						playerWarzone.respawnPlayer(event, playerTeam, player);
					} else {
						// All good - proceed with scoring
						playerTeam.addPoint();
						Team victim = playerWarzone.getVictimTeamForFlagThief(player.getName());
						
						// Notify everyone
						for (Team t : playerWarzone.getTeams()) {
							if (War.war.isSpoutServer()) {
								for (Player p : t.getPlayers()) {
									SpoutPlayer sp = SpoutManager.getPlayer(p);
									if (sp.isSpoutCraftEnabled()) {
						                sp.sendNotification(
						                		SpoutDisplayer.cleanForNotification(playerTeam.getKind().getColor() + player.getName() + ChatColor.YELLOW + " captured"),
						                		SpoutDisplayer.cleanForNotification(victim.getKind().getColor() + victim.getName() + ChatColor.YELLOW + " flag!"),
						                		victim.getKind().getMaterial(),
						                		victim.getKind().getData(),
						                		10000);
									}
								}
							}
							t.teamcast(playerTeam.getKind().getColor() + player.getName() + ChatColor.WHITE
									+ " captured team " + victim.getName() + "'s flag. Team " + playerTeam.getName() + " scores one point.");
						}
						
						// Detect win conditions
						if (playerTeam.getPoints() >= playerTeam.getTeamConfig().resolveInt(TeamConfig.MAXSCORE)) {
							if (playerWarzone.hasPlayerState(player.getName())) {
								playerWarzone.restorePlayerState(player);
							}
							playerWarzone.handleScoreCapReached(playerTeam.getName());
							event.setTo(playerWarzone.getTeleport());
						} else {
							// just added a point
							victim.getFlagVolume().resetBlocks(); // bring back flag to team that lost it
							victim.initializeTeamFlag();
							
							playerWarzone.respawnPlayer(event, playerTeam, player);
							playerTeam.resetSign();
							playerWarzone.getLobby().resetTeamGateSign(playerTeam);
						}
					}
					
					playerWarzone.removeFlagThief(player.getName());
					
					return;
				}
			}
			
			// Bomb detonation
			if (playerWarzone.isBombThief(player.getName())) {
				// smoky
				playerWarzone.getWorld().playEffect(player.getLocation(), Effect.SMOKE, 0);
				
				// Make sure game ends can't occur simultaneously. 
				// Not thread safe. See Warzone.handleDeath() for details.
				boolean inEnemySpawn = false;
				Team victim = null;
				for (Team team : playerWarzone.getTeams()) {
					if (team != playerTeam 
							&& team.getSpawnVolume().contains(player.getLocation()) 
							&& team.getPlayers().size() > 0) {
						inEnemySpawn = true;
						victim = team;
						break;
					}
				}
				
				if (inEnemySpawn && playerTeam.getPlayers().contains(player)) {
					// Made sure player is still part of team, game may have ended while waiting.
					// Ignored the scorers that happened immediately after the game end.
					Bomb bomb = playerWarzone.getBombForThief(player.getName());
					
					// Boom!
					if (!playerWarzone.getWarzoneConfig().getBoolean(WarzoneConfig.UNBREAKABLE)) {
						// Don't blow up if warzone is unbreakable
						playerWarzone.getWorld().createExplosion(player.getLocation(), 2F);
					}
					
					if (playerWarzone.isReinitializing()) {
						// Battle already ended or interrupted
						playerWarzone.respawnPlayer(event, playerTeam, player);
					} else {
						// All good - proceed with scoring
						playerTeam.addPoint();
						
						// Notify everyone
						for (Team t : playerWarzone.getTeams()) {
							if (War.war.isSpoutServer()) {
								for (Player p : t.getPlayers()) {
									SpoutPlayer sp = SpoutManager.getPlayer(p);
									if (sp.isSpoutCraftEnabled()) {
						                sp.sendNotification(
						                		SpoutDisplayer.cleanForNotification(playerTeam.getKind().getColor() + player.getName() + ChatColor.YELLOW + " blew up "),
						                		SpoutDisplayer.cleanForNotification(victim.getKind().getColor() + victim.getName() + ChatColor.YELLOW + "'s spawn!"),
						                		victim.getKind().getMaterial(),
						                		victim.getKind().getData(),
						                		10000);
									}
								}
							}
							t.teamcast(playerTeam.getKind().getColor() + player.getName() + ChatColor.WHITE
									+ " blew up team " + victim.getName() + "'s spawn. Team " + playerTeam.getName() + " scores one point.");
						}
						
						// Detect win conditions
						if (playerTeam.getPoints() >= playerTeam.getTeamConfig().resolveInt(TeamConfig.MAXSCORE)) {
							if (playerWarzone.hasPlayerState(player.getName())) {
								playerWarzone.restorePlayerState(player);
							}
							playerWarzone.handleScoreCapReached(playerTeam.getName());
							event.setTo(playerWarzone.getTeleport());
						} else {
							// just added a point
							
							// restore bombed team's spawn
							victim.getSpawnVolume().resetBlocks(); 
							victim.initializeTeamSpawn();
							
							// bring back tnt
							bomb.getVolume().resetBlocks();
							bomb.addBombBlocks();
							
							playerWarzone.respawnPlayer(event, playerTeam, player);
							playerTeam.resetSign();
							playerWarzone.getLobby().resetTeamGateSign(playerTeam);
						}					
					}
					
					playerWarzone.removeBombThief(player.getName());
					
					return;
				}
			}
			
			// Cake retrieval
			if (playerWarzone.isCakeThief(player.getName())) {
				// smoky
				if (System.currentTimeMillis() % 13 == 0) {
					playerWarzone.getWorld().playEffect(player.getLocation(), Effect.POTION_BREAK, playerTeam.getKind().getPotionEffectColor());
				}
				
				// Make sure game ends can't occur simultaneously. 
				// Not thread safe. See Warzone.handleDeath() for details.
				boolean inSpawn = playerTeam.getSpawnVolume().contains(player.getLocation());
															
				if (inSpawn && playerTeam.getPlayers().contains(player)) {
					// Made sure player is still part of team, game may have ended while waiting.
					// Ignored the scorers that happened immediately after the game end.
					boolean hasOpponent = false; 
					for (Team t : playerWarzone.getTeams()) {
						if (t != playerTeam && t.getPlayers().size() > 0) {
							hasOpponent = true;
						}
					}
					
					// Don't let someone alone make points off cakes
					if (hasOpponent) {
						Cake cake = playerWarzone.getCakeForThief(player.getName());
						
						if (playerWarzone.isReinitializing()) {
							// Battle already ended or interrupted
							playerWarzone.respawnPlayer(event, playerTeam, player);
						} else {
							// All good - proceed with scoring
							// Woot! Cake effect: 1 pt + full lifepool
							playerTeam.addPoint();
							playerTeam.setRemainingLives(playerTeam.getTeamConfig().resolveInt(TeamConfig.LIFEPOOL));
							
							// Notify everyone
							for (Team t : playerWarzone.getTeams()) {
								if (War.war.isSpoutServer()) {
									for (Player p : t.getPlayers()) {
										SpoutPlayer sp = SpoutManager.getPlayer(p);
										if (sp.isSpoutCraftEnabled()) {
							                sp.sendNotification(
							                		SpoutDisplayer.cleanForNotification(playerTeam.getKind().getColor() + player.getName() + ChatColor.YELLOW + " captured"),
							                		SpoutDisplayer.cleanForNotification(ChatColor.YELLOW + "cake " + ChatColor.GREEN + cake.getName() + ChatColor.YELLOW + "!"),
							                		playerTeam.getKind().getMaterial(),
							                		playerTeam.getKind().getData(),
							                		10000);
										}
									}
								}
								
								t.teamcast(playerTeam.getKind().getColor() + player.getName() + ChatColor.WHITE
										+ " captured cake " + ChatColor.GREEN + cake.getName() + ChatColor.WHITE + ". Team " + playerTeam.getName() + " scores one point and gets a full lifepool.");
							}
							
							// Detect win conditions
							if (playerTeam.getPoints() >= playerTeam.getTeamConfig().resolveInt(TeamConfig.MAXSCORE)) {
								if (playerWarzone.hasPlayerState(player.getName())) {
									playerWarzone.restorePlayerState(player);
								}
								playerWarzone.handleScoreCapReached(playerTeam.getName());
								event.setTo(playerWarzone.getTeleport());
							} else {
								// just added a point
								
								// bring back cake
								cake.getVolume().resetBlocks();
								cake.addCakeBlocks();
								
								playerWarzone.respawnPlayer(event, playerTeam, player);
								playerTeam.resetSign();
								playerWarzone.getLobby().resetTeamGateSign(playerTeam);
							}
						}
						
						playerWarzone.removeCakeThief(player.getName());
					}
					
					return;
				}
			}
			
			// Class selection lock
			if (!playerTeam.getSpawnVolume().contains(player.getLocation()) && 
					playerWarzone.getLoadoutSelections().keySet().contains(player.getName())
					&& playerWarzone.getLoadoutSelections().get(player.getName()).isStillInSpawn()) {
				playerWarzone.getLoadoutSelections().get(player.getName()).setStillInSpawn(false);
			}
			
		} else if (locZone != null && locZone.getLobby() != null && !locZone.getLobby().isLeavingZone(playerLoc) && !isMaker) {
			// player is not in any team, but inside warzone boundaries, get him out
			Warzone zone = Warzone.getZoneByLocation(playerLoc);
			event.setTo(zone.getTeleport());
			War.war.badMsg(player, "You can't be inside a warzone without a team.");
			return;
		}
	}
	
	@EventHandler
	public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
		if (War.war.isLoaded() && event.isSneaking()) {
			Warzone playerWarzone = Warzone.getZoneByLocation(event.getPlayer());
			Team playerTeam = Team.getTeamByPlayerName(event.getPlayer().getName());
			if (playerWarzone != null && playerTeam != null && playerTeam.getInventories().resolveLoadouts().keySet().size() > 1 && playerTeam.getSpawnVolume().contains(event.getPlayer().getLocation())) {
				if (playerWarzone.getLoadoutSelections().keySet().contains(event.getPlayer().getName())
						&& playerWarzone.getLoadoutSelections().get(event.getPlayer().getName()).isStillInSpawn()) {
					LoadoutSelection selection = playerWarzone.getLoadoutSelections().get(event.getPlayer().getName());
					List<Loadout> loadouts = (List<Loadout>)new ArrayList(playerTeam.getInventories().resolveNewLoadouts()).clone();
					for (Iterator<Loadout> it = loadouts.iterator(); it.hasNext();) {
						Loadout ldt = it.next();
						if ("first".equals(ldt.getName())) {
							it.remove();
							continue;
						}
						if (ldt.requiresPermission() && !event.getPlayer().hasPermission(ldt.getPermission())) {
							it.remove();
							continue;
						}
					}
					int currentIndex = (selection.getSelectedIndex() + 1) % loadouts.size();
					selection.setSelectedIndex(currentIndex);
					
					playerWarzone.equipPlayerLoadoutSelection(event.getPlayer(), playerTeam, false, true);
				} else {
					War.war.badMsg(event.getPlayer(), "Can't change loadout after exiting the spawn.");
				}
			}
		}
	}
	
	@EventHandler
	public void onPlayerRespawn(PlayerRespawnEvent event) {
		if (War.war.isLoaded()) {
			// Anyone who died in warzones needs to go back there pronto!
			for (Warzone zone : War.war.getWarzones()) {
				if (zone.getReallyDeadFighters().contains(event.getPlayer().getName())) {
					zone.getReallyDeadFighters().remove(event.getPlayer().getName());
					for (Team team : zone.getTeams()) {
						if (team.getPlayers().contains(event.getPlayer())) {
							event.setRespawnLocation(team.getTeamSpawn());
							zone.respawnPlayer(team, event.getPlayer());
							break;
						}
					}
					
					if (zone.hasPlayerState(event.getPlayer().getName())) {
						// If not member of a team and zone has your state, then game ended while you were dead
						zone.gameEndTeleport(event.getPlayer());
					}
					
					break;
				}
			}
		}
	}

	@EventHandler
	public void onPlayerTeleport(final PlayerTeleportEvent event) {
		if (War.war.isLoaded()) {
			Warzone playerWarzone = Warzone.getZoneByPlayerName(event.getPlayer().getName());
			Team playerTeam = Team.getTeamByPlayerName(event.getPlayer().getName());
			if (playerWarzone != null) {
				if (!playerWarzone.getVolume().contains(event.getTo())) {
					// Prevent teleporting out of the warzone
					if (!playerWarzone.getWarzoneConfig().getBoolean(WarzoneConfig.REALDEATHS)) {
						War.war.badMsg(event.getPlayer(), "Use /leave (or /war leave) to exit the zone.");
					}
					if (playerWarzone.isFlagThief(event.getPlayer().getName())) {
						Team victimTeam = playerWarzone.getVictimTeamForFlagThief(event.getPlayer().getName());

						// Get event.getPlayer() back to spawn
						playerWarzone.respawnPlayer(event, playerTeam, event.getPlayer());
						playerWarzone.removeFlagThief(event.getPlayer().getName());

						// Bring back flag of victim team
						victimTeam.getFlagVolume().resetBlocks();
						victimTeam.initializeTeamFlag();

						for (Team team : playerWarzone.getTeams()) {
							team.teamcast(event.getPlayer().getName() + " dropped the " + victimTeam.getName() + " flag!");
						}
					} else if (playerWarzone.isCakeThief(event.getPlayer().getName())) {
						Cake cake = playerWarzone.getCakeForThief(event.getPlayer().getName());

						// Get event.getPlayer() back to spawn
						playerWarzone.respawnPlayer(event, playerTeam, event.getPlayer());
						playerWarzone.removeCakeThief(event.getPlayer().getName());

						// Bring back cake
						cake.getVolume().resetBlocks();
						cake.addCakeBlocks();

						for (Team team : playerWarzone.getTeams()) {
							team.teamcast(event.getPlayer().getName() + " dropped the " + cake.getName() + " cake!");
						}
					} else if (playerWarzone.isBombThief(event.getPlayer().getName())) {
						Bomb bomb = playerWarzone.getBombForThief(event.getPlayer().getName());

						// Get event.getPlayer() back to spawn
						playerWarzone.respawnPlayer(event, playerTeam, event.getPlayer());
						playerWarzone.removeBombThief(event.getPlayer().getName());

						// Bring back bomb
						bomb.getVolume().resetBlocks();
						bomb.addBombBlocks();

						for (Team team : playerWarzone.getTeams()) {
							team.teamcast(event.getPlayer().getName() + " dropped the " + bomb.getName() + " bomb!");
						}
					} else {
						// Get event.getPlayer() back to spawn
						playerWarzone.respawnPlayer(event, playerTeam, event.getPlayer());
					}
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
