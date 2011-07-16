package com.tommytony.war;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import bukkit.tommytony.war.War;

import com.tommytony.war.utils.SignHelper;
import com.tommytony.war.volumes.Volume;

/**
 *
 * @author tommytony
 *
 */
public class Team {
	private List<Player> players = new ArrayList<Player>();
	private Location teamSpawn = null;
	private Location teamFlag = null;
	private String name;
	private int remainingLives;
	private int points = 0;
	private Volume spawnVolume;
	private Volume flagVolume;
	private final Warzone warzone;
	private TeamKind kind;
	private War war;

	public Team(String name, TeamKind kind, Location teamSpawn, War war, Warzone warzone) {
		this.warzone = warzone;
		this.setName(name);
		this.teamSpawn = teamSpawn;
		this.war = war;
		this.setSpawnVolume(new Volume(name, war, warzone.getWorld()));
		this.kind = kind;
		this.setFlagVolume(null); // no flag at the start
	}

	public TeamKind getKind() {
		return this.kind;
	}

	private void setSpawnVolume() {
		if (this.spawnVolume.isSaved()) {
			this.spawnVolume.resetBlocks();
		}
		int x = this.teamSpawn.getBlockX();
		int y = this.teamSpawn.getBlockY();
		int z = this.teamSpawn.getBlockZ();

		if (this.warzone.getSpawnStyle().equals(TeamSpawnStyles.INVISIBLE)) {
			this.spawnVolume.setCornerOne(this.warzone.getWorld().getBlockAt(x, y - 1, z));
			this.spawnVolume.setCornerTwo(this.warzone.getWorld().getBlockAt(x, y + 3, z));
		} else if (this.warzone.getSpawnStyle().equals(TeamSpawnStyles.SMALL)) {
			this.spawnVolume.setCornerOne(this.warzone.getWorld().getBlockAt(x - 1, y - 1, z - 1));
			this.spawnVolume.setCornerTwo(this.warzone.getWorld().getBlockAt(x + 1, y + 3, z + 1));
		} else {
			// flat or big
			this.spawnVolume.setCornerOne(this.warzone.getWorld().getBlockAt(x - 2, y - 1, z - 2));
			this.spawnVolume.setCornerTwo(this.warzone.getWorld().getBlockAt(x + 2, y + 3, z + 2));
		}
	}

