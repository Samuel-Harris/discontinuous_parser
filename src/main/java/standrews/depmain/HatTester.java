/*
 * Copyright (c) 2018. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package standrews.depmain;

import standrews.depautomata.HatConfig;
import standrews.depautomata.SimpleConfig;
import standrews.classification.FeatureSpecification;
import standrews.depbase.Token;
import standrews.depmethods.DeterministicParser;
import standrews.depmethods.HatParser;

public class HatTester extends SimpleTester {

    public HatTester(final FeatureSpecification featSpec) {
        super(featSpec);
    }

    private class ParserWithCompression extends HatParser {
        public ParserWithCompression(final Token[] tokens,
                                     final int viewMin,
                                     final int viewMax) {
            super(tokens, viewMin, viewMax);
        }

        protected void apply(final SimpleConfig simpleConfig, final String[] action) {
            HatConfig config = (HatConfig) simpleConfig;
            final String[] actionUncompressed = actionFromCompression(config, action);
            super.apply(config, actionUncompressed);
        }

        protected boolean applicable(final SimpleConfig simpleConfig, final String[] action) {
            HatConfig config = (HatConfig) simpleConfig;
            final String[] actionUncompressed = actionFromCompression(config, action);
            return actionUncompressed.length > 0 &&
                    super.applicable(config, actionUncompressed);
        }
    }

    protected DeterministicParser makeParser(final Token[] tokens) {
        final int viewMin = featSpec.getIntFeature("viewMin", 0);
        final int viewMax = featSpec.getIntFeature("viewMax", 0);
        return new ParserWithCompression(tokens, viewMin, viewMax);
    }
}
