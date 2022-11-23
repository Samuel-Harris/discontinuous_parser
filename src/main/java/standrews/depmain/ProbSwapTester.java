/*
 * Copyright (c) 2019. University of St Andrews
 */

package standrews.depmain;

import standrews.classification.FeatureSpecification;
import standrews.depbase.Token;
import standrews.depmethods.SimpleParser;
import standrews.depmethods.SwapParser;

public class ProbSwapTester extends ProbTester {

    public ProbSwapTester(final FeatureSpecification featSpec) {
        super(featSpec);
    }

    protected SimpleParser makeParser(final Token[] tokens) {
        return new SwapParser(tokens);
    }

}
