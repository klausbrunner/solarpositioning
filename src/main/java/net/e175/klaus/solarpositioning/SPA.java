package net.e175.klaus.solarpositioning;

import static java.lang.Math.*;
import static net.e175.klaus.solarpositioning.MathUtil.*;

/**
 * Calculate topocentric solar position using the NREL SPA algorithm.
 *
 * <p>This follows the SPA algorithm described in Reda, I.; Andreas, A. (2003): Solar Position
 * Algorithm for Solar Radiation Applications. NREL Report No. TP-560-34302, Revised January 2008.
 *
 * <p>This is <i>not</i> a port of the C code, but a re-implementation based on the published
 * procedure.
 *
 * @author Klaus Brunner
 */
final class SPA {

  private static final double SUNRISE_SUNSET = -0.83337;

  private SPA() {}

  /**
   * Time-dependent intermediate values from SPA calculation (steps 1-11). These values depend only
   * on the datetime and can be computed once and reused for multiple coordinates.
   */
  record SpaTimeDependent(double r, double nuDegrees, double alphaDegrees, double deltaDegrees) {}

  static SolarEvents.Position eventPosition(JulianDate time, double latitude, double longitude) {
    double phi = toRadians(latitude), u = atan(0.99664719 * tan(phi));
    double sinPhi = sin(phi), cosPhi = cos(phi), x = cos(u), z = 0.99664719 * sin(u);
    var parts = calculateSpaTimeDependentParts(time);
    double delta = toRadians(parts.deltaDegrees());
    double hourAngle = parts.nuDegrees() + longitude - parts.alphaDegrees();
    double parallax = sin(toRadians(8.794 / (3600 * parts.r())));
    // SPA's topocentric parallax correction in vector form, stable even at the zenith.
    double vx = cos(delta) * cos(toRadians(hourAngle)) - x * parallax;
    double vy = cos(delta) * sin(toRadians(hourAngle));
    double vz = sin(delta) - z * parallax;
    double projection = (cosPhi * vx + sinPhi * vz) / sqrt(vx * vx + vy * vy + vz * vz);
    return new SolarEvents.Position(toDegrees(asin(max(-1.0, min(1.0, projection)))), hourAngle);
  }

  // Continuous Julian dates let event searches cross calendar boundaries without a clock jump.
  static SpaTimeDependent calculateSpaTimeDependentParts(final JulianDate jd) {
    final double jme = jd.julianEphemerisMillennium();
    final double jce = jd.julianEphemerisCentury();

    // calculate Earth heliocentric longitude, L
    final double lDegrees = lbrToNormalizedDegrees(jme, TERMS_L);

    // calculate Earth heliocentric latitude, B
    final double bDegrees = lbrToNormalizedDegrees(jme, TERMS_B);

    // calculate Earth radius vector, R
    final double r = calculateLBRPolynomial(jme, TERMS_R);
    assert r != 0;

    // calculate geocentric longitude, theta
    final double thetaDegrees = limitTo(lDegrees + 180, 360);
    // calculate geocentric latitude, beta
    final double betaDegrees = -bDegrees;
    final double beta = toRadians(betaDegrees);

    // calculate nutation
    final double[] xTerms = calculateNutationTerms(jce);
    final DeltaPsiEpsilon deltaPsiEpsilon = calculateDeltaPsiEpsilon(jce, xTerms);

    // calculate the true obliquity of the ecliptic
    final double epsilonDegrees =
        calculateTrueObliquityOfEcliptic(jd, deltaPsiEpsilon.deltaEpsilon);
    final double epsilon = toRadians(epsilonDegrees);

    // calculate aberration correction
    final double deltaTau = -20.4898 / (3600 * r);

    // calculate the apparent sun longitude
    final double lambdaDegrees = thetaDegrees + deltaPsiEpsilon.deltaPsi + deltaTau;
    final double lambda = toRadians(lambdaDegrees);

    // Calculate the apparent sidereal time at Greenwich
    final double nuDegrees =
        calculateApparentSiderealTimeAtGreenwich(jd, deltaPsiEpsilon.deltaPsi, epsilonDegrees);

    final AlphaDelta geocentricCoordinates =
        calculateGeocentricSunCoordinates(beta, epsilon, lambda);

    return new SpaTimeDependent(
        r, nuDegrees, geocentricCoordinates.alpha, geocentricCoordinates.delta);
  }

