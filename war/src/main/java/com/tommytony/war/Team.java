package com.tommytony.war;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.getspout.spoutapi.SpoutManager;
import org.getspout.spoutapi.player.SpoutPlayer;


import com.tommytony.war.config.InventoryBag;
import com.tommytony.war.config.ScoreboardType;
import com.tommytony.war.config.TeamConfig;
import com.tommytony.war.config.TeamConfigBag;
import com.tommytony.war.config.TeamKind;
import com.tommytony.war.config.TeamSpawnStyle;
import com.tommytony.war.structure.Bomb;
import com.tommytony.war.structure.Cake;
import com.tommytony.war.utility.Direction;
import com.tommytony.war.utility.PlayerStatTracker;
import com.tommytony.war.utility.SignHelper;
import com.tommytony.war.volume.BlockInfo;
import com.tommytony.war.volume.Volume;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;

/**
 *
 * @author tommytony, grinning
 *
 */
public class Team {
	private List<Player> players = new ArrayList<Player>();
	private Map<Player, Integer> kills = new HashMap<Player, Integer>(6); //keeps track of kills per life
	private List<Player> fiveKillStreak = new ArrayList<Player>(4); //keeps track of who has a five killstreak (airstrike)
	private List<Player> sevenKillStreak = new ArrayList<Player>(4); //keeps track of who has a seven killstrea (dogs)
	private List<Player> inTeamChat = new CopyOnWriteArrayList<Player>(); //keeps track of who is in teamchat
	private List<BukkitTask> doggieManagers = new ArrayList<BukkitTask>(); //keeps track of doggie's and keeping them in the fight!
	private Location teamSpawn = null;
	private Location teamFlag = null;
	private String name;
	private int remainingLives;
	private int points = 0;
	private Volume spawnVolume;
	private Volume flagVolume;
	private final Warzone warzone;
	private TeamKind kind;

	private TeamConfigBag teamConfig;
	private InventoryBag inventories;

