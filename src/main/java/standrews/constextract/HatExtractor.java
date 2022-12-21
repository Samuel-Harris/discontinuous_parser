/*
 * Copyright (c) 2019. University of St Andrews
 */

package standrews.constextract;

import standrews.classification.FeatureVectorGenerator;
import standrews.classification.FellowResponseVectorGenerator;
import standrews.classification.MLP;
import standrews.classification.MLPFactory;
import standrews.constautomata.HatConfig;
import standrews.constmethods.HatParser;

import java.util.*;

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
    private final MLP fellowClassifier;

    public HatExtractor(final FeatureVectorGenerator featureVectorGenerator,
                        final MLPFactory mlpFactory,
                        final boolean suppressCompression, double tol, int patience) {
        super(featureVectorGenerator, mlpFactory, tol, patience);

        this.suppressCompression = suppressCompression;
        this.fellowClassifier = mlpFactory.makeMLP(new FellowResponseVectorGenerator(), tol, patience);
//		completeHatClassifiers(treebank);
    }

    public void train() {
        super.train();
        fellowClassifier.train();

        System.gc();
    }

    public List<Double> validateBatch() {
        double actionLossScore = actionClassifier.validateBatch();
        double catLossScore = catClassifier.validateBatch();
        double fellowLossScore = fellowClassifier.validateBatch();

        System.gc();

        return List.of(actionLossScore, catLossScore, fellowLossScore);
    }

    public MLP getFellowClassifier() {
        return fellowClassifier;
    }

    public void extract(final HatConfig config, final String[] action) {
//		final Features actionFeats = extract(config);
        final double[] featureVector = featureVectorGenerator.generateFeatureVector(config);
        actionClassifier.addObservation(Arrays.copyOf(featureVector, featureVector.length), action[0]);
        if (action[0].equals(reduceUpHat)) {
//			final Features catFeats = extract(config);
            catClassifier.addObservation(Arrays.copyOf(featureVector, featureVector.length), action[1]);
        } else if (action[0].equals(reduceToHat) || action[0].equals(reduceFromHat)) {
            int fellowIndex = Integer.parseInt(action[1]);
            final int compressedFellowIndex = HatParser.actionToCompression(config, action[0], fellowIndex);
//			final Features fellowFeats = extract(config);
//			fellowFeats.putString("action", action[0]);

            fellowClassifier.addObservation(Arrays.copyOf(featureVector, featureVector.length), compressedFellowIndex);
        }
    }

    protected double[] extract(final HatConfig config) {
//		final Features feats = super.extract(config);
//		extractHatCats(feats, config);
//		extractHatPoss(feats, config);
//		extractHatLeftCats(feats, config);
//		extractHatRightCats(feats, config);
//		extractHatLeftIndex(feats, config);
//		extractHatRightIndex(feats, config);
//		extractHatForms(feats, config);
//		if (!suppressCompression) {
//			extractCompressionLeft(feats, config);
//			extractCompressionRight(feats, config);
//		}
//		return feats;
        return	featureVectorGenerator.generateFeatureVector(config);
    }

//	protected void completeHatClassifiers(ConstTreebank treebank) {
//		if (continuousTraining) {
//			hatCompleteClassifier(actionClassifier, treebank);
//			simpleCompleteClassifier(fellowClassifier, treebank);
//			makeAllAction(fellowClassifier);
//			hatCompleteClassifier(fellowClassifier, treebank);
//			makeAllFellowResponses(fellowClassifier, treebank);
//			hatCompleteClassifier(catClassifier, treebank);
//		}
//	}
//	protected void hatCompleteClassifier(final Classifier classifier,
//										 final ConstTreebank treebank) {
//		makeAllHatCats(classifier, treebank);
//		makeAllHatPoss(classifier, treebank);
//		makeAllHatLeftCats(classifier, treebank);
//		makeAllHatRightCats(classifier, treebank);
//		makeAllHatLeftIndex(classifier);
//		makeAllHatRightIndex(classifier);
//		makeAllHatForms(classifier);
//		if (!suppressCompression) {
//			makeAllLeftCompression(classifier, treebank);
//			makeAllRightCompression(classifier, treebank);
//		}
//	}

    public void startValidating() {
        super.startValidating();
        fellowClassifier.startValidating();
    }

    public void stopValidating() {
        super.stopValidating();
        fellowClassifier.stopValidating();
    }

    public boolean isTraining() {
        return super.isTraining() || fellowClassifier.isTraining();
    }

    @Override
    public Iterator<String[]> predict(final HatConfig config) {
//		final Features actionFeats = extract(config);
        final double[] featureVector = featureVectorGenerator.generateFeatureVector(config);
        final String[] acs = (String[]) actionClassifier.predictAll(featureVector);
        return new ActionIterator(config, acs);
    }

    private class ActionIterator implements Iterator<String[]> {
        private final HatConfig config;
        private final LinkedList<String> acs;
        private String ac = null;
        private LinkedList<Integer> fellows;
        private String[] action = null;
        public ActionIterator(final HatConfig config, String[] acs) {
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
                    final double[] catFeats = extract(config);
                    final String cat = (String) catClassifier.predict(catFeats);
                    action = new String[]{ac, cat};
                    ac = null;
                    return true;
                } else {
                    final double[] fellowFeats = extract(config);
                    final Integer[] fs = (Integer[]) fellowClassifier.predictAll(fellowFeats);
                    fellows = new LinkedList<>(Arrays.asList(fs));
                }
            }
            if (fellows.isEmpty()) {
                ac = null;
                return hasNext();
            }
            final int fellow = fellows.removeFirst();
            action = new String[]{ac, Integer.toString(fellow)};
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

