package com.tommytony.war.mapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

import com.tommytony.war.War;
import com.tommytony.war.utility.Loadout;
import java.util.Collections;
import org.bukkit.Color;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

public class LoadoutYmlMapper {
	
	/**
	 * Deserializes loadouts from the configuration.
	 * Backwards compatibility: returns new-style loadouts and still modifies
	 * the loadouts parameter.
	 * @param config A configuration section that contains loadouts
	 * @param loadouts Map of the loadout names and the items. This will be
	 * cleared and written to by the method, cannot be final.
	 * @return list of new style loadouts
	 */
	public static List<Loadout> fromConfigToLoadouts(ConfigurationSection config, HashMap<String, HashMap<Integer, ItemStack>> loadouts) {
		List<String> loadoutNames = config.getStringList("names");
		loadouts.clear();
		List<Loadout> ldts = new ArrayList<Loadout>();
		for (String name : loadoutNames) {
			HashMap<Integer, ItemStack> newLoadout = new HashMap<Integer, ItemStack>();
			Loadout ldt = fromConfigToLoadout(config, newLoadout, name);
			ldts.add(ldt);
			loadouts.put(name, newLoadout);
		}
		Collections.sort(ldts);
		return ldts;
	}
	/**
	 * Deserialize a loadout from the configuration.
	 * Backwards compatibility: returns new-style loadout and still modifies the
	 * loadout parameter.
	 * @param config A configuration section that contains loadouts
	 * @param loadout Map of slots and items in the loadout. Will be written to
	 * by the method, cannot be final.
	 * @param loadoutName The name of the loadout
	 * @return new style loadout
	 */
	@SuppressWarnings("deprecation")
	public static Loadout fromConfigToLoadout(ConfigurationSection config, HashMap<Integer, ItemStack> loadout, String loadoutName) {
		List<Integer> slots = config.getIntegerList(loadoutName + ".slots");
		for (Integer slot : slots) {
			if (config.isItemStack(loadoutName + "." + Integer.toString(slot))) {
				loadout.put(slot, config.getItemStack(loadoutName + "." + Integer.toString(slot)));
				continue;
			}
			String prefix = loadoutName + "." + slot + ".";
			int id = config.getInt(prefix + "id");
			int amount = config.getInt(prefix + "amount");
			short durability = (short)config.getInt(prefix + "durability");
			
			ItemStack stack = new ItemStack(id, amount, durability);
			stack.setDurability(durability);
			
			if (config.contains(prefix + "enchantments")) {
				List<String> enchantmentStringList = config.getStringList(prefix + "enchantments");
				for (String enchantmentString : enchantmentStringList) {
					String[] enchantmentStringSplit = enchantmentString.split(",");
					if (enchantmentStringSplit.length == 2) {
						int enchantId = Integer.parseInt(enchantmentStringSplit[0]);
						int level = Integer.parseInt(enchantmentStringSplit[1]);
						War.war.safelyEnchant(stack, Enchantment.getById(enchantId), level);
					}
				}
			}
			if (config.contains(prefix + "armorcolor")) {
				int rgb = config.getInt(prefix + "armorcolor");
				Color clr = Color.fromRGB(rgb);
				LeatherArmorMeta meta = (LeatherArmorMeta) stack.getItemMeta();
				meta.setColor(clr);
				stack.setItemMeta(meta);
			}
			if (config.contains(prefix + "name")) {
				String itemName = config.getString(prefix + "name");
				ItemMeta meta = stack.getItemMeta();
				meta.setDisplayName(itemName);
				stack.setItemMeta(meta);
			}
			if (config.contains(prefix + "lore")) {
				List<String> itemLore = config.getStringList(prefix + "lore");
				ItemMeta meta = stack.getItemMeta();
				meta.setLore(itemLore);
				stack.setItemMeta(meta);
			}
			loadout.put(slot, stack);
		}
		String permission = config.getString(loadoutName + ".permission", "");
		return new Loadout(loadoutName, loadout, permission);
	}
	
	public static void fromLoadoutsToConfig(HashMap<String, HashMap<Integer, ItemStack>> loadouts, ConfigurationSection section) {
		List<String> sortedNames = sortNames(loadouts);
		
		section.set("names", sortedNames);
		for (String name : sortedNames) {
			fromLoadoutToConfig(name, loadouts.get(name), section);
		}
	}

	/**
	 * Serializes a list of new style loadouts to the configuration.
	 * @param loadouts List of new style loadouts
	 * @param section Section of the configuration to write to
	 */
	public static void fromLoadoutsToConfig(List<Loadout> loadouts, ConfigurationSection section) {
		Collections.sort(loadouts);
		List<String> names = new ArrayList<String>();
		for (Loadout ldt : loadouts) {
			names.add(ldt.getName());
			LoadoutYmlMapper.fromLoadoutToConfig(ldt, section);
		}
		section.set("names", names);
	}
	
	public static List<String> sortNames(HashMap<String, HashMap<Integer, ItemStack>> loadouts) {
		List<String> sortedNames = new ArrayList<String>();
		
		// default comes first
		if (loadouts.containsKey("default")) {
			sortedNames.add("default");
		}
		
		for (String name : loadouts.keySet()) {
			if (!name.equals("default")) {
				sortedNames.add(name);
			}
		}
		
		return sortedNames; 
	}

	private static List<Integer> toIntList(Set<Integer> keySet) {
		List<Integer> list = new ArrayList<Integer>();
		for (Integer key : keySet) {
			list.add(key);
		}
		return list;
	}

	/**
	 * Serialize a new style loadout to the configuration
	 * @param loadout New style loadout
	 * @param section Section of the configuration to write to
	 */
	public static void fromLoadoutToConfig(Loadout loadout, ConfigurationSection section) {
		LoadoutYmlMapper.fromLoadoutToConfig(loadout.getName(), loadout.getContents(), section);
		if (loadout.requiresPermission()) {
			section.set(loadout.getName() + ".permission", loadout.getPermission());
		}
	}
	public static void fromLoadoutToConfig(String loadoutName, HashMap<Integer, ItemStack> loadout, ConfigurationSection section) {
		ConfigurationSection loadoutSection = section.createSection(loadoutName);
		
		if (loadoutSection != null) {
			loadoutSection.set("slots", toIntList(loadout.keySet()));
			for (Integer slot : loadout.keySet()) {
				ItemStack stack = loadout.get(slot);
				loadoutSection.set(slot.toString(), stack);
			}
		}
	}
}
