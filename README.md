# solarpositioning

![CI](https://github.com/klausbrunner/solarpositioning/workflows/CI/badge.svg) [![Maven](https://img.shields.io/maven-central/v/net.e175.klaus/solarpositioning?color=dodgerblue)](https://central.sonatype.com/artifact/net.e175.klaus/solarpositioning/)
[![javadoc](https://javadoc.io/badge2/net.e175.klaus/solarpositioning/javadoc.svg)](https://javadoc.io/doc/net.e175.klaus/solarpositioning)

A Java library for finding topocentric solar coordinates, i.e. the sun’s position on the sky for a given date,
latitude, and longitude (and other parameters), as well as times of sunrise and sunset. Calculations are based on
well-known, peer-reviewed algorithms: [SPA](http://dx.doi.org/10.1016/j.solener.2003.12.003) by Reda and Andreas and,
alternatively, [Grena/ENEA](http://dx.doi.org/10.1016/j.solener.2012.01.024) by Grena. More than 1000 test points are
included to validate against the reference code and other sources.

A command-line application using this library is available as [solarpos](https://github.com/klausbrunner/solarpos).

## Usage

### Maven coordinates

```xml

<dependency>
    <groupId>net.e175.klaus</groupId>
    <artifactId>solarpositioning</artifactId>
    <version>2.0.0</version>
</dependency>
```

### Requirements

Java 17 or newer. No additional runtime dependencies.

(Still stuck on old Java? Use version `0.1.10` of this library, which requires Java 8 only.)

### Code

The API is intentionally "flat", comprising a handful of static methods and simple record classes.
To get refraction-corrected topocentric coordinates:

```java
import net.e175.klaus.solarpositioning.*;

import java.time.ZonedDateTime;

public class App {
    public static void main(String[] args) {
        ZonedDateTime dateTime = new ZonedDateTime.now();

        // replace SPA with Grena3 as needed
        var position = SPA.calculateSolarPosition(
                dateTime,
                48.21, // latitude (degrees)
                16.37, // longitude (degrees)
                190, // elevation (m)
                DeltaT.estimate(dateTime.toLocalDate()), // delta T (s)
                1010, // avg. air pressure (hPa)
                11); // avg. air temperature (°C)

        System.out.println(position);
    }
}
```

The SPA class includes a method to calculate the times of sunrise, sun transit, and sunset in one fell swoop:

```java
var result=SPA.calculateSunriseTransitSet(
        dateTime,
        70.978, // latitude  
        25.974, // longitude
        69); // delta T
```

Twilight start and end times can be obtained like sunrise and sunset, but assuming a different horizon:

```java
var result=SPA.calculateSunriseTransitSet(
        dateTime,
        70.978, // latitude  
        25.974, // longitude
        69, // delta T
        SPA.Horizon.CIVIL_TWILIGHT); 
```

See the Javadoc for more methods.

### Which position algorithm should I use?

* For many applications, Grena3 should work just fine. It's simple, fast, and pretty accurate for a time window from
  2010 to 2110 CE.
* If you're looking for maximum accuracy or need to calculate for historic dates, use SPA. It's widely considered a
  reference algorithm for solar positioning, being very accurate and usable in a very large time window. Its only
  downside is that it's relatively slow.

### Notes on sunrise, sunset, and twilight

* Calculation is based on the usual correction of 0.833° on the zenith angle, i.e. sunrise and sunset are assumed to
  occur when the center of the solar disc is 50 arc-minutes below the horizon.
* As a general note on accuracy, Jean Meeus advises that "giving rising or setting times .. more accurately than to the
  nearest minute makes no sense" (_Astronomical Algorithms_). Errors increase the farther the position from the equator,
  i.e. values for polar regions are much less reliable.
* The SPA sunset/sunrise algorithm is one of the most accurate ones around. Results of this implementation correspond
  very closely to the [NOAA calculator](http://www.esrl.noaa.gov/gmd/grad/solcalc/)'s, with maximum differences of just a
  few seconds even for polar regions.

### What's this "delta T" thing?

See [Wikipedia](https://en.wikipedia.org/wiki/ΔT_(timekeeping)) for an explanation. For many simple applications, this
value could be
negligible as it's just over a minute (about 70 seconds) as of this writing. However, if you're looking for maximum
accuracy, you should either use a current observed value (available from e.g. the US Naval Observatory) or at least a
solid estimate.

The DeltaT class provides an estimator based on polynomials fitting a number of observed (or extrapolated) historical
values, published by [Espenak and Meeus](http://eclipse.gsfc.nasa.gov/SEcat5/deltatpoly.html) in 2007 and slightly
updated by [Espenak](https://www.eclipsewise.com/help/deltatpoly2014.html) in 2014. Here's a plot of its output compared
with some published ΔT data:

![deltat](resources/deltat.png)

### Is the code thread-safe?

Yes. None of the classes hold any mutable shared state.
