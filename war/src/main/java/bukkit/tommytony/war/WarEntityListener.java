package bukkit.tommytony.war;

import java.util.List;

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
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityListener;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;

import com.tommytony.war.Team;
import com.tommytony.war.Warzone;

/**
 * 
 * @author tommytony
 * 
 */
public class WarEntityListener extends EntityListener {

	private final War war;

	public WarEntityListener(War war) {
		this.war = war;
	}

	private void handlerAttackDefend(EntityDamageByEntityEvent event) {
		Entity attacker = event.getDamager();
		Entity defender = event.getEntity();

		if (attacker != null && defender != null && attacker instanceof Player && defender instanceof Player) {
			// only let adversaries (same warzone, different team) attack each other
			Player a = (Player) attacker;
			Player d = (Player) defender;
			Warzone attackerWarzone = this.war.getPlayerTeamWarzone(a.getName());
			Team attackerTeam = this.war.getPlayerTeam(a.getName());
			Warzone defenderWarzone = this.war.getPlayerTeamWarzone(d.getName());
			Team defenderTeam = this.war.getPlayerTeam(d.getName());
			if (attackerTeam != null && defenderTeam != null && attackerTeam != defenderTeam && attackerWarzone == defenderWarzone) {
				// Make sure one of the players isn't in the spawn
				if (defenderTeam.getSpawnVolume().contains(d.getLocation())) { // attacking person in spawn
					if (!defenderWarzone.isFlagThief(d.getName())) { // thiefs can always be attacked
						this.war.badMsg(a, "Can't attack a player that's inside his team's spawn.");
						event.setCancelled(true);
					}
				} else if (attackerTeam.getSpawnVolume().contains(a.getLocation()) && !attackerTeam.getSpawnVolume().contains(d.getLocation())) {
					// only let a player inside spawn attack an enemy player if that player enters the spawn
					if (!attackerWarzone.isFlagThief(a.getName())) { // thiefs can always attack
						this.war.badMsg(a, "Can't attack a player from inside your spawn.");
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
					this.war.badMsg(a, "Friendly fire is on! Please, don't hurt your teammates."); // if ff is on, let the attack go through
				} else {
					this.war.badMsg(a, "Your attack missed! Your target is on your team.");
					event.setCancelled(true); // ff is off
				}
			} else if (attackerTeam == null && defenderTeam == null && this.war.canPvpOutsideZones(a)) {
				// let normal PVP through is its not turned off or if you have perms
			} else if (attackerTeam == null && defenderTeam == null && !this.war.canPvpOutsideZones(a)) {
				if (!this.war.isDisablePvpMessage()) {
					this.war.badMsg(a, "You need the 'war.pvp' permission to attack players outside warzones.");
				}
				event.setCancelled(true); // global pvp is off
			} else {
				this.war.badMsg(a, "Your attack missed!");
				if (attackerTeam == null) {
					this.war.badMsg(a, "You must join a team " + ", then you'll be able to damage people " + "in the other teams in that warzone.");
				} else if (defenderTeam == null) {
					this.war.badMsg(a, "Your target is not in a team.");
				} else if (attacker != null && defender != null && attacker.getEntityId() == defender.getEntityId()) {
					// You just hit yourself, probably with a bouncing arrow
				} else if (attackerTeam == defenderTeam) {
					this.war.badMsg(a, "Your target is on your team.");
				} else if (attackerWarzone != defenderWarzone) {
					this.war.badMsg(a, "Your target is playing in another warzone.");
				}
				event.setCancelled(true); // can't attack someone inside a warzone if you're not in a team
			}

		} else if (defender instanceof Player) {
			// attacked by dispenser arrow most probably
			// Detect death, prevent it and respawn the player
			Player d = (Player) defender;
			Warzone defenderWarzone = this.war.getPlayerTeamWarzone(d.getName());
			if (d != null && defenderWarzone != null && event.getDamage() >= d.getHealth()) {
				String deathMessage = "";
				if (event instanceof EntityDamageByProjectileEvent)
					deathMessage = "A dispenser killed " + this.war.getPlayerTeam(d.getName()).getKind().getColor() + d.getDisplayName();
				else if (event.getDamager() instanceof CraftTNTPrimed)
					deathMessage = this.war.getPlayerTeam(d.getName()).getKind().getColor() + d.getDisplayName() + ChatColor.WHITE + " exploded";
				else 
					deathMessage = this.war.getPlayerTeam(d.getName()).getKind().getColor() + d.getDisplayName() + ChatColor.WHITE + " died";
				for (Team team : defenderWarzone.getTeams()) {
					team.teamcast(deathMessage);
				}
				defenderWarzone.handleDeath(d);
				event.setCancelled(true);
			}
		}
	}

	@Override
	public void onEntityExplode(EntityExplodeEvent event) {
		if (this.war.isLoaded()) {
			// protect zones elements, lobbies and warhub from creepers
			List<Block> explodedBlocks = event.blockList();
			for (Block block : explodedBlocks) {
				if (this.war.getWarHub() != null && this.war.getWarHub().getVolume().contains(block)) {
					event.setCancelled(true);
					this.war.logInfo("Explosion prevented at warhub.");
					return;
				}
				for (Warzone zone : this.war.getWarzones()) {
					if (zone.isImportantBlock(block)) {
						event.setCancelled(true);
						this.war.logInfo("Explosion prevented in zone " + zone.getName() + ".");
						return;
					} else if (zone.getLobby() != null && zone.getLobby().getVolume().contains(block)) {
						event.setCancelled(true);
						this.war.logInfo("Explosion prevented at zone " + zone.getName() + " lobby.");
						return;
					}
				}
			}
		}
	}

	@Override
	public void onEntityDamage(EntityDamageEvent event) {
		if (this.war.isLoaded()) {
			Entity entity = event.getEntity();
			if (entity instanceof Player && this.war.getPlayerTeamWarzone(((Player) entity).getName()) != null) {
				event.setCancelled(false);
			}

			if (event instanceof EntityDamageByEntityEvent || event instanceof EntityDamageByProjectileEvent) {
				this.handlerAttackDefend((EntityDamageByEntityEvent) event);
			} else {
				// Detect death (from , prevent it and respawn the player
				if (entity instanceof Player) {
					Player player = (Player) entity;
					Warzone zone = this.war.getPlayerTeamWarzone(player.getName());
					if (zone != null && event.getDamage() >= player.getHealth()) {
						String deathMessage = "";
						deathMessage = this.war.getPlayerTeam(player.getName()).getKind().getColor() + player.getDisplayName() + ChatColor.WHITE + " died";
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
		if (this.war.isLoaded()) {
			Entity entity = event.getEntity();
			if (entity instanceof Player) {
				Player player = (Player) entity;
				Team team = this.war.getPlayerTeam(player.getName());
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

	@Override
	public void onCreatureSpawn(CreatureSpawnEvent event) {
		if (this.war.isLoaded()) {
			Location location = event.getLocation();
			Warzone zone = this.war.warzone(location);
			if (zone != null && zone.isNoCreatures()) {
				event.setCancelled(true);
				// war.logInfo("Prevented " + event.getMobType().getName() + " from spawning in zone " + zone.getName());
			}
		}
	}

	@Override
	public void onEntityRegainHealth(EntityRegainHealthEvent event) {
		if (this.war.isLoaded() && event.getRegainReason() == RegainReason.REGEN) {
			Entity entity = event.getEntity();
			if (entity instanceof Player) {
				Player player = (Player) entity;
				Location location = player.getLocation();
				Warzone zone = this.war.warzone(location);
				if (zone != null) {
					event.setCancelled(true);
				}
			}
		}
	}
}
