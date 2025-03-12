package net.e175.klaus.solarpositioning;

import static java.lang.Math.*;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

/**
 * Calculate topocentric solar position and sunrise/sunset/twilight times using the NREL SPA
 * algorithm.
 *
 * <p>This follows the SPA algorithm described in Reda, I.; Andreas, A. (2003): Solar Position
 * Algorithm for Solar Radiation Applications. NREL Report No. TP-560-34302, Revised January 2008.
 *
 * <p>This is <i>not</i> a port of the C code, but a re-implementation based on the published
 * procedure.
 *
 * @author Klaus Brunner
 */
public final class SPA {

  private static final int MS_PER_DAY = 24 * 60 * 60 * 1000;

  private static final double SUNRISE_SUNSET = -0.83337;

  private SPA() {}

  /**
   * Predefined elevation angles to use in sunrise-sunset calculation. This allows to get twilight
   * times as well as standard sunrise and sunset.
   */
  public enum Horizon {
    SUNRISE_SUNSET(SPA.SUNRISE_SUNSET),
    CIVIL_TWILIGHT(-6),
    NAUTICAL_TWILIGHT(-12),
    ASTRONOMICAL_TWILIGHT(-18);

    private final double elevation;

    public double elevation() {
      return elevation;
    }

    Horizon(double elevation) {
      this.elevation = elevation;
    }
  }

  /**
   * Calculate topocentric solar position: the location of the sun on the sky for a certain point in
   * time on a certain point of the Earth's surface.
   *
   * <p>This follows the SPA algorithm described in Reda, I.; Andreas, A. (2003): Solar Position
   * Algorithm for Solar Radiation Applications. NREL Report No. TP-560-34302, Revised January 2008.
   * The algorithm is supposed to work for the years -2000 to 6000, with uncertainties of +/-0.0003
   * degrees.
   *
   * @param date Observer's local date and time.
   * @param latitude Observer's latitude, in degrees (negative south of equator).
   * @param longitude Observer's longitude, in degrees (negative west of Greenwich).
   * @param elevation Observer's elevation, in meters.
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
      final double elevation,
      final double deltaT,
      final double pressure,
      final double temperature) {
    MathUtil.checkLatLonRange(latitude, longitude);

    // calculate Julian (ephemeris) date and millennium
    final JulianDate jd = new JulianDate(date, deltaT);
    final double jme = jd.julianEphemerisMillennium();
    final double jce = jd.julianEphemerisCentury();

    // calculate Earth heliocentric longitude, L
    final double[] lTerms = calculateLBRTerms(jme, TERMS_L);
    final double lDegrees = limitDegreesTo360(toDegrees(calculateLBRPolynomial(jme, lTerms)));

    // calculate Earth heliocentric latitude, B
    final double[] bTerms = calculateLBRTerms(jme, TERMS_B);
    final double bDegrees = limitDegreesTo360(toDegrees(calculateLBRPolynomial(jme, bTerms)));

    // calculate Earth radius vector, R
    final double[] rTerms = calculateLBRTerms(jme, TERMS_R);
    final double r = calculateLBRPolynomial(jme, rTerms);
    assert r != 0;

    // calculate geocentric longitude, theta
    final double thetaDegrees = limitDegreesTo360(lDegrees + 180);
    // calculate geocentric latitude, beta
    final double betaDegrees = -bDegrees;
    final double beta = toRadians(betaDegrees);

    // calculate nutation
    final double[] xTerms = calculateNutationTerms(jce);
    final double[] deltaPsiI = calculateDeltaPsiI(jce, xTerms);
    final double[] deltaEpsilonI = calculateDeltaEpsilonI(jce, xTerms);
    final double deltaPsi = calculateDeltaPsiEpsilon(deltaPsiI);
    final double deltaEpsilon = calculateDeltaPsiEpsilon(deltaEpsilonI);

    // calculate the true obliquity of the ecliptic
    final double epsilonDegrees = calculateTrueObliquityOfEcliptic(jd, deltaEpsilon);
    final double epsilon = toRadians(epsilonDegrees);

    // calculate aberration correction
    final double deltaTau = -20.4898 / (3600 * r);

    // calculate the apparent sun longitude
    final double lambdaDegrees = thetaDegrees + deltaPsi + deltaTau;
    final double lambda = toRadians(lambdaDegrees);

    // Calculate the apparent sidereal time at Greenwich
    final double nuDegrees = calculateApparentSiderealTimeAtGreenwich(jd, deltaPsi, epsilonDegrees);

    // Calculate the geocentric sun right ascension
    final double alphaDegrees = calculateGeocentricSunRightAscension(beta, epsilon, lambda);

    // Calculate geocentric sun declination
    final double deltaDegrees = toDegrees(calculateGeocentricSunDeclination(beta, epsilon, lambda));

    // Calculate observer local hour angle
    final double hDegrees = limitDegreesTo360(nuDegrees + longitude - alphaDegrees);
    final double h = toRadians(hDegrees);

    // Calculate the topocentric sun right ascension and sun declination
    final double xiDegrees = 8.794 / (3600 * r);
    final double xi = toRadians(xiDegrees);
    final double phi = toRadians(latitude);
    final double delta = toRadians(deltaDegrees);
    final double u = atan(0.99664719 * tan(phi));
    final double x = cos(u) + elevation * cos(phi) / 6378140;
    final double y = 0.99664719 * sin(u) + (elevation * sin(phi)) / 6378140;

    final double x1 = cos(delta) - x * sin(xi) * cos(h);
    final double deltaAlphaDegrees = toDegrees(atan2(-x * sin(xi) * sin(h), x1));
    final double deltaPrime =
        atan2((sin(delta) - y * sin(xi)) * cos(toRadians(deltaAlphaDegrees)), x1);

    // Calculate the topocentric local hour angle,
    final double hPrimeDegrees = hDegrees - deltaAlphaDegrees;
    final double hPrime = toRadians(hPrimeDegrees);

    return calculateTopocentricSolarPosition(pressure, temperature, phi, deltaPrime, hPrime);
  }

  /**
   * Calculate topocentric solar position: the location of the sun on the sky for a certain point in
   * time on a certain point of the Earth's surface.
   *
   * <p>This follows the SPA algorithm described in Reda, I.; Andreas, A. (2003): Solar Position
   * Algorithm for Solar Radiation Applications. NREL Report No. TP-560-34302, Revised January 2008.
   * The algorithm is supposed to work for the years -2000 to 6000, with uncertainties of +/-0.0003
   * degrees.
   *
   * <p>This method does not perform refraction correction.
   *
   * @param date Observer's local date and time.
   * @param latitude Observer's latitude, in degrees (negative south of equator).
   * @param longitude Observer's longitude, in degrees (negative west of Greenwich).
   * @param elevation Observer's elevation, in meters.
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
      final double elevation,
      final double deltaT) {
    return calculateSolarPosition(
        date, latitude, longitude, elevation, deltaT, Double.NaN, Double.NaN);
  }

  private enum Type {
    NORMAL,
    ALL_DAY,
    ALL_NIGHT
  }

  private record AlphaDelta(double alpha, double delta) {}

  /**
   * Calculate the times of sunrise, sun transit (solar noon), and sunset for a given day. The
   * calculation is based on the astronomical definition of sunrise and sunset, using a refraction
   * correction of -0.8333°.
   *
   * @param day GregorianCalendar of day for which sunrise/transit/sunset are to be calculated. The
   *     time of day (hour, minute, second, millisecond) is ignored.
   * @param latitude Observer's latitude, in degrees (negative south of equator).
   * @param longitude Observer's longitude, in degrees (negative west of Greenwich).
   * @param deltaT Difference between earth rotation time and terrestrial time (or Universal Time
   *     and Terrestrial Time), in seconds. See {@link JulianDate#JulianDate(ZonedDateTime, double)}
   *     and {@link DeltaT}.
   * @throws IllegalArgumentException for nonsensical latitude/longitude
   * @return An implementation of {@link SunriseResult} depending on the type of day.
   */
  public static SunriseResult calculateSunriseTransitSet(
      final ZonedDateTime day, final double latitude, final double longitude, final double deltaT) {
    return calculateSunriseTransitSet(day, latitude, longitude, deltaT, Horizon.SUNRISE_SUNSET);
  }

