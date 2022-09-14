/*
 * Copyright (c) 2019. University of St Andrews
 */

package standrews.constmain;

import standrews.classification.FeatureSpecification;
import standrews.constautomata.HatConfig;
import standrews.constautomata.SimpleConfig;
import standrews.constbase.ConstTree;
import standrews.constmethods.DeterministicParser;
import standrews.constmethods.HatParser;

public class HatTester extends SimpleTester {

	public HatTester(final FeatureSpecification featSpec) {
		super(featSpec);
	}

	private class ParserWithCompression extends HatParser {
		public ParserWithCompression(final ConstTree tree,
									 final int viewMin,
									 final int viewMax) {
			super(tree, viewMin, viewMax);
		}

		protected void apply(final SimpleConfig simpleConfig, final String[] action) {
			HatConfig config = (HatConfig) simpleConfig;
			final String[] actionUncompressed = actionFromCompression(config, action);
			super.apply(config, actionUncompressed);
		}

		protected boolean applicable(final SimpleConfig simpleConfig, final String[] action) {
			HatConfig config = (HatConfig) simpleConfig;
			final String[] actionUncompressed = actionFromCompression(config, action);
			return actionUncompressed.length > 0 &&
					super.applicable(config, actionUncompressed);
		}
	}

	protected DeterministicParser makeParser(final ConstTree tree) {
		final int viewMin = featSpec.getIntFeature("viewMin", 0);
		final int viewMax = featSpec.getIntFeature("viewMax", 0);
		return new ParserWithCompression(tree, viewMin, viewMax);
	}
}
