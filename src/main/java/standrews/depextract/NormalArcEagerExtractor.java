/*
 * Copyright (c) 2018. University of St Andrews
 */

package standrews.depextract;

import standrews.depautomata.DependencyVertex;
import standrews.depautomata.SimpleConfig;
import standrews.classification.Classifier;
import standrews.classification.ClassifierFactory;
import standrews.classification.FeatureSpecification;
import standrews.classification.Features;
import standrews.depbase.Deprel;
import standrews.depbase.Form;
import standrews.depbase.Lemma;
import standrews.depbase.Upos;
import standrews.depmethods.NormalArcEagerParser;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;

public class NormalArcEagerExtractor extends ArcEagerExtractor {

	public static final String shift = NormalArcEagerParser.shift;
	public static final String rightArc = NormalArcEagerParser.rightArc;
	public static final String leftArc = NormalArcEagerParser.leftArc;
	public static final String reduceLeft = NormalArcEagerParser.reduceLeft;
	public static final String reduceRight = NormalArcEagerParser.reduceRight;

	/**
	 * Labels attached to prefix elements.
	 */
	public static final String nil = "nil";

	/**
	 * Classifier for actions with nil label.
	 */
	public Classifier actionNilClassifier;

	/**
	 * Separate classifier for configurations with nil in top-of-stack?
	 */
	protected boolean useNilClassifier = false;

	/**
	 * Should indices in prefix be corrected for nil top-of-stack?
	 */
	protected boolean normalizationCorrection = true;

	public NormalArcEagerExtractor(final FeatureSpecification featSpec, final ClassifierFactory factory) {
		super(featSpec, factory);
	}

	public NormalArcEagerExtractor(final FeatureSpecification featSpec, final ClassifierFactory factory,
								   final String actionFile, final String actionNilFile,
								   final String deprelLeftFile, final String deprelRightFile) {
		this(featSpec, factory);
		makeClassifiers(actionFile, actionNilFile, deprelLeftFile, deprelRightFile);
		completeClassifiers();
	}

	protected void makeClassifiers(final String actionFile, final String actionNilFile,
								   final String deprelLeftFile, final String deprelRightFile) {
		this.actionClassifier = factory.makeClassifier(actionFile);
		this.actionNilClassifier = factory.makeClassifier(actionNilFile);
		this.deprelLeftClassifier = factory.makeClassifier(deprelLeftFile);
		this.deprelRightClassifier = factory.makeClassifier(deprelRightFile);
	}

	public void train() {
		actionClassifier.train();
		if (useNilClassifier)
			actionNilClassifier.train();
		deprelLeftClassifier.train();
		if (useTwoDeprelClassifiers)
			deprelRightClassifier.train();
	}

	protected Classifier getDeprelClassifier(final String ac) {
		return useTwoDeprelClassifiers && ac.equals(reduceLeft) ?
				deprelLeftClassifier :
				deprelRightClassifier;
	}

	protected Classifier getActionClassifier(final SimpleConfig config) {
		return useNilClassifier && normalizationCorrection(config) ?
				actionNilClassifier :
				actionClassifier;
	}

	protected boolean normalizationCorrection(final SimpleConfig config) {
		return normalizationCorrection &&
				config.getLabelRight(0).equals(nil);
	}

	protected void completeClassifiers() {
		if (getContinuousTraining()) {
			if (useNilClassifier) {
				completeActionNonNilClassifier(actionClassifier);
				completeActionNilClassifier(actionNilClassifier);
			} else {
				completeActionClassifier(actionClassifier);
			}
			completeDeprelClassifier(deprelLeftClassifier);
			completeDeprelClassifier(deprelRightClassifier);
		}
	}

	protected void completeActionNonNilClassifier(Classifier classifier) {
		simpleCompleteClassifier(classifier);
		makeAllActionNonNilResponses(classifier);
	}

	protected void completeActionNilClassifier(Classifier classifier) {
		simpleCompleteClassifier(classifier);
		makeAllActionNilResponses(classifier);
	}

	public void extract(final SimpleConfig config, final String[] action) {
		final Features actionFeats = extract(config);
		final String ac = action[0];
		getActionClassifier(config).addObservation(actionFeats, ac);
		if (action.length > 1) {
			final Features deprelFeats = extract(config);
			if (!useTwoDeprelClassifiers)
				deprelFeats.putString("action", ac);
			getDeprelClassifier(ac).addObservation(deprelFeats, action[1]);
		}
	}

	public Iterator<String[]> predict(final SimpleConfig config) {
		final Features actionFeats = extract(config);
		final String[] acs = getActionClassifier(config).predictAll(actionFeats);
		return new ActionIterator(config, acs);
	}

