/*
 * Copyright (c) 2019. University of St Andrews
 */

package standrews.constmain;

import javafx.util.Pair;
import standrews.classification.FeatureVectorGenerator;
import standrews.classification.MLP;
import standrews.constextract.HatExtractor;
import standrews.constbase.ConstTree;
import standrews.constbase.ConstTreebank;
import standrews.constmethods.HatParser;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class SimpleTrainer {

    protected FeatureVectorGenerator featureVectorGenerator;
    protected final int maxEpochs;
    private final List<Double> actionLossList;
    private final List<Double> catLossList;
    private final List<Double> fellowLossList;
    private final String lossListOutDirectory;

    public SimpleTrainer(final FeatureVectorGenerator featureVectorGenerator, int maxEpochs, String lossListOutDirectory) {
        this.featureVectorGenerator = featureVectorGenerator;
        this.maxEpochs = maxEpochs;
        this.lossListOutDirectory = lossListOutDirectory;

        actionLossList = new ArrayList<>();
        catLossList = new ArrayList<>();
        fellowLossList = new ArrayList<>();
    }

    protected boolean leftDependentsFirst = false;

    protected boolean strict = false;

    protected boolean projectivize = false;

    public void setLeftDependentsFirst(final boolean b) {
        leftDependentsFirst = b;
    }

    public void setStrict(final boolean b) {
        strict = b;
    }

    public void setProjectivize(final boolean p) {
        projectivize = p;
    }

    public void train(final ConstTreebank treebank,
                     final int n, final HatExtractor extractor) {
        train(treebank, null, n, extractor);
    }

    public void train(final ConstTreebank treebank,
                     final String corpusCopy,
                     final int n, final HatExtractor extractor) {
//        final ConstTreebank subbank = treebank.part(0, n);
//        copyTraining(subbank, corpusCopy);

        for (int epoch = 0; epoch < maxEpochs; epoch++) {
            reportFine("Epoch " + epoch);

            Optional<Pair<List<ConstTree>, List<double[][]>>> miniBatchOptional = treebank.getNextTrainMiniBatch();
            while (miniBatchOptional.isPresent()) {
                Pair<List<ConstTree>, List<double[][]>> miniBatch = miniBatchOptional.get();
                List<ConstTree> trees = miniBatch.getKey();
                List<double[][]> embeddingsList = miniBatch.getValue();

                for (int i = 0; i < trees.size(); i++) {
                    ConstTree tree = trees.get(i);
                    double[][] embeddings = embeddingsList.get(i);

                    HatParser parser = makeParser(tree);
                    parser.observe(extractor, embeddings);
                }

                miniBatchOptional = treebank.getNextTrainMiniBatch();
            }

            treebank.resetTrainTreebankIterator();

            extractor.train();
            validate(treebank, extractor);

            if (!extractor.isTraining()) {
                return;
            }
        }

        writeLossListToFile("actionLosses.csv", actionLossList);
        writeLossListToFile("categoryLosses.csv", catLossList);
        writeLossListToFile("fellowLosses.csv", fellowLossList);
    }

    public void writeLossListToFile(String fileName, List<Double> lossList) {
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(lossListOutDirectory + fileName, StandardCharsets.UTF_8);
        } catch (FileNotFoundException e) {
            fail("Cannot create file: " + e);
        } catch (UnsupportedEncodingException e) {
            fail("Unsupported encoding: " + e);
        } catch (IOException e) {
            e.printStackTrace();
            fail("IO exception: " + e);
        }

        String output = lossList.stream().map(String::valueOf).collect(Collectors.joining(","));
        writer.write(output);
        writer.close();
    }

    private void validate(final ConstTreebank treebank, final HatExtractor extractor) {
        double actionClassifierLossScoreSum = 0;
        double catClassifierLossScoreSum = 0;
        double fellowClassifierLossScoreSum = 0;

        extractor.startValidating();

        Optional<Pair<List<ConstTree>, List<double[][]>>> miniBatchOptional = treebank.getNextValidateMiniBatch();
        while (miniBatchOptional.isPresent()) {
            Pair<List<ConstTree>, List<double[][]>> miniBatch = miniBatchOptional.get();
            List<ConstTree> trees = miniBatch.getKey();
            List<double[][]> embeddingsList = miniBatch.getValue();

            for (int i = 0; i < trees.size(); i++) {
                ConstTree tree = trees.get(i);
                double[][] embeddings = embeddingsList.get(i);

                HatParser parser = makeParser(tree);
                parser.observe(extractor, embeddings);
            }

            List<Double> lossScoreSum = extractor.validateMiniBatch();
            actionClassifierLossScoreSum += lossScoreSum.get(0);
            catClassifierLossScoreSum += lossScoreSum.get(1);
            fellowClassifierLossScoreSum += lossScoreSum.get(2);

            miniBatchOptional = treebank.getNextValidateMiniBatch();
        }

        MLP actionClassifier = extractor.getActionClassifier();
        MLP catClassifier = extractor.getCatClassifier();
        MLP fellowClassifier = extractor.getFellowClassifier();

        int n = treebank.getValidateSetSize();
        double actionClassifierLossScoreAverage = actionClassifierLossScoreSum / n;
        double catClassifierLossScoreAverage = catClassifierLossScoreSum / n;
        double fellowClassifierLossScoreAverage = fellowClassifierLossScoreSum / n;

        actionClassifier.applyEarlyStoppingIfApplicable(actionClassifierLossScoreAverage);
        catClassifier.applyEarlyStoppingIfApplicable(catClassifierLossScoreAverage);
        fellowClassifier.applyEarlyStoppingIfApplicable(fellowClassifierLossScoreAverage);

        printLossResults(actionClassifier, actionClassifierLossScoreAverage, "Action");
        printLossResults(catClassifier, catClassifierLossScoreAverage, "Category");
        printLossResults(fellowClassifier, fellowClassifierLossScoreAverage, "Fellow");
        System.out.println();
        System.out.flush();

        treebank.resetValidateTreebankIterator();
        extractor.stopValidating();
    }

    private void printLossResults(MLP classifier, double loss, String classifierName) {
        List<Double> lossList = null;
        switch (classifierName) {
            case "Action":
                lossList = actionLossList;
                break;
            case "Category":
                lossList = catLossList;
                break;
            case "Fellow":
                lossList = fellowLossList;
                break;
            default:
                System.err.println("Error: unknown classifier " + classifierName);
                System.exit(1);
        }

        if (classifier.isTraining()) {
            System.out.println(classifierName + " classifier loss: " + loss + "; " + classifier.getEpochsWithoutImprovement() + " epochs without improvement");
            lossList.add(loss);
        } else {
            System.out.println(classifierName + " classifier loss: " + classifier.getLastLossScore() + "; training complete");

            if (lossList.get(lossList.size()-1) != classifier.getLastLossScore()) {
                lossList.add(loss);
            }
        }
    }

    private void copyTraining(ConstTreebank treebank,
                              final String corpusCopy) {
        PrintWriter trainWriter = null;
        try {
            if (corpusCopy != null)
                trainWriter = new PrintWriter(corpusCopy, "UTF-8");
        } catch (FileNotFoundException e) {
            fail("Cannot create file: " + e);
        } catch (UnsupportedEncodingException e) {
            fail("Unsupported encoding: " + e);
        }
        if (trainWriter != null) {
            trainWriter.print("" + treebank);
            trainWriter.close();
        }
    }

    protected boolean allowableTree(final ConstTree tree) {
        return projectivize || tree.isProjective();
    }

    protected HatParser makeParser(final ConstTree tree) {
        final HatParser parser = new HatParser(tree);
        parser.setLeftDependentsFirst(leftDependentsFirst);
        return parser;
    }

//    protected DeterministicParser makeParser(final ConstTree tree) {
//		/*
//		if (nonprojectiveAllowed)
//			tokens = OptimalProjectivizer.nonprojectiveAllowed(tokens);
//			*/
//        final SimpleParser parser = new SimpleParser(tree);
//        parser.setLeftDependentsFirst(leftDependentsFirst);
//        return parser;
//    }

    private static Logger logger() {
        final Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass().getName());
        log.setParent(Logger.getGlobal());
        return log;
    }

    /**
     * Report failure.
     *
     * @param message The thing that failed.
     */
    protected static void fail(final String message) {
        logger().severe(message);
    }

    /**
     * Report fine comment.
     *
     * @param message The message.
     */
    protected void reportFine(final String message) {
        final Logger log = Logger.getLogger(getClass().getName());
        log.setParent(Logger.getGlobal());
        log.fine(message);
    }
}
