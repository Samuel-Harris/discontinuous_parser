/*
 * Copyright (c) 2018. University of St Andrews
 */

package standrews.depextract;

import standrews.depautomata.HatConfig;
import standrews.depautomata.SimpleConfig;
import standrews.classification.*;
import standrews.depbase.Deprel;
import standrews.depbase.Form;
import standrews.depbase.Lemma;
import standrews.depbase.Upos;
import standrews.depmethods.HatParser;

import java.util.*;

public class HatExtractor extends SimpleExtractor {
	public static final String shift = HatParser.shift;
	public static final String reduceToHat = HatParser.reduceToHat;
	public static final String reduceFromHat = HatParser.reduceFromHat;

	/**
	 * Specification featSpec uses in addition to those in SimpleExtractor:
	 * parentPoss: From which prefix elements, is the parent's POS a feature.
	 * prefixLabels: From which prefix elements do we take label.
	 */

	/**
	 * Specification featSpec uses in addition to those in SimpleExtractor:
	 * hatPoss: From which prefix elements is POS to be feature, relative to hat index.
	 * hatLeftCap: For the feature of absolute hat index, what is its cap (non-negative).
	 * hatRightCap: For the feature of hat index relative to length of prefix, what is its cap (non-negative).
	 * viewMin: How far back can features look (negative or zero).
	 * viewMax: How far forward can features look (positive or zero).
	 */

	/**
	 * Experimental option to suppress use of compression features.
	 */
	private boolean suppressCompression;

	/**
	 * Classifier that determines fellow index.
	 */
	protected final Classifier fellowClassifier;

	public HatExtractor(final FeatureSpecification featSpec,
						final ClassifierFactory factory,
						final String actionFile, final String fellowFile,
						final String deprelLeftFile, final String deprelRightFile,
						final boolean suppressCompression) {
		super(featSpec, factory, actionFile, deprelLeftFile, deprelRightFile);
		this.suppressCompression = suppressCompression;
		this.fellowClassifier = factory.makeClassifier(fellowFile);
		completeHatClassifiers();
	}

	public void train() {
		super.train();
		fellowClassifier.train();
	}

	public void extract(final SimpleConfig simpleConfig, final String[] action) {
		final HatConfig config = (HatConfig) simpleConfig;
		final Features actionFeats = extract(config);
		System.out.println("fix this");
		System.exit(1);
//		actionClassifier.addObservation(actionFeats, action[0]);
		if (action.length > 1) {
			final int viewMin = featSpec.getIntFeature("viewMin", 0);
			final int viewMax = featSpec.getIntFeature("viewMax", 0);
			final String[] actionCompressed =
					HatParser.actionToCompression(config, action, viewMin, viewMax);
			final Features fellowFeats = extract(config);
			fellowFeats.putString("action", action[0]);
//			fellowClassifier.addObservation(fellowFeats, actionCompressed[1]);
			final Features deprelFeats = extract(config);
			deprelFeats.putString("action", action[0]);
			deprelFeats.putString("fellow", actionCompressed[1]);
//			deprelLeftClassifier.addObservation(deprelFeats, action[2]); // TODO
		}
	}

	protected Features extract(final SimpleConfig simpleConfig) {
		final HatConfig config = (HatConfig) simpleConfig;
		final Features feats = super.extract(config);
		extractHatPoss(feats, config);
		extractHatLeftIndex(feats, config);
		extractHatRightIndex(feats, config);
		extractHatForms(feats, config);
		extractHatLemmas(feats, config);
		extractHatLeftDeprels(feats, config);
		extractHatRightDeprels(feats, config);
		if (!suppressCompression) {
			extractCompressionLeft(feats, config);
			extractCompressionRight(feats, config);
		}
		return feats;
	}

	protected void completeHatClassifiers() {
		if (getContinuousTraining()) {
			hatCompleteClassifier(actionClassifier);
			simpleCompleteClassifier(fellowClassifier);
			makeAllAction(fellowClassifier);
			hatCompleteClassifier(fellowClassifier);
			makeAllFellowResponses(fellowClassifier);
			hatCompleteDeprelClassifier(deprelLeftClassifier);
			hatCompleteDeprelClassifier(deprelRightClassifier);
		}
	}