	private class ActionIterator implements Iterator<String[]> {
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
			if (ac.equals(shift) ||
					ac.equals(rightArc) ||
					ac.equals(leftArc)) {
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

	public double actionProbability(final SimpleConfig config, String action) {
		final Features actionFeats = extract(config);
		return getActionClassifier(config).probability(actionFeats, action);
	}

	protected void extractPrefixPoss(final Features feats, final SimpleConfig config) {
		for (int i : featSpec.getIntsFeature("prefixPoss")) {
			String posStr = null;
			final int j = normalizationCorrection(config) ? i+1 : i;
			if (j < config.prefixLength()) {
				Upos pos = config.getPrefixRight(j).getToken().upos;
				if (pos != null)
					posStr = pos.toString();
			}
			feats.putString(prefixPosFeature(i), posStr);
		}
	}

	protected void extractSuffixPoss(final Features feats, final SimpleConfig config) {
		for (int i : featSpec.getIntsFeature("suffixPoss")) {
			Upos pos = null;
			final int j = normalizationCorrection(config) ? i - 1 : i;
			if (j < 0) {
				pos = config.getPrefixRight(0).getToken().upos;
			} else if (j < config.suffixLength()) {
				pos = config.getSuffixLeft(j).getToken().upos;
			}
			final String posStr = pos != null ? pos.toString() : null;
			feats.putString(suffixPosFeature(i), posStr);
		}
	}

	protected void extractPrefixForms(final Features feats, final SimpleConfig config) {
		for (int i : featSpec.getIntsFeature("prefixForms")) {
			double[] vec = featSpec.getFormVec().get();
			final int j = normalizationCorrection(config) ? i+1 : i;
			if (j < config.prefixLength()) {
				final Form form = config.getPrefixRight(j).getToken().form;
				if (form != null) {
					String s = form.toString();
					vec = featSpec.getFormVec().get(s);
				}
			}
			feats.putVector(prefixFormFeature(i), vec);
		}
	}

	protected void extractSuffixForms(final Features feats, final SimpleConfig config) {
		for (int i : featSpec.getIntsFeature("suffixForms")) {
			Form form = null;
			final int j = normalizationCorrection(config) ? i-1 : i;
			if (j < 0) {
				form = config.getPrefixRight(0).getToken().form;
			} else if (j < config.suffixLength()) {
				form = config.getSuffixLeft(j).getToken().form;
			}
			final double[] vec = form != null ?
				featSpec.getFormVec().get(form.toString()) :
				featSpec.getFormVec().get();
			feats.putVector(suffixFormFeature(i), vec);
		}
	}

	protected void extractPrefixLemmas(final Features feats, final SimpleConfig config) {
		for (int i : featSpec.getIntsFeature("prefixLemmas")) {
			double[] vec = featSpec.getFormVec().get();
			final int j = normalizationCorrection(config) ? i+1 : i;
			if (j < config.prefixLength()) {
				final Lemma lemma = config.getPrefixRight(j).getToken().lemma;
				if (lemma != null) {
					String s = lemma.toString();
					vec = featSpec.getLemmaVec().get(s);
				}
			}
			feats.putVector(prefixLemmaFeature(i), vec);
		}
	}

	protected void extractSuffixLemmas(final Features feats, final SimpleConfig config) {
		for (int i : featSpec.getIntsFeature("suffixLemmas")) {
			Lemma lemma = null;
			final int j = normalizationCorrection(config) ? i-1 : i;
			if (j < 0) {
				lemma = config.getPrefixRight(0).getToken().lemma;
			} else if (j < config.suffixLength()) {
				lemma = config.getSuffixLeft(j).getToken().lemma;
			}
			final double[] vec = lemma != null ?
				featSpec.getLemmaVec().get(lemma.toString()) :
				featSpec.getLemmaVec().get();
			feats.putVector(suffixLemmaFeature(i), vec);
		}
	}

	protected void extractPrefixLabels(final Features feats, final SimpleConfig config) {
		for (int i : featSpec.getIntsFeature("prefixLabels")) {
			String labelStr = null;
			final int j = normalizationCorrection(config) ? i+1 : i;
			if (j < config.prefixLength()) {
				labelStr = config.getLabelRight(j);
			}
			feats.putString(prefixLabelFeature(i), labelStr);
		}
	}

	protected void extractParentPoss(final Features feats, final SimpleConfig config) {
		for (int i : featSpec.getIntsFeature("parentPoss")) {
			String posStr = null;
			final int j = i; // normalizationCorrection(config) ? i+1 : i;
			if (j < config.prefixLength() - 1 &&
					config.getLabelRight(j).equals(NormalArcEagerParser.rightChild)) {
				DependencyVertex parent = config.getPrefixRight(j+1);
				if (parent != null) {
					Upos pos = parent.getToken().upos;
					if (pos != null) {
						posStr = pos.toString();
					}
				}
			}
			feats.putString(parentPosFeature(i), posStr);
		}
	}

	protected void extractLeftDeprels(final Features feats, final SimpleConfig config) {
		for (int i : featSpec.getIntsFeature("leftDeprels")) {
			String deprelStr = null;
			final int j = normalizationCorrection(config) ? i+1 : i;
			if (j < config.prefixLength()) {
				final Deprel deprel = config.getPrefixRight(j).getLeftmostDeprel();
				if (deprel != null)
					deprelStr = deprel.uniPart();
			}
			feats.putString(leftDepFeature(i), deprelStr);
		}
	}

	protected void extractRightDeprels(final Features feats, final SimpleConfig config) {
		for (int i : featSpec.getIntsFeature("rightDeprels")) {
			String deprelStr = null;
			final int j = normalizationCorrection(config) ? i+1 : i;
			if (j < config.prefixLength()) {
				final Deprel deprel = config.getPrefixRight(j).getRightmostDeprel();
				if (deprel != null)
					deprelStr = deprel.uniPart();
			}
			feats.putString(rightDepFeature(i), deprelStr);
		}
	}

	protected void makeAllActionNonNilResponses(final Classifier classifier) {
		classifier.addResponseValue(shift);
		classifier.addResponseValue(reduceLeft);
	}

	protected void makeAllActionNilResponses(final Classifier classifier) {
		classifier.addResponseValue(rightArc);
		classifier.addResponseValue(leftArc);
		classifier.addResponseValue(reduceRight);
	}

	protected String[] actionNames() {
		return NormalArcEagerParser.actionNames;
	}

	protected String[] prefixLabels() {
		if (normalizationCorrection)
			return new String[] {NormalArcEagerParser.leftChild, NormalArcEagerParser.rightChild};
		else
			return NormalArcEagerParser.prefixLabels;
	}
}
