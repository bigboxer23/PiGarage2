package com.bigboxer23.garage.util;

import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinState;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** */
public class GpioPinDigitalOutputFacade {
	private static final Logger logger = LoggerFactory.getLogger(GpioPinDigitalOutputFacade.class);
	private Optional<GpioPinDigitalOutput> pin;

	private boolean high = false;

	public GpioPinDigitalOutputFacade(Pin pin, PinState defaultState) {
		try {
			this.pin = Optional.ofNullable(GpioFactory.getInstance().provisionDigitalOutputPin(pin, defaultState));
		} catch (UnsatisfiedLinkError e) {
			logger.warn("GpioPinDigitalOutputFacade: can't load GPIO library, maybe not running on pi");
			this.pin = Optional.empty();
		}
	}

	public void low() {
		high = false;
		pin.ifPresent(GpioPinDigitalOutput::low);
	}

	public void high() {
		high = true;
		pin.ifPresent(GpioPinDigitalOutput::high);
	}

	protected void setHigh(boolean high) {
		this.high = high;
	}
}
