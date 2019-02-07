package com.bigboxer23.garage.services;

import com.bigboxer23.util.http.HttpClientUtils;
import org.apache.http.client.methods.HttpGet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


/**
 * Service to communicate status changes back to a central hub.  If hub url
 * isn't defined, does nothing.
 */
@Component
public class CommunicationService extends BaseService
{
	@Value("${GarageOpenUrl}")
	private String kOpenUrl;

	@Value("${GarageCloseUrl}")
	private String kCloseUrl;

	@Value("${GarageNotificationUrl}")
	private String kNotificationUrl;

	@Value("${GarageMotionUrl}")
	private String kMotionUrl;

	public void garageDoorOpened()
	{
		myLogger.info("Sending Notification");
		doAction(kNotificationUrl);
		myLogger.info("Notification Sent");
		doAction(kOpenUrl);
	}

	public void garageDoorClosed()
	{
		doAction(kCloseUrl);
	}

	public void motionDetected()
	{
		doAction(kMotionUrl);
	}

	private void doAction(String theUrl)
	{
		if (theUrl == null || theUrl.length() == 0)
		{
			return;
		}
		HttpClientUtils.execute(new HttpGet(theUrl));
	}
}