	protected void hatCompleteDeprelClassifier(Classifier classifier) {
		hatCompleteClassifier(classifier);
		makeAllFellow(classifier);
	}

	protected void hatCompleteClassifier(Classifier classifier) {
		makeAllHatPoss(classifier);
		makeAllHatLeftIndex(classifier);
		makeAllHatRightIndex(classifier);
		makeAllHatForms(classifier);
		makeAllHatLemmas(classifier);
		makeAllHatLeftDeprels(classifier);
		makeAllHatRightDeprels(classifier);
		if (!suppressCompression) {
			makeAllLeftCompression(classifier);
			makeAllRightCompression(classifier);
		}
	}

	public Iterator<String[]> predict(final SimpleConfig config) {
		final Features actionFeats = extract(config);
		final String[] acs = actionClassifier.predictAll(actionFeats);
		return new ActionIterator(config, acs);
	}

	private class ActionIterator implements Iterator<String[]> {
		private final SimpleConfig config;
		private final LinkedList<String> acs;
		private String ac = null;
		private LinkedList<String> fellows;
		private String[] action = null;
		public ActionIterator(final SimpleConfig config, String[] acs) {
			this.config = config;
			this.acs = new LinkedList(Arrays.asList(acs));
		}

		@Override
		public boolean hasNext() {
			if (action != null)
				return true;
			if (ac == null) {
				if (acs.isEmpty())
					return false;
				ac = acs.removeFirst();
				if (ac.equals(shift)) {
					action = new String[]{ac};
					ac = null;
					return true;
				} else {
					final Features fellowFeats = extract(config);
					fellowFeats.putString("action", ac);
					final String[] fs = fellowClassifier.predictAll(fellowFeats);
					fellows = new LinkedList(Arrays.asList(fs));
				}
			}
			if (fellows.isEmpty()) {
				ac = null;
				return hasNext();
			}
			final String fellow = fellows.removeFirst();
			final Features deprelFeats = extract(config);
			deprelFeats.putString("action", ac);
			deprelFeats.putString("fellow", fellow);
			final String deprel = deprelLeftClassifier.predict(deprelFeats); // TODO
			action = new String[]{ac, fellow, deprel};
			return true;
		}

		@Override
		public String[] next() {
			final String[] nextAc = action;
			action = null;
			return nextAc;
		}
	}

	// TODO
	public double actionProbability(final HatConfig config, String[] action) {
		double actionP = super.actionProbability(config, action[0]);
		switch (action[0]) {
			case shift:
				break;
			case reduceToHat:
				// reduceToHat(config, Integer.parseInt(action[1]), action[2]);
				break;
			case reduceFromHat:
				// reduceFromHat(config, Integer.parseInt(action[1]), action[2]);
				break;
		}
		return actionP;
	}

	protected String hatPosFeature(int i) {
		return "hatPos_" + i;
	}
	protected void extractHatPoss(final Features feats, final HatConfig config) {
		for (int i : featSpec.getIntsFeature("hatPoss")) {
			String posStr = null;
			final int abs = config.getHatAbsoluteIndex(i);
			if (0 <= abs && abs < config.prefixLength()) {
				final Upos pos = config.getPrefixLeft(abs).getToken().upos;
				if (pos != null)
					posStr = pos.toString();
			}
			feats.putString(hatPosFeature(i), posStr);
		}
	}
	protected void makeAllHatPoss(final Classifier classifier) {
		for (int i : featSpec.getIntsFeature("hatPoss")) {
			for (String pos : Upos.UPOSS)
				classifier.addStringValue(hatPosFeature(i), pos);
			classifier.addStringValue(hatPosFeature(i), null);
		}
	}

	protected String hatLeftIndexFeature() {
		return "hatLeftIndex";
	}
	protected void extractHatLeftIndex(final Features feats, final HatConfig config) {
		final int hatLeftCap = featSpec.getIntFeature("hatLeftCap", 0);
		final int cappedIndex = Math.min(config.getAbsoluteHatIndex(), hatLeftCap);
		feats.putString(hatLeftIndexFeature(), "" + cappedIndex);
	}
	protected void makeAllHatLeftIndex(final Classifier classifier) {
		final int hatLeftCap = featSpec.getIntFeature("hatLeftCap", 0);
		for (int i = 0; i <= hatLeftCap; i++)
			classifier.addStringValue(hatLeftIndexFeature(), "" + i);
	}

