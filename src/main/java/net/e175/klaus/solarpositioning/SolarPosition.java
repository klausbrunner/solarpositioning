package net.e175.klaus.solarpositioning;

/**
 * Result type for an azimuth/zenith angle pair of values.
 *
 * @param azimuth Azimuth angle in degrees, measured from North (0°) going eastwards.
 * @param zenithAngle Zenith angle in degrees, measured from zenith (0°) downwards.
 */
public record SolarPosition(double azimuth, double zenithAngle) {
  public SolarPosition {
    if (azimuth < 0 || azimuth > 360) {
      throw new IllegalArgumentException("illegal value %.3f for azimuth".formatted(azimuth));
    }
    if (zenithAngle < 0 || zenithAngle > 180) {
      throw new IllegalArgumentException(
          "illegal value %.3f for zenithAngle".formatted(zenithAngle));
    }
  }
}
