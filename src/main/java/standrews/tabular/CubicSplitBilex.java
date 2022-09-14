/*
 * Copyright (c) 2018. University of St Andrews
 */

package standrews.tabular;

import java.util.TreeMap;

/**
 * Cubic-time parsing for weighted bilexical grammar.
 */
public abstract class CubicSplitBilex {

	/**
	 * Grammar.
	 */
	private final SplitBilexGrammar gram;

	private int N() {
		return gram.delexs.length;
	}

	/**
	 * Input.
	 */
	private final String[] in;

	private int n() {
		return in.length;
	}

	/**
	 * Maximum weight of two consecutive substructures, with
	 * i having right children up to some k,
	 * j having left children down to k+1,
	 * where i <= k < j, with nonterminals for the two substructures.
	 */
	protected class Pair {
		private int[][][][] weights;
		public Pair() {
			weights = new int[n()-1][][][];
			for (int i = 0; i < n()-1; i++) {
				weights[i] = new int[n()-i-1][][];
				for (int j = 0; j < n()-i-1; j++) {
					weights[i][j] = new int[N()][];
					for (int l = 0; l < N(); l++) {
						weights[i][j][l] = new int[N()];
						for (int r = 0; r < N(); r++) {
							weights[i][j][l][r] = zero();
						}
					}
				}
			}
		}
		public int get(final int i, final int j, final int l, final int r) {
			return weights[i][j-i-1][l][r];
		}
		public void set(final int i, final int j, final int l, final int r,
						final int w) {
			weights[i][j-i-1][l][r] = w;
		}
		public void update(final int i, final int j, final int l, final int r,
						   final int w) {
			set(i, j, l, r, plus(get(i, j, l, r), w));
		}
	}

	/**
	 * Maximum weight of substructure, with i having right children up to j,
	 * and delexicalized nonterminal.
	 */
	protected class Right {
		private int[][][] weights;
		public Right() {
			weights = new int[n()][][];
			for (int i = 0; i < n(); i++) {
				weights[i] = new int[n() - i][];
				for (int j = 0; j < n() - i; j++) {
					weights[i][j] = new int[N()];
					for (int d = 0; d < N(); d++) {
						weights[i][j][d] = zero();
					}
				}
			}
		}
		public int get(final int i, final int j, final int d) {
			return weights[i][j-i][d];
		}
		public void set(final int i, final int j, final int d, final int w) {
			weights[i][j-i][d] = w;
		}
		public void update(final int i, final int j, final int d, final int w) {
			set(i, j, d, plus(get(i, j, d), w));
		}
	}

	/**
	 * Maximum weight of substructure, with j having left children down to i,
	 * and delexicalized nonterminal.
	 */
	protected class Left {
		private int[][][] weights;
		public Left() {
			weights = new int[n()][][];
			for (int j = 0; j < n(); j++) {
				weights[j] = new int[j+1][];
				for (int i = 0; i < j+1; i++) {
					weights[j][j-i] = new int[N()];
					for (int d = 0; d < N(); d++) {
						weights[j][j-i][d] = zero();
					}
				}
			}
		}
		public int get(final int i, final int j, final int d) {
			return weights[j][j-i][d];
		}
		public void set(final int i, final int j, final int d, final int w) {
			weights[j][j-i][d] = w;
		}
		public void update(final int i, final int j, final int d, final int w) {
			set(i, j, d, plus(get(i, j, d), w));
		}
	}

	/**
	 * The elements of the parsing table.
	 */
	protected Pair pairRight;
	protected Pair pairLeft;
	protected Right right;
	protected Left left;

	public CubicSplitBilex(final SplitBilexGrammar gram, final String[] in) {
		this.gram = gram;
		this.in = in;
		gram.cacheNumMaps();
		initializeTable();
		fillTable();
	}

	/**
	 * Weight of dependency (i,j), where i is head and j is dependent.
	 * @param i
	 * @param j
	 */
	protected abstract int weight(final int i, final int j);

	protected int zero() {
		return Integer.MIN_VALUE;
	}

	protected int one() {
		return 0;
	}

	protected int times(final int a, final int b) {
		return a > zero() && b > zero() ? a+b : zero();
	}

