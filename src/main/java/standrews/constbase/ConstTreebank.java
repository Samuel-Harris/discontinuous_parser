/*
 * Copyright (c) 2019. University of St Andrews
 */

package standrews.constbase;

import javafx.util.Pair;
import org.deeplearning4j.nn.layers.feedforward.autoencoder.recursive.Tree;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ConstTreebank {

    protected String id;
    protected Set<String> poss;
    protected Set<String> cats;
    protected Set<String> labels;
    protected ConstTree[] trees;
    protected List<String> sentenceIds;
    protected List<String> trainSetIds;
    protected List<String> testSetIds;
    protected HashMap<String, SentenceEmbeddingsMetadata> sentenceIdEmbeddingMap;
    protected HashMap<String, ConstTree> sentenceIdTreeMap;
    protected TreebankIterator trainTreebankIterator;
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

    public void setupTreebankIterator(Random rng, int batchSize, double trainTestRatio, int queueSize) {
        // shuffle and split data into train and test data
        sentenceIds = new ArrayList<>(sentenceIdTreeMap.keySet());
        Collections.shuffle(sentenceIds, rng);
        int trainSize = (int) (trainTestRatio * sentenceIds.size());
        trainSetIds = sentenceIds.subList(0, trainSize);
        testSetIds = sentenceIds.subList(trainSize, sentenceIds.size());

        // set up train and test treebank iterators
        trainTreebankIterator = new TreebankIterator(trainSetIds, sentenceIdEmbeddingMap, batchSize, queueSize, rng);
        testTreebankIterator = new TreebankIterator(testSetIds, sentenceIdEmbeddingMap, batchSize, queueSize, rng);
    }

    public void resetTrainTreebankIterator() {
        trainTreebankIterator.reset();
    }

    public Optional<Pair<List<ConstTree>, List<double[][]>>> getNextTrainMiniBatch() {
        return getNextMiniBatch(trainTreebankIterator);
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
