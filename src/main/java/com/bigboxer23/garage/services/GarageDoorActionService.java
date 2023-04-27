package com.bigboxer23.garage.services;

import com.bigboxer23.garage.util.GPIOUtils;
import com.bigboxer23.garage.util.GpioPinDigitalFactory;
import com.bigboxer23.garage.util.GpioPinDigitalOutputFacade;
import com.pi4j.io.gpio.*;
import org.springframework.stereotype.Component;

/**
 * Service to open or close the garage door by interfacing with Raspberry Pi's GPIO and a relay
 * wired to the door opener
 */
@Component
public class GarageDoorActionService extends BaseService {
	private final GpioPinDigitalOutputFacade pinTrigger;

	/** The delay between the "press" and the "let go" */
	public static final int kTriggerDelay = Integer.getInteger("triggerDelay", 400);

	public GarageDoorActionService() {
		pinTrigger = GpioPinDigitalFactory.getInstance()
				.provisionDigitalOutputPin(GPIOUtils.getPin(Integer.getInteger("GPIO.action.pin", 7)), PinState.HIGH);
	}

	/** Close the door if it is open */
	public void closeDoor() {
		if (myStatusService.isGarageDoorOpen()) {
			myLogger.debug("Closing the door.");
			doDoorAction();
		}
	}

	/** Open the door if it is already closed */
	public void openDoor() {
		if (!myStatusService.isGarageDoorOpen()) {
			myLogger.debug("Opening the door.");
			doDoorAction();
		}
	}

	/**
	 * Trigger the opener to toggle the current state. There is no status check with this method.
	 */
	private void doDoorAction() {
		myStatusService.changingState();
		pinTrigger.low();
		try {
			Thread.sleep(kTriggerDelay);
		} catch (InterruptedException e) {
		}
		pinTrigger.high();
	}
}
