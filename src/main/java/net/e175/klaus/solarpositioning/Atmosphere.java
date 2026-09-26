package net.e175.klaus.solarpositioning;

/**
 * Atmospheric conditions for solar-position refraction correction.
 *
 * @param pressure local pressure in hPa, finite and strictly between 0 and 3000
 * @param temperature temperature in degrees Celsius, finite and strictly between -273 and 273
 */
public record Atmosphere(double pressure, double temperature) {
  /**
   * Validates the conditions against the refraction models' supported input ranges.
   *
   * @throws IllegalArgumentException if pressure or temperature is outside its range
   */
  public Atmosphere {
    if (!MathUtil.checkRefractionParamsUsable(pressure, temperature)) {
      throw new IllegalArgumentException("invalid atmospheric pressure or temperature");
    }
  }
}
