package com.tommytony.war.event;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.text.MessageFormat;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import com.tommytony.war.Team;
import com.tommytony.war.War;
import com.tommytony.war.Warzone;
import com.tommytony.war.config.TeamConfig;
import com.tommytony.war.config.WarConfig;
import com.tommytony.war.config.WarzoneConfig;
import com.tommytony.war.job.DeferredBlockResetsJob;
import com.tommytony.war.structure.Bomb;
import com.tommytony.war.utility.LoadoutSelection;

/**
 * Handles Entity-Events
 *
 * @author tommytony, Tim Düsterhus
 * @package com.tommytony.war.event
 */
public class WarEntityListener implements Listener {

	/**
	 * Handles PVP-Damage
	 *
	 * @param event
	 *                fired event
	 */
	private void handlerAttackDefend(EntityDamageByEntityEvent event) {
		Entity attacker = event.getDamager();
		Entity defender = event.getEntity();

		// Maybe an arrow was thrown
		if (attacker != null && event.getDamager() instanceof Projectile && ((Projectile)event.getDamager()).getShooter() instanceof Player){
			attacker = ((Player)((Projectile)event.getDamager()).getShooter());
		}

		if (attacker != null && defender != null && attacker instanceof Player && defender instanceof Player) {
			// only let adversaries (same warzone, different team) attack each other
			Player a = (Player) attacker;
			Player d = (Player) defender;
			Warzone attackerWarzone = Warzone.getZoneByPlayerName(a.getName());
			Team attackerTeam = Team.getTeamByPlayerName(a.getName());
			Warzone defenderWarzone = Warzone.getZoneByPlayerName(d.getName());
			Team defenderTeam = Team.getTeamByPlayerName(d.getName());

			if ((attackerTeam != null && defenderTeam != null && attackerTeam != defenderTeam && attackerWarzone == defenderWarzone)
					|| (attackerTeam != null && defenderTeam != null && attacker.getEntityId() == defender.getEntityId())) {

				LoadoutSelection defenderLoadoutState = defenderWarzone.getLoadoutSelections().get(d.getName());
				if (defenderLoadoutState != null && defenderLoadoutState.isStillInSpawn()) {
					War.war.badMsg(a, "pvp.target.spawn");
					event.setCancelled(true);
					return;
				}

				LoadoutSelection attackerLoadoutState = attackerWarzone.getLoadoutSelections().get(a.getName());
				if (attackerLoadoutState != null && attackerLoadoutState.isStillInSpawn()) {
					War.war.badMsg(a, "pvp.self.spawn");
					event.setCancelled(true);
					return;
				}

				// Make sure none of them are locked in by respawn timer
				if (defenderWarzone.isRespawning(d)) {
					War.war.badMsg(a, "pvp.target.respawn");
					event.setCancelled(true);
					return;
				} else if (attackerWarzone.isRespawning(a)) {
					War.war.badMsg(a, "pvp.self.respawn");
					event.setCancelled(true);
					return;
				} 
				
				if(!defenderWarzone.getPvpReady()) {
					//if the timer is still tickin we gotta handle defense! (there be notchz in virgina) 
					event.setCancelled(true);
					return;
				}

				if (!attackerWarzone.getWarzoneConfig().getBoolean(WarzoneConfig.PVPINZONE)) {
					// spleef-like, non-pvp, zone
					event.setCancelled(true);
					return;
				}

				// Detect death, prevent it and respawn the player
				if (event.getDamage() >= d.getHealth()) {
					if (defenderWarzone.getReallyDeadFighters().contains(d.getName())) {
						// don't re-kill a dead person
						return;
					}
					WarPlayerDeathEvent event1 = new WarPlayerDeathEvent(defenderWarzone, d, a, event.getCause());
					War.war.getServer().getPluginManager().callEvent(event1);
					if (!defenderWarzone.getWarzoneConfig().getBoolean(WarzoneConfig.REALDEATHS)) {
						// fast respawn, don't really die
						event.setCancelled(true);
					}
					if (d == a) {
						defenderWarzone.handleSuicide(d);
					} else {
						defenderWarzone.handleKill(a, d, event.getDamager());
					}
				} else if (defenderWarzone.isBombThief(d) && d.getLocation().distance(a.getLocation()) < 2) {
					// Close combat, close enough to detonate
					Bomb bomb = defenderWarzone.getBombForThief(d);

					// Kill the bomber
					WarPlayerDeathEvent event1 = new WarPlayerDeathEvent(defenderWarzone, d, null, event.getCause());
					War.war.getServer().getPluginManager().callEvent(event1);
					defenderWarzone.handleDeath(d);

					if (defenderWarzone.getWarzoneConfig().getBoolean(WarzoneConfig.REALDEATHS)) {
						// and respawn him and remove from deadmen (cause realdeath + handleDeath means no respawn and getting queued up for onPlayerRespawn)
						defenderWarzone.getReallyDeadFighters().remove(d.getName());
						defenderWarzone.respawnPlayer(defenderTeam, d);
					}

					// Blow up bomb
					if (!defenderWarzone.getWarzoneConfig().getBoolean(WarzoneConfig.UNBREAKABLE)) {
						defenderWarzone.getWorld().createExplosion(a.getLocation(), 2F);
					}

					// bring back tnt
					bomb.getVolume().resetBlocks();
					bomb.addBombBlocks();

					// Notify everyone
					for (Team t : defenderWarzone.getTeams()) {
						t.sendAchievement(attackerTeam.getKind().getColor() + a.getName() + ChatColor.YELLOW + " made ",
								defenderTeam.getKind().getColor() + d.getName() + ChatColor.YELLOW + " blow up!", new ItemStack(Material.TNT), 10000);
						t.teamcast("pvp.kill.bomb", attackerTeam.getKind().getColor() + a.getName() + ChatColor.WHITE,
								defenderTeam.getKind().getColor() + d.getName() + ChatColor.WHITE);
					}
				}
			} else if (attackerTeam != null && defenderTeam != null && attackerTeam == defenderTeam && attackerWarzone == defenderWarzone && attacker.getEntityId() != defender.getEntityId()) {
				// same team, but not same person
				if (attackerWarzone.getWarzoneConfig().getBoolean(WarzoneConfig.FRIENDLYFIRE)) {
					War.war.badMsg(a, "pvp.ff.enabled"); // if ff is on, let the attack go through
				} else {
					War.war.badMsg(a, "pvp.ff.disabled");
					event.setCancelled(true); // ff is off
				}
			} else if (attackerTeam == null && defenderTeam == null && War.war.canPvpOutsideZones(a)) {
				// let normal PVP through is its not turned off or if you have perms
			} else if (attackerTeam == null && defenderTeam == null && !War.war.canPvpOutsideZones(a)) {
				if (!War.war.getWarConfig().getBoolean(WarConfig.DISABLEPVPMESSAGE)) {
					War.war.badMsg(a, "pvp.outside.permission");
				}

				event.setCancelled(true); // global pvp is off
			} else {
				if (attackerTeam == null) {
					War.war.badMsg(a, "pvp.self.notplaying");
				} else if (defenderTeam == null) {
					War.war.badMsg(a, "pvp.target.notplaying");
				} else if (attacker != null && defender != null && attacker.getEntityId() == defender.getEntityId()) {
					// You just hit yourself, probably with a bouncing arrow
				} else if (attackerTeam == defenderTeam) {
					War.war.badMsg(a, "pvp.ff.disabled");
				} else if (attackerWarzone != defenderWarzone) {
					War.war.badMsg(a, "pvp.target.otherzone");
				}

				event.setCancelled(true); // can't attack someone inside a warzone if you're not in a team
			}
		} else if (defender instanceof Player) {
			// attacked by dispenser arrow most probably
			// Detect death, prevent it and respawn the player
			Player d = (Player) defender;
			Warzone defenderWarzone = Warzone.getZoneByPlayerName(d.getName());
			if (d != null && defenderWarzone != null && event.getDamage() >= d.getHealth()) {
				LoadoutSelection defenderLoadoutState = defenderWarzone.getLoadoutSelections().get(d.getName());
				if (defenderLoadoutState != null && defenderLoadoutState.isStillInSpawn()) {
					event.setCancelled(true);
					return;
				}
				if (defenderWarzone.getReallyDeadFighters().contains(d.getName())) {
					// don't re-kill a dead person
					return;
				}

				WarPlayerDeathEvent event1 = new WarPlayerDeathEvent(defenderWarzone, d, null, event.getCause());
				War.war.getServer().getPluginManager().callEvent(event1);
				if (!defenderWarzone.getWarzoneConfig().getBoolean(WarzoneConfig.REALDEATHS)) {
					// fast respawn, don't really die
					event.setCancelled(true);
				}
				defenderWarzone.handleNaturalKill(d, event);
			}
		}
	}

