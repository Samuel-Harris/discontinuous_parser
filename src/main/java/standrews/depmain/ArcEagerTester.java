/*
 * Copyright (c) 2018. University of St Andrews
 */

package standrews.depmain;

import standrews.classification.FeatureSpecification;
import standrews.depbase.Token;
import standrews.depmethods.ArcEagerParser;
import standrews.depmethods.DeterministicParser;

public class ArcEagerTester extends SimpleTester {

	private boolean earlyReduce;

	private boolean strict;

	public ArcEagerTester(final FeatureSpecification featSpec,
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
		return new ArcEagerParser(tokens, earlyReduce, strict);
	}
}
