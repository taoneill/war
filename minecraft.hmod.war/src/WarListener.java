import java.util.List;
import java.util.Random;
import java.util.logging.Level;



public class WarListener extends PluginListener {

	private final War war;
	private Random random = null;

	public WarListener(War war) {
		this.war = war;
		random = new Random(war.getServer().getTime());
	}
	
	public void onLogin(Player player) {    
		player.sendMessage(war.str("War is on! Pick your battle (try /warzones)."));
    }
	
	public boolean onCommand(Player player, java.lang.String[] split) {
		String command = split[0];
		
		// Player commands: /warzones, /warzone, /teams, /join, /leave
		
		// warzones
		if(command.equals("/warzones")){
			
			String warzonesMessage = "Warzones: ";
			if(war.getWarzones().isEmpty()){
				warzonesMessage += "none.";
			}
			for(Warzone warzone : war.getWarzones()) {
				
				warzonesMessage += warzone.getName() + " ("
				+ warzone.getTeams().size() + " teams, ";
				int playerTotal = 0;
				for(Team team : warzone.getTeams()) {
					playerTotal += team.getPlayers().size();
				}
				warzonesMessage += playerTotal + " players)  ";
			}
			player.sendMessage(war.str(warzonesMessage + "  Use /warzone <zone-name> to " +
					"teleport to a warzone, " +
					"then use /teams and /join <team-name>."));
			return true;
		}
		
		// warzone
		else if(command.equals("/warzone")) {
			if(split.length < 2) {
				player.sendMessage(war.str("Usage: /warzone <warzone-name>."));
			} else {
				for(Warzone warzone : war.getWarzones()) {
					if(warzone.getName().equals(split[1])){
						player.teleportTo(warzone.getTeleport());
						player.sendMessage(war.str("You've landed in warzone " + warzone.getName() +
								". Use the /join command. " + getAllTeamsMsg(player)));
						return true;
					}
				}
				player.sendMessage("No such warzone.");
			}
			return true;
		}
		
		// /teams
		else if(command.equals("/teams")){
			if(!war.inAnyWarzone(player.getLocation())) {
				player.sendMessage(war.str("Usage: /teams. " +
						"Must be in a warzone (try /warzones and /warzone)."));
			} else {
				player.sendMessage(war.str("" + getAllTeamsMsg(player)));
			}
			return true;
		}
		
		// /join <teamname>
		else if(command.equals("/join")) {
			if(split.length < 2 || !war.inAnyWarzone(player.getLocation())) {
				player.sendMessage(war.str("Usage: /join <team-name>." +
						" Teams are warzone specific." +
						" You must be inside a warzone to join a team."));
			} else {				
				// drop from old team if any
				Team previousTeam = war.getPlayerTeam(player.getName());
				if(previousTeam != null) {
					if(!previousTeam.removePlayer(player.getName())){
						war.getLogger().log(Level.WARNING, "Could not remove player " + player.getName() + " from team " + previousTeam.getName());
					}
						
				}
				
				// join new team
				String name = split[1];
				Warzone warzone = war.warzone(player.getLocation());
				List<Team> teams = warzone.getTeams();
				boolean foundTeam = false;
				for(Team team : teams) {
					if(team.getName().equals(name)) {
						team.addPlayer(player);
						Warzone zone = war.warzone(player.getLocation());
						zone.respawnPlayer(team, player);
						foundTeam = true;
					}
				}
				if(foundTeam) {
					for(Team team : teams){
						team.teamcast(war.str("" + player.getName() + " joined " + name));
					}
				} else {
					player.sendMessage(war.str("No such team. Try /teams."));
				}
				
				if(!warzone.hasPlayerInventory(player.getName())) {
					warzone.keepPlayerInventory(player);
					player.sendMessage(war.str("Your inventory has been stored until you /leave."));
				}
			}
			return true;
		}
		
		// /leave
		else if(command.equals("/leave")) {
			if(!war.inAnyWarzone(player.getLocation()) || war.getPlayerTeam(player.getName()) == null) {
				player.sendMessage(war.str("Usage: /leave. " +
						"Must be in a team already."));
			} else {
				Team playerTeam = war.getPlayerTeam(player.getName());
				playerTeam.removePlayer(player.getName());
				player.sendMessage(war.str("Left the team. You can now exit the warzone."));
				Warzone zone = war.warzone(player.getLocation());
				zone.restorePlayerInventory(player);
				player.sendMessage(war.str("Your inventory has (hopefully) been restored."));
			}
			return true;
		}
		
		
		// /team <msg>
		else if(command.equals("/team")) {
			if(!war.inAnyWarzone(player.getLocation())) {
				player.sendMessage(war.str("Usage: /team <message>. " +
						"Sends a message only to your teammates."));
			} else {
				Team playerTeam = war.getPlayerTeam(player.getName());
				String teamMessage = player.getName();
				for(int j = 1 ; j<split.length; j++) {
					String part = split[j];
					teamMessage += part + " ";
				}
				playerTeam.teamcast(war.str(teamMessage));
			}
			return true;
		}
		
		// Mod commands : /restartbattle
		
		// /restartbattle
		else if(command.equals("/restartbattle")) {
			if(!war.inAnyWarzone(player.getLocation())) {
				player.sendMessage(war.str("Usage: /restartbattle. Must be in warzone."));
			} else {
				Warzone warzone = war.warzone(player.getLocation());
				for(Team team: warzone.getTeams()) {
					team.teamcast(war.str("The battle has ended. " + getAllTeamsMsg(player) + " Resetting warzone " + warzone.getName() + "..."));
				}
				int resetBlocks = warzone.resetState();
				player.sendMessage(war.str("Warzone reset. " + resetBlocks + " blocks reset."));
			}
			return true;
		}
		
		// Warzone maker commands: /setwarzone, /savewarzone, /newteam, /setteamspawn, .. /monument?
		
		// /newteam <teamname>
		else if(command.equals("/newteam")) {
			if(split.length < 2 || !war.inAnyWarzone(player.getLocation())) {
				player.sendMessage(war.str("Usage: /newteam <team-name>." +
						" Sets the team spawn to the current location. " +
						"Must be in a warzone (try /warzones and /warzone). "));
			} else {
				String name = split[1];
				Team newTeam = new Team(name, player.getLocation());
				Warzone warzone = war.warzone(player.getLocation());
				warzone.getTeams().add(newTeam);
				warzone.addSpawnArea(newTeam, player.getLocation(), 41);				
				player.sendMessage(war.str("Team " + name + " created with spawn here."));
				WarzoneMapper.save(warzone, false);
			}
			return true;
		}
				
		// /setteamspawn 
		else if(command.equals("/teamspawn")) {
			if(split.length < 2 || !war.inAnyWarzone(player.getLocation())) {
				player.sendMessage(war.str("Usage: /setteamspawn <team-name>. " +
						"Sets the team spawn. " +
						"Must be in warzone and team must already exist."));
			} else {
				Warzone warzone = war.warzone(player.getLocation());
				List<Team> teams = warzone.getTeams();
				Team team = null;
				for(Team t : teams) {
					if(t.getName().equals(split[1])) {
						team = t;
					}
				}
				if(team != null) {
					warzone.removeSpawnArea(team);
					warzone.addSpawnArea(team, player.getLocation(), 41);
					team.setTeamSpawn(player.getLocation());
					player.sendMessage(war.str("Team " + team.getName() + " spawn relocated."));
				} else {
					player.sendMessage(war.str("Usage: /setteamspawn <team-name>. " +
							"Sets the team spawn. " +
							"Must be in warzone and team must already exist."));
				}
				
				WarzoneMapper.save(warzone, false);
			}
			return true;
		}
		
		// /deleteteam <teamname>
		else if(command.equals("/deleteteam")) {
			if(split.length < 2 || !war.inAnyWarzone(player.getLocation())) {
				player.sendMessage(war.str("Usage: /deleteteam <team-name>." +
						" Deletes the team and its spawn. " +
						"Must be in a warzone (try /warzones and /warzone). "));
			} else {
				String name = split[1];
				Warzone warzone = war.warzone(player.getLocation());
				List<Team> teams = warzone.getTeams();
				Team team = null;
				for(Team t : teams) {
					if(name.equals(t.getName())) {
						team = t;
					}
				}
				if(team != null) {
					warzone.removeSpawnArea(team);	
					warzone.getTeams().remove(team);
					WarzoneMapper.save(warzone, false);
					player.sendMessage(war.str("Team " + name + " removed."));
				} else {
					player.sendMessage(war.str("No such team."));
				}
			}
			return true;
		}
		
		// /setwarzone
		else if(command.equals("/setwarzone")) {
			if(split.length < 3 || (split.length == 3 && (!split[2].equals("southeast") && !split[2].equals("northwest")
															&& !split[2].equals("se") && !split[2].equals("nw")))) {
				player.sendMessage(war.str("Usage: /setwarzone <warzone-name> <'southeast'/'northwest'>. " +
						"Defines the battleground boundary. " +
						"The warzone is reset at the start of every battle. " +
						"This command overwrites any previously saved blocks " +
						"(i.e. make sure you reset with /restartbattle " +
						"or /resetwarzone before changing the boundary). "));
			} else {
				Warzone warzone = war.findWarzone(split[1]);
				if(warzone == null) {
					// create the warzone
					warzone = new Warzone(war, split[1]);
					war.addWarzone(warzone);
					WarMapper.save(war);
					if(split[2].equals("northwest") || split[2].equals("nw")) {
						warzone.setNorthwest(player.getLocation());
						player.sendMessage(war.str("Warzone " + warzone.getName() + " added. Northwesternmost point set at x=" 
								+ (int)warzone.getNorthwest().x + " z=" + (int)warzone.getNorthwest().z + "."));
					} else {
						warzone.setSoutheast(player.getLocation());
						player.sendMessage(war.str("Warzone " + warzone.getName() + " added. Southeasternmost point set at x=" 
								+ (int)warzone.getSoutheast().x + " z=" + (int)warzone.getSoutheast().z + "."));
					}
				} else {
					String message = "";
					if(split[2].equals("northwest") || split[2].equals("nw")) {
						warzone.setNorthwest(player.getLocation());
						message += "Northwesternmost point set at x=" + (int)warzone.getNorthwest().x 
										+ " z=" + (int)warzone.getNorthwest().z + " on warzone " + warzone.getName() + ".";
					} else {
						warzone.setSoutheast(player.getLocation());
						message += "Southeasternmost point set at x=" + (int)warzone.getSoutheast().x 
										+ " z=" + (int)warzone.getSoutheast().z + " on warzone " + warzone.getName() + ".";
					}
					
					if(warzone.getNorthwest() == null) {
						message += " Still missing northwesternmost point.";
					}
					if(warzone.getSoutheast() == null) {
						message += " Still missing southeasternmost point.";
					}
					if(warzone.getNorthwest() != null && warzone.getSoutheast() != null) {
						if(warzone.ready()) {
							message += " Warzone " + warzone.getName() + " almost ready. Use /newteam while inside the warzone to create new teams. Make sure to use /setwarzonestart to " +
									"set the warzone teleport point and initial state.";
						} else if (warzone.tooSmall()) {
							message += " Warzone " + warzone.getName() + " is too small. Min north-south size: 20. Min east-west size: 20.";
						} else if (warzone.tooBig()) {
							message += " Warzone " + warzone.getName() + " is too Big. Max north-south size: 1000. Max east-west size: 1000.";
						}
					}
					player.sendMessage(war.str(message));
				}
				WarzoneMapper.save(warzone, false);
				
			}
			return true;
		}		

		// /savewarzone
		else if(command.equals("/savewarzone")) {
			if(!war.inAnyWarzone(player.getLocation())) {
				player.sendMessage(war.str("Usage: /savewarzone. Must be in warzone. " +
						"Changes the warzone state at the beginning of every battle. " +
						"Also sets the teleport point for this warzone " +
						"(i.e. make sure to use /warzone or the warzone tp point will change). " +
						"Just like /setwarzone, this command overwrites any previously saved blocks " +
						"(i.e. make sure you reset with /restartbattle " +
						"or /resetwarzone before changing start state). "));
			} else {
				Warzone warzone = war.warzone(player.getLocation());
				int savedBlocks = warzone.saveState();
				warzone.setTeleport(player.getLocation());
				player.sendMessage(war.str("Warzone " + warzone.getName() + " initial state and teleport location changed. Saved " + savedBlocks + " blocks."));
				WarzoneMapper.save(warzone, true);
			}
			return true;
		}
		
		// /deletewarzone
		else if(command.equals("/deletewarzone")) {
			if(!war.inAnyWarzone(player.getLocation())) {
				player.sendMessage(war.str("Usage: /deletewarzone." +
						" Deletes the warzone. " +
						"Must be in the warzone (try /warzones and /warzone). "));
			} else {
				Warzone warzone = war.warzone(player.getLocation());
				for(Team t : warzone.getTeams()) {
					warzone.removeSpawnArea(t);
				}
				for(Monument m : warzone.getMonuments()) {
					m.remove();
				}
				war.getWarzones().remove(warzone);
				WarMapper.save(war);
				WarzoneMapper.delete(warzone.getName());
				player.sendMessage(war.str("Warzone " + warzone.getName() + " removed."));
			}
			return true;
		}
		
		// /monument
		else if(command.equals("/monument")) {
			if(!war.inAnyWarzone(player.getLocation())) {
				player.sendMessage(war.str("Usage: /monument <name>. Must be in warzone."));
			} else {
				Warzone warzone = war.warzone(player.getLocation());
				String monumentName = split[1];
				if(warzone.hasMonument(monumentName)) {
					// move the existing monument
					Monument monument = warzone.getMonument(monumentName);
					monument.remove();
					monument.setLocation(player.getLocation());
					player.sendMessage(war.str("Monument " + monument.getName() + " was moved."));
				} else {
					// create a new monument
					Monument monument = new Monument(split[1], war, player.getLocation());
					warzone.getMonuments().add(monument);
					player.sendMessage(war.str("Monument " + monument.getName() + " created."));
				}
				WarzoneMapper.save(warzone, false);
			}
			return true;
		}
		
		// /deletemonument <name>
		else if(command.equals("/deletemonument")) {
			if(split.length < 2 || !war.inAnyWarzone(player.getLocation())) {
				player.sendMessage(war.str("Usage: /deletemonument <team-name>." +
						" Deletes the monument. " +
						"Must be in a warzone (try /warzones and /warzone). "));
			} else {
				String name = split[1];
				Warzone warzone = war.warzone(player.getLocation());
				Monument monument = warzone.getMonument(name);
				if(monument != null) {
					monument.remove();
					warzone.getMonuments().remove(monument);
					WarzoneMapper.save(warzone, false);
					player.sendMessage(war.str("Monument " + name + " removed."));
				} else {
					player.sendMessage(war.str("No such monument."));
				}
			}
			return true;
		}
		
        return false;
    }
	
	

