/*
 * Copyright (c) 2018. University of St Andrews
 */

package standrews.tabular;

/**
 * Tabular dependency parsing in cubic time.
 */
public abstract class CubicDependency {

    /**
     * Length of input.
     */
    protected int n;

    /**
     * The elements of the parsing table.
     */
    protected Pair pairInside;
    protected Pair pairRightInside;
    protected Pair pairLeftInside;
    protected Right rightInside;
    protected Left leftInside;
    protected Right rightOutside;
    protected Left leftOutside;
    protected Pair pairRightOutside;
    protected Pair pairLeftOutside;
    protected Pair pairOutside;
    protected int complete;

    /**
     * For use by subclass.
     */

    protected CubicDependency() {
    }

    /**
     * @param n
     */
    public CubicDependency(final int n) {
        this.n = n;
        initializeTable();
        fillTable();
    }

    protected void initializeTable() {
        pairInside = new Pair();
        pairRightInside = new Pair();
        pairLeftInside = new Pair();
        rightInside = new Right();
        leftInside = new Left();
        rightOutside = new Right();
        leftOutside = new Left();
        pairRightOutside = new Pair();
        pairLeftOutside = new Pair();
        pairOutside = new Pair();
        complete = 0;
    }

    protected void fillTable() {
        for (int l = 2; l <= n; l++) {
            combinePairInside(l);
            combineRightInside(l);
            combineLeftInside(l);
        }
        combineComplete();
        for (int l = n; l >= 2; l--) {
            combineRightOutside(l);
            combineLeftOutside(l);
            combinePairOutside(l);
        }
    }

    /**
     * Weight of dependency (i,j), where i is head and j is dependent.
     *
     * @param i
     * @param j
     */
    protected abstract int weight(final int i, final int j);

    /**
     * Weight of best structure including dependency (i, j).
     */
    public int bestWeight(final int i, final int j) {
        if (i < j)
            return pairRightOutside.get(i, j) + pairRightInside.get(i, j);
        else
            return pairLeftOutside.get(j, i) + pairLeftInside.get(j, i);
    }

    /**
     * Maximum weight of substructure, with i having right dependents up to j.
     */
    protected class Right {
        private int[][] weights;

        public Right() {
            weights = new int[n][];
            for (int i = 0; i < n; i++)
                weights[i] = new int[n - i];
        }

        public int get(final int i, final int j) {
            return weights[i][j - i];
        }

        public void set(final int i, final int j, final int w) {
            weights[i][j - i] = w;
        }

        public void update(final int i, final int j, final int w) {
            set(i, j, Math.max(get(i, j), w));
        }
    }

    /**
     * Maximum weight of substructure, with j having left dependents down to i.
     */
    protected class Left {
        private int[][] weights;

        public Left() {
            weights = new int[n][];
            for (int j = 0; j < n; j++)
                weights[j] = new int[j + 1];
        }

        public int get(final int i, final int j) {
            return weights[j][j - i];
        }

        public void set(final int i, final int j, final int w) {
            weights[j][j - i] = w;
        }

        public void update(final int i, final int j, final int w) {
            set(i, j, Math.max(get(i, j), w));
        }
    }

    /**
     * Maximum weight of two consecutive substructures, with
     * i having right dependents up to some k,
     * j having left dependents down to k+1,
     * where i <= k < j.
     */
    protected class Pair {
        private int[][] weights;

        public Pair() {
            weights = new int[n - 1][];
            for (int i = 0; i < n - 1; i++)
                weights[i] = new int[n - i - 1];
        }

        public int get(final int i, final int j) {
            return weights[i][j - i - 1];
        }

        public void set(final int i, final int j, final int w) {
            weights[i][j - i - 1] = w;
        }

        public void update(final int i, final int j, final int w) {
            set(i, j, Math.max(get(i, j), w));
        }
    }

    protected void combinePairInside(final int l) {
        for (int l1 = 1; l1 < l; l1++) {
            for (int i = 0; i <= n - l; i++) {
                final int k = i + l1 - 1;
                final int j = i + l - 1;
                final int w = rightInside.get(i, k) +
                        leftInside.get(k + 1, j);
                pairInside.update(i, j, w);
            }
        }
        for (int i = 0; i <= n - l; i++) {
            final int j = i + l - 1;
            final int w = pairInside.get(i, j);
            final int w1 = w + weight(i, j);
            final int w2 = w + weight(j, i);
            pairRightInside.update(i, j, w1);
            pairLeftInside.update(i, j, w2);
        }
    }

    protected void combineRightInside(final int l) {
        for (int l1 = 2; l1 <= l; l1++) {
            for (int i = 0; i <= n - l; i++) {
                final int k = i + l1 - 1;
                final int j = i + l - 1;
                final int w = pairRightInside.get(i, k) + rightInside.get(k, j);
                rightInside.update(i, j, w);
            }
        }
    }

    protected void combineLeftInside(final int l) {
        for (int l1 = 1; l1 < l; l1++) {
            for (int i = 0; i <= n - l; i++) {
                final int k = i + l1 - 1;
                final int j = i + l - 1;
                final int w = leftInside.get(i, k) + pairLeftInside.get(k, j);
                leftInside.update(i, j, w);
            }
        }
    }

    protected void combineComplete() {
        complete = rightInside.get(0, n - 1);
        rightOutside.update(0, n - 1, 0);
    }

    protected void combineRightOutside(final int l) {
        for (int l1 = 2; l1 <= l; l1++) {
            for (int i = 0; i <= n - l; i++) {
                final int k = i + l1 - 1;
                final int j = i + l - 1;
                final int w = rightOutside.get(i, j);
                final int w1 = pairRightInside.get(i, k);
                final int w2 = rightInside.get(k, j);
                pairRightOutside.update(i, k, w + w2);
                rightOutside.update(k, j, w + w1);
            }
        }
    }

    protected void combineLeftOutside(final int l) {
        for (int l1 = 1; l1 < l; l1++) {
            for (int i = 0; i <= n - l; i++) {
                final int k = i + l1 - 1;
                final int j = i + l - 1;
                final int w = leftOutside.get(i, j);
                final int w1 = leftInside.get(i, k);
                final int w2 = pairLeftInside.get(k, j);
                leftOutside.update(i, k, w + w2);
                pairLeftOutside.update(k, j, w + w1);
            }
        }
    }

    protected void combinePairOutside(final int l) {
        for (int i = 0; i <= n - l; i++) {
            final int j = i + l - 1;
            final int w1 = pairRightOutside.get(i, j) + weight(i, j);
            final int w2 = pairLeftOutside.get(i, j) + weight(j, i);
            pairOutside.update(i, j, w1);
            pairOutside.update(i, j, w2);
        }
        for (int l1 = 1; l1 < l; l1++) {
            for (int i = 0; i <= n - l; i++) {
                final int k = i + l1 - 1;
                final int j = i + l - 1;
                final int w = pairOutside.get(i, j);
                final int w1 = rightInside.get(i, k);
                final int w2 = leftInside.get(k + 1, j);
                rightOutside.update(i, k, w + w2);
                leftOutside.update(k + 1, j, w + w1);
            }
        }
    }

    // For testing.
    public static void main(String[] args) {
        CubicDependency parser = new CubicDependency(50) {
            public int weight(final int i, final int j) {
                if (i == 1)
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
    }

}
