/*
 * Copyright (c) 2019. University of St Andrews
 */

package standrews.depmethods;

import standrews.depautomata.DependencyVertex;
import standrews.depbase.generation.DependencyStructureGenerator;
import standrews.depautomata.SimpleConfig;
import standrews.depbase.Token;
import standrews.tabular.CubicSplitBilex;
import standrews.tabular.ComplexShiftReduceBilexGrammar;
import standrews.tabular.ShiftReduceBilexGrammar;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SimpleDynamicParserTesterOld extends SimpleParser {

    final boolean strict;

    /**
     * Simple grammar for parsing configuration.
     */
    private ShiftReduceBilexGrammar simpleGram;
    /**
     * Complex grammar for parsing configuration.
     */
    private ComplexShiftReduceBilexGrammar complexGram;

    public SimpleDynamicParserTesterOld(final Token[] tokens,
                                        final boolean leftDependentsFirst,
                                        final boolean strict) {
        super(tokens);
        setLeftDependentsFirst(leftDependentsFirst);
        this.strict = strict;
        simpleGram = new ShiftReduceBilexGrammar(leftDependentsFirst, strict);
        complexGram = new ComplexShiftReduceBilexGrammar(leftDependentsFirst, strict);
    }

    /**
     * Determine which action is needed in current configuration.
     *
     * @param config Current configuration.
     * @return The action.
     */
    @Override
    protected String[] getAction(final SimpleConfig config) {
        if (config.prefixLength() >= 2) {
            TreeSet<String> predictedCubicSimple = predictCubicSimple(config);
            TreeSet<String> predictedCubicComplex = predictCubicComplex(config);
            TreeSet<String> predictedQuad = predictQuad(config);
            if (!toString(cubicSimpleScores(config)).equals(toString(cubicComplexScores(config)))) {
                if (config.prefixLength() + config.suffixLength() < 20) {
                    System.out.println("simple " + toString(cubicSimpleScores(config)));
                    System.out.println();
                    System.out.println("complex " + toString(cubicComplexScores(config)));
                    printConfig(config);
                }
            } else if (!toString(cubicSimpleScores(config)).equals(toString(quadScores(config)))) {
                if (config.prefixLength() + config.suffixLength() < 20) {
                    System.out.println("simple " + toString(cubicSimpleScores(config)));
                    System.out.println();
                    System.out.println("quad " + toString(quadScores(config)));
                    printConfig(config);
                    printConfigPruned(config);
                    System.out.println();
                }
            }
            final DependencyVertex v0 = config.getPrefixRight(0);
            final DependencyVertex v1 = config.getPrefixRight(1);
            final Token t0 = v0.getToken();
            final Token t1 = v1.getToken();
            if (graph.isDependentOf(t0.id, t1.id) &&
                    areChildrenAttached(v0)) {
                final String deprel = deprel(t0);
				/*
				if (!toString(predictedCubicSimple).equals("reduceLeft"))
					System.out.println("cub simple: " + toString(predictedCubicSimple) + " VERSUS reduceLeft");
				if (!toString(predictedCubicComplex).equals("reduceLeft"))
					System.out.println("cub complex: " + toString(predictedCubicComplex) + " VERSUS reduceLeft");
				if (!toString(predictedQuad).equals("reduceLeft")) {
					System.out.println("quad: " + toString(predictedQuad) + " VERSUS reduceLeft");
					printConfig(config);
				}
				*/
                return new String[]{"reduceLeft", deprel};
            }
            if (graph.isDependentOf(t1.id, t0.id) &&
                    areChildrenAttached(v1) &&
                    (leftDependentsFirst || areRightChildrenAttached(v0))) {
                final String deprel = deprel(t1);
				/*
				if (!toString(predictedCubicSimple).equals("reduceRight"))
					System.out.println("cub simple: " + toString(predictedCubicSimple) + " VERSUS reduceRight");
				if (!toString(predictedCubicComplex).equals("reduceRight"))
					System.out.println("cub complex: " + toString(predictedCubicComplex) + " VERSUS reduceRight");
				if (!toString(predictedQuad).equals("reduceRight")) {
					System.out.println("quad: " + toString(predictedQuad) + " VERSUS reduceRight");
					printConfig(config);
				}
				*/
                return new String[]{"reduceRight", deprel};
            }
        }
        if (config.suffixLength() > 0) {
            return new String[]{"shift"};
        }
        return new String[]{};
    }

    /**
     * NONE means that the component has not yet been used.
     * CONSUMED means that the current node has been consumed.
     * FRESH means that the component has been entered, but the current node
     * has not been consumed.
     */
    private enum CompState {NONE, USED, UNUSED}

    ;
    private static final int nStates = CompState.values().length;

    /**
     * How many edges are found hereafter by the ideal computation,
     * for a prefix of a stack and an element in the remainder
     * of the stack, or in the remaining input.
     */
    private class PotentialGains {
        /**
         * Total stack size.
         */
        final public int m;
        /**
         * Remaining input size.
         */
        final public int n;
        /**
         * gain[i][k] is maximal number of edges found with
         * prefix of the total stack up to element with index i,
         * and element with index k+i+1.
         * With boolean that says whether a link is applied to the current component.
         */
        final private int[][][] gains;

        /**
         * @param m
         */
        public PotentialGains(final int m, final int n) {
            this.m = m;
            this.n = n;
            gains = new int[m - 1][][];
            for (int i = 0; i < m - 1; i++) {
                gains[i] = new int[m - 1 - i + n][];
                for (int j = 0; j < m - 1 - i + n; j++) {
                    gains[i][j] = new int[nStates];
                    for (int state = 0; state < nStates; state++) {
                        gains[i][j][state] = zero();
                    }
                }
            }
        }

        public int getStack(final int i, final int j, final CompState state) {

            return gains[i][j - i - 1][state.ordinal()];
        }

        public int getInput(final int i, final int j, final CompState state) {
            return gains[i][j + m - 1 - i][state.ordinal()];
        }

        public void setStack(final int i, final int j, final CompState state, final int g) {

            gains[i][j - i - 1][state.ordinal()] = g;
        }

        public void setInput(final int i, final int j, final CompState state, final int g) {
            gains[i][j + m - 1 - i][state.ordinal()] = g;
        }
    }

    /**
     * Explores information that suggests possible parse actions,
     * by quadratic time analysis.
     *
     * @param config
     * @return
     */
    private TreeSet<String> predictQuad(final SimpleConfig config) {
        return getActions(quadScores(config));
    }

    private TreeMap<String, Integer> quadScores(final SimpleConfig config) {
        final TreeMap<String, Integer> scores = initScores();
        ArrayList<DependencyVertex> prefixList = prefix(config);
        ArrayList<DependencyVertex> suffixList = suffix(config);
        int m = prefixList.size();
        int n = suffixList.size();
        final boolean restricted = leftDependentsFirst && strict;
        if (m > 2) {
            ArrayList<DependencyVertex> rightList = new ArrayList(prefixList.subList(0, m - 2));
            rightList.add(prefixList.get(m - 1));
            boolean[] rightBlock = new boolean[m - 1];
            if (restricted) {
                for (int i = 0; i < m - 2; i++)
                    rightBlock[i] = true;
            }
            final int rightScore = times(
                    edgeWeight(prefixList.get(m - 1), prefixList.get(m - 2)),
                    computeSquadScore(rightList, suffixList, rightBlock, !leftDependentsFirst));
            scores.put("reduceRight", rightScore);
        }
        ArrayList<DependencyVertex> leftList = new ArrayList<>(prefixList.subList(0, m - 1));
        boolean[] leftBlock = new boolean[m - 1];
        if (restricted) {
            for (int i = 0; i < m - 1; i++)
                leftBlock[i] = true;
        }
        final int leftScore = times(
                edgeWeight(prefixList.get(m - 2), prefixList.get(m - 1)),
                computeSquadScore(leftList, suffixList, leftBlock, false));
        scores.put("reduceLeft", leftScore);
        if (n > 0) {
            prefixList.add(suffixList.remove(0));
            boolean[] shiftBlock = new boolean[m + 1];
            if (leftDependentsFirst) {
                shiftBlock[m - 1] = true;
                if (strict)
                    for (int i = 0; i < m - 1; i++)
                        shiftBlock[i] = true;
            }
            final int shiftScore = computeSquadScore(prefixList, suffixList, shiftBlock, false);
            scores.put("shift", shiftScore);
        }
        return scores;
    }

    private boolean trace = false;

    private int computeSquadScore(final ArrayList<DependencyVertex> prefixList,
                                  final ArrayList<DependencyVertex> suffixList,
                                  final boolean[] leftBlock, final boolean rightBlock) {
        final int m = prefixList.size();
        final int n = suffixList.size();
        final ArrayList<DependencyVertex> prunedSuffixList = prunedSuffix(suffixList);
        final int np = prunedSuffixList.size();
        final int pruneScore = n - np;
        final int internalScore = internalScore(prunedSuffixList);
        final PotentialGains gains = new PotentialGains(m, np);
        final int childScore =
                rightBlock ? one() :
                        childLinks(prefixList, prunedSuffixList, m - 1, false);
        if (m < 2) {
            return times(childScore, times(pruneScore, internalScore));
        }
        if (trace) {
            System.out.println();
            System.out.println("ROOT " + (m - 2) + " " + (m - 1));
        }
        final int stackScore = scoreStack(prefixList, prunedSuffixList, m - 2, m - 1,
                CompState.NONE, gains, leftBlock, 0);
        // System.out.println("pruneScore " + pruneScore);
        // System.out.println("internalScore " + internalScore);
        // System.out.println("stackScore " + stackScore);
        // System.out.println("total " + times(times(pruneScore, internalScore), stackScore));
        return times(times(times(childScore, pruneScore),
                        internalScore),
                stackScore);
    }

    private int scoreStack(final ArrayList<DependencyVertex> prefixList,
                           final ArrayList<DependencyVertex> suffixList,
                           final int i, final int j, final CompState state,
                           final PotentialGains gains,
                           final boolean[] block,
                           int depth) {
        if (i < 0)
            return one();
        if (trace) {
            indent(depth);
            System.out.println("START stack " + i + " " + j + " " + state);
        }
        int g = gains.getStack(i, j, state);
        if (g != zero()) {
            if (trace) {
                indent(depth);
                System.out.println("mem " + g);
            }
            return g;
        }
        g = one();

        if (!block[j]) {
            int leftLink = leftLinkViaStack(prefixList, i, j);
            if (leftLink < 0)
                leftLink = leftLinkViaInput(prefixList, suffixList, i, j);
            if (leftLink >= 0) {
                int iLeft = skipLink(prefixList, leftLink, i);
                int gRight = times(
                        edgeWeight(prefixList.get(j), prefixList.get(i)),
                        scoreStack(prefixList, suffixList, iLeft, j, state, gains, block, depth + 1));
                g = plus(g, gRight);
            }
        }

        CompState stateLeft = updateStack(prefixList, suffixList, state, j, i);
        boolean childLoss = state != CompState.NONE &&
                childRepresentative(prefixList, suffixList, i) ==
                        rightParentRepresentative(prefixList, suffixList, j);
        int gChildren = childLinks(prefixList, suffixList, i, childLoss);
        int gLeft = times(times(gChildren,
                        edgeWeight(prefixList.get(i), prefixList.get(j))),
                scoreStack(prefixList, suffixList, i - 1, i, stateLeft, gains, block, depth + 1));
        g = plus(g, gLeft);

        int shiftLink = parentIndex(prefixList, suffixList, j);
        if (shiftLink >= 0 && state != CompState.USED) {
            int gShift = times(
                    state == CompState.NONE ? 1 : 0,
                    scoreInput(prefixList, suffixList, i, shiftLink, CompState.USED, gains, block, depth + 1));
            g = plus(g, gShift);
        }

        if (trace) {
            indent(depth);
            System.out.println("END stack " + i + " " + j + " " + state + " " + g);
        }
        gains.setStack(i, j, state, g);
        return g;
    }

    private int scoreInput(final ArrayList<DependencyVertex> prefixList,
                           final ArrayList<DependencyVertex> suffixList,
                           final int i, final int j, final CompState state,
                           final PotentialGains gains, final boolean[] block,
                           int depth) {
        if (i < 0)
            return one();
        if (trace) {
            indent(depth);
            System.out.println("START input " + i + " " + j + " " + state);
        }
        int g = gains.getInput(i, j, state);
        if (g != zero()) {
            if (trace) {
                indent(depth);
                System.out.println("mem " + g);
            }
            return g;
        }
        g = one();

        int jNew = componentNext(prefixList, suffixList, i, j);
        if (jNew >= 0) {
            int gRight = times(
                    1,
                    scoreInput(prefixList, suffixList, i - 1, jNew, state, gains, block, depth + 1));
            g = plus(g, gRight);
        } else {
            int leftLink = leftLinkViaComponent(prefixList, suffixList, i, j);
            if (leftLink >= 0) {
                int iLeft = skipLink(prefixList, leftLink, i);
                int gRight =
                        scoreInput(prefixList, suffixList, iLeft, j, state, gains, block, depth + 1);
                g = plus(g, gRight);
            }
        }

        CompState shiftState = updateInput(prefixList, suffixList, state, j, i);
        int gChildren = childLinks(prefixList, suffixList, i, false);
        int gLeft = times(
                gChildren,
                scoreStack(prefixList, suffixList, i - 1, i, shiftState, gains, block, depth + 1));
        g = plus(g, gLeft);

        if (trace) {
            indent(depth);
            System.out.println("END input " + i + " " + j + " " + state + " " + g);
        }
        gains.setInput(i, j, state, g);
        return g;
    }

    /**
     * The rightmost node up to i, linked to j. Return -1 if none.
     *
     * @param prefixList
     * @param j
     * @return
     */
    private int leftLinkViaStack(final ArrayList<DependencyVertex> prefixList,
                                 final int i, final int j) {
        for (int k = i; k >= 0; k--)
            if (k < i && isLink(prefixList.get(k), prefixList.get(j)) ||
                    isEdge(prefixList.get(j), prefixList.get(k)))
                return k;
        return -1;
    }

    /**
     * The leftmost node greater than m, and smaller than i,
     * such that there is a link between that node and i.
     * If there is no such link, then i-1.
     * The returned value should be at least m+1.
     *
     * @param prefixList
     * @param i
     * @return
     */
    private int skipLink(final ArrayList<DependencyVertex> prefixList,
                         final int m, final int i) {
        if (m < i - 1)
            for (int k = m; k < i; k++)
                if (isLink(prefixList.get(k), prefixList.get(i)))
                    return Math.max(k, m + 1);
        return i - 1;
    }

    /**
     * The rightmost node up to i, linked to j via remaining input. Return -1 if none.
     */
    private int leftLinkViaInput(final ArrayList<DependencyVertex> prefixList,
                                 final ArrayList<DependencyVertex> suffixList,
                                 final int i, final int j) {
        final int p = parentIndex(prefixList, suffixList, j);
        return p < 0 ? -1 : leftLinkViaComponent(prefixList, suffixList, i, p);
    }

    private int leftLinkViaComponent(final ArrayList<DependencyVertex> prefixList,
                                     final ArrayList<DependencyVertex> suffixList,
                                     final int i, final int j) {
        final int r = componentRepresentative(suffixList, j);
        for (int k = i; k >= 0; k--)
            if (parentChildComponent(prefixList.get(k), suffixList) == r)
                return k == i ? -1 : k;
        return -1;
    }

    /**
     * TODO
     *
     * @param prefixList
     * @param i
     * @param j
     * @return
     */
    private int componentNext(final ArrayList<DependencyVertex> prefixList,
                              final ArrayList<DependencyVertex> suffixList,
                              final int i, final int j) {
        int ri = parentRepresentative(prefixList, suffixList, i);
        int rj = componentRepresentative(suffixList, j);
        return ri == rj ? parentIndex(prefixList, suffixList, i) : -1;
    }

    /**
     * Translate vertex to position in suffix. Or -1 if vertex is not in suffix.
     */
    private int vertexToSuffixIndex(final DependencyVertex v,
                                    final ArrayList<DependencyVertex> suffixList) {
        for (int k = 0; k < suffixList.size(); k++) {
            if (suffixList.get(k).getToken().id.equals(v.getToken().id))
                return k;
        }
        return -1;
    }

    /**
     * Index of parent in suffix, given index in prefix.
     * Return -1 if none.
     */
    private int parentIndex(final ArrayList<DependencyVertex> prefixList,
                            final ArrayList<DependencyVertex> suffixList,
                            final int i) {
        for (int k = 0; k < suffixList.size(); k++)
            if (isEdge(suffixList.get(k), prefixList.get(i)))
                return k;
        return -1;
    }

    private int childIndex(final ArrayList<DependencyVertex> prefixList,
                           final ArrayList<DependencyVertex> suffixList,
                           final int i) {
        for (int k = 0; k < suffixList.size(); k++)
            if (isEdge(prefixList.get(i), suffixList.get(k)))
                return k;
        return -1;
    }

    private int parentChildIndex(final ArrayList<DependencyVertex> prefixList,
                                 final ArrayList<DependencyVertex> suffixList,
                                 final int i) {
        for (int k = 0; k < suffixList.size(); k++)
            if (isLink(suffixList.get(k), prefixList.get(i)))
                return k;
        return -1;
    }

    private int parentChildComponent(final DependencyVertex v,
                                     final ArrayList<DependencyVertex> suffixList) {
        for (int k = 0; k < suffixList.size(); k++)
            if (isLink(suffixList.get(k), v))
                return componentRepresentative(suffixList, k);
        return -1;
    }

    /**
     * Get index of ancestor that uniquely determines component of edges in remaining input.
     *
     * @param i
     * @param config
     * @return
     */
    private int componentRepresentative(final ArrayList<DependencyVertex> suffixList,
                                        final int i) {
        final DependencyVertex parent = goldVertex(suffixList.get(i)).getParent();
        if (vertexToSuffixIndex(parent, suffixList) < 0)
            return i;
        else
            return componentRepresentative(suffixList, vertexToSuffixIndex(parent, suffixList));
    }

    /**
     * Get representative of component of parent in suffix.
     * Return -1 if the parent is not in suffix.
     */
    private int parentRepresentative(final ArrayList<DependencyVertex> prefixList,
                                     final ArrayList<DependencyVertex> suffixList,
                                     final int i) {
        final int p = parentIndex(prefixList, suffixList, i);
        return p < 0 ? -1 : componentRepresentative(suffixList, p);
    }

    private int childRepresentative(final ArrayList<DependencyVertex> prefixList,
                                    final ArrayList<DependencyVertex> suffixList,
                                    final int i) {
        final int p = childIndex(prefixList, suffixList, i);
        return p < 0 ? -1 : componentRepresentative(suffixList, p);
    }

    private int parentChildRepresentative(final ArrayList<DependencyVertex> prefixList,
                                          final ArrayList<DependencyVertex> suffixList,
                                          final int i) {
        final int p = parentChildIndex(prefixList, suffixList, i);
        return p < 0 ? -1 : componentRepresentative(suffixList, p);
    }

    /**
     * Child
     *
     * @param prefixList
     * @param suffixList
     * @param j
     * @return
     */
    private int childLinks(final ArrayList<DependencyVertex> prefixList,
                           final ArrayList<DependencyVertex> suffixList,
                           final int i, final boolean used) {
        int c = 0;
        for (int k = 0; k < suffixList.size(); k++)
            if (isEdge(prefixList.get(i), suffixList.get(k)))
                c++;
        return !used ? c : Math.max(0, c - 1);
    }

    private CompState updateStack(final ArrayList<DependencyVertex> prefixList,
                                  final ArrayList<DependencyVertex> suffixList,
                                  final CompState oldState, final int iOld, final int iNew) {
        if (oldState == CompState.USED || oldState == CompState.UNUSED) {
            int pOld = rightParentIndex(prefixList, suffixList, iOld);
            int rOld = rightParentRepresentative(prefixList, suffixList, iOld);
            int pNew = leftParentChildIndex(prefixList, suffixList, iNew);
            int rNew = leftParentChildRepresentative(prefixList, suffixList, iNew);
            if (rNew != rOld)
                return CompState.NONE;
            else if (pOld == pNew)
                return oldState;
            else
                return CompState.UNUSED;
        } else {
            return CompState.NONE;
        }
    }

    private CompState updateInput(final ArrayList<DependencyVertex> prefixList,
                                  final ArrayList<DependencyVertex> suffixList,
                                  final CompState oldState, final int iOld, final int iNew) {
        int rOld = componentRepresentative(suffixList, iOld);
        int pNew = leftParentChildIndex(prefixList, suffixList, iNew);
        int rNew = leftParentChildRepresentative(prefixList, suffixList, iNew);
        if (rNew != rOld)
            return CompState.NONE;
        else if (iOld == pNew)
            return oldState;
        else
            return CompState.UNUSED;
    }

    private int rightParentIndex(final ArrayList<DependencyVertex> prefixList,
                                 final ArrayList<DependencyVertex> suffixList,
                                 final int i) {
        for (int k = i; k < prefixList.size(); k++) {
            int p = parentIndex(prefixList, suffixList, k);
            if (p >= 0)
                return p;
        }
        return -1;
    }

    private int rightParentRepresentative(final ArrayList<DependencyVertex> prefixList,
                                          final ArrayList<DependencyVertex> suffixList,
                                          final int i) {
        for (int k = i; k < prefixList.size(); k++)
            if (parentIndex(prefixList, suffixList, k) >= 0)
                return parentRepresentative(prefixList, suffixList, k);
        return -1;
    }

    private int leftParentChildIndex(final ArrayList<DependencyVertex> prefixList,
                                     final ArrayList<DependencyVertex> suffixList,
                                     final int i) {
        for (int k = i; k >= 0; k--) {
            if (k == i) {
                int p = parentIndex(prefixList, suffixList, k);
                if (p >= 0)
                    return p;
            } else {
                int p = parentChildIndex(prefixList, suffixList, k);
                if (p >= 0)
                    return p;
            }
        }
        return -1;
    }

    private int leftParentChildRepresentative(final ArrayList<DependencyVertex> prefixList,
                                              final ArrayList<DependencyVertex> suffixList,
                                              final int i) {
        for (int k = i; k >= 0; k--) {
            if (k == i) {
                if (parentIndex(prefixList, suffixList, k) >= 0)
                    return parentRepresentative(prefixList, suffixList, k);
            } else {
                if (parentChildIndex(prefixList, suffixList, k) >= 0)
                    return parentChildRepresentative(prefixList, suffixList, k);
            }
        }
        return -1;
    }

    /**
     * Explores information that suggests possible parse actions,
     * using several simple grammars.
     */
    private TreeSet<String> predictCubicSimple(final SimpleConfig config) {
        return getActions(cubicSimpleScores(config));
    }

    private TreeMap<String, Integer> cubicSimpleScores(final SimpleConfig config) {
        final ArrayList<DependencyVertex> prefixList = prefix(config);
        final ArrayList<DependencyVertex> suffixList = suffix(config);
        final List<DependencyVertex> combiList =
                Stream.concat(prefixList.stream(), suffixList.stream())
                        .collect(Collectors.toList());
        final int m = prefixList.size();
        final int n = suffixList.size();
        final TreeMap<String, Integer> scores = initScores();
        if (m > 2) {
            final String[] inRight = ShiftReduceBilexGrammar.getInput(m - 1, n);
            final CubicSplitBilex parserRight = new CubicSplitBilex(simpleGram, inRight) {
                public int weight(final int i, final int j) {
                    final int ip = i < m - 2 ? i : i + 1;
                    final int jp = j < m - 2 ? j : j + 1;
                    if (!leftDependentsFirst && i == m - 2 && j > m - 2)
                        return zero();
                    else
                        return edgeWeight(combiList.get(ip), combiList.get(jp));
                }
            };
            scores.put("reduceRight", parserRight.rootWeight(ShiftReduceBilexGrammar.suf) +
                    edgeWeight(combiList.get(m - 1), combiList.get(m - 2)));
        }
        final String[] inLeft = ShiftReduceBilexGrammar.getInput(m - 1, n);
        final CubicSplitBilex parserLeft = new CubicSplitBilex(simpleGram, inLeft) {
            public int weight(final int i, final int j) {
                final int ip = i < m - 1 ? i : i + 1;
                final int jp = j < m - 1 ? j : j + 1;
                if (leftDependentsFirst && strict && i == m - 2 && j < m - 2)
                    return zero();
                else
                    return edgeWeight(combiList.get(ip), combiList.get(jp));
            }
        };
        scores.put("reduceLeft", parserLeft.rootWeight(ShiftReduceBilexGrammar.suf) +
                edgeWeight(combiList.get(m - 2), combiList.get(m - 1)));
        if (n > 0) {
            final String[] inShift = ShiftReduceBilexGrammar.getInput(m + 1, n - 1);
            final CubicSplitBilex parserShift = new CubicSplitBilex(simpleGram, inShift) {
                public int weight(final int i, final int j) {
                    if (leftDependentsFirst && i == m - 1 && j < m - 1)
                        return zero();
                    else
                        return edgeWeight(combiList.get(i), combiList.get(j));
                }
            };
            scores.put("shift", parserShift.rootWeight(ShiftReduceBilexGrammar.suf));
        }
        return scores;
    }

    /**
     * Explores information that suggests possible parse actions,
     * using one complex grammar.
     */
    private TreeSet<String> predictCubicComplex(final SimpleConfig config) {
        return getActions(cubicComplexScores(config));
    }

    private TreeMap<String, Integer> cubicComplexScores(final SimpleConfig config) {
        final ArrayList<DependencyVertex> prefixList = prefix(config);
        final ArrayList<DependencyVertex> suffixList = suffix(config);
        final List<DependencyVertex> combiList =
                Stream.concat(prefixList.stream(), suffixList.stream())
                        .collect(Collectors.toList());
        final int m = prefixList.size();
        final int n = suffixList.size();
        final String[] in = ComplexShiftReduceBilexGrammar.getInput(m, n);
        final CubicSplitBilex parser = new CubicSplitBilex(complexGram, in) {
            public int weight(final int i, final int j) {
                return edgeWeight(combiList.get(i), combiList.get(j));
            }
        };

        int reduceRight = zero();
        int reduceLeft = zero();
        int shift = zero();
        final String[] rightNonts =
                leftDependentsFirst ?
                        new String[]{complexGram.ultPen, complexGram.ultPenChildren} :
                        new String[]{complexGram.ultPen};
        final String[] leftNonts =
                new String[]{complexGram.penUltNochildren};
        final String[] shiftNonts =
                leftDependentsFirst ?
                        new String[]{complexGram.suf, complexGram.penUltChildren} :
                        new String[]{complexGram.suf, complexGram.ultPenChildren, complexGram.penUltChildren};
        for (String delex : rightNonts)
            reduceRight = plus(reduceRight, parser.rootWeight(delex));
        for (String delex : leftNonts)
            reduceLeft = plus(reduceLeft, parser.rootWeight(delex));
        for (String delex : shiftNonts)
            shift = plus(shift, parser.rootWeight(delex));
        TreeMap<String, Integer> scores = scores(reduceRight, reduceLeft, shift);
		/*
		System.out.println("cub right " + reduceRight);
		System.out.println("cub left " + reduceLeft);
		System.out.println("cub shift " + shift);
		for (int i = 0; i < in.length; i++)
			System.out.print(" " + in[i]);
		System.out.println();

		for (int i = 0; i < in.length; i++)
			for (int j = 0; j < in.length; j++) {
				final Token ti = prunedList.get(i).getToken();
				final Token tj = prunedList.get(j).getToken();
				if (graph.isDependentOf(tj.id, ti.id))
					System.out.println("" + i + " " + j);
			}

		System.out.println();
		*/
        return scores;
    }

    private ArrayList<DependencyVertex> prefix(final SimpleConfig config) {
        final ArrayList<DependencyVertex> prefixList = new ArrayList<>();
        for (int i = 0; i < config.prefixLength(); i++)
            prefixList.add(config.getPrefixLeft(i));
        return prefixList;
    }

    private ArrayList<DependencyVertex> suffix(final SimpleConfig config) {
        final ArrayList<DependencyVertex> suffixList = new ArrayList<>();
        for (int i = 0; i < config.suffixLength(); i++)
            suffixList.add(config.getSuffixLeft(i));
        return suffixList;
    }

    /**
     * From suffix, take elements such that not all descendants are in suffix or
     * such that parent is not in suffix.
     */
	/*
	private Vector<DependencyVertex> prunedSuffix(final Vector<DependencyVertex> suffixList) {
		final TreeSet<DependencyVertex> suffixSet =	new TreeSet<>(suffixList);
		return prunedSuffix(graph.root, suffixSet, false);
	}
	*/

    /**
     * From suffix, take elements such that not all descendants are in suffix or
     * such that parent is not in suffix.
     * @param vertex
     * @param suffixSet
     * @return
     */
	/*
	private Vector<DependencyVertex> prunedSuffix(final DependencyVertex vertex,
												  final TreeSet<DependencyVertex> suffixSet,
												  final boolean parentInSuffix) {
		final Vector<DependencyVertex> l = new Vector<>();
		final boolean inSuffix = suffixSet.contains(vertex);
		final DependencyVertex[] descs = vertex.getDescendants();
		final boolean allInSuffix = suffixSet.containsAll(Arrays.asList(descs));
		if (!inSuffix) {
			for (DependencyVertex desc : vertex.getRightChildren())
				l.addAll(prunedSuffix(desc, suffixSet, inSuffix));
		} else if (allInSuffix) {
			if (!parentInSuffix)
				l.add(vertex);
		} else {
			for (DependencyVertex desc : vertex.getLeftChildren())
				l.addAll(prunedSuffix(desc, suffixSet, inSuffix));
			l.add(vertex);
			for (DependencyVertex desc : vertex.getRightChildren())
				l.addAll(prunedSuffix(desc, suffixSet, inSuffix));
		}
		return l;
	}
	*/

    /**
     * Number of edges internally.
     *
     * @param suffixList
     * @return
     */
    private int internalScore(final ArrayList<DependencyVertex> suffixList) {
        final TreeSet<DependencyVertex> suffixSet = new TreeSet<>(suffixList);
        int s = 0;
        for (int i = 0; i < suffixList.size(); i++) {
            final DependencyVertex p = goldVertex(suffixList.get(i)).getParent();
            if (suffixSet.contains(p)) {
                s++;
            }
        }
        return s;
    }

    /**
     * All scores are non-existent.
     *
     * @return
     */
    private TreeMap<String, Integer> initScores() {
        return scores(zero(), zero(), zero());
    }

    /**
     * Scores to mapping.
     *
     * @return
     */
    private TreeMap<String, Integer> scores(final int reduceRight,
                                            final int reduceLeft,
                                            final int shift) {
        TreeMap<String, Integer> scores = new TreeMap<>();
        scores.put("reduceRight", reduceRight);
        scores.put("reduceLeft", reduceLeft);
        scores.put("shift", shift);
        return scores;
    }

    private TreeSet<String> getActions(final TreeMap<String, Integer> scores) {
        final int reduceRight = scores.get("reduceRight");
        final int reduceLeft = scores.get("reduceLeft");
        final int shift = scores.get("shift");
        final int max = plus(reduceRight, plus(reduceLeft, shift));
        final TreeSet<String> actions = new TreeSet<>();
        if (reduceRight == max)
            actions.add("reduceRight");
        if (reduceLeft == max)
            actions.add("reduceLeft");
        if (shift == max)
            actions.add("shift");
        return actions;
    }

    /**
     * Print configurations with pruned suffix.
     */
    private void printConfigPruned(final SimpleConfig config) {
        ArrayList<DependencyVertex> prefixList = prefix(config);
        ArrayList<DependencyVertex> suffixList = suffix(config);
		/* RIGHT
		Vector<DependencyVertex> list = new Vector(prefixList.subList(0, prefixList.size() - 2));
		list.add(prefixList.get(prefixList.size() - 1));
		*/
		/* LEFT
		Vector<DependencyVertex> list = new Vector(prefixList.subList(0, prefixList.size() - 1));
		*/
        /* SHIFT */
        ArrayList<DependencyVertex> list = new ArrayList(prefixList);
        list.add(suffixList.remove(0));
        final ArrayList<DependencyVertex> prunedSuffixList = prunedSuffix(suffixList);
        System.out.println("pruned prefix/suffix " + list.size() + "/" +
                prunedSuffixList.size());
        final int m = list.size();
        final int n = prunedSuffixList.size();
        for (int i = 0; i < m + n; i++) {
            for (int j = 0; j < m + n; j++) {
                final Token ti = i < m ?
                        list.get(i).getToken() :
                        prunedSuffixList.get(i - m).getToken();
                final Token tj = j < m ?
                        list.get(j).getToken() :
                        prunedSuffixList.get(j - m).getToken();
                if (graph.isDependentOf(tj.id, ti.id))
                    System.out.println("" + i + " " + j);
            }
        }
        System.exit(1);
    }

    /**
     * Scores of actions as string.
     */
	/*
	private String toString(final TreeMap<String,Integer> scores) {
		final int[] vals = new int[]{
				scores.get("reduceRight"),
				scores.get("reduceLeft"),
				scores.get("shift")};
		String str = "";
		for (int i = 0; i < vals.length; i++) {
			if (vals[i] == zero())
				str += " -";
			else
				str += " " + vals[i];
		}
		return str;
	}
	*/
    private String toString(final TreeSet<String> actions) {
        return String.join(", ", actions);
    }

    private void indent(final int level) {
        System.out.print(String.join("",
                Collections.nCopies(level * 3, " ")));
    }

    public static void main(String[] args) {
        boolean leftFirst = false;
        boolean strict = false;
        boolean random = true;
        Random r = new Random();
        DependencyStructureGenerator gen = new DependencyStructureGenerator(20, 0.55);
        if (random) {
            for (int i = 0; i < 10000; i++) {
                Token[] struct = gen.generateDepStruct();
                SimpleDynamicParserTesterOld parser = new SimpleDynamicParserTesterOld(
                        struct, leftFirst, strict);
                parser.prepareTraining();
                final SimpleConfig config = parser.makeInitialConfig(struct);
                for (int j = 0; j < (struct.length - 1) / 2; j++)
                    parser.shift(config);
                parser.getAction(config);
                int nRemove = r.nextInt(5);
                while (nRemove > 0 && config.prefixLength() > 1) {
                    int rem = r.nextInt(config.prefixLength() - 1) + 1;
                    config.removePrefixLeft(rem);
                    nRemove--;
                }
            }
        } else {
            int[][] edges = new int[][]{
                    {1, 2},
                    {2, 9},
                    {3, 7},
                    {4, 5},
                    {5, 3},
                    {6, 3},
                    {7, 2},
                    {8, 9},
                    {9, 0}
                    // {10,12},
                    //{11,10},
                    //{12,4},
                    //{13,14},
                    //{14,16},
                    //{15,14},
                    //{16,12}
            };
            Token[] struct = gen.generateDepStruct(edges);
            SimpleDynamicParserTesterOld parser = new SimpleDynamicParserTesterOld(
                    struct, leftFirst, strict);
            parser.prepareTraining();
            final SimpleConfig config = parser.makeInitialConfig(struct);
            for (int j = 0; j < (struct.length - 1) / 2; j++)
                parser.shift(config);
            parser.getAction(config);
        }
    }
}
