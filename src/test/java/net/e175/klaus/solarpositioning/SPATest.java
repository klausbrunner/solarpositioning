package net.e175.klaus.solarpositioning;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SPATest {

    private static final double TOLERANCE = 0.0001;

    @Test
    void testSpaExample() {
        ZonedDateTime time = ZonedDateTime.of(2003, 10, 17, 12, 30, 30, 0, ZoneOffset.ofHours(-7));

        AzimuthZenithAngle result = SPA.calculateSolarPosition(time, 39.742476, -105.1786, 1830.14, 67, 820, 11);

        assertEquals(194.340241, result.getAzimuth(), TOLERANCE / 10);
        assertEquals(50.111622, result.getZenithAngle(), TOLERANCE / 10);
    }

    @Test
    void testSouthernSolstice() {
        ZonedDateTime time = ZonedDateTime.of(2012, 12, 22, 12, 0, 0, 0, ZoneOffset.UTC);

        AzimuthZenithAngle result = SPA.calculateSolarPosition(time, -41, 0, 100, 0, 1000, 20);

        assertEquals(359.08592, result.getAzimuth(), TOLERANCE);
        assertEquals(17.5658, result.getZenithAngle(), TOLERANCE);

        result = SPA.calculateSolarPosition(time, -3, 0, 100, 0, 1000, 20);

        assertEquals(180.790356, result.getAzimuth(), TOLERANCE);
        assertEquals(20.4285, result.getZenithAngle(), TOLERANCE);
    }

    @Test
    void testSillyRefractionParameters() {
        ZonedDateTime time = ZonedDateTime.of(2003, 10, 17, 12, 30, 30, 0, ZoneOffset.ofHours(-7));

        AzimuthZenithAngle result = SPA.calculateSolarPosition(time, 39.742476, -105.1786, 1830.14, 67, -2, 1000);
        assertEquals(194.34024, result.getAzimuth(), TOLERANCE);
        assertEquals(50.1279, result.getZenithAngle(), TOLERANCE);

        AzimuthZenithAngle result2 = SPA.calculateSolarPosition(time, 39.742476, -105.1786, 1830.14, 67);
        assertEquals(result, result2);
    }

    @Test
    void testSillyLatLon() {
        ZonedDateTime time = ZonedDateTime.of(2003, 10, 17, 12, 30, 30, 0, ZoneOffset.ofHours(-7));

        assertThrows(IllegalArgumentException.class, () -> SPA.calculateSolarPosition(time, 139.742476, -105.1786, 1830.14, 67, 820, 11));

        assertThrows(IllegalArgumentException.class, () -> SPA.calculateSolarPosition(time, 39.742476, -205.1786, 1830.14, 67, 820, 11));
    }

    @ParameterizedTest
    @CsvFileSource(resources = "/azimuth_zenith/spa_reference_testdata.csv")
    void testBulkSpaReferenceValues(ZonedDateTime dateTime, double lat, double lon, double refAzimuth, double refZenith) {
        AzimuthZenithAngle res = SPA.calculateSolarPosition(dateTime, lat, lon, 0, 0, 1000, 10);

        assertEquals(refAzimuth, res.getAzimuth(), TOLERANCE / 100);
        assertEquals(refZenith, res.getZenithAngle(), TOLERANCE / 100);
    }

}
