package com.bigboxer23.garage.services;

import com.bigboxer23.garage.util.GPIOUtils;
import com.pi4j.io.gpio.*;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Service to monitor the status of the garage door.
 *
 * It runs a thread and checks for the amount of time the door has been opened, triggering the closing of it after a
 * set amount of time.
 */
@Component
public class GarageDoorStatusService extends BaseService
{
	/**
	 * Time to wait before closing
	 */
	public static final long kAutoCloseDelay = Integer.getInteger("close.delay", 1000 * 60 * 10);//ms * seconds * minutes -> 10 Minutes

	/**
	 * Last time the door was detected open
	 */
	private long myOpenTime = -1;

	/**
	 * Delay before measuring for real again
	 */
	private long myChangingStateDelay = -1;

	/**
	 * During state change delay, the value to return
	 */
	private boolean myTempState = false;

	/**
	 * Pin to get our status from
	 */
	private GpioPinDigitalInput myGarageDoorPin;

	private GpioPinDigitalInput myHouseDoorPin;

	private long myLastOpenHouseDoor = System.currentTimeMillis();

	public GarageDoorStatusService()
	{
		GpioController aGPIOFactory = GpioFactory.getInstance();
		myGarageDoorPin = aGPIOFactory.provisionDigitalInputPin(GPIOUtils.getPin(Integer.getInteger("GPIO.status.pin", 2)), PinPullResistance.PULL_DOWN);
		/*
		 * Listen for status changes.  These can apparently trigger multiple times even
		 * when the status isn't really changing, so we use the open time as our gauge for "last"
		 * status, and don't set close status (or open) unless we're moving from the opposite
		 * state.
		 *
		 * @param theEvent
		 */
		myGarageDoorPin.addListener((GpioPinListenerDigital) theEvent ->
		{
			if(isGarageDoorOpen() && myOpenTime < 0)
			{
				myOpenTime = System.currentTimeMillis() + kAutoCloseDelay;
				myLogger.info("Garage Door Opened.");
				myCommunicationService.garageDoorOpened();
			}
			if(!isGarageDoorOpen() && myOpenTime != -1)
			{
				myOpenTime = -1;
				myLogger.info("Garage Door Closed.");
				myCommunicationService.garageDoorClosed();
			}
		});
		if(isGarageDoorOpen())
		{
			myOpenTime = System.currentTimeMillis() + kAutoCloseDelay;
		}
		myLogger.info("GarageDoorStatusService Startup:" + (isGarageDoorOpen() ? "Garage Door Opened." : "Garage Door Closed."));
		myHouseDoorPin = aGPIOFactory.provisionDigitalInputPin(GPIOUtils.getPin(Integer.getInteger("GPIO.status.house.pin", 6)), PinPullResistance.PULL_UP);
		myHouseDoorPin.addListener((GpioPinListenerDigital) theEvent ->
		{
			if (isHouseDoorOpen())
			{
				myLastOpenHouseDoor = System.currentTimeMillis();
				resetGarageDoorOpenTime();
				myCommunicationService.houseDoorOpened();
				//Maybe?: myActionService.openDoor();
			}
		});
	}

	private boolean isHouseDoorOpen()
	{
		return myHouseDoorPin.getState().isHigh();
	}

	/**
	 *
	 * @return true if opened within 5 seconds
	 */
	public boolean isHouseDoorRecentlyOpened()
	{
		myLogger.warn("time: " + System.currentTimeMillis() + " " + myLastOpenHouseDoor + " " + (System.currentTimeMillis() - myLastOpenHouseDoor));
		return System.currentTimeMillis() - myLastOpenHouseDoor <= 15000;
	}

	public long getLastHouseDoorOpen()
	{
		return myLastOpenHouseDoor;
	}

	public void resetGarageDoorOpenTime()
	{
		if(isGarageDoorOpen() && myOpenTime > 0 && (myOpenTime - System.currentTimeMillis()) < kAutoCloseDelay)
		{
			myLogger.debug("Resetting close time");
			myOpenTime = System.currentTimeMillis() + kAutoCloseDelay;
		}
	}

	/**
	 * Get the amount of time before auto close happens
	 *
	 * @return milliseconds until auto close happens
	 */
	public long getAutoCloseTimeRemaining()
	{
		return myOpenTime > 0 ? (myOpenTime - System.currentTimeMillis()) : 0;
	}

	/**
	 * Garage door is going open -> close or close -> open.
	 * Takes 12 secs to get there, don't want to return non-correct status until it's actually changed.
	 */
	public void changingState()
	{
		myTempState = !isGarageDoorOpen();
		//Garage door takes 12 seconds to open, only get state after that amount of time has passed.
		myChangingStateDelay = System.currentTimeMillis() + (12 * 1000);
	}

	public boolean isGarageDoorOpen()
	{
		if (myChangingStateDelay > System.currentTimeMillis())
		{
			return myTempState;
		}
		myChangingStateDelay = -1;
		boolean anIsOpen = !myGarageDoorPin.getState().isHigh();
		myLogger.debug("Garage is " + (anIsOpen ? "Open" : "Closed"));
		return anIsOpen;
	}

	/**
	 * Set the auto close time forward 10x the normal wait, so won't close for a long while
	 */
	public void disableAutoClose()
	{
		myLogger.info("disabling auto close.");
		myOpenTime = System.currentTimeMillis() + (10 * kAutoCloseDelay);
	}

	/**
	 * Check if we're open and if we've been opened too long.  If so, use the action service to close the door.
	 */
	@Scheduled(fixedRate = 10000)
	public void iterate()
	{
		myLogger.debug("open time: " + myOpenTime + " current: " + System.currentTimeMillis() + " check: " + (System.currentTimeMillis() - myOpenTime));
		if (myOpenTime > 0 && (System.currentTimeMillis() - myOpenTime - 30000) > 0)
		{
			myCommunicationService.garageDoorClosing();
		}
		if(myOpenTime > 0 && (System.currentTimeMillis() - myOpenTime) > 0)
		{
			myLogger.info("Garage has been open too long, closing.");
			if (!isGarageDoorOpen())
			{
				myOpenTime = -1;
				return;
			}
			myOpenTime = System.currentTimeMillis() + kAutoCloseDelay;
			myActionService.closeDoor();
		}
	}
}
