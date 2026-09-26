package net.e175.klaus.solarpositioning;

import static java.lang.Math.*;
import static net.e175.klaus.solarpositioning.JulianDate.toJulianDate;
import static net.e175.klaus.solarpositioning.MathUtil.*;

import java.time.*;
import java.util.*;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;

/**
 * Searches for solar events using a position provider, defaulting to SPA.
 *
 * <p>The {@code nextRise}, {@code nextSet} and {@code nextTransit} searches run forwards, excluding
 * {@code start} and including {@code end}. An empty result means the requested crossing does not
 * occur in that interval. Passing a returned instant as the next start searches for the following
 * event. {@code forDate} collects all events in a local calendar date, including its start and
 * excluding the following date. Rise and set are independent of transit.
 *
 * <p>Horizon crossings use the unrefracted, topocentric solar centre at sea level. The standard
 * sunrise horizon includes the conventional allowance for refraction and solar radius; twilight and
 * custom horizons use their specified geometric elevation. Terrain and weather are not modelled.
 * Numerical crossing brackets are refined to one millisecond; this is not a claim of equivalent
 * accuracy for observed events, especially near a grazing horizon. Assignment to dates likewise has
 * one-millisecond resolution.
 *
 * <p>Instants use Java's continuous, proleptic Gregorian time line, including before 1582. As in
 * the position API, UTC approximates UT1, and {@code deltaT} is TT minus UT1 in seconds. It is held
 * constant during each search. SPA supports years -2000 through 6000; Grena3 supports 2010 through
 * 2110. Both UT and TT must stay within the selected provider's range.
 */
public final class SolarEvents {
  private static final double HOURS_PER_SECOND = 1.0 / 3600.0;
  private static final double TIME_TOLERANCE = HOURS_PER_SECOND / 1000.0;
  // Conservative allowance for Julian-date quantisation and floating-point evaluation.
  private static final double ROUNDING_ERROR = 1e-8;

  private final PositionProvider provider;
  private final Instant minTime;
  private final Instant maxTime;

  /** Creates a reusable event calculator using SPA positions. */
  public SolarEvents() {
    this(SPA::eventPosition, -2000, 6000);
  }

  /** Creates a reusable event calculator using Grena3 positions, for years 2010 through 2110. */
  public static SolarEvents grena3() {
    return new SolarEvents(Grena3::eventPosition, 2010, 2110);
  }

  /**
   * Creates an event calculator with a custom solar-position model.
   *
   * @param provider position calculation; must satisfy {@link PositionProvider}'s contract
   * @param firstYear first supported year, inclusive, no earlier than -2000
   * @param lastYear last supported year, inclusive, no later than 6000
   */
  public SolarEvents(PositionProvider provider, int firstYear, int lastYear) {
    this.provider = Objects.requireNonNull(provider);
    if (firstYear < -2000 || lastYear > 6000 || lastYear < firstYear) {
      throw new IllegalArgumentException("invalid provider year range");
    }
    minTime = LocalDate.of(firstYear, 1, 1).atStartOfDay(ZoneOffset.UTC).toInstant();
    maxTime = LocalDate.of(lastYear + 1, 1, 1).atStartOfDay(ZoneOffset.UTC).toInstant();
  }

  /**
   * Supplies unrefracted, sea-level solar positions. Julian dates are continuous UT, with TT
   * available via {@link JulianDate#julianEphemerisDay()}. Longitude is positive east.
   *
   * <p>This is a solar-model extension, not an arbitrary moving-object search. The sines of
   * elevation and hour angle must be continuous and have absolute second derivatives (in hours)
   * below {@code 0.1 * abs(cos(latitude)) + 0.0001} and {@code 0.1}, respectively. Hour angle is
   * independent of latitude and increases westwards through zero at upper transit. Results must be
   * deterministic; a provider must also be thread-safe if its calculator is shared between threads.
   */
  @FunctionalInterface
  public interface PositionProvider {
    Position position(JulianDate time, double latitude, double longitude);
  }

  /**
   * Coordinates needed by the search, in degrees.
   *
   * @param elevation unrefracted topocentric elevation of the solar centre at sea level
   * @param hourAngle geocentric local hour angle, positive west; any full-turn wrapping is accepted
   */
  public record Position(double elevation, double hourAngle) {
    public Position {
      if (!Double.isFinite(elevation) || !Double.isFinite(hourAngle)) {
        throw new IllegalArgumentException("non-finite solar position");
      }
    }
  }

