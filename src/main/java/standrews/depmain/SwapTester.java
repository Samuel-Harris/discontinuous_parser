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
import standrews.depmethods.DeterministicParser;
import standrews.depmethods.SwapParser;

public class SwapTester extends SimpleTester {

    public SwapTester(final FeatureSpecification featSpec) {
        super(featSpec);
    }

    protected DeterministicParser makeParser(final Token[] tokens) {
        return new SwapParser(tokens);
    }
}
