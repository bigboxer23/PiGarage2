package com.bigboxer23.garage.sensors;

import java.nio.charset.Charset;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

/** Wrapper around the adafruit driver to return temp and humidity from a DHT22 Sensor */
@Slf4j
public class DHT22Sensor {

	private static final String kTemp = "Temp =";
	private static final String kHumidity = "Hum =";
	private static final long kPollingInterval = 60000;

	private final int myPin;
	private String myLastValue;

	private long myLastUpdate = System.currentTimeMillis();

	/**
	 * @param thePin the GPIO pin number to use
	 */
	public DHT22Sensor(int thePin) {
		myPin = thePin;
	}

	public float getHumidity() {
		return parseHumidity(myLastValue);
	}

	public void checkForUpdates() {
		String aValues = readValues();
		log.debug("Values read: " + aValues);
		if (aValues != null && aValues.indexOf('%') > 0) {
			log.debug("Updating values.");
			myLastValue = aValues;
		}
	}

	public synchronized float getTemperature() {
		if (isProcessHung()) {
			killHungAdafruit_DHTProcess();
		}
		return parseTemperature(myLastValue);
	}

	private float parseTemperature(String theValue) {
		if (theValue == null) {
			return Float.MIN_VALUE;
		}
		float aCelsius =
				Float.parseFloat(theValue.substring(theValue.indexOf(kTemp) + kTemp.length(), theValue.indexOf('*')));
		return (aCelsius * 1.8000f) + 32.00f;
	}

	private float parseHumidity(String theValue) {
		if (theValue == null) {
			return Float.MIN_VALUE;
		}
		return Float.parseFloat(
				theValue.substring(theValue.indexOf(kHumidity) + kHumidity.length(), theValue.indexOf('%')));
	}

	private String readValues() {
		try {
			for (int ai = 0; ai < 10; ai++) {
				log.debug("Reading value from sensor");
				Process aProcess = Runtime.getRuntime().exec(String.format("Adafruit_DHT 22 %d", myPin));
				String aResult = IOUtils.toString(aProcess.getInputStream(), Charset.defaultCharset());
				log.debug("done reading value from sensor...");
				if (aResult.contains("Temp")) {
					myLastUpdate = System.currentTimeMillis();
					return aResult;
				}
				log.debug("Bad result from sensor " + aResult);
				Thread.sleep(1000);
			}
			return null;
		} catch (Exception theException) {
			log.error(String.format("Could not read the DHT22 sensor at pin %d", myPin), theException);
			return null;
		}
	}

	/**
	 * If process hasn't updated in > 5min, kill it, try again
	 *
	 * @return
	 */
	private boolean isProcessHung() {
		return myLastUpdate < System.currentTimeMillis() - (300000); // 5 * 60 * 1000, 5min
	}

	private void killHungAdafruit_DHTProcess() {
		try {
			log.debug("Killing hung process, last update " + myLastUpdate);
			int aProcessResult =
					Runtime.getRuntime().exec("sudo killall -9 Adafruit_DHT").waitFor();
			log.info(aProcessResult + " Sensor process is hung, killing process. Last update " + myLastUpdate);
			myLastUpdate = System.currentTimeMillis();
		} catch (Exception theE) {
			log.error("killHungAdafruit_DHTProcess", theE);
		}
	}
}
