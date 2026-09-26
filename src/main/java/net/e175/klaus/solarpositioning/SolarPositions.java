package net.e175.klaus.solarpositioning;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * Calculates topocentric solar positions, using SPA by default.
 *
 * <p>Instances are immutable and reusable. Overloads without height assume sea level; overloads
 * without an atmosphere omit refraction correction. Delta T is always supplied explicitly. Both
 * {@link Instant} and {@link ZonedDateTime} are accepted, using the proleptic Gregorian calendar
 * and UTC as an approximation to UT1.
 *
 * <p>Calculation validity follows the selected model: SPA is designed for years -2000 through 6000,
 * and Grena3 for 2010 through 2110. Use {@link #forTime(ZonedDateTime, double)} to reuse
 * time-dependent calculations across multiple locations.
 */
public final class SolarPositions {
  private enum Algorithm {
    SPA,
    GRENA3
  }

  private final Algorithm algorithm;

  /** Creates a reusable position calculator using SPA. */
  public SolarPositions() {
    this(Algorithm.SPA);
  }

  /** Creates a reusable position calculator using Grena3. */
  public static SolarPositions grena3() {
    return new SolarPositions(Algorithm.GRENA3);
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
   * @throws IllegalArgumentException for invalid coordinates or non-finite delta T
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
   * @throws IllegalArgumentException for invalid coordinates or non-finite delta T
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
   * @throws IllegalArgumentException for invalid coordinates or non-finite delta T
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
   * @throws IllegalArgumentException for invalid coordinates or non-finite delta T
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
   * @throws IllegalArgumentException for invalid coordinates, non-finite height or delta T, or
   *     nonzero height with Grena3
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
   * @throws IllegalArgumentException for invalid coordinates, non-finite height or delta T, or
   *     nonzero height with Grena3
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
   * @throws IllegalArgumentException for invalid coordinates, non-finite height or delta T, or
   *     nonzero height with Grena3
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
   * @throws IllegalArgumentException for invalid coordinates, non-finite height or delta T, or
   *     nonzero height with Grena3
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
   * Prepares positions at one time, reusing the selected model's time-dependent calculations.
   *
   * @param time observer's date and time
   * @param deltaT TT minus UT1 in seconds
   * @return an immutable calculator for locations at this time
   * @throws IllegalArgumentException for non-finite delta T
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
   * @throws IllegalArgumentException for non-finite delta T
   * @throws NullPointerException if time is null
   * @see #forTime(ZonedDateTime, double)
   */
  public AtTime forTime(Instant time, double deltaT) {
    Objects.requireNonNull(time, "time");
    if (!Double.isFinite(deltaT)) {
      throw new IllegalArgumentException("deltaT must be finite");
    }
    var julianDate = new JulianDate(time, deltaT);
    return switch (algorithm) {
      case SPA -> {
        var parts = SPA.calculateSpaTimeDependentParts(julianDate);
        yield new AtTime(
            (latitude, longitude, height, pressure, temperature) ->
                SPA.calculateSolarPositionWithTimeDependentParts(
                    latitude, longitude, height, pressure, temperature, parts));
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
            });
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

    private AtTime(Calculation calculation) {
      this.calculation = calculation;
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
