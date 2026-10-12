package net.e175.klaus.solarpositioning;

import static java.lang.Math.*;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.DoubleUnaryOperator;

/**
 * Calculates topocentric solar positions. Use {@link #spa()} for SPA or {@link #grena3()} for
 * Grena3.
 *
 * <p>Instances are immutable and reusable. Overloads without height assume sea level; overloads
 * without an atmosphere omit refraction correction. Delta T is always supplied explicitly. Both
 * {@link Instant} and {@link ZonedDateTime} are accepted, using the proleptic Gregorian calendar
 * and UTC as an approximation to UT1.
 *
 * <p>SPA supports years -2000 through 6000, and Grena3 supports 2010 through 2110. Both UT and TT
 * must stay within the selected model's range. Use {@link #forTime(ZonedDateTime, double)} to reuse
 * time-dependent calculations across multiple locations.
 */
public final class SolarPositions {
  private enum Algorithm {
    SPA(-2000, 6000),
    GRENA3(2010, 2110);

    private final TimeRange range;

    Algorithm(int firstYear, int lastYear) {
      range = new TimeRange(firstYear, lastYear);
    }
  }

  private static final SolarPositions SPA_INSTANCE = new SolarPositions(Algorithm.SPA);
  private static final SolarPositions GRENA3_INSTANCE = new SolarPositions(Algorithm.GRENA3);

  private final Algorithm algorithm;

  /** Returns the shared, immutable position calculator using SPA. */
  public static SolarPositions spa() {
    return SPA_INSTANCE;
  }

  /** Returns the shared, immutable position calculator using Grena3. */
  public static SolarPositions grena3() {
    return GRENA3_INSTANCE;
  }

  private SolarPositions(Algorithm algorithm) {
    this.algorithm = algorithm;
  }

  /**
   * Calculates a sea-level position without refraction correction.
   *
   * @param time observer's date and time
   * @param latitude latitude in degrees, positive north
   * @param longitude longitude in degrees, positive east
   * @param deltaT TT minus UT1 in seconds
   * @return solar azimuth and zenith angle in degrees
   * @throws IllegalArgumentException for invalid coordinates, non-finite delta T, or a time outside
   *     the model's supported range
   * @throws NullPointerException if time is null
   */
  public SolarPosition at(ZonedDateTime time, double latitude, double longitude, double deltaT) {
    return at(time, latitude, longitude, 0.0, deltaT);
  }

  /**
   * Calculates a sea-level position without refraction correction.
   *
   * @param time instant of observation
   * @param latitude latitude in degrees, positive north
   * @param longitude longitude in degrees, positive east
   * @param deltaT TT minus UT1 in seconds
   * @return solar azimuth and zenith angle in degrees
   * @throws IllegalArgumentException for invalid coordinates, non-finite delta T, or a time outside
   *     the model's supported range
   * @throws NullPointerException if time is null
   * @see #at(ZonedDateTime, double, double, double)
   */
  public SolarPosition at(Instant time, double latitude, double longitude, double deltaT) {
    return forTime(time, deltaT).at(latitude, longitude);
  }

  /**
   * Calculates a sea-level position with atmospheric refraction correction.
   *
   * @param time observer's date and time
   * @param latitude latitude in degrees, positive north
   * @param longitude longitude in degrees, positive east
   * @param deltaT TT minus UT1 in seconds
   * @param atmosphere pressure and temperature for the model's refraction correction
   * @return solar azimuth and corrected zenith angle in degrees
   * @throws IllegalArgumentException for invalid coordinates, non-finite delta T, or a time outside
   *     the model's supported range
   * @throws NullPointerException if time or atmosphere is null
   */
  public SolarPosition at(
      ZonedDateTime time, double latitude, double longitude, double deltaT, Atmosphere atmosphere) {
    return at(time, latitude, longitude, 0.0, deltaT, atmosphere);
  }