	protected String hatRightIndexFeature() {
		return "hatRightIndex";
	}
	protected void extractHatRightIndex(final Features feats, final HatConfig config) {
		final int hatRightCap = featSpec.getIntFeature("hatRightCap", 0);
		final int cappedIndex = Math.min(config.prefixLength() - 1 - config.getAbsoluteHatIndex(), hatRightCap);
		feats.putString(hatRightIndexFeature(), "" + cappedIndex);
	}
	protected void makeAllHatRightIndex(final Classifier classifier) {
		final int hatRightCap = featSpec.getIntFeature("hatRightCap", 0);
		for (int i = 0; i <= hatRightCap; i++)
			classifier.addStringValue(hatRightIndexFeature(), "" + i);
	}

	protected String hatFormFeature(final int i) {
		return "hatForm_" + i;
	}
	protected void extractHatForms(final Features feats, final HatConfig config) {
		for (int i : featSpec.getIntsFeature("hatForms")) {
			double[] vec = featSpec.getFormVec().get();
			final int abs = config.getHatAbsoluteIndex(i);
			if (0 <= abs && abs < config.prefixLength()) {
				final Form form = config.getPrefixLeft(abs).getToken().form;
				if (form != null) {
					String s = form.toString();
					vec = featSpec.getFormVec().get(s);
				}
			}
			feats.putVector(hatFormFeature(i), vec);
		}
	}
	protected void makeAllHatForms(final Classifier classifier) {
		int len = featSpec.getFormVec().getLength();
		for (int i : featSpec.getIntsFeature("hatForms")) {
			classifier.setVectorLength(hatFormFeature(i), len);
		}
	}

	protected String hatLemmaFeature(final int i) {
		return "hatLemma_" + i;
	}
	protected void extractHatLemmas(final Features feats, final HatConfig config) {
		for (int i : featSpec.getIntsFeature("hatLemmas")) {
			double[] vec = featSpec.getLemmaVec().get();
			final int abs = config.getHatAbsoluteIndex(i);
			if (0 <= abs && abs < config.prefixLength()) {
				final Lemma lemma = config.getPrefixLeft(abs).getToken().lemma;
				if (lemma != null) {
					String s = lemma.toString();
					vec = featSpec.getLemmaVec().get(s);
				}
			}
			feats.putVector(hatLemmaFeature(i), vec);
		}
	}
	protected void makeAllHatLemmas(final Classifier classifier) {
		int len = featSpec.getLemmaVec().getLength();
		for (int i : featSpec.getIntsFeature("hatLemmas")) {
			classifier.setVectorLength(hatLemmaFeature(i), len);
		}
	}

	protected String hatLeftDepFeature(int i) {
		return "hatLeftDep_" + i;
	}
	protected void extractHatLeftDeprels(final Features feats, final HatConfig config) {
		for (int i : featSpec.getIntsFeature("hatLeftDeprels")) {
			String deprelStr = null;
			final int abs = config.getHatAbsoluteIndex(i);
			if (0 <= abs && abs < config.prefixLength()) {
				final Deprel deprel = config.getPrefixLeft(abs).getLeftmostDeprel();
				if (deprel != null)
					deprelStr = deprel.uniPart();
			}
			feats.putString(hatLeftDepFeature(i), deprelStr);
		}
	}
	protected void makeAllHatLeftDeprels(final Classifier classifier) {
		for (int i : featSpec.getIntsFeature("hatLeftDeprels")) {
			for (String deprel : Deprel.DEPRELS)
				classifier.addStringValue(hatLeftDepFeature(i), deprel);
			classifier.addStringValue(hatLeftDepFeature(i), null);
		}
	}

