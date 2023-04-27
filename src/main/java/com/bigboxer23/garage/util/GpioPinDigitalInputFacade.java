package com.bigboxer23.garage.util;

import com.pi4j.io.gpio.*;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** */
public class GpioPinDigitalInputFacade {
	private static final Logger logger = LoggerFactory.getLogger(GpioPinDigitalOutputFacade.class);
	private Optional<GpioPinDigitalInput> pin;

	private boolean high = false;

	public GpioPinDigitalInputFacade(Pin pin, PinPullResistance resistance) {
		try {
			this.pin = Optional.ofNullable(GpioFactory.getInstance().provisionDigitalInputPin(pin, resistance));
			this.pin.get().addListener();
		} catch (UnsatisfiedLinkError e) {
			logger.warn("GpioPinDigitalInputFacade: can't load GPIO library, maybe not running on pi");
			this.pin = Optional.empty();
		}
	}

	public boolean isHigh() {
		return pin.map(GpioPinDigital::isHigh).orElse(high);
	}

	public void addListener(GpioPinListenerDigital listener) {
		pin.ifPresent(p -> p.addListener(listener)); // TODO:
	}

	protected void setHigh(boolean high) {
		this.high = high;
	}
}
