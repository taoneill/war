package com.tommytony.war.config;

import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;

public enum TeamKind {
	WHITE (DyeColor.WHITE, Material.WHITE_WOOL, ChatColor.WHITE, 450),
	ORANGE (DyeColor.ORANGE, Material.ORANGE_WOOL, ChatColor.GOLD, 51),
	MAGENTA (DyeColor.MAGENTA, Material.MAGENTA_WOOL, ChatColor.LIGHT_PURPLE, 353),
	BLUE (DyeColor.LIGHT_BLUE, Material.LIGHT_BLUE_WOOL, ChatColor.BLUE, 23),
	GOLD (DyeColor.YELLOW, Material.YELLOW_WOOL, ChatColor.YELLOW, 403), // yellow = gold
	GREEN (DyeColor.LIME, Material.LIME_WOOL, ChatColor.GREEN, 612),
	PINK (DyeColor.PINK, Material.PINK_WOOL, ChatColor.LIGHT_PURPLE, 929),
	GRAY (DyeColor.GRAY, Material.GRAY_WOOL, ChatColor.DARK_GRAY, 600),
	IRON (DyeColor.GRAY, Material.GRAY_WOOL, ChatColor.GRAY, 154), // lightgrey = iron
	DIAMOND (DyeColor.CYAN, Material.CYAN_WOOL, ChatColor.DARK_AQUA, 738), // cyan = diamond
	PURPLE (DyeColor.PURPLE, Material.PURPLE_WOOL, ChatColor.DARK_PURPLE, 153),
	NAVY (DyeColor.BLUE, Material.BLUE_WOOL, ChatColor.DARK_BLUE, 939),
	BROWN (DyeColor.BROWN, Material.BROWN_WOOL, ChatColor.DARK_RED, 908),
	DARKGREEN (DyeColor.GREEN, Material.GREEN_WOOL, ChatColor.DARK_GREEN, 612),
	RED (DyeColor.RED, Material.RED_WOOL, ChatColor.RED, 245),
	BLACK (DyeColor.BLACK, Material.BLACK_WOOL, ChatColor.BLACK, 0);

	private final DyeColor dyeColor;
	private final ChatColor chatColor;
	private final Material material;
	private final int potionEffectColor;

	TeamKind(DyeColor blockHeadColor, Material material, ChatColor color, int potionEffectColor) {
		this.dyeColor = blockHeadColor;
		this.material = material;
		this.chatColor = color;
		this.potionEffectColor = potionEffectColor;
	}

	public static TeamKind teamKindFromString(String str) {
		String lowered = str.toLowerCase();
		for (TeamKind kind : TeamKind.values()) {
			if (kind.toString().startsWith(lowered)) {
				return kind;
			}
		}
		return null;
	}

	public static TeamKind getTeam(String teamName) {
		for (TeamKind team : TeamKind.values()) {
			if (team.toString().equalsIgnoreCase(teamName)) {
				return team;
			}
		}
		return null;
	}

	/**
	 * Get wool block data for the dye color.
	 * @deprecated TODO remove all spout craft support
	 * @return wool color data value
	 */
	public byte getData() {
		return this.dyeColor.getWoolData();
	}

	/**
	 * Get the color of this team in chat messages.
	 *
	 * @return team chat color.
	 */
	public ChatColor getColor() {
		return this.chatColor;
	}

	/**
	 * Get the color of the wool block as a bukkit color.
	 *
	 * @return wool block color.
	 */
	public org.bukkit.Color getBukkitColor() {
		return this.dyeColor.getColor();
	}

	/**
	 * Get head block material.
	 *
	 * @return team head block material.
	 */
	public Material getMaterial() {
		return this.material;
	}

	@Override
	public String toString() {
		return super.toString().toLowerCase();
	}

	/**
	 * Get color of the team's potion effect, for thieves.
	 *
	 * @return potion effect color.
	 */
	public int getPotionEffectColor() {
		return this.potionEffectColor;
	}

	/**
	 * Get a single item of this team's wool head block.
	 *
	 * @return single block head item.
	 */
	public ItemStack getBlockHead() {
		return new ItemStack(this.material, 1);
	}

	/**
	 * Check if a block is this team's color block.
	 *
	 * @param block Wool block to check.
	 * @return true if block is this team's color.
	 */
	public boolean isTeamBlock(BlockState block) {
		return block.getType() == material;
	}

	/**
	 * Check if an item is this team's color block.
	 *
	 * @param item Wool item to check.
	 * @return true if item is this team's color.
	 */
	public boolean isTeamItem(ItemStack item) {
		return item.getType() == material;
	}

	public String getFormattedName() {
		return this.getColor() + this.name().toLowerCase() + ChatColor.WHITE;
	}

	public String getCapsName() {
		return String.valueOf(name().charAt(0)) + name().substring(1).toLowerCase();
	}

	/**
	 * Get a colored hat item for the team.
	 * @return Hat item with the team's color.
	 */
	public ItemStack getHat() {
		ItemStack helmet = new ItemStack(Material.LEATHER_HELMET);
		LeatherArmorMeta meta = (LeatherArmorMeta) helmet.getItemMeta();
		meta.setColor(this.getBukkitColor());
		helmet.setItemMeta(meta);
		return helmet;
	}
}
