package com.tommytony.war.mappers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

public class LoadoutYmlMapper {
	
	public static void fromConfigToLoadouts(ConfigurationSection config, HashMap<String, HashMap<Integer, ItemStack>> loadouts) {
		List<String> loadoutNames = config.getStringList("names");
		loadouts.clear();
		for (String name : loadoutNames) {
			HashMap<Integer, ItemStack> newLoadout = new HashMap<Integer, ItemStack>();
			loadouts.put(name, newLoadout);
			fromConfigToLoadout(config, newLoadout, name);
		}
	}
	
	public static void fromConfigToLoadout(ConfigurationSection config, HashMap<Integer, ItemStack> loadout, String loadoutName) {
		List<Integer> slots = config.getIntegerList(loadoutName + ".slots");
		for (Integer slot : slots) {
			String prefix = loadoutName + "." + slot + ".";
			
			int id = config.getInt(prefix + "id");
			byte data = (byte)config.getInt(prefix + "data");
			int amount = config.getInt(prefix + "amount");
			short durability = (short)config.getInt(prefix + "durability");
			
			ItemStack stack = new ItemStack(id, amount, durability, data);
			stack.setDurability(durability);
			
			if (config.contains(prefix + "enchantments")) {
				List<String> enchantmentStringList = config.getStringList(prefix + "enchantments");
				for (String enchantmentString : enchantmentStringList) {
					String[] enchantmentStringSplit = enchantmentString.split(",");
					if (enchantmentStringSplit.length == 2) {
						int enchantId = Integer.parseInt(enchantmentStringSplit[0]);
						int level = Integer.parseInt(enchantmentStringSplit[1]);
						stack.addEnchantment(Enchantment.getById(enchantId), level);
					}
				}
			}
		}
	}
	
	public static void fromLoadoutsToConfig(HashMap<String, HashMap<Integer, ItemStack>> loadouts, ConfigurationSection section) {
		section.set("names", toStringList(loadouts.keySet()));
		for (String name : loadouts.keySet()) {
			fromLoadoutToConfig(name, loadouts.get(name), section);
		}
	}
	
	private static List<String> toStringList(Set<String> keySet) {
		List<String> list = new ArrayList<String>();
		for (String key : keySet) {
			list.add(key);
		}
		return list;
	}
	
	private static List<Integer> toIntList(Set<Integer> keySet) {
		List<Integer> list = new ArrayList<Integer>();
		for (Integer key : keySet) {
			list.add(key);
		}
		return list;
	}

	public static void fromLoadoutToConfig(String loadoutName, HashMap<Integer, ItemStack> loadout, ConfigurationSection section) {
		ConfigurationSection loadoutSection = section.createSection(loadoutName);
		loadoutSection.set("slots", toIntList(loadout.keySet()));
		for (Integer slot : loadout.keySet()) {
			ConfigurationSection slotSection = loadoutSection.createSection(slot.toString());
			ItemStack stack = loadout.get(slot);
			
			slotSection.set("id", stack.getTypeId());
			slotSection.set("data", stack.getData().getData());
			slotSection.set("amount", stack.getAmount());
			slotSection.set("durability", stack.getDurability());

			if (stack.getEnchantments().keySet().size() > 0) {
				List<String> enchantmentStringList = new ArrayList<String>();
				for (Enchantment enchantment : stack.getEnchantments().keySet()) {
					int level = stack.getEnchantments().get(enchantment);
					enchantmentStringList.add(enchantment.getId() + "," + level);
				}
				slotSection.set("enchantments", enchantmentStringList);
			}
		}
	}
}
