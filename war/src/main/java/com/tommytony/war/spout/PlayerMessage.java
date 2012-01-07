package com.tommytony.war.spout;

public class PlayerMessage {

	private final String message;
	private final long sendTime;

	public PlayerMessage(String message) {
		this.message = message;
		this.sendTime = System.currentTimeMillis();
	}

	public String getMessage() {
		return message;
	}
	
	public long getSendTime() {
		return sendTime;
	}
}
