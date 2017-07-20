package com.tommytony.war.event;

import java.util.HashMap;
import java.util.List;

import com.tommytony.war.config.WarConfig;

import org.apache.commons.lang.Validate;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.getspout.spoutapi.SpoutManager;
import org.getspout.spoutapi.player.SpoutPlayer;

import com.tommytony.war.Team;
import com.tommytony.war.War;
import com.tommytony.war.Warzone;
import com.tommytony.war.Warzone.LeaveCause;
import com.tommytony.war.command.ZoneSetter;
import com.tommytony.war.config.FlagReturn;
import com.tommytony.war.config.TeamConfig;
import com.tommytony.war.config.WarzoneConfig;
import com.tommytony.war.spout.SpoutDisplayer;
import com.tommytony.war.structure.Bomb;
import com.tommytony.war.structure.Cake;
import com.tommytony.war.structure.WarHub;
import com.tommytony.war.structure.ZoneLobby;
import com.tommytony.war.utility.Direction;
import com.tommytony.war.utility.Loadout;
import com.tommytony.war.utility.LoadoutSelection;
import com.tommytony.war.volume.Volume;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;

/**
 * @author tommytony, Tim Düsterhus
 */
public class WarPlayerListener implements Listener {
	private java.util.Random random = new java.util.Random();
	private HashMap<String, Location> latestLocations = new HashMap<String, Location>(); 

