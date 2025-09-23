package net.e175.klaus.solarpositioning.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.e175.klaus.solarpositioning.SPA;
import net.e175.klaus.solarpositioning.SolarPosition;
import org.junit.jupiter.api.Test;

class SPABulkProcessingTest {

  private static final double TOLERANCE = 1e-6;

  @Test
  void timeDependentPartsProduceIdenticalResults() {
    ZonedDateTime dateTime =
        ZonedDateTime.of(LocalDateTime.of(2024, 6, 21, 12, 0), ZoneId.of("America/Los_Angeles"));
    double latitude = 37.7749;
    double longitude = -122.4194;
    double elevation = 10;
    double deltaT = 69.0;
    double pressure = 1013.25;
    double temperature = 20;

    // Calculate using traditional method
    SolarPosition traditional =
        SPA.calculateSolarPosition(
            dateTime, latitude, longitude, elevation, deltaT, pressure, temperature);

    // Calculate using separated time-dependent parts
    SPA.SpaTimeDependent timeDependent = SPA.calculateSpaTimeDependentParts(dateTime, deltaT);
    SolarPosition optimized =
        SPA.calculateSolarPositionWithTimeDependentParts(
            latitude, longitude, elevation, pressure, temperature, timeDependent);

    assertThat(optimized.azimuth()).isCloseTo(traditional.azimuth(), within(TOLERANCE));
    assertThat(optimized.zenithAngle()).isCloseTo(traditional.zenithAngle(), within(TOLERANCE));
  }

  @Test
  void timeDependentPartsProduceIdenticalResultsWithoutRefraction() {
    ZonedDateTime dateTime =
        ZonedDateTime.of(LocalDateTime.of(2024, 1, 15, 8, 30), ZoneId.of("Europe/Berlin"));
    double latitude = 52.5200;
    double longitude = 13.4050;
    double elevation = 34;
    double deltaT = 69.2;

    // Calculate using traditional method
    SolarPosition traditional =
        SPA.calculateSolarPosition(dateTime, latitude, longitude, elevation, deltaT);

    // Calculate using separated time-dependent parts
    SPA.SpaTimeDependent timeDependent = SPA.calculateSpaTimeDependentParts(dateTime, deltaT);
    SolarPosition optimized =
        SPA.calculateSolarPositionWithTimeDependentParts(
            latitude, longitude, elevation, timeDependent);

    assertThat(optimized.azimuth()).isCloseTo(traditional.azimuth(), within(TOLERANCE));
    assertThat(optimized.zenithAngle()).isCloseTo(traditional.zenithAngle(), within(TOLERANCE));
  }

  @Test
  void bulkProcessingWithFixedTime() {
    ZonedDateTime dateTime =
        ZonedDateTime.of(LocalDateTime.of(2024, 3, 20, 14, 0), ZoneId.of("UTC"));
    double deltaT = 69.0;

    // Pre-compute time-dependent parts once
    SPA.SpaTimeDependent timeDependent = SPA.calculateSpaTimeDependentParts(dateTime, deltaT);

    // Test multiple coordinate pairs
    List<Coordinate> coordinates =
        List.of(
            new Coordinate(0, 0, 0), // Equator, Prime Meridian
            new Coordinate(51.5074, -0.1278, 10), // London
            new Coordinate(-33.8688, 151.2093, 5), // Sydney
            new Coordinate(35.6762, 139.6503, 40), // Tokyo
            new Coordinate(40.7128, -74.0060, 10), // New York
            new Coordinate(-54.8019, -68.3030, 0) // Ushuaia
            );

    for (Coordinate coord : coordinates) {
      // Calculate traditional way
      SolarPosition traditional =
          SPA.calculateSolarPosition(
              dateTime, coord.latitude, coord.longitude, coord.elevation, deltaT);

      // Calculate optimized way
      SolarPosition optimized =
          SPA.calculateSolarPositionWithTimeDependentParts(
              coord.latitude, coord.longitude, coord.elevation, timeDependent);

      assertThat(optimized.azimuth())
          .as("Azimuth for lat=%f, lon=%f", coord.latitude, coord.longitude)
          .isCloseTo(traditional.azimuth(), within(TOLERANCE));
      assertThat(optimized.zenithAngle())
          .as("Zenith angle for lat=%f, lon=%f", coord.latitude, coord.longitude)
          .isCloseTo(traditional.zenithAngle(), within(TOLERANCE));
    }
  }

  @Test
  void bulkProcessingWithRandomCoordinates() {
    ZonedDateTime dateTime =
        ZonedDateTime.of(LocalDateTime.of(2024, 7, 1, 6, 0), ZoneId.of("Asia/Shanghai"));
    double deltaT = 69.1;
    double pressure = 1000;
    double temperature = 15;

    // Pre-compute time-dependent parts once
    SPA.SpaTimeDependent timeDependent = SPA.calculateSpaTimeDependentParts(dateTime, deltaT);

    Random random = new Random(42); // Fixed seed for reproducibility
    for (int i = 0; i < 100; i++) {
      double latitude = random.nextDouble() * 180 - 90; // -90 to +90
      double longitude = random.nextDouble() * 360 - 180; // -180 to +180
      double elevation = random.nextDouble() * 1000; // 0 to 1000 meters

      // Calculate traditional way
      SolarPosition traditional =
          SPA.calculateSolarPosition(
              dateTime, latitude, longitude, elevation, deltaT, pressure, temperature);

      // Calculate optimized way
      SolarPosition optimized =
          SPA.calculateSolarPositionWithTimeDependentParts(
              latitude, longitude, elevation, pressure, temperature, timeDependent);

      assertThat(optimized.azimuth())
          .as("Azimuth for random coordinate #%d", i)
          .isCloseTo(traditional.azimuth(), within(TOLERANCE));
      assertThat(optimized.zenithAngle())
          .as("Zenith angle for random coordinate #%d", i)
          .isCloseTo(traditional.zenithAngle(), within(TOLERANCE));
    }
  }