	/**
	 * Protects important structures from explosions
	 *
	 * @see EntityListener.onEntityExplode()
	 */
	@EventHandler
	public void onEntityExplode(final EntityExplodeEvent event) {
		if (!War.war.isLoaded()) {
			return;
		}
		// protect zones elements, lobbies and warhub from creepers and tnt
		List<Block> explodedBlocks = event.blockList();
		List<Block> dontExplode = new ArrayList<Block>();

		boolean explosionInAWarzone = event.getEntity() != null && Warzone.getZoneByLocation(event.getEntity().getLocation()) != null;

		if (!explosionInAWarzone && War.war.getWarConfig().getBoolean(WarConfig.TNTINZONESONLY) && event.getEntity() instanceof TNTPrimed) {
			// if tntinzonesonly:true, no tnt blows up outside zones
			event.setCancelled(true);
			return;
		}

		for (Block block : explodedBlocks) {
			if (block.getType() == Material.TNT) {
				continue; // don't restore TNT (failed to track down regression cause)
			}
			if (War.war.getWarHub() != null && War.war.getWarHub().getVolume().contains(block)) {
				dontExplode.add(block);
			} else {
				boolean inOneZone = false;
				for (Warzone zone : War.war.getWarzones()) {
					if (zone.isImportantBlock(block)) {
						dontExplode.add(block);
						if (zone.isBombBlock(block)) {
							// tnt doesn't get reset like normal blocks, gotta schedule a later reset just for the Bomb
							// structure's tnt block
							DeferredBlockResetsJob job = new DeferredBlockResetsJob();
							BlockState tnt = block.getState();
							tnt.setType(Material.TNT);
							job.add(tnt);
							War.war.getServer().getScheduler().scheduleSyncDelayedTask(War.war, job, 10);
						}
						inOneZone = true;
						break;
					} else if (zone.getLobby() != null && zone.getLobby().getVolume().contains(block)) {
						dontExplode.add(block);
						inOneZone = true;
						break;
					} else if (zone.getVolume().contains(block)) {
						inOneZone = true;
					}
				}

				if (!inOneZone && explosionInAWarzone) {
					// if the explosion originated in warzone, always rollback
					dontExplode.add(block);
				}
			}
		}

		int dontExplodeSize = dontExplode.size();
		if (dontExplode.size() > 0) {
			// Reset the exploded blocks that shouldn't have exploded (some of these are zone artifacts, if rollbackexplosion some may be outside-of-zone blocks
			DeferredBlockResetsJob job = new DeferredBlockResetsJob();
			for (Block dont : dontExplode) {
				job.add(dont.getState());
			}
			War.war.getServer().getScheduler().scheduleSyncDelayedTask(War.war, job);

			// Changed explosion yield following proportion of explosion prevention (makes drops less buggy too)
			int explodedSize = explodedBlocks.size();
			float middleYeild = (float)(explodedSize - dontExplodeSize) / (float)explodedSize;
			float newYeild = middleYeild * event.getYield();

			event.setYield(newYeild);
		}
	}

