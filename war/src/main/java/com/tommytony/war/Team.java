package com.tommytony.war;

import com.tommytony.war.config.*;
import com.tommytony.war.spout.SpoutDisplayer;
import com.tommytony.war.utility.Direction;
import com.tommytony.war.volume.Volume;
import org.apache.commons.lang.Validate;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Sign;
import org.getspout.spoutapi.SpoutManager;
import org.getspout.spoutapi.player.SpoutPlayer;
import org.kitteh.tag.TagAPI;

import java.io.File;
import java.text.MessageFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Level;

/**
 *
 * @author tommytony
 *
 */
public class Team {
	private final Warzone warzone;
	Random teamSpawnRandomizer = new Random();
	private List<Player> players = new ArrayList<Player>();
	private List<Player> teamChatPlayers = new ArrayList<Player>();
	private List<Location> teamSpawns;
	private Location teamFlag = null;
	private String name;
	private int remainingLives;
	private int points = 0;
	private Map<Location, Volume> spawnVolumes;
	private Volume flagVolume;
	private TeamKind kind;
	private TeamConfigBag teamConfig;
	private InventoryBag inventories;

	public Team(String name, TeamKind kind, List<Location> teamSpawn, Warzone warzone) {
		this.warzone = warzone;
		this.teamConfig = new TeamConfigBag(warzone);
		this.inventories = new InventoryBag(warzone);	// important constructors for cascading configs
		this.setName(name);
		this.teamSpawns = new ArrayList<Location>(teamSpawn);
		this.spawnVolumes = new HashMap<Location, Volume>();
		for (Location spawn : teamSpawn) {
			this.setSpawnVolume(spawn, new Volume(name + teamSpawns.indexOf(spawn), warzone.getWorld()));
		}
		this.kind = kind;
		this.setFlagVolume(null); // no flag at the start
	}

	public static Team getTeamByPlayerName(String playerName) {
		for (Warzone warzone : War.war.getWarzones()) {
			Team team = warzone.getPlayerTeam(playerName);
			if (team != null) {
				return team;
			}
		}
		return null;
	}

	public Warzone getZone() {
		return this.warzone;
	}

	public TeamKind getKind() {
		return this.kind;
	}

	private void createSpawnVolume(Location teamSpawn) {
		Volume spawnVolume = this.spawnVolumes.get(teamSpawn);
		if (spawnVolume.isSaved()) {
			spawnVolume.resetBlocks();
		}
		int x = teamSpawn.getBlockX();
		int y = teamSpawn.getBlockY();
		int z = teamSpawn.getBlockZ();

		TeamSpawnStyle style = this.getTeamConfig().resolveSpawnStyle();
		if (style.equals(TeamSpawnStyle.INVISIBLE)) {
			spawnVolume.setCornerOne(this.warzone.getWorld().getBlockAt(x, y - 1, z));
			spawnVolume.setCornerTwo(this.warzone.getWorld().getBlockAt(x, y + 3, z));
		} else if (style.equals(TeamSpawnStyle.SMALL)) {
			spawnVolume.setCornerOne(this.warzone.getWorld().getBlockAt(x - 1, y - 1, z - 1));
			spawnVolume.setCornerTwo(this.warzone.getWorld().getBlockAt(x + 1, y + 3, z + 1));
		} else {
			// flat or big
			spawnVolume.setCornerOne(this.warzone.getWorld().getBlockAt(x - 2, y - 1, z - 2));
			spawnVolume.setCornerTwo(this.warzone.getWorld().getBlockAt(x + 2, y + 3, z + 2));
		}
	}

	public void initializeTeamSpawns() {
		for (Location teamSpawn : this.spawnVolumes.keySet()) {
			initializeTeamSpawn(teamSpawn);
		}
	}

