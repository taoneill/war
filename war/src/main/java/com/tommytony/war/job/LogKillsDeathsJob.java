package com.tommytony.war.job;

import com.google.common.collect.ImmutableList;
import com.tommytony.war.War;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import org.bukkit.OfflinePlayer;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Job to insert kills and deaths information to MySQL database.
 *
 * @author cmastudios
 */
public class LogKillsDeathsJob extends BukkitRunnable {

	private final ImmutableList<KillsDeathsRecord> records;

	public LogKillsDeathsJob(final ImmutableList<KillsDeathsRecord> records) {
		this.records = records;
	}

	@Override
	/**
	 * Adds all #records to database at #databaseURL. Will attempt to open a
	 * connection to the database at #databaseURL. This method is thread safe.
	 */
	public void run() {
		Connection conn = null;
		try {
			conn = War.war.getMysqlConfig().getConnection();
			Statement createStmt = conn.createStatement();
			createStmt.executeUpdate("CREATE TABLE IF NOT EXISTS `war_kills` (`date` datetime NOT NULL, `player` varchar(16) NOT NULL, `kills` int(11) NOT NULL, `deaths` int(11) NOT NULL, KEY `date` (`date`)) ENGINE=InnoDB DEFAULT CHARSET=latin1");
			createStmt.close();
			PreparedStatement stmt = conn.prepareStatement("INSERT INTO war_kills (date, player, kills, deaths) VALUES (NOW(), ?, ?, ?)");
			conn.setAutoCommit(false);
			for (KillsDeathsRecord kdr : records) {
				stmt.setString(1, kdr.getPlayer().getName());
				stmt.setInt(2, kdr.getKills());
				stmt.setInt(3, kdr.getDeaths());
				stmt.addBatch();
			}
			stmt.executeBatch();
			conn.commit();
			stmt.close();
			final String deleteClause =
					War.war.getMysqlConfig().getLoggingDeleteClause();
			if (!deleteClause.isEmpty()) {
				Statement deleteStmt = conn.createStatement();
				deleteStmt.executeUpdate(
						"DELETE FROM war_kills " + deleteClause);
				deleteStmt.close();
				conn.commit();
			}
		} catch (SQLException ex) {
			War.war.getLogger().log(Level.SEVERE,
					"Inserting kill-death logs into database", ex);
		} finally {
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException ex) {
				}
			}
		}
	}

	public static final class KillsDeathsRecord {

		private final OfflinePlayer player;
		private final int kills;
		private final int deaths;

		public KillsDeathsRecord(OfflinePlayer player, int kills, int deaths) {
			this.player = player;
			this.kills = kills;
			this.deaths = deaths;
		}

		public OfflinePlayer getPlayer() {
			return player;
		}

		public int getKills() {
			return kills;
		}

		public int getDeaths() {
			return deaths;
		}
	}
}