  /** Conventional geometric elevations of the solar centre, in degrees. */
  public enum Horizon {
    SUNRISE_SUNSET(-50.0 / 60.0),
    CIVIL_TWILIGHT(-6.0),
    NAUTICAL_TWILIGHT(-12.0),
    ASTRONOMICAL_TWILIGHT(-18.0);

    private final double elevation;

    Horizon(double elevation) {
      this.elevation = elevation;
    }

    public double elevation() {
      return elevation;
    }
  }

  /** The solar centre's position relative to the selected horizon at the start of a date. */
  public enum HorizonState {
    ABOVE,
    BELOW,
    /** Within the numerical rounding allowance of the horizon. */
    ON_HORIZON
  }

  /**
   * All events in a local date, with inclusive start and exclusive end. Lists are immutable,
   * chronological, and use the requested time zone. A date may contain zero or multiple events of
   * each kind. A skipped calendar date has equal start and end and empty lists.
   *
   * @param start beginning of the local date
   * @param end beginning of the following local date
   * @param stateAtStart solar-centre state relative to the selected horizon at start
   * @param rises rising crossings
   * @param transits upper meridian transits, independent of the horizon
   * @param sets setting crossings
   */
  public record Day(
      ZonedDateTime start,
      ZonedDateTime end,
      HorizonState stateAtStart,
      List<ZonedDateTime> rises,
      List<ZonedDateTime> transits,
      List<ZonedDateTime> sets) {
    public Day {
      Objects.requireNonNull(start);
      Objects.requireNonNull(end);
      Objects.requireNonNull(stateAtStart);
      rises = List.copyOf(rises);
      transits = List.copyOf(transits);
      sets = List.copyOf(sets);
    }

    /** No horizon crossings and a start above the horizon; false for an empty date. */
    public boolean alwaysAbove() {
      return end.isAfter(start)
          && stateAtStart == HorizonState.ABOVE
          && rises.isEmpty()
          && sets.isEmpty();
    }

    /** No horizon crossings and a start below the horizon; false for an empty date. */
    public boolean alwaysBelow() {
      return end.isAfter(start)
          && stateAtStart == HorizonState.BELOW
          && rises.isEmpty()
          && sets.isEmpty();
    }
  }

  /**
   * Calculates standard sunrise, sunset and transit for a local date.
   *
   * @param date local calendar date
   * @param zone time zone defining the date and returned times
   * @param latitude observer latitude in degrees, positive north
   * @param longitude observer longitude in degrees, positive east
   * @param deltaT TT minus UT1 in seconds, held constant during the date
   * @return all events and initial horizon state
   */
  public Day forDate(
      LocalDate date, ZoneId zone, double latitude, double longitude, double deltaT) {
    return forDate(date, zone, latitude, longitude, deltaT, Horizon.SUNRISE_SUNSET);
  }

  /**
   * Calculates events for one standard horizon and a local date.
   *
   * @param date local calendar date
   * @param zone time zone defining the date and returned times
   * @param latitude observer latitude in degrees, positive north
   * @param longitude observer longitude in degrees, positive east
   * @param deltaT TT minus UT1 in seconds
   * @param horizon selected sunrise or twilight horizon
   * @return all events and initial horizon state
   */
  public Day forDate(
      LocalDate date,
      ZoneId zone,
      double latitude,
      double longitude,
      double deltaT,
      Horizon horizon) {
    return forDate(date, zone, latitude, longitude, deltaT, horizon.elevation());
  }

  /**
   * Calculates events for one custom geometric elevation and a local date.
   *
   * @param date local calendar date
   * @param zone time zone defining the date and returned times
   * @param latitude observer latitude in degrees, positive north
   * @param longitude observer longitude in degrees, positive east
   * @param deltaT TT minus UT1 in seconds
   * @param elevationAngle solar-centre elevation in degrees, from -90 to 90
   * @return all events and initial horizon state
   */
  public Day forDate(
      LocalDate date,
      ZoneId zone,
      double latitude,
      double longitude,
      double deltaT,
      double elevationAngle) {
    return forDateMultiple(date, zone, latitude, longitude, deltaT, elevationAngle)
        .get(elevationAngle);
  }

