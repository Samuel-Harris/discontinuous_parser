/*
 * Copyright (c) 2018. University of St Andrews
 */

package standrews.depmain;

import standrews.depmethods.*;
import standrews.depextract.ArcEagerExtractor;
import standrews.classification.FeatureSpecification;
import standrews.depextract.SimpleExtractor;
import standrews.depbase.Token;
import standrews.tabular.OptimalProjectivizer;

public class ArcEagerDynamicTrainer extends SimpleDynamicTrainer {

	protected boolean earlyReduce;
	protected boolean strict;

	public ArcEagerDynamicTrainer(final FeatureSpecification featSpec,
								  final double strayProb,
								  final int nIterations,
								  final boolean early,
								  final boolean strict,
								  final boolean dynProjective) {
		super(featSpec, strayProb, nIterations, dynProjective);
		setEarlyReduce(early);
		setStrict(strict);
	}

	public void setEarlyReduce(final boolean b) {
		earlyReduce = b;
	}

	public void setStrict(final boolean b) {
		strict = b;
	}

	protected SimpleExtractor freshExtractor(final SimpleExtractor oldSimple) {
		ArcEagerExtractor old = (ArcEagerExtractor) oldSimple;
		SimpleExtractor fresh =
				new ArcEagerExtractor(old.featSpec,
						old.factory, null, null, null
				);
		return fresh;
	}

	protected DeterministicParser makeParser(Token[] tokens) {
		if (nonprojectiveAllowed)
			tokens = OptimalProjectivizer.projectivize(tokens);
		final ArcEagerParser parser = new ArcEagerParser(tokens, earlyReduce, strict);
		return parser;
	}

	protected DeterministicParser makeStrayingParser(Token[] tokens,
											  final SimpleExtractor simpleStaticExtractor) {
		final ArcEagerExtractor staticExtractor =
				(ArcEagerExtractor) simpleStaticExtractor;
		if (nonprojectiveAllowed && dynProjective)
			tokens = OptimalProjectivizer.projectivize(tokens);
		final ArcEagerDynamicParser parser =
				new ArcEagerDynamicParserCubic(tokens, earlyReduce, strict, staticExtractor);
		parser.setStrayProb(strayProb);
		parser.setChooseMode(DynamicChooseMode.PRELIM);
		return parser;
	}
}
