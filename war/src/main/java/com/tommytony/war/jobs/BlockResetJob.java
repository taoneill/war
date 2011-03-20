package com.tommytony.war.jobs;

import com.tommytony.war.volumes.Volume;

public class BlockResetJob implements Runnable {

	private final Volume volume;

	public BlockResetJob(Volume volume) {
		this.volume = volume;
	}
	
	public void run() {
		volume.resetBlocks();
	}

}
