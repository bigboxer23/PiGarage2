package com.bigboxer23.garage.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * wrap get/set of controller
 */
public abstract class BaseService
{
	protected static final Logger myLogger = LoggerFactory.getLogger(BaseService.class);

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
