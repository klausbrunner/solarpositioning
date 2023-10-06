package net.e175.klaus.solarpositioning.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import net.e175.klaus.solarpositioning.DeltaT;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

class DeltaTTest {

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
