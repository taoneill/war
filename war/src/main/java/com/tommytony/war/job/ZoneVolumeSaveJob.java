package com.tommytony.war.job;

import com.tommytony.war.War;
import com.tommytony.war.mapper.ZoneVolumeMapper;
import com.tommytony.war.volume.Volume;

import java.sql.SQLException;
import java.util.logging.Level;

import org.bukkit.scheduler.BukkitRunnable;

public class ZoneVolumeSaveJob extends BukkitRunnable {
	private final Volume volume;
	private final String zoneName;

	public ZoneVolumeSaveJob(Volume volume, String zoneName) {
		this.volume = volume;
		this.zoneName = zoneName;
	}

	@Override
	public void run() {
		try {
			ZoneVolumeMapper.save(this.volume, this.zoneName);
		} catch (SQLException ex) {
			War.war.log(ex.getMessage(), Level.SEVERE);
		}
	}
}