  /**
   * Calculates a local date for several standard horizons, sharing the transit search.
   *
   * @param date local calendar date
   * @param zone time zone defining the date and returned times
   * @param latitude observer latitude in degrees, positive north
   * @param longitude observer longitude in degrees, positive east
   * @param deltaT TT minus UT1 in seconds
   * @param horizons selected sunrise or twilight horizons; duplicates are ignored
   * @return an immutable map, one result per distinct horizon
   */
  public Map<Horizon, Day> forDateMultiple(
      LocalDate date,
      ZoneId zone,
      double latitude,
      double longitude,
      double deltaT,
      Horizon... horizons) {
    var days =
        forDateMultiple(
            date,
            zone,
            latitude,
            longitude,
            deltaT,
            Arrays.stream(horizons).mapToDouble(Horizon::elevation).toArray());
    Map<Horizon, Day> result = new EnumMap<>(Horizon.class);
    for (Horizon horizon : horizons) result.put(horizon, days.get(horizon.elevation()));
    return Map.copyOf(result);
  }

  /**
   * Calculates a local date for several custom elevations, sharing the transit search.
   *
   * @param date local calendar date
   * @param zone time zone defining the date and returned times
   * @param latitude observer latitude in degrees, positive north
   * @param longitude observer longitude in degrees, positive east
   * @param deltaT TT minus UT1 in seconds
   * @param elevationAngles geometric solar-centre elevations, from -90 to 90; duplicates are
   *     ignored
   * @return an immutable map, one result per distinct elevation
   */
  public Map<Double, Day> forDateMultiple(
      LocalDate date,
      ZoneId zone,
      double latitude,
      double longitude,
      double deltaT,
      double... elevationAngles) {
    checkLatLonRange(latitude, longitude);
    for (double elevation : elevationAngles) checkElevationAngle(elevation);
    ZonedDateTime start = date.atStartOfDay(zone), end = date.plusDays(1).atStartOfDay(zone);
    checkInterval(start.toInstant(), end.toInstant(), deltaT);
    var transits =
        collect(start, end, deltaT, t -> nextTransit(t, end.toInstant(), longitude, deltaT));
    double initialAltitude =
        sin(
            toRadians(
                position(toJulianDate(start.toInstant()), latitude, longitude, deltaT)
                    .elevation()));
    Map<Double, Day> result = new HashMap<>();
    for (double elevation : elevationAngles) {
      result.computeIfAbsent(
          elevation,
          h -> {
            double initial = initialAltitude - sin(toRadians(h));
            HorizonState state =
                abs(initial) <= ROUNDING_ERROR
                    ? HorizonState.ON_HORIZON
                    : initial > 0 ? HorizonState.ABOVE : HorizonState.BELOW;
            return new Day(
                start,
                end,
                state,
                collect(
                    start,
                    end,
                    deltaT,
                    t -> nextRise(t, end.toInstant(), latitude, longitude, deltaT, h)),
                transits,
                collect(
                    start,
                    end,
                    deltaT,
                    t -> nextSet(t, end.toInstant(), latitude, longitude, deltaT, h)));
          });
    }
    return Map.copyOf(result);
  }

  private List<ZonedDateTime> collect(
      ZonedDateTime start,
      ZonedDateTime end,
      double deltaT,
      Function<Instant, Optional<Instant>> next) {
    // Include a boundary crossing to the search's resolution, then assign events by their
    // returned timestamps to [start, end). Keep the lookback within the provider's UT/TT range.
    Instant cursor = start.toInstant().minusMillis(1);
    if (beforeRange(cursor, deltaT)) cursor = start.toInstant();
    List<ZonedDateTime> result = new ArrayList<>();
    while (true) {
      var event = next.apply(cursor);
      if (event.isEmpty() || !event.orElseThrow().isBefore(end.toInstant())) break;
      cursor = event.orElseThrow();
      if (!cursor.isBefore(start.toInstant())) result.add(cursor.atZone(start.getZone()));
    }
    return List.copyOf(result);
  }

  /**
   * Finds the first rising crossing of a standard horizon.
   *
   * @param start exclusive start instant
   * @param end inclusive end instant
   * @param latitude observer latitude in degrees, positive north
   * @param longitude observer longitude in degrees, positive east
   * @param deltaT TT minus UT1 in seconds
   * @param horizon selected sunrise or twilight horizon
   * @return the crossing time, or empty when absent
   */
  public Optional<Instant> nextRise(
      Instant start,
      Instant end,
      double latitude,
      double longitude,
      double deltaT,
      Horizon horizon) {
    return nextRise(start, end, latitude, longitude, deltaT, horizon.elevation());
  }

