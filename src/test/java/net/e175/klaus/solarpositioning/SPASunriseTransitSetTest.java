package net.e175.klaus.solarpositioning;

import static net.e175.klaus.solarpositioning.SunriseTransitSet.Type.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.stream.Stream;
import org.assertj.core.data.TemporalUnitOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

class SPASunriseTransitSetTest {

  private static final TemporalUnitOffset WITHIN_A_MINUTE = within(1, ChronoUnit.MINUTES);

  void compare(
      SunriseTransitSet result,
      SunriseTransitSet.Type refType,
      String refSunrise,
      String refTransit,
      String refSunset,
      TemporalUnitOffset tolerance) {
    if (refType != null) {
      assertThat(result.getType()).isEqualTo(refType);
    }

    if (refType == NORMAL) {
      assertThat(result.getSunrise()).isCloseTo(refSunrise, tolerance);
      assertThat(result.getSunset()).isCloseTo(refSunset, tolerance);
    } else {
      assertThat(result.getSunrise()).isNull();
      assertThat(result.getSunset()).isNull();
    }

    if (refTransit != null) {
      assertThat(result.getTransit()).isCloseTo(refTransit, tolerance);
    }
  }

  @Test
  void testSpaExampleSunriseTransitSet() {
    ZonedDateTime time = ZonedDateTime.of(2003, 10, 17, 12, 30, 30, 0, ZoneOffset.ofHours(-7));

    SunriseTransitSet res = SPA.calculateSunriseTransitSet(time, 39.742476, -105.1786, 67);

    compare(
        res,
        NORMAL,
        "2003-10-17T06:12:43-07:00",
        "2003-10-17T11:46:04-07:00",
        "2003-10-17T17:18:51-07:00",
        WITHIN_A_MINUTE);
  }

  @Test
  void testAllDay() {
    ZonedDateTime time = ZonedDateTime.of(2015, 6, 17, 12, 30, 30, 0, ZoneOffset.ofHours(2));

    // location is Honningsvåg, Norway (near North Cape)
    SunriseTransitSet res = SPA.calculateSunriseTransitSet(time, 70.978056, 25.974722, 0);

    compare(res, ALL_DAY, null, "2015-06-17T12:16:55+02:00", null, WITHIN_A_MINUTE);
  }

  @Test
  void testAllNight() {
    ZonedDateTime time = ZonedDateTime.of(2015, 1, 17, 12, 30, 30, 0, ZoneOffset.ofHours(2));

    // location is Honningsvåg, Norway (near North Cape)
    SunriseTransitSet res = SPA.calculateSunriseTransitSet(time, 70.978056, 25.974722, 0);

    compare(res, ALL_NIGHT, null, null, null, WITHIN_A_MINUTE);
  }

  @Test
  void testNZSunriseTransitSet() {
    ZonedDateTime time = ZonedDateTime.of(2015, 6, 17, 12, 30, 30, 0, ZoneOffset.ofHours(12));

    SunriseTransitSet res = SPA.calculateSunriseTransitSet(time, -36.8406, 174.74, 0);

    // NOAA: 7:32, 12:21:41, 17:11
    compare(
        res,
        NORMAL,
        "2015-06-17T07:32:26+12:00",
        "2015-06-17T12:21:46+12:00",
        "2015-06-17T17:11:03+12:00",
        WITHIN_A_MINUTE);
  }

  @Test
  void testDSToffDayBerlin() {
    ZonedDateTime time = ZonedDateTime.of(2015, 10, 25, 12, 0, 0, 0, ZoneId.of("Europe/Berlin"));

    SunriseTransitSet res = SPA.calculateSunriseTransitSet(time, 52.33, 13.3, 68);

    // NOAA: 6:49, 11:50:53, 16:52
    compare(
        res,
        NORMAL,
        "2015-10-25T06:49:02+01:00",
        "2015-10-25T11:50:55+01:00",
        "2015-10-25T16:51:59+01:00",
        WITHIN_A_MINUTE);
  }

  @Test
  void testDSTonDayBerlin() {
    ZonedDateTime time = ZonedDateTime.of(2016, 3, 27, 12, 0, 0, 0, ZoneId.of("Europe/Berlin"));

    SunriseTransitSet res = SPA.calculateSunriseTransitSet(time, 52.33, 13.3, 68);

    // NOAA: 06:52, 13:12:01, 19:33
    compare(
        res,
        NORMAL,
        "2016-03-27T06:52:19+02:00",
        "2016-03-27T13:12:02+02:00",
        "2016-03-27T19:32:49+02:00",
        WITHIN_A_MINUTE);
  }

  @Test
  void testDSToffDayAuckland() {
    ZonedDateTime time = ZonedDateTime.of(2016, 4, 3, 12, 0, 0, 0, ZoneId.of("Pacific/Auckland"));

    SunriseTransitSet res = SPA.calculateSunriseTransitSet(time, -36.84, 174.74, 68);

    // NOAA: 06:36, same, 18:12
    compare(
        res,
        NORMAL,
        "2016-04-03T06:36:09+12:00",
        "2016-04-03T12:24:19+12:00",
        "2016-04-03T18:11:55+12:00",
        WITHIN_A_MINUTE);
  }