	public void initializeTeamSpawn(Location teamSpawn) {
		// Set the spawn
		int x = teamSpawn.getBlockX();
		int y = teamSpawn.getBlockY();
		int z = teamSpawn.getBlockZ();

		ItemStack light = this.warzone.getWarzoneMaterials().getLightBlock();

		TeamSpawnStyle style = this.getTeamConfig().resolveSpawnStyle();
		if (!style.equals(TeamSpawnStyle.INVISIBLE)) {
			// first ring
			this.setBlock(x + 1, y - 1, z + 1, this.kind);
			this.setBlock(x + 1, y - 1, z, this.kind);
			this.setBlock(x + 1, y - 1, z - 1, this.kind);
			this.setBlock(x, y - 1, z + 1, this.kind);
			BlockState lightBlock = this.warzone.getWorld().getBlockAt(x, y - 1, z).getState();
			lightBlock.setType(light.getType());
			lightBlock.setData(light.getData());
			lightBlock.update(true);
			this.setBlock(x, y - 1, z - 1, this.kind);
			this.setBlock(x - 1, y - 1, z + 1, this.kind);
			this.setBlock(x - 1, y - 1, z, this.kind);
			this.setBlock(x - 1, y - 1, z - 1, this.kind);
		}

		// Orientation
		int yaw = 0;
		if (teamSpawn.getYaw() >= 0) {
			yaw = (int) (teamSpawn.getYaw() % 360);
		} else {
			yaw = (int) (360 + (teamSpawn.getYaw() % 360));
		}
		Block signBlock = null;
		BlockFace signDirection = null;

		if (style.equals(TeamSpawnStyle.SMALL)) {
			// SMALL style
			if (yaw >= 0 && yaw < 90) {
				signDirection = BlockFace.SOUTH_WEST.getOppositeFace();
				signBlock = this.warzone.getWorld().getBlockAt(x, y, z).getRelative(Direction.NORTH()).getRelative(Direction.WEST());
			} else if (yaw >= 90 && yaw <= 180) {
				signDirection = BlockFace.NORTH_WEST.getOppositeFace();
				signBlock = this.warzone.getWorld().getBlockAt(x, y, z).getRelative(Direction.NORTH()).getRelative(Direction.EAST());
			} else if (yaw >= 180 && yaw < 270) {
				signDirection = BlockFace.NORTH_EAST.getOppositeFace();
				signBlock = this.warzone.getWorld().getBlockAt(x, y, z).getRelative(Direction.SOUTH()).getRelative(Direction.EAST());
			} else if (yaw >= 270 && yaw <= 360) {
				signDirection = BlockFace.SOUTH_EAST.getOppositeFace();
				signBlock = this.warzone.getWorld().getBlockAt(x, y, z).getRelative(Direction.SOUTH()).getRelative(Direction.WEST());
			}
		} else if (!style.equals(TeamSpawnStyle.INVISIBLE)) {
			// outer ring (FLAT or BIG)
			this.setBlock(x + 2, y - 1, z + 2, this.kind);
			this.setBlock(x + 2, y - 1, z + 1, this.kind);
			this.setBlock(x + 2, y - 1, z, this.kind);
			this.setBlock(x + 2, y - 1, z - 1, this.kind);
			this.setBlock(x + 2, y - 1, z - 2, this.kind);

			this.setBlock(x - 1, y - 1, z + 2, this.kind);
			this.setBlock(x - 1, y - 1, z - 2, this.kind);

			this.setBlock(x, y - 1, z + 2, this.kind);
			this.setBlock(x, y - 1, z - 2, this.kind);

			this.setBlock(x + 1, y - 1, z + 2, this.kind);
			this.setBlock(x + 1, y - 1, z - 2, this.kind);

			this.setBlock(x - 2, y - 1, z + 2, this.kind);
			this.setBlock(x - 2, y - 1, z + 1, this.kind);
			this.setBlock(x - 2, y - 1, z, this.kind);
			this.setBlock(x - 2, y - 1, z - 1, this.kind);
			this.setBlock(x - 2, y - 1, z - 2, this.kind);

			if (yaw >= 0 && yaw < 90) {
				signDirection = BlockFace.SOUTH_WEST.getOppositeFace();
				signBlock = this.warzone.getWorld().getBlockAt(x, y, z).getRelative(Direction.NORTH(), 2).getRelative(Direction.WEST(), 2);

				if (style.equals(TeamSpawnStyle.BIG)) {
					// rim
					this.setBlock(x - 2, y, z - 1, this.kind);
					this.setBlock(x - 2, y, z - 2, this.kind);
					this.setBlock(x - 1, y, z - 2, this.kind);
					this.setBlock(x, y, z - 2, this.kind);
					this.setBlock(x + 1, y, z - 2, this.kind);
					this.setBlock(x + 2, y, z - 2, this.kind);
					this.setBlock(x + 2, y, z - 1, this.kind);
					this.setBlock(x + 2, y, z, this.kind);
					this.setBlock(x + 2, y, z + 1, this.kind);
					this.setBlock(x + 2, y, z + 2, this.kind);
					this.setBlock(x + 1, y, z + 2, this.kind);

					// tower
					this.setBlock(x, y + 1, z - 2, this.kind);
					this.setBlock(x + 1, y + 1, z - 2, this.kind);
					this.setBlock(x + 2, y + 1, z - 2, this.kind);
					this.setBlock(x + 2, y + 1, z - 1, this.kind);
					this.setBlock(x + 2, y + 1, z, this.kind);

					this.setBlock(x + 1, y + 2, z - 2, this.kind);
					this.setBlock(x + 2, y + 2, z - 2, this.kind);
					this.setBlock(x + 2, y + 2, z - 1, this.kind);

					this.setBlock(x + 2, y + 3, z - 2, this.kind);
				}
			} else if (yaw >= 90 && yaw <= 180) {
				signDirection = BlockFace.NORTH_WEST.getOppositeFace();
				signBlock = this.warzone.getWorld().getBlockAt(x, y, z).getRelative(Direction.NORTH(), 2).getRelative(Direction.EAST(), 2);
				if (style.equals(TeamSpawnStyle.BIG)) {
					// rim
					this.setBlock(x + 1, y, z - 2, this.kind);
					this.setBlock(x + 2, y, z - 2, this.kind);
					this.setBlock(x + 2, y, z - 1, this.kind);
					this.setBlock(x + 2, y, z, this.kind);
					this.setBlock(x + 2, y, z + 1, this.kind);
					this.setBlock(x + 2, y, z + 2, this.kind);
					this.setBlock(x + 1, y, z + 2, this.kind);
					this.setBlock(x, y, z + 2, this.kind);
					this.setBlock(x - 1, y, z + 2, this.kind);
					this.setBlock(x - 2, y, z + 2, this.kind);
					this.setBlock(x - 2, y, z + 1, this.kind);

					// tower
					this.setBlock(x + 2, y + 1, z, this.kind);
					this.setBlock(x + 2, y + 1, z + 1, this.kind);
					this.setBlock(x + 2, y + 1, z + 2, this.kind);
					this.setBlock(x + 1, y + 1, z + 2, this.kind);
					this.setBlock(x, y + 1, z + 2, this.kind);

					this.setBlock(x + 2, y + 2, z + 1, this.kind);
					this.setBlock(x + 2, y + 2, z + 2, this.kind);
					this.setBlock(x + 1, y + 2, z + 2, this.kind);

					this.setBlock(x + 2, y + 3, z + 2, this.kind);
				}
			} else if (yaw >= 180 && yaw < 270) {
				signDirection = BlockFace.NORTH_EAST.getOppositeFace();
				signBlock = this.warzone.getWorld().getBlockAt(x, y, z).getRelative(Direction.SOUTH(), 2).getRelative(Direction.EAST(), 2);
				if (style.equals(TeamSpawnStyle.BIG)) {
					// rim
					this.setBlock(x + 2, y, z + 1, this.kind);
					this.setBlock(x + 2, y, z + 2, this.kind);
					this.setBlock(x + 1, y, z + 2, this.kind);
					this.setBlock(x, y, z + 2, this.kind);
					this.setBlock(x - 1, y, z + 2, this.kind);
					this.setBlock(x - 2, y, z + 2, this.kind);
					this.setBlock(x - 2, y, z + 1, this.kind);
					this.setBlock(x - 2, y, z, this.kind);
					this.setBlock(x - 2, y, z - 1, this.kind);
					this.setBlock(x - 2, y, z - 2, this.kind);
					this.setBlock(x - 1, y, z - 2, this.kind);

					// tower
					this.setBlock(x, y + 1, z + 2, this.kind);
					this.setBlock(x - 1, y + 1, z + 2, this.kind);
					this.setBlock(x - 2, y + 1, z + 2, this.kind);
					this.setBlock(x - 2, y + 1, z + 1, this.kind);
					this.setBlock(x - 2, y + 1, z, this.kind);

					this.setBlock(x - 1, y + 2, z + 2, this.kind);
					this.setBlock(x - 2, y + 2, z + 2, this.kind);
					this.setBlock(x - 2, y + 2, z + 1, this.kind);

					this.setBlock(x - 2, y + 3, z + 2, this.kind);
				}
			} else if (yaw >= 270 && yaw <= 360) {
				signDirection = BlockFace.SOUTH_EAST.getOppositeFace();
				signBlock = this.warzone.getWorld().getBlockAt(x, y, z).getRelative(Direction.SOUTH(), 2).getRelative(Direction.WEST(), 2);
				if (style.equals(TeamSpawnStyle.BIG)) {
					// rim
					this.setBlock(x - 1, y, z + 2, this.kind);
					this.setBlock(x - 2, y, z + 2, this.kind);
					this.setBlock(x - 2, y, z + 1, this.kind);
					this.setBlock(x - 2, y, z, this.kind);
					this.setBlock(x - 2, y, z - 1, this.kind);
					this.setBlock(x - 2, y, z - 2, this.kind);
					this.setBlock(x - 1, y, z - 2, this.kind);
					this.setBlock(x, y, z - 2, this.kind);
					this.setBlock(x + 1, y, z - 2, this.kind);
					this.setBlock(x + 2, y, z - 2, this.kind);
					this.setBlock(x + 2, y, z - 1, this.kind);

					// tower
					this.setBlock(x - 2, y + 1, z, this.kind);
					this.setBlock(x - 2, y + 1, z - 1, this.kind);
					this.setBlock(x - 2, y + 1, z - 2, this.kind);
					this.setBlock(x - 1, y + 1, z - 2, this.kind);
					this.setBlock(x, y + 1, z - 2, this.kind);

					this.setBlock(x - 2, y + 2, z - 1, this.kind);
					this.setBlock(x - 2, y + 2, z - 2, this.kind);
					this.setBlock(x - 1, y + 2, z - 2, this.kind);

					this.setBlock(x - 2, y + 3, z - 2, this.kind);
				}
			}
		}

		if (signBlock != null) {
			String[] lines;
			if (this.getTeamConfig().resolveInt(TeamConfig.LIFEPOOL) == -1) {
				lines = MessageFormat
						.format(War.war.getString("sign.team.unlimited"),
								this.name,
								this.players.size(),
								this.getTeamConfig().resolveInt(
										TeamConfig.TEAMSIZE),
								this.points,
								this.getTeamConfig().resolveInt(
										TeamConfig.MAXSCORE)).split("\n");
			} else {
				lines = MessageFormat
						.format(War.war.getString("sign.team.limited"),
								this.name,
								this.players.size(),
								this.getTeamConfig().resolveInt(
										TeamConfig.TEAMSIZE),
								this.points,
								this.getTeamConfig().resolveInt(
										TeamConfig.MAXSCORE),
								this.remainingLives,
								this.getTeamConfig().resolveInt(
										TeamConfig.LIFEPOOL)).split("\n");
			}
			signBlock.setType(Material.SIGN_POST);
			org.bukkit.block.Sign block = (org.bukkit.block.Sign) signBlock
					.getState();
			org.bukkit.material.Sign data = (Sign) block.getData();
			data.setFacingDirection(signDirection);
			block.setData(data);
			for (int i = 0; i < 4; i++) {
				block.setLine(i, lines[i]);
			}
			block.update(true);
		}

		if (War.war.isSpoutServer()) {
			War.war.getSpoutDisplayer().updateStats(this.warzone);
		}
	}

