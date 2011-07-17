package com.tommytony.war.jobs;

import com.tommytony.war.mappers.ZoneVolumeMapper;
import com.tommytony.war.volumes.Volume;

public class ZoneVolumeSaveJob extends Thread {
	private final Volume volume;
	private final String zoneName;

	public ZoneVolumeSaveJob(Volume volume, String zoneName) {
		this.volume = volume;
		this.zoneName = zoneName;
	}

	@Override
	public void run() {
		ZoneVolumeMapper.save(this.volume, this.zoneName);
	}
}
