package com.tommytony.war.mapper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;

import org.bukkit.entity.Player;

import com.tommytony.war.War;

public abstract class PlayerStatsDatabaseMapper implements PlayerStatsMapper {

	protected abstract String getDatabaseString();
	
	protected abstract Connection getConnection();
	
	public void init() {
		try {
		    Connection c = this.getConnection();
		    Statement s = c.createStatement();
		    s.executeUpdate("CREATE TABLE if not exists stats ("
		            + "name varchar(64)" //64 should be enough characters for the name
		            + "kills int"
		            + "deaths int"
		            + "wins int"
		            + "losses int"
		    		+ ")");
		    s.close();
		    c.close();
		} catch (SQLException e) {
			War.war.log("Failed to connect to " + this.getDatabaseString() + " database", Level.WARNING);
		}
	}
	
    public void close() {
		
	}
    
    public int load(Player player, int stat) {
		Connection c = this.getConnection();
		PreparedStatement ps = null;
		int ret = 0;
		try {
		    switch(stat) {
		        case KILL:
		    	    ps = c.prepareStatement("SELECT kills FROM stats WHERE name = ?");
		    	    ps.setString(1, player.getName());
		    	    ret = this.determineResult(ps.executeQuery());
		    	    break;
		        case DEATH:
		    	    ps = c.prepareStatement("SELECT deaths FROM stats WHERE name = ?");
		    	    ps.setString(1, player.getName());
		    	    ret = this.determineResult(ps.executeQuery());
		    	    break;
		        case WIN:
		    	    ps = c.prepareStatement("SELECT wins FROM stats WHERE name = ?");
		    	    ps.setString(1, player.getName());
		    	    ret = this.determineResult(ps.executeQuery());
		    	    break;
		        case LOSS:
		    	    ps = c.prepareStatement("SELECT losses FROM stats WHERE name = ?");
		    	    ps.setString(1, player.getName());
		    	    ret = this.determineResult(ps.executeQuery());
		    	    break;
		        default:
		    	    ret = 0;
		    	    break;
		    }
		ps.close();
		c.close();
	    } catch(SQLException e) {
	    	War.war.log(this.getDatabaseString() + " error when loading player data", Level.WARNING);
	    }
		return ret;
	}
    
    public int[] load(Player p) {
    	int[] ret = new int[4];
    	try {
		    Connection c = this.getConnection();
		    PreparedStatement ps = c.prepareStatement("SELECT kills,deaths,wins,losses FROM stats where name = ?");
		    ps.setString(1, p.getName());
		    ResultSet res = ps.executeQuery();
		    for(int i = 0; i < 4; i++) {
		    	ret[i] = res.getInt(i + 1);
		    }
		    res.close();
		    ps.close();
		    c.close();
    	} catch(SQLException e) {
    		War.war.log(this.getDatabaseString() + " error when loading player data", Level.WARNING);
    	}
		return ret;
	}
    
	public void save(Player player, int stat, int data) {
		try {
			Connection c = this.getConnection();
			PreparedStatement ps = null;
		    if(this.load(player, KILL) != -1) { //then we just need to update data because a record exists already
			    ps = c.prepareStatement("UPDATE stats SET kills = ?, deaths = ?, wins = ?, losses = ? WHERE name = ?");
			    switch(stat) {
			        case KILL:
			        	ps = c.prepareStatement("UPDATE stats SET kills = ? WHERE name = ?");
			        	break;
			        case DEATH:
			        	ps = c.prepareStatement("UPDATE stats SET deaths = ? WHERE name = ?");
			        	break;
			        case WIN:
			        	ps = c.prepareStatement("UPDATE stats SET wins = ? WHERE name = ?");
			        	break;
			        case LOSS:
			        	ps = c.prepareStatement("UPDATE stats SET losses = ? WHERE name = ?");
			        	break;
			        default:
			        	return;
			    }
			    ps.setInt(1, data);
			    ps.setString(2, player.getName());
			    ps.executeUpdate();
		    } else { //we need to create their stuff...
		    	this.save(player, null, true); //it actually doesn't matter about the second arg in this case
		    }
		} catch(SQLException e) {
			War.war.log(this.getDatabaseString() + " failed to insert data into database", Level.WARNING);
		}
	}
	
	public void save(Player player, int[] stats) {
		this.save(player, stats, false);
	}
	
	private void save(Player player, int[] stats, boolean makeNewPlayer) {
		try {
			Connection c = this.getConnection();
			PreparedStatement ps = null;
			if(makeNewPlayer) {
				ps = c.prepareStatement("INSERT INTO stats (name, kills, deaths, wins, losses) VALUES (?, ?, ?, ?, ?)");
				ps.setString(1, player.getName());
				for(int i = 2; i < 6; i++) {
					ps.setInt(i, 0);
				}
				ps.executeUpdate();
				return;
			}
			if(this.load(player, KILL) != -1) {
				ps = c.prepareStatement("UPDATE stats SET kills = ?, deaths = ?, wins = ?, losses = ? WHERE name = ?");
				for(int i = 0; i < 4; i++) {
					ps.setInt(i + 1, stats[i]);
				}
				ps.setString(5, player.getName());
				ps.executeUpdate();
			} else {
				ps = c.prepareStatement("INSERT INTO stats (name, kills, deaths, wins, losses) VALUES (?, ?, ?, ?, ?)");
				ps.setString(1, player.getName());
				for(int i = 2; i < 6; i++) {
					ps.setInt(i, 0);
				}
				ps.executeUpdate();
			}
		} catch(SQLException e) {
			War.war.log(this.getDatabaseString() + " failed to insert data into database", Level.WARNING);
		}
	}
	
	private int determineResult(ResultSet set) {
		try {
		    if(set.wasNull()) {
			    return -1;
		    } 
		    return set.getInt(1);
		} catch(SQLException e) {
			War.war.log(this.getDatabaseString() + " error", Level.WARNING);
		}
		return -1;
	}
}
