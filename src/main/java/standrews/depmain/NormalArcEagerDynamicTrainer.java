/*
 * Copyright (c) 2018. University of St Andrews
 */

package standrews.depmain;

import standrews.classification.FeatureSpecification;
import standrews.depmethods.*;
import standrews.depextract.NormalArcEagerExtractor;
import standrews.depextract.SimpleExtractor;
import standrews.depbase.Token;
import standrews.tabular.OptimalProjectivizer;

public class NormalArcEagerDynamicTrainer extends SimpleDynamicTrainer {

	public NormalArcEagerDynamicTrainer(final FeatureSpecification featSpec,
										final double strayProb, final int nIterations,
										final boolean dynProjective) {
		super(featSpec, strayProb, nIterations, dynProjective);
	}

	protected SimpleExtractor freshExtractor(final SimpleExtractor oldSimple) {
		NormalArcEagerExtractor old = (NormalArcEagerExtractor) oldSimple;
		SimpleExtractor fresh =
				new NormalArcEagerExtractor(old.featSpec,
						old.factory, null, null, null, null
				);
		return fresh;
	}

	protected DeterministicParser makeParser(Token[] tokens) {
		if (nonprojectiveAllowed)
			tokens = OptimalProjectivizer.projectivize(tokens);
		final NormalArcEagerParser parser = new NormalArcEagerParser(tokens);
		return parser;
	}

	protected DeterministicParser makeStrayingParser(Token[] tokens,
											  final SimpleExtractor simpleStaticExtractor) {
		final NormalArcEagerExtractor staticExtractor =
				(NormalArcEagerExtractor) simpleStaticExtractor;
		if (nonprojectiveAllowed && dynProjective)
			tokens = OptimalProjectivizer.projectivize(tokens);
		final NormalArcEagerDynamicParser parser =
				new NormalArcEagerDynamicParserCubic(tokens, staticExtractor);
		parser.setStrayProb(strayProb);
		parser.setChooseMode(DynamicChooseMode.PRELIM);
		return parser;
	}
}
