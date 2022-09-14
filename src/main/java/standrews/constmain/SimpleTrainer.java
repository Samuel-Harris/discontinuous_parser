/*
 * Copyright (c) 2019. University of St Andrews
 */

package standrews.constmain;

import standrews.classification.FeatureSpecification;
import standrews.constextract.SimpleExtractor;
import standrews.constbase.ConstTree;
import standrews.constbase.ConstTreebank;
import standrews.constmethods.DeterministicParser;
import standrews.constmethods.SimpleParser;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.lang.invoke.MethodHandles;
import java.util.logging.Logger;

public class SimpleTrainer {

	protected FeatureSpecification featSpec;

	public SimpleTrainer(final FeatureSpecification featSpec) {
		this.featSpec = featSpec;
	}

	protected boolean leftDependentsFirst = false;

	protected boolean strict = false;

	protected boolean projectivize = false;

	public void setLeftDependentsFirst(final boolean b) {
		leftDependentsFirst = b;
	}

	public void setStrict(final boolean b) {
		strict = b;
	}

	public void setProjectivize(final boolean p) {
		projectivize = p;
	}

	public int train(final ConstTreebank treebank,
					 final int n, final SimpleExtractor extractor) {
		return train(treebank, null, n, extractor);
	}

	public int train(final ConstTreebank treebank,
					 final String corpusCopy,
					 final int n, final SimpleExtractor extractor) {
		final ConstTreebank subbank = treebank.part(0, n);
		copyTraining(subbank, corpusCopy);
		int i = 0;
		for (int epo = 0; epo == 0 || extractor.getContinuousTraining() && epo < extractor.getNEpochs(); epo++) {
			if (extractor.getContinuousTraining())
				reportFine("Epoch " + epo);
			i = 0;
			for (ConstTree tree : subbank.getTrees()) {
				if (allowableTree(tree)) {
					DeterministicParser parser = makeParser(tree);
					parser.observe(extractor);
					i++;
				}
			}
			extractor.train();
		}
		return i;
	}

	private void copyTraining(ConstTreebank treebank,
					 final String corpusCopy) {
		PrintWriter trainWriter = null;
		try {
			if (corpusCopy != null)
				trainWriter = new PrintWriter(corpusCopy, "UTF-8");
		} catch (FileNotFoundException e) {
			fail("Cannot create file: " + e);
		} catch (UnsupportedEncodingException e) {
			fail("Unsupported encoding: " + e);
		}
		if (trainWriter != null) {
			trainWriter.print("" + treebank);
			trainWriter.close();
		}
	}

	protected boolean allowableTree(final ConstTree tree) {
		return projectivize || tree.isProjective();
	}

	protected DeterministicParser makeParser(final ConstTree tree) {
		/*
		if (nonprojectiveAllowed)
			tokens = OptimalProjectivizer.nonprojectiveAllowed(tokens);
			*/
		final SimpleParser parser = new SimpleParser(tree);
		parser.setLeftDependentsFirst(leftDependentsFirst);
		return parser;
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

	/**
	 * Report fine comment.
	 *
	 * @param message The message.
	 */
	protected void reportFine(final String message) {
		final Logger log = Logger.getLogger(getClass().getName());
		log.setParent(Logger.getGlobal());
		log.fine(message);
	}
}
