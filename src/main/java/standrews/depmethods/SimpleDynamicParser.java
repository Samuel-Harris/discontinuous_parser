/*
 * Copyright (c) 2019. University of St Andrews
 */

package standrews.depmethods;

import standrews.depautomata.DependencyVertex;
import standrews.depautomata.SimpleConfig;
import standrews.depextract.SimpleExtractor;
import standrews.depbase.Token;

import java.util.*;

public abstract class SimpleDynamicParser extends SimpleParser {

    /**
     * The order of attachment of children is to be enforced strictly.
     */
    final protected boolean strict;

    /**
     * Extractor obtained by static oracle.
     */
    final protected SimpleExtractor staticExtractor;

    protected DynamicChooseMode chooseMode = DynamicChooseMode.RANDOM;

    /**
     * What is probability that not optimal action is taken.
     */
    protected double strayProb = 0.0;

    final protected Random random = new Random();

    public SimpleDynamicParser(final Token[] tokens,
                               final boolean leftDependentsFirst,
                               final boolean strict,
                               final SimpleExtractor preliminaryExtractor) {
        super(tokens);
        setLeftDependentsFirst(leftDependentsFirst);
        this.strict = strict;
        this.staticExtractor = preliminaryExtractor;
    }

    public void setChooseMode(final DynamicChooseMode chooseMode) {
        this.chooseMode = chooseMode;
    }

    public void setStrayProb(final double strayProb) {
        this.strayProb = strayProb;
    }

    /**
     * Get best action as in static oracle.
     *
     * @param config Current configuration.
     * @return
     */
    protected String[] getAction(final SimpleConfig config) {
        if (config.prefixLength() >= 2) {
            final TreeMap<String, Integer> scores = scores(config);
            final Vector<String> actions = getBestActions(scores);
            String[] otherAction = getOtherActions(config, actions);
            if (otherAction != null)
                return otherAction;
        }
        if (config.suffixLength() > 0) {
            return new String[]{"shift"};
        }
        return new String[]{};
    }

    /**
     * Get action that may stray from correct one.
     *
     * @param config
     * @return
     */
    public String[] getStrayAction(final SimpleConfig config) {
        if (config.prefixLength() >= 2) {
            if (chooseMode == DynamicChooseMode.PRELIM || chooseMode == DynamicChooseMode.PRELIM_PRELIM) {
                final Iterator<String[]> actions = staticExtractor.predict(config);
                while (actions.hasNext()) {
                    final String[] ac = actions.next();
                    if (applicable(config, ac)) {
                        return ac;
                    }
                }
            }
            final TreeMap<String, Integer> scores = scores(config);
            final Vector<String> actions = getPossibleActions(scores);
            String[] otherAction = getOtherActions(config, actions);
            if (otherAction != null)
                return otherAction;
        }
        if (config.suffixLength() > 0) {
            return shift();
        }
        return none();
    }

    private String[] getOtherActions(final SimpleConfig config,
                                     final Vector<String> actions) {
        String action = "null";
        if (actions.size() == 0) {
            /* skip */
        } else if (actions.size() == 1 || chooseMode == DynamicChooseMode.FIRST) {
            action = actions.get(0);
        } else if (chooseMode == DynamicChooseMode.LAST) {
            action = actions.get(actions.size() - 1);
        } else {
            if (chooseMode == DynamicChooseMode.PRELIM_PRELIM) {
                Iterator<String[]> predActions = staticExtractor.predict(config);
                while (predActions.hasNext()) {
                    final String[] ac = predActions.next();
                    if (actions.contains(ac[0])) {
                        return ac;
                    }
                }
            }
            final int i = random.nextInt(actions.size());
            action = actions.get(i);
        }
        if (action.equals("reduceLeft")) {
            final DependencyVertex v0 = config.getPrefixRight(0);
            final Token t0 = v0.getToken();
            final String deprel = deprel(t0);
            return reduceLeft(deprel);
        } else if (action.equals("reduceRight")) {
            final DependencyVertex v1 = config.getPrefixRight(1);
            final Token t1 = v1.getToken();
            final String deprel = deprel(t1);
            return reduceRight(deprel);
        } else if (action.equals("shift")) {
            return shift();
        }
        return null;
    }

    /**
     * Get action that is to be done for dynamic oracle.
     *
     * @param config
     * @return
     */
    protected String[] getStepAction(final SimpleConfig config) {
        if (random.nextDouble() <= strayProb) {
            return getStrayAction(config);
        } else {
            return getAction(config);
        }
    }

    protected abstract TreeMap<String, Integer> scores(final SimpleConfig config);
}
