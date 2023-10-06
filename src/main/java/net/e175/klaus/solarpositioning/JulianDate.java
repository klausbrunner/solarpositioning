package net.e175.klaus.solarpositioning;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

/**
 * Calculate Julian date for a given point in time. This follows the algorithm described in Reda,
 * I.; Andreas, A. (2003): Solar Position Algorithm for Solar Radiation Applications. NREL Report
 * No. TP-560-34302, Revised January 2008.
 *
 * @author Klaus Brunner
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
    this(
        calcJulianDate(
            createUtcDateTime(date).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime()),
        deltaT);
  }

  static ZonedDateTime createUtcDateTime(final ZonedDateTime fromDateTime) {
    return fromDateTime.withZoneSameInstant(ZoneOffset.UTC);
  }

  private static double calcJulianDate(LocalDateTime localDateTime) {
    int y = localDateTime.getYear();
    int m = localDateTime.getMonthValue();

    if (m < 3) {
      y = y - 1;
      m = m + 12;
    }

    final double d =
        localDateTime.getDayOfMonth()
            + (localDateTime.getHour()
                    + (localDateTime.getMinute() + localDateTime.getSecond() / 60.0) / 60.0)
                / 24.0;
    final double jd =
        Math.floor(365.25 * (y + 4716.0)) + Math.floor(30.6001 * (m + 1)) + d - 1524.5;
    final double a = Math.floor(y / 100.0);
    final double b = jd > 2299160.0 ? (2.0 - a + Math.floor(a / 4.0)) : 0.0;

    return jd + b;
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
