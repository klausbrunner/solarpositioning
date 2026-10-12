# solarpositioning

![CI](https://github.com/klausbrunner/solarpositioning/workflows/CI/badge.svg) [![Maven](https://img.shields.io/maven-central/v/net.e175.klaus/solarpositioning?color=dodgerblue)](https://central.sonatype.com/artifact/net.e175.klaus/solarpositioning/)
[![javadoc](https://javadoc.io/badge2/net.e175.klaus/solarpositioning/javadoc.svg)](https://javadoc.io/doc/net.e175.klaus/solarpositioning)

A Java library for finding the Sun's position as seen from a given location, and times of
sunrise, sunset, transit and twilight. It implements [SPA](https://doi.org/10.1016/j.solener.2003.12.003)
by Reda and Andreas and [Grena3](https://doi.org/10.1016/j.solener.2012.01.024) by Grena.
More than 1000 test points validate the results against reference code and other sources.
Solar events are found by searching positions from the chosen model.

> [!NOTE]
> This library is **not** based on or derived from any code published by NREL, ENEA or other parties. It implements the position algorithms as described in the respective papers.

## Usage

Java 17 or newer. No additional runtime dependencies.

```xml
<dependency>
    <groupId>net.e175.klaus</groupId>
    <artifactId>solarpositioning</artifactId>
    <version>[3.0.1,)</version>
</dependency>
```

Version 3 introduced breaking API changes. Older 2.x versions remain available, but will not be maintained.

See the [Javadoc](https://javadoc.io/doc/net.e175.klaus/solarpositioning) for the full API and method contracts.

### Solar position

To get a position with atmospheric refraction correction:

```java
var dateTime = ZonedDateTime.now();
var deltaT = DeltaT.estimate(dateTime);
var positions = SolarPositions.spa();
var position = positions.at(
    dateTime,
    48.21, // latitude (degrees)
    16.37, // longitude (degrees)
    190, // height above sea level (m)
    deltaT, // delta T (s)
    new Atmosphere(1010, 11)); // local pressure (hPa), temperature (°C)

System.out.println(position);
System.out.println(position.elevation());
```

`at` accepts either a `ZonedDateTime` or an `Instant`; angles are in degrees.
Omit the atmosphere for an unrefracted position, and omit height to assume sea level.
Use `SolarPositions.grena3()` for Grena3, which requires zero height.

### Many locations at one time

If you're calculating positions for many locations, prepare the time-dependent work once:

```java
var snapshot = positions.forTime(dateTime, deltaT);
var vienna = snapshot.at(48.21, 16.37, 190);
var berlin = snapshot.at(52.52, 13.41, 34);
```

This works with both models, using the same height and atmosphere options as `at`.
The snapshot also exposes `rightAscension()` and `declination()` in degrees, and
`earthRadiusVector()` as an `OptionalDouble` in astronomical units (SPA only).

### Sunrise, sunset and twilight

`forDate` returns every sunrise, transit and sunset in a local calendar date:

```java
var date = LocalDate.of(2026, 9, 25);
var zone = ZoneId.of("Europe/Vienna");
var calculator = SolarEvents.spa();
var events = calculator.forDate(
    date, zone, 48.21, 16.37, DeltaT.estimate(date));

System.out.println(events.rises());
System.out.println(events.transits());
System.out.println(events.sets());
System.out.println(events.alwaysAbove()); // continuous daylight
System.out.println(events.alwaysBelow()); // continuous night
```

Returned times use the requested zone, including daylight-saving rules. Lists may be empty
or contain several events; a date need not have a rise/set pair or a transit.
Use `SolarEvents.grena3()` to select Grena3.

Pass a `Horizon`, such as `CIVIL_TWILIGHT`, or a numeric elevation for other crossings.
`forDateMultiple` collects several horizons at once. You can also supply your own
position model through `SolarEvents.of` and `PositionProvider`.

For an arbitrary interval, `nextRise`, `nextSet` and `nextTransit` return `Optional<Instant>`.
Searches exclude `start` and include `end`; an empty result means no event in that interval.
Pass a returned instant as the next start to continue searching.

### Next elevation crossing

To find when the Sun's centre reaches a particular elevation with your chosen height and
atmosphere, use `SolarPositions.nextElevationRise` or `nextElevationSet`:

```java
var start = date.atStartOfDay(zone).toInstant();
var end = date.plusDays(1).atStartOfDay(zone).toInstant();
var target = new ElevationTarget(10.0, 190.0, new Atmosphere(1010, 11));
var elevationRise = positions.nextElevationRise(start, end,
    48.21, 16.37, DeltaT.estimate(date), target);
```

These also return `Optional<Instant>` and use the same interval rules. The angle is apparent
with an atmosphere; omit it for geometric elevation. Height changes topocentric parallax,
not the visible horizon. Refraction cutoff jumps count as crossings.

## Practical questions

### Which position algorithm should I use?

- For many applications, Grena3 should work just fine. It's simple, fast, and pretty accurate for a time window from
  2010 to 2110 CE.
- If you're looking for maximum accuracy or need to calculate for historic dates, use SPA. It's widely considered a
  reference algorithm for solar positioning, and supports years -2000 through 6000.

While Grena3 is about an order of magnitude faster than SPA, in absolute terms we are talking about microseconds. The difference mostly matters for bulk calculations.

### How accurate are the event times?

`SolarEvents` searches unrefracted topocentric solar-centre positions at sea level
from the chosen model.
Sunrise and sunset occur when the Sun's centre is 50 arcminutes (about 0.833°) below the
geometric horizon, allowing for average atmospheric refraction and the Sun's apparent radius.
Twilight and custom elevations use exactly the selected geometric angle, without an
additional refraction correction.

The search resolves crossings to one millisecond, but don't expect to observe sunrise that
precisely: weather and terrain can shift it by minutes
([USNO](https://aa.usno.navy.mil/faq/RST_defs)). Shallow crossings also amplify the position
model's angular uncertainty. Transit means meridian passage, not necessarily maximum elevation.

Positions and events use the proleptic Gregorian calendar, including before 1582; UTC
approximates UT1. Both UT and TT must stay within the model's supported years.

### What's this "delta T" thing?

It's the difference between terrestrial time and time based on Earth's rotation (TT − UT1),
in seconds; [Wikipedia](<https://en.wikipedia.org/wiki/ΔT_(timekeeping)>) has the longer explanation.
For most everyday uses, `DeltaT.estimate()` is a sensible starting point. If you're after maximum
accuracy, use an observed value. Event searches hold delta T constant throughout each query.

`DeltaT.estimate()` uses polynomials originally published by [Espenak and Meeus](http://eclipse.gsfc.nasa.gov/SEcat5/deltatpoly.html)
and [updated by Espenak in 2014](https://www.eclipsewise.com/help/deltatpoly2014.html), with custom fits from 2015 onwards
([derivation and comparisons](https://klaus.brunners.name/posts/delta-t-polynomials/)). Future values remain
uncertain, especially beyond 2100.

### Is it thread-safe?

Yes. The calculators and prepared snapshots are immutable and reusable; none of the classes
hold mutable shared state.
