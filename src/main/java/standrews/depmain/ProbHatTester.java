/*
 * Copyright (c) 2019. University of St Andrews
 */

package standrews.depmain;

import standrews.classification.FeatureSpecification;
import standrews.depbase.Token;
import standrews.depmethods.HatParser;
import standrews.depmethods.SimpleParser;

public class ProbHatTester extends ProbTester {

    public ProbHatTester(final FeatureSpecification featSpec) {
        super(featSpec);
    }

    protected SimpleParser makeParser(final Token[] tokens) {
        final int viewMin = featSpec.getIntFeature("viewMin", 0);
        final int viewMax = featSpec.getIntFeature("viewMax", 0);
        return new HatParser(tokens, viewMin, viewMax);
    }
}
