package net.e175.klaus.solarpositioning;

import static java.lang.Math.*;

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
final class Grena3 {

  private Grena3() {}

  record TimeDependent(double siderealTime, double alpha, double sinDelta, double cosDelta) {}

  static SolarPosition calculateSolarPositionWithTimeDependentParts(
      double latitude, double longitude, double pressure, double temperature, TimeDependent parts) {
    MathUtil.checkLatLonRange(latitude, longitude);
    var position = position(parts, latitude, longitude);
    final double eP = position.elevationRadians();
    final double gamma = position.azimuthRadians();

    // refraction correction (disabled for silly parameter values)
    final boolean doCorrect =
        MathUtil.checkRefractionParamsUsable(pressure, temperature) && eP > 0.0;

    final double deltaRe =
        doCorrect
            ? (0.08422 * (pressure / 1000))
                / ((273.0 + temperature) * tan(eP + 0.003138 / (eP + 0.08919)))
            : 0.0;

    final double z = PI / 2 - eP - deltaRe;

    return new SolarPosition(MathUtil.limitTo(toDegrees(gamma + PI), 360.0), toDegrees(z));
  }

  static SolarEvents.Position eventPosition(JulianDate time, double latitude, double longitude) {
    var parts = calculateTimeDependentParts(time);
    var position = position(parts, latitude, longitude);
    return new SolarEvents.Position(
        toDegrees(position.elevationRadians()), toDegrees(position.hourAngleRadians()));
  }

  // Unrefracted angles in radians; azimuth is measured westwards from south.
  private record Position(
      double elevationRadians, double azimuthRadians, double hourAngleRadians) {}

  static TimeDependent calculateTimeDependentParts(JulianDate time) {
    // Continuous days from Grena's epoch (2060-01-01), avoiding the rounded calendar formula.
    final double t = time.julianDate() - 2473459.5;
    final double tE = t + 1.1574e-5 * time.deltaT();
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

    final double sDelta = sin(delta);
    final double cDelta = sqrt(1 - sDelta * sDelta);
    return new TimeDependent(1.7528311 + 6.300388099 * t, alpha, sDelta, cDelta);
  }

  private static Position position(TimeDependent parts, double latitude, double longitude) {
    double H = parts.siderealTime() + toRadians(longitude) - parts.alpha();
    H = ((H + PI) % (2 * PI)) - PI;
    if (H < -PI) {
      H += 2 * PI;
    }

    // end of "short procedure"
    final double sPhi = sin(toRadians(latitude));
    final double cPhi = sqrt(1 - sPhi * sPhi);
    final double sDelta = parts.sinDelta();
    final double cDelta = parts.cosDelta();
    final double sH = sin(H);
    final double cH = cos(H);

    // Roundoff can put the sine just outside [-1, 1] at the zenith or nadir.
    final double sEpsilon0 = max(-1.0, min(1.0, sPhi * sDelta + cPhi * cDelta * cH));
    final double eP = asin(sEpsilon0) - 4.26e-5 * sqrt(1.0 - sEpsilon0 * sEpsilon0);
    final double gamma = atan2(sH, cH * sPhi - (sDelta * cPhi) / cDelta);

    return new Position(eP, gamma, H);
  }
}
