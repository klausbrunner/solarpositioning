package net.e175.klaus.solarpositioning;

final class MathUtil {
    private MathUtil() {
    }

    static double polynomial(double x, double coeff0, double coeff1, double coeff2, double coeff3) {
        return ((coeff3 * x + coeff2) * x + coeff1) * x + coeff0;
    }

    static double polynomial(double x, double coeff0, double coeff1, double coeff2) {
        return (coeff2 * x + coeff1) * x + coeff0;
    }

    static double polynomial(double x, double coeff0, double coeff1) {
        return coeff1 * x + coeff0;
    }

    static double polynomial(double x, double... coeffs) {
        int n = coeffs.length - 1;
        double sum = coeffs[n];
        for (int i = n - 1; i >= 0; i--) {
            sum = coeffs[i] + (x * sum);
        }
        return sum;
    }
}
