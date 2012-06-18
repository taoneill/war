package com.tommytony.war.utility;

public class WarhubMaterials {
	private final int floorId;
	private final byte floorData;
	
	private final int gateId;
	private final byte gateData;
	
	private final int lightId;
	private final byte lightData;
	
	public WarhubMaterials(int floorId, byte floorData, int gateId, byte gateData, int lightId, byte lightData) {
		this.floorId = floorId;
		this.floorData = floorData;
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
	
}
