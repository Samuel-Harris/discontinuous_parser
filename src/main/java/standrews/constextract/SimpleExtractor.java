/*
 * Copyright (c) 2019. University of St Andrews
 */

package standrews.constextract;

import standrews.classification.ActionResponseVectorGenerator;
import standrews.classification.CatResponseVectorGenerator;
import standrews.classification.FeatureVectorGenerator;
import standrews.classification.MLP;
import standrews.classification.MLPFactory;
import standrews.constautomata.HatConfig;
import standrews.constautomata.SimpleConfig;
import standrews.constbase.*;
import standrews.constmethods.SimpleParser;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Optional;

public class SimpleExtractor {
    public static final String shift = SimpleParser.shift;
    public static final String reduceUp = SimpleParser.reduceUp;
    public static final String reduceLeft = SimpleParser.reduceLeft;
    public static final String reduceRight = SimpleParser.reduceRight;

    /**
     * Specification uses:
     * stackPoss: From which stack elements is POS to be feature.
     * inputPoss: From which input elements is POS to be feature.
     * leftCat: From which stack elements is the leftmost child cat to be feature.
     * rightCat: From which stack elements is the rightmost child cat to be feature.
     */
    public FeatureVectorGenerator featureVectorGenerator;

    /**
     * Factory for making classifiers.
     */
    public MLPFactory mlpFactory;

    /**
     * Continuous training of classifiers.
     */
    protected boolean continuousTraining;

    /**
     * Batch size of classifiers.
     */
    protected int batchSize;

    /**
     * Number of epochs of classifiers.
     */
    protected int nEpochs;

    /**
     * Classifier for actions.
     */
    public MLP actionClassifier;

    /**
     * Classifier for categories.
     */
    public MLP catClassifier;

    protected SimpleExtractor() {
        // only used by subclasses
    }

    public SimpleExtractor(final ConstTreebank treebank,
                           final FeatureVectorGenerator featureVectorGenerator,
                           final MLPFactory mlpFactory,
                           final String actionFile, final String catFile) {
        this.featureVectorGenerator = featureVectorGenerator;
        this.mlpFactory = mlpFactory;
        this.actionClassifier = mlpFactory.makeMLP(new ActionResponseVectorGenerator());
        this.catClassifier = mlpFactory.makeMLP(new CatResponseVectorGenerator(featureVectorGenerator.getCatIndexMap()));
//		completeClassifiers(treebank);
    }

//    public boolean getContinuousTraining() {
//        return continuousTraining;
//    }

    public int getBatchSize() {
        return batchSize;
    }

    public int getNEpochs() {
        return nEpochs;
    }

    public void train() {
        actionClassifier.train();
        catClassifier.train();
    }

    public void extract(final HatConfig config, final String[] action) {
//		final Features actionFeats = extract(config);
        final double[] featureVector = featureVectorGenerator.generateFeatureVector(config);  // ***********fix this with hat symbol
        actionClassifier.addObservation(Arrays.copyOf(featureVector, featureVector.length), action[0]);
        if (action.length > 1) {
//			final Features catFeats = extract(config);
            catClassifier.addObservation(Arrays.copyOf(featureVector, featureVector.length), action[1]);
        }
    }

//	protected Features extract(final SimpleConfig config) {
//		final Features feats = new Features();
//		extractInputPoss(feats, config);
//		extractInputForms(feats, config);
//		extractStackCats(feats, config);
//		extractStackPoss(feats, config);
//		extractStackForms(feats, config);
//		extractLeftCats(feats, config);
//		extractRightCats(feats, config);
//		return feats;
//	}

//	protected void completeClassifiers(ConstTreebank treebank) {
//		if (continuousTraining) {
//			simpleCompleteClassifier(actionClassifier, treebank);
//			makeAllActionResponses(actionClassifier);
//			simpleCompleteClassifier(catClassifier, treebank);
//			makeAllCatResponses(catClassifier, treebank);
//		}
//	}
//	protected void simpleCompleteClassifier(Classifier classifier, ConstTreebank treebank) {
//		makeAllInputPoss(classifier, treebank);
//		makeAllInputForms(classifier);
//		makeAllStackCats(classifier, treebank);
//		makeAllStackPoss(classifier, treebank);
//		makeAllStackForms(classifier);
//		makeAllLeftCats(classifier, treebank);
//		makeAllRightCats(classifier, treebank);
//	}