  private record RiseSetParams(double nuDegrees, AlphaDelta[] alphaDeltas, double[] m) {}

  /**
   * Calculate the times of sunrise, sun transit (solar noon), and sunset for a given day. The
   * definition of sunrise or sunset can be chosen based on a horizon type (defined via its
   * elevation angle).
   *
   * @param day GregorianCalendar of day for which sunrise/transit/sunset are to be calculated. The
   *     time of day (hour, minute, second, millisecond) is ignored.
   * @param latitude Observer's latitude, in degrees (negative south of equator).
   * @param longitude Observer's longitude, in degrees (negative west of Greenwich).
   * @param deltaT Difference between earth rotation time and terrestrial time (or Universal Time
   *     and Terrestrial Time), in seconds. See {@link JulianDate#JulianDate(ZonedDateTime, double)}
   *     and {@link DeltaT}.
   * @param horizon Horizon (basically, elevation angle) to use as the sunrise/sunset definition.
   *     This can be used to calculate twilight times.
   * @throws IllegalArgumentException for nonsensical latitude/longitude
   * @return An implementation of {@link SunriseResult} depending on the type of day.
   */
  public static SunriseResult calculateSunriseTransitSet(
      final ZonedDateTime day,
      final double latitude,
      final double longitude,
      final double deltaT,
      final Horizon horizon) {
    final RiseSetParams params = calcRiseSetParams(day, latitude, longitude);

    return calcRiseAndSet(
        day,
        longitude,
        deltaT,
        horizon,
        toRadians(latitude),
        params.nuDegrees,
        params.alphaDeltas,
        params.m);
  }

