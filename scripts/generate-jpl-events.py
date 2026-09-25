"""Small independent fixture: Skyfield 1.55 / JPL DE440s, fixed TT-UT1 = 69.184s.

Run with a path to de440s.bsp. Input/output instants label UT1 as UTC to match SPA's
UTC≈UT1 convention. Geometric topocentric solar centre, WGS84 sea level, no refraction.
"""

import csv
from datetime import datetime, timezone
import sys
from skyfield.api import load, load_file, wgs84
from skyfield import almanac

ts = load.timescale(delta_t=69.184)
eph = load_file(sys.argv[1])
writer = csv.writer(sys.stdout, lineterminator="\n")
writer.writerow(
    ["start", "end", "latitude", "longitude", "horizon", "rises", "sets", "transits"]
)
cases = [
    ("2020-02-16", 78.216667, 15.633333),
    ("2020-04-16", 78.216667, 15.633333),
    ("2020-08-25", 78.216667, 15.633333),
    ("2025-01-05", -66.566667, 0),
    ("2020-06-10", 0, 179.9),
    ("2020-04-16", 0, 179.9),
    ("2020-07-05", 61.21666666666667, -149.86666666666667),
    ("2024-03-18", 90, 0),
    ("2024-09-20", -90, 0),
    ("2024-06-21", 90, 0),
    ("2024-12-21", -90, 0),
    ("2003-10-17", 39.742476, -105.1786),
    ("2024-03-20", 48.21, 16.37),
    ("2024-12-21", -36.84, 174.74),
]


def stamp(jd):
    return (
        datetime.fromtimestamp((jd - 2440587.5) * 86400, timezone.utc)
        .isoformat(timespec="milliseconds")
        .replace("+00:00", "Z")
    )


for day, lat, lon in cases:
    start = (
        datetime.fromisoformat(day).replace(tzinfo=timezone.utc).timestamp() / 86400
        + 2440587.5
    )
    a, b = ts.ut1_jd(start), ts.ut1_jd(start + 1)
    observer = eph["earth"] + wgs84.latlon(lat, lon)
    transits = almanac.find_transits(observer, eph["sun"], a, b)
    for horizon in [-50 / 60, -6, -12, -18]:

        def above(t):
            return (
                observer.at(t).observe(eph["sun"]).apparent().altaz()[0].degrees
                > horizon
            )

        above.step_days = 5 / 1440
        events, state = almanac.find_discrete(a, b, above, epsilon=0.001 / 86400)
        rises = ";".join(stamp(t.ut1) for t in events[state == 1])
        sets = ";".join(stamp(t.ut1) for t in events[state == 0])
        writer.writerow(
            [
                stamp(start),
                stamp(start + 1),
                lat,
                lon,
                horizon,
                rises,
                sets,
                ";".join(stamp(t.ut1) for t in transits),
            ]
        )
