/*
 * Copyright (c) 2018. University of St Andrews
 */

package standrews.depextract;

import standrews.depautomata.SimpleConfig;
import standrews.classification.*;
import standrews.depmethods.SwapParser;

import java.util.*;

public class SwapExtractor extends SimpleExtractor {

    public static final String shift = SwapParser.shift;
    public static final String swap = SwapParser.swap;
    public static final String reduceLeft = SwapParser.reduceLeft;
    public static final String reduceRight = SwapParser.reduceRight;

    public SwapExtractor(final FeatureSpecification featSpec,
                         final ClassifierFactory factory, final String actionFile,
                         final String deprelLeftFile, final String deprelRightFile) {
        super(featSpec, factory, actionFile, deprelLeftFile, deprelRightFile);
    }

    public Iterator<String[]> predict(final SimpleConfig config) {
        final Features actionFeats = extract(config);
        final String[] acs = actionClassifier.predictAll(actionFeats);
        return new ActionIterator(config, acs);
    }

    private class ActionIterator implements Iterator<String[]> {
        private final SimpleConfig config;
        private final LinkedList<String> acs;

        public ActionIterator(final SimpleConfig config, String[] acs) {
            this.config = config;
            this.acs = new LinkedList(Arrays.asList(acs));
        }

        @Override
        public boolean hasNext() {
            return !acs.isEmpty();
        }

        @Override
        public String[] next() {
            if (acs.isEmpty())
                return null;
            final String ac = acs.removeFirst();
            if (ac.equals(shift) || ac.equals(swap)) {
                return new String[]{ac};
            } else {
                Features deprelFeats = extract(config);
                deprelFeats.putString("action", ac);
                final Classifier deprelClassifier =
                        ac.equals(reduceLeft) ? deprelLeftClassifier : deprelRightClassifier;
                final String deprel = deprelClassifier.predict(deprelFeats);
                return new String[]{ac, deprel};
            }
        }
    }

    protected void makeAllActionResponses(final Classifier classifier) {
        for (String actionName : SwapParser.actionNames)
            classifier.addResponseValue(actionName);
    }

    protected void makeAllAction(final Classifier classifier) {
        for (String actionName : SwapParser.actionNames)
            classifier.addStringValue("action", actionName);
    }
}
