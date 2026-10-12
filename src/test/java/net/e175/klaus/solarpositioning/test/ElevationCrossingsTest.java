package net.e175.klaus.solarpositioning.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import net.e175.klaus.solarpositioning.Atmosphere;
import net.e175.klaus.solarpositioning.ElevationTarget;
import net.e175.klaus.solarpositioning.SolarEvents;
import net.e175.klaus.solarpositioning.SolarPositions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ElevationCrossingsTest {
  private static final double DELTA_T = 69.184, LATITUDE = 52.0, LONGITUDE = 13.4;
  private static final Atmosphere ATMOSPHERE = new Atmosphere(1013.25, 15);
  private static final Instant START = Instant.parse("2024-03-20T00:00:00Z");
  private static final Instant END = START.plus(Duration.ofDays(1));

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void crossingsMatchPositionsAcrossSettingsAndSeasons(boolean grena) {
    var positions = grena ? SolarPositions.grena3() : SolarPositions.spa();
    var atmospheres =
        List.of(
            Optional.<Atmosphere>empty(),
            Optional.of(ATMOSPHERE),
            Optional.of(new Atmosphere(850, -10)),
            Optional.of(new Atmosphere(1100, -70)));
    for (int month : new int[] {1, 3, 6}) {
      var start = LocalDate.of(2024, month, 21).atStartOfDay(ZoneOffset.UTC).toInstant();
      var end = start.plus(Duration.ofDays(1));
      for (double latitude : new double[] {-35, 0, 52}) {
        for (double height : grena ? new double[] {0} : new double[] {0, 2500}) {
          for (var atmosphere : atmospheres) {
            for (double angle : new double[] {-10, 0, 10, 40}) {
              var target = new ElevationTarget(angle, height, atmosphere);
              for (boolean rising : new boolean[] {true, false}) {
                var event = crossing(positions, rising, start, end, latitude, target);
                String context =
                    "grena="
                        + grena
                        + ", latitude="
                        + latitude
                        + ", "
                        + start
                        + ", "
                        + target
                        + ", rising="
                        + rising;
                if (event.isPresent()) {
                  var time = event.orElseThrow();
                  assertThat(time).isAfter(start).isBeforeOrEqualTo(end);
                  assertTransition(positions, time, latitude, target, rising, 100_000_000, context);
                } else {
                  double before = elevation(positions, start, latitude, target);
                  for (int i = 1; i <= 96; i++) {
                    double after =
                        elevation(positions, start.plusSeconds(i * 900L), latitude, target);
                    assertThat(crosses(before, after, angle, rising)).as(context).isFalse();
                    before = after;
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  @Test
  void spaCrossingsUseObserverHeight() {
    var positions = SolarPositions.spa();
    var start = Instant.parse("2024-12-21T00:00:00Z");
    var target = new ElevationTarget(0, 5000);
    // Shallow winter crossings make the height effect exceed the 1 ms search resolution.
    for (boolean rising : new boolean[] {true, false}) {
      var event =
          crossing(positions, rising, start, start.plus(Duration.ofDays(1)), 63, target)
              .orElseThrow();
      assertTransition(positions, event, 63, target, rising, 1_100_000, "height-adjusted crossing");
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void geometricSeaLevelCrossingsMatchStandardEventsAndResume(boolean grena) {
    var positions = grena ? SolarPositions.grena3() : SolarPositions.spa();
    var events = grena ? SolarEvents.grena3() : SolarEvents.spa();
    for (double angle : new double[] {-18, 0, 10}) {
      var target = new ElevationTarget(angle, 0);
      for (boolean rising : new boolean[] {true, false}) {
        var event = crossing(positions, rising, START, END, LATITUDE, target).orElseThrow();
        var standard =
            rising
                ? events.nextRise(START, END, LATITUDE, LONGITUDE, DELTA_T, angle)
                : events.nextSet(START, END, LATITUDE, LONGITUDE, DELTA_T, angle);
        assertThat(event).isEqualTo(standard.orElseThrow());
        assertThat(crossing(positions, rising, event, END, LATITUDE, target)).isEmpty();
        var bounded = crossing(positions, rising, START, event, LATITUDE, target).orElseThrow();
        assertThat(Duration.between(event, bounded).abs()).isLessThan(Duration.ofNanos(1_100_000));
      }
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void refractionCutoffJumpsCountAsCrossings(boolean grena) {
    var positions = grena ? SolarPositions.grena3() : SolarPositions.spa();
    var events = grena ? SolarEvents.grena3() : SolarEvents.spa();
    double cutoff = grena ? 0 : -0.83337;
    var target = new ElevationTarget(grena ? 0.2 : -0.5, 0, ATMOSPHERE);
    for (boolean rising : new boolean[] {true, false}) {
      var event = crossing(positions, rising, START, END, LATITUDE, target).orElseThrow();
      var standard =
          rising
              ? events.nextRise(START, END, LATITUDE, LONGITUDE, DELTA_T, cutoff)
              : events.nextSet(START, END, LATITUDE, LONGITUDE, DELTA_T, cutoff);
      assertThat(event).isEqualTo(standard.orElseThrow());
      assertTransition(positions, event, LATITUDE, target, rising, 100_000_000, "cutoff crossing");
    }
  }

  @Test
  void invalidTargetsAreRejectedAtConstruction() {
    for (double angle : new double[] {Double.NaN, Double.POSITIVE_INFINITY, -91, 91}) {
      assertThrows(IllegalArgumentException.class, () -> new ElevationTarget(angle, 0));
    }
    for (double height :
        new double[] {Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY}) {
      assertThrows(IllegalArgumentException.class, () -> new ElevationTarget(0, height));
    }
    assertThrows(NullPointerException.class, () -> new ElevationTarget(0, 0, (Atmosphere) null));
    assertThrows(
        NullPointerException.class, () -> new ElevationTarget(0, 0, (Optional<Atmosphere>) null));
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void missingCrossingsAndInvalidQueriesRemainDistinct(boolean grena) {
    var positions = grena ? SolarPositions.grena3() : SolarPositions.spa();
    var target = new ElevationTarget(0, 0);
    var summer = Instant.parse("2024-06-21T00:00:00Z");
    var unreachable = new ElevationTarget(90, 0, ATMOSPHERE);
    var excessive = new ElevationTarget(0, 0, new Atmosphere(2000, -250));
    for (boolean rising : new boolean[] {true, false}) {
      assertThat(crossing(positions, rising, summer, summer.plus(Duration.ofDays(1)), 90, target))
          .isEmpty();
      assertThat(crossing(positions, rising, START, START, LATITUDE, target)).isEmpty();
      assertThat(crossing(positions, rising, START, END, LATITUDE, unreachable)).isEmpty();
      assertThrows(
          IllegalArgumentException.class,
          () -> crossing(positions, rising, END, START, LATITUDE, unreachable));
      assertThrows(
          IllegalArgumentException.class,
          () -> crossing(positions, rising, START, END, 91, unreachable));
      assertThrows(
          IllegalArgumentException.class,
          () -> crossing(positions, rising, Instant.MIN, END, LATITUDE, unreachable));
      assertThrows(
          IllegalArgumentException.class,
          () -> crossing(positions, rising, START, END, LATITUDE, excessive));
      assertThrows(
          NullPointerException.class,
          () -> crossing(positions, rising, START, END, LATITUDE, null));
    }
    assertThrows(
        IllegalArgumentException.class,
        () -> positions.nextElevationRise(START, END, LATITUDE, LONGITUDE, Double.NaN, target));
    assertThrows(
        IllegalArgumentException.class,
        () -> positions.nextElevationSet(START, END, LATITUDE, 181, DELTA_T, target));
    if (grena) {
      assertThrows(
          IllegalArgumentException.class,
          () ->
              positions.nextElevationRise(
                  START, END, LATITUDE, LONGITUDE, DELTA_T, new ElevationTarget(0, 1)));
    }
  }

  private static Optional<Instant> crossing(
      SolarPositions positions,
      boolean rising,
      Instant start,
      Instant end,
      double latitude,
      ElevationTarget target) {
    return rising
        ? positions.nextElevationRise(start, end, latitude, LONGITUDE, DELTA_T, target)
        : positions.nextElevationSet(start, end, latitude, LONGITUDE, DELTA_T, target);
  }

  private static double elevation(
      SolarPositions positions, Instant time, double latitude, ElevationTarget target) {
    var position =
        target.atmosphere().isPresent()
            ? positions.at(
                time,
                latitude,
                LONGITUDE,
                target.height(),
                DELTA_T,
                target.atmosphere().orElseThrow())
            : positions.at(time, latitude, LONGITUDE, target.height(), DELTA_T);
    return position.elevation();
  }

  private static boolean crosses(double before, double after, double angle, boolean rising) {
    return rising ? before < angle && after > angle : before > angle && after < angle;
  }

  private static void assertTransition(
      SolarPositions positions,
      Instant time,
      double latitude,
      ElevationTarget target,
      boolean rising,
      long marginNanos,
      String context) {
    double before = elevation(positions, time.minusNanos(marginNanos), latitude, target);
    double after = elevation(positions, time.plusNanos(marginNanos), latitude, target);
    assertThat(crosses(before, after, target.angle(), rising))
        .as("%s: %s -> %s", context, before, after)
        .isTrue();
  }
}
