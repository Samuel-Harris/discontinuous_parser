/*
 * Copyright (c) 2019. University of St Andrews
 */

package standrews.constbase;

import javafx.util.Pair;

import java.util.*;
import java.util.logging.Logger;

public class ConstTreebank {

    protected String id;
    protected Set<String> poss;
    protected Set<String> cats;
    protected Set<String> labels;
    protected ConstTree[] trees;
    protected List<String> sentenceIds;
    protected List<String> trainSetIds;
    protected List<String> validationSetIds;
    protected List<String> testSetIds;
    protected HashMap<String, SentenceEmbeddingsMetadata> sentenceIdEmbeddingMap;
    protected HashMap<String, ConstTree> sentenceIdTreeMap;
    protected TreebankIterator trainTreebankIterator;
    protected TreebankIterator validationTreebankIterator;
    protected TreebankIterator testTreebankIterator;

    public ConstTreebank(String id,
                         Set<String> poss, Set<String> cats, Set<String> labels,
                         ConstTree[] trees) {
        this.id = id;
        this.poss = poss;
        this.cats = cats;
        this.labels = labels;
        this.trees = trees;

        sentenceIds = new ArrayList<>(trees.length);
        for (ConstTree tree : trees) {
            sentenceIds.add(tree.id);
        }
    }

    public ConstTreebank() {
        this("", new TreeSet<>(), new TreeSet<>(), new TreeSet<>(), new ConstTree[0]);
    }

    public String getId() {
        return id;
    }

    public Set<String> getPoss() {
        return poss;
    }

    public Set<String> getCats() {
        return cats;
    }

    public Set<String> getPossAndCats() {
        Set<String> all = new TreeSet<String>(poss);
        poss.addAll(cats);
        return all;
    }

    public void setupTreebankIterator(Random rng, int miniBatchSize, double trainRatio, double validationRatio, int queueSize) throws ArithmeticException {
        if (trainRatio + validationRatio >= 1.0) {
            throw new ArithmeticException("Error: trainRatio and validationRatio add up to more than 1 or more");
        }

        // shuffle and split data into train, validation, and test data
        sentenceIds = new ArrayList<>(sentenceIdTreeMap.keySet());
        Collections.shuffle(sentenceIds, rng);
        int trainSize = (int) (trainRatio * sentenceIds.size());
        int validationSize = (int) (validationRatio * sentenceIds.size());
        trainSetIds = sentenceIds.subList(0, trainSize);
        validationSetIds = sentenceIds.subList(trainSize, trainSize + validationSize);
        testSetIds = sentenceIds.subList(trainSize + validationSize, sentenceIds.size());

        // set up train and test treebank iterators
        trainTreebankIterator = new TreebankIterator(trainSetIds, sentenceIdEmbeddingMap, miniBatchSize, queueSize, rng);
        validationTreebankIterator = new TreebankIterator(validationSetIds, sentenceIdEmbeddingMap, miniBatchSize, queueSize, rng);
        testTreebankIterator = new TreebankIterator(testSetIds, sentenceIdEmbeddingMap, miniBatchSize, queueSize, rng);
    }

    public void resetTreebankIterator(DatasetSplit datasetSplit) {
        switch (datasetSplit) {
            case TRAIN:
                trainTreebankIterator.reset();
                break;
            case VALIDATION:
                validationTreebankIterator.reset();
                break;
            case TEST:
                testTreebankIterator.reset();
                break;
        }
    }