  /**
   * Calculate the times of sunrise, sun transit (solar noon), and sunset for a given day and
   * horizon types. This is useful to get sunrise/sunset and multiple twilight times in one call and
   * is expected to be faster than separate calls.
   *
   * @param day GregorianCalendar of day for which sunrise/transit/sunset are to be calculated. The
   *     time of day (hour, minute, second, millisecond) is ignored.
   * @param latitude Observer's latitude, in degrees (negative south of equator).
   * @param longitude Observer's longitude, in degrees (negative west of Greenwich).
   * @param deltaT Difference between earth rotation time and terrestrial time (or Universal Time
   *     and Terrestrial Time), in seconds. See {@link JulianDate#JulianDate(ZonedDateTime, double)}
   *     and {@link DeltaT}.
   * @param horizons Horizons (basically, elevation angles) to use as the sunrise/sunset definition.
   *     This can be used to calculate twilight times.
   * @return A Map with one key-value pair for each unique horizon type and {@link SunriseResult}.
   *     This map may or may not be mutable.
   * @throws IllegalArgumentException for nonsensical latitude/longitude
   */
  public static Map<Horizon, SunriseResult> calculateSunriseTransitSet(
      final ZonedDateTime day,
      final double latitude,
      final double longitude,
      final double deltaT,
      final Horizon... horizons) {

    final RiseSetParams params = calcRiseSetParams(day, latitude, longitude);
    final Map<Horizon, SunriseResult> result = new HashMap<>(horizons.length + 1, 1);

    for (Horizon horizon : horizons) {
      result.put(
          horizon,
          calcRiseAndSet(
              day,
              longitude,
              deltaT,
              horizon,
              toRadians(latitude),
              params.nuDegrees,
              params.alphaDeltas,
              params.m));
    }

    return result;
  }

  private static RiseSetParams calcRiseSetParams(
      ZonedDateTime day, double latitude, double longitude) {
    MathUtil.checkLatLonRange(latitude, longitude);

    final ZonedDateTime dayStart = startOfDayUT(day);
    final JulianDate jd = new JulianDate(dayStart, 0);

    // A.2.1. Calculate the apparent sidereal time at Greenwich at 0 UT, nu (in degrees)
    final double jce = jd.julianEphemerisCentury();
    final double[] xTerms = calculateNutationTerms(jce);
    final double[] deltaPsiI = calculateDeltaPsiI(jce, xTerms);
    final double[] deltaEpsilonI = calculateDeltaEpsilonI(jce, xTerms);
    final double deltaPsi = calculateDeltaPsiEpsilon(deltaPsiI);
    final double deltaEpsilon = calculateDeltaPsiEpsilon(deltaEpsilonI);
    final double epsilonDegrees = calculateTrueObliquityOfEcliptic(jd, deltaEpsilon);

    final double nuDegrees = calculateApparentSiderealTimeAtGreenwich(jd, deltaPsi, epsilonDegrees);

    // A.2.2. Calculate the geocentric right ascension and declination at 0 TT for day before, same
    // day, next day
    final AlphaDelta[] alphaDeltas = new AlphaDelta[3];
    for (int i = 0; i < alphaDeltas.length; i++) {
      JulianDate currentJd = new JulianDate(jd.julianDate() + i - 1, 0);
      double currentJme = currentJd.julianEphemerisMillennium();
      AlphaDelta ad = calculateAlphaDelta(currentJme, deltaPsi, epsilonDegrees);
      alphaDeltas[i] = ad;
    }

    final double[] m = new double[3];
    // A.2.3. Calculate the approximate sun transit time, m0, in fraction of day
    m[0] = (alphaDeltas[1].alpha - longitude - nuDegrees) / 360;

    return new RiseSetParams(nuDegrees, alphaDeltas, m);
  }