	@SuppressWarnings("unused")
	public void initializeTeamSpawn() {
		// make air
		this.spawnVolume.setToMaterial(Material.AIR);

		// Set the spawn
		int x = this.teamSpawn.getBlockX();
		int y = this.teamSpawn.getBlockY();
		int z = this.teamSpawn.getBlockZ();

		if (this.warzone.getSpawnStyle().equals(TeamSpawnStyles.INVISIBLE)) {
			// nothing but glowstone
			this.warzone.getWorld().getBlockAt(x, y - 1, z).setType(Material.GLOWSTONE);
		} else {
			// first ring
			this.setBlock(x + 1, y - 1, z + 1, this.kind);
			this.setBlock(x + 1, y - 1, z, this.kind);
			this.setBlock(x + 1, y - 1, z - 1, this.kind);
			this.setBlock(x, y - 1, z + 1, this.kind);
			this.warzone.getWorld().getBlockAt(x, y - 1, z).setType(Material.GLOWSTONE);
			this.setBlock(x, y - 1, z - 1, this.kind);
			this.setBlock(x - 1, y - 1, z + 1, this.kind);
			this.setBlock(x - 1, y - 1, z, this.kind);
			this.setBlock(x - 1, y - 1, z - 1, this.kind);
		}

		// Orientation
		int yaw = 0;
		if (this.teamSpawn.getYaw() >= 0) {
			yaw = (int) (this.teamSpawn.getYaw() % 360);
		} else {
			yaw = (int) (360 + (this.teamSpawn.getYaw() % 360));
		}
		Block signBlock = null;
		int signData = 0;

		if (this.warzone.getSpawnStyle().equals(TeamSpawnStyles.INVISIBLE)) {
			// INVISIBLE style
			signBlock = this.warzone.getWorld().getBlockAt(x, y, z);
			if (yaw >= 0 && yaw < 90) {
				signData = 10;
			} else if (yaw >= 90 && yaw <= 180) {
				signData = 14;
			} else if (yaw >= 180 && yaw < 270) {
				signData = 2;
			} else if (yaw >= 270 && yaw <= 360) {
				signData = 6;
			}
		} else if (this.warzone.getSpawnStyle().equals(TeamSpawnStyles.SMALL)) {
			// SMALL style
			if (yaw >= 0 && yaw < 90) {
				signData = 10;
				signBlock = this.warzone.getWorld().getBlockAt(x, y, z).getFace(BlockFace.NORTH).getFace(BlockFace.WEST);
			} else if (yaw >= 90 && yaw <= 180) {
				signData = 14;
				signBlock = this.warzone.getWorld().getBlockAt(x, y, z).getFace(BlockFace.NORTH).getFace(BlockFace.EAST);
			} else if (yaw >= 180 && yaw < 270) {
				signData = 2;
				signBlock = this.warzone.getWorld().getBlockAt(x, y, z).getFace(BlockFace.SOUTH).getFace(BlockFace.EAST);
			} else if (yaw >= 270 && yaw <= 360) {
				signData = 6;
				signBlock = this.warzone.getWorld().getBlockAt(x, y, z).getFace(BlockFace.SOUTH).getFace(BlockFace.WEST);
			}
		} else {
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

			BlockFace facing = null;
			BlockFace opposite = null;
			if (yaw >= 0 && yaw < 90) {
				facing = BlockFace.NORTH_WEST;
				opposite = BlockFace.SOUTH_EAST;
				signData = 10;
				signBlock = this.warzone.getWorld().getBlockAt(x, y, z).getFace(BlockFace.NORTH, 2).getFace(BlockFace.WEST, 2);

				if (this.warzone.getSpawnStyle().equals(TeamSpawnStyles.BIG)) {
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
				facing = BlockFace.NORTH_EAST;
				opposite = BlockFace.SOUTH_WEST;
				signData = 14;
				signBlock = this.warzone.getWorld().getBlockAt(x, y, z).getFace(BlockFace.NORTH, 2).getFace(BlockFace.EAST, 2);
				if (this.warzone.getSpawnStyle().equals(TeamSpawnStyles.BIG)) {
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
				facing = BlockFace.SOUTH_EAST;
				opposite = BlockFace.NORTH_WEST;
				signData = 2;
				signBlock = this.warzone.getWorld().getBlockAt(x, y, z).getFace(BlockFace.SOUTH, 2).getFace(BlockFace.EAST, 2);
				if (this.warzone.getSpawnStyle().equals(TeamSpawnStyles.BIG)) {
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
				facing = BlockFace.SOUTH_WEST;
				opposite = BlockFace.NORTH_EAST;
				signData = 6;
				signBlock = this.warzone.getWorld().getBlockAt(x, y, z).getFace(BlockFace.SOUTH, 2).getFace(BlockFace.WEST, 2);
				if (this.warzone.getSpawnStyle().equals(TeamSpawnStyles.BIG)) {
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
			String[] lines = new String[4];
			lines[0] = "Team " + this.name;
			lines[1] = this.players.size() + "/" + this.warzone.getTeamCap() + " players";
			lines[2] = this.points + "/" + this.warzone.getScoreCap() + " pts";
			if (this.warzone.getLifePool() == -1) {
				lines[3] = "unlimited lives";
			} else {
				lines[3] = this.remainingLives + "/" + this.warzone.getLifePool() + " lives left";
			}

			SignHelper.setToSign(this.war, signBlock, (byte) signData, lines);
		}
	}

	private void setBlock(int x, int y, int z, TeamKind kind) {
		Block block = this.warzone.getWorld().getBlockAt(x, y, z);
		block.setType(kind.getMaterial());
		block.setData(kind.getData());
	}

	public void setTeamSpawn(Location teamSpawn) {

		this.teamSpawn = teamSpawn;

		// this resets the block to old state
		this.setSpawnVolume();
		this.getSpawnVolume().saveBlocks();

		this.initializeTeamSpawn();
	}

	public Location getTeamSpawn() {
		return this.teamSpawn;
	}

	public void addPlayer(Player player) {
		this.players.add(player);
	}

	public List<Player> getPlayers() {
		return this.players;
	}

	public void teamcast(String message) {
		for (Player player : this.players) {
			this.war.msg(player, message);
		}
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return this.name;
	}

	public boolean removePlayer(String name) {
		Player thePlayer = null;
		for (Player player : this.players) {
			if (player.getName().equals(name)) {
				thePlayer = player;
			}
		}
		if (thePlayer != null) {
			this.players.remove(thePlayer);
			return true;
		}
		return false;
	}

	public void setRemainingLives(int remainingLives) {
		this.remainingLives = remainingLives;
	}

	public int getRemainingLifes() {
		return this.remainingLives;
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
			this.teamcast("Can't score until at least one player joins another team.");
		}
	}

	public int getPoints() {
		return this.points;
	}

	public Volume getSpawnVolume() {

		return this.spawnVolume;
	}

	public void resetSign() {
		this.getSpawnVolume().resetBlocks();
		this.initializeTeamSpawn(); // reset everything instead of just sign

		if (this.warzone.getLobby() != null) {
			this.warzone.getLobby().resetTeamGateSign(this);
		}
	}

	public void setSpawnVolume(Volume volume) {
		this.spawnVolume = volume;
	}

	public void resetPoints() {
		this.points = 0;
	}

	public void setFlagVolume(Volume flagVolume) {
		this.flagVolume = flagVolume;
	}

	public Volume getFlagVolume() {
		return this.flagVolume;
	}

	private void setFlagVolume() {
		if (this.flagVolume == null) {
			this.flagVolume = new Volume(this.getName() + "flag", this.war, this.warzone.getWorld());
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

	@SuppressWarnings("unused")
	public void initializeTeamFlag() {
		// make air
		this.flagVolume.setToMaterial(Material.AIR);

		// Set the flag blocks
		int x = this.teamFlag.getBlockX();
		int y = this.teamFlag.getBlockY();
		int z = this.teamFlag.getBlockZ();

		// first ring
		this.warzone.getWorld().getBlockAt(x + 1, y - 1, z + 1).setType(Material.OBSIDIAN);
		this.warzone.getWorld().getBlockAt(x + 1, y - 1, z).setType(Material.OBSIDIAN);
		this.warzone.getWorld().getBlockAt(x + 1, y - 1, z - 1).setType(Material.OBSIDIAN);
		this.warzone.getWorld().getBlockAt(x, y - 1, z + 1).setType(Material.OBSIDIAN);
		this.warzone.getWorld().getBlockAt(x, y - 1, z).setType(Material.GLOWSTONE);
		this.warzone.getWorld().getBlockAt(x, y - 1, z - 1).setType(Material.OBSIDIAN);
		this.warzone.getWorld().getBlockAt(x - 1, y - 1, z + 1).setType(Material.OBSIDIAN);
		this.warzone.getWorld().getBlockAt(x - 1, y - 1, z).setType(Material.OBSIDIAN);
		this.warzone.getWorld().getBlockAt(x - 1, y - 1, z - 1).setType(Material.OBSIDIAN);

		// flag
		this.warzone.getWorld().getBlockAt(x, y + 1, z).setType(this.kind.getMaterial());
		this.warzone.getWorld().getBlockAt(x, y + 1, z).setData(this.kind.getData());
		this.warzone.getWorld().getBlockAt(x, y + 2, z).setType(Material.FENCE);

		// Flag post using Orientation
		int yaw = 0;
		if (this.teamFlag.getYaw() >= 0) {
			yaw = (int) (this.teamFlag.getYaw() % 360);
		} else {
			yaw = (int) (360 + (this.teamFlag.getYaw() % 360));
		}
		BlockFace facing = null;
		BlockFace opposite = null;
		if ((yaw >= 0 && yaw < 45) || (yaw >= 315 && yaw <= 360)) {
			facing = BlockFace.WEST;
			opposite = BlockFace.EAST;
			this.warzone.getWorld().getBlockAt(x, y, z - 1).setType(Material.FENCE);
			this.warzone.getWorld().getBlockAt(x, y + 1, z - 1).setType(Material.FENCE);
			this.warzone.getWorld().getBlockAt(x, y + 2, z - 1).setType(Material.FENCE);
		} else if (yaw >= 45 && yaw < 135) {
			facing = BlockFace.NORTH;
			opposite = BlockFace.SOUTH;
			this.warzone.getWorld().getBlockAt(x + 1, y, z).setType(Material.FENCE);
			this.warzone.getWorld().getBlockAt(x + 1, y + 1, z).setType(Material.FENCE);
			this.warzone.getWorld().getBlockAt(x + 1, y + 2, z).setType(Material.FENCE);
		} else if (yaw >= 135 && yaw < 225) {
			facing = BlockFace.EAST;
			opposite = BlockFace.WEST;
			this.warzone.getWorld().getBlockAt(x, y, z + 1).setType(Material.FENCE);
			this.warzone.getWorld().getBlockAt(x, y + 1, z + 1).setType(Material.FENCE);
			this.warzone.getWorld().getBlockAt(x, y + 2, z + 1).setType(Material.FENCE);
		} else if (yaw >= 225 && yaw < 315) {
			facing = BlockFace.SOUTH;
			opposite = BlockFace.NORTH;
			this.warzone.getWorld().getBlockAt(x - 1, y, z).setType(Material.FENCE);
			this.warzone.getWorld().getBlockAt(x - 1, y + 1, z).setType(Material.FENCE);
			this.warzone.getWorld().getBlockAt(x - 1, y + 2, z).setType(Material.FENCE);
		}
	}

	public void setTeamFlag(Location teamFlag) {

		this.teamFlag = teamFlag;

		// this resets the block to old state
		this.setFlagVolume();
		this.getFlagVolume().saveBlocks();

		this.initializeTeamFlag();
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
}
