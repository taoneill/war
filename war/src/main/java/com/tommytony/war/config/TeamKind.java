package com.tommytony.war.config;

import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.material.MaterialData;
import org.bukkit.material.Wool;
import org.getspout.spoutapi.gui.Color;

public enum TeamKind {
	WHITE (DyeColor.WHITE, Material.WOOL, ChatColor.WHITE, 450),
	ORANGE (DyeColor.ORANGE, Material.WOOL, ChatColor.GOLD, 51),
	MAGENTA (DyeColor.MAGENTA, Material.WOOL, ChatColor.LIGHT_PURPLE, 353),
	BLUE (DyeColor.LIGHT_BLUE, Material.WOOL, ChatColor.BLUE, 23),
	GOLD (DyeColor.YELLOW, Material.WOOL, ChatColor.YELLOW, 403), // yellow = gold
	GREEN (DyeColor.LIME, Material.WOOL, ChatColor.GREEN, 612),
	PINK (DyeColor.PINK, Material.WOOL, ChatColor.LIGHT_PURPLE, 929),
	GRAY (DyeColor.GRAY, Material.WOOL, ChatColor.DARK_GRAY, 600),
	IRON (DyeColor.SILVER, Material.WOOL, ChatColor.GRAY, 154), // lightgrey = iron
	DIAMOND (DyeColor.CYAN, Material.WOOL, ChatColor.DARK_AQUA, 738), // cyan = diamond
	PURPLE (DyeColor.PURPLE, Material.WOOL, ChatColor.DARK_PURPLE, 153),
	NAVY (DyeColor.BLUE, Material.WOOL, ChatColor.DARK_BLUE, 939),
	BROWN (DyeColor.BROWN, Material.WOOL, ChatColor.DARK_RED, 908),
	DARKGREEN (DyeColor.GREEN, Material.WOOL, ChatColor.DARK_GREEN, 612),
	RED (DyeColor.RED, Material.WOOL, ChatColor.RED, 245),
	BLACK (DyeColor.BLACK, Material.WOOL, ChatColor.BLACK, 0);

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
	 *
	 * @return wool color data value
	 */
	@SuppressWarnings("deprecation")
	public byte getData() {
		return this.dyeColor.getWoolData();
	}

	/**
	 * Get the color of the wool head block.
	 *
	 * @return head wool color.
	 */
	public DyeColor getDyeColor() {
		return this.dyeColor;
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
	 * Get the color of this team in the Spout client GUI.
	 *
	 * @return spout chat GUI color
	 */
	public Color getSpoutColor() {
		return new org.getspout.spoutapi.gui.Color(
				dyeColor.getColor().getRed(), dyeColor.getColor().getGreen(),
				dyeColor.getColor().getBlue());
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
	 * Get head block material. Should always be {@link Material#WOOL}.
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
	 * Get a single item of this team's wool head block. Creates a single block
	 * with data from {@link #getBlockData()}.
	 *
	 * @return single block head item.
	 */
	public ItemStack getBlockHead() {
		return new Wool(this.dyeColor).toItemStack(1);
	}

	/**
	 * Get wool head block data (for creating blocks).
	 *
	 * @return wool head block data.
	 */
	public MaterialData getBlockData() {
		return new Wool(this.dyeColor);
	}

	/**
	 * Check if a block is this team's color block.
	 *
	 * @param block Wool block to check.
	 * @return true if block is this team's color.
	 */
	public boolean isTeamBlock(BlockState block) {
		if (block.getType() != Material.WOOL || !(block.getData() instanceof Wool)) {
			return false;
		}
		Wool wool = (Wool) block.getData();
		return wool.getColor() == dyeColor;
	}

	/**
	 * Check if an item is this team's color block.
	 *
	 * @param item Wool item to check.
	 * @return true if item is this team's color.
	 */
	public boolean isTeamItem(ItemStack item) {
		if (item.getType() != Material.WOOL || !(item.getData() instanceof Wool)) {
			return false;
		}
		Wool wool = (Wool) item.getData();
		return wool.getColor() == dyeColor;
	}

	/**
	 * Check if a block data is this team's block data.
	 *
	 * @param data Wool block data.
	 * @return true if data is this team's data.
	 */
	public boolean isTeamBlock(MaterialData data) {
		return data instanceof Wool && ((Wool)data).getColor() == this.dyeColor;
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
