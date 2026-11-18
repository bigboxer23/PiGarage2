package com.bigboxer23.garage.services;

import com.bigboxer23.garage.util.GPIOUtils;
import com.bigboxer23.garage.util.GpioPinDigitalFactory;
import com.bigboxer23.garage.util.GpioPinDigitalInputFacade;
import com.bigboxer23.utils.file.FilePersistedString;
import com.pi4j.io.gpio.*;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import java.time.Clock;
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

	/** Hardware level debounce to filter contact bounce on the house door sensor */
	private static final int kHousePinDebounceMs = 50;

	/** Ignore repeat house door opens within this window (bounce/noise, not a real second open) */
	private static final long kHouseDoorRepeatMs = 2000;

	/** Last time the door was detected open */
	private long myOpenTime = -1;

	/** Delay before measuring for real again */
	private long myChangingStateDelay = -1;

	/** During state change delay, the value to return */
	private boolean myTempState = false;

	/** Pin to get our status from */
	private GpioPinDigitalInputFacade myGarageDoorPin;

	private GpioPinDigitalInputFacade myHouseDoorPin;

	private long myLastOpenHouseDoor;

	/**
	 * Persisted so the last open time survives restarts (written once per open, negligible SD wear)
	 */
	private final FilePersistedString historicOpenTimeFile;

	private long historicOpenTime;

	private long lastGarageDoorEvent = -1;

	private final Clock clock;

	public GarageDoorStatusService() {
		this(GpioPinDigitalFactory.getInstance(), Clock.systemUTC(), new FilePersistedString("garageHistoricOpenTime"));
	}

	GarageDoorStatusService(GpioPinDigitalFactory gpioFactory, Clock clock, FilePersistedString historicOpenTimeFile) {
		this.clock = clock;
		this.historicOpenTimeFile = historicOpenTimeFile;
		myLastOpenHouseDoor = clock.millis();
		historicOpenTime = readHistoricOpenTime();
		myGarageDoorPin = gpioFactory.provisionDigitalInputPin(
				GPIOUtils.getPin(Integer.getInteger("GPIO.status.pin", 2)), PinPullResistance.PULL_DOWN);
		myGarageDoorPin.addListener(this::onGarageDoorPinEvent);
		if (isGarageDoorOpen()) {
			myOpenTime = clock.millis() + kAutoCloseDelay;
			if (historicOpenTime < 0) {
				setHistoricOpenTime(clock.millis());
			}
		}
		log.info("GarageDoorStatusService Startup:"
				+ (isGarageDoorOpen() ? "Garage Door Opened." : "Garage Door Closed."));
		myHouseDoorPin = gpioFactory.provisionDigitalInputPin(
				GPIOUtils.getPin(Integer.getInteger("GPIO.status.house.pin", 6)), PinPullResistance.PULL_UP);
		myHouseDoorPin.setDebounce(kHousePinDebounceMs);
		myHouseDoorPin.addListener(this::onHouseDoorPinEvent);
	}

	/**
	 * Listen for status changes. These can apparently trigger multiple times even when the status
	 * isn't really changing, so we use the open time as our gauge for "last" status, and don't set
	 * close status (or open) unless we're moving from the opposite state.
	 */
	private void onGarageDoorPinEvent(GpioPinDigitalStateChangeEvent event) {
		if (lastGarageDoorEvent + 2000 > clock.millis()) {
			log.debug("ignoring garage open/close event for debounce: "
					+ event.getState().isHigh()
					+ " "
					+ isGarageDoorOpen());
			return;
		}
		lastGarageDoorEvent = clock.millis();
		if (isGarageDoorOpen() && myOpenTime < 0) {
			myOpenTime = clock.millis() + kAutoCloseDelay;
			setHistoricOpenTime(clock.millis());
			log.info("Garage Door Opened.");
			myCommunicationService.garageDoorOpened();
		}
		if (!isGarageDoorOpen() && myOpenTime != -1) {
			myOpenTime = -1;
			log.info("Garage Door Closed.");
			myCommunicationService.garageDoorClosed();
		}
	}

	private void onHouseDoorPinEvent(GpioPinDigitalStateChangeEvent event) {
		if (event.getState() != PinState.HIGH) {
			return;
		}
		long now = clock.millis();
		if (now - myLastOpenHouseDoor < kHouseDoorRepeatMs) {
			log.debug("House door opened within repeat window, ignoring.");
			return;
		}
		myLastOpenHouseDoor = now;
		resetGarageDoorOpenTime();
		myCommunicationService.houseDoorOpened();
		// Maybe?: myActionService.openDoor();
	}

	private boolean isHouseDoorOpen() {
		return myHouseDoorPin.isHigh();
	}

	/**
	 * @return true if opened within 5 seconds
	 */
	public boolean isHouseDoorRecentlyOpened() {
		log.warn("time: " + clock.millis() + " " + myLastOpenHouseDoor + " " + (clock.millis() - myLastOpenHouseDoor));
		return clock.millis() - myLastOpenHouseDoor <= 15000;
	}

	public long getLastHouseDoorOpen() {
		return myLastOpenHouseDoor;
	}

	public void resetGarageDoorOpenTime() {
		if (isGarageDoorOpen() && myOpenTime > 0 && (myOpenTime - clock.millis()) < kAutoCloseDelay) {
			log.debug("Resetting close time");
			myOpenTime = clock.millis() + kAutoCloseDelay;
		}
	}

	/**
	 * Get the amount of time before auto close happens
	 *
	 * @return milliseconds until auto close happens
	 */
	public long getAutoCloseTimeRemaining() {
		return Math.max(0, myOpenTime - clock.millis());
	}

	/**
	 * Garage door is going open -> close or close -> open. Takes 12 secs to get there, don't want
	 * to return non-correct status until it's actually changed.
	 */
	public void changingState() {
		myTempState = !isGarageDoorOpen();
		// Garage door takes 12 seconds to open, only get state after that amount of time has
		// passed.
		myChangingStateDelay = clock.millis() + (12 * 1000);
	}

	public boolean isGarageDoorOpen() {
		if (myChangingStateDelay > clock.millis()) {
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
			myOpenTime = clock.millis() + theOpenTime;
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
				+ clock.millis()
				+ " check: "
				+ (clock.millis() - myOpenTime));
		if (myOpenTime > 0 && (clock.millis() - myOpenTime - 30000) > 0) {
			myCommunicationService.garageDoorClosing();
		}
		if (myOpenTime > 0 && (clock.millis() - myOpenTime) > 0) {
			log.info("Garage has been open too long, closing.");
			if (!isGarageDoorOpen()) {
				myOpenTime = -1;
				return;
			}
			myOpenTime = clock.millis() + kAutoCloseDelay;
			myActionService.closeDoor();
		}
		if (myOpenTime == -1 && isGarageDoorOpen()) {
			log.warn("Detected unset open time and open garage door, setting open time");
			myOpenTime = clock.millis() + kAutoCloseDelay;
		}
	}

	public long getHistoricOpenTime() {
		return historicOpenTime;
	}

	private void setHistoricOpenTime(long time) {
		historicOpenTime = time;
		historicOpenTimeFile.set(String.valueOf(time));
	}

	private long readHistoricOpenTime() {
		String value = historicOpenTimeFile.get();
		if (value == null || value.isEmpty()) {
			return -1;
		}
		try {
			return Long.parseLong(value);
		} catch (NumberFormatException e) {
			log.warn("invalid persisted historic open time: " + value, e);
			return -1;
		}
	}
}
