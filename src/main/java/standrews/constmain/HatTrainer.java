/*
 * Copyright (c) 2019. University of St Andrews
 */

package standrews.constmain;

import standrews.classification.FeatureVectorGenerator;
import standrews.constbase.ConstTree;
import standrews.constmethods.DeterministicParser;
import standrews.constmethods.HatParser;

public class HatTrainer extends SimpleTrainer {

    public HatTrainer(final FeatureVectorGenerator featureVectorGenerator) {
        super(featureVectorGenerator);
    }

    protected boolean allowableTree(final ConstTree tree) {
        return true;
    }

    protected DeterministicParser makeParser(final ConstTree tree) {
        final HatParser parser = new HatParser(tree);
        parser.setLeftDependentsFirst(leftDependentsFirst);
        return parser;
    }

}
