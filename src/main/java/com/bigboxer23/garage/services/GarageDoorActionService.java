package com.bigboxer23.garage.services;

import com.bigboxer23.garage.util.GPIOUtils;
import com.pi4j.io.gpio.*;
import org.springframework.stereotype.Component;

/**
 * Service to open or close the garage door by interfacing with Raspberry Pi's GPIO and
 * a relay wired to the door opener
 */
@Component
public class GarageDoorActionService extends BaseService
{
	private GpioPinDigitalOutput myPinTrigger;

	/**
	 * Pin to use for triggering actions
	 */
	private static final Pin kActionPin = GPIOUtils.getPin(Integer.getInteger("GPIO.action.pin", 7));

	/**
	 * The delay between the "press" and the "let go"
	 */
	public static final int kTriggerDelay = Integer.getInteger("triggerDelay", 400);

	public GarageDoorActionService()
	{
		myPinTrigger = GpioFactory.getInstance().provisionDigitalOutputPin(kActionPin, "MyActionPin", PinState.HIGH);
	}

	/**
	 * Close the door if it is open
	 */
	public void closeDoor()
	{
		if(myStatusService.isGarageDoorOpen())
		{
			myLogger.debug("Closing the door.");
			doDoorAction();
		}
	}

	/**
	 * Open the door if it is already closed
	 */
	public void openDoor()
	{
		if(!myStatusService.isGarageDoorOpen())
		{
			myLogger.debug("Opening the door.");
			doDoorAction();
		}
	}

	/**
	 * Trigger the opener to toggle the current state.  There
	 * is no status check with this method.
	 */
	private void doDoorAction()
	{
		myStatusService.changingState();
		myPinTrigger.low();
		try
		{
			Thread.sleep(kTriggerDelay);
		}
		catch (InterruptedException e){}
		myPinTrigger.high();
	}
}