//	protected void extractHatCats(final Features feats, final HatConfig config) {
//		for (int i : featureVectorGenerator.getIntsFeature("hatCats")) {
//			String cat = null;
//			final int abs = config.getHatAbsoluteIndex(i);
//			if (0 <= abs && abs < config.stackLength()) {
//				cat = config.getStackLeft(abs).getCat();
//			}
//			feats.putString(hatCatFeature(i), cat);
//		}
//	}
//	protected void makeAllHatCats(final Classifier classifier,
//								  final ConstTreebank treebank) {
//		for (int i : featureVectorGenerator.getIntsFeature("hatCats")) {
//			for (String cat : treebank.getPossAndCats())
//				classifier.addStringValue(hatCatFeature(i), cat);
//			classifier.addStringValue(hatCatFeature(i), null);
//		}
//	}

//	protected String hatPosFeature(int i) {
//		return "hatPos_" + i;
//	}
//	protected void extractHatPoss(final Features feats, final HatConfig config) {
//		for (int i : featureVectorGenerator.getIntsFeature("hatPoss")) {
//			String pos = null;
//			final int abs = config.getHatAbsoluteIndex(i);
//			if (0 <= abs && abs < config.stackLength()) {
//				final ConstLeaf leaf = config.getStackLeft(abs).getHeadLeaf();
//				if (leaf != null)
//					pos = leaf.getCat();
//			}
//			feats.putString(hatPosFeature(i), pos);
//		}
//	}
//	protected void makeAllHatPoss(final Classifier classifier,
//								  final ConstTreebank treebank) {
//		for (int i : featureVectorGenerator.getIntsFeature("hatPoss")) {
//			for (String pos : treebank.getPoss())
//				classifier.addStringValue(hatPosFeature(i), pos);
//			classifier.addStringValue(hatPosFeature(i), null);
//		}
//	}

//	protected String hatFormFeature(final int i) {
//		return "hatForm_" + i;
//	}
//	protected void extractHatForms(final Features feats,
//									 final HatConfig config) {
//		for (int i : featureVectorGenerator.getIntsFeature("hatForms")) {
//			double[] vec = featureVectorGenerator.getFormVec().get();
//			final int abs = config.getHatAbsoluteIndex(i);
//			if (0 <= abs && abs < config.stackLength()) {
//				final ConstLeaf leaf = config.getStackLeft(abs).getHeadLeaf();
//				if (leaf != null)
//					vec = featureVectorGenerator.getFormVec().get(leaf.getForm());
//			}
//			feats.putVector(hatFormFeature(i), vec);
//		}
//	}
//	protected void makeAllHatForms(final Classifier classifier) {
//		int len = featureVectorGenerator.getFormVec().getLength();
//		for (int i : featureVectorGenerator.getIntsFeature("hatForms")) {
//			classifier.setVectorLength(hatFormFeature(i), len);
//		}
//	}

//	protected String hatLeftCatFeature(int i) {
//		return "hatLeftCat_" + i;
//	}
//	protected void extractHatLeftCats(final Features feats, final HatConfig config) {
//		for (int i : featureVectorGenerator.getIntsFeature("hatLeftCats")) {
//			String cat = null;
//			final int abs = config.getHatAbsoluteIndex(i);
//			if (0 <= abs && abs < config.stackLength()) {
//				cat = getLeftmostCat(config.getStackLeft(abs));
//			}
//			feats.putString(hatLeftCatFeature(i), cat);
//		}
//	}
//	protected void makeAllHatLeftCats(final Classifier classifier,
//								  final ConstTreebank treebank) {
//		for (int i : featureVectorGenerator.getIntsFeature("hatLeftCats")) {
//			for (String cat : treebank.getPossAndCats())
//				classifier.addStringValue(hatLeftCatFeature(i), cat);
//			classifier.addStringValue(hatLeftCatFeature(i), null);
//		}
//	}