  /**
   * Calculates a sea-level position with atmospheric refraction correction.
   *
   * @param time instant of observation
   * @param latitude latitude in degrees, positive north
   * @param longitude longitude in degrees, positive east
   * @param deltaT TT minus UT1 in seconds
   * @param atmosphere pressure and temperature for the model's refraction correction
   * @return solar azimuth and corrected zenith angle in degrees
   * @throws IllegalArgumentException for invalid coordinates, non-finite delta T, or a time outside
   *     the model's supported range
   * @throws NullPointerException if time or atmosphere is null
   * @see #at(ZonedDateTime, double, double, double, Atmosphere)
   */
  public SolarPosition at(
      Instant time, double latitude, double longitude, double deltaT, Atmosphere atmosphere) {
    return forTime(time, deltaT).at(latitude, longitude, atmosphere);
  }

  /**
   * Calculates a position at the observer's height, without refraction correction.
   *
   * @param time observer's date and time
   * @param latitude latitude in degrees, positive north
   * @param longitude longitude in degrees, positive east
   * @param height height above sea level in metres; may be negative, but must be zero for Grena3
   * @param deltaT TT minus UT1 in seconds
   * @return solar azimuth and zenith angle in degrees
   * @throws IllegalArgumentException for invalid coordinates, non-finite height or delta T, nonzero
   *     height with Grena3, or a time outside the model's supported range
   * @throws NullPointerException if time is null
   */
  public SolarPosition at(
      ZonedDateTime time, double latitude, double longitude, double height, double deltaT) {
    return forTime(time, deltaT).at(latitude, longitude, height);
  }

  /**
   * Calculates a position at the observer's height, without refraction correction.
   *
   * @param time instant of observation
   * @param latitude latitude in degrees, positive north
   * @param longitude longitude in degrees, positive east
   * @param height height above sea level in metres; may be negative, but must be zero for Grena3
   * @param deltaT TT minus UT1 in seconds
   * @return solar azimuth and zenith angle in degrees
   * @throws IllegalArgumentException for invalid coordinates, non-finite height or delta T, nonzero
   *     height with Grena3, or a time outside the model's supported range
   * @throws NullPointerException if time is null
   * @see #at(ZonedDateTime, double, double, double, double)
   */
  public SolarPosition at(
      Instant time, double latitude, double longitude, double height, double deltaT) {
    return forTime(time, deltaT).at(latitude, longitude, height);
  }

  /**
   * Calculates a position at the observer's height with atmospheric refraction correction.
   *
   * @param time observer's date and time
   * @param latitude latitude in degrees, positive north
   * @param longitude longitude in degrees, positive east
   * @param height height above sea level in metres; may be negative, but must be zero for Grena3
   * @param deltaT TT minus UT1 in seconds
   * @param atmosphere pressure and temperature for the model's refraction correction
   * @return solar azimuth and corrected zenith angle in degrees
   * @throws IllegalArgumentException for invalid coordinates, non-finite height or delta T, nonzero
   *     height with Grena3, or a time outside the model's supported range
   * @throws NullPointerException if time or atmosphere is null
   */
  public SolarPosition at(
      ZonedDateTime time,
      double latitude,
      double longitude,
      double height,
      double deltaT,
      Atmosphere atmosphere) {
    return forTime(time, deltaT).at(latitude, longitude, height, atmosphere);
  }

  /**
   * Calculates a position at the observer's height with atmospheric refraction correction.
   *
   * @param time instant of observation
   * @param latitude latitude in degrees, positive north
   * @param longitude longitude in degrees, positive east
   * @param height height above sea level in metres; may be negative, but must be zero for Grena3
   * @param deltaT TT minus UT1 in seconds
   * @param atmosphere pressure and temperature for the model's refraction correction
   * @return solar azimuth and corrected zenith angle in degrees
   * @throws IllegalArgumentException for invalid coordinates, non-finite height or delta T, nonzero
   *     height with Grena3, or a time outside the model's supported range
   * @throws NullPointerException if time or atmosphere is null
   * @see #at(ZonedDateTime, double, double, double, double, Atmosphere)
   */
  public SolarPosition at(
      Instant time,
      double latitude,
      double longitude,
      double height,
      double deltaT,
      Atmosphere atmosphere) {
    return forTime(time, deltaT).at(latitude, longitude, height, atmosphere);
  }

