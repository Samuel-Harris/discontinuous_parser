/*
 * Copyright (c) 2019. University of St Andrews
 */

package standrews.depextract;

import standrews.aux.Counter;
import standrews.aux.PropertyWeights;
import standrews.aux.StringPair;
import standrews.aux.StringTriple;
import standrews.depautomata.HatConfig;
import standrews.depbase.Upos;
import standrews.depmethods.WholeHatParser;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class WholeHatExtractor {

    public static final String shift = WholeHatParser.shift;
    public static final String reduce = WholeHatParser.reduce;
    public static final String complete = WholeHatParser.complete;

    /**
     * How often is there choice whether hat can be placed on this or on state further to
     * the left.
     */
    private int lexhatCount = 0;

    /**
     * How often is there decision to place hat on this state.
     */
    private int lexhatThisCount = 0;

    public double pLexhat() {
        return 1.0 * (lexhatCount - lexhatThisCount) / lexhatCount;
    }

    /**
     * How often is there choice whether hat can be placed on this or on rule element further to
     * the left.
     */
    private int rulehatCount = 0;

    /**
     * How often is there decision to place hat on this state.
     */
    private int rulehatThisCount = 0;

    public double pRulehat() {
        return 1.0 * (rulehatCount - rulehatThisCount) / rulehatCount;
    }

    /**
     * How often could next stack element be a child.
     */
    private int leftCount = 0;

    /**
     * How often is next stack element a child.
     */
    private int leftThisCount = 0;

    public double pLeft() {
        return 1.0 * (leftCount - leftThisCount) / leftCount;
    }

    /**
     * How often could next stack element be a child.
     */
    private int rightCount = 0;

    /**
     * How often is next stack element a child.
     */
    private int rightThisCount = 0;

    public double pRight() {
        return 1.0 * (rightCount - rightThisCount) / rightCount;
    }

    private double posSmooth = 0.5;
    private double lastSmooth = 0.5;

    /**
     * How often is previous POS seen.
     */
    private Counter<StringPair> headPrevLeftCount = new Counter<>();
    /**
     * How often is this POS followed by other POS.
     */
    private Counter<StringTriple> headPrevNextLeftCount = new Counter<>();
    /**
     * How often is this last POS.
     */
    private Counter<StringPair> headPrevLastLeftCount = new Counter<>();

    public double pRuleNextLeft(String par, String prev, String cur) {
        double denom =
                headPrevLeftCount.get(new StringPair(par, prev));
        double num =
                headPrevNextLeftCount.get(new StringTriple(par, prev, cur));
        return (num + posSmooth) /
                (denom + lastSmooth + posSmooth * Upos.UPOSS.length);
    }

    public double pRuleLastLeft(String par, String prev) {
        double denom =
                headPrevLeftCount.get(new StringPair(par, prev));
        double num =
                headPrevLastLeftCount.get(new StringPair(par, prev));
        return (num + lastSmooth) /
                (denom + lastSmooth + posSmooth * Upos.UPOSS.length);
    }

    /**
     * How often is previous POS seen.
     */
    private Counter<StringPair> headPrevRightCount = new Counter<>();
    /**
     * How often is this POS followed by other POS.
     */
    private Counter<StringTriple> headPrevNextRightCount = new Counter<>();
    /**
     * How often is this last POS.
     */
    private Counter<StringPair> headPrevLastRightCount = new Counter<>();

    public double pRuleNextRight(String par, String prev, String cur) {
        double denom =
                headPrevRightCount.get(new StringPair(par, prev));
        double num =
                headPrevNextRightCount.get(new StringTriple(par, prev, cur));
        return (num + posSmooth) /
                (denom + lastSmooth + posSmooth * Upos.UPOSS.length);
    }

    public double pRuleLastRight(String par, String prev) {
        double denom =
                headPrevRightCount.get(new StringPair(par, prev));
        double num =
                headPrevLastRightCount.get(new StringPair(par, prev));
        return (num + lastSmooth) /
                (denom + lastSmooth + posSmooth * Upos.UPOSS.length);
    }

    public void extract(final HatConfig config, final String[] action) {
        switch (action[0]) {
            case shift:
                extractShift(config);
                break;
            case reduce: {
                int parent = Integer.parseInt(action[1]);
                int nChildren = Integer.parseInt(action[2]);
                int[] children = Arrays.stream(Arrays.copyOfRange(action, 3, 3 + nChildren))
                        .mapToInt(Integer::parseInt)
                        .toArray();
                String[] deprels = Arrays.copyOfRange(action, 3 + nChildren, 3 + 2 * nChildren);
                extractReduce(config, parent, children, deprels);
                break;
            }
        }
    }

    private void extractShift(final HatConfig config) {
        boolean[] mayCarryHat = new boolean[config.prefixLength()];
        for (int i = 0; i < config.prefixLength(); i++)
            mayCarryHat[i] = (i == config.prefixLength() - 1) ||
                    config.getLabelLeft(i).equals(complete);
        for (int i = config.prefixLength() - 1; i >= 0; i--) {
            if (mayCarryHat[i] && prefixHasTrue(mayCarryHat, i)) {
                lexhatCount++;
                if (i == config.getAbsoluteHatIndex()) {
                    lexhatThisCount++;
                    break;
                }
            }
        }
    }

    private void extractReduce(final HatConfig config,
                               int parent, int[] children, String[] deprels) {
        Set<Integer> elemSet = new TreeSet<>(
                IntStream.of(children).boxed().collect(Collectors.toSet()));
        elemSet.add(parent);
        int[] elems = elemSet.stream().mapToInt(Number::intValue).toArray();
        boolean[] mayCarryHat = new boolean[elems.length];
        for (int i = 0; i < elems.length; i++) {
            mayCarryHat[i] = elems[i] != parent ||
                    parent == config.prefixLength() - 1;
        }
        for (int i = elems.length - 1; i >= 0; i--) {
            if (mayCarryHat[i] && prefixHasTrue(mayCarryHat, i)) {
                rulehatCount++;
                if (elems[i] == config.getAbsoluteHatIndex()) {
                    rulehatThisCount++;
                    break;
                }
            }
        }
        int[] elemsLeft = elemSet.stream().mapToInt(Number::intValue)
                .filter(i -> i < parent).toArray();
        int[] elemsRight = elemSet.stream().mapToInt(Number::intValue)
                .filter(i -> i > parent).toArray();
        extractReduceLeftGaps(config, parent, elemsLeft);
        extractReduceRightGaps(config, parent, elemsRight);
        extractReduceLeftExpand(config, parent, elemsLeft);
        extractReduceRightExpand(config, parent, elemsRight);
    }

    private void extractReduceLeftGaps(final HatConfig config,
                                       int parent, int[] children) {
        int i = parent - 1;
        int j = children.length - 1;
        while (i > 0 && j >= 0) {
            leftCount++;
            if (children[j] == i) {
                leftThisCount++;
                j--;
            }
            i--;
        }
    }

    private void extractReduceRightGaps(final HatConfig config,
                                        int parent, int[] children) {
        int i = parent + 1;
        int j = 0;
        while (i < config.prefixLength() - 1 && j < children.length) {
            rightCount++;
            if (children[j] == i) {
                rightThisCount++;
                j++;
            }
            i++;
        }
    }

    private void extractReduceLeftExpand(final HatConfig config,
                                         int parent, int[] children) {
        if (parent == 0)
            return;
        String par = config.getPrefixLeft(parent)
                .getToken().upos.toString();
        for (int i = children.length - 1; i >= 0; i--) {
            String prev = i == children.length - 1 ? "" :
                    config.getPrefixLeft(children[i + 1])
                            .getToken().upos.toString();
            String cur = config.getPrefixLeft(children[i])
                    .getToken().upos.toString();
            headPrevLeftCount.incr(new StringPair(par, prev));
            headPrevNextLeftCount.incr(new StringTriple(par, prev, cur));
        }
        String prev = children.length == 0 ? "" : config.getPrefixLeft(
                        children[0])
                .getToken().upos.toString();
        headPrevLeftCount.incr(new StringPair(par, prev));
        headPrevLastLeftCount.incr(new StringPair(par, prev));
    }

    private void extractReduceRightExpand(final HatConfig config,
                                          int parent, int[] children) {
        String par = parent == 0 ? "" : config.getPrefixLeft(parent)
                .getToken().upos.toString();
        for (int i = 0; i < children.length; i++) {
            String prev = i == 0 ? "" : config.getPrefixLeft(children[i - 1])
                    .getToken().upos.toString();
            String cur = config.getPrefixLeft(children[i])
                    .getToken().upos.toString();
            headPrevRightCount.incr(new StringPair(par, prev));
            headPrevNextRightCount.incr(new StringTriple(par, prev, cur));
        }
        String prev = children.length == 0 ? "" : config.getPrefixLeft(
                        children[children.length - 1])
                .getToken().upos.toString();
        headPrevRightCount.incr(new StringPair(par, prev));
        headPrevLastRightCount.incr(new StringPair(par, prev));
    }

    public static final String stackhatWeight = WholeHatParser.stackhatWeight;
    public static final String rulehatWeight = WholeHatParser.rulehatWeight;
    public static final String gapLeftWeight = WholeHatParser.gapLeftWeight;
    public static final String gapRightWeight = WholeHatParser.gapRightWeight;
    public static final String expandLeftWeight = WholeHatParser.expandLeftWeight;
    public static final String expandRightWeight = WholeHatParser.expandRightWeight;
    public static final String normWeight = WholeHatParser.normWeight;
    public static final String[] weightedProperties = WholeHatParser.weightedProperties;

    public PropertyWeights prob(final HatConfig config, final String[] action) {
        switch (action[0]) {
            case shift:
                return probShift(config);
            case reduce: {
                int parent = Integer.parseInt(action[1]);
                int nChildren = Integer.parseInt(action[2]);
                int[] children = Arrays.stream(Arrays.copyOfRange(action, 3, 3 + nChildren))
                        .mapToInt(Integer::parseInt)
                        .toArray();
                String[] deprels = Arrays.copyOfRange(action, 3 + nChildren, 3 + 2 * nChildren);
                return probReduce(config, parent, children, deprels);
            }
            default:
                return new PropertyWeights(weightedProperties);
        }
    }

    private PropertyWeights probShift(final HatConfig config) {
        PropertyWeights weights = new PropertyWeights(weightedProperties);
        double p = 1;
        boolean[] mayCarryHat = new boolean[config.prefixLength()];
        for (int i = 0; i < config.prefixLength(); i++)
            mayCarryHat[i] = (i == config.prefixLength() - 1) ||
                    config.getLabelLeft(i).equals(complete);
        for (int i = config.prefixLength() - 1; i >= 0; i--) {
            if (mayCarryHat[i] && prefixHasTrue(mayCarryHat, i)) {
                if (i == config.getAbsoluteHatIndex()) {
                    p *= 1 - pLexhat();
                    break;
                } else {
                    p *= pLexhat();
                }
            }
        }
        weights.addNegLog(stackhatWeight, p);
        return weights;
    }

    private PropertyWeights probReduce(final HatConfig config,
                                       int parent, int[] children, String[] deprels) {
        PropertyWeights weights = new PropertyWeights(weightedProperties);
        double p = 1;
        Set<Integer> elemSet = new TreeSet<>(
                IntStream.of(children).boxed().collect(Collectors.toSet()));
        elemSet.add(parent);
        int[] elems = elemSet.stream().mapToInt(Number::intValue).toArray();
        boolean[] mayCarryHat = new boolean[elems.length];
        for (int i = 0; i < elems.length; i++) {
            mayCarryHat[i] = elems[i] != parent ||
                    parent == config.prefixLength() - 1;
        }
        for (int i = elems.length - 1; i >= 0; i--) {
            if (mayCarryHat[i] && prefixHasTrue(mayCarryHat, i)) {
                if (elems[i] == config.getAbsoluteHatIndex()) {
                    p *= 1 - pRulehat();
                    break;
                } else {
                    p *= pRulehat();
                }
            }
        }
        int[] elemsLeft = elemSet.stream().mapToInt(Number::intValue)
                .filter(i -> i < parent).toArray();
        int[] elemsRight = elemSet.stream().mapToInt(Number::intValue)
                .filter(i -> i > parent).toArray();
        double pLeft = probReduceLeftGaps(config, parent, elemsLeft);
        double pRight = probReduceRightGaps(config, parent, elemsRight);
        double pLeftExpand = probReduceLeftExpand(config, parent, elemsLeft);
        double pRightExpand = probReduceRightExpand(config, parent, elemsRight);
        double pNorm = probNormalise(config, parent, elemsRight);
        weights.addNegLog(rulehatWeight, p);
        weights.addNegLog(gapLeftWeight, pLeft);
        weights.addNegLog(gapRightWeight, pRight);
        weights.addNegLog(expandLeftWeight, pLeftExpand);
        weights.addNegLog(expandRightWeight, pRightExpand);
        weights.addNegLog(normWeight, pNorm);
        return weights;
    }

    private double probReduceLeftGaps(final HatConfig config,
                                      int parent, int[] children) {
        int i = parent - 1;
        int j = children.length - 1;
        double p = 1;
        while (i > 0 && j >= 0) {
            if (children[j] == i) {
                p *= 1 - pLeft();
                j--;
            } else {
                p *= pLeft();
            }
            i--;
        }
        return p;
    }

    private double probReduceRightGaps(final HatConfig config,
                                       int parent, int[] children) {
        int i = parent + 1;
        int j = 0;
        double p = 1;
        while (i < config.prefixLength() - 1 && j < children.length) {
            if (children[j] == i) {
                p *= 1 - pRight();
                j++;
            } else {
                p *= pRight();
            }
            i++;
        }
        return p;
    }

    private double probReduceLeftExpand(final HatConfig config,
                                        int parent, int[] children) {
        double p = 1;
        if (parent == 0)
            return p;
        String par = config.getPrefixLeft(parent)
                .getToken().upos.toString();
        for (int i = children.length - 1; i >= 0; i--) {
            String prev = i == children.length - 1 ? "" :
                    config.getPrefixLeft(children[i + 1])
                            .getToken().upos.toString();
            String cur = config.getPrefixLeft(children[i])
                    .getToken().upos.toString();
            p *= pRuleNextLeft(par, prev, cur);
        }
        String prev = children.length == 0 ? "" : config.getPrefixLeft(
                        children[0])
                .getToken().upos.toString();
        p *= pRuleLastLeft(par, prev);
        return p;
    }

    private double probReduceRightExpand(final HatConfig config,
                                         int parent, int[] children) {
        double p = 1;
        String par = parent == 0 ? "" : config.getPrefixLeft(parent)
                .getToken().upos.toString();
        for (int i = 0; i < children.length; i++) {
            String prev = i == 0 ? "" :
                    config.getPrefixLeft(children[i - 1])
                            .getToken().upos.toString();
            String cur = config.getPrefixLeft(children[i])
                    .getToken().upos.toString();
            p *= pRuleNextRight(par, prev, cur);
        }
        String prev = children.length == 0 ? "" : config.getPrefixLeft(
                        children[children.length - 1])
                .getToken().upos.toString();
        p *= pRuleLastRight(par, prev);
        return p;
    }

    private double probNormalise(final HatConfig config,
                                 int parent, int[] rightChildren) {
        if (parent + rightChildren.length < config.prefixLength() - 1) {
            String par = config.getPrefixLeft(parent)
                    .getToken().upos.toString();
            return 1.0 / (1 - pRuleLastLeft(par, "") * pRuleLastRight(par, ""));
        } else {
            return 1;
        }
    }

    private boolean prefixHasTrue(boolean[] mayCarryHat, int i) {
        for (int j = 0; j < i; j++)
            if (mayCarryHat[j])
                return true;
        return false;
    }

}
