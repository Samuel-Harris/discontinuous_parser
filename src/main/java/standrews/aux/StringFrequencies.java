/*
 * Copyright (c) 2020. University of St Andrews
 */

package standrews.aux;

import java.util.TreeMap;

public class StringFrequencies extends TreeMap<String,Integer> {

	private int nullFreq = 0;

	public void add(final String s) {
		if (s == null)
			nullFreq++;
		else if (get(s) == null)
			put(s, 1);
		else
			put(s, get(s) + 1);
	}

	public int getNull() {
		return nullFreq;
	}
}
