package net.e175.klaus.solarpositioning;

final class MathUtil {
  private MathUtil() {}

  static double polynomial(double x, double... coeffs) {
    int n = coeffs.length - 1;
    double sum = coeffs[n];
    for (int i = n - 1; i >= 0; i--) {
      sum = Math.fma(x, sum, coeffs[i]);
    }
    return sum;
  }

  static void checkLatLonRange(double latitude, double longitude) {
    if (latitude < -90.0 || latitude > 90.0 || longitude < -180.0 || longitude > 180.0) {
      throw new IllegalArgumentException("latitude/longitude out of range");
    }
  }

  static boolean checkRefractionParamsUsable(double pressure, double temperature) {
    return Double.isFinite(pressure)
        && Double.isFinite(temperature)
        && pressure > 0.0
        && pressure < 3000.0
        && temperature > -273
        && temperature < 273;
  }
}
