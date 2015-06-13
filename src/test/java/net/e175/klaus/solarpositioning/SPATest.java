package net.e175.klaus.solarpositioning;

import static org.junit.Assert.assertEquals;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

import org.junit.Test;

public class SPATest {

	private static final double TOLERANCE = 0.0001;

	@Test
	public void testSpaExample() {
		GregorianCalendar time = new GregorianCalendar(new SimpleTimeZone(-7 * 60 * 60 * 1000, "LST"));
		time.set(2003, Calendar.OCTOBER, 17, 12, 30, 30); // 17 October 2003, 12:30:30 LST-07:00

		AzimuthZenithAngle result = SPA.calculateSolarPosition(time, 39.742476, -105.1786, 1830.14, 67, 820, 11);

		assertEquals(194.340241, result.getAzimuth(), TOLERANCE/10);
		assertEquals(50.111622, result.getZenithAngle(), TOLERANCE/10);
	}

	@Test
	public void testNearEquator1() {
		GregorianCalendar time = new GregorianCalendar(new SimpleTimeZone(-4 * 60 * 60 * 1000, "AMT"));
		time.set(2015, Calendar.JUNE, 12, 9, 34, 11);

		AzimuthZenithAngle result = SPA.calculateSolarPosition(time, -3.107, -60.025, 100, 69, 1000, 20);

		assertEquals(51.608, result.getAzimuth(), TOLERANCE);
		assertEquals(44.1425, result.getZenithAngle(), TOLERANCE);
	}

	@Test
	public void testSouthernSolstice() {
		GregorianCalendar time = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
		time.set(2012, Calendar.DECEMBER, 22, 12, 0, 0);

		AzimuthZenithAngle result = SPA.calculateSolarPosition(time, -41, 0, 100, 0, 1000, 20);

		assertEquals(359.08592, result.getAzimuth(), TOLERANCE);
		assertEquals(17.5658, result.getZenithAngle(), TOLERANCE);

		result = SPA.calculateSolarPosition(time, -3, 0, 100, 0, 1000, 20);

		assertEquals(180.790356, result.getAzimuth(), TOLERANCE);
		assertEquals(20.4285, result.getZenithAngle(), TOLERANCE);
	}

	@Test
	public void testSillyRefractionParameters() {
		GregorianCalendar time = new GregorianCalendar(new SimpleTimeZone(-7 * 60 * 60 * 1000, "LST"));
		time.set(2003, Calendar.OCTOBER, 17, 12, 30, 30); // 17 October 2003, 12:30:30 LST-07:00

		AzimuthZenithAngle result = SPA.calculateSolarPosition(time, 39.742476, -105.1786, 1830.14, 67, -2, 1000);
		assertEquals(194.34024, result.getAzimuth(), TOLERANCE);
		assertEquals(50.1279, result.getZenithAngle(), TOLERANCE);

		result = SPA.calculateSolarPosition(time, 39.742476, -105.1786, 1830.14, 67);
		assertEquals(194.34024, result.getAzimuth(), TOLERANCE);
		assertEquals(50.1279, result.getZenithAngle(), TOLERANCE);
	}

	@Test
	public void testSpaExampleSunriseTransitSet() {
		GregorianCalendar time = new GregorianCalendar(new SimpleTimeZone(-7 * 60 * 60 * 1000, "LST"));
		time.set(2003, Calendar.OCTOBER, 17, 12, 30, 30); // 17 October 2003, 12:30:30 LST-07:00

		GregorianCalendar[] res = SPA.calculateSunriseTransitSet(time, 39.742476, -105.1786, 67);

		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		df.setTimeZone(time.getTimeZone());

		assertEquals("2003-10-17T06:12:43", df.format(res[0].getTime()));
		assertEquals("2003-10-17T11:46:04", df.format(res[1].getTime()));
		assertEquals("2003-10-17T17:20:19", df.format(res[2].getTime()));
	}

}