	protected String hatRightDepFeature(int i) {
		return "hatRightDep_" + i;
	}
	protected void extractHatRightDeprels(final Features feats, final HatConfig config) {
		for (int i : featSpec.getIntsFeature("hatRightDeprels")) {
			String deprelStr = null;
			final int abs = config.getHatAbsoluteIndex(i);
			if (0 <= abs && abs < config.prefixLength()) {
				final Deprel deprel = config.getPrefixLeft(abs).getRightmostDeprel();
				if (deprel != null)
					deprelStr = deprel.uniPart();
			}
			feats.putString(hatRightDepFeature(i), deprelStr);
		}
	}
	protected void makeAllHatRightDeprels(final Classifier classifier) {
		for (int i : featSpec.getIntsFeature("hatRightDeprels")) {
			for (String deprel : Deprel.DEPRELS)
				classifier.addStringValue(hatRightDepFeature(i), deprel);
			classifier.addStringValue(hatRightDepFeature(i), null);
		}
	}

	protected String leftCompressionFeature() {
		return "leftCompression";
	}
	protected void extractCompressionLeft(final Features feats, final HatConfig config) {
		TreeSet<String> poss = new TreeSet<>();
		final int viewMin = featSpec.getIntFeature("viewMin", 0);
		for (int i = 0; i < config.getAbsoluteHatIndex() + viewMin; i++) {
			final Upos pos = config.getPrefixLeft(i).getToken().upos;
			if (pos != null)
				poss.add(pos.toString());
		}
		feats.putSet(leftCompressionFeature(), poss);
	}
	protected void makeAllLeftCompression(final Classifier classifier) {
		classifier.addSetValues(leftCompressionFeature(), Arrays.asList(Upos.UPOSS));
	}

	protected String rightCompressionFeature() {
		return "rightCompression";
	}
	protected void extractCompressionRight(final Features feats, final HatConfig config) {
		TreeSet<String> poss = new TreeSet<>();
		final int viewMax = featSpec.getIntFeature("viewMax", 0);
		for (int i = config.prefixLength() - 1; i > config.getAbsoluteHatIndex() + viewMax; i--) {
			final Upos pos = config.getPrefixLeft(i).getToken().upos;
			if (pos != null)
				poss.add(pos.toString());
		}
		feats.putSet(rightCompressionFeature(), poss);
	}
	protected void makeAllRightCompression(final Classifier classifier) {
		classifier.addSetValues(rightCompressionFeature(), Arrays.asList(Upos.UPOSS));
	}

	protected void makeAllActionResponses(final Classifier classifier) {
		for (String actionName : HatParser.actionNames)
			classifier.addResponseValue(actionName);
	}

	protected void makeAllAction(final Classifier classifier) {
		for (String actionName : HatParser.actionNames)
			classifier.addStringValue("action", actionName);
	}

	protected void makeAllFellowResponses(final Classifier classifier) {
		final int viewMin = featSpec.getIntFeature("viewMin", 0);
		final int viewMax = featSpec.getIntFeature("viewMax", 0);
		for (int i = viewMin; i <= viewMax; i++) {
			if (i != 0) {
				classifier.addResponseValue("" + i);
			}
		}
		for (String pos : Upos.UPOSS) {
			classifier.addResponseValue(HatParser.compressionLeft(pos));
			classifier.addResponseValue(HatParser.compressionRight(pos));
		}
		classifier.addResponseValue(HatParser.compressionLeft(null));
		classifier.addResponseValue(HatParser.compressionRight(null));
	}

	protected void makeAllFellow(final Classifier classifier) {
		final int viewMin = featSpec.getIntFeature("viewMin", 0);
		final int viewMax = featSpec.getIntFeature("viewMax", 0);
		for (int i = viewMin; i <= viewMax; i++) {
			if (i != 0) {
				classifier.addStringValue("fellow", "" + i);
			}
		}
		for (String pos : Upos.UPOSS) {
			classifier.addStringValue("fellow", HatParser.compressionLeft(pos));
			classifier.addStringValue("fellow", HatParser.compressionRight(pos));
		}
		classifier.addStringValue("fellow", HatParser.compressionLeft(null));
		classifier.addStringValue("fellow", HatParser.compressionRight(null));
	}
}
