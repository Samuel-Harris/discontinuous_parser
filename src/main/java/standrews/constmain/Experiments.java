/*
 * Copyright (c) 2019. University of St Andrews
 */

package standrews.constmain;

import standrews.aux_.LogHandler;
import standrews.aux_.TimerMilli;
import standrews.classification.FeatureSpecification;
import standrews.classification.FeatureVectorGenerator;
import standrews.classification.MLPFactory;
import standrews.constbase.*;
import standrews.constbase.heads.*;
import standrews.constextract.HatExtractor;
import standrews.constextract.WholeHatExtractor;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.invoke.MethodHandles;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class Experiments {

    private static String tmp = "tmp/";

//    public static ConstTreebank negraBank(String headSide) {
//        String path = "/home/mjn/Data/Negra/negra-attach.export";
//        ConstTreebank bank = new NegraTreebank(path);
//        HeadFinder finder = new NegraHeadFinder();
//        switch (headSide) {
//            case "left":
//                finder = new LeftHeadFinder();
//                break;
//            case "right":
//                finder = new RightHeadFinder();
//                break;
//        }
//        finder.makeHeadedTreebank(bank);
//        bank.gatherSymbols();
//        return bank;
//    }

    public static ConstTreebank tigerBank(String path, String headSide, Random rng, int miniBatchSize, double trainRatio, double validationRatio, int treebankIteratorQueueSize) throws ArithmeticException {
        String embeddingsDirectory = "../datasets/tiger2.1_bert_corrected_embeddings/";
        ConstTreebank bank = new NegraTreebank(path, embeddingsDirectory, 505);
        bank.removeCycles();
        HeadFinder finder = new TigerHeadFinder();
        switch (headSide) {
            case "left":
                finder = new LeftHeadFinder();
                break;
            case "right":
                finder = new RightHeadFinder();
                break;
        }
        finder.makeHeadedTreebank(bank);
        bank.gatherSymbols();
        bank.setupTreebankIterator(rng, miniBatchSize, trainRatio, validationRatio, treebankIteratorQueueSize);
        return bank;
    }

//	private static ClassifierFactory classifierFactory() {
//		ClassifierFactory f = new DeeplearningClassifierFactory();
//		f.setContinuousTraining(true);
//		// best result with batchsize 10 and 20 epochs
//		f.setBatchSize(100);
//		f.setNEpochs(3);
//		return f;
//		// return new LibSVMClassifierFactory();
//		// return new NeurophClassifierFactory();
//		// return new LibLinearClassifierFactory();
//	}

//    private static void includeVecMappings(
//            final FeatureSpecification spec,
//            final String lang, final ConstTreebank treebank) {
//        final int len = 100;
//        if (spec.hasIntsFeature("inputForms") ||
//                spec.hasIntsFeature("stackForms") ||
//                spec.hasIntsFeature("hatForms")) {
//            Word2VecMapping em = new Word2VecMapping();
//            em.train(new SentenceWordIterator(treebank), len);
//            em.setWindowSize(2);
//            spec.setFormVec(em);
//            // spec.setFormVec(new GloveMapping("/home/mjn/Data/GloVe/glove.6B.50d.txt", 50));
//            // new GloveMapping("/home/mjn/Data/GloVe/" + lang + ".100.txt", 100))
//            em.setNormalize(true);
//            spec.setFormVec(em);
//        }
//    }

//    private static FeatureSpecification simpleSpec(
//            final String lang, final ConstTreebank treebank,
//            final boolean goldPos) {
//        final FeatureSpecification spec = new FeatureSpecification();
//        spec.setIntsFeature("inputPoss", 0, 2);
//        spec.setIntsFeature("inputForms", 0, 2);
//        spec.setIntsFeature("stackCats", 0, 3);
//        spec.setIntsFeature("stackPoss", 0, 3);
//        spec.setIntsFeature("stackForms", 0, 3);
//        spec.setIntsFeature("leftCats", 0, 3);
//        spec.setIntsFeature("rightCats", 0, 3);
//        spec.setGoldPos(goldPos);
//        includeVecMappings(spec, lang, treebank);
//        // includePosTagger(spec, lang, corpus);
//        return spec;
//    }

//    private static FeatureSpecification hatSpec(
//            final String lang, final ConstTreebank treebank,
//            final boolean goldPos) {
//        final FeatureSpecification spec = new FeatureSpecification();
////		spec.setIntsFeature("inputPoss", 0, 2);  // Parts of Speech of buffer elements
////		spec.setIntsFeature("inputForms", 0, 2);  // vectors of buffer elements
////		spec.setIntsFeature("hatCats", -3, 3);  // categories of stack elements
////		spec.setIntsFeature("hatPoss", -3, 3);  // Parts of Speech of stack elements
////		spec.setIntsFeature("hatForms", -3, 3);  // vectors of stack elements
////		spec.setIntsFeature("hatLeftCats", -3, 3);
////		spec.setIntsFeature("hatRightCats", -3, 3);
////		spec.setIntFeature("hatLeftCap", 2);
////		spec.setIntFeature("hatRightCap", 5);
////		spec.setIntFeature("viewMin", -3);
////		spec.setIntFeature("viewMax", 3);
//        spec.setGoldPos(goldPos);
//        includeVecMappings(spec, lang, treebank);
//        // includePosTagger(spec, lang, corpus);
//        return spec;
//    }

    private static FeatureSpecification hatSpecCheap(
            final String lang, final ConstTreebank treebank,
            final boolean goldPos) {
        final FeatureSpecification spec = new FeatureSpecification();
        spec.setIntsFeature("inputPoss", 0, 2);
        spec.setIntsFeature("inputForms", 0, 2);
        spec.setIntFeature("viewMin", -3);
        spec.setIntFeature("viewMax", 3);
        return spec;
    }

//	public static SimpleExtractor trainSimple(
//			final String lang, final ConstTreebank treebank,
//			final int n,
//			final FeatureSpecification spec,
//			final boolean leftFirst,
//			final boolean projectivize) {
//		final String actionFile = tmp + "actionfile";
//		final String catFile = tmp + "catfile";
//		final SimpleExtractor extractor =
//				new SimpleExtractor(treebank, spec,
//						classifierFactory(), actionFile, catFile);
//		final SimpleTrainer trainer = new SimpleTrainer(spec);
//		trainer.setLeftDependentsFirst(leftFirst);
//		trainer.setProjectivize(projectivize);
//		final int nDone = trainer.train(treebank, n, extractor);
//		if (nDone != n)
//			fail("" + lang + ": processed " + nDone + " of " + n);
//		return extractor;
//	}

    public static HatExtractor trainHat(
            final String lang,
            final ConstTreebank treebank,
            final int n,
            final FeatureVectorGenerator featureVectorGenerator,
            final boolean leftFirst,
            final boolean measureTrainLoss, int[] hiddenLayers, int maxEpochs, int networkMiniBatchSize,
            double learningRate, double l2Lambda, double dropoutRate, double tol, int patience, int seed) {
        final MLPFactory mlpFactory = new MLPFactory(
                featureVectorGenerator.getVectorLength(),
                hiddenLayers,
                learningRate,
                l2Lambda,
                dropoutRate,
                seed);
        final HatExtractor extractor = new HatExtractor(
                featureVectorGenerator,
                mlpFactory,
                networkMiniBatchSize,
                tol,
                patience);
        final SimpleTrainer trainer = new SimpleTrainer(featureVectorGenerator, maxEpochs, tmp, measureTrainLoss);
        trainer.setLeftDependentsFirst(leftFirst);
        trainer.train(treebank, n, extractor);
//        if (nDone != n)
//            fail("" + lang + ": processed " + nDone + " of " + n);
        return extractor;
    }

    public static WholeHatExtractor trainWholeHat(
            final String lang, final ConstTreebank treebank,
            final int n) {
        final WholeHatExtractor extractor = new WholeHatExtractor();
        final WholeHatTrainer trainer = new WholeHatTrainer();
        trainer.train(treebank, n, extractor);
        return extractor;
    }

//	public static HatExtractorAnalysis trainHatAnalysis(
//			final String lang, final ConstTreebank treebank,
//			final int n,
//			final FeatureSpecification spec,
//			final boolean leftFirst,
//			final boolean suppressCompression) {
//		final String actionFile = tmp + "actionfile";
//		final String fellowFile = tmp + "fellowfile";
//		final String catFile = tmp + "catfile";
//		final HatExtractorAnalysis extractor =
//				new HatExtractorAnalysis(treebank, spec,
//						classifierFactory(), actionFile, fellowFile, catFile,
//						suppressCompression);
//		final SimpleTrainer trainer = new HatTrainer(spec);
//		trainer.setLeftDependentsFirst(leftFirst);
//		final int nDone = trainer.train(treebank, n, extractor);
//		if (nDone != n)
//			fail("" + lang + ": processed " + nDone + " of " + n);
//		return extractor;
//	}

//	public static void trainTestSimple(
//			final String lang, final ConstTreebank treebank,
//			final String goldFile,
//			final String parsedFile,
//			final int nTrain, final int nTest,
//			final boolean leftFirst,
//			final boolean projectivize,
//			final boolean goldPos) {
//		final FeatureSpecification spec = simpleSpec(lang, treebank, goldPos);
//		final SimpleExtractor extractor = trainSimple(lang, treebank, nTrain, spec, leftFirst, projectivize);
//		final SimpleTester tester = new SimpleTester(spec);
//		tester.test(treebank, goldFile, parsedFile, nTrain, nTest, extractor);
//	}

    public static void trainTestHat(
            final String lang, final ConstTreebank treebank,
            final String goldFile,
            final String parsedFile,
            final int nTrain, final int nTest,
            final boolean leftFirst,
            final boolean measureTrainLoss, int[] hiddenLayers, int maxEpochs, int networkMiniBatchSize,
            double learningRate, double l2Lambda, double dropoutRate, double tol, int patience, int seed) {
        FeatureVectorGenerator featureVectorGenerator = new FeatureVectorGenerator(treebank);
        final HatExtractor extractor = trainHat(lang, treebank, nTrain, featureVectorGenerator, leftFirst,
                measureTrainLoss, hiddenLayers, maxEpochs, networkMiniBatchSize, learningRate, l2Lambda, dropoutRate, tol, patience, seed);
        final HatTester tester = new HatTester(featureVectorGenerator);
        tester.test(treebank, goldFile, parsedFile, nTrain, nTest, extractor);
    }

    public static void trainTestWholeHat(
            final String lang, final ConstTreebank treebank,
            final int nTrain, final int nTest) {
        final WholeHatExtractor extractor = trainWholeHat(lang, treebank, nTrain);
        final WholeHatTester tester = new WholeHatTester();
        tester.test(treebank, nTrain, nTest, extractor);
    }

//	public static void trainHatAnalysis(
//			final String lang, final ConstTreebank treebank,
//			final int nTrain,
//			final boolean leftFirst,
//			final boolean suppressCompression,
//			final boolean goldPos) {
//		final FeatureSpecification spec = hatSpec(lang, treebank, goldPos);
//		final HatExtractorAnalysis extractor = trainHatAnalysis(lang, treebank, nTrain, spec,
//				leftFirst, suppressCompression);
//		reportLogFile(extractor.analysis());
//	}

//	public static void doTrainingAndTestingSimple(
//			final String lang, final ConstTreebank treebank,
//			final int nTrain, final int nTest,
//			final boolean leftFirst,
//			final boolean projectivize,
//			final boolean goldPos) {
//		final TimerMilli timer = new TimerMilli();
//		timer.start();
//		trainTestSimple(lang, treebank,
//				tmp + "goldfile", tmp + "parsedfile",
//				nTrain, nTest,
//				leftFirst,
//				projectivize,
//				goldPos);
//		timer.stop();
//		final String report = "Simple took " + timer.seconds() + " s";
//		reportFine(report);
//		reportLogFile(report);
//	}

    public static void doTrainingAndTestingHat(
            final String lang, final ConstTreebank treebank,
            final int nTrain, final int nTest,
            final boolean leftFirst,
            final boolean measureTrainLoss,
            final boolean goldPos, int[] hiddenLayers, int maxEpochs, int networkMiniBatchSize,
            double learningRate, double l2Lambda, double dropoutRate, double tol, int patience, int seed) {
        final TimerMilli timer = new TimerMilli();
        timer.start();
        trainTestHat(lang, treebank,
                tmp + "goldfile.export", tmp + "parsedfile.export",
                nTrain, nTest,
                leftFirst,
                measureTrainLoss,
                hiddenLayers,
                maxEpochs, networkMiniBatchSize,
                learningRate, l2Lambda, dropoutRate, tol, patience, seed);
        timer.stop();
        final String report = "Hat took " + timer.seconds() + " s";
        reportFine(report);
        reportLogFile(report);
    }

    public static void doTrainingAndTestingWholeHat(
            final String lang, final ConstTreebank treebank,
            final int nTrain, final int nTest) {
        final TimerMilli timer = new TimerMilli();
        timer.start();
        trainTestWholeHat(lang, treebank, nTrain, nTest);
        timer.stop();
        final String report = "Whole hat took " + timer.seconds() + " s";
        reportFine(report);
        reportLogFile(report);
    }

//	public static void doTrainingHatAnalysis(
//			final String lang,
//			final ConstTreebank treebank,
//			final int nTrain,
//			final boolean leftFirst,
//			final boolean suppressCompression,
//			final boolean goldPos) {
//		final TimerMilli timer = new TimerMilli();
//		timer.start();
//		trainHatAnalysis(lang, treebank,
//				nTrain,
//				leftFirst,
//				suppressCompression,
//				goldPos);
//		timer.stop();
//		final String report = "Hat analysis took " + timer.seconds() + " s";
//		reportFine(report);
//		reportLogFile(report);
//	}

    private static void doMethod() throws ArithmeticException {

//		final String method = "simple";
        final String method = "hat";
        // final String method = "hatminus";

        // final String bankname = "negra";
        final String bankname = "tiger";
        final String bankPath = "../datasets/tigercorpus2.1_small/corpus/tiger_negraformat.export";

        // final String headSide = "";
        // final String headSide = "left";
        final String headSide = "right";

        ConstTreebank treebank = null;
        String lang = "";
        boolean measureTrainLoss = true;
        int nTrain = 0;
        int nTest = 0;
        double trainRatio = 0.7;
        double validationRatio = 0.2;  // testRatio = 1 - trainRatio - validationRatio
        int[] hiddenLayers = new int[]{256, 256};
        int maxEpochs = 3;
        double learningRate = 0.0005;
        double l2Lambda = 0.001;
        double dropoutRate = 0.1;
        double tol = 0.001;
        int patience = 5;
        int seed = 123;
        int fetchMiniBatchSize = 50;
        int networkMiniBatchSize = 128;
        int treebankIteratorQueueSize = 32;
        Random rng = new Random(seed);

        switch (bankname) {
//            case "negra":
//                // Negra has 20602 trees. 80% is 16482.
//                treebank = negraBank(headSide);
//                lang = "de";
//                nTrain = 16482;
//                nTest = 2060;
//                break;
            case "tiger":
                // Tiger has 50472 trees. 80% is 40377.
                treebank = tigerBank(bankPath, headSide, rng, fetchMiniBatchSize, trainRatio, validationRatio, treebankIteratorQueueSize);
                lang = "de";
                nTrain = 800;
                nTest = 200;
//				nTrain = 40377;
//				nTest = 5047;
                break;
            default:
                fail("Unknown bankname " + bankname);
        }

        reportFine("testing whether each word in each sentence has exactly one embedding vector...");
        for (ConstTree tree : treebank.getTrees()) {
            int numEmbeddings = treebank.getSentenceEmbeddingsMetadata("s" + tree.getId()).getSentenceLength();
            if (tree.length() != numEmbeddings) {
                System.err.println("Error: sentence " + tree.getId() + " has an incorrect number of word embeddings");
                System.err.println("Expected: " + tree.length() + ". Found: " + numEmbeddings);
                System.exit(1);
            }
        }
        reportFine("Embeddings for every word in every sentence have been found");

        final boolean leftFirst = true;
        // final boolean leftFirst = false;
        final boolean projectivize = false;
        final boolean goldPos = true;
        doTrainingAndTestingHat(lang, treebank, nTrain, nTest, leftFirst, measureTrainLoss, goldPos, hiddenLayers,
                maxEpochs, networkMiniBatchSize, learningRate, l2Lambda, dropoutRate, tol, patience, seed);

//        if (method.equals("simple")) {
//			doTrainingAndTestingSimple(lang, treebank, nTrain, nTest,
//					leftFirst, projectivize, goldPos);
//        } else if (method.equals("hat")) {
//            doTrainingAndTestingHat(lang, treebank, nTrain, nTest, leftFirst, false, goldPos, embeddingsBank);
//        } else if (method.equals("hatminus")) {
//            doTrainingAndTestingHat(lang, treebank, nTrain, nTest, leftFirst, true, goldPos, embeddingsBank);
//        }
    }

//    private static void doWholeHat() {
//        final String lang = "de";
//        final String headSide = "";
//        // final String headSide = "left";
//        // final String headSide = "right";
//        final int nTrain = 16482;
//        final int nTest = 2060;
//        final ConstTreebank treebank = negraBank(headSide);
//        doTrainingAndTestingWholeHat(lang, treebank, nTrain, nTest);
//    }

//	private static void doHatAnalysis() {
//		final String lang = "de";
//		final String headSide = "";
//		// final String headSide = "left";
//		// final String headSide = "right";
//		// final int nTrain = 20602;
//		// final ConstTreebank treebank = negraBank(headSide);
//		final int nTrain = 1000;
//		// final int nTrain = 10;
//		final ConstTreebank treebank = tigerBank(headSide);
//		final boolean leftFirst = true;
//		// final boolean leftFirst = false;
//		final boolean projectivize = false;
//		final boolean goldPos = true;
//		doTrainingHatAnalysis(lang, treebank, nTrain,
//					leftFirst, false, goldPos);
//	}

    private static void convertTiger() {
        String source = "../datasets/tigercorpus2.1/corpus/tiger_release_aug07.xml";
        String target = "../datasets/tigercorpus2.1/corpus/tiger_negraformat.export";
        // ConstTreebank treebank = new TigerTreebank(file);
        TigerTreebank.convertNegra(source, target);
    }

    public static void main(String[] args) throws ArithmeticException {
        setLoggerHandler();
        clearLogFile();
        doMethod();
//		 doWholeHat();
//		doHatAnalysis();
//		 convertTiger();
    }

    private static void setLoggerHandler() {
        LogManager.getLogManager().reset();
        final Logger globalLog = Logger.getGlobal();
        globalLog.setLevel(Level.ALL);
        globalLog.addHandler(new LogHandler());
    }

    private static Logger logger() {
        final Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass().getName());
        log.setParent(Logger.getGlobal());
        return log;
    }

    private static void clearLogFile() {
        logger();
        try {
            final PrintWriter logWriter = new PrintWriter("java.txt");
            logWriter.close();
        } catch (FileNotFoundException e) {
            logger().severe("Cannot create file: " + e);
        }
    }

    private static void reportLogFile(final String message) {
        try {
            final FileWriter logWriter = new FileWriter("java.txt", true);
            logWriter.write(message + "\n");
            logWriter.close();
        } catch (IOException e) {
            logger().severe("Cannot write to file: " + e);
        }
    }

    /**
     * Report failure.
     *
     * @param message The thing that failed.
     */
    private static void fail(final String message) {
        logger().log(Level.WARNING, message);
    }

    /**
     * Report fine comment.
     *
     * @param message The message.
     */
    private static void reportFine(final String message) {
        logger().fine(message);
    }


}