  /**
   * Finds the first rising solar-centre elevation crossing using the same observation settings as
   * {@code at}.
   *
   * <p>The angle is geometric without an atmosphere, apparent with one. Refraction cutoff jumps
   * count as crossings; a tangency alone does not. Height affects parallax, not the visible
   * horizon. Searches use the same one-millisecond numerical resolution as {@link SolarEvents}.
   * Passing a returned instant as the next start searches for the following event.
   *
   * @param start exclusive start instant
   * @param end inclusive end instant
   * @param latitude observer latitude in degrees, positive north
   * @param longitude observer longitude in degrees, positive east
   * @param deltaT TT minus UT1 in seconds, held constant throughout the search
   * @param target elevation, observer height and optional atmosphere
   * @return the crossing time, or empty when absent
   * @throws IllegalArgumentException for invalid coordinates, interval or delta T, nonzero height
   *     with Grena3, times outside the model's UT/TT range, or refraction too strong to guarantee a
   *     monotonic elevation mapping
   * @throws NullPointerException if start, end or target is null
   */
  public Optional<Instant> nextElevationRise(
      Instant start,
      Instant end,
      double latitude,
      double longitude,
      double deltaT,
      ElevationTarget target) {
    return elevationCrossing(start, end, latitude, longitude, deltaT, target, true);
  }

  /**
   * Finds the first setting solar-centre elevation crossing using the same observation settings as
   * {@code at}.
   *
   * <p>Uses the same crossing rules and numerical resolution as {@link #nextElevationRise}.
   *
   * @param start exclusive start instant
   * @param end inclusive end instant
   * @param latitude observer latitude in degrees, positive north
   * @param longitude observer longitude in degrees, positive east
   * @param deltaT TT minus UT1 in seconds, held constant throughout the search
   * @param target elevation, observer height and optional atmosphere
   * @return the crossing time, or empty when absent
   * @throws IllegalArgumentException for invalid coordinates, interval or delta T, nonzero height
   *     with Grena3, times outside the model's UT/TT range, or refraction too strong to guarantee a
   *     monotonic elevation mapping
   * @throws NullPointerException if start, end or target is null
   */
  public Optional<Instant> nextElevationSet(
      Instant start,
      Instant end,
      double latitude,
      double longitude,
      double deltaT,
      ElevationTarget target) {
    return elevationCrossing(start, end, latitude, longitude, deltaT, target, false);
  }

  private Optional<Instant> elevationCrossing(
      Instant start,
      Instant end,
      double latitude,
      double longitude,
      double deltaT,
      ElevationTarget target,
      boolean rising) {
    Objects.requireNonNull(target, "target");
    MathUtil.checkLatLonRange(latitude, longitude);
    if (algorithm == Algorithm.GRENA3 && target.height() != 0.0) {
      throw new IllegalArgumentException("Grena3 requires zero observer height");
    }
    SolarEvents.PositionProvider provider =
        switch (algorithm) {
          case SPA ->
              (time, lat, lon) -> SPA.eventPositionAtHeight(time, lat, lon, target.height());
          case GRENA3 -> Grena3::eventPosition;
        };
    var events = new SolarEvents(provider, algorithm.range);
    events.checkInterval(start, end, deltaT);
    var angle = geometricThreshold(target);
    if (angle.isEmpty()) {
      return Optional.empty();
    }
    return rising
        ? events.nextRise(start, end, latitude, longitude, deltaT, angle.orElseThrow())
        : events.nextSet(start, end, latitude, longitude, deltaT, angle.orElseThrow());
  }

