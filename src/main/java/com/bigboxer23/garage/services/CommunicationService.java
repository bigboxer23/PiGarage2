package com.bigboxer23.garage.services;

import com.bigboxer23.utils.http.OkHttpUtil;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Service to communicate status changes back to a central hub. If hub url isn't defined, does
 * nothing.
 */
@Component
public class CommunicationService extends BaseService {
	@Value("${GarageOpenUrl}")
	private String kOpenUrl;

	@Value("${GarageCloseUrl}")
	private String kCloseUrl;

	@Value("${GarageNotificationUrl}")
	private String kNotificationUrl;

	@Value("${GarageMotionUrl}")
	private String kMotionUrl;

	@Value("${GarageHouseDoorUrl}")
	private String kHouseDoorUrl;

	@Value("$GarageCloseWarningUrl")
	private String kGarageCloseWarningUrl;

	public void garageDoorOpened() {
		logger.info("Potentially Sending Notification " + myStatusService.isHouseDoorRecentlyOpened());
		// Don't trigger notification if house door was recently used, assuming someone inside the
		// house is opening it
		if (!myStatusService.isHouseDoorRecentlyOpened()) {
			doAction(kNotificationUrl);
		}
		doAction(kOpenUrl);
	}

	public void garageDoorClosing() {
		logger.debug("garageDoorClosing");
		doAction(kGarageCloseWarningUrl);
	}

	public void garageDoorClosed() {
		doAction(kCloseUrl);
	}

	public void motionDetected() {
		logger.debug("informing motion url");
		doAction(kMotionUrl);
	}

	public void houseDoorOpened() {
		logger.info("House door opened");
		doAction(kHouseDoorUrl);
	}

	private void doAction(String theUrl) {
		if (theUrl == null || theUrl.length() == 0) {
			return;
		}
		try {
			OkHttpUtil.getSynchronous(theUrl, null);
		} catch (IOException e) {
			logger.warn("error doing action: " + theUrl, e);
		}
	}
}
