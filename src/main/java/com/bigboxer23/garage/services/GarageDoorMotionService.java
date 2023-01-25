package com.bigboxer23.garage.services;

import com.bigboxer23.garage.util.GPIOUtils;
import com.pi4j.io.gpio.*;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import org.springframework.stereotype.Component;

/**
 * Sensor wired up to GPIO3 (pin 15), 5v (pin 2), grd (pin 6) Listens for state change (meaning
 * motion) and tells the status service to reset its auto close timer (we're actively in the garage
 * working on something)
 */
@Component
public class GarageDoorMotionService extends BaseService {
	/** Pin to use for status */
	private static final Pin kMotionPin = GPIOUtils.getPin(Integer.getInteger("GPIO.motion.pin", 3));

	/** Delay because the sensor bounces up and down so we don't want to reset a bunch of times */
	private static final long kDelay = 5 * 1000; // 5 seconds

	private GpioPinDigitalInput myStatusPin;

	/** Last time we've detected motion */
	private long myLastTime = -1;

	public GarageDoorMotionService() {
		GpioController aGPIOFactory = GpioFactory.getInstance();
		myStatusPin = aGPIOFactory.provisionDigitalInputPin(kMotionPin, PinPullResistance.PULL_DOWN);
		/*
		 * Listen for status changes, inform the status service we're working so it won't auto
		 * close the door.
		 *
		 * @param theEvent
		 */
		myStatusPin.addListener((GpioPinListenerDigital) theEvent -> {
			if (isMotionDetected() && (myLastTime == -1 || System.currentTimeMillis() - myLastTime > kDelay)) {
				myLogger.debug("Motion detected, within debounce time, ignoring.");
				myLastTime = System.currentTimeMillis();
				return;
			}
			myLastTime = -1;
			myLogger.info("Motion detected.");
			myStatusService.resetGarageDoorOpenTime();
			myCommunicationService.motionDetected();
		});
	}

	public boolean isMotionDetected() {
		return myStatusPin.getState().isHigh();
	}
}
