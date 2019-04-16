package com.bigboxer23.garage;

import com.bigboxer23.garage.services.BaseService;
import com.google.gson.Gson;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.bigboxer23.garage.services.GarageDoorStatusService.kAutoCloseDelay;


/**
 * Controller to return data about sensor's input
 */
@RestController
@EnableAutoConfiguration
public class WebServiceController extends BaseService
{
	/**
	 * Get the distance to the object in front of sensor, by calling python script
	 *
	 * @return
	 */
	@GetMapping(path = "/Status2", produces = "application/json;charset=UTF-8")
	public String getStatus()
	{
		myLogger.debug("Checking status requested");
		return new Gson().toJson(getGarageData());
	}

	private GarageData getGarageData()
	{
		return new GarageData(myWeatherService.getTemperature(),
				myWeatherService.getHumidity(),
				myStatusService.isGarageDoorOpen(),
				myStatusService.getAutoCloseTimeRemaining(),
				myStatusService.getLastHouseDoorOpen());
	}

	@GetMapping(path = "/Close", produces = "application/json;charset=UTF-8")
	public String close()
	{
		myLogger.info("Closing door requested");
		myActionService.closeDoor();
		GarageData aData = getGarageData();
		aData.setAutoClose(-1);
		aData.setOpen(false);
		return new Gson().toJson(aData);
	}

	@GetMapping(path = "/Open", produces = "application/json;charset=UTF-8")
	public String open()
	{
		myLogger.info("Opening door requested");
		myActionService.openDoor();
		GarageData aData = getGarageData();
		aData.setAutoClose(kAutoCloseDelay);
		aData.setOpen(true);
		return new Gson().toJson(aData);
	}

	@GetMapping(path = "/DisableAutoClose", produces = "application/json;charset=UTF-8")
	public String disableAutoClose()
	{
		if (myStatusService.isGarageDoorOpen())
		{
			myStatusService.disableAutoClose();
		}
		GarageData aData = getGarageData();
		aData.setAutoClose(myStatusService.getAutoCloseTimeRemaining());
		return new Gson().toJson(aData);
	}
}
