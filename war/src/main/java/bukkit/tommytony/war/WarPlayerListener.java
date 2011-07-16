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
		this.random = new Random();
	}

	@Override
	public void onPlayerQuit(PlayerQuitEvent event) {
		if (this.war.isLoaded()) {
			Player player = event.getPlayer();
			Team team = this.war.getPlayerTeam(player.getName());
			if (team != null) {
				Warzone zone = this.war.getPlayerTeamWarzone(player.getName());
				if (zone != null) {
					zone.handlePlayerLeave(player, zone.getTeleport(), true);
				}
			}
			if (this.war.isWandBearer(player)) {
				this.war.removeWandBearer(player);
			}
		}
	}

	@Override
	public void onPlayerDropItem(PlayerDropItemEvent event) {
		if (this.war.isLoaded()) {
			Player player = event.getPlayer();
			Team team = this.war.getPlayerTeam(player.getName());
			if (team != null) {
				Warzone zone = this.war.getPlayerTeamWarzone(player.getName());

				if (zone.isFlagThief(player.getName())) {
					// a flag thief can't drop his flag
					this.war.badMsg(player, "Can't drop items while stealing flag. What are you doing?! Run!");
					event.setCancelled(true);

				} else {
					Item item = event.getItemDrop();
					if (item != null) {
						ItemStack itemStack = item.getItemStack();
						if (itemStack != null && itemStack.getType() == team.getKind().getMaterial() && itemStack.getData().getData() == team.getKind().getData()) {
							// Can't drop your team's kind block
							this.war.badMsg(player, "Can't drop " + team.getName() + " block blocks.");
							event.setCancelled(true);
							return;
						}

						if (zone.isNearWall(player.getLocation()) && itemStack != null) {
							this.war.badMsg(player, "Can't drop items near the zone border!");
							event.setCancelled(true);
							return;
						}
					}
				}
			}
			if (this.war.isWandBearer(player)) {
				Item item = event.getItemDrop();
				if (item.getItemStack().getType() == Material.WOOD_SWORD) {
					String zoneName = this.war.getWandBearerZone(player);
					this.war.removeWandBearer(player);
					this.war.msg(player, "You dropped the zone " + zoneName + " wand.");
				}
			}
		}
	}

	@Override
	public void onPlayerPickupItem(PlayerPickupItemEvent event) {
		if (this.war.isLoaded()) {
			Player player = event.getPlayer();
			Team team = this.war.getPlayerTeam(player.getName());
			if (team != null) {
				Warzone zone = this.war.getPlayerTeamWarzone(player.getName());

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
								// war.badMsg(player, "You already have a " + team.getName() + " block.");
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
		if (this.war.isLoaded()) {
			Player player = event.getPlayer();
			Inventory inventory = event.getInventory();
			Team team = this.war.getPlayerTeam(player.getName());
			if (team != null && inventory instanceof PlayerInventory) {
				// make sure the player doesn't have too many precious blocks
				// or illegal armor (i.e. armor not found in loadout)
				PlayerInventory playerInv = (PlayerInventory) inventory;
				ItemStack teamKindBlock = new ItemStack(team.getKind().getMaterial(), team.getKind().getData());
				if (playerInv.contains(teamKindBlock, 2)) {
					playerInv.remove(teamKindBlock);
					playerInv.addItem(teamKindBlock);
					this.war.badMsg(player, "All that " + team.getName() + " must have been heavy!");
				}
			}
		}
	}

	@Override
	public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
		if (this.war.isLoaded()) {
			Player player = event.getPlayer();
			Team talkingPlayerTeam = this.war.getPlayerTeam(player.getName());
			if (talkingPlayerTeam != null) {
				String msg = event.getMessage();
				String[] split = msg.split(" ");
				if (!this.war.isZoneMaker(player) && split.length > 0 && split[0].startsWith("/")) {
					String command = split[0].substring(1);
					if (!command.equals("war") && !command.equals("zones") && !command.equals("warzones") && !command.equals("zone") && !command.equals("warzone") && !command.equals("teams") && !command.equals("join") && !command.equals("leave") && !command.equals("team") && !command.equals("warhub") && !command.equals("zonemaker")) {
						this.war.badMsg(player, "Can't use anything but War commands (e.g. /leave, /warhub) while you're playing in a warzone.");
						event.setCancelled(true);
					}
				}
			}
		}
	}

	@Override
	public void onPlayerKick(PlayerKickEvent event) {
		if (this.war.isLoaded()) {
			Player player = event.getPlayer();
			String reason = event.getReason();
			if (reason.contains("moved") || reason.contains("too quickly") || reason.contains("Hacking")) {
				boolean inWarzone = this.war.inAnyWarzone(player.getLocation());
				boolean inLobby = this.war.inAnyWarzone(player.getLocation());
				boolean inWarhub = false;
				if (this.war.getWarHub() != null && this.war.getWarHub().getVolume().contains(player.getLocation())) {
					inWarhub = true;
				}
				if (inWarzone || inLobby || inWarhub) {
					event.setCancelled(true);
					this.war.logWarn("Prevented " + player.getName() + " from getting kicked.");
				}
			}
		}
	}

	@Override
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (this.war.isLoaded()) {
			Player player = event.getPlayer();
			if (player.getItemInHand().getType() == Material.WOOD_SWORD && this.war.isWandBearer(player)) {
				String zoneName = this.war.getWandBearerZone(player);
				ZoneSetter setter = new ZoneSetter(this.war, player, zoneName);
				if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_AIR) {
					this.war.badMsg(player, "Too far.");
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

	@Override
	public void onPlayerMove(PlayerMoveEvent event) {
		if (this.war.isLoaded()) {
			Player player = event.getPlayer();
			Location playerLoc = event.getFrom(); // same as player.getLoc. Don't call again we need same result.
			Warzone locZone = null;
			ZoneLobby locLobby = null;
			locZone = this.war.warzone(playerLoc);
			locLobby = this.war.lobby(playerLoc);
			boolean canPlay = this.war.canPlayWar(player);
			boolean isMaker = this.war.isZoneMaker(player);

			// Zone walls
			Team currentTeam = this.war.getPlayerTeam(player.getName());
			Warzone playerWarzone = this.war.getPlayerTeamWarzone(player.getName()); // this uses the teams, so it asks: get the player's team's warzone
			boolean protecting = false;
			if (currentTeam != null) {
				// Warzone nearbyZone = war.zoneOfZoneWallAtProximity(playerLoc);
				protecting = playerWarzone.protectZoneWallAgainstPlayer(player);
			} else {
				Warzone nearbyZone = this.war.zoneOfZoneWallAtProximity(playerLoc);
				if (nearbyZone != null && !isMaker) {
					protecting = nearbyZone.protectZoneWallAgainstPlayer(player);
				}
			}

			if (!protecting) {
				// zone makers still need to delete their walls
				// make sure to delete any wall guards as you leave
				for (Warzone zone : this.war.getWarzones()) {
					zone.dropZoneWallGuardIfAny(player);
				}
			}

			// Warzone lobby gates
			if (locLobby != null) {
				Warzone zone = locLobby.getZone();
				Team oldTeam = this.war.getPlayerTeam(player.getName());
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

								if (this.war.getWarHub() != null) {
									this.war.getWarHub().resetZoneSign(zone);
								}
							} else {
								event.setTo(zone.getTeleport());
								// player.teleport(zone.getTeleport());
								this.war.badMsg(player, "All teams are full.");
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
								if (this.war.getWarHub() != null) {
									this.war.getWarHub().resetZoneSign(zone);
								}
								zone.keepPlayerInventory(player);
								this.war.msg(player, "Your inventory is in storage until you /leave.");
								zone.respawnPlayer(event, team, player);
								for (Team t : zone.getTeams()) {
									t.teamcast("" + player.getName() + " joined team " + team.getName() + ".");
								}
							} else {
								event.setTo(zone.getTeleport());
								this.war.badMsg(player, "Team " + team.getName() + " is full.");
							}
							return;
						}
					}

					if (this.war.getWarHub() != null && zone.getLobby().isInWarHubLinkGate(playerLoc) && !this.war.getWarHub().getVolume().contains(player.getLocation())) {
						this.dropFromOldTeamIfAny(player);
						event.setTo(this.war.getWarHub().getLocation());
						// player.teleport(war.getWarHub().getLocation());
						this.war.msg(player, "Welcome to the War hub.");
						return;
					}
				}

			}

			// Warhub zone gates
			WarHub hub = this.war.getWarHub();
			if (hub != null && hub.getVolume().contains(player.getLocation())) {
				Warzone zone = hub.getDestinationWarzoneForLocation(playerLoc);
				if (zone != null && zone.getTeleport() != null) {
					event.setTo(zone.getTeleport());
					// player.teleport(zone.getTeleport());
					this.war.msg(player, "Welcome to warzone " + zone.getName() + ".");
					return;
				}
			}

			boolean isLeaving = playerWarzone != null && playerWarzone.getLobby().isLeavingZone(playerLoc);
			Team playerTeam = this.war.getPlayerTeam(player.getName());
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
					this.war.badMsg(player, "Use /leave to exit the zone.");
					event.setTo(playerTeam.getTeamSpawn());
					return;
				}

				// Monuments
				if (playerTeam != null && playerWarzone.nearAnyOwnedMonument(playerLoc, playerTeam) && player.getHealth() < 20 && player.getHealth() > 0 // don't heal the dead
						&& this.random.nextInt(77) == 3) { // one chance out of many of getting healed
					int currentHp = player.getHealth();
					int newHp = currentHp + locZone.getMonumentHeal();
					if (newHp > 20) {
						newHp = 20;
					}
					player.setHealth(newHp);
					String isS = "s"; // no 's' in 'hearts' when it's just one heart
					if (newHp - currentHp == 2) {
						isS = "";
					}
					String heartNum = ""; // since (newHp-currentHp)/2 won't give the right amount
					if (newHp - currentHp == 2) {
						heartNum = "1 ";
					} else if (newHp - currentHp % 2 == 0) {
						heartNum = ((newHp - currentHp) / 2) + " ";
					} else {
						heartNum = ((newHp - currentHp - 1) / 2) + ".5 ";
					}
					this.war.msg(player, "Your dance pleases the monument's voodoo. You gain " + heartNum + "heart" + isS + "!");
					return;
				}

				// Flag capture
				if (playerWarzone.isFlagThief(player.getName()) && (playerTeam.getSpawnVolume().contains(player.getLocation()) || (playerTeam.getFlagVolume() != null && playerTeam.getFlagVolume().contains(player.getLocation())))) {
					if (playerWarzone.isTeamFlagStolen(playerTeam)) {
						this.war.badMsg(player, "You can't capture the enemy flag until your team's flag is returned.");
					} else {
						synchronized (playerWarzone) {
							// flags can be captured at own spawn or own flag pole
							playerTeam.addPoint();
							if (playerTeam.getPoints() >= playerWarzone.getScoreCap()) {
								if (playerWarzone.hasPlayerInventory(player.getName())) {
									playerWarzone.restorePlayerInventory(player);
								}
								playerWarzone.handleScoreCapReached(player, playerTeam.getName());
								event.setTo(playerWarzone.getTeleport());
								// player.teleport(playerWarzone.getTeleport());
							} else {
								// added a point
								Team victim = playerWarzone.getVictimTeamForThief(player.getName());
								victim.getFlagVolume().resetBlocks(); // bring back flag to team that lost it
								victim.initializeTeamFlag();
								for (Team t : playerWarzone.getTeams()) {
									t.teamcast(player.getName() + " captured team " + victim.getName() + "'s flag. Team " + playerTeam.getName() + " scores one point.");
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
			} else if (locZone != null && locZone.getLobby() != null && !locZone.getLobby().isLeavingZone(playerLoc) && !isMaker) {
				// player is not in any team, but inside warzone boundaries, get him out
				Warzone zone = this.war.warzone(playerLoc);
				event.setTo(zone.getTeleport());
				// player.teleport(zone.getTeleport());
				this.war.badMsg(player, "You can't be inside a warzone without a team.");
				return;
			}
		}
	}

	private void handleDisabledZone(PlayerMoveEvent event, Player player, Warzone zone) {
		if (zone.getLobby() != null) {
			this.war.badMsg(player, "This warzone is disabled.");
			event.setTo(zone.getTeleport());
		}
	}

	private void dropFromOldTeamIfAny(Player player) {
		// drop from old team if any
		Team previousTeam = this.war.getPlayerTeam(player.getName());
		if (previousTeam != null) {
			if (!previousTeam.removePlayer(player.getName())) {
				this.war.logWarn("Could not remove player " + player.getName() + " from team " + previousTeam.getName());
			}
		}
	}

	public String getAllTeamsMsg(Player player) {
		String teamsMessage = "Teams: ";
		Warzone warzone = this.war.warzone(player.getLocation());
		ZoneLobby lobby = this.war.lobby(player.getLocation());
		if (warzone == null && lobby != null) {
			warzone = lobby.getZone();
		} else {
			lobby = warzone.getLobby();
		}
		if (warzone.getTeams().isEmpty()) {
			teamsMessage += "none.";
		}
		for (Team team : warzone.getTeams()) {
			teamsMessage += team.getName() + " (" + team.getPoints() + " points, " + team.getRemainingLifes() + "/" + warzone.getLifePool() + " lives left. ";
			for (Player member : team.getPlayers()) {
				teamsMessage += member.getName() + " ";
			}
			teamsMessage += ")  ";
		}
		return teamsMessage;
	}
}
