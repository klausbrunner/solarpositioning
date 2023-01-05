package net.e175.klaus.solarpositioning;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

public class SPATest {

    private static final double TOLERANCE = 0.0001;

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssxxx");

    @Test
    public void testSpaExample() {
        ZonedDateTime time = ZonedDateTime.of(2003, 10, 17, 12, 30, 30, 0, ZoneOffset.ofHours(-7));

        AzimuthZenithAngle result = SPA.calculateSolarPosition(time, 39.742476, -105.1786, 1830.14, 67, 820, 11);

        assertEquals(194.340241, result.getAzimuth(), TOLERANCE / 10);
        assertEquals(50.111622, result.getZenithAngle(), TOLERANCE / 10);
    }

    @Test
    public void testNearEquator1() {
        ZonedDateTime time = ZonedDateTime.of(2015, 6, 12, 9, 34, 11, 0, ZoneOffset.ofHours(-4));

        AzimuthZenithAngle result = SPA.calculateSolarPosition(time, -3.107, -60.025, 100, 69, 1000, 20);

        assertEquals(51.608, result.getAzimuth(), TOLERANCE);
        assertEquals(44.1425, result.getZenithAngle(), TOLERANCE);
    }

    @Test
    public void testSouthernSolstice() {
        ZonedDateTime time = ZonedDateTime.of(2012, 12, 22, 12, 0, 0, 0, ZoneOffset.UTC);

        AzimuthZenithAngle result = SPA.calculateSolarPosition(time, -41, 0, 100, 0, 1000, 20);

        assertEquals(359.08592, result.getAzimuth(), TOLERANCE);
        assertEquals(17.5658, result.getZenithAngle(), TOLERANCE);

        result = SPA.calculateSolarPosition(time, -3, 0, 100, 0, 1000, 20);

        assertEquals(180.790356, result.getAzimuth(), TOLERANCE);
        assertEquals(20.4285, result.getZenithAngle(), TOLERANCE);
    }

    @Test
    public void testSillyRefractionParameters() {
        ZonedDateTime time = ZonedDateTime.of(2003, 10, 17, 12, 30, 30, 0, ZoneOffset.ofHours(-7));

        AzimuthZenithAngle result = SPA.calculateSolarPosition(time, 39.742476, -105.1786, 1830.14, 67, -2, 1000);
        assertEquals(194.34024, result.getAzimuth(), TOLERANCE);
        assertEquals(50.1279, result.getZenithAngle(), TOLERANCE);

        result = SPA.calculateSolarPosition(time, 39.742476, -105.1786, 1830.14, 67);
        assertEquals(194.34024, result.getAzimuth(), TOLERANCE);
        assertEquals(50.1279, result.getZenithAngle(), TOLERANCE);
    }

    @Test
    public void testSpaExampleSunriseTransitSet() {
        ZonedDateTime time = ZonedDateTime.of(2003, 10, 17, 12, 30, 30, 0, ZoneOffset.ofHours(-7));

        SunriseTransitSet res = SPA.calculateSunriseTransitSet(time, 39.742476, -105.1786, 67);

        assertEquals(SunriseTransitSet.Type.NORMAL, res.getType());
        assertEquals("2003-10-17T06:12:43-07:00", DF.format(res.getSunrise()));
        assertEquals("2003-10-17T11:46:04-07:00", DF.format(res.getTransit()));
        assertEquals("2003-10-17T17:18:51-07:00", DF.format(res.getSunset())); // SPA paper: 17:20:19, NOAA calc: 17:19
    }

    @Test
    public void testOtherSpaExampleSunriseTransitSet() {
        ZonedDateTime time = ZonedDateTime.of(2004, 12, 4, 12, 30, 30, 0, ZoneOffset.UTC);

        SunriseTransitSet res = SPA.calculateSunriseTransitSet(time, -35.0, 0, 0);

        assertEquals(SunriseTransitSet.Type.NORMAL, res.getType());
        assertEquals("2004-12-04T04:38:56+00:00", DF.format(res.getSunrise()));
        assertEquals("2004-12-04T19:02:02+00:00", DF.format(res.getSunset())); // SPA paper has 19:02:02.5
    }

    @Test
    public void testNoSunset() {
        ZonedDateTime time = ZonedDateTime.of(2015, 6, 17, 12, 30, 30, 0, ZoneOffset.ofHours(2));

        // location is Honningsvåg, Norway (near North Cape)
        SunriseTransitSet res = SPA.calculateSunriseTransitSet(time, 70.978056, 25.974722, 0);

        assertEquals(SunriseTransitSet.Type.ALL_DAY, res.getType());
        assertNull(res.getSunrise());
        assertEquals("2015-06-17T12:16:55+02:00", DF.format(res.getTransit())); // NOAA calc says 12:16:50
        assertNull(res.getSunset());
    }

    @Test
    public void testNZSunriseTransitSet() {
        ZonedDateTime time = ZonedDateTime.of(2015, 6, 17, 12, 30, 30, 0, ZoneOffset.ofHours(12));

        SunriseTransitSet res = SPA.calculateSunriseTransitSet(time, -36.8406, 174.74, 0);

        assertEquals(SunriseTransitSet.Type.NORMAL, res.getType());
        assertEquals("2015-06-17T07:32:26+12:00", DF.format(res.getSunrise())); // NOAA: 7:32 (no seconds given)
        assertEquals("2015-06-17T12:21:46+12:00", DF.format(res.getTransit())); // NOAA: 12:21:41
        assertEquals("2015-06-17T17:11:03+12:00", DF.format(res.getSunset())); // NOAA: 17:11 (no seconds given)
    }

