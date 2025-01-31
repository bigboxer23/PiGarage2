package com.bigboxer23.garage.util;

import com.pi4j.io.gpio.*;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

/** */
@Slf4j
public class GpioPinDigitalInputFacade {
	private Optional<GpioPinDigitalInput> pin;

	private boolean high = false;

	public GpioPinDigitalInputFacade(Pin pin, PinPullResistance resistance) {
		try {
			this.pin = Optional.ofNullable(GpioFactory.getInstance().provisionDigitalInputPin(pin, resistance));
		} catch (UnsatisfiedLinkError e) {
			log.warn("GpioPinDigitalInputFacade: can't load GPIO library, maybe not running on pi");
			this.pin = Optional.empty();
		}
	}

	public boolean isHigh() {
		return pin.map(GpioPinDigital::isHigh).orElse(high);
	}

	public void addListener(GpioPinListenerDigital listener) {
		pin.ifPresent(p -> p.addListener(listener)); // TODO:need to add functionality to listen/fire even
		// w/o pin for testing
	}

	protected void setHigh(boolean high) {
		this.high = high;
	}
}