	public boolean onDamage(PluginLoader.DamageType damageType, BaseEntity attacker, BaseEntity defender, int damageAmount) {
		if(attacker != null && defender != null && attacker.isPlayer() && defender.isPlayer()) {
			// only let adversaries (same warzone, different team) attack each other
			Player a = attacker.getPlayer();
			Player d = defender.getPlayer();
			Warzone attackerWarzone = war.warzone(a.getLocation());
			Team attackerTeam = war.getPlayerTeam(a.getName());
			Warzone defenderWarzone = war.warzone(d.getLocation());
			Team defenderTeam = war.getPlayerTeam(d.getName());
			if(attackerTeam != null && defenderTeam != null 
					&& attackerTeam != defenderTeam 			
					&& attackerWarzone == defenderWarzone) {
				war.getLogger().log(Level.INFO, a.getName() + " hit " + d.getName() + " for " + damageAmount);
				return false;	// adversaries!
			} else if (attackerTeam != null && defenderTeam != null 
					&& attackerTeam == defenderTeam 			
					&& attackerWarzone == defenderWarzone) {
				// same team
				if(attackerWarzone.getFriendlyFire()) {
					a.sendMessage(war.str("Friendly fire!"));
					return false;	// if ff is on, let the attack go through
				} else {
					return true;	// ff is off
				}
			} else if (attackerTeam == null && defenderTeam == null){
				// normal PVP
				return false;
			} else {
				a.sendMessage(war.str("Your attack missed!"));
				if(attackerTeam == null) {
					a.sendMessage(war.str(" You must join a team " +
						", then you'll be able to damage people " +
						"in the other teams in that warzone."));
				} else if (defenderTeam == null) {
					a.sendMessage(war.str("Your target is not in a team."));
				} else if (attackerTeam == defenderTeam) {
					a.sendMessage(war.str("Your target is on your team."));
				} else if (attackerWarzone != defenderWarzone) {
					a.sendMessage(war.str("Your target is playing in another warzone."));
				}
				return true; // can't attack someone inside awarzone if you're not in a team
			}
		}
		// mobs are always dangerous
		return false;
	}

