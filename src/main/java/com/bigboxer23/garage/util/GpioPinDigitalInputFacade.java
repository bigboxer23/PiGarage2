package com.bigboxer23.garage.util;

import com.pi4j.io.gpio.*;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.extern.slf4j.Slf4j;

/**
 * Wraps a GPIO input pin. When the GPIO library can't be loaded (not running on a pi), the pin
 * state is held in memory and can be driven with {@link #simulate(PinState)}.
 */
@Slf4j
public class GpioPinDigitalInputFacade {
	private Optional<GpioPinDigitalInput> pin;

	private boolean high = false;

	/** Listeners registered while no hardware pin is available, fired by {@link #simulate} */
	private final List<GpioPinListenerDigital> simulatedListeners = new CopyOnWriteArrayList<>();

	public GpioPinDigitalInputFacade(Pin pin, PinPullResistance resistance) {
		try {
			this.pin = Optional.ofNullable(GpioFactory.getInstance().provisionDigitalInputPin(pin, resistance));
		} catch (UnsatisfiedLinkError e) {
			log.warn("GpioPinDigitalInputFacade: can't load GPIO library, maybe not running on pi");
			this.pin = Optional.empty();
		}
	}

	/** Create a facade with no hardware pin behind it, state is driven by {@link #simulate} */
	public GpioPinDigitalInputFacade() {
		this.pin = Optional.empty();
	}

	public boolean isHigh() {
		return pin.map(GpioPinDigital::isHigh).orElse(high);
	}

	public void setDebounce(int debounceMs) {
		pin.ifPresent(p -> p.setDebounce(debounceMs));
	}

	public void addListener(GpioPinListenerDigital listener) {
		if (pin.isPresent()) {
			pin.get().addListener(listener);
		} else {
			simulatedListeners.add(listener);
		}
	}

	/** Set the in-memory pin state without notifying listeners (no effect on a hardware pin) */
	public void setHigh(boolean high) {
		this.high = high;
	}

	/**
	 * Set the in-memory pin state and notify listeners as if the hardware pin changed (no effect on
	 * a hardware pin)
	 */
	public void simulate(PinState state) {
		setHigh(state.isHigh());
		GpioPinDigitalStateChangeEvent event = new GpioPinDigitalStateChangeEvent(this, null, state);
		simulatedListeners.forEach(listener -> listener.handleGpioPinDigitalStateChangeEvent(event));
	}
}
