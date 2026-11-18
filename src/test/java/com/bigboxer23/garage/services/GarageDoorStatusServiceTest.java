package com.bigboxer23.garage.services;

import static com.bigboxer23.garage.services.GarageDoorStatusService.kAutoCloseDelay;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.bigboxer23.garage.TestClock;
import com.bigboxer23.garage.util.GpioPinDigitalFactory;
import com.bigboxer23.garage.util.GpioPinDigitalInputFacade;
import com.bigboxer23.utils.file.FilePersistedString;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GarageDoorStatusServiceTest {
	private static final long START = 1_700_000_000_000L;

	/** Past the 2s debounce on both door pins */
	private static final long PAST_DEBOUNCE = 2_001;

	private final TestClock clock = new TestClock(START);

	/** Garage door reed switch: high == closed */
	private final GpioPinDigitalInputFacade garagePin = new GpioPinDigitalInputFacade();

	private final GpioPinDigitalInputFacade housePin = new GpioPinDigitalInputFacade();

	private final InMemoryPersistedString historicFile = new InMemoryPersistedString();

	private final CommunicationService communication = mock(CommunicationService.class);

	private final GarageDoorActionService action = mock(GarageDoorActionService.class);

	private final GpioPinDigitalFactory gpio = new GpioPinDigitalFactory() {
		@Override
		public GpioPinDigitalInputFacade provisionDigitalInputPin(Pin pin, PinPullResistance resistance) {
			if (pin == RaspiPin.GPIO_02) {
				return garagePin;
			}
			if (pin == RaspiPin.GPIO_06) {
				return housePin;
			}
			throw new IllegalArgumentException("unexpected pin " + pin);
		}
	};

	@BeforeEach
	public void closedByDefault() {
		garagePin.setHigh(true);
	}

	private GarageDoorStatusService start() {
		GarageDoorStatusService service = new GarageDoorStatusService(gpio, clock, historicFile);
		service.setCommunicationService(communication);
		service.setActionService(action);
		return service;
	}

	private void openGarageDoor() {
		garagePin.simulate(PinState.LOW);
	}

	private void closeGarageDoor() {
		garagePin.simulate(PinState.HIGH);
	}

	// --- startup ---

	@Test
	public void startupClosed() {
		historicFile.set("123");
		GarageDoorStatusService service = start();
		assertFalse(service.isGarageDoorOpen());
		assertEquals(0, service.getAutoCloseTimeRemaining());
		assertEquals(123, service.getHistoricOpenTime());
		assertEquals(START, service.getLastHouseDoorOpen());
	}

	@Test
	public void startupOpenWithNoHistorySetsDeadlineAndPersistsOpenTime() {
		garagePin.setHigh(false);
		GarageDoorStatusService service = start();
		assertTrue(service.isGarageDoorOpen());
		assertEquals(kAutoCloseDelay, service.getAutoCloseTimeRemaining());
		assertEquals(START, service.getHistoricOpenTime());
		assertEquals(String.valueOf(START), historicFile.get());
	}

	@Test
	public void startupOpenKeepsPersistedOpenTime() {
		historicFile.set("123");
		garagePin.setHigh(false);
		GarageDoorStatusService service = start();
		assertEquals(kAutoCloseDelay, service.getAutoCloseTimeRemaining());
		assertEquals(123, service.getHistoricOpenTime());
		assertEquals("123", historicFile.get());
	}

	@Test
	public void missingOrInvalidPersistedOpenTimeIsUnset() {
		assertEquals(-1, start().getHistoricOpenTime());
		historicFile.set("");
		assertEquals(-1, start().getHistoricOpenTime());
		historicFile.set("not a number");
		assertEquals(-1, start().getHistoricOpenTime());
	}

	// --- garage door pin events ---

	@Test
	public void doorOpeningStartsTimerPersistsAndNotifies() {
		GarageDoorStatusService service = start();
		clock.advance(PAST_DEBOUNCE);
		openGarageDoor();
		assertTrue(service.isGarageDoorOpen());
		assertEquals(kAutoCloseDelay, service.getAutoCloseTimeRemaining());
		assertEquals(START + PAST_DEBOUNCE, service.getHistoricOpenTime());
		assertEquals(String.valueOf(START + PAST_DEBOUNCE), historicFile.get());
		verify(communication).garageDoorOpened();
		verifyNoMoreInteractions(communication);
	}

	@Test
	public void doorClosingClearsTimerAndNotifies() {
		GarageDoorStatusService service = start();
		openGarageDoor();
		clock.advance(PAST_DEBOUNCE);
		closeGarageDoor();
		assertFalse(service.isGarageDoorOpen());
		assertEquals(0, service.getAutoCloseTimeRemaining());
		verify(communication).garageDoorClosed();
	}

	@Test
	public void eventsWithinDebounceAreIgnored() {
		GarageDoorStatusService service = start();
		openGarageDoor();
		clock.advance(1_999);
		closeGarageDoor();
		verify(communication, never()).garageDoorClosed();
		// the pin really is closed now; the next event outside the window catches up
		clock.advance(2);
		closeGarageDoor();
		verify(communication).garageDoorClosed();
		assertEquals(0, service.getAutoCloseTimeRemaining());
	}

	@Test
	public void repeatEventsInSameStateDoNotRenotify() {
		start();
		openGarageDoor();
		clock.advance(PAST_DEBOUNCE);
		openGarageDoor();
		verify(communication, times(1)).garageDoorOpened();

		clock.advance(PAST_DEBOUNCE);
		closeGarageDoor();
		clock.advance(PAST_DEBOUNCE);
		closeGarageDoor();
		verify(communication, times(1)).garageDoorClosed();
	}

	@Test
	public void closeEventWhenNeverOpenedDoesNotNotify() {
		start();
		closeGarageDoor();
		verifyNoInteractions(communication);
	}

	// --- changingState ---

	@Test
	public void changingStateReportsPredictedStateFor12Seconds() {
		GarageDoorStatusService service = start();
		service.changingState();
		assertTrue(service.isGarageDoorOpen(), "predicted open while the door travels");
		clock.advance(11_999);
		assertTrue(service.isGarageDoorOpen());
		clock.advance(1);
		assertFalse(service.isGarageDoorOpen(), "door never moved, pin wins after 12s");
	}

	// --- timer adjustments ---

	@Test
	public void resetGarageDoorOpenTimeRestartsTimerWhenOpen() {
		garagePin.setHigh(false);
		GarageDoorStatusService service = start();
		clock.advance(60_000);
		assertEquals(kAutoCloseDelay - 60_000, service.getAutoCloseTimeRemaining());
		service.resetGarageDoorOpenTime();
		assertEquals(kAutoCloseDelay, service.getAutoCloseTimeRemaining());
	}

	@Test
	public void resetGarageDoorOpenTimeDoesNothingWhenClosed() {
		GarageDoorStatusService service = start();
		service.resetGarageDoorOpenTime();
		assertEquals(0, service.getAutoCloseTimeRemaining());
	}

	@Test
	public void setAutoCloseDelayOnlyAppliesWhenOpen() {
		GarageDoorStatusService service = start();
		service.setAutoCloseDelay(5_000);
		assertEquals(0, service.getAutoCloseTimeRemaining());

		garagePin.setHigh(false);
		service.setAutoCloseDelay(5_000);
		assertEquals(5_000, service.getAutoCloseTimeRemaining());
	}

	@Test
	public void autoCloseTimeRemainingNeverNegative() {
		garagePin.setHigh(false);
		GarageDoorStatusService service = start();
		clock.advance(kAutoCloseDelay + 60_000);
		assertEquals(0, service.getAutoCloseTimeRemaining());
	}

	// --- iterate (auto close) ---

	@Test
	public void iterateBeforeDeadlineDoesNotClose() {
		garagePin.setHigh(false);
		GarageDoorStatusService service = start();
		clock.advance(kAutoCloseDelay);
		service.iterate();
		verify(action, never()).closeDoor();
	}

	@Test
	public void iteratePastDeadlineClosesAndRearms() {
		garagePin.setHigh(false);
		GarageDoorStatusService service = start();
		clock.advance(kAutoCloseDelay + 1);
		service.iterate();
		verify(action).closeDoor();
		assertEquals(kAutoCloseDelay, service.getAutoCloseTimeRemaining());
	}

	@Test
	public void iteratePastDeadlineWithDoorAlreadyClosedClearsTimer() {
		garagePin.setHigh(false);
		GarageDoorStatusService service = start();
		garagePin.setHigh(true); // closed without the pin event firing
		clock.advance(kAutoCloseDelay + 1);
		service.iterate();
		verify(action, never()).closeDoor();
		assertEquals(0, service.getAutoCloseTimeRemaining());
	}

	@Test
	public void iterateArmsTimerWhenDoorOpenWithoutEvent() {
		GarageDoorStatusService service = start();
		garagePin.setHigh(false); // opened without the pin event firing
		service.iterate();
		verify(action, never()).closeDoor();
		assertEquals(kAutoCloseDelay, service.getAutoCloseTimeRemaining());
	}

	@Test
	public void iterateWhenClosedDoesNothing() {
		GarageDoorStatusService service = start();
		clock.advance(kAutoCloseDelay * 2);
		service.iterate();
		verifyNoInteractions(action, communication);
		assertEquals(0, service.getAutoCloseTimeRemaining());
	}

	// --- house door ---

	@Test
	public void houseDoorOpenNotifiesAndRestartsGarageTimer() {
		garagePin.setHigh(false);
		GarageDoorStatusService service = start();
		clock.advance(60_000);
		housePin.simulate(PinState.HIGH);
		verify(communication).houseDoorOpened();
		assertEquals(START + 60_000, service.getLastHouseDoorOpen());
		assertEquals(kAutoCloseDelay, service.getAutoCloseTimeRemaining());
	}

	@Test
	public void houseDoorRepeatWithinWindowIgnored() {
		GarageDoorStatusService service = start();
		clock.advance(PAST_DEBOUNCE);
		housePin.simulate(PinState.HIGH);
		clock.advance(1_999);
		housePin.simulate(PinState.HIGH);
		verify(communication, times(1)).houseDoorOpened();
		assertEquals(START + PAST_DEBOUNCE, service.getLastHouseDoorOpen());
		clock.advance(1);
		housePin.simulate(PinState.HIGH);
		verify(communication, times(2)).houseDoorOpened();
	}

	@Test
	public void houseDoorWithinWindowOfStartupIgnored() {
		start();
		clock.advance(1_999);
		housePin.simulate(PinState.HIGH);
		verifyNoInteractions(communication);
	}

	@Test
	public void houseDoorClosingIgnored() {
		start();
		clock.advance(PAST_DEBOUNCE);
		housePin.simulate(PinState.LOW);
		verifyNoInteractions(communication);
	}

	@Test
	public void houseDoorRecentlyOpenedFor15Seconds() {
		GarageDoorStatusService service = start();
		clock.advance(PAST_DEBOUNCE);
		housePin.simulate(PinState.HIGH);
		clock.advance(15_000);
		assertTrue(service.isHouseDoorRecentlyOpened());
		clock.advance(1);
		assertFalse(service.isHouseDoorRecentlyOpened());
	}

	/** Keeps the value in memory instead of writing .index_ files into the working directory */
	private static class InMemoryPersistedString extends FilePersistedString {
		private String value;

		InMemoryPersistedString() {
			super("unused");
		}

		@Override
		public String get() {
			return value;
		}

		@Override
		public void set(String value) {
			this.value = value;
		}
	}
}
