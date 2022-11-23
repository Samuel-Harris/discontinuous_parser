/*
 * Copyright (c) 2019. University of St Andrews
 */

package standrews.depmethods;

import standrews.aux.PropertyWeights;
import standrews.depautomata.DependencyVertex;
import standrews.depautomata.SimpleConfig;
import standrews.depautomata.HatConfig;
import standrews.depbase.Token;
import standrews.depextract.WholeHatExtractor;

import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class WholeHatParser extends DeterministicParser {
    /**
     * The first parts of actions.
     */
    public static final String shift = "shift";
    public static final String reduce = "reduce";
    public static final String[] actionNames = {shift, reduce};

    public String[] actionNames() {
        return actionNames;
    }

    public String[] none() {
        return new String[]{};
    }

    public String[] shift() {
        return new String[]{shift};
    }

    public String[] reduce(final int parent,
                           final int[] children,
                           final String[] deprels) {
        return Stream.concat(Stream.of(reduce),
                        Stream.concat(
                                Stream.concat(
                                        IntStream.of(parent, children.length)
                                                .mapToObj(Integer::toString),
                                        Arrays.stream(children)
                                                .mapToObj(Integer::toString)),
                                Arrays.stream(deprels)))
                .toArray(String[]::new);
    }

    /**
     * Labels attached to prefix elements.
     */
    public static final String incomplete = "incomplete";
    public static final String complete = "complete";
    public static final String[] prefixLabels = {incomplete, complete};

    public WholeHatParser(final Token[] tokens) {
        super(tokens);
    }

    @Override
    protected HatConfig makeInitialConfig(final Token[] tokens) {
        return new HatConfig(tokens, incomplete);
    }

    protected String[] getAction(final SimpleConfig simpleConfig) {
        final HatConfig config = (HatConfig) simpleConfig;
        final int i0 = config.getAbsoluteHatIndex();
        final DependencyVertex v0 = config.getPrefixLeft(i0);
        final Token t0 = v0.getToken();
        if (config.getLabelLeft(i0).equals(complete)) {
            for (int i1 = config.prefixLength() - 1; i1 >= 0; i1--) {
                final DependencyVertex v1 = config.getPrefixLeft(i1);
                final Token t1 = v1.getToken();
                if (graph.isDependentOf(t0.id, t1.id)) {
                    final int[] children = withParentCompleted(t1, config);
                    final int nChildren = goldVertex(v1).getChildren().length;
                    if (children.length == nChildren)
                        return reduce(i1, children, deprelsOf(children, config));
                }
            }
        } else if (i0 > 0) {
            final int[] children = withParentCompleted(t0, config);
            final DependencyVertex[] goldChildren = goldVertex(v0).getChildren();
            final int nChildren = goldChildren == null ? 0 : goldChildren.length;
            if (children.length == nChildren)
                return reduce(i0, children, deprelsOf(children, config));
        }
        if (config.suffixLength() > 0)
            return shift();
        return none();
    }

    protected int[] withParentCompleted(Token parent, HatConfig config) {
        List<Integer> children = new ArrayList<>();
        for (int i = 0; i < config.prefixLength(); i++) {
            final DependencyVertex v = config.getPrefixLeft(i);
            final Token t = v.getToken();
            if (config.getLabelLeft(i).equals(complete) &&
                    graph.isDependentOf(t.id, parent.id))
                children.add(i);
        }
        return children.stream()
                .mapToInt(Integer::intValue)
                .toArray();
    }

    protected String[] deprelsOf(int[] children, HatConfig config) {
        String[] deprels = new String[children.length];
        for (int i = 0; i < children.length; i++) {
            final DependencyVertex v = config.getPrefixLeft(children[i]);
            deprels[i] = goldVertex(v).getToken().deprel.toString();
        }
        return deprels;
    }

    protected void apply(final SimpleConfig simpleConfig, final String[] action) {
        final HatConfig config = (HatConfig) simpleConfig;
        switch (action[0]) {
            case shift:
                shift(config);
                break;
            case reduce: {
                int parent = Integer.parseInt(action[1]);
                int nChildren = Integer.parseInt(action[2]);
                int[] children = Arrays.stream(Arrays.copyOfRange(action, 3, 3 + nChildren))
                        .mapToInt(Integer::parseInt)
                        .toArray();
                String[] deprels = Arrays.copyOfRange(action, 3 + nChildren, 3 + 2 * nChildren);
                reduce(config, parent, children, deprels);
                break;
            }
            default:
                fail("apply", config);
                break;
        }
    }

    protected boolean applicable(final SimpleConfig simpleConfig, final String[] action) {
        final HatConfig config = (HatConfig) simpleConfig;
        switch (action[0]) {
            case shift: {
                return config.suffixLength() > 0;
            }
            case reduce: {
                int parent = Integer.parseInt(action[1]);
                int nChildren = Integer.parseInt(action[2]);
                int[] children = Arrays.stream(Arrays.copyOfRange(action, 3, 3 + nChildren))
                        .mapToInt(Integer::parseInt)
                        .toArray();
                if (parent < 0 || config.prefixLength() <= parent ||
                        config.getLabelLeft(parent).equals(complete))
                    return false;
                for (int i = 0; i < nChildren; i++) {
                    if (children[i] < 0 || config.prefixLength() <= children[i] ||
                            config.getLabelLeft(children[i]).equals(incomplete))
                        return false;
                }
                if (parent == 0 &&
                        nChildren + 1 == config.prefixLength() &&
                        config.suffixLength() == 0)
                    return false;
                return true;
            }
            default:
                return false;
        }
    }

    protected void shift(final HatConfig config) {
        final DependencyVertex shifted = config.removeSuffixLeft();
        config.addPrefixRight(shifted, incomplete);
        config.setAbsoluteHatIndex(config.prefixLength() - 1);
    }

    protected void reduce(final HatConfig config,
                          int parent,
                          final int[] children,
                          final String[] deprels) {
        final DependencyVertex head = config.getPrefixLeft(parent);
        for (int i = children.length - 1; i >= 0; i--) {
            int j = children[i];
            final DependencyVertex dep = config.removePrefixLeft(j);
            head.addChild(dep);
            if (j < parent)
                parent--;
        }
        config.setLabelLeft(parent, complete);
        config.setAbsoluteHatIndex(parent);
    }

    public void observe(final WholeHatExtractor extractor) {
        prepareTraining();
        final HatConfig config = makeInitialConfig(tokens);
        while (!config.isFinal()) {
            final String[] action = getAction(config);
            if (action.length == 0) {
                fail("training", config);
                break;
            }
            extractor.extract(config, action);
            apply(config, action);
        }
    }

    public static final String stackhatWeight = "stackhatWeight";
    public static final String rulehatWeight = "rulehatWeight";
    public static final String gapLeftWeight = "gapLeftWeight";
    public static final String gapRightWeight = "gapRightWeight";
    public static final String expandLeftWeight = "expandLeftWeight";
    public static final String expandRightWeight = "expandRightWeight";
    public static final String normWeight = "normWeight";
    public static final String[] weightedProperties =
            {stackhatWeight, rulehatWeight,
                    gapLeftWeight, gapRightWeight,
                    expandLeftWeight, expandRightWeight,
                    normWeight
            };

    public PropertyWeights prob(final WholeHatExtractor extractor) {
        prepareTraining();
        final HatConfig config = makeInitialConfig(tokens);
        PropertyWeights weights = new PropertyWeights(weightedProperties);
        while (!config.isFinal()) {
            final String[] action = getAction(config);
            if (action.length == 0) {
                fail("training", config);
                break;
            }
            PropertyWeights stepWeights = extractor.prob(config, action);
            weights.add(stepWeights);
            apply(config, action);
        }
        return weights;
    }

}
