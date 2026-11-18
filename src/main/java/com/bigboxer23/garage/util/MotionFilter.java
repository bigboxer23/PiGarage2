package com.bigboxer23.garage.util;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Filters raw rising edges from the PIR motion sensor.
 *
 * <ul>
 *   <li>Debounce: once motion is accepted, further edges within {@code debounceMs} are ignored.
 *   <li>Stuck sensor detection: the PIR occasionally falls into a self-retriggering oscillation
 *       (observed: a rising edge every ~3.2s for hours). If at least {@code maxEdges} rising edges
 *       land inside {@code windowMs}, the sensor is considered faulty and all motion is suppressed
 *       until the edge rate drops below half that threshold.
 * </ul>
 *
 * Time is passed in so this can be unit tested without a clock.
 */
public class MotionFilter {
	public enum Result {
		/** Real motion, act on it */
		ACCEPTED,
		/** Within debounce window of the last accepted motion */
		DEBOUNCED,
		/** Sensor appears to be oscillating, ignore */
		SUPPRESSED
	}

	private final long debounceMs;

	private final long windowMs;

	private final int maxEdges;

	/** Timestamps of recent rising edges, oldest first, never more than maxEdges entries */
	private final Deque<Long> recentEdges = new ArrayDeque<>();

	private long lastAccepted = -1;

	private boolean faulty = false;

	public MotionFilter(long debounceMs, long windowMs, int maxEdges) {
		if (maxEdges < 2) {
			throw new IllegalArgumentException("maxEdges must be >= 2");
		}
		this.debounceMs = debounceMs;
		this.windowMs = windowMs;
		this.maxEdges = maxEdges;
	}

	public synchronized Result onRisingEdge(long now) {
		recentEdges.addLast(now);
		while (!recentEdges.isEmpty() && now - recentEdges.peekFirst() > windowMs) {
			recentEdges.removeFirst();
		}
		while (recentEdges.size() > maxEdges) {
			recentEdges.removeFirst();
		}
		if (faulty) {
			faulty = recentEdges.size() >= maxEdges / 2;
		} else {
			faulty = recentEdges.size() >= maxEdges;
		}
		if (faulty) {
			return Result.SUPPRESSED;
		}
		if (lastAccepted != -1 && now - lastAccepted < debounceMs) {
			return Result.DEBOUNCED;
		}
		lastAccepted = now;
		return Result.ACCEPTED;
	}

	public synchronized boolean isFaulty() {
		return faulty;
	}
}
