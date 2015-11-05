package net.e175.klaus.solarpositioning;

import org.junit.Test;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class SPATest {

    private static final double TOLERANCE = 0.0001;
    private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";

    @Test
    public void testSpaExample() {
        GregorianCalendar time = new GregorianCalendar(new SimpleTimeZone(-7 * 60 * 60 * 1000, "LST"));
        time.set(2003, Calendar.OCTOBER, 17, 12, 30, 30); // 17 October 2003, 12:30:30 LST-07:00

        AzimuthZenithAngle result = SPA.calculateSolarPosition(time, 39.742476, -105.1786, 1830.14, 67, 820, 11);

        assertEquals(194.340241, result.getAzimuth(), TOLERANCE / 10);
        assertEquals(50.111622, result.getZenithAngle(), TOLERANCE / 10);
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

        DateFormat df = getDateFormat(time);

        assertEquals("2003-10-17T06:12:43-0700", df.format(res[0].getTime()));
        assertEquals("2003-10-17T11:46:04-0700", df.format(res[1].getTime()));
        assertEquals("2003-10-17T17:20:19-0700", df.format(res[2].getTime()));
    }

    private DateFormat getDateFormat(GregorianCalendar time) {
        DateFormat df = new SimpleDateFormat(DATE_FORMAT);
        df.setTimeZone(time.getTimeZone());
        return df;
    }

    @Test
    public void testOtherSpaExampleSunriseTransitSet() {
        GregorianCalendar time = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        time.set(2004, Calendar.DECEMBER, 4, 12, 30, 30);

        GregorianCalendar[] res = SPA.calculateSunriseTransitSet(time, -35.0, 0, 0);

        DateFormat df = getDateFormat(time);

        assertEquals("2004-12-04T04:38:57+0000", df.format(res[0].getTime()));
        assertEquals("2004-12-04T19:02:01+0000", df.format(res[2].getTime())); // SPA paper has 19:02:02.5
    }

    @Test
    public void testNoSunset() {
        GregorianCalendar time = new GregorianCalendar(new SimpleTimeZone(2 * 60 * 60 * 1000, "CEST"));
        time.set(2015, Calendar.JUNE, 17, 12, 30, 30);

        GregorianCalendar[] res = SPA.calculateSunriseTransitSet(time, 70.978056, 25.974722, 68);

        DateFormat df = getDateFormat(time);

        assertNull(res[0]);
        assertEquals("2015-06-17T12:16:55+0200", df.format(res[1].getTime())); // NOAA calc says 12:16:50
        assertNull(res[2]);
    }

    @Test
    public void testNZSunriseTransitSet() {
        GregorianCalendar time = new GregorianCalendar(new SimpleTimeZone(12 * 60 * 60 * 1000, "NZST"));
        time.set(2015, Calendar.JUNE, 17, 12, 30, 30);

        GregorianCalendar[] res = SPA.calculateSunriseTransitSet(time, -36.8406, 174.74, 0);

        DateFormat df = getDateFormat(time);

        assertEquals("2015-06-17T07:32:45+1200", df.format(res[0].getTime()));
        assertEquals("2015-06-17T17:11:04+1200", df.format(res[2].getTime()));
    }

    @Test
    public void testDSToffDay() {
        GregorianCalendar time = new GregorianCalendar(TimeZone.getTimeZone("Europe/Berlin"));
        time.set(2015, Calendar.OCTOBER, 25, 12, 0, 0);

        GregorianCalendar[] res = SPA.calculateSunriseTransitSet(time, 52.33, 13.3, 68);

        DateFormat df = getDateFormat(time);

        assertEquals("2015-10-25T06:49:00+0100", df.format(res[0].getTime())); // NOAA: same (no seconds given)
        assertEquals("2015-10-25T11:50:55+0100", df.format(res[1].getTime())); // NOAA: 11:50:53
        assertEquals("2015-10-25T16:52:02+0100", df.format(res[2].getTime())); // NOAA: 16:52 (no seconds given)
    }

    @Test
    public void testDSTonDay() {
        GregorianCalendar time = new GregorianCalendar(TimeZone.getTimeZone("Europe/Berlin"));
        time.set(2016, Calendar.MARCH, 27, 12, 0, 0);

        GregorianCalendar[] res = SPA.calculateSunriseTransitSet(time, 52.33, 13.3, 68);

        DateFormat df = getDateFormat(time);

        assertEquals("2016-03-27T06:52:19+0200", df.format(res[0].getTime())); // NOAA: 06:52 (no seconds given)
        assertEquals("2016-03-27T13:12:02+0200", df.format(res[1].getTime())); // NOAA: 13:12:01
        assertEquals("2016-03-27T19:32:49+0200", df.format(res[2].getTime())); // NOAA: 19:33 (no seconds given)
    }

}
