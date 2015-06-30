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

        assertEquals(toDegrees(1.13381), result.getZenithAngle(), TOLERANCE / 10);
        assertEquals(toDegrees(-0.0591845 + PI) % 360.0, result.getAzimuth(), TOLERANCE / 10);
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

    @Test
    public void testNearEquator1() {
        GregorianCalendar time = new GregorianCalendar(new SimpleTimeZone(-4 * 60 * 60 * 1000, "AMT"));
        time.set(2015, Calendar.JUNE, 12, 9, 34, 11);

        AzimuthZenithAngle result = Grena3.calculateSolarPosition(time, -3.107, -60.025, 69, 1000, 20);

        assertEquals(51.608, result.getAzimuth(), TOLERANCE);
        assertEquals(44.1425, result.getZenithAngle(), TOLERANCE);
    }

    @Test
    public void testSouthernSolstice() {
        GregorianCalendar time = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        time.set(2012, Calendar.DECEMBER, 22, 12, 0, 0);

        AzimuthZenithAngle result = Grena3.calculateSolarPosition(time, -41, 0, 0, 1000, 20);

        assertEquals(359.08592, result.getAzimuth(), TOLERANCE * 1.5); // seems to be an algorithm problem
        assertEquals(17.5658, result.getZenithAngle(), TOLERANCE);

        result = Grena3.calculateSolarPosition(time, -3, 0, 0, 1000, 20);

        assertEquals(180.790356, result.getAzimuth(), TOLERANCE * 1.5); // seems to be an algorithm problem
        assertEquals(20.4285, result.getZenithAngle(), TOLERANCE);
    }

    @Test
    public void testSillyRefractionParameters() {
        GregorianCalendar time = new GregorianCalendar(new SimpleTimeZone(-7 * 60 * 60 * 1000, "LST"));
        time.set(2003, Calendar.OCTOBER, 17, 12, 30, 30); // 17 October 2003, 12:30:30 LST-07:00

        AzimuthZenithAngle result = Grena3.calculateSolarPosition(time, 39.742476, -105.1786, 67, -2, 1000);
        assertEquals(194.34024, result.getAzimuth(), TOLERANCE);
        assertEquals(50.1279, result.getZenithAngle(), TOLERANCE);

        result = Grena3.calculateSolarPosition(time, 39.742476, -105.1786, 67);
        assertEquals(194.34024, result.getAzimuth(), TOLERANCE);
        assertEquals(50.1279, result.getZenithAngle(), TOLERANCE);
    }

}
