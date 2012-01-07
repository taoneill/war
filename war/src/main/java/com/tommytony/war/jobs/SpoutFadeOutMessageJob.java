package com.tommytony.war.jobs;

import bukkit.tommytony.war.War;

public class SpoutFadeOutMessageJob implements Runnable {

	public SpoutFadeOutMessageJob() {
	}

	public void run() {
		War.war.getSpoutMessenger().fadeOutOldMessages();
	}

}