  @Test
  void testDSTonDayAuckland() {
    ZonedDateTime time = ZonedDateTime.of(2015, 9, 27, 12, 0, 0, 0, ZoneId.of("Pacific/Auckland"));

    SunriseTransitSet res = SPA.calculateSunriseTransitSet(time, -36.84, 174.74, 68);

    // NOAA: 07:04, 13:12:19, 19:21
    compare(
        res,
        NORMAL,
        "2015-09-27T07:04:14+13:00",
        "2015-09-27T13:12:17+13:00",
        "2015-09-27T19:20:56+13:00",
        WITHIN_A_MINUTE);
  }

  @Test
  void testSillyLatLon() {
    ZonedDateTime time = ZonedDateTime.of(2003, 10, 17, 12, 30, 30, 0, ZoneOffset.ofHours(-7));

    assertThrows(
        IllegalArgumentException.class,
        () -> SPA.calculateSunriseTransitSet(time, 139.742476, -105.1786, 67));

    assertThrows(
        IllegalArgumentException.class,
        () -> SPA.calculateSunriseTransitSet(time, 39.742476, -205.1786, 67));
  }

  void compare(
      SunriseTransitSet res,
      ZonedDateTime baseDateTime,
      SunriseTransitSet.Type type,
      LocalTime sunrise,
      LocalTime transit,
      LocalTime sunset,
      TemporalUnitOffset tolerance) {
    compare(
        res,
        type,
        makeZonedDateTimeString(baseDateTime, sunrise),
        makeZonedDateTimeString(baseDateTime, transit),
        makeZonedDateTimeString(baseDateTime, sunset),
        tolerance);
  }

  String makeZonedDateTimeString(ZonedDateTime baseDateTime, LocalTime localTime) {
    return localTime != null
        ? ZonedDateTime.of(baseDateTime.toLocalDate(), localTime, baseDateTime.getOffset())
            .format(DateTimeFormatter.ISO_DATE_TIME)
        : null;
  }

  @ParameterizedTest
  @CsvFileSource(resources = "/sunrise/spa_reference_testdata.csv")
  void testBulkSpaReferenceValues(
      ZonedDateTime dateTime,
      double lat,
      double lon,
      LocalTime sunrise,
      LocalTime transit,
      LocalTime sunset) {
    SunriseTransitSet res = SPA.calculateSunriseTransitSet(dateTime, lat, lon, 0);

    compare(
        res,
        dateTime,
        sunrise != null ? NORMAL : null,
        sunrise,
        transit,
        sunset,
        within(1, ChronoUnit.SECONDS));
  }

  @ParameterizedTest
  @CsvFileSource(resources = "/sunrise/usno_reference_testdata.csv")
  void testBulkUSNOReferenceValues(
      ZonedDateTime dateTime,
      double lat,
      double lon,
      SunriseTransitSet.Type type,
      LocalTime sunrise,
      LocalTime sunset) {
    SunriseTransitSet res = SPA.calculateSunriseTransitSet(dateTime, lat, lon, 0);

    if (res.getType() == NORMAL) {
      AzimuthZenithAngle pos = SPA.calculateSolarPosition(res.getSunrise(), lat, lon, 0, 0);
      assertEquals(90.83337, pos.getZenithAngle(), 0.01);

      pos = SPA.calculateSolarPosition(res.getSunset(), lat, lon, 0, 0);
      assertEquals(90.83337, pos.getZenithAngle(), 0.01);
    }

    compare(res, dateTime, type, sunrise, null, sunset, WITHIN_A_MINUTE);
  }

  @ParameterizedTest
  @CsvFileSource(resources = "/sunrise/usno_reference_testdata_extreme.csv")
  void testBulkUSNOExtremeReferenceValues(
      ZonedDateTime dateTime,
      double lat,
      double lon,
      SunriseTransitSet.Type type,
      LocalTime sunrise,
      LocalTime sunset) {
    SunriseTransitSet res = SPA.calculateSunriseTransitSet(dateTime, lat, lon, 0);

    if (res.getType() == NORMAL) {
      AzimuthZenithAngle pos = SPA.calculateSolarPosition(res.getSunrise(), lat, lon, 0, 0);
      assertEquals(90.83337, pos.getZenithAngle(), 0.1);

      pos = SPA.calculateSolarPosition(res.getSunset(), lat, lon, 0, 0);
      assertEquals(90.83337, pos.getZenithAngle(), 0.1);
    }

    compare(res, dateTime, type, sunrise, null, sunset, within(2, ChronoUnit.MINUTES));
  }

  @ParameterizedTest
  @CsvFileSource(resources = "/cities.csv", useHeadersInDisplayName = true)
  void testCivilTwilightAgainstPosition(String name, double lat, double lon) {
    Stream.iterate(
            ZonedDateTime.of(
                LocalDate.of(2023, Month.JANUARY, 1), LocalTime.of(12, 0), ZoneOffset.UTC),
            i -> i.plusDays(1))
        .limit(366)
        .forEach(
            dateTime -> {
              SunriseTransitSet res =
                  SPA.calculateSunriseTransitSet(dateTime, lat, lon, 0, SPA.Horizon.CIVIL_TWILIGHT);

              if (res.getType() == NORMAL) {
                AzimuthZenithAngle pos =
                    SPA.calculateSolarPosition(res.getSunrise(), lat, lon, 0, 0);
                assertEquals(96, pos.getZenithAngle(), 0.2, dateTime.toString() + " " + res);
              }
            });
  }

