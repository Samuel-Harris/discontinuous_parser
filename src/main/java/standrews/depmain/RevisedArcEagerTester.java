/*
 * Copyright (c) 2018. University of St Andrews
 */

package standrews.depmain;

import standrews.classification.FeatureSpecification;
import standrews.depbase.Token;
import standrews.depmethods.DeterministicParser;
import standrews.depmethods.RevisedArcEagerParser;

public class RevisedArcEagerTester extends SimpleTester {

	private boolean earlyReduce;

	private boolean strict;

	public RevisedArcEagerTester(final FeatureSpecification featSpec,
								 final boolean early, final boolean strict) {
		super(featSpec);
		setEarlyReduce(early);
		setStrict(strict);
	}

	public void setEarlyReduce(final boolean b) {
		earlyReduce = b;
	}

	public void setStrict(final boolean b) {
		strict = b;
	}

	protected DeterministicParser makeParser(final Token[] tokens) {
		return new RevisedArcEagerParser(tokens, earlyReduce, strict);
	}
}
