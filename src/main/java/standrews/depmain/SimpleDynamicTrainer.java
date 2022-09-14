/*
 * Copyright (c) 2018. University of St Andrews
 */

package standrews.depmain;

import standrews.classification.FeatureSpecification;
import standrews.depmethods.*;
import standrews.depextract.SimpleExtractor;
import standrews.depbase.DependencyStructure;
import standrews.depbase.Token;
import standrews.depbase.Treebank;
import standrews.tabular.OptimalProjectivizer;

public class SimpleDynamicTrainer extends SimpleTrainer {

	/**
	 * What is probability that not optimal action is taken.
	 */
	protected final double strayProb;

	/**
	 * Is projectivization to be used in dynamic phase.
	 */
	protected boolean dynProjective;

	/**
	 * How many iterations. Should be positive number.
	 */
	protected final int nIterations;

	public SimpleDynamicTrainer(final FeatureSpecification featSpec,
								final double strayProb, final int nIterations,
								final boolean dynProjective) {
		super(featSpec);
		this.strayProb = strayProb;
		this.nIterations = nIterations;
		this.dynProjective = dynProjective;
	}

	public int train(final String corpus,
					 final String corpusCopy,
					 final int n, final SimpleExtractor extractor) {
		SimpleExtractor prevExtractor = freshExtractor(extractor);
		super.train(corpus, corpusCopy, n, prevExtractor);
		prevExtractor.train();
		final Treebank treebank = makeTreebank(corpus, n);
		final int nIter = 1;
		int i = 0;
		for (int iter = 0; iter < nIter; iter++) {
			SimpleExtractor nextExtractor = iter < nIter - 1 ?
					freshExtractor(extractor) :	extractor;
			for (int epo = 0; epo == 0 || extractor.getContinuousTraining() && epo < extractor.getNEpochs(); epo++) {
				if (extractor.getContinuousTraining())
					reportFine("Epoch " + epo);
				i = 0;
				for (DependencyStructure struct : treebank.depStructs) {
					final Token[] tokens = retaggedTokens(struct);
					if (allowableTree(tokens, i, n)) {
						DeterministicParser parser = makeStrayingParser(tokens, prevExtractor);
						parser.observe(nextExtractor);
						i++;
					}
				}
				nextExtractor.train();
			}
			prevExtractor = nextExtractor;
		}
		return i;
	}

	protected SimpleExtractor freshExtractor(final SimpleExtractor old) {
		SimpleExtractor fresh =
				new SimpleExtractor(old.featSpec,
						old.factory, null, null, null
				);
		return fresh;
	}

	protected DeterministicParser makeParser(Token[] tokens) {
		if (nonprojectiveAllowed)
			tokens = OptimalProjectivizer.projectivize(tokens);
		final SimpleParser parser = new SimpleParser(tokens);
		parser.setLeftDependentsFirst(leftDependentsFirst);
		// new SimpleDynamicParserLinear(tokens, leftDependentsFirst, strict, null);
		// parser.setStrayProb(0);
		return parser;
	}

	protected DeterministicParser makeStrayingParser(Token[] tokens,
											  final SimpleExtractor staticExtractor) {
		if (dynProjective && nonprojectiveAllowed)
			tokens = OptimalProjectivizer.projectivize(tokens);
		final SimpleDynamicParser parser =
				dynProjective ?
				new SimpleDynamicParserLinear(tokens, leftDependentsFirst, strict,
						staticExtractor) :
				new SimpleDynamicParserCubicSimple(tokens, leftDependentsFirst, strict,
						staticExtractor);
		parser.setStrayProb(strayProb);
		parser.setProjective(dynProjective);
		// parser.setChooseMode(SimpleDynamicParser.ChooseMode.PRELIM_PRELIM);
		parser.setChooseMode(DynamicChooseMode.PRELIM);
		return parser;
	}
}
