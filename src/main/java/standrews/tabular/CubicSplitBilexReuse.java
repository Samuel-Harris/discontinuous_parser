/*
 * Copyright (c) 2018. University of St Andrews
 */

package standrews.tabular;

import java.util.TreeMap;

/**
 * Cubic-time parsing for weighted bilexical grammar.
 * Reuse tables to avoid fragmenting memory.
 */
public abstract class CubicSplitBilexReuse {

    private static int NTable = 0;
    private static int nTable = 0;

    /**
     * Grammar.
     */
    protected final SplitBilexGrammar gram;

    private int N() {
        return gram.delexs.length;
    }

    /**
     * Input.
     */
    protected final String[] in;

    private int n() {
        return in.length;
    }

    /**
     * Maximum weight of two consecutive substructures, with
     * i having right children up to some k,
     * j having left children down to k+1,
     * where i <= k < j, with nonterminals for the two substructures.
     */
    protected static class Pair {
        // private int[][][][] weights;
        private int[] weights;

        public Pair(final int N, final int n) {
            weights = new int[N * N * n * (n - 1) / 2];
        }

        public void init(final int N, final int n) {
            int size = N * N * n * (n - 1) / 2;
            for (int x = 0; x < size; x++)
                weights[x] = zero();
        }

        public int get(final int i, final int j, final int l, final int r,
                       final int N, final int n) {
            return weights[N * N * (j * (j - 1) / 2 + j - i - 1) + N * l + r];
        }

        public void set(final int i, final int j, final int l, final int r,
                        final int w, final int N, final int n) {
            weights[N * N * (j * (j - 1) / 2 + j - i - 1) + N * l + r] = w;
        }

        public void update(final int i, final int j, final int l, final int r,
                           final int w, final int N, final int n) {
            set(i, j, l, r, plus(get(i, j, l, r, N, n), w), N, n);
        }
    }

    /**
     * Maximum weight of substructure, with i having right children up to j,
     * and delexicalized nonterminal.
     */
    protected static class Right {
        // private int[][][] weights;
        private int[] weights;

        public Right(final int N, final int n) {
            weights = new int[N * (n + 1) * n / 2];
        }

        public void init(final int N, final int n) {
            int size = N * (n + 1) * n / 2;
            for (int x = 0; x < size; x++)
                weights[x] = zero();
        }

        public int get(final int i, final int j, final int d,
                       final int N, final int n) {
            return weights[N * ((j + 1) * j / 2 + (j - i)) + d];
        }

        public void set(final int i, final int j, final int d, final int w,
                        final int N, final int n) {
            weights[N * ((j + 1) * j / 2 + (j - i)) + d] = w;
        }

        public void update(final int i, final int j, final int d, final int w,
                           final int N, final int n) {
            set(i, j, d, plus(get(i, j, d, N, n), w), N, n);
        }
    }

    /**
     * Maximum weight of substructure, with j having left children down to i,
     * and delexicalized nonterminal.
     */
    protected static class Left {
        // private int[][][] weights;
        private int[] weights;

        public Left(final int N, final int n) {
            weights = new int[N * (n + 1) * n / 2];
        }

        public void init(final int N, final int n) {
            int size = N * (n + 1) * n / 2;
            for (int x = 0; x < size; x++)
                weights[x] = zero();
        }

        public int get(final int i, final int j, final int d,
                       final int N, final int n) {
            return weights[N * ((j + 1) * j / 2 + (j - i)) + d];
        }

        public void set(final int i, final int j, final int d, final int w,
                        final int N, final int n) {
            weights[N * ((j + 1) * j / 2 + (j - i)) + d] = w;
        }

        public void update(final int i, final int j, final int d, final int w,
                           final int N, final int n) {
            set(i, j, d, plus(get(i, j, d, N, n), w), N, n);
        }
    }

    /**
     * The elements of the parsing table.
     */
    protected static Pair pairRight;
    protected static Pair pairLeft;
    protected static Right right;
    protected static Left left;

    public CubicSplitBilexReuse(final SplitBilexGrammar gram, final String[] in, final int initialSize) {
        this.gram = gram;
        this.in = in;
        // TimerNano timer = new TimerNano();
        // timer.init();
        initializeTable(initialSize);
        // long t = timer.stop();
        // DataCollectionSum.globalCollection.add("init", in.length, 0.000001 * t);
        // timer.init();
        fillTable();
        // t = timer.stop();
        // DataCollectionSum.globalCollection.add("fill", in.length, 0.000001 * t);
    }