	private void setBlock(int x, int y, int z, TeamKind kind) {
		BlockState block = this.warzone.getWorld().getBlockAt(x, y, z).getState();
		block.setType(kind.getMaterial());
		block.setData(kind.getBlockData());
		block.update(true);
	}

	public void addTeamSpawn(Location teamSpawn) {
		if (!this.teamSpawns.contains(teamSpawn)) {
			this.teamSpawns.add(teamSpawn);
		}
		// this resets the block to old state
		this.setSpawnVolume(teamSpawn, new Volume(name + teamSpawns.indexOf(teamSpawn), warzone.getWorld()));
		this.createSpawnVolume(teamSpawn);
		this.spawnVolumes.get(teamSpawn).saveBlocks();

		this.initializeTeamSpawn(teamSpawn);
	}

	public List<Location> getTeamSpawns() {
		return this.teamSpawns;
	}

	public Location getRandomSpawn() {
		return this.teamSpawns.get(teamSpawnRandomizer.nextInt(this.teamSpawns.size()));
	}

	public void addPlayer(Player player) {
		this.players.add(player);
		if (War.war.isTagServer()) {
			TagAPI.refreshPlayer(player);
		}
		if (this.warzone.getScoreboard() != null && this.warzone.getScoreboardType() != ScoreboardType.NONE) {
			player.setScoreboard(this.warzone.getScoreboard());
		}
		warzone.updateScoreboard();
	}

