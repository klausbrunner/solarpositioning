package net.e175.klaus.solarpositioning;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

import org.junit.Test;

public class JulianDateTest {

	private static final double TOLERANCE = 0.00001;

	@Test
	public void testConstructor() {
		GregorianCalendar utcTime = createCalendar();
		JulianDate julDate = new JulianDate(utcTime);
		assertNotNull(julDate);
	}

	private GregorianCalendar createCalendar() {
		return new GregorianCalendar(TimeZone.getTimeZone("GMT"));
	}

	@Test
	public void testWithTimeZone() {
		GregorianCalendar time = new GregorianCalendar(new SimpleTimeZone(-7 * 60 * 60 * 1000, "LST"));
		time.set(2003, 9, 17, 12, 30, 30); // 17 October 2003, 12:30:30-07:00
		JulianDate julDate = new JulianDate(time);

		assertEquals(2452930.312847, julDate.getJulianDate(), TOLERANCE);
	}

	@Test
	public void testY2K() {
		GregorianCalendar utcTime = createCalendar();
		utcTime.set(2000, 0, 1, 12, 0, 0);
		JulianDate julDate = new JulianDate(utcTime);

		assertEquals(2451545.0, julDate.getJulianDate(), TOLERANCE);
	}

	@Test
	public void testPre1000() {
		GregorianCalendar utcTime = createCalendar();
		utcTime.set(837, 3, 10, 7, 12, 0);
		JulianDate julDate = new JulianDate(utcTime);

		assertEquals(2026871.8, julDate.getJulianDate(), TOLERANCE);
	}

	@Test
	public void testPre0() {
		GregorianCalendar utcTime = createCalendar();
		utcTime.set(123, 11, 31, 0, 0, 0);
		utcTime.set(Calendar.ERA, GregorianCalendar.BC);
		JulianDate julDate = new JulianDate(utcTime);

		assertEquals(1676496.5, julDate.getJulianDate(), TOLERANCE);
	}

	@Test
	public void testPre02() {
		GregorianCalendar utcTime = createCalendar();
		utcTime.set(122, 0, 1, 0, 0, 0);
		utcTime.set(Calendar.ERA, GregorianCalendar.BC);
		JulianDate julDate = new JulianDate(utcTime);

		assertEquals(1676497.5, julDate.getJulianDate(), TOLERANCE);
	}

	@Test
	public void testJulian0() {
		GregorianCalendar utcTime = createCalendar();
		utcTime.set(4712, 0, 1, 12, 0, 0);
		utcTime.set(Calendar.ERA, GregorianCalendar.BC);
		JulianDate julDate = new JulianDate(utcTime);

		assertEquals(0.0, julDate.getJulianDate(), TOLERANCE);
	}

}