  private static SunriseResult calcRiseAndSet(
      ZonedDateTime day,
      double longitude,
      double deltaT,
      Horizon horizon,
      double phi,
      double nuDegrees,
      AlphaDelta[] alphaDeltas,
      double[] m) {
    // A.2.4. Calculate the local hour angle H0 corresponding to ...
    final double acosArg =
        (sin(toRadians(horizon.elevation())) - sin(phi) * sin(toRadians(alphaDeltas[1].delta)))
            / (cos(phi) * cos(toRadians(alphaDeltas[1].delta)));

    final Type type =
        acosArg < -1.0 ? Type.ALL_DAY : (acosArg > 1.0 ? Type.ALL_NIGHT : Type.NORMAL);

    final double h0 = acos(acosArg);

    final double h0Degrees = limitTo(toDegrees(h0), 180.0);

    // A.2.5. Calculate the approximate sunrise time, m1, in fraction of day,
    m[1] = limitTo(m[0] - h0Degrees / 360.0, 1);

    // A.2.6. Calculate the approximate sunset time, m2, in fraction of day,
    m[2] = limitTo(m[0] + h0Degrees / 360.0, 1);

    m[0] = limitTo(m[0], 1);

    // A.2.8. Calculate the sidereal time at Greenwich, in degrees, for the sun transit, sunrise,
    // and sunset
    final double[] nu = new double[3];
    for (int i = 0; i < m.length; i++) {
      nu[i] = nuDegrees + 360.985647 * m[i];
    }

    // A.2.9. Calculate the terms ni
    final double[] n = new double[3];
    for (int i = 0; i < m.length; i++) {
      n[i] = m[i] + deltaT / 86400.0;
    }

    // A.2.10. Calculate the values alpha'i and delta'i , in degrees
    final double a = limitIfNecessary(alphaDeltas[1].alpha - alphaDeltas[0].alpha);
    final double aPrime = limitIfNecessary(alphaDeltas[1].delta - alphaDeltas[0].delta);

    final double b = limitIfNecessary(alphaDeltas[2].alpha - alphaDeltas[1].alpha);
    final double bPrime = limitIfNecessary(alphaDeltas[2].delta - alphaDeltas[1].delta);

    final double c = b - a;
    final double cPrime = bPrime - aPrime;

    final AlphaDelta[] alphaDeltaPrimes = new AlphaDelta[3];
    for (int i = 0; i < alphaDeltaPrimes.length; i++) {
      double alphaPrimeI = alphaDeltas[1].alpha + (n[i] * (a + b + c * n[i])) / 2.0;
      double deltaPrimeI = alphaDeltas[1].delta + (n[i] * (aPrime + bPrime + cPrime * n[i])) / 2.0;

      alphaDeltaPrimes[i] = new AlphaDelta(alphaPrimeI, deltaPrimeI);
    }

    // A.2.11. Calculate the local hour angle for the sun transit, sunrise, and sunset
    final double[] hPrime = new double[3];
    for (int i = 0; i < hPrime.length; i++) {
      double hPrimeI = nu[i] + longitude - alphaDeltaPrimes[i].alpha;
      hPrime[i] = limitHprime(hPrimeI);
    }

    // A.2.12. Calculate the sun altitude for the sun transit, sunrise, and sunset, hi
    final double[] h = new double[3];
    for (int i = 0; i < h.length; i++) {
      double deltaPrimeRad = toRadians(alphaDeltaPrimes[i].delta);

      h[i] =
          toDegrees(
              asin(
                  sin(phi) * sin(deltaPrimeRad)
                      + cos(phi) * cos(deltaPrimeRad) * cos(toRadians(hPrime[i]))));
    }

    // A.2.13. Calculate the sun transit, T (in fraction of day)
    final double t = m[0] - hPrime[0] / 360.0;

    // A.2.14. Calculate the sunrise, R (in fraction of day)
    final double r =
        m[1]
            + (h[1] - horizon.elevation())
                / (360.0
                    * cos(toRadians(alphaDeltaPrimes[1].delta))
                    * cos(phi)
                    * sin(toRadians(hPrime[1])));

    // A.2.15. Calculate the sunset, S (in fraction of day)
    final double s =
        m[2]
            + (h[2] - horizon.elevation())
                / (360.0
                    * cos(toRadians(alphaDeltaPrimes[2].delta))
                    * cos(phi)
                    * sin(toRadians(hPrime[2])));

    return switch (type) {
      case NORMAL ->
          new SunriseResult.RegularDay(
              addFractionOfDay(day, r), addFractionOfDay(day, t), addFractionOfDay(day, s));
      case ALL_DAY -> new SunriseResult.AllDay(addFractionOfDay(day, t));
      case ALL_NIGHT -> new SunriseResult.AllNight(addFractionOfDay(day, t));
    };
  }

  private static ZonedDateTime addFractionOfDay(ZonedDateTime day, double fraction) {
    final int millisPlus = (int) (MS_PER_DAY * fraction);
    return day.truncatedTo(ChronoUnit.DAYS).plus(millisPlus, ChronoUnit.MILLIS);
  }

  /** limit H' values according to A.2.11 */
  private static double limitHprime(double hPrime) {
    hPrime /= 360.0;
    final double limited = 360.0 * (hPrime - floor(hPrime));

    if (limited < -180.0) {
      return limited + 360.0;
    } else if (limited > 180.0) {
      return limited - 360.0;
    } else {
      return limited;
    }
  }

  /** Limit to 0..1 if absolute value > 2. Refer to A.2.10 in NREL report. */
  private static double limitIfNecessary(double val) {
    return (abs(val) > 2.0) ? limitTo(val, 1.0) : val;
  }

  private static AlphaDelta calculateAlphaDelta(
      double jme, double deltaPsi, double epsilonDegrees) {
    // calculate Earth heliocentric latitude, B
    final double[] bTerms = calculateLBRTerms(jme, TERMS_B);
    final double bDegrees = limitDegreesTo360(toDegrees(calculateLBRPolynomial(jme, bTerms)));

    // calculate Earth radius vector, R
    final double[] rTerms = calculateLBRTerms(jme, TERMS_R);
    final double r = calculateLBRPolynomial(jme, rTerms);
    assert r != 0;

    // calculate Earth heliocentric longitude, L
    final double[] lTerms = calculateLBRTerms(jme, TERMS_L);
    final double lDegrees = limitDegreesTo360(toDegrees(calculateLBRPolynomial(jme, lTerms)));

    // calculate geocentric longitude, theta
    final double thetaDegrees = limitDegreesTo360(lDegrees + 180);

    // calculate geocentric latitude, beta
    final double betaDegrees = -bDegrees;
    final double beta = toRadians(betaDegrees);
    final double epsilon = toRadians(epsilonDegrees);

    // calculate aberration correction
    final double deltaTau = -20.4898 / (3600 * r);

    // calculate the apparent sun longitude
    final double lambdaDegrees = thetaDegrees + deltaPsi + deltaTau;
    final double lambda = toRadians(lambdaDegrees);

    // Calculate the geocentric sun right ascension
    final double alphaDegrees = calculateGeocentricSunRightAscension(beta, epsilon, lambda);
    // Calculate geocentric sun declination
    final double deltaDegrees = toDegrees(calculateGeocentricSunDeclination(beta, epsilon, lambda));

    return new AlphaDelta(alphaDegrees, deltaDegrees);
  }

