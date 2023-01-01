/*
 * Copyright (c) 2019. University of St Andrews
 */

package standrews.constmain;

import javafx.util.Pair;
import standrews.aux_.TimerMilli;
import standrews.classification.FeatureVectorGenerator;
import standrews.classification.MLP;
import standrews.constbase.ClassifierName;
import standrews.constbase.DatasetSplit;
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
    private final String lossListOutDirectory;
    private final boolean measureTrainLoss;

    private final List<Double> actionValidLossList;
    private final List<Double> catValidLossList;
    private final List<Double> fellowValidLossList;

    private final List<Double> actionTrainLossList;
    private final List<Double> catTrainLossList;
    private final List<Double> fellowTrainLossList;

    public SimpleTrainer(final FeatureVectorGenerator featureVectorGenerator,
                         int maxEpochs, String lossListOutDirectory,
                         boolean measureTrainLoss) {
        this.featureVectorGenerator = featureVectorGenerator;
        this.maxEpochs = maxEpochs;
        this.lossListOutDirectory = lossListOutDirectory;
        this.measureTrainLoss = measureTrainLoss;

        actionValidLossList = new ArrayList<>();
        catValidLossList = new ArrayList<>();
        fellowValidLossList = new ArrayList<>();

        actionTrainLossList = new ArrayList<>();
        catTrainLossList = new ArrayList<>();
        fellowTrainLossList = new ArrayList<>();
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
                      String actionFilePath,
                      String catFilePath,
                      String fellowFilePath,
                     final int n, final HatExtractor extractor, boolean startWithValidation) {
        train(treebank, actionFilePath, catFilePath, fellowFilePath, null, n, extractor, startWithValidation);
    }

    public void train(final ConstTreebank treebank,
                      String actionFilePath,
                      String catFilePath,
                      String fellowFilePath,
                     final String corpusCopy,
                     final int n, final HatExtractor extractor, boolean startWithValidation) {
//        final ConstTreebank subbank = treebank.part(0, n);
//        copyTraining(subbank, corpusCopy);

        try {
            if (startWithValidation) {
                validate(treebank, extractor);
            }

            for (int epoch = 0; epoch < maxEpochs; epoch++) {
                reportFine("Epoch " + epoch);

                Optional<List<Pair<ConstTree, double[][]>>> miniBatchOptional = treebank.getNextMiniBatch(DatasetSplit.TRAIN);
                while (miniBatchOptional.isPresent()) {
                    List<Pair<ConstTree, double[][]>> miniBatch = miniBatchOptional.get();

                    observeMiniBatch(extractor, miniBatch);

                    miniBatchOptional = treebank.getNextMiniBatch(DatasetSplit.TRAIN);
                }

                treebank.resetTreebankIterator(DatasetSplit.TRAIN);

                extractor.train();

                Runtime runtime = Runtime.getRuntime();
                System.out.println("memory usage: " + (((double) runtime.totalMemory() - (double) runtime.freeMemory())*100.0/((double) runtime.maxMemory())) + "% of " + runtime.maxMemory()/1000000000 + "gb");

                try {
                    extractor.saveClassifiers(actionFilePath + epoch, catFilePath + epoch, fellowFilePath + epoch);
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }

                validate(treebank, extractor);

                if (!extractor.isTraining()) {
                    return;
                }
            }
        } catch (OutOfMemoryError e) {
            e.printStackTrace();

            try {
                extractor.saveClassifiers(actionFilePath, catFilePath, fellowFilePath);
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }

            if (measureTrainLoss) {
                writeLossListToFile("actionTrainLosses.csv", actionTrainLossList);
                writeLossListToFile("categoryTrainLosses.csv", catTrainLossList);
                writeLossListToFile("fellowTrainLosses.csv", fellowTrainLossList);
            }

            writeLossListToFile("actionValidationLosses.csv", actionValidLossList);
            writeLossListToFile("categoryValidationLosses.csv", catValidLossList);
            writeLossListToFile("fellowValidationLosses.csv", fellowValidLossList);

            System.exit(1);
        }

        if (measureTrainLoss) {
            writeLossListToFile("actionTrainLosses.csv", actionTrainLossList);
            writeLossListToFile("categoryTrainLosses.csv", catTrainLossList);
            writeLossListToFile("fellowTrainLosses.csv", fellowTrainLossList);
        }

        writeLossListToFile("actionValidationLosses.csv", actionValidLossList);
        writeLossListToFile("categoryValidationLosses.csv", catValidLossList);
        writeLossListToFile("fellowValidationLosses.csv", fellowValidLossList);
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

    private void validate(final ConstTreebank treebank, HatExtractor extractor) {
        extractor.startValidating();

        double[] validationLossScores = calculateAverageLossScores(treebank, extractor, DatasetSplit.VALIDATION);
        double actionValidationLossScore = validationLossScores[0];
        double catValidationLossScore = validationLossScores[1];
        double fellowValidationLossScore = validationLossScores[2];

        MLP actionClassifier = extractor.getActionClassifier();
        MLP catClassifier = extractor.getCatClassifier();
        MLP fellowClassifier = extractor.getFellowClassifier();


        if (measureTrainLoss) {
            double[] trainLossScores = calculateAverageLossScores(treebank, extractor, DatasetSplit.TRAIN);
            double actionTrainLossScore = trainLossScores[0];
            double catTrainScore = trainLossScores[1];
            double fellowTrainLossScore = trainLossScores[2];

            actionClassifier.applyEarlyStoppingIfApplicable(actionTrainLossScore, actionValidationLossScore);
            catClassifier.applyEarlyStoppingIfApplicable(catTrainScore, catValidationLossScore);
            fellowClassifier.applyEarlyStoppingIfApplicable(fellowTrainLossScore, fellowValidationLossScore);

            printLossResults(actionClassifier, actionTrainLossScore, actionValidationLossScore, ClassifierName.ACTION);
            printLossResults(catClassifier, catTrainScore, catValidationLossScore, ClassifierName.CATEGORY);
            printLossResults(fellowClassifier, fellowTrainLossScore, fellowValidationLossScore, ClassifierName.FELLOW);
        } else {
            actionClassifier.applyEarlyStoppingIfApplicable(actionValidationLossScore);
            catClassifier.applyEarlyStoppingIfApplicable(catValidationLossScore);
            fellowClassifier.applyEarlyStoppingIfApplicable(fellowValidationLossScore);

            printLossResults(actionClassifier, actionValidationLossScore, ClassifierName.ACTION);
            printLossResults(catClassifier, catValidationLossScore, ClassifierName.CATEGORY);
            printLossResults(fellowClassifier, fellowValidationLossScore, ClassifierName.FELLOW);
        }
        System.out.println();
        System.out.flush();

        extractor.stopValidating();
    }

    public double[] calculateAverageLossScores(ConstTreebank treebank, HatExtractor extractor, DatasetSplit datasetSplit) {
        double actionClassifierLossScoreSum = 0;
        double catClassifierLossScoreSum = 0;
        double fellowClassifierLossScoreSum = 0;

        Optional<List<Pair<ConstTree, double[][]>>> miniBatchOptional = treebank.getNextMiniBatch(datasetSplit);
        while (miniBatchOptional.isPresent()) {
            List<Pair<ConstTree, double[][]>> miniBatch = miniBatchOptional.get();

            observeMiniBatch(extractor, miniBatch);

            List<Double> lossScoreSums = extractor.validateMiniBatch();
            actionClassifierLossScoreSum += lossScoreSums.get(0);
            catClassifierLossScoreSum += lossScoreSums.get(1);
            fellowClassifierLossScoreSum += lossScoreSums.get(2);

//            int validateBatchSize = 5;
//            for (int i = 0; i < Math.ceil((double) miniBatch.size() / (double) validateBatchSize); i++) {
//                observeMiniBatch(extractor, miniBatch.subList(i*validateBatchSize, Math.min((i+1)*validateBatchSize, miniBatch.size())));
//
//                List<Double> lossScoreSums = extractor.validateMiniBatch();
//                actionClassifierLossScoreSum += lossScoreSums.get(0);
//                catClassifierLossScoreSum += lossScoreSums.get(1);
//                fellowClassifierLossScoreSum += lossScoreSums.get(2);
//            }

            miniBatchOptional = treebank.getNextMiniBatch(datasetSplit);
        }

        treebank.resetTreebankIterator(datasetSplit);

        double n = treebank.getSetSize(datasetSplit);
        return new double[] {actionClassifierLossScoreSum / n, catClassifierLossScoreSum / n, fellowClassifierLossScoreSum / n};
    }

    private void observeMiniBatch(HatExtractor extractor, List<Pair<ConstTree, double[][]>> miniBatch) {
        for (Pair<ConstTree, double[][]> treeAndEmbeddings: miniBatch) {
            ConstTree tree = treeAndEmbeddings.getKey();
            double[][] embeddings = treeAndEmbeddings.getValue();

            HatParser parser = makeParser(tree);
            parser.observe(extractor, embeddings);
        }
    }

    private void printLossResults(MLP classifier, double validationLoss, ClassifierName classifierName) {
        List<Double> lossList = null;
        switch (classifierName) {
            case ACTION:
                lossList = actionValidLossList;
                break;
            case CATEGORY:
                lossList = catValidLossList;
                break;
            case FELLOW:
                lossList = fellowValidLossList;
                break;
            default:
                System.err.println("Error: unknown classifier " + classifierName);
                System.exit(1);
        }

        if (classifier.isTraining()) {
            System.out.println(classifierName + " classifier loss: " + validationLoss + "; " + classifier.getEpochsWithoutImprovement() + " epochs without improvement");
            lossList.add(validationLoss);
        } else {
            System.out.println(classifierName + " classifier loss: " + classifier.getLastLossScore() + "; training complete");

            if (lossList.size() > 0 && lossList.get(lossList.size()-1) != classifier.getLastLossScore()) {
                lossList.add(validationLoss);
            }
        }
    }

    private void printLossResults(MLP classifier, double trainLoss, double validationLoss, ClassifierName classifierName) {
        List<Double> trainLossList = null;
        List<Double> validLossList = null;
        switch (classifierName) {
            case ACTION:
                trainLossList = actionTrainLossList;
                validLossList = actionValidLossList;
                break;
            case CATEGORY:
                trainLossList = catTrainLossList;
                validLossList = catValidLossList;
                break;
            case FELLOW:
                trainLossList = fellowTrainLossList;
                validLossList = fellowValidLossList;
                break;
            default:
                System.err.println("Error: unknown classifier " + classifierName);
                System.exit(1);
        }

        if (classifier.isTraining()) {
            System.out.println(classifierName + " classifier train loss: " + trainLoss + "; validation loss: " + validationLoss + "; " + classifier.getEpochsWithoutImprovement() + " epochs without improvement");
            trainLossList.add(trainLoss);
            validLossList.add(validationLoss);
        } else {
            System.out.println(classifierName + " classifier train loss: " + classifier.getLastTrainLossScore() + "; validation loss: " + classifier.getLastLossScore() + "; training complete");

            if (validLossList.get(validLossList.size()-1) != classifier.getLastLossScore()) {
                trainLossList.add(trainLoss);
                validLossList.add(validationLoss);
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
