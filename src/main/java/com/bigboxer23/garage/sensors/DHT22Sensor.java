package com.bigboxer23.garage.sensors;

import org.apache.commons.io.IOUtils;

import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Wrapper around the adafruit driver to return temp and humidity from
 * a DHT22 Sensor
 */
public class DHT22Sensor
{
	private static Logger myLogger = Logger.getLogger("com.jones.DHT22Sensor");

	private final static String kTemp = "Temp =";
	private final static String kHumidity = "Hum =";
	private static final long kPollingInterval = 60000;

	private final int myPin;
	private String myLastValue;

	/**
	 *
	 * @param thePin the GPIO pin number to use
	 */
	public DHT22Sensor(int thePin)
	{
		myPin = thePin;
		new Thread(() -> {
			while(true)
			{
				checkForUpdates();
				try
				{
					Thread.sleep(kPollingInterval);
				}
				catch (InterruptedException theE)
				{
					theE.printStackTrace();
				}
			}
		}).start();
	}

	public float getHumidity()
	{
		return parseHumidity(myLastValue);
	}

	private void checkForUpdates()
	{
		String aValues = readValues();
		myLogger.config("Values read: " + aValues);
		if (aValues != null && aValues.indexOf('%') > 0)
		{
			myLogger.config("Updating values.");
			myLastValue = aValues;
		}
	}

	public synchronized float getTemperature()
	{
		return parseTemperature(myLastValue);
	}

	private float parseTemperature(String theValue)
	{
		if (theValue == null)
		{
			return Float.MIN_VALUE;
		}
		float aCelsius = Float.parseFloat(theValue.substring(theValue.indexOf(kTemp) + kTemp.length(), theValue.indexOf('*')));
		return (aCelsius * 1.8000f) + 32.00f;
	}

	private float parseHumidity(String theValue)
	{
		if (theValue == null)
		{
			return Float.MIN_VALUE;
		}
		return Float.parseFloat(theValue.substring(theValue.indexOf(kHumidity) + kHumidity.length(), theValue.indexOf('%')));
	}

	private String readValues()
	{
		try
		{
			for(int ai = 0; ai < 10; ai++)
			{
				myLogger.config("Reading value from sensor");
				Process aProcess = Runtime.getRuntime().exec(String.format("Adafruit_DHT 22 %d", myPin));
				String aResult = IOUtils.toString(aProcess.getInputStream(), Charset.defaultCharset());
				myLogger.config("done reading value from sensor...");
				if (aResult.contains("Temp"))
				{
					return aResult;
				}
				myLogger.config("Bad result from sensor " + aResult);
				Thread.sleep(1000);
			}
			return null;
		}
		catch (Exception theException)
		{
			myLogger.log(Level.WARNING, String.format("Could not read the DHT22 sensor at pin %d", myPin), theException);
			return null;
		}
	}
}
