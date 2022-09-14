/*
 * Copyright (c) 2018. University of St Andrews
 */

package standrews.tabular;

import standrews.depbase.Id;
import standrews.depbase.Token;

import java.util.TreeMap;

public class OptimalProjectivizer {
	public static Token[] projectivize(final Token[] tokens) {
		CubicSplitBilex parser = parse(tokens);
		TreeMap<Integer,Integer> parent = parser.decode(AllBilexGrammar.any);
		Token[] newTokens = new Token[tokens.length];
		for (int i = 0; i < tokens.length; i++) {
			final int par = parent.get(i+1) != null ? parent.get(i+1) : 0;
			newTokens[i] = tokens[i].getParented(new Id("" + par), tokens[i].deprel);
		}
		return newTokens;
	}

	public static int countProjectivization(final Token[] tokens) {
		CubicSplitBilex parser = parse(tokens);
		return parser.countDecode(AllBilexGrammar.any);
	}

	private static CubicSplitBilex parse(final Token[] tokens) {
		AllBilexGrammar gram = new AllBilexGrammar();
		String[] in = AllBilexGrammar.getInput(tokens.length + 1);
		return new CubicSplitBilex(gram, in) {
			@Override
			protected int weight(int i, int j) {
				if (j == 0)
					return zero();
				else if (tokens[j - 1].head.major == i)
					return 1;
				else
					return one();
			}
		};
	}
}
