package net.e175.klaus.solarpositioning.test;

import static org.junit.jupiter.api.Assertions.assertThrows;

import net.e175.klaus.solarpositioning.SolarPosition;
import org.junit.jupiter.api.Test;

class SolarPositionTest {

  @Test
  public void rejectsSillyAzimuth() {
    assertThrows(IllegalArgumentException.class, () -> new SolarPosition(-0.1, 90));
    assertThrows(IllegalArgumentException.class, () -> new SolarPosition(360.1, 90));
  }

  @Test
  public void rejectsSillyZenithAngle() {
    assertThrows(IllegalArgumentException.class, () -> new SolarPosition(90, -0.1));
    assertThrows(IllegalArgumentException.class, () -> new SolarPosition(90, 180.1));
  }

  @Test
  public void rejectsNonFiniteValues() {
    assertThrows(IllegalArgumentException.class, () -> new SolarPosition(Double.NaN, 90));
    assertThrows(IllegalArgumentException.class, () -> new SolarPosition(90, Double.NaN));
    assertThrows(
        IllegalArgumentException.class, () -> new SolarPosition(Double.POSITIVE_INFINITY, 90));
    assertThrows(
        IllegalArgumentException.class, () -> new SolarPosition(90, Double.POSITIVE_INFINITY));
  }
}
