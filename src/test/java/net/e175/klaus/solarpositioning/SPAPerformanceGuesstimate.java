package net.e175.klaus.solarpositioning;

import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * As the name says, this is merely a guesstimate to get a rough idea of SPA's performance. Like all
 * microbenchmarks on the JVM, its results will be distorted by garbage collection, JIT compilation,
 * and other JVM optimisations.
 */
public class SPAPerformanceGuesstimate {

    public static void main(final String[] args) {
        final long numIterations = 1000000;
        final GregorianCalendar cal = new GregorianCalendar();

        System.out.printf("calculating %d positions...%n", numIterations);

        final long startTimeNanos = System.nanoTime();
        for (int i = 0; i < numIterations; i++) {
            AzimuthZenithAngle result = SPA.calculateSolarPosition(cal, 39.742476, -105.1786, 1830.14, 67, 820, 11);
            cal.add(Calendar.SECOND, (int) result.getZenithAngle());
        }
        final double elapsedTimeSeconds = (System.nanoTime() - startTimeNanos) / 1e9;

        System.out.printf("elapsed time: %.3f s%n", elapsedTimeSeconds);
        System.out.printf("throughput: %.1f positions / s%n", numIterations / elapsedTimeSeconds);
        System.out.printf("mean time per position: %.3f µs", elapsedTimeSeconds * 1e6 / numIterations);
    }
}
