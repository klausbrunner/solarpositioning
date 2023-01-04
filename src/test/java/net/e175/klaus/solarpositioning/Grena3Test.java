package net.e175.klaus.solarpositioning;

import org.junit.Test;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static java.lang.Math.PI;
import static java.lang.Math.toDegrees;
import static org.junit.Assert.assertEquals;

public class Grena3Test {

    private static final double TOLERANCE = 0.01; // advertised max error vis-a-vis SPA

    @Test
    public void cSampleComparison() {
        ZonedDateTime time = ZonedDateTime.of(2012, 1, 1, 12, 0, 0, 0, ZoneOffset.ofHours(1));

        AzimuthZenithAngle result = Grena3.calculateSolarPosition(time,
                toDegrees(0.73117), toDegrees(0.21787), 65, 1000, 20);

        assertEquals(toDegrees(1.13381), result.getZenithAngle(), TOLERANCE / 10);
        assertEquals(toDegrees(-0.0591845 + PI) % 360.0, result.getAzimuth(), TOLERANCE / 10);
    }

    @Test
    public void spaComparison() {
        ZonedDateTime time = ZonedDateTime.of(2015, 6, 28, 17, 45, 12, 0, ZoneOffset.UTC);

        AzimuthZenithAngle result = Grena3.calculateSolarPosition(time,
                52.509663, 13.376481, 68, 1000, 20);

        assertEquals(291.232854, result.getAzimuth(), TOLERANCE);
        assertEquals(76.799924, result.getZenithAngle(), TOLERANCE);
    }

    @Test
    public void testNearEquator1() {
        ZonedDateTime time = ZonedDateTime.of(2015, 6, 12, 9, 34, 11, 0, ZoneOffset.ofHours(-4));

        AzimuthZenithAngle result = Grena3.calculateSolarPosition(time, -3.107, -60.025, 69, 1000, 20);

        assertEquals(51.608, result.getAzimuth(), TOLERANCE);
        assertEquals(44.1425, result.getZenithAngle(), TOLERANCE);
    }

    @Test
    public void testSouthernSolstice() {
        ZonedDateTime time = ZonedDateTime.of(2012, 12, 22, 12, 0, 0, 0, ZoneOffset.UTC);

        AzimuthZenithAngle result = Grena3.calculateSolarPosition(time, -41, 0, 0, 1000, 20);

        assertEquals(359.08592, result.getAzimuth(), TOLERANCE * 1.5); // seems to be an algorithm problem
        assertEquals(17.5658, result.getZenithAngle(), TOLERANCE);

        result = Grena3.calculateSolarPosition(time, -3, 0, 0, 1000, 20);

        assertEquals(180.790356, result.getAzimuth(), TOLERANCE * 1.5); // seems to be an algorithm problem
        assertEquals(20.4285, result.getZenithAngle(), TOLERANCE);
    }

    @Test
    public void testSillyRefractionParameters() {
        ZonedDateTime time = ZonedDateTime.of(2003, 10, 17, 12, 30, 30, 0, ZoneOffset.ofHours(-7));

        AzimuthZenithAngle result = Grena3.calculateSolarPosition(time, 39.742476, -105.1786, 67, -2, 1000);
        assertEquals(194.34024, result.getAzimuth(), TOLERANCE);
        assertEquals(50.1279, result.getZenithAngle(), TOLERANCE);

        result = Grena3.calculateSolarPosition(time, 39.742476, -105.1786, 67);
        assertEquals(194.34024, result.getAzimuth(), TOLERANCE);
        assertEquals(50.1279, result.getZenithAngle(), TOLERANCE);
    }

}
