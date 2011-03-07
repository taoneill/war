package com.tommytony.war;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Material;

/**
 * 
 * @author tommytony
 *
 */
public class TeamKinds {
	private static final List<TeamKind> teamKinds = new ArrayList<TeamKind>();  

	static {
		getTeamkinds().add(new TeamKind("white", Material.WOOL, (byte) 0, ChatColor.WHITE));
		getTeamkinds().add(new TeamKind("orange", Material.WOOL, (byte) 1, ChatColor.GOLD));
		getTeamkinds().add(new TeamKind("magenta", Material.WOOL, (byte) 2, ChatColor.LIGHT_PURPLE));
		getTeamkinds().add(new TeamKind("blue", Material.WOOL, (byte) 3, ChatColor.BLUE));
		getTeamkinds().add(new TeamKind("gold", Material.WOOL, (byte) 4, ChatColor.YELLOW)); // yellow = gold
		getTeamkinds().add(new TeamKind("green", Material.WOOL, (byte) 5, ChatColor.GREEN));
		getTeamkinds().add(new TeamKind("pink", Material.WOOL, (byte) 6, ChatColor.WHITE));
		getTeamkinds().add(new TeamKind("gray", Material.WOOL, (byte) 7, ChatColor.DARK_GRAY));
		getTeamkinds().add(new TeamKind("iron", Material.WOOL, (byte) 8, ChatColor.GRAY)); // lightgrey = iron
		getTeamkinds().add(new TeamKind("diamond", Material.WOOL, (byte) 9, ChatColor.DARK_AQUA)); // cyan = diamond
		getTeamkinds().add(new TeamKind("purple", Material.WOOL, (byte) 10, ChatColor.DARK_PURPLE));
		getTeamkinds().add(new TeamKind("navy", Material.WOOL, (byte) 11, ChatColor.DARK_BLUE));
		getTeamkinds().add(new TeamKind("brown", Material.WOOL, (byte) 12, ChatColor.DARK_RED));
		getTeamkinds().add(new TeamKind("darkgreen", Material.WOOL, (byte) 13, ChatColor.DARK_GREEN));
		getTeamkinds().add(new TeamKind("red", Material.WOOL, (byte) 14, ChatColor.RED));
		getTeamkinds().add(new TeamKind("black", Material.WOOL, (byte) 15, ChatColor.BLACK));
	}
	
	public static TeamKind teamKindFromString(String str) {
		String lowered = str.toLowerCase();
		for(TeamKind kind : getTeamkinds()) {
			if(kind.getDefaultName().startsWith(str)) {
				return kind;
			}
		}
		return null;
	}

	public static List<TeamKind> getTeamkinds() {
		return teamKinds;
	}
}