	public List<Player> getPlayers() {
		return this.players;
	}
	
	public void teamcast(String message) {
		// by default a teamcast is a notification
		teamcast(message, true);
	}

	public void teamcast(String message, boolean isNotification) {
		for (Player player : this.players) {
			if (War.war.isSpoutServer()) {
				SpoutPlayer sp = SpoutManager.getPlayer(player);
				if (sp.isSpoutCraftEnabled() && isNotification) {
					// team notifications go to the top left for Spout players to lessen War spam in chat box
					War.war.getSpoutDisplayer().msg(sp, message);
				} else {
					War.war.msg(player, message);
				}
			} else {
				War.war.msg(player, message);
			}
		}
	}

	public void teamcast(String message, Object... args) {
		// by default a teamcast is a notification
		teamcast(message, true, args);
	}

	public void teamcast(String message, boolean isNotification, Object... args) {
		for (Player player : this.players) {
			if (War.war.isSpoutServer()) {
				SpoutPlayer sp = SpoutManager.getPlayer(player);
				if (sp.isSpoutCraftEnabled() && isNotification) {
					// team notifications go to the top left for Spout players to lessen War spam in chat box
					War.war.getSpoutDisplayer().msg(sp, MessageFormat.format(message, args));
				} else {
					War.war.msg(player, message, args);
				}
			} else {
				War.war.msg(player, message, args);
			}
		}
	}