	/**
	 * Handles damage on Players
	 *
	 * @see EntityListener.onEntityDamage()
	 */
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onEntityDamage(final EntityDamageEvent event) {


		if (!War.war.isLoaded()) {
			return;
		}

		Entity entity = event.getEntity();
		if (!(entity instanceof Player)) {

			return;
		}
		Player player = (Player) entity;

		// prevent godmode
		Warzone zone = Warzone.getZoneByPlayerName(player.getName());
		if (zone != null) {
			event.setCancelled(false);
		}

		// pass pvp-damage
		if (event instanceof EntityDamageByEntityEvent) {
			this.handlerAttackDefend((EntityDamageByEntityEvent) event);
		} else  {
			Team team = Team.getTeamByPlayerName(player.getName());

			if (zone != null && team != null) {
				LoadoutSelection playerLoadoutState = zone.getLoadoutSelections().get(player.getName());
				if (team.isSpawnLocation(player.getLocation())
						&& playerLoadoutState != null && playerLoadoutState.isStillInSpawn()) {
					// don't let a player still in spawn get damaged
					event.setCancelled(true);
				} else if (event.getDamage() >= player.getHealth()) {
					if (zone.getReallyDeadFighters().contains(player.getName())) {
						// don't re-count the death points of an already dead person
						return;
					}

					// Detect death, prevent it and respawn the player
					WarPlayerDeathEvent event1 = new WarPlayerDeathEvent(zone, player, null, event.getCause());
					War.war.getServer().getPluginManager().callEvent(event1);
					if (!zone.getWarzoneConfig().getBoolean(WarzoneConfig.REALDEATHS)) {
						// fast respawn, don't really die
						event.setCancelled(true);
					}
					zone.handleNaturalKill(player, event);
				}
			}
		}
	}

