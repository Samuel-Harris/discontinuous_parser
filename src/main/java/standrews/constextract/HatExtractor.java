/*
 * Copyright (c) 2019. University of St Andrews
 */

package standrews.constextract;

import standrews.classification.Classifier;
import standrews.classification.ClassifierFactory;
import standrews.classification.FeatureSpecification;
import standrews.classification.Features;
import standrews.constautomata.HatConfig;
import standrews.constautomata.SimpleConfig;
import standrews.constbase.ConstLeaf;
import standrews.constbase.ConstTreebank;
import standrews.constmethods.HatParser;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TreeSet;

public class HatExtractor extends SimpleExtractor {
	public static final String shift = HatParser.shift;
	public static final String reduceUpHat = HatParser.reduceUpHat;
	public static final String reduceToHat = HatParser.reduceToHat;
	public static final String reduceFromHat = HatParser.reduceFromHat;

	/**
	 * Experimental option to suppress use of compression features.
	 */
	private boolean suppressCompression = false;

	/**
	 * Classifier that determines fellow index.
	 */
	protected final Classifier fellowClassifier;

	public HatExtractor(final ConstTreebank treebank,
						final FeatureSpecification featSpec,
						final ClassifierFactory factory,
						final String actionFile, final String fellowFile, final String catFile,
						final boolean suppressCompression) {
		super(treebank, featSpec, factory, actionFile, catFile);
		this.suppressCompression = suppressCompression;
		this.fellowClassifier = factory.makeClassifier(fellowFile);
		completeHatClassifiers(treebank);
	}

	public void train() {
		super.train();
		fellowClassifier.train();
	}

	public void extract(final SimpleConfig simpleConfig, final String[] action) {
		final HatConfig config = (HatConfig) simpleConfig;
		final Features actionFeats = extract(config);
		actionClassifier.addObservation(actionFeats, action[0]);
		if (action[0].equals(reduceUpHat)) {
			final Features catFeats = extract(config);
			catClassifier.addObservation(catFeats, action[1]);
		} else if (action[0].equals(reduceToHat) || action[0].equals(reduceFromHat)) {
			final int viewMin = featSpec.getIntFeature("viewMin", 0);
			final int viewMax = featSpec.getIntFeature("viewMax", 0);
			final String[] actionCompressed =
					HatParser.actionToCompression(config, action, viewMin, viewMax);
			final Features fellowFeats = extract(config);
			fellowFeats.putString("action", action[0]);
			fellowClassifier.addObservation(fellowFeats, actionCompressed[1]);
		}
	}

	protected Features extract(final SimpleConfig simpleConfig) {
		final HatConfig config = (HatConfig) simpleConfig;
		final Features feats = super.extract(config);
		extractHatCats(feats, config);
		extractHatPoss(feats, config);
		extractHatLeftCats(feats, config);
		extractHatRightCats(feats, config);
		extractHatLeftIndex(feats, config);
		extractHatRightIndex(feats, config);
		extractHatForms(feats, config);
		if (!suppressCompression) {
			extractCompressionLeft(feats, config);
			extractCompressionRight(feats, config);
		}
		return feats;
	}

	protected void completeHatClassifiers(ConstTreebank treebank) {
		if (continuousTraining) {
			hatCompleteClassifier(actionClassifier, treebank);
			simpleCompleteClassifier(fellowClassifier, treebank);
			makeAllAction(fellowClassifier);
			hatCompleteClassifier(fellowClassifier, treebank);
			makeAllFellowResponses(fellowClassifier, treebank);
			hatCompleteClassifier(catClassifier, treebank);
		}
	}
	protected void hatCompleteClassifier(final Classifier classifier,
										 final ConstTreebank treebank) {
		makeAllHatCats(classifier, treebank);
		makeAllHatPoss(classifier, treebank);
		makeAllHatLeftCats(classifier, treebank);
		makeAllHatRightCats(classifier, treebank);
		makeAllHatLeftIndex(classifier);
		makeAllHatRightIndex(classifier);
		makeAllHatForms(classifier);
		if (!suppressCompression) {
			makeAllLeftCompression(classifier, treebank);
			makeAllRightCompression(classifier, treebank);
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
				} else if (ac.equals(reduceUpHat)) {
					final Features catFeats = extract(config);
					final String cat = catClassifier.predict(catFeats);
					action = new String[]{ac, cat};
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
			action = new String[]{ac, fellow};
			return true;
		}

		@Override
		public String[] next() {
			final String[] nextAc = action;
			action = null;
			return nextAc;
		}
	}

