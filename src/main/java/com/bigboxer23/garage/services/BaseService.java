package com.bigboxer23.garage.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

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
	public void setMotionService(@Lazy GarageDoorMotionService theMotionService)
	{
		myMotionService = theMotionService;
	}

	@Autowired
	public void setStatusService(@Lazy GarageDoorStatusService theStatusService)
	{
		myStatusService = theStatusService;
	}

	@Autowired
	public void setActionService(@Lazy GarageDoorActionService theActionService)
	{
		myActionService = theActionService;
	}

	@Autowired
	public void setWeatherService(@Lazy WeatherService theWeatherService)
	{
		myWeatherService = theWeatherService;
	}

	@Autowired
	public void setCommunicationService(@Lazy CommunicationService theCommunicationService)
	{
		myCommunicationService = theCommunicationService;
	}
}
