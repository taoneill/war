package com.tommytony.war.job;

import com.tommytony.war.Warzone;

/**
 * @author grinning
 */
public class ZoneTimeJob implements Runnable {

	private Warzone zone;
	
	public ZoneTimeJob(Warzone zone) {
		this.zone = zone;
	}
	
	@Override
	public void run() {
		zone.setPvpReady(true);
	}

	
}
