package com.tommytony.war.structure;

public class HubLobbyMaterials {
	private final int floorId;
	private final byte floorData;
	
	private final int outlineId;
	private final byte outlineData;
	
	private final int gateId;
	private final byte gateData;
	
	private final int lightId;
	private final byte lightData;
	
	public HubLobbyMaterials(int floorId, byte floorData, int outlineId, byte outlineData, int gateId, byte gateData, int lightId, byte lightData) {
		this.floorId = floorId;
		this.floorData = floorData;
		this.outlineId = outlineId;
		this.outlineData = outlineData;
		this.gateId = gateId;
		this.gateData = gateData;
		this.lightId = lightId;
		this.lightData = lightData;
	}

	public int getFloorId() {
		return floorId;
	}

	public byte getFloorData() {
		return floorData;
	}

	public int getGateId() {
		return gateId;
	}

	public byte getGateData() {
		return gateData;
	}

	public int getLightId() {
		return lightId;
	}

	public byte getLightData() {
		return lightData;
	}

	public int getOutlineId() {
		return outlineId;
	}

	public byte getOutlineData() {
		return outlineData;
	}
	
}
