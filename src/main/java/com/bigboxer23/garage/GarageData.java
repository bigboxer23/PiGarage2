package com.bigboxer23.garage;

import com.google.gson.annotations.SerializedName;

/**
 *
 */
public class GarageData
{
	@SerializedName("temperature")
	private float myTemperature;

	@SerializedName("humidity")
	private float myHumidity;

	@SerializedName("door")
	private boolean myIsOpen;

	@SerializedName("autoClose")
	private long myAutoClose;

	public GarageData(float theTemperature, float theHumidity, boolean theIsOpen, long theAutoClose)
	{
		myTemperature = theTemperature;
		myHumidity = theHumidity;
		myIsOpen = theIsOpen;
		myAutoClose = theAutoClose;
	}

	public void setAutoClose(long theAutoClose)
	{
		myAutoClose = theAutoClose;
	}

	public void setOpen(boolean theOpen)
	{
		myIsOpen = theOpen;
	}
}