//	protected String hatRightCatFeature(int i) {
//		return "hatRightCat_" + i;
//	}
//	protected void extractHatRightCats(final Features feats, final HatConfig config) {
//		for (int i : featureVectorGenerator.getIntsFeature("hatRightCats")) {
//			String cat = null;
//			final int abs = config.getHatAbsoluteIndex(i);
//			if (0 <= abs && abs < config.stackLength()) {
//				cat = getRightmostCat(config.getStackLeft(abs));
//			}
//			feats.putString(hatRightCatFeature(i), cat);
//		}
//	}
//	protected void makeAllHatRightCats(final Classifier classifier,
//								  final ConstTreebank treebank) {
//		for (int i : featureVectorGenerator.getIntsFeature("hatRightCats")) {
//			for (String cat : treebank.getPossAndCats())
//				classifier.addStringValue(hatRightCatFeature(i), cat);
//			classifier.addStringValue(hatRightCatFeature(i), null);
//		}
//	}

//	protected String hatLeftIndexFeature() {
//		return "hatLeftIndex";
//	}
//	protected void extractHatLeftIndex(final Features feats, final HatConfig config) {
//		final int hatLeftCap = featureVectorGenerator.getIntFeature("hatLeftCap", 0);
//		final int cappedIndex = Math.min(config.getAbsoluteHatIndex(), hatLeftCap);
//		feats.putString(hatLeftIndexFeature(), "" + cappedIndex);
//	}
//	protected void makeAllHatLeftIndex(final Classifier classifier) {
//		final int hatLeftCap = featureVectorGenerator.getIntFeature("hatLeftCap", 0);
//		for (int i = 0; i <= hatLeftCap; i++)
//			classifier.addStringValue(hatLeftIndexFeature(), "" + i);
//	}

//	protected String hatRightIndexFeature() {
//		return "hatRightIndex";
//	}
//	protected void extractHatRightIndex(final Features feats, final HatConfig config) {
//		final int hatRightCap = featureVectorGenerator.getIntFeature("hatRightCap", 0);
//		final int cappedIndex = Math.min(config.stackLength() - 1 - config.getAbsoluteHatIndex(), hatRightCap);
//		feats.putString(hatRightIndexFeature(), "" + cappedIndex);
//	}
//	protected void makeAllHatRightIndex(final Classifier classifier) {
//		final int hatRightCap = featureVectorGenerator.getIntFeature("hatRightCap", 0);
//		for (int i = 0; i <= hatRightCap; i++)
//			classifier.addStringValue(hatRightIndexFeature(), "" + i);
//	}

//	protected String leftCompressionFeature() {
//		return "leftCompression";
//	}
//	protected void extractCompressionLeft(final Features feats, final HatConfig config) {
//		TreeSet<String> cats = new TreeSet<>();
//		final int viewMin = featureVectorGenerator.getIntFeature("viewMin", 0);
//		for (int i = 0; i < config.getAbsoluteHatIndex() + viewMin; i++) {
//			final String cat = config.getStackLeft(i).getCat();
//			if (cat != null)
//				cats.add(cat);
//		}
//		feats.putSet(leftCompressionFeature(), cats);
//	}
//	protected void makeAllLeftCompression(final Classifier classifier,
//									final ConstTreebank treebank) {
//		classifier.addSetValues(leftCompressionFeature(), treebank.getPossAndCats());
//	}

//	protected String rightCompressionFeature() {
//		return "rightCompression";
//	}
//	protected void extractCompressionRight(final Features feats, final HatConfig config) {
//		TreeSet<String> cats = new TreeSet<>();
//		final int viewMax = featureVectorGenerator.getIntFeature("viewMax", 0);
//		for (int i = config.stackLength() - 1; i > config.getAbsoluteHatIndex() + viewMax; i--) {
//			final String cat = config.getStackLeft(i).getCat();
//			if (cat != null)
//				cats.add(cat);
//		}
//		feats.putSet(rightCompressionFeature(), cats);
//	}
//	protected void makeAllRightCompression(final Classifier classifier,
//									final ConstTreebank treebank) {
//		classifier.addSetValues(rightCompressionFeature(), treebank.getPossAndCats());
//	}

//	protected void makeAllActionResponses(final Classifier classifier) {
//		for (String actionName : HatParser.actionNames)
//			classifier.addResponseValue(actionName);
//	}
//
//	protected void makeAllAction(final Classifier classifier) {
//		for (String actionName : HatParser.actionNames) {
//			classifier.addStringValue("action", actionName);
//		}
//	}

//	protected void makeAllFellowResponses(final Classifier classifier,
//									final ConstTreebank treebank) {
//		final int viewMin = featureVectorGenerator.getIntFeature("viewMin", 0);
//		final int viewMax = featureVectorGenerator.getIntFeature("viewMax", 0);
//		for (int i = viewMin; i <= viewMax; i++) {
//			if (i != 0) {
//				classifier.addResponseValue("" + i);
//			}
//		}
//		for (String cat : treebank.getPossAndCats()) {
//			classifier.addResponseValue(HatParser.compressionLeft(cat));
//			classifier.addResponseValue(HatParser.compressionRight(cat));
//		}
//		classifier.addResponseValue(HatParser.compressionLeft(null));
//		classifier.addResponseValue(HatParser.compressionRight(null));
//	}

}
