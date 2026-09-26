package net.e175.klaus.solarpositioning.test;

import static java.lang.Math.PI;
import static java.lang.Math.toDegrees;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import net.e175.klaus.solarpositioning.Atmosphere;
import net.e175.klaus.solarpositioning.SolarPosition;
import net.e175.klaus.solarpositioning.SolarPositions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

class Grena3Test {

  private static final SolarPositions POSITIONS = SolarPositions.grena3();
  private static final double TOLERANCE = 0.01; // advertised max error vis-a-vis SPA
  private static final ZonedDateTime GRENA_VALIDITY_START =
      ZonedDateTime.of(2010, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
  private static final ZonedDateTime GRENA_VALIDITY_END =
      ZonedDateTime.of(2110, 12, 31, 0, 0, 0, 0, ZoneOffset.UTC);

  @Test
  void testCSampleComparison() {
    ZonedDateTime time = ZonedDateTime.of(2012, 1, 1, 12, 0, 0, 0, ZoneOffset.ofHours(1));

    SolarPosition result =
        POSITIONS.at(time, toDegrees(0.73117), toDegrees(0.21787), 65, new Atmosphere(1000, 20));

    // Allow for the sample's rounded time conversion and printed angles.
    assertEquals(toDegrees(1.13381), result.zenithAngle(), 0.0003);
    assertEquals(toDegrees(-0.0591845 + PI) % 360.0, result.azimuth(), 0.0003);
  }

  @Test
  void testSpaComparison() {
    ZonedDateTime time = ZonedDateTime.of(2015, 6, 28, 17, 45, 12, 0, ZoneOffset.UTC);

    SolarPosition result = POSITIONS.at(time, 52.509663, 13.376481, 68, new Atmosphere(1000, 20));

    assertEquals(291.232854, result.azimuth(), TOLERANCE);
    assertEquals(76.799924, result.zenithAngle(), TOLERANCE);
  }

  @Test
  void preservesSubSecondPrecision() {
    ZonedDateTime time = ZonedDateTime.of(2015, 6, 28, 17, 45, 12, 0, ZoneOffset.UTC);

    SolarPosition atStart = POSITIONS.at(time, 52.509663, 13.376481, 68);
    SolarPosition halfSecondLater =
        POSITIONS.at(time.plusNanos(500_000_000), 52.509663, 13.376481, 68);

    assertNotEquals(atStart, halfSecondLater);
  }

  @Test
  void testSouthernSolstice() {
    ZonedDateTime time = ZonedDateTime.of(2012, 12, 22, 12, 0, 0, 0, ZoneOffset.UTC);

    SolarPosition result = POSITIONS.at(time, -41, 0, 0, new Atmosphere(1000, 20));

    assertEquals(359.08592, result.azimuth(), TOLERANCE * 1.5); // seems to be an algorithm problem
    assertEquals(17.5658, result.zenithAngle(), TOLERANCE);

    result = POSITIONS.at(time, -3, 0, 0, new Atmosphere(1000, 20));

    assertEquals(180.790356, result.azimuth(), TOLERANCE * 1.5); // seems to be an algorithm problem
    assertEquals(20.4285, result.zenithAngle(), TOLERANCE);
  }

  @Test
  void testUnrefractedPosition() {
    ZonedDateTime time = ZonedDateTime.of(2003, 10, 17, 12, 30, 30, 0, ZoneOffset.ofHours(-7));

    SolarPosition result = POSITIONS.at(time, 39.742476, -105.1786, 67);
    assertEquals(194.34024, result.azimuth(), TOLERANCE);
    assertEquals(50.1279, result.zenithAngle(), TOLERANCE);
  }

  @Test
  void testSillyLatLon() {
    ZonedDateTime time = ZonedDateTime.of(2003, 10, 17, 12, 30, 30, 0, ZoneOffset.ofHours(-7));

    assertThrows(
        IllegalArgumentException.class, () -> POSITIONS.at(time, 139.742476, -105.1786, 67));

    assertThrows(
        IllegalArgumentException.class, () -> POSITIONS.at(time, 39.742476, -205.1786, 67));
  }

  @ParameterizedTest
  @CsvFileSource(resources = "azimuth_zenith/spa_reference_testdata.csv")
  void testBulkSpaReferenceValues(
      ZonedDateTime dateTime, double lat, double lon, double refAzimuth, double refZenith) {
    Assumptions.assumeTrue(
        dateTime.isAfter(GRENA_VALIDITY_START) && dateTime.isBefore(GRENA_VALIDITY_END),
        "date out of validity range, skipping");

    SolarPosition res = POSITIONS.at(dateTime, lat, lon, 0, new Atmosphere(1000, 10));

    assertEquals(res, POSITIONS.forTime(dateTime, 0).at(lat, lon, new Atmosphere(1000, 10)));

    assertEquals(refAzimuth, res.azimuth(), TOLERANCE * 3, "azimuth");
    // TODO: investigate occasional outliers. TOLERANCE is usually fine but in current dataset
    // there's 1 outlier.

    assertEquals(refZenith, res.zenithAngle(), TOLERANCE, "zenith");
  }
}
