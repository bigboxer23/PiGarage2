package com.bigboxer23.garage.util;

import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.PinState;

/** */
public class GpioPinDigitalFactory {
	private static GpioPinDigitalFactory instance;

	public static GpioPinDigitalFactory getInstance() {
		if (instance == null) {
			instance = new GpioPinDigitalFactory();
		}
		return instance;
	}

	public GpioPinDigitalOutputFacade provisionDigitalOutputPin(Pin pin, PinState defaultState) {
		return new GpioPinDigitalOutputFacade(pin, defaultState);
	}

	public GpioPinDigitalInputFacade provisionDigitalInputPin(Pin pin, PinPullResistance resistance) {
		return new GpioPinDigitalInputFacade(pin, resistance);
	}
}
