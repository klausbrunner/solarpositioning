package net.e175.klaus.solarpositioning;

import static java.lang.Math.*;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

/**
 * Calculate topocentric solar position using the ENEA/Grena algorithm.
 *
 * <p>This follows the no. 3 algorithm described in Grena, 'Five new algorithms for the computation
 * of sun position from 2010 to 2110', Solar Energy 86 (2012) pp. 1323-1337.
 *
 * <p>This is <i>not</i> a port of the C code, but a re-implementation based on the published
 * procedure.
 *
 * @author Klaus Brunner
 */
public final class Grena3 {

  private Grena3() {}

  /**
   * Calculate topocentric solar position: the location of the sun on the sky for a certain point in
   * time on a certain point of the Earth's surface.
   *
   * <p>This follows the no. 3 algorithm described in Grena, 'Five new algorithms for the
   * computation of sun position from 2010 to 2110', Solar Energy 86 (2012) pp. 1323-1337.
   *
   * <p>The algorithm is supposed to work for the years 2010 to 2110, with a maximum error of 0.01
   * degrees.
   *
   * <p>This method does not perform refraction correction.
   *
   * @param date Observer's local date and time.
   * @param latitude Observer's latitude, in degrees (negative south of equator).
   * @param longitude Observer's longitude, in degrees (negative west of Greenwich).
   * @param deltaT Difference between earth rotation time and terrestrial time (or Universal Time
   *     and Terrestrial Time), in seconds. See {@link JulianDate#JulianDate(ZonedDateTime, double)}
   *     and {@link DeltaT}.
   * @return Topocentric solar position (azimuth measured eastward from north)
   * @throws IllegalArgumentException for nonsensical latitude/longitude
   * @see SolarPosition
   */
  public static SolarPosition calculateSolarPosition(
      final ZonedDateTime date,
      final double latitude,
      final double longitude,
      final double deltaT) {
    return calculateSolarPosition(date, latitude, longitude, deltaT, Double.NaN, Double.NaN);
  }

  /**
   * Calculate topocentric solar position: the location of the sun on the sky for a certain point in
   * time on a certain point of the Earth's surface.
   *
   * <p>This follows the no. 3 algorithm described in Grena, 'Five new algorithms for the
   * computation of sun position from 2010 to 2110', Solar Energy 86 (2012) pp. 1323-1337.
   *
   * <p>The algorithm is supposed to work for the years 2010 to 2110, with a maximum error of 0.01
   * degrees.
   *
   * @param date Observer's local date and time.
   * @param latitude Observer's latitude, in degrees (negative south of equator).
   * @param longitude Observer's longitude, in degrees (negative west of Greenwich).
   * @param deltaT Difference between earth rotation time and terrestrial time (or Universal Time
   *     and Terrestrial Time), in seconds. See {@link JulianDate#JulianDate(ZonedDateTime, double)}
   *     and {@link DeltaT}.
   * @param pressure Annual average local pressure, in millibars (or hectopascals). Used for
   *     refraction correction of zenith angle. If unsure, 1000 is a reasonable default.
   * @param temperature Annual average local temperature, in degrees Celsius. Used for refraction
   *     correction of zenith angle.
   * @return Topocentric solar position (azimuth measured eastward from north)
   * @throws IllegalArgumentException for nonsensical latitude/longitude
   * @see SolarPosition
   */
  public static SolarPosition calculateSolarPosition(
      final ZonedDateTime date,
      final double latitude,
      final double longitude,
      final double deltaT,
      final double pressure,
      final double temperature) {
    MathUtil.checkLatLonRange(latitude, longitude);

    final double t = calcT(date);
    final double tE = t + 1.1574e-5 * deltaT;
    final double omegaAtE = 0.0172019715 * tE;

    final double lambda =
        -1.388803
            + 1.720279216e-2 * tE
            + 3.3366e-2 * sin(omegaAtE - 0.06172)
            + 3.53e-4 * sin(2.0 * omegaAtE - 0.1163);

    final double epsilon = 4.089567e-1 - 6.19e-9 * tE;

    final double sLambda = sin(lambda);
    final double cLambda = cos(lambda);
    final double sEpsilon = sin(epsilon);
    final double cEpsilon = sqrt(1 - sEpsilon * sEpsilon);

    double alpha = atan2(sLambda * cEpsilon, cLambda);
    if (alpha < 0) {
      alpha += 2 * PI;
    }

    final double delta = asin(sLambda * sEpsilon);

    double H = 1.7528311 + 6.300388099 * t + toRadians(longitude) - alpha;
    H = ((H + PI) % (2 * PI)) - PI;
    if (H < -PI) {
      H += 2 * PI;
    }

    // end of "short procedure"
    final double sPhi = sin(toRadians(latitude));
    final double cPhi = sqrt((1 - sPhi * sPhi));
    final double sDelta = sin(delta);
    final double cDelta = sqrt(1 - sDelta * sDelta);
    final double sH = sin(H);
    final double cH = cos(H);

    final double sEpsilon0 = sPhi * sDelta + cPhi * cDelta * cH;
    final double eP = asin(sEpsilon0) - 4.26e-5 * sqrt(1.0 - sEpsilon0 * sEpsilon0);
    final double gamma = atan2(sH, cH * sPhi - sDelta * cPhi / cDelta);

    // refraction correction (disabled for silly parameter values)
    final boolean doCorrect =
        MathUtil.checkRefractionParamsUsable(pressure, temperature) && eP > 0.0;

    final double deltaRe =
        doCorrect
            ? (0.08422 * (pressure / 1000))
                / ((273.0 + temperature) * tan(eP + 0.003138 / (eP + 0.08919)))
            : 0.0;

    final double z = PI / 2 - eP - deltaRe;

    return new SolarPosition(toDegrees(gamma + PI) % 360.0, toDegrees(z));
  }

  private static double calcT(ZonedDateTime date) {
    ZonedDateTime utc = date.withZoneSameInstant(ZoneOffset.UTC);

    int m = utc.getMonthValue();
    int y = utc.getYear();
    final int d = utc.getDayOfMonth();
    final double h = utc.getHour() + utc.getMinute() / 60d + utc.getSecond() / (60d * 60);

    if (m <= 2) {
      m += 12;
      y -= 1;
    }

    return (int) (365.25 * (y - 2000))
        + (int) (30.6001 * (m + 1))
        - (int) (0.01 * y)
        + d
        + 0.0416667 * h
        - 21958;
  }
}
