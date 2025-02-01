package com.bigboxer23.garage.util;

import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinState;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

/** */
@Slf4j
public class GpioPinDigitalOutputFacade {
	private Optional<GpioPinDigitalOutput> pin;

	private boolean high = false;

	public GpioPinDigitalOutputFacade(Pin pin, PinState defaultState) {
		try {
			this.pin = Optional.ofNullable(GpioFactory.getInstance().provisionDigitalOutputPin(pin, defaultState));
		} catch (UnsatisfiedLinkError e) {
			log.warn("GpioPinDigitalOutputFacade: can't load GPIO library, maybe not running on pi");
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
