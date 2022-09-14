/*
 * Copyright (c) 2018. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package standrews.aux;

public class TimerMilli {
	/**
	 * Conversion factor from milliseconds to seconds.
	 */
	private static final double MS_TO_S = 1000.0;

	/**
	 * Last start time of the timer, in milliseconds.
	 */
	private long start;
	/**
	 * Last stop time of the timer, in milliseconds.
	 */
	private long stop;

	/**
	 * Initialise and start the timer.
	 */
	public TimerMilli() {
		start = 0;
		stop = 0;
	}

	/**
	 * Start the timer after initializing.
	 */
	public void init() {
		start = System.currentTimeMillis();
	}

	/**
	 * Start the timer.
	 */
	public void start() {
		start = System.currentTimeMillis() - (stop - start);
	}

	/**
	 * Stop the timer.
	 * Return the amount of time (in milliseconds).
	 */
	public long stop() {
		stop = System.currentTimeMillis();
		return millis();
	}

	/**
	 * Get the amount of time (in milliseconds) that
	 * the timer ran between starting and stopping.
	 *
	 * @return Number of milliseconds that passed between
	 * starting and stopping the timer.
	 */
	public long millis() {
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
		return inSeconds(millis());
	}

	/**
	 * Convert a number of milliseconds to seconds.
	 *
	 * @param millis A number of milliseconds.
	 * @return The number of seconds that the milliseconds represent.
	 */
	private double inSeconds(final long millis) {
		return millis / MS_TO_S;
	}
}
