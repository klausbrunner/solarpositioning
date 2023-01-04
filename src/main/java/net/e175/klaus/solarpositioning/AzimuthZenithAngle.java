package net.e175.klaus.solarpositioning;

/**
 * A simple data class for keeping an azimuth/zenith angle pair of values.
 */
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
        return "AzimuthZenithAngle{" +
                String.format("azimuth=%.6f°", azimuth) +
                String.format(", zenithAngle=%.6f°", zenithAngle) +
                '}';
    }
}
