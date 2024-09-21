package com.bigboxer23.garage;

import static com.bigboxer23.garage.services.GarageDoorStatusService.kAutoCloseDelay;

import com.bigboxer23.garage.services.BaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

/** Controller to return data about sensor's input */
@RestController
@EnableAutoConfiguration
@Tag(name = "Garage Controller", description = "Service to control the garage door pi")
public class WebServiceController extends BaseService {
	/**
	 * Get the distance to the object in front of sensor, by calling python script
	 *
	 * @return
	 */
	@GetMapping(value = "/Status2", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(
			summary = "Get status about the garage",
			description = "Gets various statuses associated with the garage's current state (temp,"
					+ " humidity, state, autoclose, etc)")
	public GarageData getStatus() {
		logger.debug("Checking status requested");
		return getGarageData();
	}

	private GarageData getGarageData() {
		return new GarageData(
				myWeatherService.getTemperature(),
				myWeatherService.getHumidity(),
				myStatusService.isGarageDoorOpen(),
				myStatusService.getAutoCloseTimeRemaining(),
				myStatusService.getLastHouseDoorOpen(),
				myStatusService.getHistoricOpenTime());
	}

	@GetMapping(value = "/Close", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "close the garage", description = "If open, this endpoint will trigger garage closing")
	public GarageData close() {
		logger.info("Closing door requested");
		myActionService.closeDoor();
		myStatusService.setAutoCloseDelay(-1);
		GarageData aData = getGarageData();
		aData.setStatus("false");
		return aData;
	}

	@GetMapping(value = "/Open", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "open the garage", description = "If closed, this endpoint will trigger garage opening")
	public GarageData open() {
		logger.info("Opening door requested");
		myActionService.openDoor();
		GarageData aData = getGarageData();
		aData.setAutoClose(kAutoCloseDelay);
		aData.setStatus("true");
		return aData;
	}

	@GetMapping(value = "/ToggleOpenClosed", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(
			summary = "open or close the garage depending on current state",
			description = "If closed, this endpoint will trigger garage opening and vice versa")
	public GarageData toggleOpenClosed() {
		GarageData data = getGarageData();
		boolean isOpen = Boolean.parseBoolean(data.getStatus());
		if (isOpen) {
			myActionService.closeDoor();
		} else {
			myActionService.openDoor();
		}
		data.setStatus(String.valueOf(!isOpen));
		return data;
	}

	@GetMapping(value = "/SetAutoCloseDelay/{delay}", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(
			summary = "disables auto close by some delay",
			description = "Turns auto close off for delay defined by the path variable")
	public GarageData setAutoCloseDelay(
			@Parameter(description = "how long to delay auto close, in ms") @PathVariable(value = "delay") Long delay) {
		logger.info("set auto close requested: " + delay);
		myStatusService.setAutoCloseDelay(delay);
		return getGarageData();
	}

	@GetMapping(value = "/DisableAutoClose", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(
			summary = "disables auto close of the garage by 3 hours",
			description = "Turns auto close off for the next three hours")
	public GarageData disableAutoClose() {
		logger.info("disabling auto close requested.");
		myStatusService.disableAutoClose();
		return getGarageData();
	}
}
