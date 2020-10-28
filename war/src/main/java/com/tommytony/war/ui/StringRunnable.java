package com.tommytony.war.ui;

/**
 * Created by Connor on 7/27/2017.
 */
public abstract class StringRunnable implements Runnable {
	private String value;

	public void setValue(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}
}
