package com.tommytony.war.mappers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
			byte data = Byte.parseByte(config.getString(prefix + "data"));
			int amount = config.getInt(prefix + "amount");
			short durability = Short.parseShort(config.getString(prefix + "durability"));
			
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
	
	public static void fromLoadoutsToConfig(HashMap<String, HashMap<Integer, ItemStack>> loadouts, ConfigurationSection config) {
		config.set("names", loadouts.keySet());
		for (String name : loadouts.keySet()) {
			fromLoadoutToConfig(loadouts.get(name), config, name);
		}
	}
	
	public static void fromLoadoutToConfig(HashMap<Integer, ItemStack> loadout, ConfigurationSection config, String loadoutName) {
		//ConfigurationSection loadoutSection = config.createSection(loadoutName);
		//loadoutSection.set("slots", loadout.keySet());
		config.set(loadoutName + ".slots", loadout.keySet());
		for (Integer slot : loadout.keySet()) {
//			ConfigurationSection slotSection = loadoutSection.createSection(slot.toString());
//			ItemStack stack = loadout.get(slot);
//			
//			slotSection.set("id", stack.getTypeId());
//			slotSection.set("data", Byte.toString(stack.getData().getData()));
//			slotSection.set("amount", stack.getAmount());
//			slotSection.set("durability", Short.toString(stack.getDurability()));
			
			ItemStack stack = loadout.get(slot);
			String slotPrefix = loadoutName + "." + slot + ".";			
			config.set(slotPrefix + "id", stack.getTypeId());
			config.set(slotPrefix + "data", Byte.toString(stack.getData().getData()));
			config.set(slotPrefix + "amount", stack.getAmount());
			config.set(slotPrefix + "durability", Short.toString(stack.getDurability()));
			
			if (stack.getEnchantments().keySet().size() > 0) {
				List<String> enchantmentStringList = new ArrayList<String>();
				for (Enchantment enchantment : stack.getEnchantments().keySet()) {
					int level = stack.getEnchantments().get(enchantment);
					enchantmentStringList.add(enchantment.getId() + "," + level);
				}
				config.set(slotPrefix + "enchantments", enchantmentStringList);
			}
		}
	}
}