	protected int plus(final int a, final int b) {
		return Math.max(a, b);
	}

	protected void initializeTable() {
		pairRight = new Pair();
		pairLeft = new Pair();
		right = new Right();
		left = new Left();
	}

	protected void fillTable() {
		for (int i = 0; i < n(); i++) {
			right.update(i, i, gram.getNum(this.in[i]), one());
			left.update(i, i, gram.getNum(this.in[i]), one());
		}
		for (int len = 2; len <= n(); len++) {
			combinePair(len);
			combineRight(len);
			combineLeft(len);
		}
	}

	protected void combinePair(final int len) {
		for (int len1 = 1; len1 < len; len1++) {
			for (int i = 0; i <= n()-len; i++) {
				final int k = i + len1 - 1;
				final int j = i + len - 1;
				for (int l = 0; l < N(); l++) {
					if (right.get(i, k, l) != zero()) {
						for (int r = 0; r < N(); r++) {
							final int w = times(right.get(i, k, l), left.get(k + 1, j, r));
							final int w1 = times(w, weight(i, j));
							final int w2 = times(w, weight(j, i));
							pairRight.update(i, j, l, r, w1);
							pairLeft.update(i, j, l, r, w2);
						}
					}
				}
			}
		}
	}
	protected void combineRight(final int len) {
		for (int len1 = 2; len1 <= len; len1++) {
			for (int i = 0; i <= n()-len; i++) {
				final int k = i + len1 - 1;
				final int j = i + len - 1;
				for (int d = 0; d < N(); d++) {
					final int wr = right.get(k, j, d);
					if (wr != zero()) {
						for (int l = 0; l < N(); l++) {
							for (int r = 0; r < N(); r++) {
								final int wpr = pairRight.get(i, k, l, r);
								final int w = times(wpr, wr);
								final int m = gram.getRightNum(l, gram.getMiddleNum(r, d));
								if (m >= 0)
									right.update(i, j, m, w);
							}
						}
					}
				}
			}
		}
	}
	protected void combineLeft(final int len) {
		for (int len1 = 1; len1 < len; len1++) {
			for (int i = 0; i <= n() - len; i++) {
				final int k = i + len1 - 1;
				final int j = i + len - 1;
				for (int d = 0; d < N(); d++) {
					final int wl = left.get(i, k, d);
					if (wl != zero()) {
						for (int l = 0; l < N(); l++) {
							for (int r = 0; r < N(); r++) {
								final int wpl = pairLeft.get(k, j, l, r);
								final int w = times(wl, wpl);
								final int m = gram.getLeftNum(r, gram.getMiddleNum(d, l));
								if (m >= 0)
									left.update(i, j, m, w);
							}
						}
					}
				}
			}
		}
	}

	public int rootWeight(final String delex) {
		final int d = gram.getNum(delex);
		int w = zero();
		for (int l = 0; l < N(); l++) {
			for (int r = 0; r < N(); r++) {
				final int w1 = left.get(0, 0, l);
				final int w2 = right.get(0, n()-1, r);
				if (gram.getMiddleNum(l, r) == d)
					w = plus(w, times(w1, w2));
			}
		}
		return w;
	}

	public void decodePairRight(final int first, final int last,
								final int l, final int r,
								final TreeMap<Integer,Integer> parent) {
		final int w = pairRight.get(first, last, l, r);
		for (int i = first; i < last; i++) {
			final int we = weight(first, last);
			final int w1 = right.get(first, i, l);
			final int w2 = left.get(i+1, last, r);
			if (w == times(we, times(w1, w2))) {
				decodeRight(first, i, l, parent);
				decodeLeft(i+1, last, r, parent);
				return;
			}
		}
	}

	public void decodePairLeft(final int first, final int last,
							   final int l, final int r,
							   final TreeMap<Integer,Integer> parent) {
		final int w = pairLeft.get(first, last, l, r);
		for (int i = first; i < last; i++) {
			final int we = weight(last, first);
			final int w1 = right.get(first, i, l);
			final int w2 = left.get(i+1, last, r);
			if (w == times(we, times(w1, w2))) {
				decodeRight(first, i, l, parent);
				decodeLeft(i+1, last, r, parent);
				return;
			}
		}
	}