  @ParameterizedTest
  @CsvFileSource(resources = "/sunrise/usno_reference_testdata_civil.csv")
  void testBulkUSNOReferenceValuesCivil(
      ZonedDateTime dateTime,
      double lat,
      double lon,
      SunriseTransitSet.Type type,
      LocalTime sunrise,
      LocalTime sunset) {
    SunriseTransitSet res =
        SPA.calculateSunriseTransitSet(dateTime, lat, lon, 0, SPA.Horizon.CIVIL_TWILIGHT);

    if (res.getType() == NORMAL) {
      AzimuthZenithAngle pos = SPA.calculateSolarPosition(res.getSunrise(), lat, lon, 0, 0);
      assertEquals(96, pos.getZenithAngle(), 0.02);

      pos = SPA.calculateSolarPosition(res.getSunset(), lat, lon, 0, 0);
      assertEquals(96, pos.getZenithAngle(), 0.02);
    }

    compare(res, dateTime, type, sunrise, null, sunset, within(2, ChronoUnit.MINUTES));
  }

  @Test
  void testAllHorizons() {
    // Lerwick, Scotland 60.1547 -1.1494
    // 2023-03-01 according to HMNAO: "Daily Rise, Set and Twilight Times for the British Isles"
    //
    // sunrise sunset civil_start civil_end nautical_start nautical_end astro_start astro_end  UTC
    // 07:04   17:31  06:22       18:13     05:34          19:01        04:45       19:51

    final ZonedDateTime dateTime = ZonedDateTime.parse("2023-03-01T12:00:00Z");
    final double lat = 60.1547;
    final double lon = -1.1494;
    final double deltaT = 69.2;

    SunriseTransitSet res = SPA.calculateSunriseTransitSet(dateTime, lat, lon, deltaT);
    compare(res, NORMAL, "2023-03-01T07:04:00Z", null, "2023-03-01T17:31:00Z", WITHIN_A_MINUTE);

    res = SPA.calculateSunriseTransitSet(dateTime, lat, lon, deltaT, SPA.Horizon.CIVIL_TWILIGHT);
    compare(res, NORMAL, "2023-03-01T06:22:00Z", null, "2023-03-01T18:13:00Z", WITHIN_A_MINUTE);

    res = SPA.calculateSunriseTransitSet(dateTime, lat, lon, deltaT, SPA.Horizon.NAUTICAL_TWILIGHT);
    compare(res, NORMAL, "2023-03-01T05:34:00Z", null, "2023-03-01T19:01:00Z", WITHIN_A_MINUTE);

    res =
        SPA.calculateSunriseTransitSet(
            dateTime, lat, lon, deltaT, SPA.Horizon.ASTRONOMICAL_TWILIGHT);
    compare(res, NORMAL, "2023-03-01T04:45:00Z", null, "2023-03-01T19:51:00Z", WITHIN_A_MINUTE);
  }

  @Test
  void testAllHorizonsWithSingleCall() {
    // Lerwick, Scotland 60.1547 -1.1494
    // 2023-03-01 according to HMNAO: "Daily Rise, Set and Twilight Times for the British Isles"
    //
    // sunrise sunset civil_start civil_end nautical_start nautical_end astro_start astro_end  UTC
    // 07:04   17:31  06:22       18:13     05:34          19:01        04:45       19:51

    final ZonedDateTime dateTime = ZonedDateTime.parse("2023-03-01T12:00:00Z");
    final double lat = 60.1547;
    final double lon = -1.1494;
    final double deltaT = 69.2;

    Map<SPA.Horizon, SunriseTransitSet> results =
        SPA.calculateSunriseTransitSet(dateTime, lat, lon, deltaT, SPA.Horizon.values());

    compare(
        results.get(SPA.Horizon.SUNRISE_SUNSET),
        NORMAL,
        "2023-03-01T07:04:00Z",
        null,
        "2023-03-01T17:31:00Z",
        WITHIN_A_MINUTE);

    compare(
        results.get(SPA.Horizon.CIVIL_TWILIGHT),
        NORMAL,
        "2023-03-01T06:22:00Z",
        null,
        "2023-03-01T18:13:00Z",
        WITHIN_A_MINUTE);

    compare(
        results.get(SPA.Horizon.NAUTICAL_TWILIGHT),
        NORMAL,
        "2023-03-01T05:34:00Z",
        null,
        "2023-03-01T19:01:00Z",
        WITHIN_A_MINUTE);

    compare(
        results.get(SPA.Horizon.ASTRONOMICAL_TWILIGHT),
        NORMAL,
        "2023-03-01T04:45:00Z",
        null,
        "2023-03-01T19:51:00Z",
        WITHIN_A_MINUTE);
  }
}
