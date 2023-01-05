package net.e175.klaus.solarpositioning;


import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class JulianDateTest {

    private static final double TOLERANCE = 0.0000001;

    @Test
    public void testConstructor() {
        JulianDate julDate = new JulianDate(ZonedDateTime.now());
        assertNotNull(julDate);
    }

    @Test
    public void testWithTimeZone() {
        // 17 October 2003, 12:30:30-07:00
        ZoneId zone = ZoneOffset.ofHours(-7);
        ZonedDateTime time = ZonedDateTime.of(2003, 10, 17, 12, 30, 30, 0, zone);

        JulianDate julDate = new JulianDate(time);

        assertEquals(2452930.312847222, julDate.getJulianDate(), TOLERANCE);
    }

    @Test
    public void testY2K() {
        ZonedDateTime utcTime = ZonedDateTime.of(2000, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC);

        JulianDate julDate = new JulianDate(utcTime);

        assertEquals(2451545.0, julDate.getJulianDate(), TOLERANCE);
    }

    @Test
    public void testPre1000() {
        ZonedDateTime utcTime = ZonedDateTime.of(837, 4, 10, 7, 12, 0, 0, ZoneOffset.UTC);

        JulianDate julDate = new JulianDate(utcTime);

        assertEquals(2026871.8, julDate.getJulianDate(), TOLERANCE);
    }

    @Test
    public void testPre0() {
        ZonedDateTime utcTime = ZonedDateTime.of(-123, 12, 31, 0, 0, 0, 0, ZoneOffset.UTC);

        JulianDate julDate = new JulianDate(utcTime);

        assertEquals(1676496.5, julDate.getJulianDate(), TOLERANCE);
    }

    @Test
    public void testPre02() {
        ZonedDateTime utcTime = ZonedDateTime.of(-122, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

        JulianDate julDate = new JulianDate(utcTime);

        assertEquals(1676497.5, julDate.getJulianDate(), TOLERANCE);
    }

    @Test
    public void testJulian0() {
        ZonedDateTime utcTime = ZonedDateTime.of(-4712, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC);

        JulianDate julDate = new JulianDate(utcTime);

        assertEquals(0.0, julDate.getJulianDate(), TOLERANCE);
    }

    @Test
    public void testJulianDays() {
        JulianDate jd = new JulianDate(2452929.500000, 0);
        assertEquals(0.03790554, jd.getJulianCentury(), TOLERANCE);
        assertEquals(0.00379056, jd.getJulianEphemerisMillennium(), TOLERANCE);
    }

}
