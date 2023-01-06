package net.e175.klaus.solarpositioning;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import java.time.*;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;

public class SPASunriseTransitSetTest {

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssxxx");

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
    public void testAllDay() {
        ZonedDateTime time = ZonedDateTime.of(2015, 6, 17, 12, 30, 30, 0, ZoneOffset.ofHours(2));

        // location is Honningsvåg, Norway (near North Cape)
        SunriseTransitSet res = SPA.calculateSunriseTransitSet(time, 70.978056, 25.974722, 0);

        assertEquals(SunriseTransitSet.Type.ALL_DAY, res.getType());
        assertNull(res.getSunrise());
        assertEquals("2015-06-17T12:16:55+02:00", DF.format(res.getTransit())); // NOAA calc says 12:16:50
        assertNull(res.getSunset());
    }

    @Test
    public void testAllNight() {
        ZonedDateTime time = ZonedDateTime.of(2015, 1, 17, 12, 30, 30, 0, ZoneOffset.ofHours(2));

        // location is Honningsvåg, Norway (near North Cape)
        SunriseTransitSet res = SPA.calculateSunriseTransitSet(time, 70.978056, 25.974722, 0);

        assertEquals(SunriseTransitSet.Type.ALL_NIGHT, res.getType());
        assertNull(res.getSunrise());
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


    @ParameterizedTest
    @CsvFileSource(resources = "/sunrise/spa_reference_testdata.csv")
    public void testBulkSpaReferenceValues(ZonedDateTime dateTime, double lat, double lon, LocalTime sunrise, LocalTime transit, LocalTime sunset) {
        SunriseTransitSet res = SPA.calculateSunriseTransitSet(dateTime, lat, lon, 0);

        Duration tolerance = Duration.ofSeconds(1);
        compare(sunrise, res.getSunrise(), tolerance);
        compare(sunset, res.getSunset(), tolerance);

        if (sunrise != null) {
            compare(transit, res.getTransit(), tolerance);
            assertEquals(SunriseTransitSet.Type.NORMAL, res.getType());
        } else {
            assertNotEquals(SunriseTransitSet.Type.NORMAL, res.getType());
        }
    }

    @ParameterizedTest
    @CsvFileSource(resources = "/sunrise/usno_reference_testdata.csv")
    public void testBulkUSNOReferenceValues(ZonedDateTime dateTime, double lat, double lon, SunriseTransitSet.Type type, LocalTime sunrise, LocalTime sunset) {
        SunriseTransitSet res = SPA.calculateSunriseTransitSet(dateTime, lat, lon, 0);

        Duration tolerance = Duration.ofMinutes(1);
        compare(sunrise, res.getSunrise(), tolerance);
        compare(sunset, res.getSunset(), tolerance);
        assertEquals(type, res.getType());
    }

    @ParameterizedTest
    @CsvFileSource(resources = "/sunrise/usno_reference_testdata_extreme.csv")
    public void testBulkUSNOExtremeReferenceValues(ZonedDateTime dateTime, double lat, double lon, SunriseTransitSet.Type type, LocalTime sunrise, LocalTime sunset) {
        SunriseTransitSet res = SPA.calculateSunriseTransitSet(dateTime, lat, lon, 0);

        Duration tolerance = Duration.ofMinutes(15);
        compare(sunrise, res.getSunrise(), tolerance);
        compare(sunset, res.getSunset(), tolerance);
        assertEquals(type, res.getType());
    }

    private static void compare(LocalTime localRef, ZonedDateTime zonedResult, Duration tolerance) {
        if (zonedResult == null || localRef == null) {
            assertEquals(localRef, zonedResult);
        } else {
            ZonedDateTime zonedRef = ZonedDateTime.of(zonedResult.toLocalDate(), localRef, zonedResult.getZone());

            assertTrue(zonedResult.isAfter(zonedRef.minus(tolerance)) &&
                            zonedResult.isBefore(zonedRef.plus(tolerance)),
                    "expected " + localRef + ", actual " + zonedResult.toLocalTime());
        }
    }

}
