package com.tommytony.war.job;

import com.tommytony.war.War;

public class SpoutFadeOutMessageJob implements Runnable {

	public SpoutFadeOutMessageJob() {
	}

	public void run() {
		War.war.getSpoutDisplayer().fadeOutOldMessages();
	}

}
