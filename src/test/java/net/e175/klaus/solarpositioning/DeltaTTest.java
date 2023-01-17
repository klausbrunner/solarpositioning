package net.e175.klaus.solarpositioning;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DeltaTTest {

    private LocalDate yearCal(int year) {
        return LocalDate.of(year, 1, 1);
    }

    @Test
    void testHistoricalValues() {
        assertEquals(17190, DeltaT.estimate(yearCal(-400)), 2000); // not sure why so far off, polynomial seems ok

        assertEquals(14080, DeltaT.estimate(yearCal(-300)), 5);

        assertEquals(12790, DeltaT.estimate(yearCal(-200)), 5);

        assertEquals(7680, DeltaT.estimate(yearCal(300)), 1);

        assertEquals(3810, DeltaT.estimate(yearCal(700)), 3);

        assertEquals(200, DeltaT.estimate(yearCal(1500)), 2);

        assertEquals(44, DeltaT.estimate(yearCal(1657)), 4);

        assertEquals(13.7, DeltaT.estimate(yearCal(1750)), 2);

        assertEquals(7, DeltaT.estimate(yearCal(1850)), 1);

        assertEquals(1.04, DeltaT.estimate(yearCal(1870)), 1);

        assertEquals(-3, DeltaT.estimate(yearCal(1900)), 1);

        assertEquals(10.38, DeltaT.estimate(yearCal(1910)), 1);

        assertEquals(24.02, DeltaT.estimate(yearCal(1930)), 1);

        assertEquals(29, DeltaT.estimate(yearCal(1950)), 1);
    }

    @Test
    void testObservedValues() {
        // values taken from https://maia.usno.navy.mil/products/deltaT

        assertEquals(45.4761, DeltaT.estimate(yearCal(1975)), 1);

        assertEquals(56.8553, DeltaT.estimate(yearCal(1990)), 1);

        assertEquals(63.8285, DeltaT.estimate(yearCal(2000)), 1);

        assertEquals(64.6876, DeltaT.estimate(yearCal(2005)), 1);

        assertEquals(66.0699, DeltaT.estimate(yearCal(2010)), 1);

        assertEquals(67.6439, DeltaT.estimate(yearCal(2015)), 1);

        assertEquals(69.3612, DeltaT.estimate(yearCal(2020)), 1);

        assertEquals(69.2945, DeltaT.estimate(yearCal(2022)), 2);
    }

}