	public boolean onHealthChange(Player player, int before, int after) {
		
		if(after <= 0) {
			Team team = war.getPlayerTeam(player.getName());
			if(team != null){
				// teleport to team spawn upon death
				Warzone zone = war.warzone(player.getLocation());
				boolean roundOver = false;
				synchronized(zone) {
					int remaining = team.getRemainingTickets();
					if(remaining == 0) { // your death caused your team to lose
						List<Team> teams = zone.getTeams();
						for(Team t : teams) {
							t.teamcast(war.str("The battle is over. Team " + team.getName() + " lost: " 
									+ player.getName() + " hit the bottom of their life pool." ));
							t.teamcast(war.str("A new battle begins. The warzone is being reset..."));
							if(!t.getName().equals(team.getName())) {
								// all other teams get a point
								t.addPoint();
								int x = (int)t.getTeamSpawn().x;
								int y = (int)t.getTeamSpawn().y;
								int z = (int)t.getTeamSpawn().z;
								Block block = war.getServer().getBlockAt(x, y, z);
								ComplexBlock complexBlock = war.getServer().getComplexBlock(x, y, z);
								Sign sign = (Sign)complexBlock;
								sign.setText(0, "Team " + team.getName());
								sign.setText(1, "spawn");
								sign.setText(2, team.getPoints() + " pts");
								sign.setText(3, team.getRemainingTickets() + "/" + zone.getLifePool() + " lives left");
								sign.update();
							}
						}
						zone.resetState();
						roundOver = true;
					} else {
						team.setRemainingTickets(remaining - 1);
					}
				}
				if(!roundOver) {
					zone.respawnPlayer(team, player);
					player.sendMessage(war.str("You died!"));
					List<Team> teams = zone.getTeams();
					
					int x = (int)team.getTeamSpawn().x;
					int y = (int)team.getTeamSpawn().y;
					int z = (int)team.getTeamSpawn().z;
					Block block = war.getServer().getBlockAt(x, y, z);
					ComplexBlock complexBlock = war.getServer().getComplexBlock(x, y, z);
					Sign sign = (Sign)complexBlock;
					sign.setText(0, "Team " + team.getName());
					sign.setText(1, "spawn");
					sign.setText(2, team.getPoints() + " pts");
					sign.setText(3, team.getRemainingTickets()+ "/" + zone.getLifePool() + " lives left");
					sign.update();
					
//					for(Team t : teams) {
//						//t.teamcast(war.str(player.getName() + " died. Team " + team.getName() + " has " + team.getRemainingTickets() + "/" + War.LIFEPOOL + " lives left."));
//						if(team.getRemainingTickets() == 0) {
//							t.teamcast(war.str("Team " + team.getName() + "'s life pool is empty. One more death and they will lose!"));
//						}
//					}
					war.getLogger().log(Level.INFO, player.getName() + " died and was tp'd back to team " + team.getName() + "'s spawn");
				} else {
					war.getLogger().log(Level.INFO, player.getName() + " died and battle ended in team " + team.getName() + "'s disfavor");
				}

				//return true;
			}
		}
		return false;
	}
	