	protected String hatCatFeature(int i) {
		return "hatCat_" + i;
	}
	protected void extractHatCats(final Features feats, final HatConfig config) {
		for (int i : featSpec.getIntsFeature("hatCats")) {
			String cat = null;
			final int abs = config.getHatAbsoluteIndex(i);
			if (0 <= abs && abs < config.stackLength()) {
				cat = config.getStackLeft(abs).getCat();
			}
			feats.putString(hatCatFeature(i), cat);
		}
	}
	protected void makeAllHatCats(final Classifier classifier,
								  final ConstTreebank treebank) {
		for (int i : featSpec.getIntsFeature("hatCats")) {
			for (String cat : treebank.getPossAndCats())
				classifier.addStringValue(hatCatFeature(i), cat);
			classifier.addStringValue(hatCatFeature(i), null);
		}
	}

	protected String hatPosFeature(int i) {
		return "hatPos_" + i;
	}
	protected void extractHatPoss(final Features feats, final HatConfig config) {
		for (int i : featSpec.getIntsFeature("hatPoss")) {
			String pos = null;
			final int abs = config.getHatAbsoluteIndex(i);
			if (0 <= abs && abs < config.stackLength()) {
				final ConstLeaf leaf = config.getStackLeft(abs).getHeadLeaf();
				if (leaf != null)
					pos = leaf.getCat();
			}
			feats.putString(hatPosFeature(i), pos);
		}
	}
	protected void makeAllHatPoss(final Classifier classifier,
								  final ConstTreebank treebank) {
		for (int i : featSpec.getIntsFeature("hatPoss")) {
			for (String pos : treebank.getPoss())
				classifier.addStringValue(hatPosFeature(i), pos);
			classifier.addStringValue(hatPosFeature(i), null);
		}
	}

