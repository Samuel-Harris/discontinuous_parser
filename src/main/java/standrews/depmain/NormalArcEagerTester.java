/*
 * Copyright (c) 2018. University of St Andrews
 */

package standrews.depmain;

import standrews.classification.FeatureSpecification;
import standrews.depbase.Token;
import standrews.depmethods.DeterministicParser;
import standrews.depmethods.NormalArcEagerParser;

public class NormalArcEagerTester extends SimpleTester {

	public NormalArcEagerTester(final FeatureSpecification featSpec) {
		super(featSpec);
	}

	protected DeterministicParser makeParser(final Token[] tokens) {
		return new NormalArcEagerParser(tokens);
	}
}