  /**
   * Calculate topocentric solar position using pre-computed time-dependent parts. This method
   * completes the SPA calculation (steps 12+) using cached intermediate values from {@link
   * #calculateSpaTimeDependentParts}. Used together, these provide significant speedup for
   * coordinate sweeps at fixed time.
   *
   * @param latitude Observer's latitude, in degrees (negative south of equator).
   * @param longitude Observer's longitude, in degrees (negative west of Greenwich).
   * @param elevation Observer's elevation, in meters.
   * @param pressure Annual average local pressure, in millibars (or hectopascals). Used for
   *     refraction correction of zenith angle. If unsure, 1000 is a reasonable default.
   * @param temperature Annual average local temperature, in degrees Celsius. Used for refraction
   *     correction of zenith angle.
   * @param timeDependent Pre-computed time-dependent calculations from {@link
   *     #calculateSpaTimeDependentParts}
   * @return Topocentric solar position (azimuth measured eastward from north)
   * @throws IllegalArgumentException for nonsensical latitude/longitude
   * @see SolarPosition
   */
  static SolarPosition calculateSolarPositionWithTimeDependentParts(
      final double latitude,
      final double longitude,
      final double elevation,
      final double pressure,
      final double temperature,
      final SpaTimeDependent timeDependent) {
    checkLatLonRange(latitude, longitude);

    // Extract values from time-dependent parts
    final double r = timeDependent.r;
    final double alphaDegrees = timeDependent.alphaDegrees;
    final double deltaDegrees = timeDependent.deltaDegrees;

    // Calculate observer local hour angle
    final double hDegrees = limitTo(timeDependent.nuDegrees + longitude - alphaDegrees, 360);
    final double h = toRadians(hDegrees);

    // Calculate the topocentric sun right ascension and sun declination
    final double xiDegrees = 8.794 / (3600 * r);
    final double xi = toRadians(xiDegrees);
    final double phi = toRadians(latitude);
    final double delta = toRadians(deltaDegrees);
    final double u = atan(0.99664719 * tan(phi));
    final double x = cos(u) + (elevation * cos(phi)) / 6378140;
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

  private record AlphaDelta(double alpha, double delta) {}

  private static SolarPosition calculateTopocentricSolarPosition(
      final double p,
      final double t,
      final double phi,
      final double deltaPrime,
      final double hPrime) {
    // calculate topocentric zenith angle
    final double sinPhi = sin(phi);
    final double cosPhi = cos(phi);
    final double cosHPrime = cos(hPrime);

    // Roundoff can put the sine just outside [-1, 1] at the zenith or nadir.
    final double sinElevation = sinPhi * sin(deltaPrime) + cosPhi * cos(deltaPrime) * cosHPrime;
    final double eZero = asin(max(-1.0, min(1.0, sinElevation)));
    final double topocentricZenithAngle = calculateTopocentricZenithAngle(p, t, eZero);

    // Calculate the topocentric azimuth angle
    final double gamma = atan2(sin(hPrime), cosHPrime * sinPhi - tan(deltaPrime) * cosPhi);
    final double gammaDegrees = limitTo(toDegrees(gamma), 360);
    final double topocentricAzimuthAngle = limitTo(gammaDegrees + 180, 360);

    return new SolarPosition(topocentricAzimuthAngle, topocentricZenithAngle);
  }

  private static double calculateTopocentricZenithAngle(double p, double t, double eZero) {
    final double eZeroDegrees = toDegrees(eZero);

    // refraction correction.
    // 1) extremely silly values for p and t are silently ignored, disabling correction
    // 2) only apply refraction correction when the sun is visible
    boolean doCorrect = checkRefractionParamsUsable(p, t) && eZeroDegrees > SUNRISE_SUNSET;

    if (doCorrect) {
      return (90
          - eZeroDegrees
          - ((p / 1010.0) * (283.0 / (273.0 + t)) * 1.02)
              / (60.0 * tan(toRadians(eZeroDegrees + 10.3 / (eZeroDegrees + 5.11)))));
    } else {
      return 90 - eZeroDegrees;
    }
  }

  private static AlphaDelta calculateGeocentricSunCoordinates(
      final double betaRad, final double epsilonRad, final double lambdaRad) {
    final double sinLambda = sin(lambdaRad);
    final double cosLambda = cos(lambdaRad);
    final double sinEpsilon = sin(epsilonRad);
    final double cosEpsilon = cos(epsilonRad);
    final double sinBeta = sin(betaRad);
    final double cosBeta = cos(betaRad);

    final double alpha =
        atan2(sinLambda * cosEpsilon - (sinBeta / cosBeta) * sinEpsilon, cosLambda);
    final double delta = asin(sinBeta * cosEpsilon + cosBeta * sinEpsilon * sinLambda);

    return new AlphaDelta(limitTo(toDegrees(alpha), 360), toDegrees(delta));
  }

  private static double calculateTrueObliquityOfEcliptic(
      final JulianDate jd, final double deltaEpsilon) {
    final double epsilon0 = polynomial(jd.julianEphemerisMillennium() / 10.0, OBLIQUITY_COEFFS);
    return epsilon0 / 3600 + deltaEpsilon;
  }

  private static double calculateApparentSiderealTimeAtGreenwich(
      final JulianDate jd, final double deltaPsi, final double epsilonDegrees) {
    final double nu0degrees =
        limitTo(
            280.46061837
                + 360.98564736629 * (jd.julianDate() - 2451545)
                + pow(jd.julianCentury(), 2) * (0.000387933 - jd.julianCentury() / 38710000),
            360);

    return nu0degrees + deltaPsi * cos(toRadians(epsilonDegrees));
  }

  private record DeltaPsiEpsilon(double deltaPsi, double deltaEpsilon) {}

  private static DeltaPsiEpsilon calculateDeltaPsiEpsilon(final double jce, final double[] x) {
    double deltaPsi = 0;
    double deltaEpsilon = 0;

    for (int i = 0; i < TERMS_PE.length; i++) {
      final double xjYtermSum = toRadians(calculateXjYtermSum(i, x));
      final double[] termsPeI = TERMS_PE[i];
      deltaPsi = Math.fma(Math.fma(termsPeI[1], jce, termsPeI[0]), sin(xjYtermSum), deltaPsi);
      deltaEpsilon =
          Math.fma(Math.fma(termsPeI[3], jce, termsPeI[2]), cos(xjYtermSum), deltaEpsilon);
    }

    return new DeltaPsiEpsilon(deltaPsi / 36_000_000, deltaEpsilon / 36_000_000);
  }

  private static double calculateXjYtermSum(final int i, final double[] x) {
    double sum = 0;
    for (int j = 0; j < x.length; j++) {
      sum = Math.fma(x[j], TERMS_Y[i][j], sum);
    }
    return sum;
  }

  private static double[] calculateNutationTerms(final double jce) {
    final double[] x = new double[NUTATION_COEFFS.length];
    for (int i = 0; i < x.length; i++) {
      x[i] = polynomial(jce, NUTATION_COEFFS[i]);
    }
    return x;
  }

  private static double calculateLBRPolynomial(final double jme, final double[][][] termCoeffs) {
    final double[] perPowerSums = new double[termCoeffs.length];

    for (int i = 0; i < termCoeffs.length; i++) {
      double sum = 0;
      for (double[] coeffs : termCoeffs[i]) {
        sum = Math.fma(coeffs[0], cos(coeffs[1] + coeffs[2] * jme), sum);
      }
      perPowerSums[i] = sum;
    }

    return polynomial(jme, perPowerSums) / 1e8;
  }

  private static double lbrToNormalizedDegrees(final double jme, final double[][][] termCoeffs) {
    return limitTo(toDegrees(calculateLBRPolynomial(jme, termCoeffs)), 360);
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
      {25, 3.16, 4690.48},
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
      {6, 4.67, 4690.48},
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
      {2, 3.75, 0.98},
    },
    {
      {289.0, 5.844, 6283.076},
      {35, 0, 0},
      {17, 5.49, 12566.15},
      {3, 5.2, 155.42},
      {1, 4.72, 3.52},
      {1, 5.3, 18849.23},
      {1, 5.97, 242.73},
    },
    {{114.0, 3.142, 0}, {8, 4.13, 6283.08}, {1, 3.84, 12566.15}},
    {{1, 3.14, 0}},
  };

