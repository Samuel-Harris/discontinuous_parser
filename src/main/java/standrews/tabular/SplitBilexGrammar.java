/*
 * Copyright (c) 2018. University of St Andrews
 */

package standrews.tabular;

import java.util.Arrays;
import java.util.HashMap;

/**
 * Split bilexical grammar, or split head automaton.
 * We have a subautomaton that recognizes children to the left of head,
 * and a subautomaton that recognized children to the right of the head.
 * The resulting left and right nonterminals are combined into
 * the 'middle' nonterminal.
 */
public class SplitBilexGrammar {
    /**
     * Array of all delexicalized nonterminals.
     */
    public final String[] delexs;

    /**
     * Transition in left automaton.
     */
    private HashMap<String, HashMap<String, String>> leftMap = new HashMap<>();

    /**
     * Transition in right automaton.
     */
    private HashMap<String, HashMap<String, String>> rightMap = new HashMap<>();

    /**
     * Maps left to map that maps right to middle nonterminal.
     */
    private HashMap<String, HashMap<String, String>> middleMap = new HashMap<>();

    public SplitBilexGrammar(final String[] delexs) {
        this.delexs = delexs;
    }

    public void addLeft(final String current, final String child, final String next) {
        if (leftMap.get(current) == null)
            leftMap.put(current, new HashMap<>());
        leftMap.get(current).put(child, next);
    }

    public void addRight(final String current, final String child, final String next) {
        if (rightMap.get(current) == null)
            rightMap.put(current, new HashMap<>());
        rightMap.get(current).put(child, next);
    }

    public void addMiddle(final String left, final String right, final String middle) {
        if (middleMap.get(left) == null)
            middleMap.put(left, new HashMap<>());
        middleMap.get(left).put(right, middle);
    }

    public String getLeft(final String current, final String child) {
        return leftMap.get(current) == null ? null : leftMap.get(current).get(child);
    }

    public String getRight(final String current, final String child) {
        return rightMap.get(current) == null ? null : rightMap.get(current).get(child);
    }

    public String getMiddle(final String left, final String right) {
        return middleMap.get(left) == null ? null : middleMap.get(left).get(right);
    }

    public int getNum(final String d) {
        return Arrays.asList(delexs).indexOf(d);
    }

    private int[][] cachedMiddle;

    private int[][] cachedLeft;

    private int[][] cachedRight;

    public void cacheNumMaps() {
        cachedLeft = new int[delexs.length][];
        for (int i = 0; i < delexs.length; i++) {
            cachedLeft[i] = new int[delexs.length];
            for (int j = 0; j < delexs.length; j++) {
                final String m = getLeft(delexs[i], delexs[j]);
                cachedLeft[i][j] = m == null ? -1 : getNum(m);
            }
        }
        cachedRight = new int[delexs.length][];
        for (int i = 0; i < delexs.length; i++) {
            cachedRight[i] = new int[delexs.length];
            for (int j = 0; j < delexs.length; j++) {
                final String m = getRight(delexs[i], delexs[j]);
                cachedRight[i][j] = m == null ? -1 : getNum(m);
            }
        }
        cachedMiddle = new int[delexs.length][];
        for (int i = 0; i < delexs.length; i++) {
            cachedMiddle[i] = new int[delexs.length];
            for (int j = 0; j < delexs.length; j++) {
                final String m = getMiddle(delexs[i], delexs[j]);
                cachedMiddle[i][j] = m == null ? -1 : getNum(m);
            }
        }
    }

    public int getLeftNum(final int current, final int child) {
        return current < 0 || child < 0 ? -1 : cachedLeft[current][child];
    }

    public int getRightNum(final int current, final int child) {
        return current < 0 || child < 0 ? -1 : cachedRight[current][child];
    }

    public int getMiddleNum(final int left, final int right) {
        return left < 0 || right < 0 ? -1 : cachedMiddle[left][right];
    }
}