	/**
	 * Prevents creatures from spawning in warzones if no creatures is active
	 *
	 * @see EntityListener.onCreatureSpawn()
	 */
	@EventHandler
	public void onCreatureSpawn(final CreatureSpawnEvent event) {
		if (!War.war.isLoaded()) {
			return;
		}

		Location location = event.getLocation();
		Warzone zone = Warzone.getZoneByLocation(location);
		if (zone != null && zone.getWarzoneConfig().getBoolean(WarzoneConfig.NOCREATURES)) {
			event.setCancelled(true);
		}
	}

	/**
	 * Prevents health regaining caused by peaceful mode
	 *
	 * @see EntityListener.onEntityRegainHealth()
	 */
	@EventHandler
	public void onEntityRegainHealth(final EntityRegainHealthEvent event) {
		if (!War.war.isLoaded()) {
			return;
		}

		Entity entity = event.getEntity();
		if (!(entity instanceof Player)) {
			return;
		}

		Player player = (Player) entity;
		Warzone zone = Warzone.getZoneByPlayerName(player.getName());
		if (zone != null) {
			Team team = Team.getTeamByPlayerName(player.getName());
			if (event.getRegainReason() == RegainReason.SATIATED
				&& team.getTeamConfig().resolveBoolean(TeamConfig.NOHUNGER)) {
				// noHunger setting means you can't auto-heal with full hunger bar (use saturation instead to control how fast you get hungry)
				event.setCancelled(true);
			} else if (event.getRegainReason() == RegainReason.REGEN) {
				// disable peaceful mode regen
				event.setCancelled(true);
			}
		}
	}

