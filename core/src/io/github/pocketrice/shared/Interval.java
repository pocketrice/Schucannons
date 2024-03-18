package io.github.pocketrice.shared;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static io.github.pocketrice.client.Match.truncate;

public class Interval {
    /**
     * Constant to be used with Interval instantiation to represent "infinite observable".
     */
    public static final float INFINITE = 999999f;
    Instant startTime;
    boolean justStamped, justEnded;
    @Getter
    boolean isFlagged;
    @Getter @Setter
    float intervalSec, prevDelta;


    public Interval() {
        this(Interval.INFINITE);
    }

    public Interval(float sec) {
        intervalSec = sec;
    }

    public void stamp() {
        startTime = Instant.now();
        prevDelta = delta();
        justStamped = true;
    }

    public void unstamp() {
        if (observe()) justEnded = true; // Premature termination; should still call justEnd
        unflag();
        startTime = null;
    }

    public void flag() {
        isFlagged = true;
    }

    public void unflag() {
        isFlagged = false;
    }

    public boolean isStamped() {
        return startTime != null;
    }

    public boolean observe() {
        if (isStamped()) {
            if (prevDelta < 0 && delta() >= 0) { // Passed 'peak' (should only strike once).
                justEnded = true;
            }
            prevDelta = delta();
        }
        return isStamped() && delta() <= 0;
    }

    public float delta() { // Get sec relative to interval endpoint
        return ChronoUnit.MILLIS.between(startTime.plusMillis((long) (intervalSec * 1000)), Instant.now()) / 1000f;
    }

    public float humanDelta(boolean isDecr) { // Human-friendly variant of delta (no negs and trunc decimal)
        return truncate(((isDecr) ? Math.max(0, -delta()) : intervalSec + delta()), 2);
    }

    public Interval cpy() {
        return new Interval(intervalSec);
    }

    public boolean justStamped() {
        observe();
        boolean isJust = false;

        if (justStamped) {
            justStamped = false;
            isJust = true;
        }

        return isJust;
    }

    public boolean justEnded() {
        observe();
        boolean isJust = false;

        if (justEnded) {
            justEnded = false;
            isJust = true;
        }

        return isJust;
    }


    public static boolean isTimeSurpassed(Instant instant, float sec) {
        return (instant != null && ChronoUnit.MILLIS.between(instant, Instant.now()) / 1000f > sec);
    }
}
