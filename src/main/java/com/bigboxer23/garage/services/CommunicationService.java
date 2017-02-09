package com.bigboxer23.garage.services;

import com.bigboxer23.garage.GarageOpenerApplication;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;

import java.util.logging.Level;

/**
 * Service to communicate status changes back to a central hub.  If hub url
 * isn't defined, does nothing.
 */
@Component
public class CommunicationService extends BaseService
{
	private static String kHubUrl = System.getProperty("HubURL");//, "http://192.168.0.7:8080/Lights/S/Notification/Garage");

	public void garageDoorOpened()
	{
		doAction("Opened");
	}

	public void garageDoorClosed()
	{
		//doAction("Closed");
	}

	private void doAction(String theAction)
	{
		if (kHubUrl == null)
		{
			return;
		}
		try
		{
//			URLConnection aConnection = new URL(kHubUrl + "/" + theAction).openConnection();
//			new String(ByteStreams.toByteArray(aConnection.getInputStream()), Charsets.UTF_8);
		}
		catch (Throwable e)
		{
			myLogger.log(Level.WARNING, "GarageController: ", e);
		}
	}
}
