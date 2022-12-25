/*
 * Copyright (c) 2019. University of St Andrews
 */

package standrews.constbase;

import com.codepoetics.protonpack.StreamUtils;
import javafx.util.Pair;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ConstTreebank {

    protected String id;
    protected Set<String> poss;
    protected Set<String> cats;
    protected Set<String> labels;
    protected ConstTree[] trees;

    protected List<MinibatchMetadata> trainMiniBatchMetadataList;
    protected List<MinibatchMetadata> validationMiniBatchMetadataList;
    protected List<MinibatchMetadata> testMiniBatchMetadataList;

    protected HashMap<String, ConstTree> sentenceIdTreeMap;
    protected List<MinibatchMetadata> miniBatchMetadataList;

    protected TreebankIterator trainTreebankIterator;
    protected TreebankIterator validationTreebankIterator;
    protected TreebankIterator testTreebankIterator;

    protected int nTrain;
    protected int nValid;
    protected int nTest;

    public ConstTreebank(String id,
                         Set<String> poss, Set<String> cats, Set<String> labels,
                         ConstTree[] trees) {
        this.id = id;
        this.poss = poss;
        this.cats = cats;
        this.labels = labels;
        this.trees = trees;
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

    public void setupTreebankIterator(Random rng, double trainRatio, double validationRatio, int queueSize) throws ArithmeticException {
        if (trainRatio + validationRatio >= 1.0) {
            throw new ArithmeticException("Error: trainRatio and validationRatio add up to more than 1 or more");
        }

        // shuffle and split data into train, validation, and test data
        Collections.shuffle(miniBatchMetadataList, rng);
        int trainSize = (int) (trainRatio * miniBatchMetadataList.size());
        int validationSize = (int) Math.ceil(validationRatio * miniBatchMetadataList.size());
        trainMiniBatchMetadataList = miniBatchMetadataList.subList(0, trainSize);
        validationMiniBatchMetadataList = miniBatchMetadataList.subList(trainSize, trainSize + validationSize);
        testMiniBatchMetadataList = miniBatchMetadataList.subList(trainSize + validationSize, miniBatchMetadataList.size());

        // finding sizes of datasets
        nTrain = trainMiniBatchMetadataList.stream()
                .mapToInt(MinibatchMetadata::getSize)
                .sum();
        nValid = validationMiniBatchMetadataList.stream()
                .mapToInt(MinibatchMetadata::getSize)
                .sum();
        nTest = testMiniBatchMetadataList.stream()
                .mapToInt(MinibatchMetadata::getSize)
                .sum();

        // set up train and test treebank iterators
        trainTreebankIterator = new TreebankIterator(miniBatchMetadataList, queueSize, rng);
        validationTreebankIterator = new TreebankIterator(miniBatchMetadataList, queueSize, rng);
        testTreebankIterator = new TreebankIterator(miniBatchMetadataList, queueSize, rng);
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

    public Optional<List<Pair<ConstTree, double[][]>>> getNextMiniBatch(DatasetSplit datasetSplit) {
        switch (datasetSplit) {
            case TRAIN:
                return getNextMiniBatch(trainTreebankIterator);
            case VALIDATION:
                return getNextMiniBatch(validationTreebankIterator);
            case TEST:
                return getNextMiniBatch(testTreebankIterator);
            default:
                return Optional.empty();
        }
    }

    private Optional<List<Pair<ConstTree, double[][]>>> getNextMiniBatch(TreebankIterator treebankIterator) {
        Optional<List<Pair<String, double[][]>>> idsAndEmbeddingsOptional = treebankIterator.next();
        if (idsAndEmbeddingsOptional.isEmpty()) {
            return Optional.empty();
        } else {
            List<Pair<String, double[][]>> idsAndEmbeddings = idsAndEmbeddingsOptional.get();
            List<ConstTree> miniBatchTrees = new ArrayList<>(idsAndEmbeddings.size());
            for (Pair<String, double[][]> sentenceIdAndEmbedding: idsAndEmbeddings){
                miniBatchTrees.add(sentenceIdTreeMap.get(sentenceIdAndEmbedding.getKey()));
            }

            return Optional.of(IntStream.range(0, idsAndEmbeddings.size())
                    .mapToObj(i -> new Pair<>(miniBatchTrees.get(i), idsAndEmbeddings.get(i).getValue()))
                    .collect(Collectors.toList())
            );
        }
    }

    public NegraTreebank getTestNegraTreebank() {
        List<ConstTree> testTrees = new ArrayList<>();

        testMiniBatchMetadataList.forEach(minibatchMetadata ->
                testTrees.addAll(minibatchMetadata.getSentenceIds()
                        .stream()
                        .map(sentenceId -> sentenceIdTreeMap.get(sentenceId))
                        .collect(Collectors.toList())));

        return new NegraTreebank("", poss, cats, labels, testTrees.toArray(new ConstTree[0]));
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
                return nTrain;
            case VALIDATION:
                return nValid;
            case TEST:
                return nTest;
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
