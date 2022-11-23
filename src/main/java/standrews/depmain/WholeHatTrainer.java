/*
 * Copyright (c) 2019. University of St Andrews
 */

package standrews.depmain;

import standrews.depmethods.WholeHatParser;
import standrews.depbase.DependencyStructure;
import standrews.depbase.Token;
import standrews.depbase.Treebank;
import standrews.depextract.WholeHatExtractor;

public class WholeHatTrainer {

    public int train(final String corpus, final int n,
                     final WholeHatExtractor extractor) {
        final Treebank treebank = new Treebank(corpus, n);
        int i = 0;
        for (DependencyStructure struct : treebank.depStructs) {
            final Token[] tokens = struct.getNormalTokens();
            WholeHatParser parser = new WholeHatParser(tokens);
            parser.observe(extractor);
            i++;
        }
        return i;
    }

}
