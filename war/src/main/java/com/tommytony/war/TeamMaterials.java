package com.tommytony.war;

import org.bukkit.Material;

/**
 * 
 * @author tommytony
 *
 */
public class TeamMaterials {
	public static final Material TEAMDIAMOND = Material.DIAMOND_BLOCK;
	public static final Material TEAMIRON = Material.IRON_BLOCK;
	public static final Material TEAMGOLD = Material.GOLD_BLOCK;
	
	public static Material teamMaterialFromString(String str) {
		String lowered = str.toLowerCase();
		if(lowered.equals("diamond") || lowered.equals("d")) {
			return TEAMDIAMOND;
		} else if (lowered.equals("iron") || lowered.equals("i")) {
			return TEAMIRON;
		} else if (lowered.equals("gold") || lowered.equals("g")) {
			return TEAMGOLD;
		}
		return null;
	}
	
	public static String teamMaterialToString(Material material) {
		if(material.getId() == TEAMDIAMOND.getId()) {
			return "diamond";
		}
		if(material.getId() == TEAMIRON.getId()) {
			return "iron";
		}
		if(material.getId() == TEAMGOLD.getId()) {
			return "gold";
		}
		return null;
	}
}
