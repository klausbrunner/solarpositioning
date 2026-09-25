package net.e175.klaus.solarpositioning.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.*;
import java.util.*;
import java.util.function.BiFunction;
import net.e175.klaus.solarpositioning.DeltaT;
import net.e175.klaus.solarpositioning.Grena3;
import net.e175.klaus.solarpositioning.SPA;
import net.e175.klaus.solarpositioning.SolarEvents;
import net.e175.klaus.solarpositioning.SolarEvents.Horizon;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class SolarEventsTest {
  private static final SolarEvents EVENTS = new SolarEvents();
  private static final double DELTA_T = 69.184;

  @ParameterizedTest
  @CsvFileSource(resources = "sunrise/jpl_events.csv", numLinesToSkip = 1)
  void matchesIndependentJplEvents(
      Instant start,
      Instant end,
      double latitude,
      double longitude,
      double horizon,
      String rises,
      String sets,
      String transits) {
    var result =
        EVENTS.forDate(
            start.atZone(ZoneOffset.UTC).toLocalDate(),
            ZoneOffset.UTC,
            latitude,
            longitude,
            DELTA_T,
            horizon);
    assertThat(result.start().toInstant()).isEqualTo(start);
    assertThat(result.end().toInstant()).isEqualTo(end);
    compareEvents(rises, result.rises(), timeTolerance(rises, latitude, longitude));
    compareEvents(sets, result.sets(), timeTolerance(sets, latitude, longitude));
    compareEvents(transits, result.transits(), 1.0);
  }

  private static double timeTolerance(String reference, double latitude, double longitude) {
    // SPA's position uncertainty is angular, not temporal. A very shallow crossing can
    // differ by seconds even when the two ephemerides agree within 0.0003 degrees.
    double tolerance = 0.002;
    if (reference != null) {
      for (String value : reference.split(";")) {
        Instant time = Instant.parse(value);
        double speed =
            Math.abs(
                    altitude(time.plusSeconds(1), latitude, longitude)
                        - altitude(time.minusSeconds(1), latitude, longitude))
                / 2;
        tolerance = Math.max(tolerance, 0.0003 / speed + 0.002);
      }
    }
    return tolerance;
  }

  private static void compareEvents(
      String reference, List<ZonedDateTime> actual, double tolerance) {
    List<Instant> expected =
        reference == null
            ? List.of()
            : java.util.Arrays.stream(reference.split(";")).map(Instant::parse).toList();
    assertThat(actual).hasSameSizeAs(expected);
    for (int i = 0; i < expected.size(); i++) {
      assertThat(Duration.between(expected.get(i), actual.get(i).toInstant()).abs())
          .as("event %s", expected.get(i))
          .isLessThan(Duration.ofNanos((long) (tolerance * 1e9)));
    }
  }

  @ParameterizedTest
  @CsvFileSource(
      resources = {
        "sunrise/usno_reference_testdata.csv",
        "sunrise/usno_reference_testdata_extreme.csv"
      })
  void matchesUsnoSunriseSunset(
      ZonedDateTime day,
      double latitude,
      double longitude,
      String type,
      LocalTime sunrise,
      LocalTime sunset) {
    compareUsno(day, latitude, longitude, Horizon.SUNRISE_SUNSET, type, sunrise, sunset);
  }

  @ParameterizedTest
  @CsvFileSource(resources = "sunrise/usno_reference_testdata_civil.csv")
  void matchesUsnoCivilTwilight(
      ZonedDateTime day,
      double latitude,
      double longitude,
      String type,
      LocalTime sunrise,
      LocalTime sunset) {
    compareUsno(day, latitude, longitude, Horizon.CIVIL_TWILIGHT, type, sunrise, sunset);
  }

  private static void compareUsno(
      ZonedDateTime day,
      double latitude,
      double longitude,
      Horizon horizon,
      String type,
      LocalTime rise,
      LocalTime set) {
    var result =
        EVENTS.forDate(day.toLocalDate(), ZoneOffset.UTC, latitude, longitude, DELTA_T, horizon);
    compareMinuteReference(day.toLocalDate(), rise, result.rises());
    compareMinuteReference(day.toLocalDate(), set, result.sets());
    assertThat(result.alwaysAbove()).isEqualTo(type.equals("ALL_DAY"));
    assertThat(result.alwaysBelow()).isEqualTo(type.equals("ALL_NIGHT"));
  }

  private static void compareMinuteReference(
      LocalDate day, LocalTime expected, List<ZonedDateTime> actual) {
    if (expected == null) {
      assertThat(actual).isEmpty();
    } else {
      assertThat(actual).hasSize(1);
      Instant reference = day.atTime(expected).toInstant(ZoneOffset.UTC);
      // Published annual tables are minute-resolution references, including polar transitions.
      assertThat(Duration.between(reference, actual.get(0).toInstant()).abs())
          .isLessThan(Duration.ofSeconds(90));
    }
  }

  private static Instant at(Instant origin, double seconds) {
    return origin.plusNanos((long) (seconds * 1e9));
  }

  private static double altitude(Instant time, double latitude, double longitude) {
    return 90
        - SPA.calculateSolarPosition(time.atZone(ZoneOffset.UTC), latitude, longitude, 0, DELTA_T)
            .zenithAngle();
  }

  private static void checkCrossing(
      Instant time, double latitude, double longitude, double horizon, int direction) {
    assertThat(direction * (altitude(time.minusMillis(10), latitude, longitude) - horizon))
        .isNegative();
    assertThat(direction * (altitude(time.plusMillis(10), latitude, longitude) - horizon))
        .isPositive();
  }

  @ParameterizedTest
  @CsvSource({"90, 2024-03-01, 2024-04-01", "-90, 2024-09-01, 2024-10-01"})
  void findsSeasonalSunriseAtEitherPole(double latitude, LocalDate from, LocalDate to) {
    Instant start = from.atStartOfDay(ZoneOffset.UTC).toInstant();
    Instant end = to.atStartOfDay(ZoneOffset.UTC).toInstant();
    Instant rise =
        EVENTS.nextRise(start, end, latitude, 0, DELTA_T, Horizon.SUNRISE_SUNSET).orElseThrow();
    checkCrossing(rise, latitude, 0, Horizon.SUNRISE_SUNSET.elevation(), 1);
    assertThat(EVENTS.nextRise(rise, end, latitude, 0, DELTA_T, Horizon.SUNRISE_SUNSET)).isEmpty();
    assertThat(EVENTS.nextSet(start, end, latitude, 0, DELTA_T, Horizon.SUNRISE_SUNSET)).isEmpty();
  }

  @ParameterizedTest
  @CsvSource({
    "2024-03-31,Europe/Berlin,23,1",
    "2024-10-27,Europe/Berlin,25,1",
    "1892-07-04,Pacific/Apia,48,2",
    "2011-12-30,Pacific/Apia,0,0"
  })
  void usesLocalDatesAcrossClockChanges(LocalDate day, String zone, long hours, int count) {
    ZoneId timezone = ZoneId.of(zone);
    var result = EVENTS.forDate(day, timezone, 0, 0, DELTA_T);
    assertThat(Duration.between(result.start(), result.end())).isEqualTo(Duration.ofHours(hours));
    assertThat(result.rises()).hasSize(count);
    assertThat(result.sets()).hasSize(count);
    assertThat(result.transits()).hasSize(count);
    assertThat(result.alwaysAbove()).isFalse();
    assertThat(result.alwaysBelow()).isFalse();
    for (var events : List.of(result.rises(), result.sets(), result.transits())) {
      assertThat(events).isSorted();
      for (var event : events) {
        assertThat(event.getZone()).isEqualTo(timezone);
        assertThat(event.toLocalDate()).isEqualTo(day);
        assertThat(event).isAfterOrEqualTo(result.start()).isBefore(result.end());
      }
    }
  }

  @Test
  void batchesHorizonsWithIndependentStatesAndImmutableResults() {
    LocalDate date = LocalDate.of(2024, 12, 21);
    var days = EVENTS.forDate(date, ZoneOffset.UTC, 70, 0, DELTA_T, Horizon.values());
    var night = days.get(Horizon.SUNRISE_SUNSET);
    var twilight = days.get(Horizon.CIVIL_TWILIGHT);
    assertThat(days).hasSize(4);
    assertThat(night.alwaysBelow()).isTrue();
    assertThat(twilight.alwaysBelow()).isFalse();
    assertThat(twilight.rises()).hasSize(1);
    assertThat(twilight.sets()).hasSize(1);
    assertThat(twilight.transits()).isEqualTo(night.transits()).hasSize(1);
    var custom = EVENTS.forDate(date, ZoneOffset.UTC, 70, 0, DELTA_T, -6, -6, -4.5);
    assertThat(custom).hasSize(2);
    assertThat(custom.get(-6.0)).isEqualTo(twilight);
    assertThat(custom.get(-4.5))
        .isEqualTo(EVENTS.forDate(date, ZoneOffset.UTC, 70, 0, DELTA_T, -4.5));
    assertThrows(UnsupportedOperationException.class, days::clear);
    assertThrows(UnsupportedOperationException.class, twilight.rises()::clear);
    assertThrows(UnsupportedOperationException.class, twilight.transits()::clear);
    assertThrows(UnsupportedOperationException.class, twilight.sets()::clear);
  }

  @Test
  void extremeHorizonsHaveNoCrossings() {
    var days = EVENTS.forDate(LocalDate.of(2024, 3, 20), ZoneOffset.UTC, 0, 0, DELTA_T, -90, 90);
    assertThat(days.get(-90.0).alwaysAbove()).isTrue();
    assertThat(days.get(90.0).alwaysBelow()).isTrue();
  }

  @Test
  void searchUsesContinuousTimeAcross1582() {
    Instant start = Instant.parse("1582-10-04T00:00:00Z");
    Instant end = Instant.parse("1582-10-17T00:00:00Z");
    for (int day = 4; day <= 16; day++) {
      Instant transit = EVENTS.nextTransit(start, end, 0, DELTA_T).orElseThrow();
      assertThat(transit.atOffset(ZoneOffset.UTC).getDayOfMonth()).isEqualTo(day);
      start = transit;
    }
    assertThat(EVENTS.nextTransit(start, end, 0, DELTA_T)).isEmpty();
  }

  @Test
  void resumingAtReturnedInstantDoesNotRepeatCrossing() {
    Instant start = Instant.parse("2025-05-30T00:00:00Z"), end = start.plusSeconds(86400);
    double deltaT = DeltaT.estimate(LocalDate.of(2025, 5, 30));
    // This crossing used to be returned twice, 0.7 ms apart, because converting the
    // result back to Julian time rounded differently from the internal evaluation.
    Instant rise = EVENTS.nextRise(start, end, 0, -179.9, deltaT, -0.8333).orElseThrow();
    assertThat(EVENTS.nextRise(rise, end, 0, -179.9, deltaT, -0.8333)).isEmpty();
  }

  @ParameterizedTest
  @CsvSource({
    "2024-03-20,48.21,16.37,1,1,1",
    "2020-02-16,78.216667,15.633333,1,1,1",
    "2020-04-16,78.216667,15.633333,2,1,1",
    "2020-08-25,78.216667,15.633333,0,1,1",
    "2024-03-18,90,0,1,0,1",
    "2024-09-20,-90,0,1,0,1",
    "2024-06-21,90,0,0,0,1",
    "2020-06-10,0,179.9,1,1,0"
  })
  void grenaEventsFollowGrenaPositions(
      LocalDate date, double latitude, double longitude, int rises, int sets, int transits) {
    var days =
        SolarEvents.grena3()
            .forDate(date, ZoneOffset.UTC, latitude, longitude, DELTA_T, Horizon.values());
    var standard = days.get(Horizon.SUNRISE_SUNSET);
    assertThat(standard.rises()).hasSize(rises);
    assertThat(standard.sets()).hasSize(sets);
    assertThat(standard.transits()).hasSize(transits);
    for (var time : standard.transits()) {
      // At the equator the eastward component changes sign at upper transit.
      var before =
          Grena3.calculateSolarPosition(time.minusNanos(100_000_000), 0, longitude, DELTA_T);
      var after = Grena3.calculateSolarPosition(time.plusNanos(100_000_000), 0, longitude, DELTA_T);
      assertThat(Math.sin(Math.toRadians(before.azimuth()))).isPositive();
      assertThat(Math.sin(Math.toRadians(after.azimuth()))).isNegative();
    }
    for (var entry : days.entrySet()) {
      double horizon = entry.getKey().elevation();
      var day = entry.getValue();
      for (var time : day.rises()) checkGrenaCrossing(time, latitude, longitude, horizon, 1);
      for (var time : day.sets()) checkGrenaCrossing(time, latitude, longitude, horizon, -1);
    }
  }

  private static void checkGrenaCrossing(
      ZonedDateTime time, double latitude, double longitude, double horizon, int direction) {
    // The existing position API's rounded hours-to-days constant shifts time by up to 69 ms.
    // Event searches use continuous Julian time; allow 100 ms when comparing the two paths.
    double before =
        90
            - Grena3.calculateSolarPosition(
                    time.minusNanos(100_000_000), latitude, longitude, DELTA_T)
                .zenithAngle();
    double after =
        90
            - Grena3.calculateSolarPosition(
                    time.plusNanos(100_000_000), latitude, longitude, DELTA_T)
                .zenithAngle();
    assertThat(direction * (before - horizon)).isNegative();
    assertThat(direction * (after - horizon)).isPositive();
  }

  @ParameterizedTest
  @ValueSource(doubles = {-10, -1e-8, 0, 1e-8})
  void findsAnalyticallyKnownCrossings(double horizon) {
    Instant noon = Instant.parse("2024-03-20T12:00:00Z");
    Instant start = noon.minusSeconds(43200), end = noon.plusSeconds(43200);
    // sin(elevation) = -0.5 * sin(phase/2)^2: a daily curve touching zero at noon.
    // Its crossings have a closed-form solution, independent of either position model.
    var events =
        new SolarEvents(
            (time, lat, lon) -> {
              double phase = 2 * Math.PI * (time.julianDate() - 2460390.0);
              double sine = Math.sin(phase / 2);
              return new SolarEvents.Position(
                  Math.toDegrees(Math.asin(-0.5 * sine * sine)), Math.toDegrees(phase));
            },
            2024,
            2024);
    var rises = findAll(start, end, (a, b) -> events.nextRise(a, b, 0, 0, 0, horizon));
    var sets = findAll(start, end, (a, b) -> events.nextSet(a, b, 0, 0, 0, horizon));
    if (horizon >= 0) {
      // Touching the horizon, or staying below it, creates no crossing.
      assertThat(rises).isEmpty();
      assertThat(sets).isEmpty();
    } else {
      double seconds =
          86400 / Math.PI * Math.asin(Math.sqrt(-2 * Math.sin(Math.toRadians(horizon))));
      assertSameEvents(List.of(at(noon, -seconds)), rises);
      assertSameEvents(List.of(at(noon, seconds)), sets);
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {-1, 1})
  void assignsMidnightCrossingToFollowingDate(int direction) {
    var date = LocalDate.of(2024, 3, 20);
    Instant midnight = date.atStartOfDay(ZoneOffset.UTC).toInstant();
    var events =
        new SolarEvents(
            (time, lat, lon) -> {
              double phase = 2 * Math.PI * (time.julianDate() - 2460389.5);
              return new SolarEvents.Position(
                  Math.toDegrees(Math.asin(direction * 0.5 * Math.sin(phase))),
                  Math.toDegrees(phase));
            },
            2024,
            2024);
    BiFunction<Instant, Instant, Optional<Instant>> search =
        direction > 0
            ? (a, b) -> events.nextRise(a, b, 0, 0, 0, 0.0)
            : (a, b) -> events.nextSet(a, b, 0, 0, 0, 0.0);
    assertThat(search.apply(midnight.minusSeconds(1), midnight)).contains(midnight);
    assertThat(search.apply(midnight, midnight.plusSeconds(1))).isEmpty();
    for (int offset = -1; offset <= 0; offset++) {
      var day = events.forDate(date.plusDays(offset), ZoneOffset.UTC, 0, 0, 0, 0.0);
      var crossings = direction > 0 ? day.rises() : day.sets();
      assertSameEvents(
          List.of(midnight.plusSeconds(offset * 86400L)),
          crossings.stream().map(ZonedDateTime::toInstant).toList());
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void splittingSearchPreservesEvents(boolean grena) {
    var events = grena ? SolarEvents.grena3() : EVENTS;
    var random = new Random(20260925);
    for (int sample = 0; sample < 24; sample++) {
      int year = grena ? 2011 + random.nextInt(99) : -1999 + random.nextInt(7999);
      Instant start =
          LocalDate.of(year, 1, 1)
              .plusDays(random.nextInt(365))
              .atStartOfDay(ZoneOffset.UTC)
              .toInstant()
              .plusSeconds(random.nextInt(86400));
      Instant end = start.plusSeconds(2 * 86400);
      double latitude =
          sample % 6 == 0 ? 90 : sample % 6 == 1 ? -90 : random.nextDouble() * 180 - 90;
      double longitude = random.nextDouble() * 360 - 180;
      double horizon =
          sample % 2 == 0
              ? Horizon.values()[(sample / 2) % 4].elevation()
              : random.nextDouble() * 40 - 20;
      List<BiFunction<Instant, Instant, Optional<Instant>>> searches =
          List.of(
              (a, b) -> events.nextRise(a, b, latitude, longitude, DELTA_T, horizon),
              (a, b) -> events.nextSet(a, b, latitude, longitude, DELTA_T, horizon),
              (a, b) -> events.nextTransit(a, b, longitude, DELTA_T));
      for (var search : searches) {
        var whole = findAll(start, end, search);
        var splits = new ArrayList<Instant>();
        splits.add(start.plusSeconds(1 + random.nextInt(2 * 86400 - 1)));
        for (var event : whole) {
          splits.add(event.minusMillis(2));
          splits.add(event);
          splits.add(event.plusMillis(2));
        }
        for (var split : splits) {
          if (!split.isAfter(start) || !split.isBefore(end)) continue;
          var parts = new ArrayList<>(findAll(start, split, search));
          parts.addAll(findAll(split, end, search));
          assertSameEvents(whole, parts);
        }
      }
    }
  }

  private static List<Instant> findAll(
      Instant start, Instant end, BiFunction<Instant, Instant, Optional<Instant>> search) {
    var result = new ArrayList<Instant>();
    while (true) {
      var next = search.apply(start, end);
      if (next.isEmpty()) return result;
      Instant event = next.orElseThrow();
      assertThat(event).isAfter(start).isBeforeOrEqualTo(end);
      result.add(event);
      // These intervals span at most two days. Fail promptly on repeated roots.
      assertThat(result).hasSizeLessThan(10);
      start = event;
    }
  }

  private static void assertSameEvents(List<Instant> expected, List<Instant> actual) {
    assertThat(actual).hasSameSizeAs(expected);
    for (int i = 0; i < expected.size(); i++) {
      assertThat(Duration.between(expected.get(i), actual.get(i)).abs())
          .as("event %s", expected.get(i))
          .isLessThanOrEqualTo(Duration.ofMillis(1));
    }
  }

  @Test
  void customProviderControlsPositionsAndSupportedYears() {
    // A deliberately simple solar path with known crossing times. TT and longitude shift
    // its phase, so this also checks that both reach the provider unchanged.
    var date = LocalDate.of(2024, 3, 20);
    var events =
        new SolarEvents(
            (time, latitude, longitude) -> {
              double hourAngle = (time.julianEphemerisDay() - 2460390.0) * 360 + longitude;
              double elevation =
                  Math.toDegrees(
                      Math.asin(
                          Math.cos(Math.toRadians(latitude))
                              * Math.cos(Math.toRadians(hourAngle))));
              return new SolarEvents.Position(elevation, hourAngle);
            },
            2024,
            2024);
    var day = events.forDate(date, ZoneOffset.UTC, 30, 15, 60, 0.0);
    compareEvents("2024-03-20T04:59:00Z", day.rises(), 0.002);
    compareEvents("2024-03-20T10:59:00Z", day.transits(), 0.002);
    compareEvents("2024-03-20T16:59:00Z", day.sets(), 0.002);
    assertThat(day.stateAtStart()).isEqualTo(SolarEvents.HorizonState.BELOW);
    assertThrows(
        IllegalArgumentException.class,
        () -> events.forDate(date.minusYears(1), ZoneOffset.UTC, 30, 15, 60));
    assertThrows(
        IllegalArgumentException.class,
        () -> events.forDate(LocalDate.of(2024, 12, 31), ZoneOffset.UTC, 30, 15, 60));
  }

  @ParameterizedTest
  @ValueSource(doubles = {-3600, -3599.9995, -3599.998})
  void dailyLookbackRespectsTheEphemerisLowerBound(double deltaT) {
    // TT begins at the provider's lower bound, 0.5 ms after it, or 2 ms after it.
    // The first two dates used to fail because the 1 ms lookback went outside that range.
    var events = SolarEvents.grena3();
    var date = LocalDate.of(2010, 1, 1);
    var zone = ZoneOffset.ofHours(-1);
    Instant start = date.atStartOfDay(zone).toInstant();
    Instant end = date.plusDays(1).atStartOfDay(zone).toInstant();
    var day = events.forDate(date, zone, 0, 0, deltaT);
    var expected =
        List.of(
            events.nextRise(start, end, 0, 0, deltaT, Horizon.SUNRISE_SUNSET).orElseThrow(),
            events.nextTransit(start, end, 0, deltaT).orElseThrow(),
            events.nextSet(start, end, 0, 0, deltaT, Horizon.SUNRISE_SUNSET).orElseThrow());
    var actual = List.of(day.rises(), day.transits(), day.sets());
    for (int i = 0; i < expected.size(); i++) {
      assertThat(actual.get(i)).hasSize(1);
      assertThat(Duration.between(expected.get(i), actual.get(i).get(0).toInstant()).abs())
          .isLessThanOrEqualTo(Duration.ofMillis(1));
    }
  }

  @Test
  void grenaSearchesRespectTheirNarrowerDateRange() {
    var events = SolarEvents.grena3();
    for (int year : new int[] {2010, 2110}) {
      assertThat(events.forDate(LocalDate.of(year, 1, 1), ZoneOffset.UTC, 0, 0, 0).rises())
          .hasSize(1);
    }
    assertThrows(
        IllegalArgumentException.class,
        () -> events.forDate(LocalDate.of(2009, 12, 31), ZoneOffset.UTC, 0, 0, 0));
    assertThrows(
        IllegalArgumentException.class,
        () -> events.forDate(LocalDate.of(2111, 1, 1), ZoneOffset.UTC, 0, 0, 0));
    assertThrows(
        IllegalArgumentException.class,
        () -> events.forDate(LocalDate.of(2010, 1, 1), ZoneOffset.UTC, 0, 0, -1));
    assertThrows(
        IllegalArgumentException.class,
        () -> events.forDate(LocalDate.of(2110, 12, 31), ZoneOffset.UTC, 0, 0, 1));
  }

  @Test
  void respectsSearchBoundsAndRejectsInvalidInputs() {
    var date = LocalDate.of(2024, 3, 20);
    Instant start = date.atStartOfDay(ZoneOffset.UTC).toInstant(), end = start.plusSeconds(86400);
    Instant transit = EVENTS.nextTransit(start, end, 0, DELTA_T).orElseThrow();
    assertThat(EVENTS.nextTransit(start, transit.minusMillis(2), 0, DELTA_T)).isEmpty();
    assertThat(EVENTS.nextTransit(transit, end, 0, DELTA_T)).isEmpty();
    assertThat(EVENTS.nextTransit(start, start, 0, DELTA_T)).isEmpty();
    assertThrows(IllegalArgumentException.class, () -> EVENTS.nextTransit(end, start, 0, DELTA_T));
    assertThrows(
        IllegalArgumentException.class, () -> EVENTS.nextTransit(start, end, 0, Double.NaN));
    assertThrows(
        IllegalArgumentException.class, () -> EVENTS.nextRise(start, end, 91, 0, DELTA_T, -6));
    assertThrows(
        IllegalArgumentException.class, () -> EVENTS.nextSet(start, end, 0, 0, DELTA_T, -91));
    assertThrows(
        IllegalArgumentException.class,
        () -> EVENTS.nextSet(start, end, 0, 0, DELTA_T, Double.NaN));
    assertThrows(
        IllegalArgumentException.class,
        () -> EVENTS.forDate(LocalDate.of(-2000, 1, 1), ZoneOffset.UTC, 0, 0, -1));
    assertThrows(
        IllegalArgumentException.class,
        () -> EVENTS.forDate(LocalDate.of(6000, 12, 31), ZoneOffset.UTC, 0, 0, 1));
    assertThrows(
        IllegalArgumentException.class,
        () -> EVENTS.forDate(LocalDate.of(6001, 1, 1), ZoneOffset.UTC, 0, 0, 0));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new SolarEvents((time, lat, lon) -> new SolarEvents.Position(Double.NaN, 0), 2024, 2024)
                .forDate(date, ZoneOffset.UTC, 0, 0, 0));
    assertThrows(
        IllegalArgumentException.class,
        () -> new SolarEvents((time, lat, lon) -> new SolarEvents.Position(0, 0), 2024, 2023));
  }
}