  private OptionalDouble geometricThreshold(ElevationTarget target) {
    if (target.atmosphere().isEmpty()) {
      return OptionalDouble.of(target.angle());
    }
    var atmosphere = target.atmosphere().orElseThrow();
    double cutoff = algorithm == Algorithm.SPA ? SPA.REFRACTION_CUTOFF : Grena3.REFRACTION_CUTOFF;
    double slopeBound =
        algorithm == Algorithm.SPA
            ? SPA.refractionSlopeBound(atmosphere)
            : Grena3.refractionSlopeBound(atmosphere);
    if (slopeBound >= 1.0) {
      throw new IllegalArgumentException(
          "refraction too strong to guarantee monotonic elevation crossings");
    }
    if (target.angle() <= cutoff) {
      return OptionalDouble.of(target.angle());
    }
    DoubleUnaryOperator apparent =
        angle ->
            angle
                + switch (algorithm) {
                  case SPA ->
                      SPA.refractionCorrection(
                          angle, atmosphere.pressure(), atmosphere.temperature());
                  case GRENA3 ->
                      toDegrees(
                          Grena3.refractionCorrection(
                              toRadians(angle), atmosphere.pressure(), atmosphere.temperature()));
                };
    if (target.angle() <= apparent.applyAsDouble(cutoff)) {
      return OptionalDouble.of(cutoff);
    }
    if (target.angle() > apparent.applyAsDouble(90.0)) {
      return OptionalDouble.empty();
    }
    double low = cutoff, high = 90.0;
    // Invert only the continuous active branch, once per search. The event search then works
    // with geometric elevation, preserving its continuity and curvature assumptions.
    for (int i = 0; i < 48; i++) {
      double middle = low + (high - low) / 2.0;
      if (apparent.applyAsDouble(middle) < target.angle()) {
        low = middle;
      } else {
        high = middle;
      }
    }
    return OptionalDouble.of(high);
  }

  /**
   * Prepares positions at one time, reusing the selected model's time-dependent calculations.
   *
   * @param time observer's date and time
   * @param deltaT TT minus UT1 in seconds
   * @return an immutable calculator for locations at this time
   * @throws IllegalArgumentException for non-finite delta T or a time outside the model's supported
   *     range
   * @throws NullPointerException if time is null
   */
  public AtTime forTime(ZonedDateTime time, double deltaT) {
    return forTime(time.toInstant(), deltaT);
  }

  /**
   * Prepares positions at one instant, evaluated in UTC.
   *
   * @param time instant of observation
   * @param deltaT TT minus UT1 in seconds
   * @return an immutable calculator for locations at this instant
   * @throws IllegalArgumentException for non-finite delta T or a time outside the model's supported
   *     range
   * @throws NullPointerException if time is null
   * @see #forTime(ZonedDateTime, double)
   */
  public AtTime forTime(Instant time, double deltaT) {
    Objects.requireNonNull(time, "time");
    if (!Double.isFinite(deltaT)) {
      throw new IllegalArgumentException("deltaT must be finite");
    }
    if (!algorithm.range.contains(time, deltaT)) {
      throw new IllegalArgumentException("time outside model's supported years");
    }
    var julianDate = new JulianDate(time, deltaT);
    return switch (algorithm) {
      case SPA -> {
        var parts = SPA.calculateSpaTimeDependentParts(julianDate);
        yield new AtTime(
            (latitude, longitude, height, pressure, temperature) ->
                SPA.calculateSolarPositionWithTimeDependentParts(
                    latitude, longitude, height, pressure, temperature, parts),
            OptionalDouble.of(parts.r()),
            parts.alphaDegrees(),
            parts.deltaDegrees());
      }
      case GRENA3 -> {
        var parts = Grena3.calculateTimeDependentParts(julianDate);
        yield new AtTime(
            (latitude, longitude, height, pressure, temperature) -> {
              if (height != 0.0) {
                throw new IllegalArgumentException("Grena3 requires zero observer height");
              }
              return Grena3.calculateSolarPositionWithTimeDependentParts(
                  latitude, longitude, pressure, temperature, parts);
            },
            OptionalDouble.empty(),
            toDegrees(parts.alpha()),
            toDegrees(asin(parts.sinDelta())));
      }
    };
  }

