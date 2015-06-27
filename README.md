# solarpositioning

This is a Java library for finding topocentric solar coordinates, i.e. the sun’s position on the sky at a given date, latitude, and longitude (and other parameters). Calculations are based on well-known published algorithms: [SPA](http://dx.doi.org/10.1016/j.solener.2003.12.003) by Reda and Andreas and, alternatively, <a href="http://dx.doi.org/10.1016/S0038-092X(00)00156-0">PSA</a> by Blanco-Muriel et al.

## Usage

### Maven coordinates

```xml
<dependency>
    <groupId>net.e175.klaus</groupId>
    <artifactId>solarpositioning</artifactId>
    <version>0.0.6</version> <!-- or whatever latest release is -->
</dependency>
```

Occasional snapshots are deployed to https://oss.sonatype.org/content/repositories/snapshots

### Code

```java
import net.e175.klaus.solarpositioning.*;

public class App {
  public static void main(String[] args) {
    final GregorianCalendar dateTime = new GregorianCalendar();
    final double latitude = 48.21;
    final double longitude = 16.37;

    AzimuthZenithAngle position = SPA.calculateSolarPosition(
                                            dateTime,
                                            latitude,
                                            longitude,
                                            190, // elevation (m)
                                            DeltaT.estimate(dateTime), // delta T (s)
                                            1010, // avg. air pressure (hPa)
                                            11); // avg. air temperature (°C)
    System.out.println("SPA: " + position);
  }
}
```

### Which algorithm should I use?

When in doubt, use SPA. It's widely considered the reference algorithm for solar positioning, being very accurate and usable in a very large time window. Its only downside is that it's relatively slow.

If speed is critical (e.g. you need to calculate lots of positions), consider using PSA. Note however that it's highly optimised for its specified time window (1999-2015), and will be drastically less accurate outside of it.

A fast, yet still accurate alternative would be one of the [Grena/ENEA](http://dx.doi.org/10.1016/j.solener.2012.01.024) algorithms, but that's not implemented yet.

### Is the code thread-safe?

Yes. None of the classes hold any mutable shared state. As the calculation is obviously CPU-bound, explicit multithreading does make sense whenever a lot of positions need to be calculated.

### How do I get the time of sunrise/sunset?

The SPA class now includes a method to calculate the times of sunrise, sun transit, and sunset in one fell swoop:

```java
GregorianCalendar[] res = SPA.calculateSunriseTransitSet(time, 70.978056, 25.974722, 68);
```

Note that the times of sunrise and sunset may be NULL if the sun never sets or rises during the specified day (i.e. polar days and nights).

### What's with this "delta T" thing?

See [Wikipedia](https://en.wikipedia.org/wiki/ΔT) for an explanation. For many simple applications, this value could be negligible as it's just about a minute as of this writing. However, if you're looking for maximum accuracy, you should either use a current observed value (published e.g. by the US Naval Observatory) or at least a solid estimate.

The DeltaT class provides an estimator based on polynomials fitting a number of observed (or extrapolated) historical values, published by [Espenak and Meeus](http://eclipse.gsfc.nasa.gov/SEcat5/deltatpoly.html). Here's a plot of its output compared with some published ΔT data:

![deltat](resources/deltat.png)

