package net.e175.klaus.solarpositioning;

import java.time.ZonedDateTime;
import java.util.Objects;

/** Result types for sunrise/sunset calculations. */
public sealed interface SunriseResult {

  ZonedDateTime transit();

  /**
   * A day with sunrise and sunset.
   *
   * @param sunrise Time of sunrise.
   * @param transit Time of transit (culmination), i.e. when the sun is closest to the zenith.
   * @param sunset Time of sunset.
   */
  record RegularDay(ZonedDateTime sunrise, ZonedDateTime transit, ZonedDateTime sunset)
      implements SunriseResult {
    public RegularDay {
      Objects.requireNonNull(sunrise);
      Objects.requireNonNull(transit);
      Objects.requireNonNull(sunset);
    }
  }

  /**
   * A day on which the sun is above the horizon all the time (polar day).
   *
   * @param transit Time of transit (culmination), i.e. when the sun is closest to the zenith.
   */
  record AllDay(ZonedDateTime transit) implements SunriseResult {
    public AllDay {
      Objects.requireNonNull(transit);
    }
  }

  /**
   * A day on which the sun is below the horizon all the time (polar night).
   *
   * @param transit Time of transit (culmination), i.e. when the sun is closest to the zenith.
   */
  record AllNight(ZonedDateTime transit) implements SunriseResult {
    public AllNight {
      Objects.requireNonNull(transit);
    }
  }
}