	protected String hatFormFeature(final int i) {
		return "hatForm_" + i;
	}
	protected void extractHatForms(final Features feats,
									 final HatConfig config) {
		for (int i : featSpec.getIntsFeature("hatForms")) {
			double[] vec = featSpec.getFormVec().get();
			final int abs = config.getHatAbsoluteIndex(i);
			if (0 <= abs && abs < config.stackLength()) {
				final ConstLeaf leaf = config.getStackLeft(abs).getHeadLeaf();
				if (leaf != null)
					vec = featSpec.getFormVec().get(leaf.getForm());
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

	protected String hatLeftCatFeature(int i) {
		return "hatLeftCat_" + i;
	}
	protected void extractHatLeftCats(final Features feats, final HatConfig config) {
		for (int i : featSpec.getIntsFeature("hatLeftCats")) {
			String cat = null;
			final int abs = config.getHatAbsoluteIndex(i);
			if (0 <= abs && abs < config.stackLength()) {
				cat = getLeftmostCat(config.getStackLeft(abs));
			}
			feats.putString(hatLeftCatFeature(i), cat);
		}
	}
	protected void makeAllHatLeftCats(final Classifier classifier,
								  final ConstTreebank treebank) {
		for (int i : featSpec.getIntsFeature("hatLeftCats")) {
			for (String cat : treebank.getPossAndCats())
				classifier.addStringValue(hatLeftCatFeature(i), cat);
			classifier.addStringValue(hatLeftCatFeature(i), null);
		}
	}

	protected String hatRightCatFeature(int i) {
		return "hatRightCat_" + i;
	}
	protected void extractHatRightCats(final Features feats, final HatConfig config) {
		for (int i : featSpec.getIntsFeature("hatRightCats")) {
			String cat = null;
			final int abs = config.getHatAbsoluteIndex(i);
			if (0 <= abs && abs < config.stackLength()) {
				cat = getRightmostCat(config.getStackLeft(abs));
			}
			feats.putString(hatRightCatFeature(i), cat);
		}
	}
	protected void makeAllHatRightCats(final Classifier classifier,
								  final ConstTreebank treebank) {
		for (int i : featSpec.getIntsFeature("hatRightCats")) {
			for (String cat : treebank.getPossAndCats())
				classifier.addStringValue(hatRightCatFeature(i), cat);
			classifier.addStringValue(hatRightCatFeature(i), null);
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
		final int cappedIndex = Math.min(config.stackLength() - 1 - config.getAbsoluteHatIndex(), hatRightCap);
		feats.putString(hatRightIndexFeature(), "" + cappedIndex);
	}
	protected void makeAllHatRightIndex(final Classifier classifier) {
		final int hatRightCap = featSpec.getIntFeature("hatRightCap", 0);
		for (int i = 0; i <= hatRightCap; i++)
			classifier.addStringValue(hatRightIndexFeature(), "" + i);
	}

	protected String leftCompressionFeature() {
		return "leftCompression";
	}
	protected void extractCompressionLeft(final Features feats, final HatConfig config) {
		TreeSet<String> cats = new TreeSet<>();
		final int viewMin = featSpec.getIntFeature("viewMin", 0);
		for (int i = 0; i < config.getAbsoluteHatIndex() + viewMin; i++) {
			final String cat = config.getStackLeft(i).getCat();
			if (cat != null)
				cats.add(cat);
		}
		feats.putSet(leftCompressionFeature(), cats);
	}
	protected void makeAllLeftCompression(final Classifier classifier,
									final ConstTreebank treebank) {
		classifier.addSetValues(leftCompressionFeature(), treebank.getPossAndCats());
	}

	protected String rightCompressionFeature() {
		return "rightCompression";
	}
	protected void extractCompressionRight(final Features feats, final HatConfig config) {
		TreeSet<String> cats = new TreeSet<>();
		final int viewMax = featSpec.getIntFeature("viewMax", 0);
		for (int i = config.stackLength() - 1; i > config.getAbsoluteHatIndex() + viewMax; i--) {
			final String cat = config.getStackLeft(i).getCat();
			if (cat != null)
				cats.add(cat);
		}
		feats.putSet(rightCompressionFeature(), cats);
	}
	protected void makeAllRightCompression(final Classifier classifier,
									final ConstTreebank treebank) {
		classifier.addSetValues(rightCompressionFeature(), treebank.getPossAndCats());
	}

	protected void makeAllActionResponses(final Classifier classifier) {
		for (String actionName : HatParser.actionNames)
			classifier.addResponseValue(actionName);
	}

	protected void makeAllAction(final Classifier classifier) {
		for (String actionName : HatParser.actionNames) {
			classifier.addStringValue("action", actionName);
		}
	}

	protected void makeAllFellowResponses(final Classifier classifier,
									final ConstTreebank treebank) {
		final int viewMin = featSpec.getIntFeature("viewMin", 0);
		final int viewMax = featSpec.getIntFeature("viewMax", 0);
		for (int i = viewMin; i <= viewMax; i++) {
			if (i != 0) {
				classifier.addResponseValue("" + i);
			}
		}
		for (String cat : treebank.getPossAndCats()) {
			classifier.addResponseValue(HatParser.compressionLeft(cat));
			classifier.addResponseValue(HatParser.compressionRight(cat));
		}
		classifier.addResponseValue(HatParser.compressionLeft(null));
		classifier.addResponseValue(HatParser.compressionRight(null));
	}

}
