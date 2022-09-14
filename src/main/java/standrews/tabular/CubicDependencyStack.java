/*
 * Copyright (c) 2018. University of St Andrews
 */

package standrews.tabular;

/**
 * Tabular dependency parsing in cubic time.
 * Part of the input is the stack. This limits reductions that can happen.
 */
public abstract class CubicDependencyStack extends CubicDependency {

	/**
	 * Size of stack.
	 */
	protected int m;

	/**
	 *
	 * @param n
	 */
	public CubicDependencyStack(final int n, final int m) {
		this.n = n;
		this.m = m;
		initializeTable();
		fillTable();
	}

	protected void combinePairInside(final int l) {
		for (int l1 = 1; l1 < l; l1++) {
			for (int i = 0; i <= n-l; i++) {
				final int k = i + l1 - 1;
				final int j = i + l - 1;
				if (k >= m-1 || l1 == 1) {
					final int w = rightInside.get(i, k) +
							leftInside.get(k + 1, j);
					pairInside.update(i, j, w);
				}
			}
		}
		for (int i = 0; i <= n-l; i++) {
			final int j = i + l - 1;
			final int w = pairInside.get(i, j);
			final int w1 = w + weight(i, j);
			final int w2 = w + weight(j, i);
			pairRightInside.update(i, j,  w1);
			pairLeftInside.update(i, j,  w2);
		}
	}

	protected void combinePairOutside(final int l) {
		for (int i = 0; i <= n-l; i++) {
			final int j = i + l - 1;
			final int w1 = pairRightOutside.get(i, j) + weight(i, j);
			final int w2 = pairLeftOutside.get(i, j) + weight(j, i);
			pairOutside.update(i, j, w1);
			pairOutside.update(i, j, w2);
		}
		for (int l1 = 1; l1 < l; l1++) {
			for (int i = 0; i <= n-l; i++) {
				final int k = i + l1 - 1;
				final int j = i + l - 1;
				if (k >= m-1 || l1 == 1) {
					final int w = pairOutside.get(i, j);
					final int w1 = rightInside.get(i, k);
					final int w2 = leftInside.get(k + 1, j);
					rightOutside.update(i, k, w + w2);
					leftOutside.update(k + 1, j, w + w1);
				}
			}
		}
	}

	protected void combineLeftInside(final int l) {
		for (int l1 = 1; l1 < l; l1++) {
			for (int i = 0; i <= n-l; i++) {
				final int k = i + l1 - 1;
				final int j = i + l - 1;
				if (k >= m-1) {
					final int w = leftInside.get(i, k) + pairLeftInside.get(k, j);
					leftInside.update(i, j, w);
				}
			}
		}
	}

	protected void combineLeftOutside(final int l) {
		for (int l1 = 1; l1 < l; l1++) {
			for (int i = 0; i <= n-l; i++) {
				final int k = i + l1 - 1;
				final int j = i + l - 1;
				if (k >= m-1) {
					final int w = leftOutside.get(i, j);
					final int w1 = leftInside.get(i, k);
					final int w2 = pairLeftInside.get(k, j);
					leftOutside.update(i, k, w + w2);
					pairLeftOutside.update(k, j, w + w1);
				}
			}
		}
	}

	// For testing.
	public static void main(String[] args) {
		CubicDependencyStack parser = new CubicDependencyStack(50, 30) {
			public int weight(final int i, final int j) {
				if (j == 1)
					return 1;
				else
					return 0;
			}
		};
		System.out.println(parser.bestWeight(0, 1));
		System.out.println(parser.bestWeight(0, 2));
		System.out.println(parser.bestWeight(0, 3));
		System.out.println(parser.bestWeight(3, 1));
		System.out.println(parser.bestWeight(3, 2));
		System.out.println(parser.bestWeight(4, 3));
	}

}