  @Test
  void recycleTimeDependentPartsForMultipleCoordinates() {
    ZonedDateTime dateTime =
        ZonedDateTime.of(LocalDateTime.of(2024, 9, 22, 15, 30), ZoneId.of("Europe/Berlin"));
    double deltaT = 69.0;

    // Compute time-dependent parts once
    SPA.SpaTimeDependent timeDependent = SPA.calculateSpaTimeDependentParts(dateTime, deltaT);

    // Test a few different coordinates using the same time-dependent parts
    double[][] coordinates = {
      {52.5200, 13.4050, 34}, // Berlin
      {48.8566, 2.3522, 35}, // Paris
      {41.9028, 12.4964, 21} // Rome
    };

    for (double[] coord : coordinates) {
      double lat = coord[0];
      double lon = coord[1];
      double elevation = coord[2];

      // Calculate using traditional method
      SolarPosition traditional = SPA.calculateSolarPosition(dateTime, lat, lon, elevation, deltaT);

      // Calculate using recycled time-dependent parts
      SolarPosition optimized =
          SPA.calculateSolarPositionWithTimeDependentParts(lat, lon, elevation, timeDependent);

      assertThat(optimized.azimuth())
          .as("Azimuth for coordinate lat=%f, lon=%f", lat, lon)
          .isCloseTo(traditional.azimuth(), within(TOLERANCE));
      assertThat(optimized.zenithAngle())
          .as("Zenith angle for coordinate lat=%f, lon=%f", lat, lon)
          .isCloseTo(traditional.zenithAngle(), within(TOLERANCE));
    }
  }

  @Test
  void performanceBenefitOfBulkProcessing() {
    ZonedDateTime dateTime =
        ZonedDateTime.of(LocalDateTime.of(2024, 12, 21, 12, 0), ZoneId.of("UTC"));
    double deltaT = 69.0;

    // Generate a grid of coordinates
    List<Coordinate> coordinates = new ArrayList<>();
    for (double lat = -60; lat <= 60; lat += 10) {
      for (double lon = -180; lon <= 170; lon += 10) {
        coordinates.add(new Coordinate(lat, lon, 0));
      }
    }

    // Warm up JVM
    for (int warmup = 0; warmup < 10; warmup++) {
      for (Coordinate coord : coordinates) {
        SPA.calculateSolarPosition(
            dateTime, coord.latitude, coord.longitude, coord.elevation, deltaT);
      }
    }

    // Time traditional approach
    long startTraditional = System.nanoTime();
    for (Coordinate coord : coordinates) {
      SPA.calculateSolarPosition(
          dateTime, coord.latitude, coord.longitude, coord.elevation, deltaT);
    }
    long traditionalTime = System.nanoTime() - startTraditional;

    // Time optimized approach
    long startOptimized = System.nanoTime();
    SPA.SpaTimeDependent timeDependent = SPA.calculateSpaTimeDependentParts(dateTime, deltaT);
    for (Coordinate coord : coordinates) {
      SPA.calculateSolarPositionWithTimeDependentParts(
          coord.latitude, coord.longitude, coord.elevation, timeDependent);
    }
    long optimizedTime = System.nanoTime() - startOptimized;

    double speedup = (double) traditionalTime / optimizedTime;
    System.out.printf(
        "Traditional: %.2f ms, Optimized: %.2f ms, Speedup: %.2fx%n",
        traditionalTime / 1_000_000.0, optimizedTime / 1_000_000.0, speedup);

    // Optimized approach should be significantly faster
    assertThat(optimizedTime).isLessThan(traditionalTime);
  }

  @Test
  void timeDependentPartsWithExtremeValues() {
    // Test at extreme latitudes
    ZonedDateTime dateTime =
        ZonedDateTime.of(LocalDateTime.of(2024, 6, 21, 0, 0), ZoneId.of("UTC"));
    double deltaT = 69.0;

    SPA.SpaTimeDependent timeDependent = SPA.calculateSpaTimeDependentParts(dateTime, deltaT);

    // North Pole
    double latitude = 89.9;
    double longitude = 0;
    double elevation = 0;

    SolarPosition traditional =
        SPA.calculateSolarPosition(dateTime, latitude, longitude, elevation, deltaT);
    SolarPosition optimized =
        SPA.calculateSolarPositionWithTimeDependentParts(
            latitude, longitude, elevation, timeDependent);

    assertThat(optimized.azimuth()).isCloseTo(traditional.azimuth(), within(TOLERANCE));
    assertThat(optimized.zenithAngle()).isCloseTo(traditional.zenithAngle(), within(TOLERANCE));

    // South Pole
    latitude = -89.9;
    traditional = SPA.calculateSolarPosition(dateTime, latitude, longitude, elevation, deltaT);
    optimized =
        SPA.calculateSolarPositionWithTimeDependentParts(
            latitude, longitude, elevation, timeDependent);

    assertThat(optimized.azimuth()).isCloseTo(traditional.azimuth(), within(TOLERANCE));
    assertThat(optimized.zenithAngle()).isCloseTo(traditional.zenithAngle(), within(TOLERANCE));
  }

  private record Coordinate(double latitude, double longitude, double elevation) {}
}