	/**
	 * Send an achievement to all players on the team.
	 * Currently implemented using SpoutCraft.
	 * @param line1 Achievement first line
	 * @param line2 Achievement second line
	 * @param icon Item to display in the achievement
	 * @param ticks Duration the achievement should be displayed
	 */
	public void sendAchievement(String line1, String line2, ItemStack icon, int ticks) {
		if (!War.war.isSpoutServer())
			return;
		line1 = SpoutDisplayer.cleanForNotification(line1);
		line2 = SpoutDisplayer.cleanForNotification(line2);
		for (Player player : this.players) {
			SpoutPlayer spoutPlayer = SpoutManager.getPlayer(player);
			if (!spoutPlayer.isSpoutCraftEnabled())
				continue;
			spoutPlayer.sendNotification(line1, line2, icon, ticks);
		}
	}
	
	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void removePlayer(Player thePlayer) {
		this.players.remove(thePlayer);
		synchronized (teamChatPlayers) {
			this.teamChatPlayers.remove(thePlayer);
		}
		this.warzone.dropAllStolenObjects(thePlayer, false);
		if (War.war.isTagServer()) {
			TagAPI.refreshPlayer(thePlayer);
		}
		if (War.war.isSpoutServer()) {
			War.war.getSpoutDisplayer().updateStats(thePlayer);
		}
		thePlayer.setFireTicks(0);
		thePlayer.setRemainingAir(300);
		if (!this.warzone.getReallyDeadFighters().contains(thePlayer.getName())) {
			this.warzone.restorePlayerState(thePlayer);
		}
		this.warzone.getLoadoutSelections().remove(thePlayer);
		warzone.updateScoreboard();
	}

	public int getRemainingLives() {
		return this.remainingLives;
	}

	public void setRemainingLives(int remainingLives) {
		this.remainingLives = remainingLives;
		warzone.updateScoreboard();
	}

