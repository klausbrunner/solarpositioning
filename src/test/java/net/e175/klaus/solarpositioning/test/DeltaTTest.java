package net.e175.klaus.solarpositioning.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import net.e175.klaus.solarpositioning.DeltaT;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.junit.jupiter.params.provider.CsvSource;

class DeltaTTest {

  @Test
  void testDateTimeInputsUseLocalDate() {
    final LocalDate date = LocalDate.of(2026, 1, 1);
    final double expected = DeltaT.estimate(date);
    final var dateTime = date.atTime(0, 30);
    assertEquals(expected, DeltaT.estimate(dateTime));
    assertEquals(expected, DeltaT.estimate(dateTime.atOffset(ZoneOffset.ofHours(14))));
    assertEquals(expected, DeltaT.estimate(dateTime.atZone(ZoneOffset.ofHours(14))));
    assertThrows(DateTimeException.class, () -> DeltaT.estimate(LocalTime.NOON));
  }

  private LocalDate yearCal(int year) {
    return LocalDate.of(year, 1, 1);
  }

  @Test
  void testHistoricalValues() {
    Assertions.assertEquals(27364, DeltaT.estimate(yearCal(-1000)), 2000);

    assertEquals(17190, DeltaT.estimate(yearCal(-400)), 2000);

    assertEquals(14080, DeltaT.estimate(yearCal(-300)), 3);

    assertEquals(12790, DeltaT.estimate(yearCal(-200)), 2);

    assertEquals(7680, DeltaT.estimate(yearCal(300)), 1);

    assertEquals(3810, DeltaT.estimate(yearCal(700)), 3);

    assertEquals(200, DeltaT.estimate(yearCal(1500)), 2);

    assertEquals(44, DeltaT.estimate(yearCal(1657)), 4);

    assertEquals(13.7, DeltaT.estimate(yearCal(1750)), 2);

    assertEquals(7, DeltaT.estimate(yearCal(1850)), 1);

    assertEquals(1.04, DeltaT.estimate(yearCal(1870)), 1);

    assertEquals(-3, DeltaT.estimate(yearCal(1900)), 1);

    assertEquals(10.38, DeltaT.estimate(yearCal(1910)), 1);

    assertEquals(24.02, DeltaT.estimate(yearCal(1930)), 1);

    assertEquals(29, DeltaT.estimate(yearCal(1950)), 1);
  }

  @ParameterizedTest
  @CsvSource({
    "2000, 63.86",
    "2005, 64.69",
    "2010, 66.155",
    "2014.999, 67.619707",
    "2015, 67.62",
    "2017, 68.411341883813577",
    "2020, 69.376973129145384",
    "2023, 69.297611033970213",
    "2026, 69.035467233469703",
    "2026.5, 69.14",
    "2027, 69.285460949825051",
    "2030, 70.218476069109713",
    "2045, 76.432822474131413",
    "2100, 121.310213415151708",
    "3000, 5787.512927094449878"
  })
  void testAdaptationReferenceValues(double year, double expected) {
    assertEquals(expected, DeltaT.estimate(year), 1e-10);
  }

  @ParameterizedTest
  @CsvSource({
    "2015, 67.6439282",
    "2017, 68.5927130",
    "2017.4246575342465, 68.8085579",
    "2020, 69.3611665",
    "2023, 69.2038475",
    "2026, 69.1099131",
    "2026.498630136986321, 69.1691721"
  })
  void testRecentIersObservations(double year, double observed) {
    // IERS 20u24 C04, downloaded 14 September 2026; Delta T = 32.184 + TAI-UTC - UT1-UTC.
    assertEquals(observed, DeltaT.estimate(year), 0.22);
  }

  @ParameterizedTest
  @CsvSource({"2015, 67.62", "2026.5, 69.14"})
  void testContinuousAtUpdatedBranchBoundaries(double year, double expected) {
    assertEquals(expected, DeltaT.estimate(Math.nextDown(year)), 1e-12);
    assertEquals(expected, DeltaT.estimate(year), 1e-12);
    assertEquals(expected, DeltaT.estimate(Math.nextUp(year)), 1e-12);
  }

  @ParameterizedTest
  @CsvSource({"2015", "2026.5"})
  void testSmoothJoinsAtUpdatedBranchBoundaries(double year) {
    double step = 1e-4;
    double leftSlope = (DeltaT.estimate(year) - DeltaT.estimate(year - step)) / step;
    double rightSlope = (DeltaT.estimate(year + step) - DeltaT.estimate(year)) / step;
    assertEquals(leftSlope, rightSlope, 2e-5);
  }

  @ParameterizedTest
  @CsvSource({
    "2014-12-31, 2014, 12",
    "2015-01-01, 2015, 1",
    "2026-06-30, 2026, 6",
    "2026-07-01, 2026, 7"
  })
  void testDateOverloadAtUpdatedBranches(LocalDate date, int year, int month) {
    assertEquals(DeltaT.estimate(year + (month - 0.5) / 12), DeltaT.estimate(date));
  }

  @Test
  void testUpperYearLimit() {
    assertThrows(IllegalArgumentException.class, () -> DeltaT.estimate(Math.nextUp(3000.0)));
  }

  @ParameterizedTest
  @CsvFileSource(resources = "deltat/deltat.data.txt")
  void testUSNODataRecent(String line) {
    // CsvFileSource apparently can't deal with space-separated formats, so need to DIY here
    String[] parts = line.split("\\s+");
    LocalDate date =
        LocalDate.of(
            Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
    double deltaT = Double.parseDouble(parts[3]);

    assertEquals(deltaT, DeltaT.estimate(date), deltaT * 0.05);
  }
}
