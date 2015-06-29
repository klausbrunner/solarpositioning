package net.e175.klaus.solarpositioning;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * Compute sun position for a given date/time and longitude/latitude.
 *
 * This is a simple Java port of the "PSA" solar positioning algorithm, as documented in:
 *
 * Blanco-Muriel et al.: Computing the Solar Vector. Solar Energy Vol 70 No 5 pp 431-441.
 * http://dx.doi.org/10.1016/S0038-092X(00)00156-0
 *
 * According to the paper, "The algorithm allows .. the true solar vector to be determined with an accuracy of 0.5
 * minutes of arc for the period 1999–2015."
 *
 * @author Klaus A. Brunner
 * @deprecated PSA shouldn't be used after the year 2015.
 */
public final class PSA {

    private static final double D_EARTH_MEAN_RADIUS = 6371.01; // in km
    private static final double D_ASTRONOMICAL_UNIT = 149597890; // in km

    private static final double PI = Math.PI;
    private static final double TWOPI = (2 * PI);
    private static final double RAD = (PI / 180);

    private PSA() {
    }

    /**
     * Calculate sun position for a given time and location.
     *
     * @param date      Note that it's unclear how well the algorithm performs before the year 1990 or after the year 2015.
     * @param latitude  in degrees (positive east of Greenwich)
     * @param longitude in degrees (positive north of equator)
     * @return Topocentric solar position (azimuth measured eastward from north)
     */
    public static AzimuthZenithAngle calculateSolarPosition(final GregorianCalendar date, final double latitude, final double longitude) {
        final Calendar utcTime = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        utcTime.setTimeInMillis(date.getTimeInMillis());

        // Main variables
        double dElapsedJulianDays;
        double dDecimalHours;
        double dEclipticLongitude;
        double dEclipticObliquity;
        double dRightAscension;
        double dDeclination;

        // Auxiliary variables
        double dY;
        double dX;

        // Calculate difference in days between the current Julian Day
        // and JD 2451545.0, which is noon 1 January 2000 Universal Time

        {
            long liAux1;
            long liAux2;
            double dJulianDate;
            // Calculate time of the day in UT decimal hours
            dDecimalHours = utcTime.get(Calendar.HOUR_OF_DAY)
                    + (utcTime.get(Calendar.MINUTE) + utcTime.get(Calendar.SECOND) / 60.0) / 60.0;
            // Calculate current Julian Day
            liAux1 = (utcTime.get(Calendar.MONTH) + 1 - 14) / 12;
            liAux2 = (1461 * (utcTime.get(Calendar.YEAR) + 4800 + liAux1)) / 4
                    + (367 * (utcTime.get(Calendar.MONTH) + 1 - 2 - 12 * liAux1)) / 12
                    - (3 * ((utcTime.get(Calendar.YEAR) + 4900 + liAux1) / 100)) / 4
                    + utcTime.get(Calendar.DAY_OF_MONTH) - 32075;
            dJulianDate = (liAux2) - 0.5 + dDecimalHours / 24.0;
            // Calculate difference between current Julian Day and JD 2451545.0
            dElapsedJulianDays = dJulianDate - 2451545.0;
        }

        // Calculate ecliptic coordinates (ecliptic longitude and obliquity of the
        // ecliptic in radians but without limiting the angle to be less than 2*Pi
        // (i.e., the result may be greater than 2*Pi)
        {
            double dMeanLongitude;
            double dMeanAnomaly;
            double dOmega;
            dOmega = 2.1429 - 0.0010394594 * dElapsedJulianDays;
            dMeanLongitude = 4.8950630 + 0.017202791698 * dElapsedJulianDays; // Radians
            dMeanAnomaly = 6.2400600 + 0.0172019699 * dElapsedJulianDays;
            dEclipticLongitude = dMeanLongitude + 0.03341607 * Math.sin(dMeanAnomaly) + 0.00034894
                    * Math.sin(2 * dMeanAnomaly) - 0.0001134 - 0.0000203 * Math.sin(dOmega);
            dEclipticObliquity = 0.4090928 - 6.2140e-9 * dElapsedJulianDays + 0.0000396 * Math.cos(dOmega);
        }

        // Calculate celestial coordinates ( right ascension and declination ) in radians
        // but without limiting the angle to be less than 2*Pi (i.e., the result
        // may be greater than 2*Pi)
        {
            double dSinEclipticLongitude;
            dSinEclipticLongitude = Math.sin(dEclipticLongitude);
            dY = Math.cos(dEclipticObliquity) * dSinEclipticLongitude;
            dX = Math.cos(dEclipticLongitude);
            dRightAscension = Math.atan2(dY, dX);
            if (dRightAscension < 0.0) {
                dRightAscension = dRightAscension + 2 * Math.PI;
            }
            dDeclination = Math.asin(Math.sin(dEclipticObliquity) * dSinEclipticLongitude);
        }

        // Calculate local coordinates ( azimuth and zenith angle ) in degrees
        {
            double dGreenwichMeanSiderealTime;
            double dLocalMeanSiderealTime;
            double dLatitudeInRadians;
            double dHourAngle;
            double dCosLatitude;
            double dSinLatitude;
            double dCosHourAngle;
            double dParallax;
            dGreenwichMeanSiderealTime = 6.6974243242 + 0.0657098283 * dElapsedJulianDays + dDecimalHours;
            dLocalMeanSiderealTime = (dGreenwichMeanSiderealTime * 15 + longitude) * RAD;
            dHourAngle = dLocalMeanSiderealTime - dRightAscension;
            dLatitudeInRadians = latitude * RAD;
            dCosLatitude = Math.cos(dLatitudeInRadians);
            dSinLatitude = Math.sin(dLatitudeInRadians);
            dCosHourAngle = Math.cos(dHourAngle);
            double zenithAngle = (Math.acos(dCosLatitude * dCosHourAngle * Math.cos(dDeclination)
                    + Math.sin(dDeclination) * dSinLatitude));
            dY = -Math.sin(dHourAngle);
            dX = Math.tan(dDeclination) * dCosLatitude - dSinLatitude * dCosHourAngle;
            double azimuth = Math.atan2(dY, dX);
            if (azimuth < 0.0) {
                azimuth = azimuth + TWOPI;
            }
            azimuth = azimuth / RAD;
            // Parallax Correction
            dParallax = (D_EARTH_MEAN_RADIUS / D_ASTRONOMICAL_UNIT) * Math.sin(zenithAngle);
            zenithAngle = (zenithAngle + dParallax) / RAD;

            return new AzimuthZenithAngle(azimuth, zenithAngle);
        }

    }

}