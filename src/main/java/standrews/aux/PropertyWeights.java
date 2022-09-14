/*
 * Copyright (c) 2019. University of St Andrews
 */

package standrews.aux;

import java.util.Map;
import java.util.TreeMap;

/**
 * Accumulate weights of properties.
 */
public class PropertyWeights extends TreeMap<String,Double> {

	public PropertyWeights(final String[] properties) {
		for (String p : properties)
			put(p, 0.0);
	}

	public void add(final PropertyWeights other) {
		for (Map.Entry<String,Double> entry : other.entrySet()) {
			add(entry.getKey(), entry.getValue());
		}
	}

	public void add(String p, double w) {
		put(p, get(p) + w);
	}

	public void addNegLog(String p, double v) {
		add(p, -Math.log(v) / Math.log(2));
	}

	public String toString() {
		StringBuffer buf = new StringBuffer();
		for (Map.Entry<String,Double> entry : entrySet()) {
			String p = entry.getKey();
			double w = entry.getValue();
			buf.append("" + p + " " + String.format("%3.2e", w) + "\n");
		}
		return buf.toString();
	}

	public String toString(double scale) {
		StringBuffer buf = new StringBuffer();
		for (Map.Entry<String,Double> entry : entrySet()) {
			String p = entry.getKey();
			double w = entry.getValue() / scale;
			buf.append("" + p + " " + String.format("%3.2e", w) + "\n");
		}
		return buf.toString();
	}

	public double sum() {
		double w = 0;
		for (Map.Entry<String,Double> entry : entrySet()) {
			w += entry.getValue();
		}
		return w;
	}

}