	public void addPoint() {
		boolean atLeastOnePlayerOnTeam = this.players.size() != 0;
		boolean atLeastOnePlayerOnOtherTeam = false;
		for (Team team : this.warzone.getTeams()) {
			if (!team.getName().equals(this.getName()) && team.getPlayers().size() > 0) {
				atLeastOnePlayerOnOtherTeam = true;
			}
		}
		if (atLeastOnePlayerOnTeam && atLeastOnePlayerOnOtherTeam) {
			this.points++;
		} else if (!atLeastOnePlayerOnOtherTeam) {
			this.teamcast("zone.score.empty");
		}
		this.warzone.updateScoreboard();
	}

	public int getPoints() {
		return this.points;
	}

	public Map<Location, Volume> getSpawnVolumes() {

		return this.spawnVolumes;
	}

	public void resetSign() {
		for (Entry<Location, Volume> spawnEntry : this.getSpawnVolumes().entrySet()) {
			spawnEntry.getValue().resetBlocks();
			this.initializeTeamSpawn(spawnEntry.getKey()); // reset everything instead of just sign
		}
		if (this.warzone.getLobby() != null) {
			this.warzone.getLobby().resetTeamGateSign(this);
		}
		if (War.war.getWarHub() != null) {
			War.war.getWarHub().resetZoneSign(warzone);
		}
	}

	public void setSpawnVolume(Location spawnLocation, Volume volume) {
		this.spawnVolumes.put(spawnLocation, volume);
	}

	public void resetPoints() {
		this.points = 0;
		warzone.updateScoreboard();
	}

	public Volume getFlagVolume() {
		return this.flagVolume;
	}

	public void setFlagVolume(Volume flagVolume) {
		this.flagVolume = flagVolume;
	}

	private void setFlagVolume() {
		if (this.flagVolume == null) {
			this.flagVolume = new Volume(this.getName() + "flag", this.warzone.getWorld());
		}
		if (this.flagVolume.isSaved()) {
			this.flagVolume.resetBlocks();
		}
		int x = this.teamFlag.getBlockX();
		int y = this.teamFlag.getBlockY();
		int z = this.teamFlag.getBlockZ();
		this.flagVolume.setCornerOne(this.warzone.getWorld().getBlockAt(x - 1, y - 1, z - 1));
		this.flagVolume.setCornerTwo(this.warzone.getWorld().getBlockAt(x + 1, y + 3, z + 1));
	}

