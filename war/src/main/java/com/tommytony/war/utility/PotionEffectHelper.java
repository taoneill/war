package com.tommytony.war.utility;

import java.util.Collection;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;

public class PotionEffectHelper {

	public static void restorePotionEffects(Player player,
			Collection<PotionEffect> potionEffects) {
		clearPotionEffects(player);
		for (PotionEffect effect : potionEffects) {
			player.addPotionEffect(effect, true);
		}
	}

	public static void clearPotionEffects(Player player) {
		for (PotionEffect effect : player.getActivePotionEffects()) {
			player.removePotionEffect(effect.getType());
		}
	}
}
