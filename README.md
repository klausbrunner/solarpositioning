# solarpositioning

This is a Java library for finding topocentric solar coordinates, i.e. the sun’s position on the sky at a given date, latitude, and longitude (and other parameters). Calculations are based on well-known published algorithms: [SPA](http://dx.doi.org/10.1016/j.solener.2003.12.003) by Reda and Andreas and, alternatively, <a href="http://dx.doi.org/10.1016/S0038-092X(00)00156-0">PSA</a> by Blanco-Muriel et al.

## Usage

### Maven coordinates

```xml
<dependency>
    <groupId>net.e175.klaus</groupId>
    <artifactId>solarpositioning</artifactId>
    <version>0.0.5</version> <!-- or whatever latest release is -->
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
                                            68, // delta T (s)
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

See current snapshot code for a "beta quality" implementation.
