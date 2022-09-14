/*
 * Copyright (c) 2018. University of St Andrews
 */

package standrews.lexical;

public class EmptyEmbedding extends Embedding{

	public EmptyEmbedding() {
	}

	public int getLength() {
		return 0;
	}

	public double[] get(final String word) {
		return get();
	}

	public double[] get() {
		return new double[0];
	}
}