	public void decodeRight(final int first, final int last, final int d,
							final TreeMap<Integer,Integer> parent) {
		final int w = right.get(first, last, d);
		if (first != last) {
			for (int i = first + 1; i <= last; i++) {
				for (int l = 0; l < N(); l++) {
					for (int r = 0; r < N(); r++) {
						for (int d2 = 0; d2 < N(); d2++) {
							final int w1 = pairRight.get(first, i, d2, l);
							final int w2 = right.get(i, last, r);
							if (d == gram.getRightNum(d2, gram.getMiddleNum(l, r))
											&& w == times(w1, w2)) {
								parent.put(i, first);
								decodePairRight(first, i, d2, l, parent);
								decodeRight(i, last, r, parent);
								return;
							}
						}
					}
				}
			}
		}
	}

	public void decodeLeft(final int first, final int last, final int d,
						   final TreeMap<Integer,Integer> parent) {
		final int w = left.get(first, last, d);
		if (first != last) {
			for (int i = first; i < last; i++) {
				for (int l = 0; l < N(); l++) {
					for (int r = 0; r < N(); r++) {
						for (int d2 = 0; d2 < N(); d2++) {
							final int w1 = left.get(first, i, l);
							final int w2 = pairLeft.get(i, last, r, d2);
							if (d == gram.getLeftNum(d2, gram.getMiddleNum(l, r))
											&& w == times(w1, w2)) {
								parent.put(i, last);
								decodeLeft(first, i, l, parent);
								decodePairLeft(i, last, r, d2, parent);
								return;
							}
						}
					}
				}
			}
		}
	}

	public TreeMap<Integer,Integer> decode(final String delex) {
		final TreeMap<Integer,Integer> parent = new TreeMap<>();
		final int d = gram.getNum(delex);
		decodeRight(0, n()-1, d, parent);
		return parent;
	}

	public int countDecodePairRight(final int first, final int last,
								final int l, final int r) {
		int c = 0;
		final int w = pairRight.get(first, last, l, r);
		for (int i = first; i < last; i++) {
			final int we = weight(first, last);
			final int w1 = right.get(first, i, l);
			final int w2 = left.get(i+1, last, r);
			if (w == times(we, times(w1, w2))) {
				final int c1 = countDecodeRight(first, i, l);
				final int c2 = countDecodeLeft(i+1, last, r);
				c += c1 * c2;
			}
		}
		return c;
	}

	public int countDecodePairLeft(final int first, final int last,
							   final int l, final int r) {
		int c = 0;
		final int w = pairLeft.get(first, last, l, r);
		for (int i = first; i < last; i++) {
			final int we = weight(last, first);
			final int w1 = right.get(first, i, l);
			final int w2 = left.get(i+1, last, r);
			if (w == times(we, times(w1, w2))) {
				final int c1 = countDecodeRight(first, i, l);
				final int c2 = countDecodeLeft(i+1, last, r);
				c += c1 * c2;
			}
		}
		return c;
	}

	public int countDecodeRight(final int first, final int last, final int d) {
		final int w = right.get(first, last, d);
		if (first != last) {
			int c = 0;
			for (int i = first + 1; i <= last; i++) {
				for (int l = 0; l < N(); l++) {
					for (int r = 0; r < N(); r++) {
						for (int d2 = 0; d2 < N(); d2++) {
							final int w1 = pairRight.get(first, i, d2, l);
							final int w2 = right.get(i, last, r);
							if (d == gram.getRightNum(d2, gram.getMiddleNum(l, r))
											&& w == times(w1, w2)) {
								final int c1 = countDecodePairRight(first, i, d2, l);
								final int c2 = countDecodeRight(i, last, r);
								c += c1 * c2;
							}
						}
					}
				}
			}
			return c;
		} else {
			return 1;
		}
	}