	public void onPlayerMove(Player player, Location from, Location to) {
		Warzone playerWarzone = war.getPlayerWarzone(player.getName());
		Team playerTeam = war.getPlayerTeam(player.getName());
		if(player != null && from != null && to != null && 
				playerTeam != null && !playerWarzone.contains(to)) {
			player.sendMessage(war.str("Can't go outside the warzone boundary! Use /leave to exit the battle."));
			if(playerWarzone.contains(from)){
				player.teleportTo(from);
			} else {
				// somehow the player made it out of the zone
				player.teleportTo(playerTeam.getTeamSpawn());
				player.sendMessage(war.str("Brought you back to your team spawn. Use /leave to exit the battle."));
			}
		}
		
		if(player != null && from != null && to != null && 
				playerTeam == null 
				&& war.inAnyWarzone(from) 
				&& !war.inAnyWarzone(to)) {
			// leaving
			Warzone zone = war.warzone(from);
			player.sendMessage(war.str("Leaving warzone " + zone.getName() + "."));
		}
		
		if(player != null && from != null && to != null && 
				playerTeam == null 
				&& !war.inAnyWarzone(from) 
				&& war.inAnyWarzone(to)) {
			// entering
			Warzone zone = war.warzone(to);
			player.sendMessage(war.str("Entering warzone " + zone.getName() + ". Tip: use /teams."));
		}
		
		if(to != null && playerTeam != null
				&& playerWarzone.nearAnyOwnedMonument(to, playerTeam) 
				&& player.getHealth() < 20
				&& random.nextInt(42) == 3 ) {	// one chance out of many of getting healed
			player.setHealth(20);
			player.sendMessage(war.str("Your dance pleases the monument's voodoo. You gain full health!"));
		}
		
    }
	