    /**
     * Weight of dependency (i,j), where i is head and j is dependent.
     *
     * @param i
     * @param j
     */
    protected abstract int weight(final int i, final int j);

    protected static int zero() {
        return Integer.MIN_VALUE;
    }

    protected static int one() {
        return 0;
    }

    protected static int times(final int a, final int b) {
        return a > zero() && b > zero() ? a + b : zero();
    }

    protected static int plus(final int a, final int b) {
        return Math.max(a, b);
    }

    protected void initializeTable(final int initialSize) {
        if (N() > NTable || n() > nTable) {
            // System.out.println("Enlarge table");
            final int nNew = Math.max(n(), initialSize);
            pairRight = new Pair(N(), nNew);
            pairLeft = new Pair(N(), nNew);
            right = new Right(N(), nNew);
            left = new Left(N(), nNew);
            NTable = N();
            nTable = nNew;
        }
        pairRight.init(N(), n());
        pairLeft.init(N(), n());
        right.init(N(), n());
        left.init(N(), n());
    }

    protected void fillTable() {
        for (int i = 0; i < n(); i++) {
            right.update(i, i, gram.getNum(this.in[i]), one(), N(), n());
            left.update(i, i, gram.getNum(this.in[i]), one(), N(), n());
        }
        for (int len = 2; len <= n(); len++) {
            combinePair(len);
            combineRight(len);
            combineLeft(len);
        }
    }

    protected void combinePair(final int len) {
        for (int len1 = 1; len1 < len; len1++) {
            for (int i = 0; i <= n() - len; i++) {
                final int k = i + len1 - 1;
                final int j = i + len - 1;
                for (int l = 0; l < N(); l++) {
                    if (right.get(i, k, l, N(), n()) != zero()) {
                        for (int r = 0; r < N(); r++) {
                            final int w = times(right.get(i, k, l, N(), n()), left.get(k + 1, j, r, N(), n()));
                            final int w1 = times(w, weight(i, j));
                            final int w2 = times(w, weight(j, i));
                            pairRight.update(i, j, l, r, w1, N(), n());
                            pairLeft.update(i, j, l, r, w2, N(), n());
                        }
                    }
                }
            }
        }
    }