  /**
   * Finds the first setting crossing of a standard horizon.
   *
   * @param start exclusive start instant
   * @param end inclusive end instant
   * @param latitude observer latitude in degrees, positive north
   * @param longitude observer longitude in degrees, positive east
   * @param deltaT TT minus UT1 in seconds
   * @param horizon selected sunrise or twilight horizon
   * @return the crossing time, or empty when absent
   */
  public Optional<Instant> nextSet(
      Instant start,
      Instant end,
      double latitude,
      double longitude,
      double deltaT,
      Horizon horizon) {
    return nextSet(start, end, latitude, longitude, deltaT, horizon.elevation());
  }

  /**
   * Finds the first rising crossing of a custom geometric elevation, without adding refraction.
   *
   * @param start exclusive start instant
   * @param end inclusive end instant
   * @param latitude observer latitude in degrees, positive north
   * @param longitude observer longitude in degrees, positive east
   * @param deltaT TT minus UT1 in seconds
   * @param elevationAngle solar-centre elevation in degrees, from -90 to 90
   * @return the crossing time, or empty when absent; a tangency alone is not a crossing
   */
  public Optional<Instant> nextRise(
      Instant start,
      Instant end,
      double latitude,
      double longitude,
      double deltaT,
      double elevationAngle) {
    return nextCrossing(start, end, latitude, longitude, deltaT, elevationAngle, 1.0);
  }

  /**
   * Finds the first setting crossing of a custom geometric elevation, without adding refraction.
   *
   * @param start exclusive start instant
   * @param end inclusive end instant
   * @param latitude observer latitude in degrees, positive north
   * @param longitude observer longitude in degrees, positive east
   * @param deltaT TT minus UT1 in seconds
   * @param elevationAngle solar-centre elevation in degrees, from -90 to 90
   * @return the crossing time, or empty when absent; a tangency alone is not a crossing
   */
  public Optional<Instant> nextSet(
      Instant start,
      Instant end,
      double latitude,
      double longitude,
      double deltaT,
      double elevationAngle) {
    return nextCrossing(start, end, latitude, longitude, deltaT, elevationAngle, -1.0);
  }

  /**
   * Finds the first upper meridian transit (solar noon). This is not necessarily the instant of
   * maximum elevation. Transit is independent of the observer's latitude and selected horizon.
   *
   * @param start exclusive start instant
   * @param end inclusive end instant
   * @param longitude observer longitude in degrees, positive east
   * @param deltaT TT minus UT1 in seconds
   * @return the transit time, or empty when absent
   */
  public Optional<Instant> nextTransit(
      Instant start, Instant end, double longitude, double deltaT) {
    checkLatLonRange(0.0, longitude);
    return search(
        start,
        end,
        deltaT,
        0.1,
        jd -> sin(toRadians(position(jd, 0.0, longitude, deltaT).hourAngle())));
  }

  private Optional<Instant> nextCrossing(
      Instant start,
      Instant end,
      double latitude,
      double longitude,
      double deltaT,
      double elevationAngle,
      double direction) {
    checkLatLonRange(latitude, longitude);
    checkElevationAngle(elevationAngle);
    double horizon = sin(toRadians(elevationAngle));
    // Engineering estimate for |f''| in hours: daily rotation contributes about
    // (2*pi/24)^2 * cos(latitude) = 0.069 * cos(latitude). Round up to 0.1 and
    // retain a floor for slower solar motion and parallax, including at the poles.
    double curvature = 0.1 * abs(cos(toRadians(latitude))) + 0.0001;
    return search(
        start,
        end,
        deltaT,
        curvature,
        jd ->
            direction
                * (sin(toRadians(position(jd, latitude, longitude, deltaT).elevation()))
                    - horizon));
  }

  private Position position(double jd, double latitude, double longitude, double deltaT) {
    return provider.position(new JulianDate(jd, deltaT), latitude, longitude);
  }