	public void initializeTeamFlag() {
		// make air (old two-high above floor)
		Volume airGap = new Volume(new Location(this.flagVolume.getWorld(),
				this.flagVolume.getCornerOne().getX(), this.flagVolume
						.getCornerOne().getY() + 1, this.flagVolume
						.getCornerOne().getZ()), new Location(
				this.flagVolume.getWorld(), this.flagVolume.getCornerTwo()
						.getX(), this.flagVolume.getCornerOne().getY() + 2,
				this.flagVolume.getCornerTwo().getZ()));
		airGap.setToMaterial(Material.AIR);

		// Set the flag blocks
		int x = this.teamFlag.getBlockX();
		int y = this.teamFlag.getBlockY();
		int z = this.teamFlag.getBlockZ();

		// first ring
		BlockState current = this.warzone.getWorld().getBlockAt(x + 1, y - 1, z + 1).getState();
		current.setType(this.warzone.getWarzoneMaterials().getMainBlock().getType());
		current.setData(this.warzone.getWarzoneMaterials().getMainBlock().getData());
		current.update(true);
		current = this.warzone.getWorld().getBlockAt(x + 1, y - 1, z).getState();
		current.setType(this.warzone.getWarzoneMaterials().getMainBlock().getType());
		current.setData(this.warzone.getWarzoneMaterials().getMainBlock().getData());
		current.update(true);
		current = this.warzone.getWorld().getBlockAt(x + 1, y - 1, z - 1).getState();
		current.setType(this.warzone.getWarzoneMaterials().getMainBlock().getType());
		current.setData(this.warzone.getWarzoneMaterials().getMainBlock().getData());
		current.update(true);
		current = this.warzone.getWorld().getBlockAt(x, y - 1, z + 1).getState();
		current.setType(this.warzone.getWarzoneMaterials().getMainBlock().getType());
		current.setData(this.warzone.getWarzoneMaterials().getMainBlock().getData());
		current.update(true);
		current = this.warzone.getWorld().getBlockAt(x, y - 1, z).getState();
		current.setType(this.warzone.getWarzoneMaterials().getLightBlock().getType());
		current.setData(this.warzone.getWarzoneMaterials().getLightBlock().getData());
		current.update(true);
		current = this.warzone.getWorld().getBlockAt(x, y - 1, z - 1).getState();
		current.setType(this.warzone.getWarzoneMaterials().getMainBlock().getType());
		current.setData(this.warzone.getWarzoneMaterials().getMainBlock().getData());
		current.update(true);
		current = this.warzone.getWorld().getBlockAt(x - 1, y - 1, z + 1).getState();
		current.setType(this.warzone.getWarzoneMaterials().getMainBlock().getType());
		current.setData(this.warzone.getWarzoneMaterials().getMainBlock().getData());
		current.update(true);
		current = this.warzone.getWorld().getBlockAt(x - 1, y - 1, z).getState();
		current.setType(this.warzone.getWarzoneMaterials().getMainBlock().getType());
		current.setData(this.warzone.getWarzoneMaterials().getMainBlock().getData());
		current.update(true);
		current = this.warzone.getWorld().getBlockAt(x - 1, y - 1, z - 1).getState();
		current.setType(this.warzone.getWarzoneMaterials().getMainBlock().getType());
		current.setData(this.warzone.getWarzoneMaterials().getMainBlock().getData());
		current.update(true);

		// flag
		BlockState flagBlock = this.warzone.getWorld().getBlockAt(x, y + 1, z).getState();
		flagBlock.setType(this.kind.getMaterial());
		flagBlock.setData(this.kind.getBlockData());
		flagBlock.update(true);

		// Flag post using Orientation
		int yaw = 0;
		if (this.teamFlag.getYaw() >= 0) {
			yaw = (int) (this.teamFlag.getYaw() % 360);
		} else {
			yaw = (int) (360 + (this.teamFlag.getYaw() % 360));
		}
		if ((yaw >= 0 && yaw < 45) || (yaw >= 315 && yaw <= 360)) {
			current = this.warzone.getWorld().getBlockAt(x, y, z - 1).getState();
			current.setType(this.warzone.getWarzoneMaterials().getStandBlock().getType());
			current.setData(this.warzone.getWarzoneMaterials().getStandBlock().getData());
			current.update(true);
			current = this.warzone.getWorld().getBlockAt(x, y + 1, z - 1).getState();
			current.setType(this.warzone.getWarzoneMaterials().getStandBlock().getType());
			current.setData(this.warzone.getWarzoneMaterials().getStandBlock().getData());
			current.update(true);
		} else if (yaw >= 45 && yaw < 135) {
			current = this.warzone.getWorld().getBlockAt(x + 1, y, z).getState();
			current.setType(this.warzone.getWarzoneMaterials().getStandBlock().getType());
			current.setData(this.warzone.getWarzoneMaterials().getStandBlock().getData());
			current.update(true);
			current = this.warzone.getWorld().getBlockAt(x + 1, y + 1, z).getState();
			current.setType(this.warzone.getWarzoneMaterials().getStandBlock().getType());
			current.setData(this.warzone.getWarzoneMaterials().getStandBlock().getData());
			current.update(true);
		} else if (yaw >= 135 && yaw < 225) {
			current = this.warzone.getWorld().getBlockAt(x, y, z + 1).getState();
			current.setType(this.warzone.getWarzoneMaterials().getStandBlock().getType());
			current.setData(this.warzone.getWarzoneMaterials().getStandBlock().getData());
			current.update(true);
			current = this.warzone.getWorld().getBlockAt(x, y + 1, z + 1).getState();
			current.setType(this.warzone.getWarzoneMaterials().getStandBlock().getType());
			current.setData(this.warzone.getWarzoneMaterials().getStandBlock().getData());
			current.update(true);
		} else if (yaw >= 225 && yaw < 315) {
			current = this.warzone.getWorld().getBlockAt(x - 1, y, z).getState();
			current.setType(this.warzone.getWarzoneMaterials().getStandBlock().getType());
			current.setData(this.warzone.getWarzoneMaterials().getStandBlock().getData());
			current.update(true);
			current = this.warzone.getWorld().getBlockAt(x - 1, y + 1, z).getState();
			current.setType(this.warzone.getWarzoneMaterials().getStandBlock().getType());
			current.setData(this.warzone.getWarzoneMaterials().getStandBlock().getData());
			current.update(true);
		}
	}

