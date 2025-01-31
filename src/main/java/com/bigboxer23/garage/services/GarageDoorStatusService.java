package com.bigboxer23.garage.services;

import com.bigboxer23.garage.util.GPIOUtils;
import com.bigboxer23.garage.util.GpioPinDigitalFactory;
import com.bigboxer23.garage.util.GpioPinDigitalInputFacade;
import com.pi4j.io.gpio.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Service to monitor the status of the garage door.
 *
 * <p>It runs a thread and checks for the amount of time the door has been opened, triggering the
 * closing of it after a set amount of time.
 */
@Slf4j
@Component
public class GarageDoorStatusService extends BaseService {
	/** Time to wait before closing */
	public static final long kAutoCloseDelay =
			Integer.getInteger("close.delay", 1000 * 60 * 10); // ms * seconds * minutes -> 10 Minutes

	private static final long kDisableAutoCloseDelay = 1000 * 60 * 60 * 3;

	/** Last time the door was detected open */
	private long myOpenTime = -1;

	/** Delay before measuring for real again */
	private long myChangingStateDelay = -1;

	/** During state change delay, the value to return */
	private boolean myTempState = false;

	/** Pin to get our status from */
	private GpioPinDigitalInputFacade myGarageDoorPin;

	private GpioPinDigitalInputFacade myHouseDoorPin;

	private long myLastOpenHouseDoor = System.currentTimeMillis();

	private long historicOpenTime = -1;

	private long lastGarageDoorEvent = -1;

	public GarageDoorStatusService() {
		myGarageDoorPin = GpioPinDigitalFactory.getInstance()
				.provisionDigitalInputPin(
						GPIOUtils.getPin(Integer.getInteger("GPIO.status.pin", 2)), PinPullResistance.PULL_DOWN);
		/*
		 * Listen for status changes.  These can apparently trigger multiple times even
		 * when the status isn't really changing, so we use the open time as our gauge for "last"
		 * status, and don't set close status (or open) unless we're moving from the opposite
		 * state.
		 *
		 * @param theEvent
		 */
		myGarageDoorPin.addListener(event -> {
			if (lastGarageDoorEvent + 2000 > System.currentTimeMillis()) {
				log.debug("ignoring garage open/close event for debounce: "
						+ event.getState().isHigh()
						+ " "
						+ isGarageDoorOpen());
				return;
			}
			lastGarageDoorEvent = System.currentTimeMillis();
			if (isGarageDoorOpen() && myOpenTime < 0) {
				myOpenTime = System.currentTimeMillis() + kAutoCloseDelay;
				historicOpenTime = System.currentTimeMillis();
				log.info("Garage Door Opened.");
				myCommunicationService.garageDoorOpened();
			}
			if (!isGarageDoorOpen() && myOpenTime != -1) {
				myOpenTime = -1;
				log.info("Garage Door Closed.");
				myCommunicationService.garageDoorClosed();
			}
		});
		if (isGarageDoorOpen()) {
			myOpenTime = System.currentTimeMillis() + kAutoCloseDelay;
			historicOpenTime = System.currentTimeMillis();
		}
		log.info("GarageDoorStatusService Startup:"
				+ (isGarageDoorOpen() ? "Garage Door Opened." : "Garage Door Closed."));
		myHouseDoorPin = GpioPinDigitalFactory.getInstance()
				.provisionDigitalInputPin(
						GPIOUtils.getPin(Integer.getInteger("GPIO.status.house.pin", 6)), PinPullResistance.PULL_UP);
		myHouseDoorPin.addListener(theEvent -> {
			if (isHouseDoorOpen()) {
				myLastOpenHouseDoor = System.currentTimeMillis();
				resetGarageDoorOpenTime();
				myCommunicationService.houseDoorOpened();
				// Maybe?: myActionService.openDoor();
			}
		});
	}

	private boolean isHouseDoorOpen() {
		return myHouseDoorPin.isHigh();
	}

	/**
	 * @return true if opened within 5 seconds
	 */
	public boolean isHouseDoorRecentlyOpened() {
		log.warn("time: "
				+ System.currentTimeMillis()
				+ " "
				+ myLastOpenHouseDoor
				+ " "
				+ (System.currentTimeMillis() - myLastOpenHouseDoor));
		return System.currentTimeMillis() - myLastOpenHouseDoor <= 15000;
	}

	public long getLastHouseDoorOpen() {
		return myLastOpenHouseDoor;
	}

	public void resetGarageDoorOpenTime() {
		if (isGarageDoorOpen() && myOpenTime > 0 && (myOpenTime - System.currentTimeMillis()) < kAutoCloseDelay) {
			log.debug("Resetting close time");
			myOpenTime = System.currentTimeMillis() + kAutoCloseDelay;
		}
	}

	/**
	 * Get the amount of time before auto close happens
	 *
	 * @return milliseconds until auto close happens
	 */
	public long getAutoCloseTimeRemaining() {
		return Math.max(0, myOpenTime - System.currentTimeMillis());
	}

	/**
	 * Garage door is going open -> close or close -> open. Takes 12 secs to get there, don't want
	 * to return non-correct status until it's actually changed.
	 */
	public void changingState() {
		myTempState = !isGarageDoorOpen();
		// Garage door takes 12 seconds to open, only get state after that amount of time has
		// passed.
		myChangingStateDelay = System.currentTimeMillis() + (12 * 1000);
	}

	public boolean isGarageDoorOpen() {
		if (myChangingStateDelay > System.currentTimeMillis()) {
			return myTempState;
		}
		myChangingStateDelay = -1;
		boolean anIsOpen = !myGarageDoorPin.isHigh();
		log.debug("Garage is " + (anIsOpen ? "Open" : "Closed"));
		return anIsOpen;
	}

	public void setAutoCloseDelay(long theOpenTime) {
		if (isGarageDoorOpen()) {
			log.info("setting auto close delay " + theOpenTime);
			myOpenTime = System.currentTimeMillis() + theOpenTime;
		}
	}

	/** Set the auto close time forward 10x the normal wait, so won't close for a long while */
	public void disableAutoClose() {
		setAutoCloseDelay(myOpenTime + kDisableAutoCloseDelay);
	}

	/**
	 * Check if we're open and if we've been opened too long. If so, use the action service to close
	 * the door.
	 */
	@Scheduled(fixedRate = 10000)
	public void iterate() {
		log.debug("open time: "
				+ myOpenTime
				+ " current: "
				+ System.currentTimeMillis()
				+ " check: "
				+ (System.currentTimeMillis() - myOpenTime));
		if (myOpenTime > 0 && (System.currentTimeMillis() - myOpenTime - 30000) > 0) {
			myCommunicationService.garageDoorClosing();
		}
		if (myOpenTime > 0 && (System.currentTimeMillis() - myOpenTime) > 0) {
			log.info("Garage has been open too long, closing.");
			if (!isGarageDoorOpen()) {
				myOpenTime = -1;
				return;
			}
			myOpenTime = System.currentTimeMillis() + kAutoCloseDelay;
			myActionService.closeDoor();
		}
		if (myOpenTime == -1 && isGarageDoorOpen()) {
			log.warn("Detected unset open time and open garage door, setting open time");
			myOpenTime = System.currentTimeMillis() + kAutoCloseDelay;
		}
	}

	public long getHistoricOpenTime() {
		return historicOpenTime;
	}
}
