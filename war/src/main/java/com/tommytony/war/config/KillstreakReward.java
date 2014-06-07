package com.tommytony.war.config;

import com.google.common.collect.ImmutableList;
import com.tommytony.war.Team;
import com.tommytony.war.War;
import com.tommytony.war.Warzone;

import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.Validate;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Manage rewards for certain killstreaks.
 *
 * @author cmastudios
 */
public class KillstreakReward {

	private ConfigurationSection section;
	private Set<String> airstrikePlayers;

	/**
	 * Creates a new killstreak reward class with default options.
	 */
	public KillstreakReward() {
		this(new MemoryConfiguration());
		section.set("3.privmsg", "You have been rewarded with some health for your kills.");
		section.set("3.reward.health", 8);
		section.set("4.reward.xp", 3);
		section.set("5.message", "{0} is on a &ckillstreak&f! 5 kills this life.");
		section.set("5.privmsg", "You have received some items for your kills.");
		section.set("5.reward.points", 1);
		section.set("5.reward.airstrike", true);
		section.set("5.reward.items", ImmutableList.of(new ItemStack(Material.ARROW, 15), new ItemStack(Material.EGG)));
		section.set("5.reward.effect", Effect.GHAST_SHRIEK.name());
		ItemStack sword = new ItemStack(Material.WOOD_SWORD);
		sword.addEnchantment(Enchantment.DAMAGE_ALL, 2);
		sword.addEnchantment(Enchantment.KNOCKBACK, 1);
		ItemMeta meta = sword.getItemMeta();
		meta.setDisplayName("The Breaker");
		meta.setLore(ImmutableList.of("Very slow speed"));
		sword.setItemMeta(meta);
		section.set("7.reward.items", ImmutableList.of(sword));
	}

	/**
	 * Creates a new killstreak reward class with options from the provided
	 * config.
	 *
	 * @param section Section to load killstreak options from, such as
	 * set.war.killstreak
	 */
	public KillstreakReward(ConfigurationSection section) {
		this.section = section;
		this.airstrikePlayers = new HashSet<String>();
	}

	/**
	 * Rewards a player for their current killstreak. The player must be in a
	 * warzone.
	 *
	 * @param player Player to reward
	 * @param kills Amount of kills to reward for
	 */
	public void rewardPlayer(Player player, int kills) {
		if (section == null) {
			/*
			 * Cancel the reward if there is no configuration for killstreaks.
			 * This can occur if the server owner has an older War config with
			 * no settings for killstreaks and have neglected to add any. Heck,
			 * they shouldn't have enabled killstreaks in the warzone anyway.
			 */
			return;
		}
		final Warzone zone = Warzone.getZoneByPlayerName(player.getName());
		final Team playerTeam = Team.getTeamByPlayerName(player.getName());
		Validate.notNull(zone, "Cannot reward player if they are not in a warzone");
		Validate.notNull(playerTeam, "Cannot reward player if they are not in a team");
		if (section.contains(Integer.toString(kills))) {
			ConfigurationSection killSection = section.getConfigurationSection(Integer.toString(kills));
			if (killSection.contains("message")) {
				final String playerName = playerTeam.getKind().getColor() + player.getName() + ChatColor.WHITE;
				final String message = ChatColor.translateAlternateColorCodes('&', MessageFormat.format(killSection.getString("message"), playerName));
				for (Team team : zone.getTeams()) {
					team.teamcast(message);
				}
			}
			if (killSection.contains("privmsg")) {
				War.war.msg(player, ChatColor.translateAlternateColorCodes('&', killSection.getString("privmsg")));
			}
			if (killSection.contains("reward.health")) {
				double health = player.getHealth() + killSection.getInt("reward.health");
				player.setHealth(health > 20 ? 20 : health); // Grant up to full health only
			}
			if (killSection.contains("reward.items")) {
				for (Object obj : killSection.getList("reward.items")) {
					if (obj instanceof ItemStack) {
						player.getInventory().addItem((ItemStack) obj);
					}
				}
			}
			if (killSection.contains("reward.xp") && !playerTeam.getTeamConfig().resolveBoolean(TeamConfig.XPKILLMETER)) {
				// Will not work if XPKILLMETER is enabled
				player.setLevel(player.getLevel() + killSection.getInt("reward.xp"));
			}
			if (killSection.contains("reward.points")) {
				for (int i = 0; i < killSection.getInt("reward.points"); i++) {
					playerTeam.addPoint();
				}
				// Detect win conditions
				if (playerTeam.getPoints() >= playerTeam.getTeamConfig().resolveInt(TeamConfig.MAXSCORE)) {
					player.getServer().getScheduler().runTaskLater(War.war, new Runnable() {
						public void run() {
							zone.handleScoreCapReached(playerTeam.getName());
						}
					}, 1L);
				} else {
					// just added a point
					playerTeam.resetSign();
					zone.getLobby().resetTeamGateSign(playerTeam);
				}
			}
			if (killSection.getBoolean("reward.airstrike")) {
				this.airstrikePlayers.add(player.getName());
			}
			if (killSection.contains("reward.effect")) {
				Effect effect = Effect.valueOf(killSection.getString("reward.effect"));
				player.getWorld().playEffect(player.getLocation(), effect, null);
			}
		}
	}

	public void saveTo(ConfigurationSection section) {
		Map<String, Object> values = this.section.getValues(true);
		for (Map.Entry<String, Object> entry : values.entrySet()) {
			section.set(entry.getKey(), entry.getValue());
		}
	}

	public Set<String> getAirstrikePlayers() {
		return airstrikePlayers;
	}
}
