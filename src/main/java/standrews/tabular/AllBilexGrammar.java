/*
 * Copyright (c) 2018. University of St Andrews
 */

package standrews.tabular;

/**
 * Bilexical grammar for exhaustive parsing.
 */
public class AllBilexGrammar extends SplitBilexGrammar {

    public static final String any = "any";
    public static String[] delexs = new String[]{any};

    public AllBilexGrammar() {
        super(delexs);
        addRight(any, any, any);
        addLeft(any, any, any);
        addMiddle(any, any, any);
    }

    public static String[] getInput(final int len) {
        final String[] in = new String[len];
        for (int i = 0; i < len; i++) {
            in[i] = any;
        }
        return in;
    }

}