	public Team(String name, TeamKind kind, Location teamSpawn, Warzone warzone) {
		this.warzone = warzone;
		this.teamConfig = new TeamConfigBag(warzone);
		this.inventories = new InventoryBag(warzone);	// important constructors for cascading configs
		this.setName(name);
		this.teamSpawn = teamSpawn;
		this.setSpawnVolume(new Volume(name, warzone.getWorld()));
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

		TeamSpawnStyle style = this.getTeamConfig().resolveSpawnStyle();
		if (style.equals(TeamSpawnStyle.INVISIBLE)) {
			this.spawnVolume.setCornerOne(this.warzone.getWorld().getBlockAt(x, y - 1, z));
			this.spawnVolume.setCornerTwo(this.warzone.getWorld().getBlockAt(x, y + 3, z));
		} else if (style.equals(TeamSpawnStyle.SMALL)) {
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
		// make air (old two-high above floor)
		Volume airGap = new Volume("airgap", this.warzone.getWorld());
		airGap.setCornerOne(new BlockInfo(
				this.spawnVolume.getCornerOne().getX(), 
				this.spawnVolume.getCornerOne().getY() + 1, 
				this.spawnVolume.getCornerOne().getZ(),
				0,
				(byte)0));
		airGap.setCornerTwo(new BlockInfo(
				this.spawnVolume.getCornerTwo().getX(), 
				this.spawnVolume.getCornerOne().getY() + 2, 
				this.spawnVolume.getCornerTwo().getZ(),
				0,
				(byte)0));
		airGap.setToMaterial(Material.AIR);

		// Set the spawn
		int x = this.teamSpawn.getBlockX();
		int y = this.teamSpawn.getBlockY();
		int z = this.teamSpawn.getBlockZ();
		
		Material light = Material.getMaterial(this.warzone.getWarzoneMaterials().getLightId());
		byte lightData = this.warzone.getWarzoneMaterials().getLightData();

		TeamSpawnStyle style = this.getTeamConfig().resolveSpawnStyle();
		if (style.equals(TeamSpawnStyle.INVISIBLE)) {
			// nothing but glowstone
			Block lightBlock = this.warzone.getWorld().getBlockAt(x, y - 1, z);
			lightBlock.setType(light);
			lightBlock.setData(lightData);
		} else {
			// first ring
			this.setBlock(x + 1, y - 1, z + 1, this.kind);
			this.setBlock(x + 1, y - 1, z, this.kind);
			this.setBlock(x + 1, y - 1, z - 1, this.kind);
			this.setBlock(x, y - 1, z + 1, this.kind);
			Block lightBlock = this.warzone.getWorld().getBlockAt(x, y - 1, z);
			lightBlock.setType(light);
			lightBlock.setData(lightData);
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

		if (style.equals(TeamSpawnStyle.INVISIBLE)) {
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
		} else if (style.equals(TeamSpawnStyle.SMALL)) {
			// SMALL style
			if (yaw >= 0 && yaw < 90) {
				signData = 10;
				signBlock = this.warzone.getWorld().getBlockAt(x, y, z).getRelative(Direction.NORTH()).getRelative(Direction.WEST());
			} else if (yaw >= 90 && yaw <= 180) {
				signData = 14;
				signBlock = this.warzone.getWorld().getBlockAt(x, y, z).getRelative(Direction.NORTH()).getRelative(Direction.EAST());
			} else if (yaw >= 180 && yaw < 270) {
				signData = 2;
				signBlock = this.warzone.getWorld().getBlockAt(x, y, z).getRelative(Direction.SOUTH()).getRelative(Direction.EAST());
			} else if (yaw >= 270 && yaw <= 360) {
				signData = 6;
				signBlock = this.warzone.getWorld().getBlockAt(x, y, z).getRelative(Direction.SOUTH()).getRelative(Direction.WEST());
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
				facing = Direction.NORTH_WEST();
				opposite = Direction.SOUTH_EAST();
				signData = 10;
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
				facing = Direction.NORTH_EAST();
				opposite = Direction.SOUTH_WEST();
				signData = 14;
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
				facing = Direction.SOUTH_EAST();
				opposite = Direction.NORTH_WEST();
				signData = 2;
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
				facing = Direction.SOUTH_WEST();
				opposite = Direction.NORTH_EAST();
				signData = 6;
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
			String[] lines = new String[4];
			lines[0] = "Team " + this.name;
			lines[1] = this.players.size() + "/" + this.getTeamConfig().resolveInt(TeamConfig.TEAMSIZE) + " players";
			lines[2] = this.points + "/" + this.getTeamConfig().resolveInt(TeamConfig.MAXSCORE)+ " pts";
			if (this.getTeamConfig().resolveInt(TeamConfig.LIFEPOOL) == -1) {
				lines[3] = "unlimited lives";
			} else {
				lines[3] = this.remainingLives + "/" + this.getTeamConfig().resolveInt(TeamConfig.LIFEPOOL) + " lives left";
			}

			SignHelper.setToSign(War.war, signBlock, (byte) signData, lines);
		}
		
		if (War.war.isSpoutServer()) {
			War.war.getSpoutDisplayer().updateStats(this.warzone);
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
		this.kills.put(player, 0);
		if (this.warzone.getScoreboardType() != ScoreboardType.NONE) {
			player.setScoreboard(this.warzone.getScoreboard());
		}
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
			this.kills.remove(thePlayer);
			thePlayer.resetMaxHealth(); //take away three killstreak if any
			
			//prevent memory leaks!
			if(this.hasFiveKillStreak(thePlayer)) {
				this.removeFiveKillStreak(thePlayer);
			}
			
			if(this.hasSevenKillStreak(thePlayer)) {
				this.removeSevenKillStreak(thePlayer);
			}
			
			if (this.warzone.isFlagThief(thePlayer.getName())) {
				Team victim = this.warzone.getVictimTeamForFlagThief(thePlayer.getName());
				victim.getFlagVolume().resetBlocks();
				victim.initializeTeamFlag();
				this.warzone.removeFlagThief(thePlayer.getName());
				for (Team t : this.warzone.getTeams()) {
					t.teamcast("Team " + ChatColor.GREEN + victim.getName() + ChatColor.WHITE + " flag was returned.");
				}
			}
			
			if (this.warzone.isBombThief(thePlayer.getName())) {
				Bomb bomb = this.warzone.getBombForThief(thePlayer.getName());
				bomb.getVolume().resetBlocks();
				bomb.addBombBlocks();
				this.warzone.removeBombThief(thePlayer.getName());
				for (Team t : this.warzone.getTeams()) {
					t.teamcast("Bomb " + ChatColor.GREEN + bomb.getName() + ChatColor.WHITE  + " was returned.");
				}
			}
			
			if (this.warzone.isCakeThief(thePlayer.getName())) {
				Cake cake = this.warzone.getCakeForThief(thePlayer.getName());
				cake.getVolume().resetBlocks();
				cake.addCakeBlocks();
				this.warzone.removeCakeThief(thePlayer.getName());
				for (Team t : this.warzone.getTeams()) {
					t.teamcast("Cake " + ChatColor.GREEN + cake.getName() + ChatColor.WHITE  + " was returned.");
				}
			}
						
			return true;
		}	
		
		return false;
	}

	public void setRemainingLives(int remainingLives) {
		this.remainingLives = remainingLives;
		if (this.warzone.getScoreboard() != null && this.warzone.getScoreboardType() == ScoreboardType.LIFEPOOL) {
			String teamName = kind.getColor() + name + ChatColor.RESET;
			OfflinePlayer teamPlayer = Bukkit.getOfflinePlayer(teamName);
			Objective obj = this.warzone.getScoreboard().getObjective("Lifepool");
			obj.getScore(teamPlayer).setScore(remainingLives);
		}
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
		if (this.warzone.getScoreboardType() == ScoreboardType.POINTS) {
			String teamName = kind.getColor() + name + ChatColor.RESET;
			this.warzone.getScoreboard().getObjective(DisplaySlot.SIDEBAR)
					.getScore(Bukkit.getOfflinePlayer(teamName)).setScore(points);
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
		if (this.warzone.getScoreboardType() == ScoreboardType.POINTS) {
			String teamName = kind.getColor() + name + ChatColor.RESET;
			this.warzone.getScoreboard().getObjective(DisplaySlot.SIDEBAR)
					.getScore(Bukkit.getOfflinePlayer(teamName)).setScore(points);
		}
	}

	public void setFlagVolume(Volume flagVolume) {
		this.flagVolume = flagVolume;
	}

	public Volume getFlagVolume() {
		return this.flagVolume;
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

	@SuppressWarnings("unused")
	public void initializeTeamFlag() {
		// make air (old two-high above floor)
		Volume airGap = new Volume("airgap", this.warzone.getWorld());
		airGap.setCornerOne(new BlockInfo(
				this.flagVolume.getCornerOne().getX(), 
				this.flagVolume.getCornerOne().getY() + 1, 
				this.flagVolume.getCornerOne().getZ(),
				0,
				(byte)0));
		airGap.setCornerTwo(new BlockInfo(
				this.flagVolume.getCornerTwo().getX(), 
				this.flagVolume.getCornerOne().getY() + 2, 
				this.flagVolume.getCornerTwo().getZ(),
				0,
				(byte)0));
		airGap.setToMaterial(Material.AIR);

		// Set the flag blocks
		int x = this.teamFlag.getBlockX();
		int y = this.teamFlag.getBlockY();
		int z = this.teamFlag.getBlockZ();
		
		Material main = Material.getMaterial(this.warzone.getWarzoneMaterials().getMainId());
		byte mainData = this.warzone.getWarzoneMaterials().getMainData();
		Material stand = Material.getMaterial(this.warzone.getWarzoneMaterials().getStandId());
		byte standData = this.warzone.getWarzoneMaterials().getStandData();
		Material light = Material.getMaterial(this.warzone.getWarzoneMaterials().getLightId());
		byte lightData = this.warzone.getWarzoneMaterials().getLightData();

		// first ring
		Block current = this.warzone.getWorld().getBlockAt(x + 1, y - 1, z + 1);
		current.setType(main);
		current.setData(mainData);
		current = this.warzone.getWorld().getBlockAt(x + 1, y - 1, z);
		current.setType(main);
		current.setData(mainData);
		current = this.warzone.getWorld().getBlockAt(x + 1, y - 1, z - 1);
		current.setType(main);
		current.setData(mainData);
		current = this.warzone.getWorld().getBlockAt(x, y - 1, z + 1);
		current.setType(main);
		current.setData(mainData);
		current = this.warzone.getWorld().getBlockAt(x, y - 1, z);
		current.setType(light);
		current.setData(lightData);
		current = this.warzone.getWorld().getBlockAt(x, y - 1, z - 1);
		current.setType(main);
		current.setData(mainData);
		current = this.warzone.getWorld().getBlockAt(x - 1, y - 1, z + 1);
		current.setType(main);
		current.setData(mainData);
		current = this.warzone.getWorld().getBlockAt(x - 1, y - 1, z);
		current.setType(main);
		current.setData(mainData);
		current = this.warzone.getWorld().getBlockAt(x - 1, y - 1, z - 1);
		current.setType(main);
		current.setData(mainData);

		// flag
		this.warzone.getWorld().getBlockAt(x, y + 1, z).setType(this.kind.getMaterial());
		this.warzone.getWorld().getBlockAt(x, y + 1, z).setData(this.kind.getData());

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
			facing = Direction.WEST();
			opposite = Direction.EAST();
			current = this.warzone.getWorld().getBlockAt(x, y, z - 1);
			current.setType(stand);
			current.setData(standData);
			current = this.warzone.getWorld().getBlockAt(x, y + 1, z - 1);
			current.setType(stand);
			current.setData(standData);
		} else if (yaw >= 45 && yaw < 135) {
			facing = Direction.NORTH();
			opposite = Direction.SOUTH();
			current = this.warzone.getWorld().getBlockAt(x + 1, y, z);
			current.setType(stand);
			current.setData(standData);
			current = this.warzone.getWorld().getBlockAt(x + 1, y + 1, z);
			current.setType(stand);
			current.setData(standData);
		} else if (yaw >= 135 && yaw < 225) {
			facing = Direction.EAST();
			opposite = Direction.WEST();
			current = this.warzone.getWorld().getBlockAt(x, y, z + 1);
			current.setType(stand);
			current.setData(standData);
			current = this.warzone.getWorld().getBlockAt(x, y + 1, z + 1);
			current.setType(stand);
			current.setData(standData);;
		} else if (yaw >= 225 && yaw < 315) {
			facing = Direction.SOUTH();
			opposite = Direction.NORTH();
			current = this.warzone.getWorld().getBlockAt(x - 1, y, z);
			current.setType(stand);
			current.setData(standData);
			current = this.warzone.getWorld().getBlockAt(x - 1, y + 1, z);
			current.setType(stand);
			current.setData(standData);
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
	
	public int getKills(Player p) {
		Integer i = this.kills.get(p);
		if(i == null) {
			return 0; //if you dont exist, you must have 0 kills and our logic is bugged...
		}
		return i; //automatic compiler typing
	}
	
	public void incKills(Player p) {
		this.kills.put(p, this.getKills(p) + 1);
		PlayerStatTracker.getStats(p).incKills(); //increment our kills
	}
	
	public void zeroKills(Player p) {
		this.kills.put(p, 0);
	}
	
	public boolean hasFiveKillStreak(Player p) {
		for(Player pl : this.fiveKillStreak) {
			if(pl.equals(p)) {
				return true;
			}
		}
		return false;
	}
	
	public void addFiveKillStreak(Player p) {
		this.fiveKillStreak.add(p);
	}
	
	public void removeFiveKillStreak(Player p) {
		this.fiveKillStreak.remove(p);
	}
	
	public boolean hasSevenKillStreak(Player p) {
		for(Player pl : this.sevenKillStreak) {
			if(pl.equals(p)) {
				return true;
			}
		}
		return false;
	}
	
	public void addSevenKillStreak(Player p) {
		this.sevenKillStreak.add(p);
	}
	
	public void removeSevenKillStreak(Player p) {
		this.sevenKillStreak.remove(p);
	}
	
	public boolean inTeamChat(Player p) {
		for(Player pl : this.inTeamChat) {
			if(pl.equals(p)) {
				return true;
			}
		}
		return false;
	}
	
	public void addToTeamchat(Player p) {
		this.inTeamChat.add(p);
	}
	
	public void removeFromTeamchat(Player p) {
		this.inTeamChat.remove(p);
	}
	
	public void addDoggyManager(BukkitTask task) {
		this.doggieManagers.add(task);
	}
	
	public void clearDoggyManagers() {
		for(BukkitTask y : this.doggieManagers) {
			y.cancel();
		}
		this.doggieManagers.clear();
	}	
}
