/*
 * Copyright (c) 2018. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package standrews.depmain;

import standrews.depbase.*;
import standrews.depautomata.DependencyGraph;
import standrews.aux.DataCollectionSum;
import standrews.aux.LogHandler;
import standrews.aux.TimerMilli;
import standrews.classification.*;
import standrews.depextract.*;
import standrews.lexical.*;

import java.io.*;
import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.LinkedList;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class Experiments {

    private static String tmp = "tmp/";

    private static ClassifierFactory classifierFactory() {
        ClassifierFactory f = new DeeplearningClassifierFactory();
        f.setContinuousTraining(true);
        // best result with batchsize 10 and 20 epochs
        f.setBatchSize(100); // 100
        f.setNEpochs(2); // 15
        return f;
        // return new LibSVMClassifierFactory();
        // return new NeurophClassifierFactory();
        // return new LibLinearClassifierFactory();
    }

    private static void includeVecMappings(final FeatureSpecification spec,
                                           final String lang,
                                           final String corpus) {
        final int len = 100;
        if (spec.hasIntsFeature("prefixForms") ||
                spec.hasIntsFeature("suffixForms") ||
                spec.hasIntsFeature("hatForms")) {
            Word2VecMapping em = new Word2VecMapping();
            em.train(new SentenceFormIterator(corpus), len);
            em.setWindowSize(2);
            spec.setFormVec(em);
            // spec.setFormVec(new GloveMapping("/home/mjn/Data/GloVe/glove.6B.50d.txt", 50));
            // new GloveMapping("/home/mjn/Data/GloVe/" + lang + ".100.txt", 100))
            em.setNormalize(true);
        }
        if (spec.hasIntsFeature("prefixLemmas") ||
                spec.hasIntsFeature("suffixLemmas") ||
                spec.hasIntsFeature("hatLemmas")) {
            Word2VecMapping em = new Word2VecMapping();
            em.train(new SentenceLemmaIterator(corpus), len);
            spec.setLemmaVec(em);
        }
    }

    private static void includePosTagger(final FeatureSpecification spec,
                                         final String lang,
                                         final String corpus) {
        if (!spec.getGoldPos()) {
            spec.setPosTagger(new HMMTagger(2, corpus));
        }
    }

    private static FeatureSpecification simpleSpec(final String lang,
                                                   final String corpus,
                                                   final boolean goldPos) {
        final FeatureSpecification spec = new FeatureSpecification();
        spec.setIntsFeature("prefixPoss", 0, 2);
        spec.setIntsFeature("suffixPoss", 0, 1);
        // spec.setIntsFeature("prefixForms", 0, 1);
        // spec.setIntsFeature("suffixForms", 0, 1);
        spec.setIntsFeature("prefixLemmas");
        spec.setIntsFeature("suffixLemmas");
        spec.setIntsFeature("leftDeprels", 0, 1);
        spec.setIntsFeature("rightDeprels", 0, 1);
        spec.setGoldPos(goldPos);
        includeVecMappings(spec, lang, corpus);
        includePosTagger(spec, lang, corpus);
        return spec;
    }

    private static FeatureSpecification swapSpec(final String lang,
                                                 final String corpus,
                                                 final boolean goldPos) {
        final FeatureSpecification spec = new FeatureSpecification();
        spec.setIntsFeature("prefixPoss", 0, 2);
        spec.setIntsFeature("suffixPoss", 0, 1);
        spec.setIntsFeature("prefixForms", 0, 1);
        spec.setIntsFeature("suffixForms", 0, 1);
        spec.setIntsFeature("prefixLemmas");
        spec.setIntsFeature("suffixLemmas");
        spec.setIntsFeature("leftDeprels", 0, 1);
        spec.setIntsFeature("rightDeprels", 0, 1);
        spec.setGoldPos(goldPos);
        includeVecMappings(spec, lang, corpus);
        includePosTagger(spec, lang, corpus);
        return spec;
    }

    private static FeatureSpecification hatSpec(final String lang,
                                                final String corpus,
                                                final boolean goldPos) {
        final FeatureSpecification spec = new FeatureSpecification();
        // spec.setIntsFeature("prefixPoss", 0, 2);
        spec.setIntsFeature("suffixPoss", 0, 1);
        // spec.setIntsFeature("prefixForms", 0, 1);
        // spec.setIntsFeature("suffixForms", 0, 1);
        spec.setIntsFeature("prefixLemmas");
        spec.setIntsFeature("suffixLemmas");
        spec.setIntsFeature("leftDeprels");
        spec.setIntsFeature("rightDeprels");
        spec.setIntsFeature("hatPoss", -2, 2);
        spec.setIntsFeature("hatForms", -1, 0);
        // spec.setIntsFeature("hatForms", -1, 1);
        spec.setIntsFeature("hatLemmas");
        spec.setIntsFeature("hatLeftDeprels", -1, 0);
        spec.setIntsFeature("hatRightDeprels", -1, 0);
        // spec.setIntsFeature("hatLeftDeprels", -1, 1);
        // spec.setIntsFeature("hatRightDeprels", -1, 1);
        spec.setIntFeature("hatLeftCap", 1);
        spec.setIntFeature("hatRightCap", 5);
        spec.setIntFeature("viewMin", -2);
        spec.setIntFeature("viewMax", 2);
        spec.setGoldPos(goldPos);
        includeVecMappings(spec, lang, corpus);
        includePosTagger(spec, lang, corpus);
        return spec;
    }

    private static FeatureSpecification hatSpecEasy(final String lang,
                                                    final String corpus,
                                                    final boolean goldPos) {
        final FeatureSpecification spec = new FeatureSpecification();
        spec.setIntFeature("viewMin", -2);
        spec.setIntFeature("viewMax", 2);
        return spec;
    }

    public static SimpleExtractor trainSimple(final String corpus,
                                              final String corpusCopy,
                                              final int n,
                                              final FeatureSpecification spec,
                                              final boolean leftFirst,
                                              final boolean strict,
                                              final boolean dynamic,
                                              final boolean nonprojectiveAllowed,
                                              final boolean dynProjectivize) {
        final String actionFile = tmp + "actionfile";
        final String deprelLeftFile = tmp + "deprelleftfile";
        final String deprelRightFile = tmp + "deprelrightfile";
        final SimpleExtractor extractor =
                new SimpleExtractor(spec, classifierFactory(), actionFile, deprelLeftFile, deprelRightFile);
        final double strayProb = 1.0;
        final int nIterations = 1;
        final SimpleTrainer trainer =
                dynamic ?
                        new SimpleDynamicTrainer(spec, strayProb, nIterations, dynProjectivize) :
                        new SimpleTrainer(spec);
        trainer.setLeftDependentsFirst(leftFirst);
        trainer.setStrict(strict);
        trainer.setNonprojectiveAllowed(nonprojectiveAllowed);
        final int nDone = trainer.train(corpus, corpusCopy, n, extractor);
        if (nDone != n)
            fail("" + corpus + ": processed " + nDone + " of " + n);
        return extractor;
    }

    public static SwapExtractor trainSwap(final String corpus,
                                          final String corpusCopy,
                                          final int n,
                                          final FeatureSpecification spec,
                                          final boolean leftFirst) {
        final String actionFile = tmp + "actionfile";
        final String deprelLeftFile = tmp + "deprelleftfile";
        final String deprelRightFile = tmp + "deprelrightfile";
        final SwapExtractor extractor =
                new SwapExtractor(spec, classifierFactory(), actionFile, deprelLeftFile, deprelRightFile);
        final SimpleTrainer trainer = new SwapTrainer(spec);
        trainer.setLeftDependentsFirst(leftFirst);
        final int nDone = trainer.train(corpus, corpusCopy, n, extractor);
        if (nDone != n)
            fail("" + corpus + ": processed " + nDone + " of " + n);
        return extractor;
    }

    public static HatExtractor trainHat(final String corpus,
                                        final String corpusCopy,
                                        final int n,
                                        final FeatureSpecification spec,
                                        final boolean leftFirst,
                                        final boolean dynamic,
                                        final boolean suppressCompression) {
        final String actionFile = tmp + "actionfile";
        final String fellowFile = tmp + "fellowfile";
        final String deprelLeftFile = tmp + "deprelleftfile";
        final String deprelRightFile = tmp + "deprelrightfile";
        final HatExtractor extractor =
                new HatExtractor(spec,
                        classifierFactory(), actionFile, fellowFile,
                        deprelLeftFile, deprelRightFile,
                        suppressCompression);
        final SimpleTrainer trainer =
                dynamic ?
                        new HatTrainer(spec) : // TODO
                        new HatTrainer(spec);
        trainer.setLeftDependentsFirst(leftFirst);
        final int nDone = trainer.train(corpus, corpusCopy, n, extractor);
        if (nDone != n)
            fail("" + corpus + ": processed " + nDone + " of " + n);
        return extractor;
    }

    public static WholeHatExtractor trainWholeHat(
            final String corpus,
            final int n) {
        final WholeHatExtractor extractor =	new WholeHatExtractor();
        final WholeHatTrainer trainer = new WholeHatTrainer();
        trainer.train(corpus, n, extractor);
        return extractor;
    }

    public static HatExtractorAnalysis trainHatAnalysis(final String corpus,
                                                        final int n,
                                                        final FeatureSpecification spec,
                                                        final boolean leftFirst) {
        final String actionFile = tmp + "actionfile";
        final String fellowFile = tmp + "fellowfile";
        final String deprelLeftFile = tmp + "deprelleftfile";
        final String deprelRightFile = tmp + "deprelrightfile";
        final HatExtractorAnalysis extractor =
                new HatExtractorAnalysis(spec,
                        actionFile, fellowFile, deprelLeftFile, deprelRightFile,
                        classifierFactory());
        final SimpleTrainer trainer = new HatTrainer(spec);
        trainer.setLeftDependentsFirst(leftFirst);
        final int nDone = trainer.train(corpus, n, extractor);
        if (nDone != n)
            fail("" + corpus + ": processed " + nDone + " of " + n);
        return extractor;
    }

    public static SimpleExtractor trainSimpleDynamicSpeed(final String corpus,
                                                          final String corpusCopy,
                                                          final int n,
                                                          final FeatureSpecification spec,
                                                          final boolean leftFirst,
                                                          final boolean strict,
                                                          final boolean projectivize,
                                                          final boolean dynProjectivize) {
        final String actionFile = tmp + "actionfile";
        final String deprelLeftFile = tmp + "deprelleftfile";
        final String deprelRightFile = tmp + "deprelrightfile";
        final SimpleExtractor extractor =
                new SimpleExtractor(spec, classifierFactory(), actionFile, deprelLeftFile, deprelRightFile);
        final double strayProb = 1.0;
        final int nIterations = 1;
        final DataCollectionSum timings = new DataCollectionSum();
        final SimpleDynamicTrainerSpeed trainer =
                new SimpleDynamicTrainerSpeed(
                        spec, strayProb, nIterations, dynProjectivize, timings);
        trainer.setLeftDependentsFirst(leftFirst);
        trainer.setStrict(strict);
        trainer.setNonprojectiveAllowed(projectivize);
        // int batch = arg1;
        // trainer.setFrom(batch * 1000);
        // trainer.setTo((batch+1) * 1000);
        // for (int i = 0; i < retries; i++) {
        trainer.train(corpus, corpusCopy, n, extractor);
        // }
		/*
		timings.store("file" + batch + ".txt");
		for (int prev = 0; prev < batch; prev++)
			timings.retrieve("file" + prev + ".txt");
			*/
        System.out.println(
                timings.averagesString("linear"));
        System.out.println(
                timings.averagesString("quadratic"));
        System.out.println(
                timings.averagesString("simple"));
        System.out.println(
                DataCollectionSum.globalCollection.averagesStrings());
        return extractor;
    }

    public static void trainTestSimple(final String lang,
                                       final String trainCorpus,
                                       final String trainFile,
                                       final String testCorpus,
                                       final String goldFile,
                                       final String parsedFile,
                                       final int nTrain, final int nTest,
                                       final boolean leftFirst,
                                       final boolean strict,
                                       final boolean dynamic,
                                       final boolean projectivize,
                                       final boolean dynProjectivize,
                                       final boolean goldPos) {
        final FeatureSpecification spec = simpleSpec(lang, trainCorpus, goldPos);
        final SimpleExtractor extractor = trainSimple(trainCorpus, trainFile,
                nTrain, spec, leftFirst, strict, dynamic, projectivize, dynProjectivize);
        final SimpleTester tester = new SimpleTester(spec);
        tester.test(testCorpus, goldFile, parsedFile, nTest, extractor);
    }

    public static void trainTestSimpleDynamicSpeed(final String lang,
                                                   final String trainCorpus,
                                                   final String trainFile,
                                                   final String testCorpus,
                                                   final String goldFile,
                                                   final String parsedFile,
                                                   final int nTrain, final int nTest,
                                                   final boolean leftFirst,
                                                   final boolean strict,
                                                   final boolean projectivize,
                                                   final boolean dynProjectivize,
                                                   final boolean goldPos) {
        final FeatureSpecification spec = simpleSpec(lang, trainCorpus, goldPos);
        final SimpleExtractor extractor = trainSimpleDynamicSpeed(trainCorpus, trainFile,
                nTrain, spec, leftFirst, strict, dynProjectivize, projectivize);
		/*
		final SimpleTester tester = new SimpleTester(spec);
		tester.test(testCorpus, goldFile, parsedFile, nTest, extractor);
		*/
    }

    public static void trainTestSwap(final String lang,
                                     final String trainCorpus,
                                     final String trainFile,
                                     final String testCorpus,
                                     final String goldFile,
                                     final String parsedFile,
                                     final int nTrain, final int nTest,
                                     final boolean leftFirst,
                                     final boolean goldPos) {
        final FeatureSpecification spec = swapSpec(lang, trainCorpus, goldPos);
        final SimpleExtractor extractor = trainSwap(trainCorpus, trainFile,
                nTrain, spec, leftFirst);
        final SimpleTester tester = new SwapTester(spec);
        tester.test(testCorpus, goldFile, parsedFile, nTest, extractor);
    }

    public static void trainTestHat(final String lang,
                                    final String trainCorpus,
                                    final String trainFile,
                                    final String testCorpus,
                                    final String goldFile,
                                    final String parsedFile,
                                    final int nTrain, final int nTest,
                                    final boolean leftFirst,
                                    final boolean dynamic,
                                    final boolean suppressCompression,
                                    final boolean goldPos) {
        final FeatureSpecification spec = hatSpec(lang, trainCorpus, goldPos);
        final HatExtractor extractor = trainHat(trainCorpus, trainFile,
                nTrain, spec, leftFirst, dynamic, suppressCompression);
        final HatTester tester = new HatTester(spec);
        tester.test(testCorpus, goldFile, parsedFile, nTest, extractor);
    }

    public static void trainTestHatProb(final String lang,
                                        final String trainCorpus,
                                        final String trainFile,
                                        final String testCorpus,
                                        final int nTrain, final int nTest,
                                        final boolean leftFirst,
                                        final boolean dynamic,
                                        final boolean suppressCompression,
                                        final boolean goldPos) {
        final FeatureSpecification spec = hatSpec(lang, trainCorpus, goldPos);
        final HatExtractor extractor = trainHat(trainCorpus, trainFile,
                nTrain, spec, leftFirst, dynamic, suppressCompression);
        final ProbHatTester tester = new ProbHatTester(spec);
        tester.test(testCorpus, nTest, extractor);
    }

    public static void trainTestWholeHat(
            final String lang,
            final String trainCorpus,
            final String testCorpus,
            final int nTrain, final int nTest) {
        final WholeHatExtractor extractor = trainWholeHat(trainCorpus, nTrain);
        final WholeHatTester tester = new WholeHatTester();
        tester.test(testCorpus, nTest, extractor);
    }

    public static void trainHatPrintAnalysis(final String lang,
                                             final String corpus,
                                             final int n,
                                             final boolean leftFirst,
                                             final boolean goldPos) {
        final FeatureSpecification spec = hatSpecEasy(lang, corpus, goldPos);
        final HatExtractorAnalysis extractor =
                trainHatAnalysis(corpus, n, spec, leftFirst);
        reportLogFile(extractor.analysis());
    }

    /**
     * Process a number of trees from each corpus to test reading
     * and basic processing.
     */
    public static void testTreebankReading() {
        for (String corpus : Corpora.trainFiles()) {
            Treebank treebank = new Treebank(corpus, 5);
            for (DependencyStructure struct : treebank.depStructs) {
                System.out.println(struct);
                DependencyGraph g = new DependencyGraph(struct.getNormalTokens());
                System.out.println("projective: " + g.isProjective());
                System.out.println(g.toString());
            }
            // To test writing:
            // treebank.write("../../tmp/parsetest/" + name);
        }
    }

    /**
     * Test training&testing on a number of trees from each corpus.
     */
    public static void testTrainingAndTestingAll(final String method,
                                                 final boolean leftFirst,
                                                 final boolean strict,
                                                 final boolean dynamic,
                                                 final boolean projectivize,
                                                 final boolean dynProjectivize,
                                                 final boolean goldPos) {
        for (String corpus : Corpora.trainFiles()) {
            final TimerMilli timer = new TimerMilli();
            try {
                String lang = Corpora.langOfCorpus(corpus);
                timer.start();
                if (method.equals("simple"))
                    trainTestSimple(lang, corpus,
                            tmp + "trainfile",
                            corpus,
                            tmp + "goldfile",
                            tmp + "parsedfile",
                            20, 20, leftFirst, strict,
                            dynamic, projectivize, dynProjectivize, goldPos);
                else if (method.equals("swap"))
                    trainTestSwap(lang, corpus,
                            tmp + "trainfile",
                            corpus,
                            tmp + "goldfile",
                            tmp + "parsedfile",
                            20, 20, leftFirst, goldPos);
                else if (method.equals("hat"))
                    trainTestHat(lang, corpus,
                            tmp + "trainfile",
                            corpus,
                            tmp + "goldfile",
                            tmp + "parsedfile",
                            20, 20, leftFirst, dynamic,
                            false, goldPos);
                timer.stop();
                reportFine("Method " + method + " for " + corpus +
                        " took " + timer.seconds() + " s");
            } catch (Exception e) {
                System.err.println(corpus);
                throw e;
            }
        }
    }

    public static void trainHatPrintAnalysisAll() {
        // final String[] langs = {"bg", "cs", "de", "en",
        // "fa", "fi", "fr", "grc", "hi", "hr", "id", "it", "ja",
        // "nl", "no", "pl", "pt", "ro", "sk", "sl", "sv"};
        final String[] langs = { "ko" };
        // final String[] langs = { "ar", "cs", "de", "et", "grc", "ja", "ko", "no" , "ru" };
        final boolean leftFirst = true;
        for (String lang : langs) {
            reportLogFile("For " + lang);
            final TimerMilli timer = new TimerMilli();
            timer.start();
            trainHatPrintAnalysis(lang, Corpora.trainFile(lang), 100000, leftFirst, true);
            timer.stop();
            reportFine("Hat analysis for " + lang + " took " + timer.seconds() + " s");
        }
    }

    /**
     * Test training&testing on particular corpora.
     */
    public static void doTrainingAndTestingSimple(final String[] langs,
                                                  final int nTrain, final int nTest,
                                                  final boolean leftFirst,
                                                  final boolean strict,
                                                  final boolean dynamic,
                                                  final boolean nonprojectiveAllowed,
                                                  final boolean dynProjectivize,
                                                  final boolean goldPos) {
        for (String lang : langs) {
            final TimerMilli timer = new TimerMilli();
            timer.start();
            trainTestSimple(lang, Corpora.trainFile(lang),
                    tmp + "train-" + lang + ".conllu",
                    Corpora.testFile(lang),
                    tmp + "gold-" + lang + ".conllu",
                    tmp + "parsed-" + lang + ".conllu",
                    nTrain, nTest,
                    leftFirst, strict, dynamic,
                    nonprojectiveAllowed, dynProjectivize, goldPos);
            timer.stop();
            final String report = "Simple for " + lang + " took " + timer.seconds() + " s";
            reportFine(report);
            reportLogFile(report);
        }
    }

    public static void doTrainingAndTestingSwap(final String[] langs,
                                                final int nTrain, final int nTest,
                                                final boolean leftFirst,
                                                final boolean goldPos) {
        for (String lang : langs) {
            final TimerMilli timer = new TimerMilli();
            timer.start();
            trainTestSwap(lang, Corpora.trainFile(lang),
                    tmp + "train-" + lang + ".conllu",
                    Corpora.testFile(lang),
                    tmp + "gold-" + lang + ".conllu",
                    tmp + "parsed-" + lang + ".conllu",
                    nTrain, nTest,
                    leftFirst, goldPos);
            timer.stop();
            final String report = "Swap for " + lang + " took " + timer.seconds() + " s";
            reportFine(report);
            reportLogFile(report);
        }
    }

    public static void doTrainingAndTestingHat(final String[] langs,
                                               final int nTrain, final int nTest,
                                               final boolean leftFirst,
                                               final boolean dynamic,
                                               final boolean suppressCompression,
                                               final boolean goldPos) {
        for (String lang : langs) {
            final TimerMilli timer = new TimerMilli();
            timer.start();
            trainTestHat(lang, Corpora.trainFile(lang),
                    tmp + "train-" + lang + ".conllu",
                    Corpora.testFile(lang),
                    tmp + "gold-" + lang + ".conllu",
                    tmp + "parsed-" + lang + ".conllu",
                    nTrain, nTest,
                    leftFirst, dynamic,
                    suppressCompression,
                    goldPos);
            timer.stop();
            final String report = "Hat for " + lang + " took " + timer.seconds() + " s";
            reportFine(report);
            reportLogFile(report);
        }
    }

    public static void doTrainingAndTestingHatProb(final String[] langs,
                                                   final int nTrain, final int nTest,
                                                   final boolean leftFirst,
                                                   final boolean dynamic,
                                                   final boolean suppressCompression,
                                                   final boolean goldPos) {
        for (String lang : langs) {
            final TimerMilli timer = new TimerMilli();
            timer.start();
            trainTestHatProb(lang, Corpora.trainFile(lang),
                    tmp + "train-" + lang + ".conllu",
                    Corpora.testFile(lang),
                    nTrain, nTest,
                    leftFirst, dynamic,
                    suppressCompression,
                    goldPos);
            timer.stop();
            final String report = "Hat prob for " + lang + " took " + timer.seconds() + " s";
            reportFine(report);
            reportLogFile(report);
        }
    }

    public static void doTrainingAndTestingWholeHat(
            final String[] langs,
            final int nTrain, final int nTest) {
        for (String lang : langs) {
            final TimerMilli timer = new TimerMilli();
            timer.start();
            trainTestWholeHat(lang,
                    Corpora.trainFile(lang),
                    Corpora.testFile(lang),
                    nTrain, nTest);
            timer.stop();
            final String report = "Whole hat for " + lang + " took " + timer.seconds() + " s";
            reportFine(report);
            reportLogFile(report);
        }
    }

    public static void doTrainingAndTestingSimpleDynamicSpeed(final String lang,
                                                              final int nTrain, final int nTest,
                                                              final boolean leftFirst,
                                                              final boolean strict,
                                                              final boolean nonprojectiveAllowed,
                                                              final boolean dynProjectivize,
                                                              final boolean goldPos) {
        trainTestSimpleDynamicSpeed(lang, Corpora.trainFile(lang),
                tmp + "train-" + lang + ".conllu",
                Corpora.testFile(lang),
                tmp + "gold-" + lang + ".conllu",
                tmp + "parsed-" + lang + ".conllu",
                nTrain, nTest,
                leftFirst, strict,
                dynProjectivize, nonprojectiveAllowed, goldPos);
    }

    private static void doMethod() {
        // final String method = "simple";
        // final String method = "swap";
        final String method = "hat";
        // final String method = "hatprob";
        // final String method = "hatminus";
        // final String[] langs = null;
        // final String[] langs = {"nl"};
        // final String[] langs = {"bg", "cs", "de", "en", "fi",
        // "grc", "ja", "nl", "no", "ro", "ru",
        // "sv", "tr", "ur"};
        // Languages with at least 4000 training sentences and 400 test sentences
        // Omitted ar, ca, es, he,
        // final String[] langs = {"de"};
        // final String[] langs = {"cu", "da", "de", "el", "eu", "got", "hu"};
        // Languages with token counts >= 300,000, plus de and grc
        // final String[] langs = {"ja", "ru", "de", "grc", "ar", "ca", "cs", "es", "fr"};
        // Languages with > 14000 projective sentences, plus de, grc
        // final String[] langs = { "ar", "cs", "de", "et", "grc"};
        // final String[] langs = { "ja", "ko", "no"};
        final String[] langs = { "de" };
        // final String[] langs = { /* "ar", "cs", "de", "et", "grc" */ "ja", "ko", "no" , "ru"};
        // final String[] langs = Corpora.someLanguages();
        // final String[] langs = Corpora.languages9000();

        // final int nTrain = Integer.MAX_VALUE;
        final int nTrain = 100; // 14000;
        // final int nTrain = 1;
        // final int nTest = Integer.MAX_VALUE;
        final int nTest = 30;
        final boolean leftFirst = true;
        // final boolean leftFirst = false;
        final boolean earlyReduce = false;
        // final boolean earlyReduce = true;
        final boolean strict = false;
        // final boolean strict = true;
        final boolean dynamic = true;
        // final boolean dynamic = false;
        /* If nonprojectiveAllowed == true, then all trees are allowable. Which means that for
         * methods that rely on the trees being projective, they will be projectivized.
         * Otherwise, non-projective trees from training corpus are ignored.
         * They are allowable in a dynamic parser depending on dynProjectivize (see below).
         */
        // final boolean nonprojectiveAllowed = true;
        final boolean nonprojectiveAllowed = false;
        /* If dynProjectivize == true and nonprojectiveAllowed == true, then
         * a dynamic parser projectivizes.
         */
        final boolean dynProjectivize = true;
        // final boolean dynProjectivize = false;
        final boolean goldPos = true;

        if (langs == null)
            testTrainingAndTestingAll(method, leftFirst, strict, dynamic,
                    nonprojectiveAllowed, dynProjectivize, goldPos);
        else if (method.equals("simple"))
            doTrainingAndTestingSimple(langs, nTrain, nTest, leftFirst, strict, dynamic,
                    nonprojectiveAllowed, dynProjectivize, goldPos);
        else if (method.equals("swap"))
            doTrainingAndTestingSwap(langs, nTrain, nTest, leftFirst, goldPos);
        else if (method.equals("hat"))
            doTrainingAndTestingHat(langs, nTrain, nTest, leftFirst, dynamic, false, goldPos);
        else if (method.equals("hatminus"))
            doTrainingAndTestingHat(langs, nTrain, nTest, leftFirst, dynamic, true, goldPos);
        else if (method.equals("hatprob"))
            doTrainingAndTestingHatProb(langs, nTrain, nTest, leftFirst, dynamic, false, goldPos);
        if (!method.equals("hatprob"))
            measureNonprojectivity(langs);
        leaveParameters(method, nTrain, nTest,
                leftFirst, earlyReduce, strict, dynamic, nonprojectiveAllowed, dynProjectivize, goldPos);
    }

    private static void doWholeHat() {
        final int nTrain = Integer.MAX_VALUE;
        // final int nTrain = 100;
        // final int nTrain = 1000;
        final int nTest = Integer.MAX_VALUE;
        // final String[] langs = { "ar", "cs", "de", "et", "grc", "ja", "ko", "no" , "ru" };
        final String[] langs = { "ko" };
        doTrainingAndTestingWholeHat(langs, nTrain, nTest);
    }

    private static void testDynamicSpeed() {
        final String lang = "de";
        final int nTrain = Integer.MAX_VALUE;
        // final int nTrain = 1000;
        final int nTest = Integer.MAX_VALUE;
        final boolean leftFirst = true;
        final boolean strict = false;
        final boolean projectivize = true;
        final boolean dynProjectivize = true;
        // final boolean dynProjectivize = false;
        final boolean goldPos = true;
        doTrainingAndTestingSimpleDynamicSpeed(lang, nTrain, nTest, leftFirst, strict,
                projectivize, dynProjectivize, goldPos);
		/*
		leaveParameters("speed", nTrain, nTest,
				leftFirst, false, strict, true, nonprojectiveAllowed, goldPos);
				*/
    }

    private static void measureNonprojectivity(final String[] langs) {
        for (String lang : langs) {
            String goldFile = tmp + "gold-" + lang + ".conllu";
            String parsedFile = tmp + "parsed-" + lang + ".conllu";
            Treebank goldBank = new Treebank(goldFile);
            Treebank parsedBank = new Treebank(parsedFile);
            String goldNonproj = String.format("%5.1f", goldBank.percentNonprojTrees());
            String parsedNonproj = String.format("%5.1f", parsedBank.percentNonprojTrees());
            reportLogFile("For " + lang + " gold has " + goldNonproj + "% nonprojectivity");
            reportLogFile("For " + lang + " parsed has " + parsedNonproj + "% nonprojectivity");
        }
    }

    // Dump parameters in file.
    private static void leaveParameters(final String method,
                                        final int nTrain,
                                        final int nTest,
                                        final boolean leftFirst,
                                        final boolean earlyReduce,
                                        final boolean strict,
                                        final boolean dynamic,
                                        final boolean projectivize,
                                        final boolean dynProjectivize,
                                        final boolean goldPos) {
        String params = method +
                (nTrain < Integer.MAX_VALUE ? "-" + nTrain : "") +
                (nTest < Integer.MAX_VALUE ? "-" + nTest : "") +
                ((method.equals("simple") ||
                        method.equals("swap") ||
                        method.equals("hat") ||
                        method.equals("hatminus")) &&
                        !leftFirst ? "-rightfirst" : "") +
                (method.equals("simple") && leftFirst &&
                        strict ? "-strict" : "") +
                (dynamic ? "-dyn" : "") +
                (projectivize ? "-proj" : "") +
                (!dynProjectivize ? "-dynunproj" : "") +
                (!goldPos ? "-inducedpos" : "") +
                "-";
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(tmp + "/params.txt", "UTF-8");
        } catch (FileNotFoundException e) {
            fail("Cannot create file: " + e);
            return;
        } catch (UnsupportedEncodingException e) {
            fail("Unsupported encoding: " + e);
            return;
        }
        writer.println(params);
        writer.close();
    }

    /**
     * Investigate all corpora. Print sizes of training sets (divided into
     * projective and nonprojective) and test sets.
     */
    private static void countCorpora() {
        final int trainMin = 0;
        // final int trainMin = 9000;
        final int trainProjMin = 0;
        // final int trainProjMin = 10;
        final int testMin = 0;
        // final int trainProjMin = 10;
        final TreeMap<String, String> langData = new TreeMap<>();
        for (File dir : Corpora.dirs()) {
            final File train = Corpora.train(dir);
            final File test = Corpora.test(dir);
            if (train != null && test != null) {
                final String dirName = dir.getName();
                final String trainName = train.getName();
                String[] dirParts = dirName.split("_|-");
                String sub = dirParts[dirParts.length - 1];
                String fullName = dirName.substring(3,
                        dirName.length() - 1 - sub.length());
                String[] trainParts = trainName.split("_|-");
                String lang = trainParts[0];
                final Treebank trainBank = new Treebank(train);
                final Treebank testBank = new Treebank(test);
                final int trainSize = trainBank.nTrees();
                final int testSize = testBank.nTrees();
                final int trainWordSize = trainBank.nWords();
                final int testWordSize = testBank.nWords();
                final int trainNProjSize = trainBank.nNonprojTrees();
                final int trainProjSize = trainSize - trainNProjSize;
                final double nprojPercent = trainBank.percentNonprojTrees();
                if (trainSize >= trainMin &&
                        trainProjSize >= trainProjMin &&
                        testSize >= testMin) {
                    final String moreData = sub +
                            " train " + trainSize + " (" + trainWordSize + ")" + " = " +
                            trainNProjSize + " nproj + " + trainProjSize + " proj " +
                            nprojPercent + "% " + "/" +
                            " test " + testSize + " (" + testWordSize + ")";
                    if (langData.get(lang) == null)
                        langData.put(lang, lang + " " + fullName);
                    langData.put(lang, langData.get(lang) + "\n   " + moreData);
                }
            }
        }

        for (String data : langData.values()) {
            System.out.println(data);
        }
        String langList = "";
        for (String lang : langData.keySet()) {
            if (!langList.equals(""))
                langList += ", ";
            langList += "\"" + lang + "\"";
        }
        System.out.println(langList);
    }

    private static void projectivizeCorpus() {
        Projectivizer projectivizer = new Projectivizer();
        final int n = 50000000;
        String lang = "de";
        String inFile = Corpora.trainFile(lang);
        String projFile = tmp + "proj-" + lang + ".conllu";
        projectivizer.projectivize(inFile, projFile, n);
    }

    private static void projectivizeCorpora() {
        final int n = 5000;
        Projectivizer projectivizer = new Projectivizer();
        final String[] langs = Corpora.someLanguages();
        for (String lang : langs) {
            String inFile = Corpora.trainFile(lang);
            String copyFile = tmp + "train-" + lang + ".conllu";
            String projFile = tmp + "proj-" + lang + ".conllu";
            copyCorpus(inFile, copyFile, n);
            projectivizer.projectivize(inFile, projFile, n);
        }
        for (String lang : langs) {
            String inFile = Corpora.testFile(lang);
            copyCorpus(inFile, tmp + "gold-" + lang + ".conllu");
        }
    }

    private static void projectivizeAllCorpora() {
        final Projectivizer projectivizer = new Projectivizer();
        final TimerMilli timer = new TimerMilli();
        int nTrees = 0;
        for (String file : Corpora.trainFiles()) {
            final Treebank treebank = new Treebank(file);
            nTrees += treebank.depStructs.length;
            timer.start();
            projectivizer.projectivize(treebank);
            timer.stop();
        }
        reportFine("Projectivizing of " +
                Corpora.trainFiles().length + " files " +
                "and " + nTrees + " trees " +
                "took " + timer.seconds() + " s " +
                "which is " + (timer.millis() * 1.0 / nTrees) + " ms per tree");
    }

    private static void projectivizeCorpusCount() {
        // final String lang = "grc";
        // final String lang = "nl";
        // final String lang = "de";
        final String lang = "ja";
        final Projectivizer projectivizer = new Projectivizer();
        String file = Corpora.trainFile(lang);
        final Treebank treebank = new Treebank(file);
        String counts = projectivizer.projectivizeCount(treebank);
        System.out.println(counts);
    }

    private static int arg1 = 0;

    /**
     * Main method for testing.
     *
     * @param args
     */
    public static void main(String[] args) {
        if (args.length > 0)
            arg1 = Integer.parseInt(args[0]);
        setLoggerHandler();
        clearLogFile();

//        countCorpora();
//        testTreebankReading();
        doMethod();
//        doWholeHat();
//        testDynamicSpeed();
//        trainHatPrintAnalysisAll();
//        projectivizeCorpus();
//        projectivizeCorpora();
//        projectivizeAllCorpora();
//        projectivizeCorpusCount();
    }

    /**
     * Normalise and copy corpus.
     *
     * @param corpus
     * @param corpusCopy
     */
    private static void copyCorpus(final String corpus,
                                   final String corpusCopy,
                                   final int n) {
        final Treebank treebank = new Treebank(corpus, n);
        PrintWriter copyWriter = null;
        try {
            copyWriter = new PrintWriter(corpusCopy, "UTF-8");
        } catch (FileNotFoundException e) {
            fail("Cannot create file: " + e);
            return;
        } catch (UnsupportedEncodingException e) {
            fail("Unsupported encoding: " + e);
            return;
        }
        for (DependencyStructure struct : treebank.depStructs) {
            final Token[] tokens = struct.getNormalTokens();
            final DependencyStructure goldStruct =
                    new DependencyStructure(tokens);
            copyWriter.println(goldStruct);
        }
        copyWriter.close();
    }

    private static void copyCorpus(final String corpus,
                                   final String corpusCopy) {
        copyCorpus(corpus, corpusCopy, Integer.MAX_VALUE);
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
