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
import standrews.depmethods.SimpleParser;

import java.util.*;

public class SimpleExtractor {
    public static final String shift = SimpleParser.shift;
    public static final String reduceLeft = SimpleParser.reduceLeft;
    public static final String reduceRight = SimpleParser.reduceRight;

    /**
     * Specification uses:
     * prefixPoss: From which prefix elements is POS to be feature.
     * suffixPoss: From which suffix elements is POS to be feature.
     * leftDeprels: From which prefix elements is the leftmost deprel to be feature.
     * rightDeprels: From which prefix elements is the rightmost deprel to be feature.
     */
    public FeatureSpecification featSpec;

    /**
     * Factory for making classifiers.
     */
    public ClassifierFactory factory;

    /**
     * Classifier for actions.
     */
    public Classifier actionClassifier;

    /**
     * Classifier for deprels of left and right children.
     */
    public Classifier deprelLeftClassifier;
    public Classifier deprelRightClassifier;

    /**
     * Use separate classifers for deprels of left and right children?
     */
    protected boolean useTwoDeprelClassifiers = false;

    protected SimpleExtractor(final FeatureSpecification featSpec, final ClassifierFactory factory) {
        this.featSpec = featSpec;
        this.factory = factory;
    }

    public SimpleExtractor(final FeatureSpecification featSpec, final ClassifierFactory factory,
                           final String actionFile,
                           final String deprelLeftFile, final String deprelRightFile) {
        this(featSpec, factory);
        makeClassifiers(actionFile, deprelLeftFile, deprelRightFile);
        completeClassifiers();
    }

    public boolean getContinuousTraining() {
        return factory.getContinuousTraining();
    }

    public int getBatchSize() {
        return factory.getBatchSize();
    }

    public int getNEpochs() {
        return factory.getNEpochs();
    }

    protected void makeClassifiers(final String actionFile, final String deprelLeftFile, final String deprelRightFile) {
        this.actionClassifier = factory.makeClassifier(actionFile);
        this.deprelLeftClassifier = factory.makeClassifier(deprelLeftFile);
        this.deprelRightClassifier = factory.makeClassifier(deprelRightFile);
    }

    public void train() {
        actionClassifier.train();
        deprelLeftClassifier.train();
        if (useTwoDeprelClassifiers)
            deprelRightClassifier.train();
    }

    protected Classifier getDeprelClassifier(final String ac) {
        return useTwoDeprelClassifiers && ac.equals(reduceLeft) ?
                deprelLeftClassifier :
                deprelRightClassifier;
    }