  private static ZonedDateTime startOfDayUT(ZonedDateTime day) {
    return day.truncatedTo(ChronoUnit.DAYS);
  }

  private static SolarPosition calculateTopocentricSolarPosition(
      final double p,
      final double t,
      final double phi,
      final double deltaPrime,
      final double hPrime) {
    // calculate topocentric zenith angle
    final double eZero =
        asin(sin(phi) * sin(deltaPrime) + cos(phi) * cos(deltaPrime) * cos(hPrime));
    final double topocentricZenithAngle = calculateTopocentricZenithAngle(p, t, eZero);

    // Calculate the topocentric azimuth angle
    final double gamma = atan2(sin(hPrime), cos(hPrime) * sin(phi) - tan(deltaPrime) * cos(phi));
    final double gammaDegrees = limitDegreesTo360(toDegrees(gamma));
    final double topocentricAzimuthAngle = limitDegreesTo360(gammaDegrees + 180);

    return new SolarPosition(topocentricAzimuthAngle, topocentricZenithAngle);
  }

  private static double calculateTopocentricZenithAngle(double p, double t, double eZero) {
    final double eZeroDegrees = toDegrees(eZero);

    // refraction correction.
    // 1) extremely silly values for p and t are silently ignored, disabling correction
    // 2) only apply refraction correction when the sun is visible
    boolean doCorrect = MathUtil.checkRefractionParamsUsable(p, t) && eZeroDegrees > SUNRISE_SUNSET;

    if (doCorrect) {
      return 90
          - eZeroDegrees
          - (p / 1010.0)
              * (283.0 / (273.0 + t))
              * 1.02
              / (60.0 * tan(toRadians(eZeroDegrees + 10.3 / (eZeroDegrees + 5.11))));
    } else {
      return 90 - eZeroDegrees;
    }
  }

  private static double calculateGeocentricSunDeclination(
      final double betaRad, final double epsilonRad, final double lambdaRad) {
    return asin(sin(betaRad) * cos(epsilonRad) + cos(betaRad) * sin(epsilonRad) * sin(lambdaRad));
  }

  private static double calculateGeocentricSunRightAscension(
      final double betaRad, final double epsilonRad, final double lambdaRad) {
    final double alpha =
        atan2(sin(lambdaRad) * cos(epsilonRad) - tan(betaRad) * sin(epsilonRad), cos(lambdaRad));

    return limitDegreesTo360(toDegrees(alpha));
  }

  private static double calculateTrueObliquityOfEcliptic(
      final JulianDate jd, final double deltaEpsilon) {
    final double epsilon0 =
        MathUtil.polynomial(jd.julianEphemerisMillennium() / 10.0, OBLIQUITY_COEFFS);
    return epsilon0 / 3600 + deltaEpsilon;
  }

  private static double calculateApparentSiderealTimeAtGreenwich(
      final JulianDate jd, final double deltaPsi, final double epsilonDegrees) {
    final double nu0degrees =
        limitDegreesTo360(
            280.46061837
                + 360.98564736629 * (jd.julianDate() - 2451545)
                + pow(jd.julianCentury(), 2) * (0.000387933 - jd.julianCentury() / 38710000));

    return nu0degrees + deltaPsi * cos(toRadians(epsilonDegrees));
  }

  private static double calculateDeltaPsiEpsilon(final double[] deltaPsiOrEpsilonI) {
    double sum = 0;
    for (final double element : deltaPsiOrEpsilonI) {
      sum += element;
    }
    return sum / 36000000;
  }

  private static double[] calculateDeltaPsiI(final double jce, final double[] x) {
    final double[] deltaPsiI = new double[TERMS_PE.length];

    for (int i = 0; i < TERMS_PE.length; i++) {
      final double a = TERMS_PE[i][0];
      final double b = TERMS_PE[i][1];
      deltaPsiI[i] = (a + b * jce) * sin(toRadians(calculateXjYtermSum(i, x)));
    }
    return deltaPsiI;
  }

  private static double[] calculateDeltaEpsilonI(final double jce, final double[] x) {
    final double[] deltaEpsilonI = new double[TERMS_PE.length];

    for (int i = 0; i < TERMS_PE.length; i++) {
      final double c = TERMS_PE[i][2];
      final double d = TERMS_PE[i][3];
      deltaEpsilonI[i] = (c + d * jce) * cos(toRadians(calculateXjYtermSum(i, x)));
    }
    return deltaEpsilonI;
  }

  private static double calculateXjYtermSum(final int i, final double[] x) {
    double sum = 0;
    for (int j = 0; j < x.length; j++) {
      sum += x[j] * TERMS_Y[i][j];
    }
    return sum;
  }

  private static double[] calculateNutationTerms(final double jce) {
    final double[] x = new double[NUTATION_COEFFS.length];
    for (int i = 0; i < x.length; i++) {
      x[i] = MathUtil.polynomial(jce, NUTATION_COEFFS[i]);
    }
    return x;
  }

