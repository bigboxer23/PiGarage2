package com.bigboxer23.garage.services;

import com.bigboxer23.garage.util.http.HttpClientUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.logging.Level;

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

	public void garageDoorOpened()
	{
		doAction(kNotificationUrl);
		doAction(kOpenUrl);
	}

	public void garageDoorClosed()
	{
		doAction(kCloseUrl);
	}

	private void doAction(String theUrl)
	{
		if (theUrl == null || theUrl.length() == 0)
		{
			return;
		}
		try
		{
			HttpClientUtils.getInstance().execute(new HttpGet(theUrl));
		}
		catch (Throwable e)
		{
			myLogger.log(Level.WARNING, "GarageController: ", e);
		}
	}
}
