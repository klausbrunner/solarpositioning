package net.e175.klaus.solarpositioning;

import java.time.ZonedDateTime;

/** Result types for sunrise/sunset calculations. */
public sealed interface SunriseResult {

  ZonedDateTime transit();

  /**
   * Result type for a day with sunrise and sunset.
   *
   * @param sunrise Time of sunrise.
   * @param transit Time of transit (culmination), i.e. when the sun is closest to the zenith.
   * @param sunset Time of sunset.
   */
  record RegularDay(ZonedDateTime sunrise, ZonedDateTime transit, ZonedDateTime sunset)
      implements SunriseResult {}

  /**
   * A day on which the sun is above the horizon all the time (polar day).
   *
   * @param transit Time of transit (culmination), i.e. when the sun is closest to the zenith.
   */
  record AllDay(ZonedDateTime transit) implements SunriseResult {}

  /**
   * A day on which the sun is below the horizon all the time (polar night).
   *
   * @param transit Time of transit (culmination), i.e. when the sun is closest to the zenith.
   */
  record AllNight(ZonedDateTime transit) implements SunriseResult {}
}
