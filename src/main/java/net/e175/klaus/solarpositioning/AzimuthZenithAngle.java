package net.e175.klaus.solarpositioning;

import java.util.Objects;

/** A simple data class for keeping an azimuth/zenith angle pair of values. */
public final class AzimuthZenithAngle {
  private final double azimuth;
  private final double zenithAngle;

  public AzimuthZenithAngle(final double azimuth, final double zenithAngle) {
    this.zenithAngle = zenithAngle;
    this.azimuth = azimuth;
  }

  public double getZenithAngle() {
    return zenithAngle;
  }

  public double getAzimuth() {
    return azimuth;
  }

  @Override
  public String toString() {
    return "AzimuthZenithAngle{"
        + String.format("azimuth=%.6f°", azimuth)
        + String.format(", zenithAngle=%.6f°", zenithAngle)
        + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    AzimuthZenithAngle that = (AzimuthZenithAngle) o;
    return Double.compare(that.azimuth, azimuth) == 0
        && Double.compare(that.zenithAngle, zenithAngle) == 0;
  }

  @Override
  public int hashCode() {
    return Objects.hash(azimuth, zenithAngle);
  }
}
