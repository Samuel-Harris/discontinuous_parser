/*
 * Copyright (c) 2019. University of St Andrews
 */

package standrews.depmain;

import standrews.classification.FeatureSpecification;
import standrews.depbase.DependencyStructure;
import standrews.depbase.Token;
import standrews.depbase.Treebank;
import standrews.depextract.SimpleExtractor;
import standrews.depmethods.SimpleParser;

import java.lang.invoke.MethodHandles;
import java.util.logging.Logger;

public abstract class ProbTester {

    protected FeatureSpecification featSpec;

    public ProbTester(final FeatureSpecification featSpec) {
        this.featSpec = featSpec;
    }

    public int test(final String corpus, final int n,
                    final SimpleExtractor extractor) {
        final Treebank treebank = new Treebank(corpus, n);
        int i = 0;
        double weight = 0;
        for (DependencyStructure struct : treebank.depStructs) {
            final Token[] tokens = struct.getNormalTokens();
            SimpleParser parser = makeParser(tokens);
            weight += parser.negLogProbability(extractor);
            i++;
        }
        reportFine("sum " + String.format("%3.2e", weight / treebank.nWords()));
        return i;
    }

    protected abstract SimpleParser makeParser(final Token[] tokens);

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

    private static void reportFine(final String message) {
        logger().fine(message);
    }
}
