/*
 * Copyright (c) 2018. University of St Andrews
 */

package standrews.lexical;

import java.util.Arrays;
import java.util.TreeMap;
import java.util.Vector;

/**
 * Tagging sequence of vectors.
 */
public abstract class VectorTagger {
	protected int inLen;
	protected TreeMap<String,Integer> outEncoding;
	protected String[] outDecoding;
	protected int outLen;

	protected class Example {
		public double[][] in;
		public int[] out;
		public Example(final double[][] in, final int[] out) {
			this.in = in;
			this.out = out;
		}
	}

	protected Vector<Example> trainingSet = new Vector<>();

	public VectorTagger(final int inLen, final String[] tagset) {
		this.inLen = inLen;
		this.outEncoding = new TreeMap<>();
		this.outDecoding = tagset;
		this.outLen = tagset.length;
		for (int i = 0; i < tagset.length; i++) {
			outEncoding.put(outDecoding[i], i);
		}
	}

	protected int[] encodeOut(final String[] out) {
		return Arrays.stream(out)
					.mapToInt(s -> outEncoding.get(s).intValue())
					.toArray();
	}

	protected String[] decodeOut(final int[] out) {
		return Arrays.stream(out)
					.mapToObj(i -> outDecoding[i])
					.toArray(String[]::new);
	}

	public void addTrain(final double[][] in, final int[] out) {
		trainingSet.add(new Example(in, out));
	}

	public void addTrain(final double[][] in, final String[] out) {
		addTrain(in, encodeOut(out));
	}

	protected abstract int[] predictEncoded(final double[][] in);

	public String[] predict(final double[][] in) {
		return decodeOut(predictEncoded(in));
	}

	protected double[][] padBackVecs(double[][] vecs, int n) {
		double[][] padded = new double[vecs.length + n][];
		System.arraycopy(vecs, 0, padded, 0, vecs.length);
		for (int i = 0; i < n; i++)
			padded[vecs.length+i] = new double[inLen];
		return padded;
	}

	protected int[] padBackPoss(int[] poss, int[] padding) {
		int[] padded = new int[poss.length + padding.length];
		System.arraycopy(poss, 0, padded, 0, poss.length);
		System.arraycopy(padding, 0, padded, poss.length, padding.length);
		return padded;
	}

	protected int[] padFrontPoss(int[] poss, int[] padding) {
		int[] padded = new int[poss.length + padding.length];
		System.arraycopy(padding, 0, padded, 0, padding.length);
		System.arraycopy(poss, 0, padded, padding.length, poss.length);
		return padded;
	}

	protected double[][] oneHotArray(int[] array, int len) {
		return Arrays.stream(array)
				.mapToObj(val -> oneHot(val, len))
				.toArray(double[][]::new);
	}

	protected double[] oneHot(int val, int len) {
		double[] a = new double[len];
		a[val] = 1;
		return a;
	}

	protected int maxIndex(double[] vals) {
		int best = -1;
		double bestVal = Double.MIN_VALUE;
		for (int i = 0; i < vals.length; i++) {
			if (vals[i] > bestVal) {
				best = i;
				bestVal = vals[i];
			}
		}
		return best;
	}

}
