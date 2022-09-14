/*
 * Copyright (c) 2018. University of St Andrews
 */

package standrews.depmain;

import standrews.classification.FeatureSpecification;
import standrews.depmethods.*;
import standrews.depextract.RevisedArcEagerExtractor;
import standrews.depextract.SimpleExtractor;
import standrews.depbase.Token;
import standrews.tabular.OptimalProjectivizer;

public class RevisedArcEagerDynamicTrainer extends SimpleDynamicTrainer {

	protected boolean earlyReduce;

	protected boolean strict;

	public RevisedArcEagerDynamicTrainer(final FeatureSpecification featSpec,
			final double strayProb,
										 final int nIterations,
										 final boolean early,
										 final boolean strict,
										 final boolean dynProjective) {
		super(featSpec, strayProb, nIterations, dynProjective);
		setEarlyReduce(early);
		setStrict(strict);
	}

	protected SimpleExtractor freshExtractor(final SimpleExtractor oldSimple) {
		RevisedArcEagerExtractor old = (RevisedArcEagerExtractor) oldSimple;
		SimpleExtractor fresh =
				new RevisedArcEagerExtractor(old.featSpec,
						old.factory, null, null, null
				);
		return fresh;
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
		final RevisedArcEagerParser parser = new RevisedArcEagerParser(tokens, earlyReduce, strict);
		return parser;
	}

	protected DeterministicParser makeStrayingParser(Token[] tokens,
											  final SimpleExtractor simpleStaticExtractor) {
		final RevisedArcEagerExtractor staticExtractor =
				(RevisedArcEagerExtractor) simpleStaticExtractor;
		if (nonprojectiveAllowed && dynProjective)
			tokens = OptimalProjectivizer.projectivize(tokens);
		final RevisedArcEagerDynamicParser parser =
				new RevisedArcEagerDynamicParserCubic(tokens, earlyReduce, strict, staticExtractor);
		parser.setStrayProb(strayProb);
		parser.setChooseMode(DynamicChooseMode.PRELIM);
		return parser;
	}
}
