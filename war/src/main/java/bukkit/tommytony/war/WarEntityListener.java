package bukkit.tommytony.war;

import java.util.List;
import java.util.Random;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.entity.CraftTNTPrimed;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
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
 * @author tommytony, Tim Düsterhus
 * @package bukkit.tommytony.war
 */
public class WarEntityListener extends EntityListener {

	private Random killSeed = new Random();
			
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

			if (attackerTeam != null && defenderTeam != null && attackerTeam != defenderTeam && attackerWarzone == defenderWarzone) {
				// Make sure one of the players isn't in the spawn
				if (defenderTeam.getSpawnVolume().contains(d.getLocation())) { // attacking person in spawn
					if (!defenderWarzone.isFlagThief(d.getName())) { // thieves can always be attacked
						War.war.badMsg(a, "Can't attack a player that's inside his team's spawn.");
						event.setCancelled(true);
						return;
					}
				} else if (attackerTeam.getSpawnVolume().contains(a.getLocation()) && !attackerTeam.getSpawnVolume().contains(d.getLocation())) {
					// only let a player inside spawn attack an enemy player if that player enters the spawn
					if (!attackerWarzone.isFlagThief(a.getName())) { // thieves can always attack
						War.war.badMsg(a, "Can't attack a player from inside your spawn.");
						event.setCancelled(true);
						return;
					}
				}
				
				if (!attackerWarzone.isPvpInZone()) {
					// spleef-like, non-pvp, zone
					event.setCancelled(true);
					return;
				}

				// Detect death, prevent it and respawn the player
				if (event.getDamage() >= d.getHealth()) {
					String killMessage = "";
					String attackerString = attackerTeam.getKind().getColor() + a.getDisplayName();
					String defenderString = defenderTeam.getKind().getColor() + d.getDisplayName();
					
					Material killerWeapon = a.getItemInHand().getType();
					String weaponString = killerWeapon.toString();
					if (killerWeapon == Material.AIR) {
						weaponString = "fist";
					} else if (killerWeapon == Material.BOW || event.getDamager() instanceof Arrow) {
						int rand = killSeed.nextInt(3);
						if (rand == 0) {
							weaponString = "arrow";
						} else if (rand == 1) {
							weaponString = "bow";
						} else {
							weaponString = "aim";
						}
						
					} else if (event.getDamager() instanceof Projectile) {
						weaponString = "aim";
					}
					
					String adjectiveString = War.war.getDeadlyAdjectives().get(this.killSeed.nextInt(War.war.getDeadlyAdjectives().size()));
					String verbString = War.war.getKillerVerbs().get(this.killSeed.nextInt(War.war.getKillerVerbs().size()));
					
					killMessage = attackerString + ChatColor.WHITE + "'s " + adjectiveString + weaponString.toLowerCase().replace('_', ' ') 
											+ " " + verbString + " " + defenderString;
					
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
					War.war.badMsg(a, "You must join a team, then you'll be able to damage people " + "in the other teams in that warzone.");
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
				String defenderString = Team.getTeamByPlayerName(d.getName()).getKind().getColor() + d.getDisplayName();
				/* if (event.getDamager() instanceof Projectile && ((Projectile)event.getDamager()).getShooter() instanceof Player){
					Player shooter = ((Player)((Projectile)event.getDamager()).getShooter());
					Team shooterTeam = Team.getTeamByPlayerName(shooter.getName()); 
					deathMessage = shooterTeam.getKind().getColor() + shooter.getDisplayName() + ChatColor.WHITE + "'s deadly aim killed " + defenderString;
				} else */ if (event.getDamager() instanceof CraftTNTPrimed) {
					deathMessage = defenderString + ChatColor.WHITE + " exploded";
				} else {
					deathMessage = defenderString + ChatColor.WHITE + " died";
				}
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
	 *
	 * @see EntityListener.onEntityExplode()
	 */
	@Override
	public void onEntityExplode(EntityExplodeEvent event) {
		if (!War.war.isLoaded()) {
			return;
		}
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

	/**
	 * Handles damage on Players
	 *
	 * @see EntityListener.onEntityDamage()
	 */
	@Override
	public void onEntityDamage(EntityDamageEvent event) {
		if (!War.war.isLoaded()) {
			return;
		}

		Entity entity = event.getEntity();
		if (!(entity instanceof Player)) {
			return;
		}
		Player player = (Player) entity;

		// prevent godmode
		if (Warzone.getZoneByPlayerName(player.getName()) != null) {
			event.setCancelled(false);
		}

		// pass pvp-damage
		if (event instanceof EntityDamageByEntityEvent) {
			this.handlerAttackDefend((EntityDamageByEntityEvent) event);
		} else {
			// Detect death, prevent it and respawn the player
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

	@Override
	public void onEntityCombust(EntityCombustEvent event) {
		if (!War.war.isLoaded()) {
			return;
		}
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

	/**
	 * Prevents creatures from spawning in warzones if no creatures is active
	 *
	 * @see EntityListener.onCreatureSpawn()
	 */
	@Override
	public void onCreatureSpawn(CreatureSpawnEvent event) {
		if (!War.war.isLoaded()) {
			return;
		}

		Location location = event.getLocation();
		Warzone zone = Warzone.getZoneByLocation(location);
		if (zone != null && zone.isNoCreatures()) {
			event.setCancelled(true);
		}
	}

	/**
	 * Prevents health regaining caused by peaceful mode
	 *
	 * @see EntityListener.onEntityRegainHealth()
	 */
	@Override
	public void onEntityRegainHealth(EntityRegainHealthEvent event) {
		if (!War.war.isLoaded() || event.getRegainReason() != RegainReason.REGEN) {
			return;
		}

		Entity entity = event.getEntity();
		if (!(entity instanceof Player)) {
			return;
		}

		Player player = (Player) entity;
		Warzone zone = Warzone.getZoneByLocation(player);
		if (zone != null) {
			event.setCancelled(true);
		}
	}
}
