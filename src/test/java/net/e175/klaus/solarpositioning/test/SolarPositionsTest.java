package net.e175.klaus.solarpositioning.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import net.e175.klaus.solarpositioning.Atmosphere;
import net.e175.klaus.solarpositioning.SolarPositions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class SolarPositionsTest {
  private static final ZonedDateTime TIME = ZonedDateTime.parse("2024-06-21T08:30:12.345678+02:00");
  private static final double LATITUDE = 48.21, LONGITUDE = 16.37, DELTA_T = 69.184;
  private static final Atmosphere ATMOSPHERE = new Atmosphere(1010, 15);

  @ParameterizedTest
  @CsvSource({
    "2024-06-21T08:30:12.345678+02:00,190",
    "2024-01-01T00:00:00.123456789+14:00,-50",
    "2024-12-31T23:59:59.987654321-11:00,0"
  })
  void preparedAndInstantPositionsMatchZonedTimes(ZonedDateTime time, double spaHeight) {
    var instant = time.toInstant();
    for (boolean useGrena3 : new boolean[] {false, true}) {
      var positions = useGrena3 ? SolarPositions.grena3() : new SolarPositions();
      double height = useGrena3 ? 0 : spaHeight;
      var snapshot = positions.forTime(time, DELTA_T);
      var instantSnapshot = positions.forTime(instant, DELTA_T);
      for (double latitude : new double[] {-90, -48.21, 0, 48.21, 90}) {
        for (double longitude : new double[] {-180, 0, 16.37, 180}) {
          var geometric = positions.at(time, latitude, longitude, DELTA_T);
          var apparent = positions.at(time, latitude, longitude, DELTA_T, ATMOSPHERE);
          var elevated = positions.at(time, latitude, longitude, height, DELTA_T);
          var elevatedApparent =
              positions.at(time, latitude, longitude, height, DELTA_T, ATMOSPHERE);
          assertEquals(geometric, positions.at(instant, latitude, longitude, DELTA_T));
          assertEquals(apparent, positions.at(instant, latitude, longitude, DELTA_T, ATMOSPHERE));
          assertEquals(elevated, positions.at(instant, latitude, longitude, height, DELTA_T));
          assertEquals(
              elevatedApparent,
              positions.at(instant, latitude, longitude, height, DELTA_T, ATMOSPHERE));
          for (var prepared : List.of(snapshot, instantSnapshot)) {
            assertEquals(geometric, prepared.at(latitude, longitude));
            assertEquals(apparent, prepared.at(latitude, longitude, ATMOSPHERE));
            assertEquals(elevated, prepared.at(latitude, longitude, height));
            assertEquals(elevatedApparent, prepared.at(latitude, longitude, height, ATMOSPHERE));
          }
        }
      }
    }
  }

  @Test
  void preparedTimesAreIndependent() {
    for (var positions : List.of(new SolarPositions(), SolarPositions.grena3())) {
      var first = positions.forTime(TIME, DELTA_T);
      var expected = first.at(LATITUDE, LONGITUDE);
      var later = positions.forTime(TIME.plusHours(6), DELTA_T + 1);
      assertEquals(
          positions.at(TIME.plusHours(6), LATITUDE, LONGITUDE, DELTA_T + 1),
          later.at(LATITUDE, LONGITUDE));
      assertEquals(expected, first.at(LATITUDE, LONGITUDE));
    }
  }

  @ParameterizedTest
  @CsvSource({"91,0", "-91,0", "0,181", "0,-181", "NaN,0", "0,NaN"})
  void preparedPositionsRejectInvalidCoordinates(double latitude, double longitude) {
    for (var positions : List.of(new SolarPositions(), SolarPositions.grena3())) {
      var snapshot = positions.forTime(TIME, DELTA_T);
      assertThrows(IllegalArgumentException.class, () -> snapshot.at(latitude, longitude));
      assertThrows(
          IllegalArgumentException.class, () -> snapshot.at(latitude, longitude, ATMOSPHERE));
    }
  }

  @ParameterizedTest
  @ValueSource(doubles = {-50, 190})
  void grenaRejectsNonzeroHeight(double height) {
    var positions = SolarPositions.grena3();
    var snapshot = positions.forTime(TIME, DELTA_T);
    assertThrows(IllegalArgumentException.class, () -> snapshot.at(LATITUDE, LONGITUDE, height));
    assertThrows(
        IllegalArgumentException.class, () -> snapshot.at(LATITUDE, LONGITUDE, height, ATMOSPHERE));
    assertThrows(
        IllegalArgumentException.class,
        () -> positions.at(TIME, LATITUDE, LONGITUDE, height, DELTA_T));
    assertThrows(
        IllegalArgumentException.class,
        () -> positions.at(TIME, LATITUDE, LONGITUDE, height, DELTA_T, ATMOSPHERE));
    assertThrows(
        IllegalArgumentException.class,
        () -> positions.at(TIME.toInstant(), LATITUDE, LONGITUDE, height, DELTA_T));
    assertThrows(
        IllegalArgumentException.class,
        () -> positions.at(TIME.toInstant(), LATITUDE, LONGITUDE, height, DELTA_T, ATMOSPHERE));
  }

  @ParameterizedTest
  @ValueSource(doubles = {Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY})
  void rejectsNonFiniteHeightAndDeltaT(double invalid) {
    for (var positions : List.of(new SolarPositions(), SolarPositions.grena3())) {
      assertThrows(
          IllegalArgumentException.class,
          () -> positions.at(TIME, LATITUDE, LONGITUDE, invalid, DELTA_T));
      assertThrows(
          IllegalArgumentException.class, () -> positions.at(TIME, LATITUDE, LONGITUDE, invalid));
      assertThrows(IllegalArgumentException.class, () -> positions.forTime(TIME, invalid));
      assertThrows(
          IllegalArgumentException.class, () -> positions.forTime(TIME.toInstant(), invalid));
      var snapshot = positions.forTime(TIME, DELTA_T);
      assertThrows(IllegalArgumentException.class, () -> snapshot.at(LATITUDE, LONGITUDE, invalid));
      assertThrows(
          IllegalArgumentException.class,
          () -> snapshot.at(LATITUDE, LONGITUDE, invalid, ATMOSPHERE));
    }
  }

  @ParameterizedTest
  @CsvSource({
    "0,15", "-1,15", "3000,15", "NaN,15", "Infinity,15",
    "1010,-273", "1010,273", "1010,NaN", "1010,Infinity", "1010,-Infinity"
  })
  void rejectsUnsupportedAtmosphere(double pressure, double temperature) {
    assertThrows(IllegalArgumentException.class, () -> new Atmosphere(pressure, temperature));
  }

  @Test
  void rejectsMissingTimeAndAtmosphere() {
    for (var positions : List.of(new SolarPositions(), SolarPositions.grena3())) {
      assertThrows(
          NullPointerException.class,
          () -> positions.at((ZonedDateTime) null, LATITUDE, LONGITUDE, DELTA_T));
      assertThrows(
          NullPointerException.class,
          () -> positions.at((Instant) null, LATITUDE, LONGITUDE, DELTA_T));
      assertThrows(
          NullPointerException.class, () -> positions.at(TIME, LATITUDE, LONGITUDE, DELTA_T, null));
      assertThrows(
          NullPointerException.class,
          () -> positions.at(TIME, LATITUDE, LONGITUDE, 0, DELTA_T, null));
      assertThrows(
          NullPointerException.class, () -> positions.forTime((ZonedDateTime) null, DELTA_T));
      assertThrows(NullPointerException.class, () -> positions.forTime((Instant) null, DELTA_T));
      var snapshot = positions.forTime(TIME, DELTA_T);
      assertThrows(NullPointerException.class, () -> snapshot.at(LATITUDE, LONGITUDE, null));
      assertThrows(NullPointerException.class, () -> snapshot.at(LATITUDE, LONGITUDE, 0, null));
    }
  }
}
