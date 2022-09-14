/*
 * Copyright (c) 2018. University of St Andrews
 */

package standrews.depmain;

import standrews.classification.FeatureSpecification;
import standrews.depbase.Token;
import standrews.depmethods.DeterministicParser;
import standrews.depmethods.NormalArcEagerParser;
import standrews.tabular.OptimalProjectivizer;

public class NormalArcEagerTrainer extends SimpleTrainer {

	public NormalArcEagerTrainer(final FeatureSpecification featSpec) {
		super(featSpec);
	}

	protected DeterministicParser makeParser(Token[] tokens) {
		if (nonprojectiveAllowed)
			tokens = OptimalProjectivizer.projectivize(tokens);
		final NormalArcEagerParser parser = new NormalArcEagerParser(tokens);
		return parser;
	}
}
