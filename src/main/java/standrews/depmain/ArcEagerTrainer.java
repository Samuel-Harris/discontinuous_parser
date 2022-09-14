/*
 * Copyright (c) 2018. University of St Andrews
 */

package standrews.depmain;

import standrews.classification.FeatureSpecification;
import standrews.depbase.Token;
import standrews.depmethods.ArcEagerParser;
import standrews.depmethods.DeterministicParser;
import standrews.tabular.OptimalProjectivizer;

public class ArcEagerTrainer extends SimpleTrainer {

	protected boolean earlyReduce;

	protected boolean strict;

	/**
	 * Is projectivization to be used in dynamic phase.
	 */
	protected boolean dynProjective = false;

	public ArcEagerTrainer(final FeatureSpecification featSpec,
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

	protected DeterministicParser makeParser(Token[] tokens) {
		if (nonprojectiveAllowed)
			tokens = OptimalProjectivizer.projectivize(tokens);
		final ArcEagerParser parser = new ArcEagerParser(tokens, earlyReduce, strict);
		return parser;
	}
}
