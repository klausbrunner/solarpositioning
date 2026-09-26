# solarpositioning

![CI](https://github.com/klausbrunner/solarpositioning/workflows/CI/badge.svg) [![Maven](https://img.shields.io/maven-central/v/net.e175.klaus/solarpositioning?color=dodgerblue)](https://central.sonatype.com/artifact/net.e175.klaus/solarpositioning/)
[![javadoc](https://javadoc.io/badge2/net.e175.klaus/solarpositioning/javadoc.svg)](https://javadoc.io/doc/net.e175.klaus/solarpositioning)

A Java library for finding topocentric solar coordinates, i.e. the sun’s position on the sky for a given date,
latitude, and longitude (and other parameters), as well as times of sunrise, transit, and sunset. Position calculations follow
well-known, peer-reviewed algorithms: [SPA](http://dx.doi.org/10.1016/j.solener.2003.12.003) by Reda and Andreas and,
alternatively, [Grena/ENEA](http://dx.doi.org/10.1016/j.solener.2012.01.024) by Grena. More than 1000 test points are
included to validate against the reference code and other sources. Solar events are found by searching positions from the chosen algorithm, with SPA as the default.

> [!NOTE]
> This library is **not** based on or derived from any code published by NREL, ENEA or other parties. It implements the position algorithms as described in the respective papers.

## Usage

### Maven coordinates

```xml
<dependency>
    <groupId>net.e175.klaus</groupId>
    <artifactId>solarpositioning</artifactId>
    <version>3.0.0</version>
</dependency>
```

Note that version 3 introduced major breaking changes in the API. Older 2.x versions remain available for download, but will not be maintained.

### Requirements

Java 17 or newer. No additional runtime dependencies.

### Code

`SolarPositions` and `SolarEvents` are immutable, reusable calculators. Both default to SPA and return simple records.
To get refraction-corrected topocentric coordinates:

```java
var dateTime = ZonedDateTime.now();

var positions = new SolarPositions();
var position = positions.at(
    dateTime,
    48.21, // latitude (degrees)
    16.37, // longitude (degrees)
    190, // height above sea level (m)
    DeltaT.estimate(dateTime.toLocalDate()), // delta T (s)
    new Atmosphere(1010, 11)); // local pressure (hPa), temperature (°C)

System.out.println(position);
System.out.println(position.elevation());
```

`at` accepts either a `ZonedDateTime` or an `Instant`. Positions and events use the
proleptic Gregorian calendar, including before 1582; UTC approximates UT1.
Omit the atmosphere for an unrefracted position, and omit height to assume sea level.
Use `SolarPositions.grena3()` for Grena3, which requires zero height.

`SolarEvents` defaults to SPA positions. Its `forDate` method returns every sunrise, transit and sunset in a local calendar date:

```java
var date = LocalDate.of(2026, 9, 25);
var calculator = new SolarEvents();
var events = calculator.forDate(
    date, ZoneId.of("Europe/Vienna"),
    48.21, 16.37, DeltaT.estimate(date));

System.out.println(events.rises());
System.out.println(events.transits());
System.out.println(events.sets());
System.out.println(events.alwaysAbove()); // continuous daylight
System.out.println(events.alwaysBelow()); // continuous night
```

To use Grena3 for events, create the calculator with `SolarEvents.grena3()`.
Grena3 is faster but less accurate, and supports only 2010–2110.
A custom solar model can implement the single-method `SolarEvents.PositionProvider` interface
and be passed to `new SolarEvents(provider, firstYear, lastYear)`. It supplies unrefracted
elevation and local hour angle; the event search and result types stay the same.

Each immutable list can be empty or contain several events. Returned times use the requested
zone and lie within the date, including its start and excluding the following date. This also
handles clock changes and skipped dates. `stateAtStart()` describes the Sun relative to the
selected horizon. A state within numerical tolerance of the horizon is `ON_HORIZON`.

Pass a horizon for twilight or a numeric elevation for a custom crossing.
Use `forDateMultiple` for a map of results, sharing transit across horizons:

```java
var twilight = calculator.forDate(date, ZoneId.of("Europe/Vienna"),
    48.21, 16.37, DeltaT.estimate(date), SolarEvents.Horizon.CIVIL_TWILIGHT);
var all = calculator.forDateMultiple(date, ZoneId.of("Europe/Vienna"),
    48.21, 16.37, DeltaT.estimate(date), SolarEvents.Horizon.values());
var custom = calculator.forDateMultiple(date, ZoneId.of("Europe/Vienna"),
    48.21, 16.37, DeltaT.estimate(date), -4.5, -10.0);
```

For an arbitrary interval, `nextRise`, `nextSet` and `nextTransit` return `Optional<Instant>`.
Searches exclude `start` and include `end`; pass a returned instant as the next start to
continue. Rise and set accept either a `Horizon` or a custom elevation in degrees:

```java
var start = date.atStartOfDay(ZoneOffset.UTC).toInstant();
var end = start.plus(Duration.ofDays(30));
var nextRise = calculator.nextRise(start, end,
    78.22, 15.63, DeltaT.estimate(date), SolarEvents.Horizon.SUNRISE_SUNSET);
```

Events are independent: a date need not contain a transit or a rise/set pair.

For positions at many locations at the same time, prepare the time-dependent calculations once.
This works with both SPA and Grena3:

```java
var snapshot = positions.forTime(dateTime, deltaT);
for (var coordinate : coordinates) {
    var position = snapshot.at(coordinate.lat, coordinate.lon, coordinate.elevation);
}
```

`forTime` accepts a `ZonedDateTime` or an `Instant`. The returned `SolarPositions.AtTime`
is immutable and thread-safe, with the same optional height and `Atmosphere` arguments as `at`.

See the Javadoc for more methods.

### Which position algorithm should I use?

- For many applications, Grena3 should work just fine. It's simple, fast, and pretty accurate for a time window from
  2010 to 2110 CE.
- If you're looking for maximum accuracy or need to calculate for historic dates, use SPA. It's widely considered a
  reference algorithm for solar positioning, being very accurate and usable in a very large time window.

While Grena3 is about an order of magnitude faster than SPA, in absolute terms we are talking about microseconds. The difference mostly matters for bulk calculations.

### Solar event accuracy

`SolarEvents` searches unrefracted topocentric solar-centre positions at sea level
from the chosen model, defaulting to SPA.
Sunrise and sunset occur when the Sun's centre is 50 arcminutes (about 0.833°) below the
geometric horizon, allowing for average atmospheric refraction and the Sun's apparent radius.
Twilight and custom elevations use exactly the selected geometric angle, without an
additional refraction correction.

Crossing brackets are refined to one millisecond, date assignment has the same resolution.
This is numerical precision, not observed-event accuracy. The position model's angular
uncertainty matters more at shallow crossings, and weather and terrain can shift observed sunrise by minutes
([USNO](https://aa.usno.navy.mil/faq/RST_defs)). A tangency alone is not a crossing, and events
less than one millisecond apart need not be distinguished.

Event searches use a continuous proleptic Gregorian calendar. Both UT and TT must remain
within the model's supported years: -2000 through 6000 for SPA, 2010 through 2110 for Grena3.
UTC approximates UT1; ΔT is held constant during each query.

The search combines interval subdivision, standard [interpolation error bounds](https://dlmf.nist.gov/3.3.E5)
and [ITP refinement](https://doi.org/10.1145/3423597). A conservative estimate of the curve's bending comes from daily rotation, with extra room for slower solar motion. It is an engineering choice checked against
reference data and edge cases, not a formal guarantee for every input.
[Astronomy Engine](https://github.com/cosinekitty/astronomy/blob/master/source/js/astronomy.ts)
uses related adaptive-search ideas, with a speed limit rather than curvature.

### What's this "delta T" thing?

See [Wikipedia](<https://en.wikipedia.org/wiki/ΔT_(timekeeping)>) for an explanation. For many simple applications, and particularly for sunrise and sunset,
this value could be negligible as it's just over a minute (about 70 seconds) as of this writing. However, if you're
looking for maximum accuracy, you should use an observed value (available from e.g. the US Naval
Observatory) or at least a solid estimate.

`DeltaT.estimate()` uses polynomials originally published by [Espenak and Meeus](http://eclipse.gsfc.nasa.gov/SEcat5/deltatpoly.html)
and [updated by Espenak in 2014](https://www.eclipsewise.com/help/deltatpoly2014.html), with custom replacement branches from 2015 onwards. The [derivation and comparisons](https://klaus.brunners.name/posts/delta-t-polynomials/) describe the fit and its
limitations. Future values remain uncertain, and extrapolation beyond 2100 is particularly speculative.

### Is it thread-safe?

Yes. None of the classes hold any mutable shared state.
