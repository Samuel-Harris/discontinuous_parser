/*
 * Copyright (c) 2018. University of St Andrews
 */

package standrews.depextract;

import standrews.depautomata.SimpleConfig;
import standrews.classification.*;
import standrews.depmethods.ArcEagerParser;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;

public class ArcEagerExtractor extends SimpleExtractor {

	public static final String shift = ArcEagerParser.shift;
	public static final String rightArc = ArcEagerParser.rightArc;
	public static final String leftArc = ArcEagerParser.leftArc;
	public static final String reduce = ArcEagerParser.reduce;
	public static final String reduceCorrect = ArcEagerParser.reduceCorrect;

	/**
	 * Specification featSpec uses in addition to those in SimpleExtractor:
	 * parentPoss: From which prefix elements, is the parent's POS a feature.
	 * prefixLabels: From which prefix elements do we take label.
	 */

	protected ArcEagerExtractor(final FeatureSpecification featSpec, final ClassifierFactory factory) {
		super(featSpec, factory);
	}

	public ArcEagerExtractor(final FeatureSpecification featSpec, final ClassifierFactory factory,
							 final String actionFile,
							 final String deprelLeftFile, final String deprelRightFile) {
		this(featSpec, factory);
		makeClassifiers(actionFile, deprelLeftFile, deprelRightFile);
		completeClassifiers();
	}

	protected Classifier getDeprelClassifier(final String ac) {
		return useTwoDeprelClassifiers && ac.equals(leftArc) ?
				deprelRightClassifier :
				deprelLeftClassifier;
	}

	public Iterator<String[]> predict(final SimpleConfig config) {
		final Features actionFeats = extract(config);
		final String[] acs = actionClassifier.predictAll(actionFeats);
		return new ActionIterator(config, acs);
	}

	protected class ActionIterator implements Iterator<String[]> {
		private final SimpleConfig config;
		private final LinkedList<String> acs;
		public ActionIterator(final SimpleConfig config, String[] acs) {
			this.config = config;
			this.acs = new LinkedList(Arrays.asList(acs));
		}

		@Override
		public boolean hasNext() {
			return !acs.isEmpty();
		}

		@Override
		public String[] next() {
			if (acs.isEmpty())
				return null;
			String ac = acs.removeFirst();
			if (ac.equals(shift) || ac.equals(reduce)) {
				return new String[]{ac};
			} else {
				Features deprelFeats = extract(config);
				if (!useTwoDeprelClassifiers)
					deprelFeats.putString("action", ac);
				String deprel = getDeprelClassifier(ac).predict(deprelFeats);
				return new String[]{ac, deprel};
			}
		}
	}

	protected String[] actionNames() {
		return ArcEagerParser.actionNames;
	}

	protected String[] prefixLabels() {
		return ArcEagerParser.prefixLabels;
	}

	/*
	protected void completeCorrectAction() {
		actionClassifier.addObservation(new Features(), reduceCorrect);
	}
	*/
}