  private static double limitDegreesTo360(final double degrees) {
    return limitTo(degrees, 360.0);
  }

  private static double limitTo(final double degrees, final double max) {
    final double dividedDegrees = degrees / max;
    final double limited = max * (dividedDegrees - floor(dividedDegrees));
    return (limited < 0) ? limited + max : limited;
  }

  private static double calculateLBRPolynomial(final double jme, final double[] terms) {
    return MathUtil.polynomial(jme, terms) / 1e8;
  }

  private static double[] calculateLBRTerms(final double jme, final double[][][] termCoeffs) {
    final double[] lbrTerms = {0, 0, 0, 0, 0, 0};

    for (int i = 0; i < termCoeffs.length; i++) { // L0, L1, ... Ln
      double lbrSum = 0;
      for (int v = 0; v < termCoeffs[i].length; v++) { // rows of each Li
        final double a = termCoeffs[i][v][0]; // coefficients
        final double b = termCoeffs[i][v][1];
        final double c = termCoeffs[i][v][2];

        lbrSum += a * cos(b + c * jme);
      }
      lbrTerms[i] = lbrSum;
    }

    return lbrTerms;
  }

  private static final double[][][] TERMS_L = {
    {
      {175347046.0, 0, 0},
      {3341656.0, 4.6692568, 6283.07585},
      {34894.0, 4.6261, 12566.1517},
      {3497.0, 2.7441, 5753.3849},
      {3418.0, 2.8289, 3.5231},
      {3136.0, 3.6277, 77713.7715},
      {2676.0, 4.4181, 7860.4194},
      {2343.0, 6.1352, 3930.2097},
      {1324.0, 0.7425, 11506.7698},
      {1273.0, 2.0371, 529.691},
      {1199.0, 1.1096, 1577.3435},
      {990, 5.233, 5884.927},
      {902, 2.045, 26.298},
      {857, 3.508, 398.149},
      {780, 1.179, 5223.694},
      {753, 2.533, 5507.553},
      {505, 4.583, 18849.228},
      {492, 4.205, 775.523},
      {357, 2.92, 0.067},
      {317, 5.849, 11790.629},
      {284, 1.899, 796.298},
      {271, 0.315, 10977.079},
      {243, 0.345, 5486.778},
      {206, 4.806, 2544.314},
      {205, 1.869, 5573.143},
      {202, 2.458, 6069.777},
      {156, 0.833, 213.299},
      {132, 3.411, 2942.463},
      {126, 1.083, 20.775},
      {115, 0.645, 0.98},
      {103, 0.636, 4694.003},
      {102, 0.976, 15720.839},
      {102, 4.267, 7.114},
      {99, 6.21, 2146.17},
      {98, 0.68, 155.42},
      {86, 5.98, 161000.69},
      {85, 1.3, 6275.96},
      {85, 3.67, 71430.7},
      {80, 1.81, 17260.15},
      {79, 3.04, 12036.46},
      {75, 1.76, 5088.63},
      {74, 3.5, 3154.69},
      {74, 4.68, 801.82},
      {70, 0.83, 9437.76},
      {62, 3.98, 8827.39},
      {61, 1.82, 7084.9},
      {57, 2.78, 6286.6},
      {56, 4.39, 14143.5},
      {56, 3.47, 6279.55},
      {52, 0.19, 12139.55},
      {52, 1.33, 1748.02},
      {51, 0.28, 5856.48},
      {49, 0.49, 1194.45},
      {41, 5.37, 8429.24},
      {41, 2.4, 19651.05},
      {39, 6.17, 10447.39},
      {37, 6.04, 10213.29},
      {37, 2.57, 1059.38},
      {36, 1.71, 2352.87},
      {36, 1.78, 6812.77},
      {33, 0.59, 17789.85},
      {30, 0.44, 83996.85},
      {30, 2.74, 1349.87},
      {25, 3.16, 4690.48}
    },
    {
      {628331966747.0, 0, 0},
      {206059.0, 2.678235, 6283.07585},
      {4303.0, 2.6351, 12566.1517},
      {425.0, 1.59, 3.523},
      {119.0, 5.796, 26.298},
      {109.0, 2.966, 1577.344},
      {93, 2.59, 18849.23},
      {72, 1.14, 529.69},
      {68, 1.87, 398.15},
      {67, 4.41, 5507.55},
      {59, 2.89, 5223.69},
      {56, 2.17, 155.42},
      {45, 0.4, 796.3},
      {36, 0.47, 775.52},
      {29, 2.65, 7.11},
      {21, 5.34, 0.98},
      {19, 1.85, 5486.78},
      {19, 4.97, 213.3},
      {17, 2.99, 6275.96},
      {16, 0.03, 2544.31},
      {16, 1.43, 2146.17},
      {15, 1.21, 10977.08},
      {12, 2.83, 1748.02},
      {12, 3.26, 5088.63},
      {12, 5.27, 1194.45},
      {12, 2.08, 4694},
      {11, 0.77, 553.57},
      {10, 1.3, 6286.6},
      {10, 4.24, 1349.87},
      {9, 2.7, 242.73},
      {9, 5.64, 951.72},
      {8, 5.3, 2352.87},
      {6, 2.65, 9437.76},
      {6, 4.67, 4690.48}
    },
    {
      {52919.0, 0, 0},
      {8720.0, 1.0721, 6283.0758},
      {309.0, 0.867, 12566.152},
      {27, 0.05, 3.52},
      {16, 5.19, 26.3},
      {16, 3.68, 155.42},
      {10, 0.76, 18849.23},
      {9, 2.06, 77713.77},
      {7, 0.83, 775.52},
      {5, 4.66, 1577.34},
      {4, 1.03, 7.11},
      {4, 3.44, 5573.14},
      {3, 5.14, 796.3},
      {3, 6.05, 5507.55},
      {3, 1.19, 242.73},
      {3, 6.12, 529.69},
      {3, 0.31, 398.15},
      {3, 2.28, 553.57},
      {2, 4.38, 5223.69},
      {2, 3.75, 0.98}
    },
    {
      {289.0, 5.844, 6283.076},
      {35, 0, 0},
      {17, 5.49, 12566.15},
      {3, 5.2, 155.42},
      {1, 4.72, 3.52},
      {1, 5.3, 18849.23},
      {1, 5.97, 242.73}
    },
    {{114.0, 3.142, 0}, {8, 4.13, 6283.08}, {1, 3.84, 12566.15}},
    {{1, 3.14, 0}}
  };