    public Optional<Pair<List<ConstTree>, List<double[][]>>> getNextMiniBatch(DatasetSplit datasetSplit) {
        TreebankIterator treebankIterator;
        switch (datasetSplit) {
            case TRAIN:
                treebankIterator = trainTreebankIterator;
                break;
            case VALIDATION:
                treebankIterator = validationTreebankIterator;
                break;
            case TEST:
                treebankIterator = testTreebankIterator;
                break;
            default:
                return Optional.empty();
        }

        Optional<Pair<List<String>, List<double[][]>>> idsAndEmbeddingsOptional = treebankIterator.next();
        if (idsAndEmbeddingsOptional.isEmpty()) {
            return Optional.empty();
        } else {
            Pair<List<String>, List<double[][]>> idsAndEmbeddings = idsAndEmbeddingsOptional.get();
            List<ConstTree> miniBatchTrees = new ArrayList<>(idsAndEmbeddings.getKey().size());
            for (String sentenceId: idsAndEmbeddings.getKey()){
                miniBatchTrees.add(sentenceIdTreeMap.get(sentenceId));
            }
            return Optional.of(new Pair<>(miniBatchTrees, idsAndEmbeddings.getValue()));
        }
    }

    public Optional<Pair<List<ConstTree>, List<double[][]>>> getNextTestMiniBatch() {
        return getNextMiniBatch(testTreebankIterator);
    }

    private Optional<Pair<List<ConstTree>, List<double[][]>>> getNextMiniBatch(TreebankIterator treebankIterator) {
        Optional<Pair<List<String>, List<double[][]>>> idsAndEmbeddingsOptional = treebankIterator.next();
        if (idsAndEmbeddingsOptional.isEmpty()) {
            return Optional.empty();
        } else {
            Pair<List<String>, List<double[][]>> idsAndEmbeddings = idsAndEmbeddingsOptional.get();
            List<ConstTree> miniBatchTrees = new ArrayList<>(idsAndEmbeddings.getKey().size());
            for (String sentenceId: idsAndEmbeddings.getKey()){
                miniBatchTrees.add(sentenceIdTreeMap.get(sentenceId));
            }
            return Optional.of(new Pair<>(miniBatchTrees, idsAndEmbeddings.getValue()));
        }
    }

    public NegraTreebank getTestNegraTreebank() {
        ConstTree[] testTrees = new ConstTree[testSetIds.size()];
        for (int i = 0; i < testSetIds.size(); i++) {
            testTrees[i] = sentenceIdTreeMap.get(testSetIds.get(i));
        }
        return new NegraTreebank("", poss, cats, labels, testTrees);
    }

    public SentenceEmbeddingsMetadata getSentenceEmbeddingsMetadata(String sentenceId) {
        return sentenceIdEmbeddingMap.get(sentenceId);
    }

    public Set<String> getLabels() {
        return labels;
    }

    public ConstTree[] getTrees() {
        return trees;
    }

    public int nTrees() {
        return trees.length;
    }

    public int getSetSize(DatasetSplit datasetSplit) {
        switch (datasetSplit) {
            case TRAIN:
                return trainSetIds.size();
            case VALIDATION:
                return validationSetIds.size();
            case TEST:
                return testSetIds.size();
            default:
                System.exit(1);
                return 0;
        }
    }

    public int nWords() {
        int nWords = 0;
        for (ConstTree tree : trees) {
            nWords += tree.length();
        }
        return nWords;
    }

    public boolean checkSymbolConsistency() {
        for (ConstTree tree : trees)
            if (!tree.isSymbolConsistent(poss, cats, labels))
                return false;
        return true;
    }

    public void checkCycles() {
        for (ConstTree tree : trees)
            if (tree.hasCycle())
                System.out.println(tree.id);
    }

    public void removeCycles() {
        for (ConstTree tree : trees) {
            while (tree.removeOneCycle()) {
            }
        }
    }

    public void gatherSymbols() {
        for (ConstTree tree : trees)
            tree.gatherSymbols(poss, cats, labels);
    }

    public ConstTreebank part(int from, int to) {
        ConstTree[] partTrees = Arrays.asList(trees).subList(from, to)
                .toArray(new ConstTree[0]);
        return new ConstTreebank(id, poss, cats, labels, partTrees);
    }

    private Logger logger() {
        final Logger log = Logger.getLogger(getClass().getName());
        log.setParent(Logger.getGlobal());
        return log;
    }

}
