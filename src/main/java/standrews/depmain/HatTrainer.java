/*
 * Copyright (c) 2018. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package standrews.depmain;

import standrews.classification.FeatureSpecification;
import standrews.depbase.Token;
import standrews.depbase.Treebank;
import standrews.depmethods.DeterministicParser;
import standrews.depmethods.HatParser;

public class HatTrainer extends SimpleTrainer {

    public HatTrainer(final FeatureSpecification featSpec) {
        super(featSpec);
    }

    protected Treebank makeTreebank(final String path, final int n) {
        return new Treebank(path, n);
    }

    protected boolean allowableTree(final Token[] tokens,
                                    final int i, final int n) {
        return true;
    }

    protected DeterministicParser makeParser(final Token[] tokens) {
        final int viewMin = featSpec.getIntFeature("viewMin", 0);
        final int viewMax = featSpec.getIntFeature("viewMax", 0);
        final HatParser parser = new HatParser(tokens, viewMin, viewMax);
        parser.setLeftDependentsFirst(leftDependentsFirst);
        return parser;
    }
}
