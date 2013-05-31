package com.tommytony.war.config;

import com.tommytony.war.Team;
import com.tommytony.war.War;
import com.tommytony.war.Warzone;
import java.text.MessageFormat;
import java.util.Map;
import org.apache.commons.lang.Validate;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Manage rewards for certain killstreaks.
 *
 * @author cmastudios
 */
public class KillstreakReward {

	private ConfigurationSection section;

	/**
	 * Creates a new killstreak reward class with default options.
	 */
	public KillstreakReward() {
		section = new MemoryConfiguration();
		section.set("3.privmsg", "You have been rewarded with some health for your kills.");
		section.set("3.reward.health", 8);
		section.set("5.message", "{0} is on a &ckillstreak&f! 5 kills this life.");
		section.set("5.privmsg", "You have received some items for your kills.");
		section.set("5.reward.item.id", 262);
		section.set("5.reward.item.damage", (short) 0);
		section.set("5.reward.item.amount", 15);
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
	}

	/**
	 * Rewards a player for their current killstreak. The player must be in a
	 * warzone.
	 *
	 * @param player Player to reward
	 * @param kills Amount of kills to reward for
	 */
	public void rewardPlayer(Player player, int kills) {
		Warzone zone = Warzone.getZoneByPlayerName(player.getName());
		Team playerTeam = Team.getTeamByPlayerName(player.getName());
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
				int health = player.getHealth() + killSection.getInt("reward.health");
				player.setHealth(health > 20 ? 20 : health); // Grant up to full health only
			}
			if (killSection.contains("reward.item")) {
				player.getInventory().addItem(this.assembleItemStack(killSection.getConfigurationSection("reward.item")));
			}
		}
	}

	private ItemStack assembleItemStack(ConfigurationSection itemSection) {
		int type = itemSection.getInt("id");
		int amount = itemSection.getInt("amount");
		short damage = (short) itemSection.getInt("damage");
		return new ItemStack(type, amount, damage);
	}

	public void saveTo(ConfigurationSection section) {
		Map<String, Object> values = this.section.getValues(true);
		for (Map.Entry<String, Object> entry : values.entrySet()) {
			section.set(entry.getKey(), entry.getValue());
		}
	}
}
