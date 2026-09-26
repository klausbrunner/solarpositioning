# Independent event references

The three `usno_reference_*.csv` files contain sunrise/sunset and civil-twilight
times from the [US Naval Observatory annual tables](https://aa.usno.navy.mil/data/RS_OneYear).
Comments identify locations; `raw/` preserves the source tables. Times have minute
resolution. These are independent references, not output from this library.

`jpl_events.csv` contains 14 UTC dates/locations at all four standard horizons:
ordinary dates, seasonal polar transitions, exact poles, one-event dates, repeated
events and dates without a transit. It was generated using Skyfield 1.55,
jplephem 2.24, NumPy 2.5.3, sgp4 2.27 and JPL DE440s:

```sh
python3 scripts/generate-jpl-events.py /path/to/de440s.bsp > src/test/resources/net/e175/klaus/solarpositioning/test/sunrise/jpl_events.csv
```

Download [DE440s](https://ssd.jpl.nasa.gov/ftp/eph/planets/bsp/de440s.bsp).
(SHA-256: `c1c7feeab882263fc493a9d5a5b2ddd71b54826cdf65d8d17a76126b260a49f2`)
Both sides use fixed TT−UT1 = 69.184 seconds and label UT1 instants as UTC to match
the library's UTC≈UT1 convention. The reference uses apparent, unrefracted
topocentric solar-centre positions on WGS84 at sea level.

The generator samples at five-minute intervals and refines detected crossings to
one millisecond. This is sufficient for these selected cases, not an oracle for
arbitrarily narrow crossings. A separate Java test constructs a roughly one-second
rise/set pair. Event counts must match exactly; timing tolerance uses SPA's stated
0.0003° angular uncertainty divided by local elevation rate, plus 2 ms. Transit
tolerance is one second. The south-pole sunrise differs by about nine seconds
because the elevation changes exceptionally slowly.