    protected void combineRight(final int len) {
        for (int len1 = 2; len1 <= len; len1++) {
            for (int i = 0; i <= n() - len; i++) {
                final int k = i + len1 - 1;
                final int j = i + len - 1;
                for (int d = 0; d < N(); d++) {
                    final int wr = right.get(k, j, d, N(), n());
                    if (wr != zero()) {
                        for (int l = 0; l < N(); l++) {
                            for (int r = 0; r < N(); r++) {
                                final int wpr = pairRight.get(i, k, l, r, N(), n());
                                final int w = times(wpr, wr);
                                final int m = gram.getRightNum(l, gram.getMiddleNum(r, d));
                                if (m >= 0)
                                    right.update(i, j, m, w, N(), n());
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
                    final int wl = left.get(i, k, d, N(), n());
                    if (wl != zero()) {
                        for (int l = 0; l < N(); l++) {
                            for (int r = 0; r < N(); r++) {
                                final int wpl = pairLeft.get(k, j, l, r, N(), n());
                                final int w = times(wl, wpl);
                                final int m = gram.getLeftNum(r, gram.getMiddleNum(d, l));
                                if (m >= 0)
                                    left.update(i, j, m, w, N(), n());
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
                final int w1 = left.get(0, 0, l, N(), n());
                final int w2 = right.get(0, n() - 1, r, N(), n());
                if (gram.getMiddleNum(l, r) == d)
                    w = plus(w, times(w1, w2));
            }
        }
        return w;
    }

    public void decodePairRight(final int first, final int last,
                                final int l, final int r,
                                final TreeMap<Integer, Integer> parent) {
        final int w = pairRight.get(first, last, l, r, N(), n());
        for (int i = first; i < last; i++) {
            final int we = weight(first, last);
            final int w1 = right.get(first, i, l, N(), n());
            final int w2 = left.get(i + 1, last, r, N(), n());
            if (w == times(we, times(w1, w2))) {
                decodeRight(first, i, l, parent);
                decodeLeft(i + 1, last, r, parent);
                return;
            }
        }
    }

    public void decodePairLeft(final int first, final int last,
                               final int l, final int r,
                               final TreeMap<Integer, Integer> parent) {
        final int w = pairLeft.get(first, last, l, r, N(), n());
        for (int i = first; i < last; i++) {
            final int we = weight(last, first);
            final int w1 = right.get(first, i, l, N(), n());
            final int w2 = left.get(i + 1, last, r, N(), n());
            if (w == times(we, times(w1, w2))) {
                decodeRight(first, i, l, parent);
                decodeLeft(i + 1, last, r, parent);
                return;
            }
        }
    }

    public void decodeRight(final int first, final int last, final int d,
                            final TreeMap<Integer, Integer> parent) {
        final int w = right.get(first, last, d, N(), n());
        if (first != last) {
            for (int i = first + 1; i <= last; i++) {
                for (int l = 0; l < N(); l++) {
                    for (int r = 0; r < N(); r++) {
                        for (int d2 = 0; d2 < N(); d2++) {
                            final int w1 = pairRight.get(first, i, d2, l, N(), n());
                            final int w2 = right.get(i, last, r, N(), n());
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
                           final TreeMap<Integer, Integer> parent) {
        final int w = left.get(first, last, d, N(), n());
        if (first != last) {
            for (int i = first; i < last; i++) {
                for (int l = 0; l < N(); l++) {
                    for (int r = 0; r < N(); r++) {
                        for (int d2 = 0; d2 < N(); d2++) {
                            final int w1 = left.get(first, i, l, N(), n());
                            final int w2 = pairLeft.get(i, last, r, d2, N(), n());
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

    public TreeMap<Integer, Integer> decode(final String delex) {
        final TreeMap<Integer, Integer> parent = new TreeMap<>();
        final int d = gram.getNum(delex);
        decodeRight(0, n() - 1, d, parent);
        return parent;
    }

    public int countDecodePairRight(final int first, final int last,
                                    final int l, final int r) {
        int c = 0;
        final int w = pairRight.get(first, last, l, r, N(), n());
        for (int i = first; i < last; i++) {
            final int we = weight(first, last);
            final int w1 = right.get(first, i, l, N(), n());
            final int w2 = left.get(i + 1, last, r, N(), n());
            if (w == times(we, times(w1, w2))) {
                final int c1 = countDecodeRight(first, i, l);
                final int c2 = countDecodeLeft(i + 1, last, r);
                c += c1 * c2;
            }
        }
        return c;
    }

    public int countDecodePairLeft(final int first, final int last,
                                   final int l, final int r) {
        int c = 0;
        final int w = pairLeft.get(first, last, l, r, N(), n());
        for (int i = first; i < last; i++) {
            final int we = weight(last, first);
            final int w1 = right.get(first, i, l, N(), n());
            final int w2 = left.get(i + 1, last, r, N(), n());
            if (w == times(we, times(w1, w2))) {
                final int c1 = countDecodeRight(first, i, l);
                final int c2 = countDecodeLeft(i + 1, last, r);
                c += c1 * c2;
            }
        }
        return c;
    }

    public int countDecodeRight(final int first, final int last, final int d) {
        final int w = right.get(first, last, d, N(), n());
        if (first != last) {
            int c = 0;
            for (int i = first + 1; i <= last; i++) {
                for (int l = 0; l < N(); l++) {
                    for (int r = 0; r < N(); r++) {
                        for (int d2 = 0; d2 < N(); d2++) {
                            final int w1 = pairRight.get(first, i, d2, l, N(), n());
                            final int w2 = right.get(i, last, r, N(), n());
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
        final int w = left.get(first, last, d, N(), n());
        if (first != last) {
            int c = 0;
            for (int i = first; i < last; i++) {
                for (int l = 0; l < N(); l++) {
                    for (int r = 0; r < N(); r++) {
                        for (int d2 = 0; d2 < N(); d2++) {
                            final int w1 = left.get(first, i, l, N(), n());
                            final int w2 = pairLeft.get(i, last, r, d2, N(), n());
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
        return countDecodeRight(0, n() - 1, d);
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
        CubicSplitBilexReuse parser = new CubicSplitBilexReuse(gram, in, 20) {
            @Override
            protected int weight(int i, int j) {
                if (i > 10)
                    return 1;
                else
                    return 0;
            }
        };
        System.out.println(parser.right.get(0, parser.n() - 1, 0,
                parser.N(), parser.n()));
        TreeMap<Integer, Integer> parent = parser.decode("any");
        for (int i = 0; i < parser.n(); i++) {
            System.out.println("" + i + " " + parent.get(i));
        }
    }

}
