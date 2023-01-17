package net.e175.klaus.solarpositioning;

import java.time.LocalDate;

import static java.lang.Math.pow;

/**
 * Finding values for Delta T, the difference between Terrestrial Time (TT) and Universal Time (UT1).
 */
public final class DeltaT {
    private DeltaT() {
    }

    /**
     * Estimate Delta T for the given date. This is based on Espenak and Meeus, "Five Millennium Canon of
     * Solar Eclipses: -1999 to +3000" (NASA/TP-2006-214141) and updated by Espenak in 2014 at
     * <a href="https://www.eclipsewise.com/help/deltatpoly2014.html">Eclipsewise</a>.
     *
     * @param forDate date and time
     * @return estimated delta T value (seconds)
     */
    public static double estimate(final LocalDate forDate) {
        final double year = decimalYear(forDate);
        final double deltaT;

        if (year < -500) {
            double u = (year - 1820) / 100;
            deltaT = -20 + 32 * pow(u, 2);
        } else if (year < 500) {
            double u = year / 100;
            deltaT = 10583.6 - 1014.41 * u + 33.78311 * pow(u, 2) - 5.952053 * pow(u, 3)
                    - 0.1798452 * pow(u, 4) + 0.022174192 * pow(u, 5) + 0.0090316521 * pow(u, 6);
        } else if (year < 1600) {
            double u = (year - 1000) / 100;
            deltaT = 1574.2 - 556.01 * u + 71.23472 * pow(u, 2) + 0.319781 * pow(u, 3)
                    - 0.8503463 * pow(u, 4) - 0.005050998 * pow(u, 5) + 0.0083572073 * pow(u, 6);
        } else if (year < 1700) {
            double t = year - 1600;
            deltaT = 120 - 0.9808 * t - 0.01532 * pow(t, 2) + pow(t, 3) / 7129;
        } else if (year < 1800) {
            double t = year - 1700;
            deltaT = 8.83 + 0.1603 * t - 0.0059285 * pow(t, 2) + 0.00013336 * pow(t, 3) - pow(t, 4) / 1174000;
        } else if (year < 1860) {
            double t = year - 1800;
            deltaT = 13.72 - 0.332447 * t + 0.0068612 * pow(t, 2) + 0.0041116 * pow(t, 3) - 0.00037436 * pow(t, 4)
                    + 0.0000121272 * pow(t, 5) - 0.0000001699 * pow(t, 6) + 0.000000000875 * pow(t, 7);
        } else if (year < 1900) {
            double t = year - 1860;
            deltaT = 7.62 + 0.5737 * t - 0.251754 * pow(t, 2) + 0.01680668 * pow(t, 3)
                    - 0.0004473624 * pow(t, 4) + pow(t, 5) / 233174;
        } else if (year < 1920) {
            double t = year - 1900;
            deltaT = -2.79 + 1.494119 * t - 0.0598939 * pow(t, 2) + 0.0061966 * pow(t, 3) - 0.000197 * pow(t, 4);
        } else if (year < 1941) {
            double t = year - 1920;
            deltaT = 21.20 + 0.84493 * t - 0.076100 * pow(t, 2) + 0.0020936 * pow(t, 3);
        } else if (year < 1961) {
            double t = year - 1950;
            deltaT = 29.07 + 0.407 * t - pow(t, 2) / 233 + pow(t, 3) / 2547;
        } else if (year < 1986) {
            double t = year - 1975;
            deltaT = 45.45 + 1.067 * t - pow(t, 2) / 260 - pow(t, 3) / 718;
        } else if (year < 2005) {
            double t = year - 2000;
            deltaT = 63.86 + 0.3345 * t - 0.060374 * pow(t, 2) + 0.0017275 * pow(t, 3) + 0.000651814 * pow(t, 4)
                    + 0.00002373599 * pow(t, 5);
        } else if (year < 2015) {
            double t = year - 2005;
            deltaT = 64.69 + 0.2930 * t;
        } else if (year <= 3000) {
            double t = year - 2015;
            deltaT = 67.62 + 0.3645 * t + 0.0039755 * pow(t, 2);
        } else {
            throw new IllegalArgumentException("no estimates possible for this time");
        }

        return deltaT;
    }

    private static double decimalYear(LocalDate forDate) {
        return forDate.getYear() + (forDate.getMonthValue() - 0.5) / 12;
    }

}
