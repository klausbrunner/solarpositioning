package net.e175.klaus.solarpositioning;

import java.time.ZonedDateTime;

/**
 * A simple wrapper class for keeping sunrise, sunset, and transit results.
 */
public final class SunriseTransitSet {
    public enum Type {
        /** This is a normal day, with a sunrise and a sunset. */
        NORMAL,
        /** The sun stays above the horizon all day. There is neither sunrise nor sunset. */
        ALL_DAY,
        /** The sun stays below the horizon all day. There is neither sunrise nor sunset. */
        ALL_NIGHT}

    private final Type type;
    private final ZonedDateTime sunrise;
    private final ZonedDateTime transit;
    private final ZonedDateTime sunset;

    public SunriseTransitSet(Type type, ZonedDateTime sunrise, ZonedDateTime transit, ZonedDateTime sunset) {
        this.type = type;
        this.sunrise = sunrise;
        this.transit = transit;
        this.sunset = sunset;
    }

    public Type getType() {
        return type;
    }

    /** Get time of sunrise. This may be null depending on the day's type. */
    public ZonedDateTime getSunrise() {
        return sunrise;
    }

    /** The sun's (upper) transit, or solar noon. This is never null, even for ALL_NIGHT days. */
    public ZonedDateTime getTransit() {
        return transit;
    }

    /** Get time of sunset. This may be null depending on the day's type. */
    public ZonedDateTime getSunset() {
        return sunset;
    }
    @Override
    public String toString() {
        return "SunriseTransitSet{" +
                "type=" + type +
                ", sunrise=" + sunrise +
                ", transit=" + transit +
                ", sunset=" + sunset +
                '}';
    }
}
