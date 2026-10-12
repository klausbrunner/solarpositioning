package net.e175.klaus.solarpositioning;

import java.util.Objects;
import java.util.Optional;

/**
 * Solar-centre elevation and observation settings for a bounded crossing search.
 *
 * <p>Matches the elevation returned by {@link SolarPositions} with these settings. Height changes
 * topocentric parallax; it does not lower the visible horizon.
 *
 * @param angle elevation in degrees, from -90 to 90; geometric without an atmosphere, apparent with
 *     one
 * @param height observer height above sea level in metres; may be negative, but must be zero for
 *     Grena3
 * @param atmosphere atmospheric correction, or empty for geometric elevation
 */
public record ElevationTarget(double angle, double height, Optional<Atmosphere> atmosphere) {
  /**
   * Validates the target's angle, height and optional atmosphere.
   *
   * @throws IllegalArgumentException for an invalid angle or non-finite height
   * @throws NullPointerException if atmosphere is null
   */
  public ElevationTarget {
    MathUtil.checkElevationAngle(angle);
    if (!Double.isFinite(height)) {
      throw new IllegalArgumentException("height must be finite");
    }
    Objects.requireNonNull(atmosphere, "atmosphere");
  }

  /** Creates a target without refraction correction. */
  public ElevationTarget(double angle, double height) {
    this(angle, height, Optional.empty());
  }

  /** Creates a target with refraction correction for the given atmosphere. */
  public ElevationTarget(double angle, double height, Atmosphere atmosphere) {
    this(angle, height, Optional.of(atmosphere));
  }
}
