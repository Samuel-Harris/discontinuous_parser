/*
 * Copyright (c) 2018. University of St Andrews
 */

package standrews.aux;

import java.sql.Time;

public class TimerNano {
    /**
     * Conversion factor from nanoseconds to seconds.
     */
    private static final double NANO_TO_S = 1000000000.0;
    /**
     * Conversion factor from nanoseconds to milliseconds.
     */
    private static final double NANO_TO_M = 1000000.0;

    /**
     * Last start time of the timer, in nanoseconds.
     */
    private long start;
    /**
     * Last stop time of the timer, in nanoseconds.
     */
    private long stop;

    /**
     * Initialise and start the timer.
     */
    public TimerNano() {
        start = 0;
        stop = 0;
    }

    /**
     * Start the timer after initializing.
     */
    public void init() {
        start = System.nanoTime();
    }

    /**
     * Start the timer.
     */
    public void start() {
        start = System.nanoTime() - (stop - start);
    }

    /**
     * Stop the timer.
     * Return the amount of time (in milliseconds).
     */
    public long stop() {
        stop = System.nanoTime();
        return nanos();
    }

    public double stopMsec() {
        return inMsec(stop());
    }

    /**
     * Get the amount of time (in milliseconds) that
     * the timer ran between starting and stopping.
     *
     * @return Number of milliseconds that passed between
     * starting and stopping the timer.
     */
    public long nanos() {
        return stop - start;
    }

    /**
     * Get the amount of time (in seconds) that
     * the timer ran between starting and stopping.
     *
     * @return Number of seconds that passed between
     * starting and stopping the timer.
     */
    public double seconds() {
        return inSeconds(nanos());
    }

    /**
     * Convert a number of nanoseconds to seconds.
     *
     * @param nanos A number of nanoseconds.
     * @return The number of seconds that the nanoseconds represent.
     */
    private double inSeconds(final long nanos) {
        return nanos / NANO_TO_S;
    }

    /**
     * Convert a number of nanoseconds to milliseconds.
     *
     * @param nanos A number of nanoseconds.
     * @return The number of seconds that the nanoseconds represent.
     */
    private double inMsec(final long nanos) {
        return nanos / NANO_TO_M;
    }
}
