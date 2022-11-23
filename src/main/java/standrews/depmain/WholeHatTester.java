/*
 * Copyright (c) 2019. University of St Andrews
 */

package standrews.depmain;

import standrews.aux.PropertyWeights;
import standrews.depbase.DependencyStructure;
import standrews.depbase.Token;
import standrews.depbase.Treebank;
import standrews.depextract.WholeHatExtractor;
import standrews.depmethods.WholeHatParser;

import java.lang.invoke.MethodHandles;
import java.util.logging.Logger;

public class WholeHatTester {

    public static final String[] weightedProperties = WholeHatParser.weightedProperties;

    public int test(final String corpus, final int n,
                    final WholeHatExtractor extractor) {
        final Treebank treebank = new Treebank(corpus, n);
        int i = 0;
        PropertyWeights weights = new PropertyWeights(weightedProperties);
        for (DependencyStructure struct : treebank.depStructs) {
            final Token[] tokens = struct.getNormalTokens();
            WholeHatParser parser = new WholeHatParser(tokens);
            weights.add(parser.prob(extractor));
            i++;
        }
        // reportFine(weights.toString(treebank.nWords()));
        reportFine("stackhat " + String.format("%3.1e",
                weights.get(WholeHatParser.stackhatWeight) / treebank.nWords()));
        reportFine("rulehat " + String.format("%3.1e",
                weights.get(WholeHatParser.rulehatWeight) / treebank.nWords()));
        reportFine("left " + String.format("%3.1e",
                weights.get(WholeHatParser.gapLeftWeight) / treebank.nWords()));
        reportFine("right " + String.format("%3.1e",
                weights.get(WholeHatParser.gapRightWeight) / treebank.nWords()));
        reportFine("expand " + String.format("%3.2e",
                (weights.get(WholeHatParser.expandLeftWeight) + weights.get(WholeHatParser.expandRightWeight))
                        / treebank.nWords()));
        reportFine("norm " + String.format("%3.1e",
                (weights.get(WholeHatParser.normWeight))
                        / treebank.nWords()));
        reportFine("sum " + String.format("%3.2e", weights.sum() / treebank.nWords()));
        reportFine("plexhat " + String.format("%3.1e", extractor.pLexhat()));
        reportFine("prulehat " + String.format("%3.1e", extractor.pRulehat()));
        reportFine("pleft " + String.format("%3.1e", extractor.pLeft()));
        reportFine("pright " + String.format("%3.1e", extractor.pRight()));
        return i;
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

    private static void reportFine(final String message) {
        logger().fine(message);
    }

}
