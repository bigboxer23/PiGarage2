package com.bigboxer23.garage;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** A clock that only moves when a test tells it to */
public class TestClock extends Clock {
	private long millis;

	public TestClock(long startMillis) {
		millis = startMillis;
	}

	public void advance(long deltaMillis) {
		millis += deltaMillis;
	}

	@Override
	public long millis() {
		return millis;
	}

	@Override
	public Instant instant() {
		return Instant.ofEpochMilli(millis);
	}

	@Override
	public ZoneId getZone() {
		return ZoneOffset.UTC;
	}

	@Override
	public Clock withZone(ZoneId zone) {
		throw new UnsupportedOperationException();
	}
}
