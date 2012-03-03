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
import com.tommytony.war.spout.SpoutDisplayer;
import com.tommytony.war.structure.Bomb;
import com.tommytony.war.structure.Cake;
import com.tommytony.war.structure.WarHub;
import com.tommytony.war.structure.ZoneLobby;
import com.tommytony.war.utility.LoadoutSelection;

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

	@EventHandler
	public void onPlayerKick(final PlayerKickEvent event) {
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

	@EventHandler
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
			if (zone != null && zone.getLoadoutSelections().containsKey(player.getName()) 
					&& zone.getLoadoutSelections().get(player.getName()).isStillInSpawn()) {
				event.setUseItemInHand(Result.DENY);
				ItemStack inHand = event.getItem();
				
				if (inHand != null) {
					ItemStack newItemInHand = new ItemStack(inHand.getType(), inHand.getAmount(), inHand.getDurability(), inHand.getData().getData());
					newItemInHand.setDurability(inHand.getDurability());
					event.getPlayer().setItemInHand(newItemInHand);
					event.setCancelled(true);
					
					War.war.badMsg(player, "Can't use items while still in spawn.");
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
			if (oldTeam == null && canPlay) { // trying to counter spammy player move
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
						if (zone.getWarzoneConfig().getBoolean(WarzoneConfig.DISABLED)) {
							this.handleDisabledZone(event, player, zone);
						} else if (team.getPlayers().size() < team.getTeamConfig().resolveInt(TeamConfig.TEAMSIZE)) {
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
					War.war.badMsg(player, "Can't leave spawn until there's a minimum of " + playerWarzone.getWarzoneConfig().getInt(WarzoneConfig.MINPLAYERS)
							+" player(s) on at least " + playerWarzone.getWarzoneConfig().getInt(WarzoneConfig.MINTEAMS) + " team(s).");
					event.setTo(playerTeam.getTeamSpawn());
					return;
				}
				if (playerWarzone.isRespawning(player)) {
					int rt = playerTeam.getTeamConfig().resolveInt(TeamConfig.RESPAWNTIMER);
					String isS = "s";
					if (rt==1) isS = "";
					War.war.badMsg(player, "Can't leave spawn for "+rt+" second"+isS+" after spawning!");
					event.setTo(playerTeam.getTeamSpawn());
					return;
				}
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

				if (playerWarzone.isTeamFlagStolen(playerTeam) && playerTeam.getTeamConfig().resolveBoolean(TeamConfig.FLAGMUSTBEHOME)) {
					War.war.badMsg(player, "You can't capture the enemy flag until your team's flag is returned.");
				} else {
					// flags can be captured at own spawn or own flag pole
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
						playerWarzone.handleScoreCapReached(player, playerTeam.getName());
						event.setTo(playerWarzone.getTeleport());
					} else {
						// just added a point
						victim.getFlagVolume().resetBlocks(); // bring back flag to team that lost it
						victim.initializeTeamFlag();
						
						playerWarzone.respawnPlayer(event, playerTeam, player);
						playerTeam.resetSign();
						playerWarzone.getLobby().resetTeamGateSign(playerTeam);
					}
					playerWarzone.removeFlagThief(player.getName());
				}
				
				return;
			}
			
			// Bomb detonation
			if (playerWarzone.isBombThief(player.getName())) {
				// smoky
				playerWarzone.getWorld().playEffect(player.getLocation(), Effect.SMOKE, 0);
				
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
				
				if (inEnemySpawn) {
					Bomb bomb = playerWarzone.getBombForThief(player.getName());
					
					// Boom!
					playerWarzone.getWorld().createExplosion(player.getLocation(), 2F);
					
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
						playerWarzone.handleScoreCapReached(player, playerTeam.getName());
						event.setTo(playerWarzone.getTeleport());
					} else {
						// just added a point
						
						// bring back flag to team that lost it
						victim.getSpawnVolume().resetBlocks(); 
						victim.initializeTeamSpawn();
						
						// bring back tnt
						bomb.getVolume().resetBlocks();
						bomb.addBombBlocks();
						
						playerWarzone.respawnPlayer(event, playerTeam, player);
						playerTeam.resetSign();
						playerWarzone.getLobby().resetTeamGateSign(playerTeam);
					}
					
					playerWarzone.removeBombThief(player.getName());
				}
				
				return;
			}
			
			// Cake retrieval
			if (playerWarzone.isCakeThief(player.getName())) {
				// smoky
				if (System.currentTimeMillis() % 13 == 0) {
					playerWarzone.getWorld().playEffect(player.getLocation(), Effect.POTION_BREAK, playerTeam.getKind().getPotionEffectColor());
				}
				
				boolean inSpawn = playerTeam.getSpawnVolume().contains(player.getLocation());
															
				if (inSpawn) {
					boolean hasOpponent = false; 
					for (Team t : playerWarzone.getTeams()) {
						if (t != playerTeam && t.getPlayers().size() > 0) {
							hasOpponent = true;
						}
					}
					
					// Don't let someone alone make points off cakes
					if (hasOpponent) {
						Cake cake = playerWarzone.getCakeForThief(player.getName());
						
						// Woot!
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
							playerWarzone.handleScoreCapReached(player, playerTeam.getName());
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
						
						playerWarzone.removeCakeThief(player.getName());
					}
				}
				
				return;
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
					int currentIndex = (selection.getSelectedIndex() + 1) % (playerTeam.getInventories().resolveLoadouts().keySet().size());
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
					break;
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
