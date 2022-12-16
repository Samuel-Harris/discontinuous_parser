/*
 * Copyright (c) 2019. University of St Andrews
 */

package standrews.constmain;

import javafx.util.Pair;
import standrews.aux_.TimerMilli;
import standrews.classification.FeatureSpecification;
import standrews.classification.FeatureVectorGenerator;
import standrews.constextract.HatExtractor;
import standrews.constextract.SimpleExtractor;
import standrews.constbase.ConstTree;
import standrews.constbase.ConstTreebank;
import standrews.constmethods.DeterministicParser;
import standrews.constmethods.HatParser;
import standrews.constmethods.SimpleParser;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

public class SimpleTrainer {

    protected FeatureVectorGenerator featureVectorGenerator;
    protected final int maxEpochs;
    protected final double tol;

    public SimpleTrainer(final FeatureVectorGenerator featureVectorGenerator, int maxEpochs, double tol) {
        this.featureVectorGenerator = featureVectorGenerator;
        this.maxEpochs = maxEpochs;
        this.tol = tol;
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

                extractor.train();
                miniBatchOptional = treebank.getNextTrainMiniBatch();
            }

            treebank.resetTrainTreebankIterator();
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
