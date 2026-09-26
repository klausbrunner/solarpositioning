package net.e175.klaus.solarpositioning.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import net.e175.klaus.solarpositioning.JulianDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class JulianDateTest {
  private static final double TOLERANCE = 0.0000001;

  @ParameterizedTest
  @CsvSource({
    "-4713-11-24T12:00:00Z,0.0",
    "-0123-12-28T00:00:00Z,1676496.5",
    "-0123-12-29T00:00:00Z,1676497.5",
    "0837-04-14T07:12:00Z,2026871.8",
    "1582-10-04T00:00:00Z,2299149.5",
    "1582-10-15T00:00:00Z,2299160.5",
    "1970-01-01T00:00:00Z,2440587.5",
    "2000-01-01T12:00:00Z,2451545.0"
  })
  void usesProlepticGregorianDates(ZonedDateTime time, double expected) {
    assertEquals(expected, new JulianDate(time).julianDate(), TOLERANCE);
    assertEquals(new JulianDate(time), new JulianDate(time.toInstant()));
    assertEquals(new JulianDate(time, 69.184), new JulianDate(time.toInstant(), 69.184));
  }

  @Test
  void usesTheInstantAcrossTimeZones() {
    var time = ZonedDateTime.of(2003, 10, 17, 12, 30, 30, 0, ZoneOffset.ofHours(-7));
    var julianDate = new JulianDate(time);
    assertEquals(2452930.312847222, julianDate.julianDate(), TOLERANCE);
    assertEquals(julianDate, new JulianDate(time.withZoneSameInstant(ZoneOffset.UTC)));
  }

  @Test
  void preservesSubSecondPrecision() {
    var start = ZonedDateTime.of(2000, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC);
    var halfSecondLater = start.plusNanos(500_000_000);
    double elapsedJulianDays =
        new JulianDate(halfSecondLater).julianDate() - new JulianDate(start).julianDate();
    assertEquals(0.5 / 86_400, elapsedJulianDays, 1e-9);
  }

  @Test
  void calculatesTimeScales() {
    var time = new JulianDate(2452929.5, 69.184);
    assertEquals(2452929.5 + 69.184 / 86400, time.julianEphemerisDay(), TOLERANCE);
    assertEquals(0.03790554, time.julianCentury(), TOLERANCE);
    assertEquals(0.03790556607143789, time.julianEphemerisCentury(), TOLERANCE);
    assertEquals(0.003790556607143789, time.julianEphemerisMillennium(), TOLERANCE);
  }
}
