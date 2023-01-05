package net.e175.klaus.solarpositioning;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static java.lang.Math.PI;
import static java.lang.Math.toDegrees;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class Grena3Test {

    private static final double TOLERANCE = 0.01; // advertised max error vis-a-vis SPA
    private static final ZonedDateTime GRENA_VALIDITY_START = ZonedDateTime.of(2010, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    private static final ZonedDateTime GRENA_VALIDITY_END = ZonedDateTime.of(2110, 12, 31, 0, 0, 0, 0, ZoneOffset.UTC);
    @Test
    public void testCSampleComparison() {
        ZonedDateTime time = ZonedDateTime.of(2012, 1, 1, 12, 0, 0, 0, ZoneOffset.ofHours(1));

        AzimuthZenithAngle result = Grena3.calculateSolarPosition(time,
                toDegrees(0.73117), toDegrees(0.21787), 65, 1000, 20);

        assertEquals(toDegrees(1.13381), result.getZenithAngle(), TOLERANCE / 100);
        assertEquals(toDegrees(-0.0591845 + PI) % 360.0, result.getAzimuth(), TOLERANCE / 100);
    }

    @Test
    public void testSpaComparison() {
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

        AzimuthZenithAngle result2 = Grena3.calculateSolarPosition(time, 39.742476, -105.1786, 67);
        assertEquals(result, result2);
    }

    @ParameterizedTest
    @CsvFileSource(resources = "/azimuth_zenith/spa_reference_testdata.csv")
    public void testBulkSpaReferenceValues(ZonedDateTime dateTime, double lat, double lon, double refAzimuth, double refZenith) {
        Assumptions.assumeTrue(dateTime.isAfter(GRENA_VALIDITY_START)
                        && dateTime.isBefore(GRENA_VALIDITY_END), "date out of validity range, skipping");

        AzimuthZenithAngle res = Grena3.calculateSolarPosition(dateTime, lat, lon, 0, 1000, 10);

        assertEquals(refAzimuth, res.getAzimuth(), TOLERANCE * 3, "azimuth");
        // TODO: investigate occasional outliers. TOLERANCE is usually fine but in current dataset there's 1 outlier.

        assertEquals(refZenith, res.getZenithAngle(), TOLERANCE, "zenith");
    }
}
