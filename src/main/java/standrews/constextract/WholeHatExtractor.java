/*
 * Copyright (c) 2019. University of St Andrews
 */

package standrews.constextract;

import standrews.aux_.Counter;
import standrews.aux_.PropertyWeights;
import standrews.aux_.StringPair;
import standrews.aux_.StringTriple;
import standrews.constautomata.HatConfig;
import standrews.constmethods.WholeHatParser;

import java.util.Arrays;

public class WholeHatExtractor {
    public static final String shift = WholeHatParser.shift;
    public static final String reduce = WholeHatParser.reduce;
    public static final String reduceRoots = WholeHatParser.reduceRoots;

    /**
     * How often is parent cat seen.
     */
    private Counter<String> catCount = new Counter<>();

    /**
     * Given parent cat, what is count of head child cat.
     */
    private Counter<StringPair> parentHeadCount = new Counter<>();

    /**
     * Given parent, how often is child cat seen if moving to right.
     */
    private Counter<StringPair> parentRightCount = new Counter<>();

    /**
     * Given parent, how often is child cat seen if moving to left.
     */
    private Counter<StringPair> parentLeftCount = new Counter<>();

    /**
     * Given parent cat and previous child cat, what is count of next child cat
     * to the right.
     */
    private Counter<StringTriple> parentRightNextCount = new Counter<>();

    /**
     * Given parent cat and previous child cat, what is count of next child cat
     * to the right.
     */
    private Counter<StringTriple> parentLeftNextCount = new Counter<>();

    /**
     * Given parent cat and previous child cat, what is count of not taking next child
     * to the right.
     */
    private Counter<StringPair> parentRightStopCount = new Counter<>();

    /**
     * Given parent cat and previous child cat, what is count of not taking next child
     * to the left.
     */
    private Counter<StringPair> parentLeftStopCount = new Counter<>();

    /**
     * How often is there a first root.
     */
    private int rootFirstCount = 0;

    /**
     * How often is there an additional root.
     */
    private int rootNextCount = 0;

    /**
     * How often does root have cat.
     */
    private Counter<String> rootCatCount = new Counter<>();

    public WholeHatExtractor() {
    }

    public void extract(final HatConfig config, final String[] action) {
        switch (action[0]) {
            case shift:
                extractShift(config);
                break;
            case reduce: {
                String cat = action[1];
                int hdIndex = Integer.parseInt(action[2]);
                int[] children = Arrays.stream(Arrays.copyOfRange(action, 3, action.length))
                        .mapToInt(Integer::parseInt)
                        .toArray();
                extractReduce(config, cat, hdIndex, children);
                break;
            }
            case reduceRoots:
                int[] children = Arrays.stream(Arrays.copyOfRange(action, 1, action.length))
                        .mapToInt(Integer::parseInt)
                        .toArray();
                extractReduceRoots(config, children);
                break;
        }
    }

    private void extractShift(final HatConfig config) {

    }

    private void extractReduce(final HatConfig config,
                               String cat, int hdIndex, int[] children) {
        catCount.incr(cat);
        int hdStackIndex = children[hdIndex];
        String headCat = config.getStackLeft(hdStackIndex).getCat();
        parentHeadCount.incr(new StringPair(cat, headCat));
        for (int i = hdIndex; i < children.length; i++) {
            int prevStackIndex = children[i];
            String prevCat = config.getStackLeft(prevStackIndex).getCat();
            parentRightCount.incr(new StringPair(cat, prevCat));
            if (i < children.length - 1) {
                int nextStackIndex = children[i + 1];
                String nextCat = config.getStackLeft(nextStackIndex).getCat();
                parentRightNextCount.incr(new StringTriple(cat, prevCat, nextCat));
            } else {
                parentRightStopCount.incr(new StringPair(cat, prevCat));
            }
        }
        for (int i = hdIndex; i >= 0; i--) {
            int prevStackIndex = children[i];
            String prevCat = config.getStackLeft(prevStackIndex).getCat();
            parentLeftCount.incr(new StringPair(cat, prevCat));
            if (i > 0) {
                int nextStackIndex = children[i - 1];
                String nextCat = config.getStackLeft(nextStackIndex).getCat();
                parentLeftNextCount.incr(new StringTriple(cat, prevCat, nextCat));
            } else {
                parentLeftStopCount.incr(new StringPair(cat, prevCat));
            }
        }
    }

    private void extractReduceRoots(final HatConfig config,
                                    int[] children) {
        if (children.length == 0) {
            System.err.println("zero children of root in WholeHatExtractor");
            System.exit(-1);
        }
        rootFirstCount++;
        rootNextCount += children.length - 1;
        for (int i = 0; i < children.length; i++) {
            int stackIndex = children[i];
            String cat = config.getStackLeft(stackIndex).getCat();
            rootCatCount.incr(cat);
        }
    }

    public static final String rootWeight = WholeHatParser.rootWeight;
    public static final String rootCatWeight = WholeHatParser.rootCatWeight;
    public static final String catWeight = WholeHatParser.catWeight;
    public static final String branchWeight = WholeHatParser.branchWeight;
    public static final String[] weightedProperties = WholeHatParser.weightedProperties;

    public PropertyWeights prob(final HatConfig config, final String[] action) {
        switch (action[0]) {
            case shift:
                return probShift(config);
            case reduce: {
                String cat = action[1];
                int hdIndex = Integer.parseInt(action[2]);
                int[] children = Arrays.stream(Arrays.copyOfRange(action, 3, action.length))
                        .mapToInt(Integer::parseInt)
                        .toArray();
                return probReduce(config, cat, hdIndex, children);
            }
            case reduceRoots:
                int[] children = Arrays.stream(Arrays.copyOfRange(action, 1, action.length))
                        .mapToInt(Integer::parseInt)
                        .toArray();
                return probReduceRoots(config, children);
            default:
                return new PropertyWeights(weightedProperties);
        }
    }

    private PropertyWeights probShift(final HatConfig config) {
        return new PropertyWeights(weightedProperties);
    }

    private PropertyWeights probReduce(final HatConfig config,
                                       String cat, int hdIndex, int[] children) {
        return new PropertyWeights(weightedProperties);
    }

    private PropertyWeights probReduceRoots(final HatConfig config,
                                            int[] children) {
        PropertyWeights weights = new PropertyWeights(weightedProperties);
        int nRoots = rootFirstCount + rootNextCount;
        double nRootsProb = 1.0 * rootFirstCount / nRoots;
        for (int i = 1; i < children.length; i++)
            nRootsProb *= 1.0 * rootNextCount / nRoots;
        double rootCatProb = 1;
        for (int i = 0; i < children.length; i++) {
            int stackIndex = children[i];
            String cat = config.getStackLeft(stackIndex).getCat();
            rootCatProb *= 1.0 * rootCatCount.get(cat) / nRoots;
        }
        weights.addNegLog(rootWeight, nRootsProb);
        weights.addNegLog(rootCatWeight, rootCatProb);
        return weights;
    }

}
