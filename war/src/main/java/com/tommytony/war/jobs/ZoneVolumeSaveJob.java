package com.tommytony.war.jobs;

import bukkit.tommytony.war.War;

import com.tommytony.war.mappers.ZoneVolumeMapper;
import com.tommytony.war.volumes.Volume;

public class ZoneVolumeSaveJob extends Thread {
	private final Volume volume;
	private final String zoneName;
	private final War war;

	public ZoneVolumeSaveJob(Volume volume, String zoneName, War war) {
		this.volume = volume;
		this.zoneName = zoneName;
		this.war = war;
	}

	@Override
	public void run() {
		ZoneVolumeMapper.save(this.volume, this.zoneName, this.war);
	}
}
