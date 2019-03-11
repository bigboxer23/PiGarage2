package com.bigboxer23.garage.services;

import com.bigboxer23.garage.sensors.DHT22Sensor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Service to get temperature and humidity from a connected sensor
 */
@Component
public class WeatherService extends BaseService
{
	private DHT22Sensor mySensor;

	private float myCachedTemperature = -1;

	private float myCachedHumidity = -1;

	private long myLastUpdate;

	public WeatherService()
	{
		mySensor = new DHT22Sensor(Integer.getInteger("GPIO.temp.pin", 10));
	}

	public float getTemperature()
	{
		myLogger.debug("Last update: " + myLastUpdate);
		return myCachedTemperature;
	}

	public float getHumidity()
	{
		myLogger.debug("Last update: " + myLastUpdate);
		return myCachedHumidity;
	}

	@Scheduled(fixedRate = 5000)
	private void refreshInformation()
	{
		myLogger.debug("Getting new sensorData...");
		myCachedTemperature = mySensor.getTemperature();
		myCachedHumidity = mySensor.getHumidity();
		myLastUpdate = System.currentTimeMillis();
	}

	@Scheduled(fixedRate = 60000)
	private void queryFromSensor()
	{
		mySensor.checkForUpdates();
	}
}