  @FunctionalInterface
  private interface Calculation {
    SolarPosition at(
        double latitude, double longitude, double height, double pressure, double temperature);
  }

  /**
   * Positions at a fixed time, created by {@link SolarPositions#forTime(ZonedDateTime, double)} or
   * {@link SolarPositions#forTime(Instant, double)}.
   *
   * <p>Instances are immutable, reusable and thread-safe. Height and atmosphere can vary by
   * location.
   */
  public static final class AtTime {
    private final Calculation calculation;
    private final OptionalDouble earthRadiusVector;
    private final double rightAscension;
    private final double declination;

    private AtTime(
        Calculation calculation,
        OptionalDouble earthRadiusVector,
        double rightAscension,
        double declination) {
      this.calculation = calculation;
      this.earthRadiusVector = earthRadiusVector;
      this.rightAscension = rightAscension;
      this.declination = declination;
    }

    /** Returns the Earth–Sun distance in astronomical units for SPA, or empty for Grena3. */
    public OptionalDouble earthRadiusVector() {
      return earthRadiusVector;
    }

    /** Returns the geocentric right ascension in degrees, in [0, 360). */
    public double rightAscension() {
      return rightAscension;
    }

    /** Returns the geocentric declination in degrees, in [-90, 90]. */
    public double declination() {
      return declination;
    }

    /**
     * Calculates a sea-level position without refraction correction.
     *
     * @param latitude latitude in degrees, positive north
     * @param longitude longitude in degrees, positive east
     * @return solar azimuth and zenith angle in degrees
     * @throws IllegalArgumentException for invalid coordinates
     */
    public SolarPosition at(double latitude, double longitude) {
      return at(latitude, longitude, 0.0);
    }

    /**
     * Calculates a sea-level position with atmospheric refraction correction.
     *
     * @param latitude latitude in degrees, positive north
     * @param longitude longitude in degrees, positive east
     * @param atmosphere pressure and temperature for the model's refraction correction
     * @return solar azimuth and corrected zenith angle in degrees
     * @throws IllegalArgumentException for invalid coordinates
     * @throws NullPointerException if atmosphere is null
     */
    public SolarPosition at(double latitude, double longitude, Atmosphere atmosphere) {
      return at(latitude, longitude, 0.0, atmosphere);
    }

    /**
     * Calculates a position at the observer's height, without refraction correction.
     *
     * @param latitude latitude in degrees, positive north
     * @param longitude longitude in degrees, positive east
     * @param height height above sea level in metres; may be negative, but must be zero for Grena3
     * @return solar azimuth and zenith angle in degrees
     * @throws IllegalArgumentException for invalid coordinates, non-finite height, or nonzero
     *     height with Grena3
     */
    public SolarPosition at(double latitude, double longitude, double height) {
      return calculate(latitude, longitude, height, Double.NaN, Double.NaN);
    }

    /**
     * Calculates a position at the observer's height with atmospheric refraction correction.
     *
     * @param latitude latitude in degrees, positive north
     * @param longitude longitude in degrees, positive east
     * @param height height above sea level in metres; may be negative, but must be zero for Grena3
     * @param atmosphere pressure and temperature for the model's refraction correction
     * @return solar azimuth and corrected zenith angle in degrees
     * @throws IllegalArgumentException for invalid coordinates, non-finite height, or nonzero
     *     height with Grena3
     * @throws NullPointerException if atmosphere is null
     */
    public SolarPosition at(
        double latitude, double longitude, double height, Atmosphere atmosphere) {
      Objects.requireNonNull(atmosphere, "atmosphere");
      return calculate(
          latitude, longitude, height, atmosphere.pressure(), atmosphere.temperature());
    }

    private SolarPosition calculate(
        double latitude, double longitude, double height, double pressure, double temperature) {
      if (!Double.isFinite(height)) {
        throw new IllegalArgumentException("height must be finite");
      }
      return calculation.at(latitude, longitude, height, pressure, temperature);
    }
  }
}