  private static final double[][][] TERMS_B = {
    {
      {280.0, 3.199, 84334.662},
      {102.0, 5.422, 5507.553},
      {80, 3.88, 5223.69},
      {44, 3.7, 2352.87},
      {32, 4, 1577.34},
    },
    {{9, 3.9, 5507.55}, {6, 1.73, 5223.69}},
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
      {26, 4.59, 10447.39},
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
      {9, 0.27, 5486.78},
    },
    {
      {4359.0, 5.7846, 6283.0758},
      {124.0, 5.579, 12566.152},
      {12, 3.14, 0},
      {9, 3.63, 77713.77},
      {6, 1.87, 5573.14},
      {3, 5.47, 18849.23},
    },
    {{145.0, 4.273, 6283.076}, {7, 3.92, 12566.15}},
    {{4, 2.56, 6283.08}},
  };

  private static final double[][] NUTATION_COEFFS = {
    {297.85036, 445267.111480, -0.0019142, 1.0 / 189474},
    {357.52772, 35999.050340, -0.0001603, -1.0 / 300000},
    {134.96298, 477198.867398, 0.0086972, 1.0 / 56250},
    {93.27191, 483202.017538, -0.0036825, 1.0 / 327270},
    {125.04452, -1934.136261, 0.0020708, 1.0 / 450000},
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
    {2, -1, 0, 2, 2},
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
    {-3, 0, 0, 0},
  };

  private static final double[] OBLIQUITY_COEFFS = {
    84381.448, -4680.93, -1.55, 1999.25, 51.38, -249.67, -39.05, 7.12, 27.87, 5.79, 2.45,
  };
}
