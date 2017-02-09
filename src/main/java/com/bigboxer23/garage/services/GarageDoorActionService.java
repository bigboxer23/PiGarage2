package com.bigboxer23.garage.services;

import com.bigboxer23.garage.GarageOpenerApplication;
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
			myLogger.config("Closing the door.");
			doDoorAction();
			myCommunicationService.garageDoorClosed();
		}
	}

	/**
	 * Open the door if it is already closed
	 */
	public void openDoor()
	{
		if(!myStatusService.isGarageDoorOpen())
		{
			myLogger.config("Opening the door.");
			doDoorAction();
			myCommunicationService.garageDoorOpened();
		}
	}

	/**
	 * Trigger the opener to toggle the current state.  There
	 * is no status check with this method.
	 */
	private void doDoorAction()
	{
			myPinTrigger.low();
			try
			{
				Thread.sleep(kTriggerDelay);
			}
			catch (InterruptedException e){}
			myPinTrigger.high();
	}
}
