package com.bigboxer23.garage;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 *
 */
@Data
@Schema(description = "JSON representing garage's status")
public class GarageData
{
	@Schema(description = "temperature in the garage", required = true)
	private float temperature;

	@Schema(description = "humidity in the garage", required = true)
	private float humidity;

	@Schema(description = "is the garage open (\"true\") or closed (\"false\")", required = true)
	private String status;

	@Schema(description = "time in ms until the garage automatically closes", required = true)
	private long autoClose;

	@Schema(description = "time in ms when the house connected door was last opened", required = true)
	private String level;

	public GarageData(float theTemperature, float theHumidity, boolean theIsOpen, long theAutoClose, long theLastHouseDoorOpen)
	{
		temperature = theTemperature;
		humidity = theHumidity;
		status = theIsOpen + "";
		autoClose = theAutoClose;
		level = theLastHouseDoorOpen + "";
	}
}

