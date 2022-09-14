/*
 * Copyright (c) 2019. University of St Andrews
 */

package standrews.constmain;

import standrews.aux.PropertyWeights;
import standrews.constbase.ConstTree;
import standrews.constbase.ConstTreebank;
import standrews.constextract.WholeHatExtractor;
import standrews.constmethods.WholeHatParser;

import java.lang.invoke.MethodHandles;
import java.util.logging.Logger;

public class WholeHatTester {

	public static final String[] weightedProperties = WholeHatParser.weightedProperties;

	public int test(final ConstTreebank treebank,
					final int m, final int n, final WholeHatExtractor extractor) {
		final ConstTreebank subbank = treebank.part(m, m+n);
		int i = 0;
		PropertyWeights weights = new PropertyWeights(weightedProperties);
		for (ConstTree tree : subbank.getTrees()) {
			WholeHatParser parser = new WholeHatParser(tree);
			weights.add(parser.prob(extractor));
			i++;
		}
		reportFine("" + weights);
		return i;
	}

	private static Logger logger() {
		final Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass().getName());
		log.setParent(Logger.getGlobal());
		return log;
	}

	/**
	 * Report failure.
	 *
	 * @param message The thing that failed.
	 */
	protected static void fail(final String message) {
		logger().severe(message);
	}

	private static void reportFine(final String message) {
		logger().fine(message);
	}

}
