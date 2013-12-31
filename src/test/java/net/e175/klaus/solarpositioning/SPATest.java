package net.e175.klaus.solarpositioning;

import static org.junit.Assert.assertEquals;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.SimpleTimeZone;

import org.junit.Test;

public class SPATest {

	private static final double TOLERANCE = 0.0001;

	@Test
	public void testSpaExample() {
		GregorianCalendar time = new GregorianCalendar(new SimpleTimeZone(-7 * 60 * 60 * 1000, "LST"));
		time.set(2003, Calendar.OCTOBER, 17, 12, 30, 30); // 17 October 2003, 12:30:30 LST-07:00

		AzimuthZenithAngle result = SPA.calculateSolarPosition(time, 39.742476, -105.1786, 1830.14, 67, 820, 11);

		assertEquals(194.34024, result.getAzimuth(), TOLERANCE);
		assertEquals(50.11162, result.getZenithAngle(), TOLERANCE);
	}

}
