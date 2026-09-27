package net.e175.klaus.solarpositioning;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

/** Inclusive UT and TT limits for a solar model. */
record TimeRange(Instant start, Instant end) {
  TimeRange(int firstYear, int lastYear) {
    this(
        LocalDate.of(firstYear, 1, 1).atStartOfDay(ZoneOffset.UTC).toInstant(),
        LocalDate.of(lastYear + 1, 1, 1).atStartOfDay(ZoneOffset.UTC).toInstant());
  }

  boolean contains(Instant time, double deltaT) {
    return !beforeStart(time, deltaT) && !time.isAfter(end) && deltaT <= secondsBetween(time, end);
  }

  boolean beforeStart(Instant time, double deltaT) {
    return time.isBefore(start) || secondsBetween(start, time) < -deltaT;
  }

  private static double secondsBetween(Instant start, Instant end) {
    // Compare elapsed seconds to delta T to avoid rounding at Julian-date boundaries.
    var duration = Duration.between(start, end);
    return duration.getSeconds() + duration.getNano() / 1e9;
  }
}