    public void extract(final SimpleConfig config, final String[] action) {
        final Features actionFeats = extract(config);
        final String ac = action[0];
        System.out.println("fix this");
        System.exit(1);
//		actionClassifier.addObservation(actionFeats, ac);
        if (action.length > 1) {
            final Features deprelFeats = extract(config);
            if (!useTwoDeprelClassifiers)
                deprelFeats.putString("action", ac);
//			getDeprelClassifier(ac).addObservation(deprelFeats, action[1]);
        }
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
            final String ac = acs.removeFirst();
            if (ac.equals(shift)) {
                return new String[]{ac};
            } else {
                final Features deprelFeats = extract(config);
                if (!useTwoDeprelClassifiers)
                    deprelFeats.putString("action", ac);
                final String deprel = getDeprelClassifier(ac).predict(deprelFeats);
                return new String[]{ac, deprel};
            }
        }
    }

    public double actionProbability(final SimpleConfig config, String action) {
        final Features actionFeats = extract(config);
        return actionClassifier.probability(actionFeats, action);
    }

    protected void completeClassifiers() {
        if (getContinuousTraining()) {
            completeActionClassifier(actionClassifier);
            completeDeprelClassifier(deprelLeftClassifier);
            completeDeprelClassifier(deprelRightClassifier);
        }
    }

    protected void completeActionClassifier(Classifier classifier) {
        simpleCompleteClassifier(classifier);
        makeAllActionResponses(classifier);
    }

    protected void completeDeprelClassifier(Classifier classifier) {
        simpleCompleteClassifier(classifier);
        if (!useTwoDeprelClassifiers)
            makeAllAction(classifier);
        makeAllDeprelResponses(classifier);
    }

    protected Features extract(final SimpleConfig config) {
        final Features feats = new Features();
        extractPrefixPoss(feats, config);
        extractSuffixPoss(feats, config);
        extractPrefixForms(feats, config);
        extractSuffixForms(feats, config);
        extractPrefixLemmas(feats, config);
        extractSuffixLemmas(feats, config);
        extractPrefixLabels(feats, config);
        extractParentPoss(feats, config);
        extractLeftDeprels(feats, config);
        extractRightDeprels(feats, config);
        return feats;
    }

    protected void simpleCompleteClassifier(Classifier classifier) {
        makeAllPrefixPoss(classifier);
        makeAllSuffixPoss(classifier);
        makeAllPrefixForms(classifier);
        makeAllSuffixForms(classifier);
        makeAllPrefixLemmas(classifier);
        makeAllSuffixLemmas(classifier);
        makeAllPrefixLabels(classifier);
        makeAllParentPoss(classifier);
        makeAllLeftDeprels(classifier);
        makeAllRightDeprels(classifier);
    }

    protected String prefixPosFeature(final int i) {
        return "prefixPos_" + i;
    }

    protected void extractPrefixPoss(final Features feats, final SimpleConfig config) {
        for (int i : featSpec.getIntsFeature("prefixPoss")) {
            String posStr = null;
            if (i < config.prefixLength()) {
                Upos pos = config.getPrefixRight(i).getToken().upos;
                if (pos != null)
                    posStr = pos.toString();
            }
            feats.putString(prefixPosFeature(i), posStr);
        }
    }

    protected void makeAllPrefixPoss(final Classifier classifier) {
        for (int i : featSpec.getIntsFeature("prefixPoss")) {
            for (String pos : Upos.UPOSS)
                classifier.addStringValue(prefixPosFeature(i), pos);
            classifier.addStringValue(prefixPosFeature(i), null);
        }
    }

    protected String suffixPosFeature(int i) {
        return "suffixPos_" + i;
    }

    protected void extractSuffixPoss(final Features feats, final SimpleConfig config) {
        for (int i : featSpec.getIntsFeature("suffixPoss")) {
            String posStr = null;
            if (i < config.suffixLength()) {
                final Upos pos = config.getSuffixLeft(i).getToken().upos;
                if (pos != null)
                    posStr = pos.toString();
            }
            feats.putString(suffixPosFeature(i), posStr);
        }
    }

    protected void makeAllSuffixPoss(final Classifier classifier) {
        for (int i : featSpec.getIntsFeature("suffixPoss")) {
            for (String pos : Upos.UPOSS)
                classifier.addStringValue(suffixPosFeature(i), pos);
            classifier.addStringValue(suffixPosFeature(i), null);
        }
    }

    protected String prefixFormFeature(final int i) {
        return "prefixForm_" + i;
    }

    protected void extractPrefixForms(final Features feats, final SimpleConfig config) {
        for (int i : featSpec.getIntsFeature("prefixForms")) {
            double[] vec = featSpec.getFormVec().get();
            if (i < config.prefixLength()) {
                final Form form = config.getPrefixRight(i).getToken().form;
                if (form != null) {
                    String s = form.toString();
                    vec = featSpec.getFormVec().get(s);
                }
            }
            feats.putVector(prefixFormFeature(i), vec);
        }
    }

    protected void makeAllPrefixForms(final Classifier classifier) {
        int len = featSpec.getFormVec().getLength();
        for (int i : featSpec.getIntsFeature("prefixForms")) {
            classifier.setVectorLength(prefixFormFeature(i), len);
        }
    }

    protected String suffixFormFeature(final int i) {
        return "suffixForm_" + i;
    }

    protected void extractSuffixForms(final Features feats, final SimpleConfig config) {
        for (int i : featSpec.getIntsFeature("suffixForms")) {
            double[] vec = featSpec.getFormVec().get();
            if (i < config.suffixLength()) {
                final Form form = config.getSuffixLeft(i).getToken().form;
                if (form != null) {
                    String s = form.toString();
                    vec = featSpec.getFormVec().get(s);
                }
            }
            feats.putVector(suffixFormFeature(i), vec);
        }
    }

    protected void makeAllSuffixForms(final Classifier classifier) {
        int len = featSpec.getFormVec().getLength();
        for (int i : featSpec.getIntsFeature("suffixForms")) {
            classifier.setVectorLength(suffixFormFeature(i), len);
        }
    }

    protected String prefixLemmaFeature(final int i) {
        return "prefixLemma_" + i;
    }

    protected void extractPrefixLemmas(final Features feats, final SimpleConfig config) {
        for (int i : featSpec.getIntsFeature("prefixLemmas")) {
            double[] vec = featSpec.getLemmaVec().get();
            if (i < config.prefixLength()) {
                final Lemma lemma = config.getPrefixRight(i).getToken().lemma;
                if (lemma != null) {
                    String s = lemma.toString();
                    vec = featSpec.getLemmaVec().get(s);
                }
            }
            feats.putVector(prefixLemmaFeature(i), vec);
        }
    }

    protected void makeAllPrefixLemmas(final Classifier classifier) {
        int len = featSpec.getLemmaVec().getLength();
        for (int i : featSpec.getIntsFeature("prefixLemmas")) {
            classifier.setVectorLength(prefixLemmaFeature(i), len);
        }
    }

    protected String suffixLemmaFeature(final int i) {
        return "suffixLemma_" + i;
    }

    protected void extractSuffixLemmas(final Features feats, final SimpleConfig config) {
        for (int i : featSpec.getIntsFeature("suffixLemmas")) {
            double[] vec = featSpec.getLemmaVec().get();
            if (i < config.suffixLength()) {
                final Lemma lemma = config.getSuffixLeft(i).getToken().lemma;
                if (lemma != null) {
                    String s = lemma.toString();
                    vec = featSpec.getLemmaVec().get(s);
                }
            }
            feats.putVector(suffixLemmaFeature(i), vec);
        }
    }

    protected void makeAllSuffixLemmas(final Classifier classifier) {
        int len = featSpec.getLemmaVec().getLength();
        for (int i : featSpec.getIntsFeature("suffixLemmas")) {
            classifier.setVectorLength(suffixLemmaFeature(i), len);
        }
    }

    protected String prefixLabelFeature(final int i) {
        return "prefixLabel_" + i;
    }

    protected void extractPrefixLabels(final Features feats, final SimpleConfig config) {
        for (int i : featSpec.getIntsFeature("prefixLabels")) {
            String labelStr = null;
            if (i < config.prefixLength()) {
                labelStr = config.getLabelRight(i);
            }
            feats.putString(prefixLabelFeature(i), labelStr);
        }
    }

    protected void makeAllPrefixLabels(final Classifier classifier) {
        for (int i : featSpec.getIntsFeature("prefixLabels"))
            for (String label : prefixLabels())
                classifier.addStringValue(prefixLabelFeature(i), label);
    }

    protected String parentPosFeature(final int i) {
        return "parentPos_" + i;
    }

    protected void extractParentPoss(final Features feats, final SimpleConfig config) {
        for (int i : featSpec.getIntsFeature("parentPoss")) {
            String posStr = null;
            if (i < config.prefixLength()) {
                DependencyVertex parent = config.getPrefixRight(i).getParent();
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

    protected void makeAllParentPoss(final Classifier classifier) {
        for (int i : featSpec.getIntsFeature("parentPoss")) {
            for (String pos : Upos.UPOSS)
                classifier.addStringValue(parentPosFeature(i), pos);
            classifier.addStringValue(parentPosFeature(i), null);
        }
    }

    protected String leftDepFeature(int i) {
        return "leftDep_" + i;
    }

    protected void extractLeftDeprels(final Features feats, final SimpleConfig config) {
        for (int i : featSpec.getIntsFeature("leftDeprels")) {
            String deprelStr = null;
            if (i < config.prefixLength()) {
                final Deprel deprel = config.getPrefixRight(i).getLeftmostDeprel();
                if (deprel != null)
                    deprelStr = deprel.uniPart();
            }
            feats.putString(leftDepFeature(i), deprelStr);
        }
    }

    protected void makeAllLeftDeprels(final Classifier classifier) {
        for (int i : featSpec.getIntsFeature("leftDeprels")) {
            for (String deprel : Deprel.DEPRELS)
                classifier.addStringValue(leftDepFeature(i), deprel);
            classifier.addStringValue(leftDepFeature(i), null);
        }
    }

    public String rightDepFeature(int i) {
        return "rightDep_" + i;
    }

    protected void extractRightDeprels(final Features feats, final SimpleConfig config) {
        for (int i : featSpec.getIntsFeature("rightDeprels")) {
            String deprelStr = null;
            if (i < config.prefixLength()) {
                final Deprel deprel = config.getPrefixRight(i).getRightmostDeprel();
                if (deprel != null)
                    deprelStr = deprel.uniPart();
            }
            feats.putString(rightDepFeature(i), deprelStr);
        }
    }

    protected void makeAllRightDeprels(final Classifier classifier) {
        for (int i : featSpec.getIntsFeature("rightDeprels")) {
            for (String deprel : Deprel.DEPRELS)
                classifier.addStringValue(rightDepFeature(i), deprel);
            classifier.addStringValue(rightDepFeature(i), null);
        }
    }

    protected void makeAllActionResponses(final Classifier classifier) {
        for (String actionName : actionNames())
            classifier.addResponseValue(actionName);
    }

    protected void makeAllAction(final Classifier classifier) {
        for (String actionName : actionNames())
            classifier.addStringValue("action", actionName);
    }

    protected void makeAllDeprelResponses(final Classifier classifier) {
        for (String deprel : Deprel.DEPRELS)
            classifier.addResponseValue(deprel);
    }

    protected String[] actionNames() {
        return SimpleParser.actionNames;
    }

    protected String[] prefixLabels() {
        return SimpleParser.prefixLabels;
    }
}
