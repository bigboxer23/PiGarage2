package com.bigboxer23.garage;

import com.bigboxer23.garage.services.BaseService;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


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
	@RequestMapping("/Status2")
	public String getStatus()
	{
		myLogger.config("Checking status requested");
		return "{\"temperature\":" + myWeatherService.getTemperature()
				+ ",\"humidity\":" + myWeatherService.getHumidity() +
				",\"door\":" + myStatusService.isGarageDoorOpen() +
				",\"autoClose\":" + myStatusService.getAutoCloseTimeRemaining() + "}";
	}

	@RequestMapping("/Close")
	public String close()
	{
		myLogger.warning("Closing door requested");
		myActionService.closeDoor();
		return "\"Closing\"";
	}

	@RequestMapping("/Open")
	public String open()
	{
		myLogger.warning("Opening door requested");
		myActionService.openDoor();
		return "\"Opening\"";
	}

	@RequestMapping("/DisableAutoClose")
	public String disableAutoClose()
	{
		myStatusService.disableAutoClose();
		return "DisablingAutoClose";
	}
}