	/**
	 * Correctly removes quitting players from warzones
	 *
	 * @see PlayerQuitEvent
	 */
	@EventHandler
	public void onPlayerQuit(final PlayerQuitEvent event) {
		if (War.war.isLoaded()) {
			Player player = event.getPlayer();
			Warzone zone = Warzone.getZoneByPlayerName(player.getName());
			if (zone != null) {
				zone.handlePlayerLeave(player, zone.getEndTeleport(LeaveCause.DISCONNECT), true);
			}

			if (War.war.isWandBearer(player)) {
				War.war.removeWandBearer(player);
			}
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onPlayerJoin(final PlayerJoinEvent event) {
		String autojoinName = War.war.getWarConfig().getString(WarConfig.AUTOJOIN);
		boolean autojoinEnabled = !autojoinName.isEmpty();
		if (autojoinEnabled) { // Won't be able to find warzone if unset
			Warzone autojoinWarzone = Warzone.getZoneByNameExact(autojoinName);
			if (autojoinWarzone == null) {
				War.war.getLogger().log(Level.WARNING, "Failed to find autojoin warzone ''{0}''.", new Object[] {autojoinName});
				return;
			}
			if (autojoinWarzone.getWarzoneConfig().getBoolean(WarzoneConfig.DISABLED) || autojoinWarzone.isReinitializing()) {
				War.war.badMsg(event.getPlayer(), "join.disabled");
				event.getPlayer().teleport(autojoinWarzone.getTeleport());
			} else if (!autojoinWarzone.getWarzoneConfig().getBoolean(WarzoneConfig.JOINMIDBATTLE) && autojoinWarzone.isEnoughPlayers()) {
				War.war.badMsg(event.getPlayer(), "join.progress");
				event.getPlayer().teleport(autojoinWarzone.getTeleport());
			} else if (autojoinWarzone.isFull()) {
				War.war.badMsg(event.getPlayer(), "join.full.all");
				event.getPlayer().teleport(autojoinWarzone.getTeleport());
			} else if (autojoinWarzone.isFull(event.getPlayer())) {
				War.war.badMsg(event.getPlayer(), "join.permission.all");
				event.getPlayer().teleport(autojoinWarzone.getTeleport());
			} else { // Player will only ever be autoassigned to a team
				autojoinWarzone.autoAssign(event.getPlayer());
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

				if (zone.isFlagThief(player)) {
					// a flag thief can't drop his flag
					War.war.badMsg(player, "drop.flag.disabled");
					event.setCancelled(true);
				} else if (zone.isBombThief(player)) {
					// a bomb thief can't drop his bomb
					War.war.badMsg(player, "drop.bomb.disabled");
					event.setCancelled(true);
				} else if (zone.isCakeThief(player)) {
					// a cake thief can't drop his cake
					War.war.badMsg(player, "drop.cake.disabled");
					event.setCancelled(true);
				} else if (zone.getWarzoneConfig().getBoolean(WarzoneConfig.NODROPS)) {
					War.war.badMsg(player, "drop.item.disabled");
					event.setCancelled(true);
				} else {
					Item item = event.getItemDrop();
					if (item != null) {
						ItemStack itemStack = item.getItemStack();
						if (itemStack != null && team.getKind().isTeamItem(itemStack)) {
							// Can't drop your team's kind block
							War.war.badMsg(player, "drop.team", team.getName());
							event.setCancelled(true);
							return;
						}

						if (zone.isNearWall(player.getLocation()) && itemStack != null
								&& !team.getTeamConfig().resolveBoolean(TeamConfig.BORDERDROP)) {
							War.war.badMsg(player, "drop.item.border");
							event.setCancelled(true);
							return;
						}
						
						if (zone.getLoadoutSelections().keySet().contains(player.getName())
								&& zone.getLoadoutSelections().get(player.getName()).isStillInSpawn()) {
							// still at spawn
							War.war.badMsg(player, "drop.item.spawn");
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
					War.war.msg(player, "drop.wand", zoneName);
				}
			}
		}
	}

	private static final int MINIMUM_TEAM_BLOCKS = 1;
	@EventHandler
	public void onPlayerPickupItem(final PlayerPickupItemEvent event) {
		if (War.war.isLoaded()) {
			Player player = event.getPlayer();
			Team team = Team.getTeamByPlayerName(player.getName());
			if (team != null) {
				Warzone zone = Warzone.getZoneByPlayerName(player.getName());

				if (zone.isFlagThief(player)) {
					// a flag thief can't pick up anything
					event.setCancelled(true);
				} else {
					Item item = event.getItem();
					if (item != null) {
						ItemStack itemStack = item.getItemStack();
						if (itemStack != null && team.getKind().isTeamItem(itemStack) &&
								player.getInventory().containsAtLeast(team.getKind().getBlockHead(), MINIMUM_TEAM_BLOCKS)) {
							// Can't pick up a second precious block
							event.setCancelled(true);
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

						War.war.badMsg(player, "command.disabled");
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
				warzone.handlePlayerLeave(player, warzone.getEndTeleport(LeaveCause.DISCONNECT), true);
			}
		}
	}

	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (War.war.isLoaded()) {
			Player player = event.getPlayer();
			if (event.getItem() != null && event.getItem().getType() == Material.WOOD_SWORD && War.war.isWandBearer(player)) {
				String zoneName = War.war.getWandBearerZone(player);
				ZoneSetter setter = new ZoneSetter(player, zoneName);
				if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_AIR) {
					War.war.badMsg(player, "wand.toofar");
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
				event.setCancelled(true);
				// Replace message with sound to reduce spamminess.
				// Whenever a player dies in the middle of conflict they will
				// likely respawn still trying to use their items to attack
				// another player.
				player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1, 0);
			} 

			if (zone != null && event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock().getType() == Material.ENDER_CHEST && !zone.getWarzoneConfig().getBoolean(WarzoneConfig.ALLOWENDER)) {
				event.setCancelled(true);
				War.war.badMsg(player, "use.ender");
			}
			Team team = Team.getTeamByPlayerName(player.getName());
			if (zone != null && team != null && event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock().getType() == Material.ENCHANTMENT_TABLE && team.getTeamConfig().resolveBoolean(TeamConfig.XPKILLMETER)) {
				event.setCancelled(true);
				War.war.badMsg(player, "use.enchant");
				if (zone.getAuthors().contains(player.getName())) {
					War.war.badMsg(player, "use.xpkillmeter");
				}
			}
			if (zone != null && team != null && event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock().getType() == Material.ANVIL && team.getTeamConfig().resolveBoolean(TeamConfig.XPKILLMETER)) {
				event.setCancelled(true);
				War.war.badMsg(player, "use.anvil");
				if (zone.getAuthors().contains(player.getName())) {
					War.war.badMsg(player, "use.xpkillmeter");
				}
			}
			if (zone != null && team != null && event.getAction() == Action.RIGHT_CLICK_BLOCK
					&& event.getClickedBlock().getState() instanceof InventoryHolder
					&& zone.isFlagThief(player)) {
				event.setCancelled(true);
				War.war.badMsg(player, "drop.flag.disabled");
			}
			if (zone == null && event.getAction() == Action.RIGHT_CLICK_BLOCK
					&& (event.getClickedBlock().getType() == Material.CHEST || event.getClickedBlock().getType() == Material.TRAPPED_CHEST)
					&& Warzone.getZoneByLocation(event.getClickedBlock().getLocation()) != null
					&& !War.war.isZoneMaker(event.getPlayer())) {
				// prevent opening chests inside a warzone if a player is not a zone maker
				event.setCancelled(true);
				player.playSound(player.getLocation(), Sound.BLOCK_CHEST_LOCKED, 1, 0);
			}
		}
		     
		if (event.getAction() == Action.RIGHT_CLICK_BLOCK
				|| event.getAction() == Action.RIGHT_CLICK_AIR) {
			Player player = event.getPlayer();
			Warzone zone = Warzone.getZoneByPlayerName(player.getName());
			if (zone != null && zone.getWarzoneConfig().getBoolean(WarzoneConfig.SOUPHEALING)) {
				ItemStack item = event.getItem();
				if ((item != null) && (item.getType() == Material.MUSHROOM_SOUP)) {
					if (player.getHealth() < 20) {
						player.setHealth(Math.min(20, player.getHealth() + 7));
						item.setType(Material.BOWL);
					} else if (player.getFoodLevel() < 20) {
						player.setFoodLevel(Math.min(20, player.getFoodLevel() + 6));
						player.setSaturation(player.getSaturation() + 7.2f);
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
		
		// Signs can automatically teleport you to specific or random warzones
		if (playerLoc.getBlock().getType() == Material.SIGN_POST) {
			Sign sign = (Sign) playerLoc.getBlock().getState();
			if (sign.getLine(0).equals("[zone]")) {
				Warzone indicated = Warzone.getZoneByName(sign.getLine(1));
				if (indicated != null) {
					player.teleport(indicated.getTeleport());
				} else if (sign.getLine(1).equalsIgnoreCase("$random")) {
					List<Warzone> warzones = War.war.getEnabledWarzones();
					if (warzones.size() == 0) return;
					int zone = random.nextInt(warzones.size());
					Warzone random = warzones.get(zone);
					player.teleport(random.getTeleport());
				} else if (sign.getLine(1).equalsIgnoreCase("$active")) {
					List<Warzone> warzones = War.war.getActiveWarzones();
					if (warzones.size() == 0) warzones = War.war.getEnabledWarzones();
					if (warzones.size() == 0) return;
					int zone = random.nextInt(warzones.size());
					Warzone random = warzones.get(zone);
					player.teleport(random.getTeleport());
				}
			}
		}
		
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
		if (locLobby != null && currentTeam == null && locLobby.isInAnyGate(playerLoc)) {
			Warzone zone = locLobby.getZone();
			Team locTeamGate = locLobby.getTeamGate(playerLoc);
			if (zone.getWarzoneConfig().getBoolean(WarzoneConfig.DISABLED) || zone.isReinitializing()) {
				War.war.badMsg(player, "join.disabled");
				event.setTo(zone.getTeleport());
			} else if (!zone.getWarzoneConfig().getBoolean(WarzoneConfig.JOINMIDBATTLE) && zone.isEnoughPlayers()) {
				War.war.badMsg(player, "join.progress");
				event.setTo(zone.getTeleport());
			} else if (zone.isFull()) {
				War.war.badMsg(player, "join.full.all");
				event.setTo(zone.getTeleport());
			} else if (zone.isFull(player)) {
				War.war.badMsg(player, "join.permission.all");
				event.setTo(zone.getTeleport());
			} else if (locTeamGate != null && locTeamGate.isFull()) {
				War.war.badMsg(player, "join.full.single", locTeamGate.getName());
				event.setTo(zone.getTeleport());
			} else if (locTeamGate != null && !War.war.canPlayWar(player, locTeamGate)) {
				War.war.badMsg(player, "join.permission.single", locTeamGate.getName());
				event.setTo(zone.getTeleport());
			} else if (zone.getLobby().isAutoAssignGate(playerLoc)) {
				zone.autoAssign(player);
			} else if (locTeamGate != null) {
				zone.assign(player, locTeamGate);
			}
			return;
		} else if (locLobby != null && currentTeam == null
				&& locLobby.isInWarHubLinkGate(playerLoc)
				&& War.war.getWarHub() != null) {
			War.war.msg(player, "warhub.teleport");
			event.setTo(War.war.getWarHub().getLocation());
			return;
		}

		// Warhub zone gates
		WarHub hub = War.war.getWarHub();
		if (hub != null && hub.getVolume().contains(player.getLocation())) {
			Warzone zone = hub.getDestinationWarzoneForLocation(playerLoc);
			if (zone != null && zone.getTeleport() != null) {
				if (zone.getWarzoneConfig().getBoolean(WarzoneConfig.AUTOJOIN)
						&& zone.getTeams().size() >= 1 && currentTeam == null) {
					if (zone.getWarzoneConfig().getBoolean(WarzoneConfig.DISABLED) || zone.isReinitializing()) {
						War.war.badMsg(player, "join.disabled");
						event.setTo(hub.getLocation());
					} else if (!zone.getWarzoneConfig().getBoolean(WarzoneConfig.JOINMIDBATTLE) && zone.isEnoughPlayers()) {
						War.war.badMsg(player, "join.progress");
						event.setTo(hub.getLocation());
					} else if (zone.isFull()) {
						War.war.badMsg(player, "join.full.all");
						event.setTo(hub.getLocation());
					} else if (zone.isFull(player)) {
						War.war.badMsg(player, "join.permission.all");
						event.setTo(hub.getLocation());
					} else {
						zone.autoAssign(player);
					}
					return;
				}
				event.setTo(zone.getTeleport());
				War.war.msg(player, "zone.teleport", zone.getName());
				return;
			}
		}

		boolean isLeaving = playerWarzone != null && playerWarzone.getLobby().isLeavingZone(playerLoc);
		Team playerTeam = Team.getTeamByPlayerName(player.getName());
		if (isLeaving) { // already in a team and in warzone, leaving
			// same as leave
			if (playerTeam != null) {
				boolean atSpawnAlready = playerTeam.isSpawnLocation(playerLoc);
				if (!atSpawnAlready) {
					playerWarzone.handlePlayerLeave(player, playerWarzone.getWarzoneConfig().getBoolean(WarzoneConfig.AUTOJOIN) ?
							War.war.getWarHub().getLocation() : playerWarzone.getTeleport(), event, true);
					return;
				}
				return;
			}
		}

		if (playerWarzone != null) {
			// Player belongs to a warzone team but is outside: he snuck out or is at spawn and died
			if (locZone == null && playerTeam != null && playerWarzone.getLobby() != null && !playerWarzone.getLobby().getVolume().contains(playerLoc) && !isLeaving) {
				List<BlockFace> nearestWalls = playerWarzone.getNearestWalls(playerLoc);
				if (!playerWarzone.getWarzoneConfig().getBoolean(WarzoneConfig.REALDEATHS)) {
					War.war.badMsg(player, "zone.leavenotice");
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
						// fell off the map, back to spawn (still need to drop objects)
						playerWarzone.dropAllStolenObjects(event.getPlayer(), false);
						playerWarzone.respawnPlayer(event, playerTeam, event.getPlayer());
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
				} else {
					playerWarzone.dropAllStolenObjects(event.getPlayer(), false);
					playerWarzone.respawnPlayer(event, playerTeam, event.getPlayer());
					return;
				}
			}
						
			LoadoutSelection loadoutSelectionState = playerWarzone.getLoadoutSelections().get(player.getName());
			FlagReturn flagReturn = playerTeam.getTeamConfig().resolveFlagReturn();
			if (!playerTeam.isSpawnLocation(playerLoc)) {
				if (!playerWarzone.isEnoughPlayers() && loadoutSelectionState != null && loadoutSelectionState.isStillInSpawn()) {
					// Be sure to keep only players that just respawned locked inside the spawn for minplayer/minteams restrictions - otherwise 
					// this will conflict with the can't-renter-spawn bump just a few lines below  
					War.war.badMsg(player, "zone.spawn.minplayers", playerWarzone.getWarzoneConfig().getInt(WarzoneConfig.MINPLAYERS),
							playerWarzone.getWarzoneConfig().getInt(WarzoneConfig.MINTEAMS));
					event.setTo(playerTeam.getRandomSpawn());
					return;
				}
				if (playerWarzone.isRespawning(player)) {
					int rt = playerTeam.getTeamConfig().resolveInt(TeamConfig.RESPAWNTIMER);
					War.war.badMsg(player, "zone.spawn.timer", rt);
					event.setTo(playerTeam.getRandomSpawn());
					return;
				}
				if (playerWarzone.isReinitializing()) {
					// don't let players wander about outside spawns during reset
					// (they could mess up the blocks that have already been reset
					// before the start of the new battle)
					War.war.msg(player, "zone.battle.reset");
					event.setTo(playerTeam.getRandomSpawn());
					return;
				}
			} else if (loadoutSelectionState != null && !loadoutSelectionState.isStillInSpawn()
					&& !playerWarzone.isCakeThief(player)
					&& (flagReturn.equals(FlagReturn.BOTH) || flagReturn.equals(FlagReturn.SPAWN)) 
					&& !playerWarzone.isFlagThief(player)) {
				
				// player is in spawn, but has left already: he should NOT be let back in - kick him out gently
				// if he sticks around too long.
				// (also, be sure you aren't preventing the flag or cake from being captured)
//				if (!CantReEnterSpawnJob.getPlayersUnderSuspicion().contains(player.getName())) {
//					CantReEnterSpawnJob job = new CantReEnterSpawnJob(player, playerTeam);
//					War.war.getServer().getScheduler().scheduleSyncDelayedTask(War.war, job, 12);
//				}
				return;
			}

			// Monuments
			if (playerTeam != null && playerWarzone.nearAnyOwnedMonument(playerLoc, playerTeam) && player.getHealth() < 20 && player.getHealth() > 0 // don't heal the dead
					&& this.random.nextInt(7) == 3) { // one chance out of many of getting healed
				int currentHp = (int) player.getHealth();
				int newHp = Math.min(20, currentHp + locZone.getWarzoneConfig().getInt(WarzoneConfig.MONUMENTHEAL));

				player.setHealth(newHp);
				double heartNum = ((double) newHp - currentHp) / 2;
				War.war.msg(player, "zone.monument.voodoo", heartNum);
				return;
			}

			// Flag capture
			if (playerWarzone.isFlagThief(player)) {
				
				// smoky
				if (System.currentTimeMillis() % 13 == 0) {
					playerWarzone.getWorld().playEffect(player.getLocation(), Effect.POTION_BREAK, playerTeam.getKind().getPotionEffectColor());
				}
				
				// Make sure game ends can't occur simultaneously. 
				// See Warzone.handleDeath() for details.
				boolean inSpawn = playerTeam.isSpawnLocation(player.getLocation());
				boolean inFlag = (playerTeam.getFlagVolume() != null && playerTeam.getFlagVolume().contains(player.getLocation()));

				if (playerTeam.getTeamConfig().resolveFlagReturn().equals(FlagReturn.BOTH)) {
					if (!inSpawn && !inFlag) {
						return;
					}
				} else if (playerTeam.getTeamConfig().resolveFlagReturn().equals(FlagReturn.SPAWN)) {
					if (inFlag) {
						War.war.badMsg(player, "zone.flagreturn.spawn");
						return;
					} else if (!inSpawn) {
						return;
					}
				} else if (playerTeam.getTeamConfig().resolveFlagReturn().equals(FlagReturn.FLAG)) {
					if (inSpawn) {
						War.war.badMsg(player, "zone.flagreturn.flag");
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
					War.war.badMsg(player, "zone.flagreturn.deadlock");
				} else {
					// flags can be captured at own spawn or own flag pole
					if (playerWarzone.isReinitializing()) {
						// Battle already ended or interrupted
						playerWarzone.respawnPlayer(event, playerTeam, player);
					} else {
						// All good - proceed with scoring
						playerTeam.addPoint();
						Team victim = playerWarzone.getVictimTeamForFlagThief(player);
						
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
							t.teamcast("zone.flagcapture.broadcast", playerTeam.getKind().getColor() + player.getName() + ChatColor.WHITE,
									victim.getName(), playerTeam.getName());
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
					
					playerWarzone.removeFlagThief(player);
					
					return;
				}
			}
			
			// Bomb detonation
			if (playerWarzone.isBombThief(player)) {
				// smoky
				playerWarzone.getWorld().playEffect(player.getLocation(), Effect.SMOKE, 0);
				
				// Make sure game ends can't occur simultaneously. 
				// Not thread safe. See Warzone.handleDeath() for details.
				boolean inEnemySpawn = false;
				Team victim = null;
				for (Team team : playerWarzone.getTeams()) {
					if (team != playerTeam 
							&& team.isSpawnLocation(player.getLocation()) 
							&& team.getPlayers().size() > 0) {
						inEnemySpawn = true;
						victim = team;
						break;
					}
				}
				
				if (inEnemySpawn && playerTeam.getPlayers().contains(player)) {
					// Made sure player is still part of team, game may have ended while waiting.
					// Ignored the scorers that happened immediately after the game end.
					Bomb bomb = playerWarzone.getBombForThief(player);
					
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
							t.teamcast("zone.bomb.broadcast", playerTeam.getKind().getColor() + player.getName() + ChatColor.WHITE,
									victim.getName(), playerTeam.getName());
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
							for (Volume spawnVolume : victim.getSpawnVolumes().values()) {
								spawnVolume.resetBlocks();
							}
							victim.initializeTeamSpawns();
							
							// bring back tnt
							bomb.getVolume().resetBlocks();
							bomb.addBombBlocks();
							
							playerWarzone.respawnPlayer(event, playerTeam, player);
							playerTeam.resetSign();
							playerWarzone.getLobby().resetTeamGateSign(playerTeam);
						}					
					}
					
					playerWarzone.removeBombThief(player);
					
					return;
				}
			}
			
			// Cake retrieval
			if (playerWarzone.isCakeThief(player)) {
				// smoky
				if (System.currentTimeMillis() % 13 == 0) {
					playerWarzone.getWorld().playEffect(player.getLocation(), Effect.POTION_BREAK, playerTeam.getKind().getPotionEffectColor());
				}
				
				// Make sure game ends can't occur simultaneously. 
				// Not thread safe. See Warzone.handleDeath() for details.
				boolean inSpawn = playerTeam.isSpawnLocation(player.getLocation());
															
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
						Cake cake = playerWarzone.getCakeForThief(player);
						
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
								t.teamcast("zone.cake.broadcast", playerTeam.getKind().getColor() + player.getName() + ChatColor.WHITE,
										ChatColor.GREEN + cake.getName() + ChatColor.WHITE, playerTeam.getName());
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
						
						playerWarzone.removeCakeThief(player);
					}
					
					return;
				}
			}
			
			// Class selection lock
			if (!playerTeam.isSpawnLocation(player.getLocation()) && 
					playerWarzone.getLoadoutSelections().keySet().contains(player.getName())
					&& playerWarzone.getLoadoutSelections().get(player.getName()).isStillInSpawn()) {
				playerWarzone.getLoadoutSelections().get(player.getName()).setStillInSpawn(false);
			}
			
		} else if (locZone != null && locZone.getLobby() != null && !locZone.getLobby().isLeavingZone(playerLoc) && !isMaker) {
			// player is not in any team, but inside warzone boundaries, get him out
			Warzone zone = Warzone.getZoneByLocation(playerLoc);
			event.setTo(zone.getTeleport());
			War.war.badMsg(player, "zone.noteamnotice");
		}
	}
	
	@EventHandler
	public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
		if (War.war.isLoaded() && event.isSneaking()) {
			Warzone playerWarzone = Warzone.getZoneByLocation(event.getPlayer());
			Team playerTeam = Team.getTeamByPlayerName(event.getPlayer().getName());
			if (playerWarzone != null && playerTeam != null && playerTeam.getInventories().resolveLoadouts().keySet().size() > 1 && playerTeam.isSpawnLocation(event.getPlayer().getLocation())) {
				if (playerWarzone.getLoadoutSelections().keySet().contains(event.getPlayer().getName())
						&& playerWarzone.getLoadoutSelections().get(event.getPlayer().getName()).isStillInSpawn()) {
					LoadoutSelection selection = playerWarzone.getLoadoutSelections().get(event.getPlayer().getName());
					List<Loadout> loadouts = new ArrayList<Loadout>(playerTeam.getInventories().resolveNewLoadouts());
					for (Iterator<Loadout> it = loadouts.iterator(); it.hasNext();) {
						Loadout ldt = it.next();
						if (ldt.getName().equals("first") ||
								(ldt.requiresPermission() && !event.getPlayer().hasPermission(ldt.getPermission()))) {
							it.remove();
						}
					}
					int currentIndex = (selection.getSelectedIndex() + 1) % loadouts.size();
					selection.setSelectedIndex(currentIndex);

					playerWarzone.equipPlayerLoadoutSelection(event.getPlayer(), playerTeam, false, true);
				} else {
					War.war.badMsg(event.getPlayer(), "zone.loadout.reenter");
				}
			}
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerRespawn(PlayerRespawnEvent event) {
		Warzone playingZone = Warzone.getZoneByPlayerName(event.getPlayer().getName());
		Warzone deadZone = Warzone.getZoneForDeadPlayer(event.getPlayer());
		if (playingZone == null && deadZone != null) {
			// Game ended while player was dead, so restore state
			deadZone.getReallyDeadFighters().remove(event.getPlayer().getName());
			if (deadZone.hasPlayerState(event.getPlayer().getName())) {
				deadZone.restorePlayerState(event.getPlayer());
			}
			event.setRespawnLocation(deadZone.getEndTeleport(LeaveCause.DISCONNECT));
			return;
		} else if (playingZone == null) {
			// Player not playing war
			return;
		} else if (deadZone == null) {
			// Player is not a 'really' dead player, nothing to do here
			return;
		}
		Team team = playingZone.getPlayerTeam(event.getPlayer().getName());
		Validate.notNull(team, String.format(
				"Failed to find a team for player %s in warzone %s on respawn.",
				event.getPlayer().getName(), playingZone.getName()));
		playingZone.getReallyDeadFighters().remove(event.getPlayer().getName());
		event.setRespawnLocation(team.getRandomSpawn());
		playingZone.respawnPlayer(team, event.getPlayer());
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
					playerWarzone.dropAllStolenObjects(event.getPlayer(), false);
					playerWarzone.respawnPlayer(event, playerTeam, event.getPlayer());
				}
			}
		}
	}
	
	@EventHandler
	public void onPlayerExpChange(PlayerExpChangeEvent event) {
		if (War.war.isLoaded()) {
			Team team = Team.getTeamByPlayerName(event.getPlayer().getName());
			if (team != null && team.getTeamConfig().resolveBoolean(TeamConfig.XPKILLMETER)) {
				event.setAmount(0);
			}
		}
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onPlayerChat(final AsyncPlayerChatEvent event) {
		Team team = Team.getTeamByPlayerName(event.getPlayer().getName());
		if (team != null && team.isInTeamChat(event.getPlayer())) {
			event.setCancelled(true);
			team.sendTeamChatMessage(event.getPlayer(), event.getMessage());
		}
	}

	@EventHandler
	public void onInventoryClick(final InventoryClickEvent event) {
		if (!(event.getWhoClicked() instanceof Player)) {
			return;
		}
		Player player = (Player) event.getWhoClicked();
		Warzone zone = Warzone.getZoneByPlayerName(player.getName());
		if (zone == null) {
			return;
		}
		if (zone.isThief(player)) {
			// Prevent thieves from taking their bomb/wool/cake into a chest, etc.
			event.setCancelled(true);
			player.playSound(player.getLocation(), Sound.BLOCK_LAVA_EXTINGUISH, 10, 10);
		} else if (event.getSlotType() == InventoryType.SlotType.ARMOR && event.getSlot() == 39
				&& zone.getWarzoneConfig().getBoolean(WarzoneConfig.BLOCKHEADS)) {
			// Magically give player a wool block when they click their helmet
			ItemStack teamBlock = zone.getPlayerTeam(player.getName()).getKind().getBlockHead();
			player.getInventory().remove(teamBlock.getType());
			// Deprecated behavior cannot be removed as it is essential to this function
			//noinspection deprecation
			event.setCursor(teamBlock);
			event.setCancelled(true);
		}
	}

	public void purgeLatestPositions() {
		this.latestLocations.clear();	
	}

	@EventHandler
	public void onBucketEmpty(PlayerBucketEmptyEvent event) {
		Player player = (Player) event.getPlayer();
		Warzone zone = Warzone.getZoneByPlayerName(player.getName());
		if (zone == null) {
			return;
		}
		if (zone.isImportantBlock(event.getBlockClicked())) {
			event.setCancelled(true);
			player.playSound(player.getLocation(), Sound.BLOCK_LAVA_EXTINGUISH, 10, 10);
		}
	}
}
