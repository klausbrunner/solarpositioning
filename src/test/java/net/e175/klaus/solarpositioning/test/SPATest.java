package net.e175.klaus.solarpositioning.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import net.e175.klaus.solarpositioning.Atmosphere;
import net.e175.klaus.solarpositioning.SolarPosition;
import net.e175.klaus.solarpositioning.SolarPositions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.junit.jupiter.params.provider.CsvSource;

class SPATest {

  private static final SolarPositions POSITIONS = new SolarPositions();
  private static final double TOLERANCE = 0.0001;

  @Test
  void testSpaExample() {
    ZonedDateTime time = ZonedDateTime.of(2003, 10, 17, 12, 30, 30, 0, ZoneOffset.ofHours(-7));

    SolarPosition result =
        POSITIONS.at(time, 39.742476, -105.1786, 1830.14, 67, new Atmosphere(820, 11));

    assertEquals(194.340241, result.azimuth(), TOLERANCE / 100);
    assertEquals(50.111622, result.zenithAngle(), TOLERANCE / 100);
  }

  @Test
  void preservesSubSecondPrecision() {
    ZonedDateTime time = ZonedDateTime.of(2003, 10, 17, 12, 30, 30, 0, ZoneOffset.ofHours(-7));

    SolarPosition atStart = POSITIONS.at(time, 39.742476, -105.1786, 1830.14, 67);
    SolarPosition halfSecondLater =
        POSITIONS.at(time.plusNanos(500_000_000), 39.742476, -105.1786, 1830.14, 67);

    assertNotEquals(atStart, halfSecondLater);
  }

  @ParameterizedTest
  @CsvSource({
    "2024-01-10T12:00:00Z,-21.98916814086509,1.8274493157629763,90",
    "2024-01-21T12:00:00Z,19.9504066797851,-177.21176826518766,-90"
  })
  void handlesZenithAndNadirRoundoff(
      ZonedDateTime time, double latitude, double longitude, double elevation) {
    var position = POSITIONS.at(time, latitude, longitude, 69.184);
    assertEquals(elevation, position.elevation(), 0.00002);
    assertEquals(position, POSITIONS.forTime(time, 69.184).at(latitude, longitude));
  }

  @Test
  void testSouthernSolstice() {
    ZonedDateTime time = ZonedDateTime.of(2012, 12, 22, 12, 0, 0, 0, ZoneOffset.UTC);

    SolarPosition result = POSITIONS.at(time, -41, 0, 100, 0, new Atmosphere(1000, 20));

    assertEquals(359.08592, result.azimuth(), TOLERANCE);
    assertEquals(17.5658, result.zenithAngle(), TOLERANCE);

    result = POSITIONS.at(time, -3, 0, 100, 0, new Atmosphere(1000, 20));

    assertEquals(180.790356, result.azimuth(), TOLERANCE);
    assertEquals(20.4285, result.zenithAngle(), TOLERANCE);
  }

  @Test
  void testUnrefractedPosition() {
    ZonedDateTime time = ZonedDateTime.of(2003, 10, 17, 12, 30, 30, 0, ZoneOffset.ofHours(-7));

    SolarPosition result = POSITIONS.at(time, 39.742476, -105.1786, 1830.14, 67);
    assertEquals(194.34024, result.azimuth(), TOLERANCE);
    assertEquals(50.1279, result.zenithAngle(), TOLERANCE);
  }

  @Test
  void testSillyLatLon() {
    ZonedDateTime time = ZonedDateTime.of(2003, 10, 17, 12, 30, 30, 0, ZoneOffset.ofHours(-7));

    assertThrows(
        IllegalArgumentException.class,
        () -> POSITIONS.at(time, 139.742476, -105.1786, 1830.14, 67, new Atmosphere(820, 11)));

    assertThrows(
        IllegalArgumentException.class,
        () -> POSITIONS.at(time, 39.742476, -205.1786, 1830.14, 67, new Atmosphere(820, 11)));
  }

  @ParameterizedTest
  @CsvFileSource(resources = "azimuth_zenith/spa_reference_testdata.csv")
  void testBulkSpaReferenceValues(
      ZonedDateTime dateTime, double lat, double lon, double refAzimuth, double refZenith) {
    SolarPosition res = POSITIONS.at(dateTime, lat, lon, 0, 0, new Atmosphere(1000, 10));

    assertEquals(res, POSITIONS.forTime(dateTime, 0).at(lat, lon, new Atmosphere(1000, 10)));

    assertEquals(refAzimuth, res.azimuth(), TOLERANCE / 100);
    assertEquals(refZenith, res.zenithAngle(), TOLERANCE / 100);
  }
}
