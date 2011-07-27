package bukkit.tommytony.war;

import java.util.List;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.entity.CraftTNTPrimed;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageByProjectileEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityListener;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;

import com.tommytony.war.Team;
import com.tommytony.war.Warzone;

/**
 * Handles Entity-Events
 *
 * @author 	tommytony, Tim Düsterhus
 * @package	bukkit.tommytony.war
 */
public class WarEntityListener extends EntityListener {

	/**
	 * Handles PVP-Damage
	 *
	 * @param EntityDamageByEntityEvent	event	fired event
	 */
	private void handlerAttackDefend(EntityDamageByEntityEvent event) {
		Entity attacker = event.getDamager();
		Entity defender = event.getEntity();

		if (attacker != null && defender != null && attacker instanceof Player && defender instanceof Player) {
			// only let adversaries (same warzone, different team) attack each other
			Player a = (Player) attacker;
			Player d = (Player) defender;
			Warzone attackerWarzone = Warzone.getZoneByPlayerName(a.getName());
			Team attackerTeam = Team.getTeamByPlayerName(a.getName());
			Warzone defenderWarzone = Warzone.getZoneByPlayerName(d.getName());
			Team defenderTeam = Team.getTeamByPlayerName(d.getName());
			if (attackerTeam != null && defenderTeam != null && attackerTeam != defenderTeam && attackerWarzone == defenderWarzone) {
				// Make sure one of the players isn't in the spawn
				if (defenderTeam.getSpawnVolume().contains(d.getLocation())) { // attacking person in spawn
					if (!defenderWarzone.isFlagThief(d.getName())) { // thiefs can always be attacked
						War.war.badMsg(a, "Can't attack a player that's inside his team's spawn.");
						event.setCancelled(true);
					}
				} else if (attackerTeam.getSpawnVolume().contains(a.getLocation()) && !attackerTeam.getSpawnVolume().contains(d.getLocation())) {
					// only let a player inside spawn attack an enemy player if that player enters the spawn
					if (!attackerWarzone.isFlagThief(a.getName())) { // thiefs can always attack
						War.war.badMsg(a, "Can't attack a player from inside your spawn.");
						event.setCancelled(true);
					}
				}

				// Detect death, prevent it and respawn the player
				if (event.getDamage() >= d.getHealth()) {
					String killMessage = "";
					killMessage = attackerTeam.getKind().getColor() + a.getDisplayName() + ChatColor.WHITE +
									" killed " + defenderTeam.getKind().getColor() + d.getDisplayName();
					for (Team team : defenderWarzone.getTeams()) {
						team.teamcast(killMessage);
					}
					defenderWarzone.handleDeath(d);
					event.setCancelled(true);
				}
			} else if (attackerTeam != null && defenderTeam != null && attackerTeam == defenderTeam && attackerWarzone == defenderWarzone && attacker.getEntityId() != defender.getEntityId()) {
				// same team, but not same person
				if (attackerWarzone.getFriendlyFire()) {
					War.war.badMsg(a, "Friendly fire is on! Please, don't hurt your teammates."); // if ff is on, let the attack go through
				} else {
					War.war.badMsg(a, "Your attack missed! Your target is on your team.");
					event.setCancelled(true); // ff is off
				}
			} else if (attackerTeam == null && defenderTeam == null && War.war.canPvpOutsideZones(a)) {
				// let normal PVP through is its not turned off or if you have perms
			} else if (attackerTeam == null && defenderTeam == null && !War.war.canPvpOutsideZones(a)) {
				if (!War.war.isDisablePvpMessage()) {
					War.war.badMsg(a, "You need the 'war.pvp' permission to attack players outside warzones.");
				}
				event.setCancelled(true); // global pvp is off
			} else {
				War.war.badMsg(a, "Your attack missed!");
				if (attackerTeam == null) {
					War.war.badMsg(a, "You must join a team " + ", then you'll be able to damage people " + "in the other teams in that warzone.");
				} else if (defenderTeam == null) {
					War.war.badMsg(a, "Your target is not in a team.");
				} else if (attacker != null && defender != null && attacker.getEntityId() == defender.getEntityId()) {
					// You just hit yourself, probably with a bouncing arrow
				} else if (attackerTeam == defenderTeam) {
					War.war.badMsg(a, "Your target is on your team.");
				} else if (attackerWarzone != defenderWarzone) {
					War.war.badMsg(a, "Your target is playing in another warzone.");
				}
				event.setCancelled(true); // can't attack someone inside a warzone if you're not in a team
			}

		} else if (defender instanceof Player) {
			// attacked by dispenser arrow most probably
			// Detect death, prevent it and respawn the player
			Player d = (Player) defender;
			Warzone defenderWarzone = Warzone.getZoneByPlayerName(d.getName());
			if (d != null && defenderWarzone != null && event.getDamage() >= d.getHealth()) {
				String deathMessage = "";
				if (event instanceof EntityDamageByProjectileEvent)
					deathMessage = "A dispenser killed " + Team.getTeamByPlayerName(d.getName()).getKind().getColor() + d.getDisplayName();
				else if (event.getDamager() instanceof CraftTNTPrimed)
					deathMessage = Team.getTeamByPlayerName(d.getName()).getKind().getColor() + d.getDisplayName() + ChatColor.WHITE + " exploded";
				else
					deathMessage = Team.getTeamByPlayerName(d.getName()).getKind().getColor() + d.getDisplayName() + ChatColor.WHITE + " died";
				for (Team team : defenderWarzone.getTeams()) {
					team.teamcast(deathMessage);
				}
				defenderWarzone.handleDeath(d);
				event.setCancelled(true);
			}
		}
	}

