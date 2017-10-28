package com.bigboxer23.garage.services;

import org.springframework.beans.factory.annotation.Autowired;

import java.util.logging.Logger;

/**
 * wrap get/set of controller
 */
public abstract class BaseService
{
	protected static Logger myLogger = Logger.getLogger("com.bigboxer23.BaseService");

	protected GarageDoorMotionService myMotionService;

	protected GarageDoorStatusService myStatusService;

	protected GarageDoorActionService myActionService;

	protected WeatherService myWeatherService;

	protected CommunicationService myCommunicationService;

	@Autowired
	public void setMotionService(GarageDoorMotionService theMotionService)
	{
		myMotionService = theMotionService;
	}

	@Autowired
	public void setStatusService(GarageDoorStatusService theStatusService)
	{
		myStatusService = theStatusService;
	}

	@Autowired
	public void setActionService(GarageDoorActionService theActionService)
	{
		myActionService = theActionService;
	}

	@Autowired
	public void setWeatherService(WeatherService theWeatherService)
	{
		myWeatherService = theWeatherService;
	}

	@Autowired
	public void setCommunicationService(CommunicationService theCommunicationService)
	{
		myCommunicationService = theCommunicationService;
	}
}