	public boolean isTeamFlagBlock(Block block) {
		if (this.teamFlag != null) {
			int flagX = this.teamFlag.getBlockX();
			int flagY = this.teamFlag.getBlockY() + 1;
			int flagZ = this.teamFlag.getBlockZ();
			if (block.getX() == flagX && block.getY() == flagY && block.getZ() == flagZ) {
				return true;
			}
		}
		return false;
	}

	public Location getTeamFlag() {
		return this.teamFlag;
	}

	public void setTeamFlag(Location teamFlag) {
		this.teamFlag = teamFlag;

		// this resets the block to old state
		this.setFlagVolume();
		this.getFlagVolume().saveBlocks();

		this.initializeTeamFlag();
	}
	
	public void deleteTeamFlag() {
		this.getFlagVolume().resetBlocks();
		this.setFlagVolume(null);
		this.teamFlag = null;
		
		// remove volume file
		String filePath = War.war.getDataFolder().getPath() + "/dat/warzone-" + this.warzone.getName() + "/volume-" + this.getName() + "flag.dat";
		if (!new File(filePath).delete()) {			
			War.war.log("Failed to delete file " + filePath, Level.WARNING);
		}
	}

	public InventoryBag getInventories() {
		return this.inventories ;
	}

	public TeamConfigBag getTeamConfig() {
		return this.teamConfig;
	}

	/**
	 * Check if any team spawns contain a certain location.
	 *
	 * @param loc Location to check if contained by a spawn.
	 * @return true if loc is part of a spawn volume, false otherwise.
	 */
	public boolean isSpawnLocation(Location loc) {
		for (Volume spawnVolume : this.spawnVolumes.values()) {
			if (spawnVolume.contains(loc)) {
				return true;
			}
		}
		
		return false;
	}

	public boolean isFull() {
		return this.getPlayers().size() == this.getTeamConfig().resolveInt(TeamConfig.TEAMSIZE);
	}

	/**
	 * Get an array of player usernames for players on this team.
	 *
	 * @return array of usernames.
	 */
	public List<String> getPlayerNames() {
		List<String> ret = new ArrayList<String>(this.players.size());
		for (Player player : this.players) {
			ret.add(player.getName());
		}
		return ret;
	}

	/**
	 * Check if a player on this team can modify a certain type of block defined in the block whitelist.
	 *
	 * @param type Type of block to check.
	 * @return true if this block can be modified, false otherwise.
	 */
	public boolean canModify(Material type) {
		for (String whitelistedBlock : this.getTeamConfig()
				.resolveString(TeamConfig.BLOCKWHITELIST).split(",")) {
			if (whitelistedBlock.equalsIgnoreCase("all")) {
				return true;
			}
			if (type.toString().equalsIgnoreCase(whitelistedBlock)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Send a team chat message with proper formatting.
	 *
	 * @param sender Player sending the message
	 * @param message Message to send
	 */
	public void sendTeamChatMessage(OfflinePlayer sender, String message) {
		String player = this.getKind().getColor() + ChatColor.stripColor(sender.getName()) + ChatColor.WHITE;
		String output = String.format("%s: %s", player, message);
		teamcast(output, false);
		War.war.getLogger().info("[TeamChat] " + output);
	}

	/**
	 * Check if a player on this team has toggled on team chat. Thread safe.
	 *
	 * @param player Player to check
	 * @return true if the player has toggled on team chat
	 */
	public boolean isInTeamChat(Player player) {
		synchronized (teamChatPlayers) {
			return this.teamChatPlayers.contains(player);
		}
	}

	/**
	 * Add a player to team chat. Thread safe.
	 * @param player Player to add
	 * @throws IllegalArgumentException Player is already in team chat
	 */
	public void addTeamChatPlayer(Player player) {
		Validate.isTrue(!isInTeamChat(player), "Player is already in team chat");
		synchronized (teamChatPlayers) {
			this.teamChatPlayers.add(player);
		}
	}

	/**
	 * Remove a player from team chat. Thread safe.
	 *
	 * @param player Player to remove
	 */
	public void removeTeamChatPlayer(Player player) {
		synchronized (teamChatPlayers) {
			this.teamChatPlayers.remove(player);
		}
	}
}