	public int countDecodeLeft(final int first, final int last, final int d) {
		final int w = left.get(first, last, d);
		if (first != last) {
			int c = 0;
			for (int i = first; i < last; i++) {
				for (int l = 0; l < N(); l++) {
					for (int r = 0; r < N(); r++) {
						for (int d2 = 0; d2 < N(); d2++) {
							final int w1 = left.get(first, i, l);
							final int w2 = pairLeft.get(i, last, r, d2);
							if (d == gram.getLeftNum(d2, gram.getMiddleNum(l, r))
											&& w == times(w1, w2)) {
								final int c1 = countDecodeLeft(first, i, l);
								final int c2 = countDecodePairLeft(i, last, r, d2);
								c += c1 * c2;
							}
						}
					}
				}
			}
			return c;
		} else {
			return 1;
		}
	}

	public int countDecode(final String delex) {
		final int d = gram.getNum(delex);
		return countDecodeRight(0, n()-1, d);
	}

	public void printTable() {
		System.out.println("INPUT");
		for (int i = 0; i < n(); i++)
			System.out.print(in[i] + " ");
		System.out.println();
		System.out.println("PAIRRIGHT");
		for (int i = 0; i < n(); i++) {
			for (int j = i+1; j < n(); j++) {
				System.out.print("i=" + i + " ");
				for (int l = 0; l < N(); l++) {
					for (int r = 0; r < N(); r++) {
						if (pairRight.get(i, j, l, r) >= 0) {
							System.out.print("j,l,r=" + j + ",");
							System.out.print("" + gram.delexs[l] + ",");
							System.out.print("" + gram.delexs[r] + "->");
							System.out.print("" + pairRight.get(i, j, l, r) + " ");
						}
					}
				}
				System.out.println();
			}
		}
		System.out.println("PAIRLEFT");
		for (int i = 0; i < n(); i++) {
			for (int j = i+1; j < n(); j++) {
				System.out.print("i=" + i + " ");
				for (int l = 0; l < N(); l++) {
					for (int r = 0; r < N(); r++) {
						if (pairLeft.get(i, j, l, r) >= 0) {
							System.out.print("j,l,r=" + j + ",");
							System.out.print("" + gram.delexs[l] + ",");
							System.out.print("" + gram.delexs[r] + "->");
							System.out.print("" + pairLeft.get(i, j, l, r) + " ");
						}
					}
				}
				System.out.println();
			}
		}
		System.out.println("RIGHT");
		for (int i = 0; i < n(); i++) {
			for (int j = i; j < n(); j++) {
				System.out.print("i=" + i + " ");
				for (int l = 0; l < N(); l++) {
					if (right.get(i, j, l) >= 0) {
						System.out.print("j,l=" + j + ",");
						System.out.print("" + gram.delexs[l] + "->");
						System.out.print("" + right.get(i, j, l) + " ");
					}

				}
				System.out.println();
			}
		}
		System.out.println("LEFT");
		for (int i = 0; i < n(); i++) {
			for (int j = i; j < n(); j++) {
				System.out.print("i=" + i + " ");
				for (int r = 0; r < N(); r++) {
					if (left.get(i, j, r) >= 0) {
						System.out.print("j,l=" + j + ",");
						System.out.print("" + gram.delexs[r] + "->");
						System.out.print("" + left.get(i, j, r) + " ");
					}

				}
				System.out.println();
			}
		}
	}

	// For testing.
	public static void main(String[] args) {
		/*
		ShiftReduceBilexGrammar gram = new ShiftReduceBilexGrammar();
		String[] in = ShiftReduceBilexGrammar.getInput(3, 3);
		CubicSplitBilex parser = new CubicSplitBilex(gram, in) {
			@Override
			protected int weight(int i, int j) {
				if (i == 1)
					return 1;
				else
					return 1;
			}
		};
		System.out.println(parser.right.get(0, parser.n()-1, 1));
		*/
		AllBilexGrammar gram = new AllBilexGrammar();
		String[] in = AllBilexGrammar.getInput(30);
		CubicSplitBilex parser = new CubicSplitBilex(gram, in) {
			@Override
			protected int weight(int i, int j) {
				if (i > 10)
					return 1;
				else
					return 0;
			}
		};
		System.out.println(parser.right.get(0, parser.n()-1, 0));
		TreeMap<Integer,Integer> parent = parser.decode("any");
		for (int i = 0; i < parser.n(); i++) {
			System.out.println("" + i + " " + parent.get(i));
		}
	}

}
