package net.e175.klaus.solarpositioning;

/**
 * A simple data class for keeping an azimuth/zenith angle pair of values.
 *
 * @param azimuth Azimuth angle in degrees, measured from North (0°) going eastwards.
 * @param zenithAngle Zenith angle in degrees, measured from zenith (0°) downwards.
 */
public record SolarPosition(double azimuth, double zenithAngle) {}