	@EventHandler
	public void onFoodLevelChange(final FoodLevelChangeEvent event) {
		if (!War.war.isLoaded() || !(event.getEntity() instanceof Player)) {
			return;
		}

		Player player = (Player) event.getEntity();
		Warzone zone = Warzone.getZoneByPlayerName(player.getName());
		Team team = Team.getTeamByPlayerName(player.getName());
		if (zone != null && team.getTeamConfig().resolveBoolean(TeamConfig.NOHUNGER)){
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onPlayerDeath(final PlayerDeathEvent event) {
		Player player = event.getEntity();
		Warzone zone = Warzone.getZoneByPlayerName(player.getName());
		if (zone != null) {
			event.getDrops().clear();
			if (!zone.getWarzoneConfig().getBoolean(WarzoneConfig.REALDEATHS)) {
				// catch the odd death that gets away from us when usually intercepting and preventing deaths
				zone.handleDeath(player);
				Team team = Team.getTeamByPlayerName(player.getName());
				if (zone.getWarzoneConfig().getBoolean(WarzoneConfig.DEATHMESSAGES)) {
					zone.broadcast("pvp.death.other", team.getKind().getColor() + player.getName());
				}
				War.war.getLogger().log(Level.WARNING, "We missed the death of player {0} - something went wrong.", player.getName());
			} else {
				event.setDeathMessage("");
			}
		}
	}

	@EventHandler
    public void onExplosionPrime(final ExplosionPrimeEvent event) {
		if (!War.war.isLoaded()) {
			return;
		}

		Location eventLocation = event.getEntity().getLocation();

		for (Warzone zone : War.war.getWarzones()) {
			if (zone.isBombBlock(eventLocation.getBlock())) {
				// prevent the Bomb from exploding on its pedestral
				event.setCancelled(true);
				return;
			}
		}
	}

	@EventHandler
	public void onProjectileHit(final ProjectileHitEvent event) {
		if (!War.war.isLoaded()) {
			return;
		}
		if (event.getEntityType() == EntityType.EGG) {
			if (event.getEntity().hasMetadata("warAirstrike")) {
				Location loc = event.getEntity().getLocation();
				Warzone zone = Warzone.getZoneByLocation(loc);
				if (zone == null) {
					return;
				}
				Location tntPlace = new Location(loc.getWorld(), loc.getX(), Warzone.getZoneByLocation(loc).getVolume().getMaxY(), loc.getZ());
				loc.getWorld().spawnEntity(tntPlace, EntityType.PRIMED_TNT);
				loc.getWorld().spawnEntity(tntPlace.clone().add(new Vector(2, 0, 0)), EntityType.PRIMED_TNT);
				loc.getWorld().spawnEntity(tntPlace.clone().add(new Vector(-2, 0, 0)), EntityType.PRIMED_TNT);
				loc.getWorld().spawnEntity(tntPlace.clone().add(new Vector(0, 0, 2)), EntityType.PRIMED_TNT);
				loc.getWorld().spawnEntity(tntPlace.clone().add(new Vector(0, 0, -2)), EntityType.PRIMED_TNT);
			}
		}
	}

	@EventHandler
	public void onProjectileLaunch(final ProjectileLaunchEvent event) {
		if (!War.war.isLoaded()) {
			return;
		}
		if (event.getEntityType() == EntityType.EGG) {
			ProjectileSource shooter = event.getEntity().getShooter();
			if (shooter instanceof Player) {
				Player player = (Player) shooter;
				Warzone zone = Warzone.getZoneByPlayerName(player.getName());
				Team team = Team.getTeamByPlayerName(player.getName());
				if (zone != null) {
					if (War.war.getKillstreakReward().getAirstrikePlayers().remove(player.getName())) {
						event.getEntity().setMetadata("warAirstrike", new FixedMetadataValue(War.war, true));
						zone.broadcast("zone.airstrike", team.getKind().getColor() + player.getName() + ChatColor.WHITE);
					}
				}
			}
		}
	}

	@EventHandler
	public void onPaintingBreakByEntity(final HangingBreakByEntityEvent event) {
		if (!War.war.isLoaded()) {
			return;
		}
		if (!(event.getRemover() instanceof Player)) {
			return;
		}
		Player player = (Player) event.getRemover();
		Warzone zone = Warzone.getZoneByLocation(event.getEntity().getLocation());
		Team team = Team.getTeamByPlayerName(player.getName());
		boolean isZoneMaker = War.war.isZoneMaker(player);
		if (team == null && isZoneMaker) {
			return;
		}
		if (zone != null && zone.getWarzoneConfig().getBoolean(WarzoneConfig.UNBREAKABLE)) {
			event.setCancelled(true);
			War.war.badMsg(player, "build.denied.zone.break");
		}
	}

	@EventHandler
	public void onPaintingPlaceByEntity(final HangingPlaceEvent event) {
		if (!War.war.isLoaded()) {
			return;
		}
		Player player = event.getPlayer();
		Warzone zone = Warzone.getZoneByLocation(event.getBlock().getLocation());
		Team team = Team.getTeamByPlayerName(player.getName());
		boolean isZoneMaker = War.war.isZoneMaker(player);
		if (team == null && isZoneMaker) {
			return;
		}
		if (zone != null && (zone.getWarzoneConfig().getBoolean(WarzoneConfig.UNBREAKABLE)
			|| (team != null && !team.getTeamConfig().resolveBoolean(TeamConfig.PLACEBLOCK)))) {
			event.setCancelled(true);
			War.war.badMsg(player, "build.denied.zone.place");
		}
	}

	@EventHandler
	public void onEntityTeleport(final EntityTeleportEvent event) {
		if (!War.war.isLoaded()) {
			return;
		}
		if (event.getEntityType() == EntityType.WOLF) {
			if (Warzone.getZoneByLocation(event.getTo()) != null) {
				// prevent wolves from teleporting to players in zones
				event.setCancelled(true);
				event.setTo(event.getFrom());
			}
		}
	}
}
