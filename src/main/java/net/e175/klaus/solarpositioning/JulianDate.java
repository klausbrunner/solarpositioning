package net.e175.klaus.solarpositioning;

import java.time.Instant;
import java.time.ZonedDateTime;

/**
 * Continuous UT Julian date and TT minus UT1 (delta T), in seconds.
 *
 * <p>Java timestamps use the proleptic Gregorian calendar, including before 1582. UTC approximates
 * UT1. Ephemeris time includes delta T.
 */
public record JulianDate(double julianDate, double deltaT) {
  /**
   * Construct a Julian date, assuming deltaT to be 0.
   *
   * @param date date and time
   */
  public JulianDate(final ZonedDateTime date) {
    this(date, 0.0);
  }

  /**
   * Construct a Julian date, observing deltaT.
   *
   * @param date date and time
   * @param deltaT Difference between earth rotation time and terrestrial time (or Universal Time
   *     and Terrestrial Time), in seconds. See <a
   *     href="https://maia.usno.navy.mil/products/deltaT">https://maia.usno.navy.mil/products/deltaT</a>.
   *     For the years 2023–2028, a reasonably accurate default would be 69.
   */
  public JulianDate(final ZonedDateTime date, final double deltaT) {
    this(date.toInstant(), deltaT);
  }

  /**
   * Constructs a Julian date from an instant, assuming delta T is zero.
   *
   * @param time instant of observation
   */
  public JulianDate(Instant time) {
    this(time, 0.0);
  }

  /**
   * Constructs a Julian date from an instant and delta T.
   *
   * @param time instant of observation; UTC approximates UT1
   * @param deltaT TT minus UT1, in seconds
   */
  public JulianDate(Instant time, double deltaT) {
    this(toJulianDate(time), deltaT);
  }

  static double toJulianDate(Instant time) {
    return 2440587.5 + time.getEpochSecond() / 86400.0 + time.getNano() / 86400e9;
  }

  public double julianEphemerisDay() {
    return julianDate + deltaT / 86400.0;
  }

  public double julianCentury() {
    return (julianDate - 2451545.0) / 36525.0;
  }

  public double julianEphemerisCentury() {
    return (julianEphemerisDay() - 2451545.0) / 36525.0;
  }

  public double julianEphemerisMillennium() {
    return julianEphemerisCentury() / 10.0;
  }
}
