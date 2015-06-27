package net.e175.klaus.solarpositioning;

import org.junit.Test;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

import static java.lang.Math.PI;
import static java.lang.Math.toDegrees;
import static org.junit.Assert.assertEquals;

public class Grena3Test {

	private static final double TOLERANCE = 0.01; // advertised max error vis-a-vis SPA

	@Test
	public void cSampleComparison() {
		GregorianCalendar time = new GregorianCalendar(new SimpleTimeZone(+1 * 60 * 60 * 1000, "CET"));
		time.set(2012, Calendar.JANUARY, 1, 12, 0, 0);

		AzimuthZenithAngle result = Grena3.calculateSolarPosition(time,
				toDegrees(0.73117), toDegrees(0.21787), 65, 1000, 20);

		assertEquals(toDegrees(1.1338), result.getZenithAngle(), TOLERANCE/10);
		assertEquals(toDegrees(-0.059186 + PI) % 360.0, result.getAzimuth(), TOLERANCE/10);
	}

	@Test
	public void spaComparison() {
		GregorianCalendar time = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
		time.set(2015, Calendar.JUNE, 28, 17, 45, 12);

		AzimuthZenithAngle result = Grena3.calculateSolarPosition(time,
				52.509663, 13.376481, 68, 1000, 20);

		assertEquals(291.232854, result.getAzimuth(), TOLERANCE);
		assertEquals(76.799924, result.getZenithAngle(), TOLERANCE);
	}

	// TODO: more tests needed (other hemispheres, polar circle, etc.)

}
