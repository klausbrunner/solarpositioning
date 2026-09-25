# solarpositioning

![CI](https://github.com/klausbrunner/solarpositioning/workflows/CI/badge.svg) [![Maven](https://img.shields.io/maven-central/v/net.e175.klaus/solarpositioning?color=dodgerblue)](https://central.sonatype.com/artifact/net.e175.klaus/solarpositioning/)
[![javadoc](https://javadoc.io/badge2/net.e175.klaus/solarpositioning/javadoc.svg)](https://javadoc.io/doc/net.e175.klaus/solarpositioning)

A Java library for finding topocentric solar coordinates, i.e. the sun’s position on the sky for a given date,
latitude, and longitude (and other parameters), as well as times of sunrise and sunset. Calculations strictly follow
well-known, peer-reviewed algorithms: [SPA](http://dx.doi.org/10.1016/j.solener.2003.12.003) by Reda and Andreas and,
alternatively, [Grena/ENEA](http://dx.doi.org/10.1016/j.solener.2012.01.024) by Grena. More than 1000 test points are
included to validate against the reference code and other sources.

> [!NOTE]
> This library is **not** based on or derived from any code published by NREL, ENEA or other parties. It implements the algorithms as described in the respective papers, with minimal adjustments documented below.

## Usage

### Maven coordinates

```xml
<dependency>
    <groupId>net.e175.klaus</groupId>
    <artifactId>solarpositioning</artifactId>
    <version>2.1.2</version>
</dependency>
```

### Requirements

Java 17 or newer. No additional runtime dependencies.

### Code

The API is intentionally "flat", comprising a handful of static methods and simple records as results.
To get refraction-corrected topocentric coordinates:

```java
var dateTime = ZonedDateTime.now();

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
```

The SPA class includes methods to calculate the times of sunrise, sun transit, and sunset in one fell swoop. The actual 
return type depends on the type of day (regular day, polar day, polar night).

```java
var result=SPA.calculateSunriseTransitSet(
        dateTime,
        70.978, // latitude  
        25.974, // longitude
        69); // delta T

if(result instanceof SunriseResult.RegularDay regular) {
    System.out.println(regular);
} else {
    System.out.println("no sunrise or sunset today!");    
}
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

For bulk position processing at a fixed time with many coordinates using SPA, use the optimized split methods for significantly better performance:

```java
// Compute time-dependent parts once
final var timeDependent = SPA.calculateSpaTimeDependentParts(dateTime, deltaT);

// Reuse for multiple coordinates (up to 10x faster)
for(var coordinate: coordinates) {
    var position = SPA.calculateSolarPositionWithTimeDependentParts(
        coordinate.lat, coordinate.lon, coordinate.elevation, timeDependent);
}
```
See the Javadoc for more methods.

### Which position algorithm should I use?

* For many applications, Grena3 should work just fine. It's simple, fast, and pretty accurate for a time window from
  2010 to 2110 CE.
* If you're looking for maximum accuracy or need to calculate for historic dates, use SPA. It's widely considered a
  reference algorithm for solar positioning, being very accurate and usable in a very large time window.

While Grena3 is about an order of magnitude faster than SPA, in absolute terms we are talking about microseconds. The difference
mostly matters for bulk calculations.

### Sunrise/sunset accuracy notes

- Sunrise and sunset use the standard solar-centre elevation of −0.833° (50 arcminutes below the geometric horizon), accounting for average atmospheric refraction and the Sun's apparent radius.
- Atmospheric variability limits the accuracy of predicted observed sunrise/sunset times: differences of a minute or more are possible, especially where the Sun crosses the horizon at a shallow angle ([USNO](https://aa.usno.navy.mil/faq/RST_defs)).
- SPA's sunrise/sunset and twilight calculations become less reliable near seasonal transitions where the Sun barely crosses the selected horizon.
- Days with only a rising or setting event are not reliably supported by SPA.

#### Difference in SPA day wrapping

The API selects the transit closest to 12:00 on the requested date's local clock (earlier on a tie). Transit can fall on an adjacent date; the result describes one solar cycle, not all events in a civil day.

Unlike SPA Appendix A.2.7, this library retains sunrise and sunset estimates’ day offsets around the selected transit instead of wrapping them independently into [0, 1). This avoids using the wrong day’s solar coordinates.

### What's this "delta T" thing?

See [Wikipedia](https://en.wikipedia.org/wiki/ΔT_(timekeeping)) for an explanation. For many simple applications, and particularly for sunrise and sunset,
this value could be negligible as it's just over a minute (about 70 seconds) as of this writing. However, if you're 
looking for maximum accuracy, you should use an observed value (available from e.g. the US Naval 
Observatory) or at least a solid estimate.

`DeltaT.estimate()` uses polynomials originally published by [Espenak and Meeus](http://eclipse.gsfc.nasa.gov/SEcat5/deltatpoly.html)
and [updated by Espenak in 2014](https://www.eclipsewise.com/help/deltatpoly2014.html), with custom replacement branches from 2015 onwards. The [derivation and comparisons](https://klaus.brunners.name/posts/delta-t-polynomials/) describe the fit and its
limitations. Future values remain uncertain, and extrapolation beyond 2100 is particularly speculative.

### Is it thread-safe?

Yes. None of the classes hold any mutable shared state.