    public boolean onBlockDestroy(Player player, Block block) {
        return false;
    }

    public boolean onBlockBreak(Player player, Block block) {
    	if(player != null && block != null) {
	    	Warzone warzone = war.warzone(player.getLocation());
	    	if(warzone != null && war.getPlayerTeam(player.getName()) == null) {
	    		// can't actually destroy blocks in a warzone if not part of a team
	    		player.sendMessage(war.str("Can't destroy part of a warzone if you're not in a team."));
	    		return true;
	    	}
	    	
	    	if(warzone != null && warzone.isImportantBlock(block)) {
	    		player.sendMessage(war.str("Can't destroy this."));
	    		return true;
	    	}
    	}
        return false;
    }
    
    public boolean onBlockPlace(Player player, Block blockPlaced, Block blockClicked, Item itemInHand) {
    	Warzone warzone = war.warzone(player.getLocation());
    	if(warzone != null) {
    		if(warzone.isImportantBlock(blockPlaced) || warzone.isImportantBlock(blockClicked)) {
    			player.sendMessage(war.str("Can't build here."));
	    		return true;
    		}
    	}
        return false;
    }

	private String getAllTeamsMsg(Player player){
		String teamsMessage = "Teams: ";
		if(war.warzone(player.getLocation()).getTeams().isEmpty()){
			teamsMessage += "none.";
		}
		Warzone warzone = war.warzone(player.getLocation());
		for(Team team :warzone.getTeams()) {
			teamsMessage += team.getName() + " (" + team.getPoints() + " points, "+ team.getRemainingTickets() + "/" + warzone.getLifePool() + " lives left. ";
			for(Player member : team.getPlayers()) {
				teamsMessage += member.getName() + " ";
			}
			teamsMessage += ")  ";
		}
		return teamsMessage;
	}
	
