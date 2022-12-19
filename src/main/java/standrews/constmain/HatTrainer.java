/*
 * Copyright (c) 2019. University of St Andrews
 */

package standrews.constmain;

import standrews.classification.FeatureVectorGenerator;
import standrews.constbase.ConstTree;
import standrews.constmethods.DeterministicParser;
import standrews.constmethods.HatParser;

public class HatTrainer extends SimpleTrainer {

    public HatTrainer(final FeatureVectorGenerator featureVectorGenerator, int maxEpochs) {
        super(featureVectorGenerator, maxEpochs);
    }

    protected boolean allowableTree(final ConstTree tree) {
        return true;
    }

}