  private static final double[][][] TERMS_B = {
    {
      {280.0, 3.199, 84334.662},
      {102.0, 5.422, 5507.553},
      {80, 3.88, 5223.69},
      {44, 3.7, 2352.87},
      {32, 4, 1577.34}
    },
    {{9, 3.9, 5507.55}, {6, 1.73, 5223.69}}
  };

  private static final double[][][] TERMS_R = {
    {
      {100013989.0, 0, 0},
      {1670700.0, 3.0984635, 6283.07585},
      {13956.0, 3.05525, 12566.1517},
      {3084.0, 5.1985, 77713.7715},
      {1628.0, 1.1739, 5753.3849},
      {1576.0, 2.8469, 7860.4194},
      {925.0, 5.453, 11506.77},
      {542.0, 4.564, 3930.21},
      {472.0, 3.661, 5884.927},
      {346.0, 0.964, 5507.553},
      {329.0, 5.9, 5223.694},
      {307.0, 0.299, 5573.143},
      {243.0, 4.273, 11790.629},
      {212.0, 5.847, 1577.344},
      {186.0, 5.022, 10977.079},
      {175.0, 3.012, 18849.228},
      {110.0, 5.055, 5486.778},
      {98, 0.89, 6069.78},
      {86, 5.69, 15720.84},
      {86, 1.27, 161000.69},
      {65, 0.27, 17260.15},
      {63, 0.92, 529.69},
      {57, 2.01, 83996.85},
      {56, 5.24, 71430.7},
      {49, 3.25, 2544.31},
      {47, 2.58, 775.52},
      {45, 5.54, 9437.76},
      {43, 6.01, 6275.96},
      {39, 5.36, 4694},
      {38, 2.39, 8827.39},
      {37, 0.83, 19651.05},
      {37, 4.9, 12139.55},
      {36, 1.67, 12036.46},
      {35, 1.84, 2942.46},
      {33, 0.24, 7084.9},
      {32, 0.18, 5088.63},
      {32, 1.78, 398.15},
      {28, 1.21, 6286.6},
      {28, 1.9, 6279.55},
      {26, 4.59, 10447.39}
    },
    {
      {103019.0, 1.10749, 6283.07585},
      {1721.0, 1.0644, 12566.1517},
      {702.0, 3.142, 0},
      {32, 1.02, 18849.23},
      {31, 2.84, 5507.55},
      {25, 1.32, 5223.69},
      {18, 1.42, 1577.34},
      {10, 5.91, 10977.08},
      {9, 1.42, 6275.96},
      {9, 0.27, 5486.78}
    },
    {
      {4359.0, 5.7846, 6283.0758},
      {124.0, 5.579, 12566.152},
      {12, 3.14, 0},
      {9, 3.63, 77713.77},
      {6, 1.87, 5573.14},
      {3, 5.47, 18849.23}
    },
    {{145.0, 4.273, 6283.076}, {7, 3.92, 12566.15}},
    {{4, 2.56, 6283.08}}
  };

  private static final double[][] NUTATION_COEFFS = {
    {297.85036, 445267.111480, -0.0019142, 1.0 / 189474},
    {357.52772, 35999.050340, -0.0001603, -1.0 / 300000},
    {134.96298, 477198.867398, 0.0086972, 1.0 / 56250},
    {93.27191, 483202.017538, -0.0036825, 1.0 / 327270},
    {125.04452, -1934.136261, 0.0020708, 1.0 / 450000}
  };

