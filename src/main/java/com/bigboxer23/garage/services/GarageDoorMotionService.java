package com.bigboxer23.garage.services;

import com.bigboxer23.garage.util.GPIOUtils;
import com.bigboxer23.garage.util.GpioPinDigitalFactory;
import com.bigboxer23.garage.util.GpioPinDigitalInputFacade;
import com.bigboxer23.garage.util.MotionFilter;
import com.pi4j.io.gpio.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * DYP-ME003 PIR sensor wired up to GPIO3 (pin 15), 5v (pin 2), grd (pin 6). A rising edge means
 * motion, which tells the status service to reset its auto close timer (we're actively in the
 * garage working on something).
 *
 * <p>The sensor sometimes gets stuck oscillating (a flaky connection or supply noise makes it
 * retrigger itself every few seconds). That would keep the door from ever auto closing, so {@link
 * MotionFilter} detects it and suppresses motion until the sensor calms down.
 */
@Slf4j
@Component
public class GarageDoorMotionService extends BaseService {
	/** Ignore repeat motion within this long of the last accepted motion */
	private static final long kDebounce = Long.getLong("motion.debounce", 5 * 1000);

	/** Window and edge count used to decide the sensor is stuck oscillating */
	private static final long kFaultWindow = Long.getLong("motion.fault.window", 10 * 60 * 1000);

	private static final int kFaultEdges = Integer.getInteger("motion.fault.edges", 120);

	/** Hardware level debounce to filter electrical chatter on the line */
	private static final int kPinDebounceMs = 50;

	private final GpioPinDigitalInputFacade statusPin;

	private final MotionFilter filter = new MotionFilter(kDebounce, kFaultWindow, kFaultEdges);

	private boolean reportedFaulty = false;

	public GarageDoorMotionService() {
		statusPin = GpioPinDigitalFactory.getInstance()
				.provisionDigitalInputPin(
						GPIOUtils.getPin(Integer.getInteger("GPIO.motion.pin", 3)), PinPullResistance.PULL_DOWN);
		statusPin.setDebounce(kPinDebounceMs);
		statusPin.addListener(theEvent -> {
			if (theEvent.getState() != PinState.HIGH) {
				return;
			}
			onMotionEdge(System.currentTimeMillis());
		});
	}

	private void onMotionEdge(long now) {
		MotionFilter.Result result = filter.onRisingEdge(now);
		if (result == MotionFilter.Result.SUPPRESSED) {
			if (!reportedFaulty) {
				reportedFaulty = true;
				log.warn("Motion sensor appears stuck (>= "
						+ kFaultEdges
						+ " triggers in "
						+ (kFaultWindow / 1000)
						+ "s), ignoring motion until it settles. Check sensor wiring.");
			}
			return;
		}
		if (reportedFaulty) {
			reportedFaulty = false;
			log.warn("Motion sensor has settled, resuming motion detection.");
		}
		if (result == MotionFilter.Result.DEBOUNCED) {
			log.debug("Motion detected, within debounce time, ignoring.");
			return;
		}
		log.info("Motion detected.");
		myStatusService.resetGarageDoorOpenTime();
		myCommunicationService.motionDetected();
	}

	public boolean isMotionDetected() {
		return statusPin.isHigh() && !filter.isFaulty();
	}
}
