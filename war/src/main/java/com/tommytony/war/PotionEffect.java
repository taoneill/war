package com.tommytony.war;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.server.MobEffect;
import net.minecraft.server.MobEffectList;

import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

public class PotionEffect {

	private int id;
	private int amplifier;
	private int time;

	public PotionEffect(int id, int amplifier, int time) {
		this.setId(id);
		this.setAmplifier(amplifier);
		this.setTime(time);
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}

	public void setAmplifier(int amplifier) {
		this.amplifier = amplifier;
	}

	public int getAmplifier() {
		return amplifier;
	}

	public void setTime(int time) {
		this.time = time;
	}

	public int getTime() {
		return time;
	}

	public static List<PotionEffect> getCurrentPotionEffects(Player player) {
		List<PotionEffect> effects = new ArrayList<PotionEffect>();
		
		for(int i = 1; i < 20; i++)
		{
			if(((CraftPlayer)player).getHandle().hasEffect(enchantIdToList(i)))
			{
				MobEffect mobEffect = ((CraftPlayer)player).getHandle().getEffect(enchantIdToList(i));
				effects.add(new PotionEffect(mobEffect.getEffectId(), mobEffect.getAmplifier(), mobEffect.getDuration()));
			}
		}
		
		return effects;
	}
	
	public static void restorePotionEffects(Player player, List<PotionEffect> potionEffects) {
		clearPotionEffects(player);
		for (PotionEffect effect : potionEffects) {
			((CraftPlayer)player).getHandle().addEffect(new MobEffect(effect.getId(), effect.getTime(), effect.getAmplifier()));
		}
	}

	public static void clearPotionEffects(Player player) {
		for (int i = 1; i < 20; i++) {
			if(((CraftPlayer)player).getHandle().hasEffect(enchantIdToList(i)))
			{
				int amplifier = ((CraftPlayer)player).getHandle().getEffect(enchantIdToList(i)).getAmplifier();
				((CraftPlayer)player).getHandle().addEffect(new MobEffect(i, -1, amplifier + 1));
			}
		}
	}

	private static MobEffectList enchantIdToList(int id) {
		switch (id) {
			case 1:
				return MobEffectList.FASTER_MOVEMENT;
			case 2:
				return MobEffectList.SLOWER_MOVEMENT;
			case 3:
				return MobEffectList.FASTER_DIG;
			case 4:
				return MobEffectList.SLOWER_DIG;
			case 5:
				return MobEffectList.INCREASE_DAMAGE;
			case 6:
				return MobEffectList.HEAL;
			case 7:
				return MobEffectList.HARM;
			case 8:
				return MobEffectList.JUMP;
			case 9:
				return MobEffectList.CONFUSION;
			case 10:
				return MobEffectList.REGENERATION;
			case 11:
				return MobEffectList.RESISTANCE;
			case 12:
				return MobEffectList.FIRE_RESISTANCE;
			case 13:
				return MobEffectList.WATER_BREATHING;
			case 14:
				return MobEffectList.INVISIBILITY;
			case 15:
				return MobEffectList.BLINDNESS;
			case 16:
				return MobEffectList.NIGHT_VISION;
			case 17:
				return MobEffectList.HUNGER;
			case 18:
				return MobEffectList.WEAKNESS;
			case 19:
				return MobEffectList.POISON;
			default:
				return null;					
		}
	}

}
