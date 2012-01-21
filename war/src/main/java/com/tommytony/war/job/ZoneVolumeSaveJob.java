package com.tommytony.war.job;

import com.tommytony.war.mapper.ZoneVolumeMapper;
import com.tommytony.war.volume.Volume;

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
