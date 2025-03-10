package net.e175.klaus.solarpositioning;

import static net.e175.klaus.solarpositioning.MathUtil.polynomial;

import java.time.LocalDate;

/**
 * Estimate values for Delta T, the difference between Terrestrial Time (TT) and Universal Time
 * (UT1).
 */
public final class DeltaT {
  private DeltaT() {}

  /**
   * Estimate Delta T for the given date. This is based on Espenak and Meeus, "Five Millennium Canon
   * of Solar Eclipses: -1999 to +3000" (NASA/TP-2006-214141) and updated by Espenak in 2014 at <a
   * href="https://www.eclipsewise.com/help/deltatpoly2014.html">Eclipsewise</a>.
   *
   * @param forDate date
   * @return estimated delta T value (seconds)
   */
  public static double estimate(final LocalDate forDate) {
    final double year = decimalYear(forDate);
    return estimate(year);
  }

  /**
   * Estimate Delta T for the given decimal year.
   *
   * @param year decimal year (e.g. 2024.5 for the middle of 2024)
   * @return estimated delta T value (seconds)
   * @see #decimalYear(LocalDate)
   */
  public static double estimate(final double year) {
    final double deltaT;

    if (year < -500) {
      double u = (year - 1820) / 100.0;
      deltaT = polynomial(u, -20, 0, 32);
    } else if (year < 500) {
      double u = year / 100.0;
      deltaT =
          polynomial(
              u, 10583.6, -1014.41, 33.78311, -5.952053, -0.1798452, 0.022174192, 0.0090316521);
    } else if (year < 1600) {
      double u = (year - 1000) / 100.0;
      deltaT =
          polynomial(
              u, 1574.2, -556.01, 71.23472, 0.319781, -0.8503463, -0.005050998, 0.0083572073);
    } else if (year < 1700) {
      double t = year - 1600;
      deltaT = polynomial(t, 120, -0.9808, -0.01532, 1.0 / 7129);
    } else if (year < 1800) {
      double t = year - 1700;
      deltaT = polynomial(t, 8.83, 0.1603, -0.0059285, 0.00013336, -1.0 / 1174000);
    } else if (year < 1860) {
      double t = year - 1800;
      deltaT =
          polynomial(
              t,
              13.72,
              -0.332447,
              0.0068612,
              0.0041116,
              -0.00037436,
              0.0000121272,
              -0.0000001699,
              0.000000000875);
    } else if (year < 1900) {
      double t = year - 1860;
      deltaT = polynomial(t, 7.62, 0.5737, -0.251754, 0.01680668, -0.0004473624, 1.0 / 233174);
    } else if (year < 1920) {
      double t = year - 1900;
      deltaT = polynomial(t, -2.79, 1.494119, -0.0598939, 0.0061966, -0.000197);
    } else if (year < 1941) {
      double t = year - 1920;
      deltaT = polynomial(t, 21.20, 0.84493, -0.076100, 0.0020936);
    } else if (year < 1961) {
      double t = year - 1950;
      deltaT = polynomial(t, 29.07, 0.407, -1.0 / 233, 1.0 / 2547);
    } else if (year < 1986) {
      double t = year - 1975;
      deltaT = polynomial(t, 45.45, 1.067, -1.0 / 260, -1.0 / 718);
    } else if (year < 2005) {
      double t = year - 2000;
      deltaT = polynomial(t, 63.86, 0.3345, -0.060374, 0.0017275, 0.000651814, 0.00002373599);
    } else if (year < 2015) {
      double t = year - 2005;
      deltaT = polynomial(t, 64.69, 0.2930);
    } else if (year <= 3000) {
      double t = year - 2015;
      deltaT = polynomial(t, 67.62, 0.3645, 0.0039755);
    } else {
      throw new IllegalArgumentException("no estimates possible for this time");
    }

    return deltaT;
  }

  private static double decimalYear(LocalDate forDate) {
    return forDate.getYear() + (forDate.getMonthValue() - 0.5) / 12;
  }
}