    public Iterator<String[]> predict(final HatConfig config) {
//		final Features actionFeats = extract(config);
        final double[] featureVector = featureVectorGenerator.generateFeatureVector(config);  // ***********fix this with hat symbol
        final String[] acs = (String[]) actionClassifier.predictAll(featureVector);
        return new ActionIterator(config, acs);
    }

    private class ActionIterator implements Iterator<String[]> {
        private final HatConfig config;
        private final LinkedList<String> acs;

        public ActionIterator(final HatConfig config, String[] acs) {
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
            final String ac = acs.removeFirst();
            if (!ac.equals(reduceUp)) {
                return new String[]{ac};
            } else {
//				final Features catFeats = extract(config);
                final double[] featureVector = featureVectorGenerator.generateFeatureVector(config);  // ***********fix this with hat symbol
                final String cat = (String) catClassifier.predict(featureVector);
                return new String[]{ac, cat};
            }
        }
    }

//	protected String inputPosFeature(int i) {
//		return "inputPos_" + i;
//	}
//	protected void extractInputPoss(final Features feats,
//									final SimpleConfig config) {
//		for (int i : featureVectorGenerator.getIntsFeature("inputPoss")) {
//			String pos = null;
//			if (i < config.inputLength())
//				pos = config.getInputLeft(i).getCat();
//			feats.putString(inputPosFeature(i), pos);
//		}
//	}
//	protected void makeAllInputPoss(final Classifier classifier,
//									final ConstTreebank treebank) {
//		for (int i : featureVectorGenerator.getIntsFeature("inputPoss")) {
//			for (String pos : treebank.getPoss())
//				classifier.addStringValue(inputPosFeature(i), pos);
//			classifier.addStringValue(inputPosFeature(i), null);
//		}
//	}
//
//	protected String inputFormFeature(final int i) {
//		return "inputForm_" + i;
//	}
//	protected void extractInputForms(final Features feats,
//									 final SimpleConfig config) {
//		for (int i : featureVectorGenerator.getIntsFeature("inputForms")) {
//			double[] vec = featureVectorGenerator.getFormVec().get();
//			if (i < config.inputLength()) {
//				final String form = config.getInputLeft(i).getForm();
//				vec = featureVectorGenerator.getFormVec().get(form);
//			}
//			feats.putVector(inputFormFeature(i), vec);
//		}
//	}
//	protected void makeAllInputForms(final Classifier classifier) {
//		int len = featureVectorGenerator.getFormVec().getLength();
//		for (int i : featureVectorGenerator.getIntsFeature("inputForms")) {
//			classifier.setVectorLength(inputFormFeature(i), len);
//		}
//	}
//
//	protected String stackCatFeature(final int i) {
//		return "stackCat_" + i;
//	}
//	protected void extractStackCats(final Features feats,
//									final SimpleConfig config) {
//		for (int i : featureVectorGenerator.getIntsFeature("stackCats")) {
//			String cat = null;
//			if (i < config.stackLength())
//				cat = config.getStackRight(i).getCat();
//			feats.putString(stackCatFeature(i), cat);
//		}
//	}
//	protected void makeAllStackCats(final Classifier classifier,
//									final ConstTreebank treebank) {
//		for (int i : featureVectorGenerator.getIntsFeature("stackCats")) {
//			for (String cat : treebank.getPossAndCats())
//				classifier.addStringValue(stackCatFeature(i), cat);
//			classifier.addStringValue(stackCatFeature(i), null);
//		}
//	}
//
//	protected String stackPosFeature(final int i) {
//		return "stackPos_" + i;
//	}
//	protected void extractStackPoss(final Features feats,
//									final SimpleConfig config) {
//		for (int i : featureVectorGenerator.getIntsFeature("stackPoss")) {
//			String pos = null;
//			if (i < config.stackLength()) {
//				ConstLeaf leaf = config.getStackRight(i).getHeadLeaf();
//				if (leaf != null)
//					pos = leaf.getCat();
//			}
//			feats.putString(stackPosFeature(i), pos);
//		}
//	}
//	protected void makeAllStackPoss(final Classifier classifier,
//									final ConstTreebank treebank) {
//		for (int i : featureVectorGenerator.getIntsFeature("stackPoss")) {
//			for (String pos : treebank.getPoss())
//				classifier.addStringValue(stackPosFeature(i), pos);
//			classifier.addStringValue(stackPosFeature(i), null);
//		}
//	}
//
//	protected String stackFormFeature(final int i) {
//		return "stackForm_" + i;
//	}
//	protected void extractStackForms(final Features feats,
//									 final SimpleConfig config) {
//		for (int i : featureVectorGenerator.getIntsFeature("stackForms")) {
//			double[] vec = featureVectorGenerator.getFormVec().get();
//			if (i < config.stackLength()) {
//				final ConstLeaf leaf = config.getStackRight(i).getHeadLeaf();
//				if (leaf != null)
//					vec = featureVectorGenerator.getFormVec().get(leaf.getForm());
//			}
//			feats.putVector(stackFormFeature(i), vec);
//		}
//	}
//	protected void makeAllStackForms(final Classifier classifier) {
//		int len = featureVectorGenerator.getFormVec().getLength();
//		for (int i : featureVectorGenerator.getIntsFeature("stackForms")) {
//			classifier.setVectorLength(stackFormFeature(i), len);
//		}
//	}
//
//	protected String leftCatFeature(int i) {
//		return "leftCat_" + i;
//	}
//	protected void extractLeftCats(final Features feats,
//								   final SimpleConfig config) {
//		for (int i : featureVectorGenerator.getIntsFeature("leftCats")) {
//			String cat = null;
//			if (i < config.stackLength())
//				cat = getLeftmostCat(config.getStackRight(i));
//			feats.putString(leftCatFeature(i), cat);
//		}
//	}
//	protected String getLeftmostCat(ConstNode node) {
//		if (node instanceof ConstInternal) {
//			ConstInternal internal = (ConstInternal) node;
//			ConstNode[] children = internal.getChildren();
//			if (internal.getHeadIndex() > 0)
//				return children[0].getCat();
//		}
//		return null;
//	}
//	protected void makeAllLeftCats(final Classifier classifier,
//									final ConstTreebank treebank) {
//		for (int i : featureVectorGenerator.getIntsFeature("leftCats")) {
//			for (String cat : treebank.getPossAndCats())
//				classifier.addStringValue(leftCatFeature(i), cat);
//			classifier.addStringValue(leftCatFeature(i), null);
//		}
//	}
//
//	public String rightCatFeature(int i) {
//		return "rightCat_" + i;
//	}
//	protected void extractRightCats(final Features feats,
//									final SimpleConfig config) {
//		for (int i : featureVectorGenerator.getIntsFeature("rightCats")) {
//			String cat = null;
//			if (i < config.stackLength())
//				cat = getRightmostCat(config.getStackRight(i));
//			feats.putString(rightCatFeature(i), cat);
//		}
//	}
//	protected String getRightmostCat(ConstNode node) {
//		if (node instanceof ConstInternal) {
//			ConstInternal internal = (ConstInternal) node;
//			ConstNode[] children = internal.getChildren();
//			if (internal.getHeadIndex() < children.length - 1)
//				return children[children.length - 1].getCat();
//		}
//		return null;
//	}
//	protected void makeAllRightCats(final Classifier classifier,
//									final ConstTreebank treebank) {
//		for (int i : featureVectorGenerator.getIntsFeature("rightCats")) {
//			for (String cat : treebank.getPossAndCats())
//				classifier.addStringValue(rightCatFeature(i), cat);
//			classifier.addStringValue(rightCatFeature(i), null);
//		}
//	}
//
//	protected void makeAllActionResponses(final Classifier classifier) {
//		for (String actionName : SimpleParser.actionNames)
//			classifier.addResponseValue(actionName);
//	}
//
//	protected void makeAllCatResponses(final Classifier classifier,
//									   final ConstTreebank treebank) {
//		for (String cat : treebank.getCats())
//			classifier.addResponseValue(cat);
//	}
}
