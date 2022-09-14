/*
 * Copyright (c) 2019. University of St Andrews
 */

package standrews.constmain;

import standrews.classification.FeatureSpecification;
import standrews.constbase.ConstTree;
import standrews.constmethods.DeterministicParser;
import standrews.constmethods.HatParser;

public class HatTrainer extends SimpleTrainer {

	public HatTrainer(final FeatureSpecification featSpec) {
		super(featSpec);
	}

	protected boolean allowableTree(final ConstTree tree) {
		return true;
	}

	protected DeterministicParser makeParser(final ConstTree tree) {
		final int viewMin = featSpec.getIntFeature("viewMin", 0);
		final int viewMax = featSpec.getIntFeature("viewMax", 0);
		final HatParser parser = new HatParser(tree, viewMin, viewMax);
		parser.setLeftDependentsFirst(leftDependentsFirst);
		return parser;
	}

}
