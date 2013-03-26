solarpositioning
================

This is a Java library containing algorithms for finding the sun’s position on the sky for a given date and latitude and longitude (and other parameters). Currently, the [PSA](http://dx.doi.org/10.1016/S0038-092X(00)00156-0) algorithm by Blanco-Muriel et al. and the [SPA](http://dx.doi.org/10.1016/j.solener.2003.12.003) algorithm by Reda and Andreas are included.

Usage
-----

```java
import net.e175.klaus.solarpositioning.*;

public class App {
  public static void main(String[] args) {
    final GregorianCalendar dateTime = new GregorianCalendar();
    final double latitude = 48.21;
    final double longitude = 16.37;

    AzimuthZenithAngle position = PSA.calculateSolarPosition(dateTime,
                                                             latitude,
                                                             longitude);
    System.out.println("PSA: " + position);


    position = SPA.calculateSolarPosition(dateTime,
                                          latitude,
                                          longitude,
                                          190, // elevation
                                          67, // delta T
                                          1010, // avg. air pressure
                                          11); // avg. air temperature
    System.out.println("SPA: " + position);
  }
}
```

Which algorithm should I use?
-----------------------------

When in doubt, use SPA. It's widely considered the reference algorithm for solar positioning, being very accurate and usable in a very large time window. Its only downside is that it's relatively slow.

If speed is critical (e.g. you need to calculate lots of positions), consider using PSA. Note however that it's highly optimised for its specified time window (1999-2005), and will be drastically less accurate outside of it.

A fast, yet still accurate alternative would be the [Grena/ENEA](http://dx.doi.org/10.1016/j.solener.2012.01.024) algorithm, but that's not implemented yet.

How do I get the time of sunrise/sunset?
----------------------------------------

Not implemented yet. (Of course you could just search for the time when the zenith angle is 90° by calculating for several times, but that's neither efficient nor elegant.)
