package com.tommytony.war.utility;

public class LoadoutSelection {

	private boolean stillInSpawn;
	private int selectedIndex;

	public LoadoutSelection(boolean stillInSpawn, int selectedIndex) {
		this.setStillInSpawn(stillInSpawn);
		this.setSelectedIndex(selectedIndex);
		
	}

	public void setStillInSpawn(boolean stillInSpawn) {
		this.stillInSpawn = stillInSpawn;
	}

	public boolean isStillInSpawn() {
		return stillInSpawn;
	}

	public void setSelectedIndex(int selectedIndex) {
		this.selectedIndex = selectedIndex;
	}

	public int getSelectedIndex() {
		return selectedIndex;
	}
}
