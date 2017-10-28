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
	private static final int kSensorPin = Integer.getInteger("GPIO.temp.pin", 10);

	private DHT22Sensor mySensor;

	private float myCachedTemperature = -1;

	private float myCachedHumidity = -1;

	private long myLastUpdate;

	public WeatherService()
	{
		mySensor = new DHT22Sensor(kSensorPin);
	}

	public float getTemperature()
	{
		myLogger.config("Last update: " + myLastUpdate);
		return myCachedTemperature;
	}

	public float getHumidity()
	{
		myLogger.config("Last update: " + myLastUpdate);
		return myCachedHumidity;
	}

	@Scheduled(fixedRate = 5000)
	private void refreshInformation()
	{
		myLogger.config("Getting new sensorData...");
		myCachedTemperature = mySensor.getTemperature();
		myCachedHumidity = mySensor.getHumidity();
		myLastUpdate = System.currentTimeMillis();
	}
}