    @Test
    public void testDSToffDayBerlin() {
        ZonedDateTime time = ZonedDateTime.of(2015, 10, 25, 12, 0, 0, 0,
                ZoneId.of("Europe/Berlin"));

        SunriseTransitSet res = SPA.calculateSunriseTransitSet(time, 52.33, 13.3, 68);

        assertEquals(SunriseTransitSet.Type.NORMAL, res.getType());
        assertEquals("2015-10-25T06:49:02+01:00", DF.format(res.getSunrise())); // NOAA: same (no seconds given)
        assertEquals("2015-10-25T11:50:55+01:00", DF.format(res.getTransit())); // NOAA: 11:50:53
        assertEquals("2015-10-25T16:51:59+01:00", DF.format(res.getSunset())); // NOAA: 16:52 (no seconds given)
    }

    @Test
    public void testDSTonDayBerlin() {
        ZonedDateTime time = ZonedDateTime.of(2016, 3, 27, 12, 0, 0, 0,
                ZoneId.of("Europe/Berlin"));

        SunriseTransitSet res = SPA.calculateSunriseTransitSet(time, 52.33, 13.3, 68);

        assertEquals(SunriseTransitSet.Type.NORMAL, res.getType());
        assertEquals("2016-03-27T06:52:19+02:00", DF.format(res.getSunrise())); // NOAA: 06:52 (no seconds given)
        assertEquals("2016-03-27T13:12:02+02:00", DF.format(res.getTransit())); // NOAA: 13:12:01
        assertEquals("2016-03-27T19:32:49+02:00", DF.format(res.getSunset())); // NOAA: 19:33 (no seconds given)
    }

    @Test
    public void testDSToffDayAuckland() {
        ZonedDateTime time = ZonedDateTime.of(2016, 4, 3, 12, 0, 0, 0,
                ZoneId.of("Pacific/Auckland"));

        SunriseTransitSet res = SPA.calculateSunriseTransitSet(time, -36.84, 174.74, 68);

        assertEquals(SunriseTransitSet.Type.NORMAL, res.getType());
        assertEquals("2016-04-03T06:36:09+12:00", DF.format(res.getSunrise())); // NOAA: 06:36 (no seconds given)
        assertEquals("2016-04-03T12:24:19+12:00", DF.format(res.getTransit())); // NOAA: same
        assertEquals("2016-04-03T18:11:55+12:00", DF.format(res.getSunset())); // NOAA: 18:12 (no seconds given)
    }

    @Test
    public void testDSTonDayAuckland() {
        ZonedDateTime time = ZonedDateTime.of(2015, 9, 27, 12, 0, 0, 0,
                ZoneId.of("Pacific/Auckland"));

        SunriseTransitSet res = SPA.calculateSunriseTransitSet(time, -36.84, 174.74, 68);

        assertEquals(SunriseTransitSet.Type.NORMAL, res.getType());
        assertEquals("2015-09-27T07:04:14+13:00", DF.format(res.getSunrise())); // NOAA: 07:04 (no seconds given)
        assertEquals("2015-09-27T13:12:17+13:00", DF.format(res.getTransit())); // NOAA: 13:12:19
        assertEquals("2015-09-27T19:20:56+13:00", DF.format(res.getSunset())); // NOAA: 19:21 (no seconds given)
    }

    private static SunriseTransitSet lybRun(int year, int month, int day) {
        ZonedDateTime time = ZonedDateTime.of(year, month, day, 0, 0, 0, 0,
                ZoneOffset.UTC);
        return SPA.calculateSunriseTransitSet(time, 78.2222, 15.6316, 0);
    }

    @Test
    public void testLongyearbyen() {
        SunriseTransitSet res;

        res = lybRun(2015, 1, 20);
        assertEquals(SunriseTransitSet.Type.ALL_NIGHT, res.getType());
        assertNull(res.getSunrise());
        assertNull(res.getSunset());

        res = lybRun(2015, 6, 20);
        assertEquals(SunriseTransitSet.Type.ALL_DAY, res.getType());
        assertNull(res.getSunrise());
        assertEquals("2015-06-20T10:58:57+00:00", DF.format(res.getTransit())); // USNO: 10:59
        assertNull(res.getSunset());
    }

    @ParameterizedTest
    @CsvFileSource(resources = "/azimuth_zenith/spa_reference_testdata.csv")
    public void testBulkSpaReferenceValues(ZonedDateTime dateTime, double lat, double lon, double refAzimuth, double refZenith) {
        AzimuthZenithAngle res = SPA.calculateSolarPosition(dateTime, lat, lon, 0, 0, 1000, 10);

        assertEquals(refAzimuth, res.getAzimuth(), TOLERANCE);
        assertEquals(refZenith, res.getZenithAngle(), TOLERANCE);
    }


    @ParameterizedTest
    @CsvFileSource(resources = "/sunrise/spa_reference_testdata.csv")
    public void testBulkSpaReferenceValues(ZonedDateTime dateTime, double lat, double lon, String sunrise, String transit, String sunset) {
        SunriseTransitSet res = SPA.calculateSunriseTransitSet(dateTime, lat, lon, 0);

        if (sunrise.startsWith("-999")) {
            assertNull(res.getSunrise());
            assertNull(res.getSunset());
            assertNotEquals(res.getType(), SunriseTransitSet.Type.NORMAL);
        } else {
            compare(sunrise, res.getSunrise());
            compare(transit, res.getTransit());
            compare(sunset, res.getSunset());
        }
    }

    private static void compare(String localTimeRef, ZonedDateTime result) {
        DateTimeFormatter parser = DateTimeFormatter.ISO_LOCAL_TIME;
        assertEquals(parser.parse(localTimeRef, LocalTime::from),
                result.toLocalTime().truncatedTo(ChronoUnit.SECONDS));
    }

}
