package com.bigboxer23.garage.util;

import static com.bigboxer23.garage.util.MotionFilter.Result.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class MotionFilterTest {
	private static final long DEBOUNCE = 5_000;

	private static final long WINDOW = 10 * 60_000;

	private static final int MAX_EDGES = 120;

	@Test
	public void firstEdgeIsAccepted() {
		assertEquals(ACCEPTED, new MotionFilter(DEBOUNCE, WINDOW, MAX_EDGES).onRisingEdge(1_000));
	}

	@Test
	public void edgesWithinDebounceAreIgnored() {
		MotionFilter filter = new MotionFilter(DEBOUNCE, WINDOW, MAX_EDGES);
		assertEquals(ACCEPTED, filter.onRisingEdge(0));
		assertEquals(DEBOUNCED, filter.onRisingEdge(1_000));
		assertEquals(DEBOUNCED, filter.onRisingEdge(4_999));
		assertEquals(ACCEPTED, filter.onRisingEdge(5_000));
	}

	@Test
	public void normalActivityNeverTripsFault() {
		MotionFilter filter = new MotionFilter(DEBOUNCE, WINDOW, MAX_EDGES);
		// A busy hour from the logs is ~20 triggers; use one every 30s for two hours
		for (long t = 0; t < 2 * 3_600_000; t += 30_000) {
			assertEquals(ACCEPTED, filter.onRisingEdge(t));
		}
		assertFalse(filter.isFaulty());
	}

	@Test
	public void oscillationIsSuppressedThenRecovers() {
		MotionFilter filter = new MotionFilter(DEBOUNCE, WINDOW, MAX_EDGES);
		long t = 0;
		// Observed failure: a rising edge every ~3.18s
		for (int i = 0; i < MAX_EDGES - 1; i++, t += 3_180) {
			assertNotEquals(SUPPRESSED, filter.onRisingEdge(t));
		}
		assertEquals(SUPPRESSED, filter.onRisingEdge(t));
		assertTrue(filter.isFaulty());
		// Keeps oscillating for an hour, stays suppressed
		for (long end = t + 3_600_000; t < end; t += 3_180) {
			assertEquals(SUPPRESSED, filter.onRisingEdge(t));
		}
		// Wires get jiggled, sensor goes quiet; next real motion is accepted
		t += WINDOW + 1;
		assertEquals(ACCEPTED, filter.onRisingEdge(t));
		assertFalse(filter.isFaulty());
	}

	@Test
	public void hysteresisKeepsFaultUntilRateDropsByHalf() {
		MotionFilter filter = new MotionFilter(DEBOUNCE, 100_000, 10);
		long t = 0;
		for (int i = 0; i < 10; i++, t += 1_000) {
			filter.onRisingEdge(t);
		}
		assertTrue(filter.isFaulty());
		// Slow down to one edge every 15s: window still holds >= half the threshold, still faulty
		for (int i = 0; i < 3; i++) {
			t += 15_000;
			assertEquals(SUPPRESSED, filter.onRisingEdge(t));
		}
		// One edge every 30s: window drains below half the threshold, recovered
		for (int i = 0; i < 4; i++) {
			t += 30_000;
			filter.onRisingEdge(t);
		}
		assertFalse(filter.isFaulty());
	}
}