	public void onDisconnect(Player player) {
		Team team = war.getPlayerTeam(player.getName());
		if(team != null) {
			team.removePlayer(player.getName());
		}
	}
	
	public boolean onIgnite(Block block, Player player) {
		if(player != null) {
			Team team = war.getPlayerTeam(player.getName()); 
			Warzone zone = war.getPlayerWarzone(player.getName());
			if(team != null && block != null && zone != null && zone.isMonumentFirestone(block)) {
				Monument monument = zone.getMonumentForFirestone(block);
				if(!monument.hasOwner()) {
					monument.ignite(team);
					List<Team> teams = zone.getTeams();
					for(Team t : teams) {
						t.teamcast(war.str("Monument " + monument.getName() + " has been ignited by team " + team.getName() + "."));
					}
				} else {
					player.sendMessage(war.str("Monument must be smothered first."));
				}
			}
		}
        return false;
    }
	
	public boolean onFlow(Block blockFrom, Block blockTo) {
		Block block = null;
		if(blockTo != null) {
			block = blockTo;
		} else if (blockFrom != null) {
			block = blockFrom;
		}
		
		if(block != null) {
			Warzone zone = war.warzone(new Location(block.getX(), block.getY(), block.getZ()));
			if(zone != null && 
					((blockTo != null && zone.isMonumentFirestone(blockTo)
						|| (blockFrom != null && zone.isMonumentFirestone(blockFrom))))) {
				Monument monument = null;
				if(blockTo != null) monument = zone.getMonumentForFirestone(blockTo);
				if(monument == null && blockFrom != null) monument = zone.getMonumentForFirestone(blockFrom);
				if(monument.hasOwner()) {
					monument.setOwnerTeam(null);
					List<Team> teams = zone.getTeams();
					for(Team team : teams) {
						team.teamcast(war.str("Monument " + monument.getName() + " has been smothered."));
					}
				}
			}
		}
		
        return false;
    }
}
