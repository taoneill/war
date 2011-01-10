package com.tommytony.war;

import org.bukkit.Material;

public class TeamMaterials {
	public static final Material TEAMDIAMOND = Material.DiamondBlock;
	public static final Material TEAMIRON = Material.IronBlock;
	public static final Material TEAMGOLD = Material.GoldBlock;
	
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
		if(material.getID() == TEAMDIAMOND.getID()) {
			return "diamond";
		}
		if(material.getID() == TEAMIRON.getID()) {
			return "iron";
		}
		if(material.getID() == TEAMGOLD.getID()) {
			return "gold";
		}
		return null;
	}
}