  private Optional<Instant> search(
      Instant start, Instant end, double deltaT, double curvature, DoubleUnaryOperator position) {
    checkInterval(start, end, deltaT);
    Duration duration = Duration.between(start, end);
    double hours = (duration.getSeconds() + duration.getNano() / 1e9) * HOURS_PER_SECOND;
    if (hours == 0.0) {
      return Optional.empty();
    }
    // Evaluate the exact Instant we return, so rounding cannot rediscover the same crossing
    // when the caller uses it as the next search's start.
    var search =
        new CrossingSearch(
            t -> position.applyAsDouble(toJulianDate(at(start, end, t, hours))), curvature);
    double crossing = search.find(0.0, hours, search.value(0.0), search.value(hours));
    if (Double.isNaN(crossing)) {
      return Optional.empty();
    }
    return Optional.of(at(start, end, crossing, hours));
  }

  private void checkInterval(Instant start, Instant end, double deltaT) {
    if (!Double.isFinite(deltaT) || end.isBefore(start)) {
      throw new IllegalArgumentException("invalid search interval or deltaT");
    }
    if (beforeRange(start, deltaT)
        || end.isAfter(maxTime)
        || toJulianDate(end) + deltaT / 86400.0 > toJulianDate(maxTime)) {
      throw new IllegalArgumentException("search interval outside provider's supported years");
    }
  }

  private boolean beforeRange(Instant time, double deltaT) {
    return time.isBefore(minTime) || toJulianDate(time) + deltaT / 86400.0 < toJulianDate(minTime);
  }

  private static Instant at(Instant start, Instant end, double hours, double durationHours) {
    // Preserve the inclusive endpoint exactly; a double round-trip can move it into the prior date.
    if (hours == durationHours) return end;
    double seconds = hours * 3600.0;
    long wholeSeconds = (long) seconds;
    Instant time =
        start.plusSeconds(wholeSeconds).plusNanos((long) ((seconds - wholeSeconds) * 1e9));
    return time.isAfter(end) ? end : time;
  }

  private record CrossingSearch(DoubleUnaryOperator function, double curvature) {
    double value(double time) {
      return function.applyAsDouble(time);
    }

    double find(double start, double end, double a, double b) {
      double width = end - start;
      double slope = (b - a) / width;
      double slopeError = curvature * width / 2.0 + 2.0 * ROUNDING_ERROR / width;
      // Standard interpolation bounds, using the conservative curvature estimate above:
      // slope error M*w/2, chord error M*w*w/8 (https://dlmf.nist.gov/3.3.E5).
      // Discard only intervals that cannot rise through zero; otherwise search left first.
      if (slope + slopeError <= 0.0
          || ((a > 0.0) == (b > 0.0)
              && min(abs(a), abs(b)) > curvature * width * width / 8.0 + ROUNDING_ERROR)) {
        return Double.NaN;
      }
      if (a < 0.0 && b >= 0.0 && slope > slopeError) {
        return refine(start, end, a, b);
      }
      if (width <= TIME_TOLERANCE) {
        return a < 0.0 && b > 0.0 ? end : Double.NaN;
      }
      double middle = start + width / 2.0;
      double m = value(middle);
      double left = find(start, middle, a, m);
      return Double.isNaN(left) ? find(middle, end, m, b) : left;
    }

    double refine(double start, double end, double a, double b) {
      // ITP (Oliveira & Takahashi): https://doi.org/10.1145/3423597.
      // Use k1 = 0.2 / initial width, k2 = 2, n0 = 1. The projection allows
      // at most one extra iteration over bisection while favouring interpolation.
      double scale = 0.2 / (end - start);
      // Maximum remaining width after a step; halve the allowance each time.
      double maxWidth = TIME_TOLERANCE;
      while (maxWidth < end - start) {
        maxWidth *= 2.0;
      }
      while (end - start > TIME_TOLERANCE) {
        double width = end - start;
        double middle = start + width / 2.0;
        // Equal values can occur on a rounded zero; use the midpoint then.
        double interpolated = a == b ? middle : start - a * width / (b - a);
        double towardsMiddle = middle - interpolated;
        double truncated =
            interpolated + copySign(min(scale * width * width, abs(towardsMiddle)), towardsMiddle);
        double radius = max(0.0, maxWidth - width / 2.0);
        double trial = middle + max(-radius, min(radius, truncated - middle));
        double value = value(trial);
        if (value > 0.0) {
          end = trial;
          b = value;
        } else {
          start = trial;
          a = value;
        }
        maxWidth /= 2.0;
      }
      // Keep the later endpoint, so the next search cannot rediscover this crossing.
      return end;
    }
  }
}
