/*
 * Copyright (c) 2018. University of St Andrews
 */

package standrews.depmain;

import standrews.depautomata.DependencyGraph;
import standrews.aux.DataCollectionSum;
import standrews.classification.FeatureSpecification;
import standrews.depextract.SimpleExtractor;
import standrews.depbase.Token;
import standrews.depmethods.DynamicChooseMode;
import standrews.depmethods.SimpleDynamicParser;
import standrews.depmethods.SimpleDynamicParserTester;
import standrews.depmethods.SimpleParser;
import standrews.tabular.OptimalProjectivizer;

public class SimpleDynamicTrainerSpeed extends SimpleDynamicTrainer {

	private DataCollectionSum timings;

	private int counter = 0;

	private int fromSentence;
	private int toSentence;

	public void setFrom(int f) {
		fromSentence = f;
	}
	public void setTo(int t) {
		toSentence = t;
	}

	/**
	 * Are in the phase of measuring speed of dynamic oracle?
	 */
	private boolean speedMeasuringPhase = false;

	public SimpleDynamicTrainerSpeed(final FeatureSpecification featSpec,
									 final double strayProb, final int nIterations,
									 final boolean dynProjective,
									 final DataCollectionSum timings) {
		super(featSpec, strayProb, nIterations, dynProjective);
		this.timings = timings;
	}

	/*
	public void list() {
		timings.list(3);
		System.out.println(
				timings.averagesString("linear", 1.0));
		System.out.println(
				timings.averagesString("quadratic", 1.0));
		System.out.println(
				timings.averagesString("simple", 1.0));
	}
	*/

	protected boolean allowableTree(final Token[] tokens, final int i, final int n) {
		final int nMeasure = 100000;
		DependencyGraph g = new DependencyGraph(tokens);
		/*
		if (speedMeasuringPhase) {
			if (fromSentence-- >= 0) {
				toSentence--;
				return false;
			}
		}
		*/
		return (!speedMeasuringPhase && i < n ||
				// i < toSentence &&
						i < nMeasure)
				&& (nonprojectiveAllowed || g.isProjective());
	}

	protected SimpleParser makeStrayingParser(Token[] tokens,
											  final SimpleExtractor staticExtractor) {
		speedMeasuringPhase = true;
		if (dynProjective && nonprojectiveAllowed)
			tokens = OptimalProjectivizer.projectivize(tokens);
		// System.out.println("done " + (counter++));
		final SimpleDynamicParser parser =
				new SimpleDynamicParserTester(tokens, leftDependentsFirst, strict,
						staticExtractor, timings);
		parser.setStrayProb(strayProb);
		parser.setProjective(dynProjective);
		// parser.setChooseMode(SimpleDynamicParser.ChooseMode.PRELIM_PRELIM);
		parser.setChooseMode(DynamicChooseMode.PRELIM);
		return parser;
	}

}
