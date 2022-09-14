/*
 * Copyright (c) 2018. University of St Andrews
 */

package standrews.depmain;

import standrews.classification.FeatureSpecification;
import standrews.depbase.Token;
import standrews.depmethods.DeterministicParser;
import standrews.depmethods.RevisedArcEagerParser;
import standrews.tabular.OptimalProjectivizer;


public class RevisedArcEagerTrainer extends ArcEagerTrainer {

	public RevisedArcEagerTrainer(final FeatureSpecification featSpec,
								  final boolean early, final boolean strict) {
		super(featSpec, early, strict);
	}

	protected DeterministicParser makeParser(Token[] tokens) {
		if (nonprojectiveAllowed)
			tokens = OptimalProjectivizer.projectivize(tokens);
		final RevisedArcEagerParser parser = new RevisedArcEagerParser(tokens, earlyReduce, strict);
		return parser;
	}
}