	/**
	 * Protects important structures from explosions
	 */
	@Override
	public void onEntityExplode(EntityExplodeEvent event) {
		if (War.war.isLoaded()) {
			// protect zones elements, lobbies and warhub from creepers
			List<Block> explodedBlocks = event.blockList();
			for (Block block : explodedBlocks) {
				if (War.war.getWarHub() != null && War.war.getWarHub().getVolume().contains(block)) {
					event.setCancelled(true);
					War.war.log("Explosion prevented at warhub.", Level.INFO);
					return;
				}
				for (Warzone zone : War.war.getWarzones()) {
					if (zone.isImportantBlock(block)) {
						event.setCancelled(true);
						War.war.log("Explosion prevented in zone " + zone.getName() + ".", Level.INFO);
						return;
					} else if (zone.getLobby() != null && zone.getLobby().getVolume().contains(block)) {
						event.setCancelled(true);
						War.war.log("Explosion prevented at zone " + zone.getName() + " lobby.", Level.INFO);
						return;
					}
				}
			}
		}
	}

	/**
	 * Handles damage on Players
	 */
	@Override
	public void onEntityDamage(EntityDamageEvent event) {
		if (War.war.isLoaded()) {
			Entity entity = event.getEntity();
			// prevent godmode
			if (entity instanceof Player && Warzone.getZoneByPlayerName(((Player) entity).getName()) != null) {
				event.setCancelled(false);
			}

			// pass pvp-damage
			if (event instanceof EntityDamageByEntityEvent || event instanceof EntityDamageByProjectileEvent) {
				this.handlerAttackDefend((EntityDamageByEntityEvent) event);
			} else {
				// Detect death, prevent it and respawn the player
				if (entity instanceof Player) {
					Player player = (Player) entity;
					Warzone zone = Warzone.getZoneByPlayerName(player.getName());
					if (zone != null && event.getDamage() >= player.getHealth()) {
						String deathMessage = "";
						deathMessage = Team.getTeamByPlayerName(player.getName()).getKind().getColor() + player.getDisplayName() + ChatColor.WHITE + " died";
						for (Team team : zone.getTeams()) {
							team.teamcast(deathMessage);
						}
						zone.handleDeath(player);
						event.setCancelled(true);
					}
				}
			}
		}
	}

	@Override
	public void onEntityCombust(EntityCombustEvent event) {
		if (War.war.isLoaded()) {
			Entity entity = event.getEntity();
			if (entity instanceof Player) {
				Player player = (Player) entity;
				Team team = Team.getTeamByPlayerName(player.getName());
				if (team != null && team.getSpawnVolume().contains(player.getLocation())) {
					// smother out the fire that didn't burn out when you respawned
					// Stop fire (upcast, watch out!)
					if (player instanceof CraftPlayer) {
						net.minecraft.server.Entity playerEntity = ((CraftPlayer) player).getHandle();
						playerEntity.fireTicks = 0;
					}
					event.setCancelled(true);
				}
			}
		}
	}

	/**
	 * Prevents creatures from spawning in warzones if no creatures is active
	 */
	@Override
	public void onCreatureSpawn(CreatureSpawnEvent event) {
		if (War.war.isLoaded()) {
			Location location = event.getLocation();
			Warzone zone = Warzone.getZoneByLocation(location);
			if (zone != null && zone.isNoCreatures()) {
				event.setCancelled(true);
				// war.logInfo("Prevented " + event.getMobType().getName() + " from spawning in zone " + zone.getName());
			}
		}
	}

	/**
	 * Prevents health regaining caused by peaceful mode
	 */
	@Override
	public void onEntityRegainHealth(EntityRegainHealthEvent event) {
		if (War.war.isLoaded() && event.getRegainReason() == RegainReason.REGEN) {
			Entity entity = event.getEntity();
			if (entity instanceof Player) {
				Player player = (Player) entity;
				Warzone zone = Warzone.getZoneByLocation(player);
				if (zone != null) {
					event.setCancelled(true);
				}
			}
		}
	}
}
