/*
 * Copyright (c) 2018. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package standrews.depmain;

import standrews.depautomata.DependencyGraph;
import standrews.classification.FeatureSpecification;
import standrews.depextract.SimpleExtractor;
import standrews.depbase.*;
import standrews.depmethods.DeterministicParser;
import standrews.depmethods.SimpleParser;
import standrews.tabular.OptimalProjectivizer;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.lang.invoke.MethodHandles;
import java.util.logging.Logger;

public class SimpleTrainer {

    protected FeatureSpecification featSpec;

    public SimpleTrainer(final FeatureSpecification featSpec) {
        this.featSpec = featSpec;
    }

    protected boolean leftDependentsFirst = false;

    protected boolean strict = false;

    protected boolean nonprojectiveAllowed = false;

    public void setLeftDependentsFirst(final boolean b) {
        leftDependentsFirst = b;
    }

    public void setStrict(final boolean b) {
        strict = b;
    }

    public void setNonprojectiveAllowed(final boolean p) {
        nonprojectiveAllowed = p;
    }

    public int train(final String corpus,
                     final int n, final SimpleExtractor extractor) {
        return train(corpus, null, n, extractor);
    }

    /**
     * Train classifiers in extractor.
     *
     * @param corpus     File with corpus.
     * @param corpusCopy Filename of copy of part of corpus used for training.
     * @param n          Number of sentences for training.
     * @param extractor  Extractor of features.
     * @return How many sentences were used in training.
     */
    public int train(final String corpus,
                     final String corpusCopy,
                     final int n, final SimpleExtractor extractor) {
        copyTraining(corpus, corpusCopy, n);
        final Treebank treebank = makeTreebank(corpus, n);
        int i = 0;
        for (int epo = 0; epo == 0 || extractor.getContinuousTraining() && epo < extractor.getNEpochs(); epo++) {
            if (extractor.getContinuousTraining())
                reportFine("Epoch " + epo);
            i = 0;
            for (DependencyStructure struct : treebank.depStructs) {
                final Token[] tokens = retaggedTokens(struct);
                if (allowableTree(tokens, i, n)) {
                    DeterministicParser parser = makeParser(tokens);
                    parser.observe(extractor);
                    i++;
                }
            }
            extractor.train();
        }
        return i;
    }

    /**
     * Make exact copy of corpus used for training.
     *
     * @param corpus
     * @param corpusCopy
     * @param n
     */
    private void copyTraining(final String corpus,
                              final String corpusCopy,
                              final int n) {
        final Treebank treebank = makeTreebank(corpus, n);
        PrintWriter trainWriter = null;
        try {
            if (corpusCopy != null)
                trainWriter = new PrintWriter(corpusCopy, "UTF-8");
        } catch (FileNotFoundException e) {
            fail("Cannot create file: " + e);
        } catch (UnsupportedEncodingException e) {
            fail("Unsupported encoding: " + e);
        }
        int i = 0;
        for (DependencyStructure struct : treebank.depStructs) {
            final Token[] tokens = retaggedTokens(struct);
            if (allowableTree(tokens, i, n)) {
                if (trainWriter != null) {
                    final DependencyStructure normalized =
                            new DependencyStructure(tokens);
                    trainWriter.println(normalized);
                }
                i++;
            }
        }
        if (trainWriter != null)
            trainWriter.close();
    }

    protected Token[] retaggedTokens(DependencyStructure struct) {
        final Token[] tokens = struct.getNormalTokens();
        if (!featSpec.getGoldPos()) {
            return tokens; // featSpec.getPosTagger().retag(tokens);
        } else {
            return tokens;
        }
    }

    protected Treebank makeTreebank(final String path, final int n) {
        return new Treebank(path, 5 * n);
    }

    protected boolean allowableTree(final Token[] tokens, final int i, final int n) {
        DependencyGraph g = new DependencyGraph(tokens);
        return i < n && (nonprojectiveAllowed || g.isProjective());
    }

    protected DeterministicParser makeParser(Token[] tokens) {
        if (nonprojectiveAllowed)
            tokens = OptimalProjectivizer.projectivize(tokens);
        final SimpleParser parser = new SimpleParser(tokens);
        parser.setLeftDependentsFirst(leftDependentsFirst);
        return parser;
    }

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
        logger().fine(message);
    }
}