  private static final double[][] TERMS_Y = {
    {0, 0, 0, 0, 1},
    {-2, 0, 0, 2, 2},
    {0, 0, 0, 2, 2},
    {0, 0, 0, 0, 2},
    {0, 1, 0, 0, 0},
    {0, 0, 1, 0, 0},
    {-2, 1, 0, 2, 2},
    {0, 0, 0, 2, 1},
    {0, 0, 1, 2, 2},
    {-2, -1, 0, 2, 2},
    {-2, 0, 1, 0, 0},
    {-2, 0, 0, 2, 1},
    {0, 0, -1, 2, 2},
    {2, 0, 0, 0, 0},
    {0, 0, 1, 0, 1},
    {2, 0, -1, 2, 2},
    {0, 0, -1, 0, 1},
    {0, 0, 1, 2, 1},
    {-2, 0, 2, 0, 0},
    {0, 0, -2, 2, 1},
    {2, 0, 0, 2, 2},
    {0, 0, 2, 2, 2},
    {0, 0, 2, 0, 0},
    {-2, 0, 1, 2, 2},
    {0, 0, 0, 2, 0},
    {-2, 0, 0, 2, 0},
    {0, 0, -1, 2, 1},
    {0, 2, 0, 0, 0},
    {2, 0, -1, 0, 1},
    {-2, 2, 0, 2, 2},
    {0, 1, 0, 0, 1},
    {-2, 0, 1, 0, 1},
    {0, -1, 0, 0, 1},
    {0, 0, 2, -2, 0},
    {2, 0, -1, 2, 1},
    {2, 0, 1, 2, 2},
    {0, 1, 0, 2, 2},
    {-2, 1, 1, 0, 0},
    {0, -1, 0, 2, 2},
    {2, 0, 0, 2, 1},
    {2, 0, 1, 0, 0},
    {-2, 0, 2, 2, 2},
    {-2, 0, 1, 2, 1},
    {2, 0, -2, 0, 1},
    {2, 0, 0, 0, 1},
    {0, -1, 1, 0, 0},
    {-2, -1, 0, 2, 1},
    {-2, 0, 0, 0, 1},
    {0, 0, 2, 2, 1},
    {-2, 0, 2, 0, 1},
    {-2, 1, 0, 2, 1},
    {0, 0, 1, -2, 0},
    {-1, 0, 1, 0, 0},
    {-2, 1, 0, 0, 0},
    {1, 0, 0, 0, 0},
    {0, 0, 1, 2, 0},
    {0, 0, -2, 2, 2},
    {-1, -1, 1, 0, 0},
    {0, 1, 1, 0, 0},
    {0, -1, 1, 2, 2},
    {2, -1, -1, 2, 2},
    {0, 0, 3, 2, 2},
    {2, -1, 0, 2, 2}
  };

  private static final double[][] TERMS_PE = {
    {-171996, -174.2, 92025, 8.9},
    {-13187, -1.6, 5736, -3.1},
    {-2274, -0.2, 977, -0.5},
    {2062, 0.2, -895, 0.5},
    {1426, -3.4, 54, -0.1},
    {712, 0.1, -7, 0},
    {-517, 1.2, 224, -0.6},
    {-386, -0.4, 200, 0},
    {-301, 0, 129, -0.1},
    {217, -0.5, -95, 0.3},
    {-158, 0, 0, 0},
    {129, 0.1, -70, 0},
    {123, 0, -53, 0},
    {63, 0, 0, 0},
    {63, 0.1, -33, 0},
    {-59, 0, 26, 0},
    {-58, -0.1, 32, 0},
    {-51, 0, 27, 0},
    {48, 0, 0, 0},
    {46, 0, -24, 0},
    {-38, 0, 16, 0},
    {-31, 0, 13, 0},
    {29, 0, 0, 0},
    {29, 0, -12, 0},
    {26, 0, 0, 0},
    {-22, 0, 0, 0},
    {21, 0, -10, 0},
    {17, -0.1, 0, 0},
    {16, 0, -8, 0},
    {-16, 0.1, 7, 0},
    {-15, 0, 9, 0},
    {-13, 0, 7, 0},
    {-12, 0, 6, 0},
    {11, 0, 0, 0},
    {-10, 0, 5, 0},
    {-8, 0, 3, 0},
    {7, 0, -3, 0},
    {-7, 0, 0, 0},
    {-7, 0, 3, 0},
    {-7, 0, 3, 0},
    {6, 0, 0, 0},
    {6, 0, -3, 0},
    {6, 0, -3, 0},
    {-6, 0, 3, 0},
    {-6, 0, 3, 0},
    {5, 0, 0, 0},
    {-5, 0, 3, 0},
    {-5, 0, 3, 0},
    {-5, 0, 3, 0},
    {4, 0, 0, 0},
    {4, 0, 0, 0},
    {4, 0, 0, 0},
    {-4, 0, 0, 0},
    {-4, 0, 0, 0},
    {-4, 0, 0, 0},
    {3, 0, 0, 0},
    {-3, 0, 0, 0},
    {-3, 0, 0, 0},
    {-3, 0, 0, 0},
    {-3, 0, 0, 0},
    {-3, 0, 0, 0},
    {-3, 0, 0, 0},
    {-3, 0, 0, 0}
  };

  private static final double[] OBLIQUITY_COEFFS = {
    84381.448, -4680.93, -1.55, 1999.25, 51.38, -249.67, -39.05, 7.12, 27.87, 5.79, 2.45
  };
}
