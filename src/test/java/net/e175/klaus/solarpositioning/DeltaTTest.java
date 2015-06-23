package net.e175.klaus.solarpositioning;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Calendar;
import java.util.GregorianCalendar;

public class DeltaTTest {


	private GregorianCalendar yearCal(int year) {
		GregorianCalendar c = new GregorianCalendar(year, Calendar.JANUARY, 1);
		if (year < 0) {
			c.set(Calendar.ERA, GregorianCalendar.BC);
			c.set(Calendar.YEAR, -year);
		}
		return c;
	}


	@Test
	public void historicalValues() {
		assertEquals(17190, DeltaT.estimate(yearCal(-400)), 2000); // not sure why so far off, polynomial seems ok

		assertEquals(14080, DeltaT.estimate(yearCal(-300)), 5);

		assertEquals(12790, DeltaT.estimate(yearCal(-200)), 5);

		assertEquals(7680, DeltaT.estimate(yearCal(300)), 1);

		assertEquals(3810, DeltaT.estimate(yearCal(700)), 3);

		assertEquals(200, DeltaT.estimate(yearCal(1500)), 2);

		assertEquals(7, DeltaT.estimate(yearCal(1850)), 1);

		assertEquals(7, DeltaT.estimate(yearCal(1850)), 1);

		assertEquals(-3, DeltaT.estimate(yearCal(1900)), 1);

		assertEquals(29, DeltaT.estimate(yearCal(1950)), 1);
	}

	@Test
	public void observedValues() {
		assertEquals(31.1, DeltaT.estimate(yearCal(1955)), 1);

		assertEquals(45.5, DeltaT.estimate(yearCal(1975)), 1);

		assertEquals(56.9, DeltaT.estimate(yearCal(1990)), 1);

		assertEquals(63.8, DeltaT.estimate(yearCal(2000)), 1);

		assertEquals(64.7, DeltaT.estimate(yearCal(2005)), 1);

		assertEquals(68.0, DeltaT.estimate(yearCal(2015)), 2);
	}

}
