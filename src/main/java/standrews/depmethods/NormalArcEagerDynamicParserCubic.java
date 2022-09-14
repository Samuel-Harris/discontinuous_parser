/*
 * Copyright (c) 2019. University of St Andrews
 */

package standrews.depmethods;

import standrews.depautomata.DependencyVertex;
import standrews.depautomata.SimpleConfig;
import standrews.depextract.NormalArcEagerExtractor;
import standrews.depbase.Token;
import standrews.tabular.CubicSplitBilexReuse;
import standrews.tabular.NormalArcEagerBilexGrammar;

import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class NormalArcEagerDynamicParserCubic extends NormalArcEagerDynamicParser {
	/**
	 * Simple grammar for parsing configuration. Cached.
	 */
	private static NormalArcEagerBilexGrammar gram = null;

	public NormalArcEagerDynamicParserCubic(final Token[] tokens,
								final NormalArcEagerExtractor preliminaryExtractor) {
		super(tokens, preliminaryExtractor);
		chooseGrammar();
	}

	private void chooseGrammar() {
		if (gram == null) {
			gram = new NormalArcEagerBilexGrammar();
			gram.cacheNumMaps();
		}
	}

	protected TreeMap<String, Integer> scores(final SimpleConfig config) {
		final TreeMap<String,Integer> scores = scores();
		if (applicable(config, shift())) {
			final SimpleConfig next = new SimpleConfig(config);
			shift(next);
			scores.put(shift, nextScore(next, false));
		}
		if (applicable(config, rightArc())) {
			final SimpleConfig next = new SimpleConfig(config);
			rightArc(next);
			scores.put(rightArc, nextScore(next, false));
		}
		if (applicable(config, leftArc())) {
			final SimpleConfig next = new SimpleConfig(config);
			leftArc(next);
			scores.put(leftArc, nextScore(next, true));
		}
		if (applicable(config, reduceLeft())) {
			final DependencyVertex head = config.getPrefixRight(1);
			final DependencyVertex dep = config.getPrefixRight(0);
			final int w1 = edgeWeight(head, dep);
			final SimpleConfig next = new SimpleConfig(config);
			reduceLeft(next, "_");
			final int w2 = nextScore(next, false);
			scores.put(reduceLeft, times(w1, w2));
		}
		if (applicable(config, reduceRight())) {
			final DependencyVertex head = config.getPrefixRight(0);
			final DependencyVertex dep = config.getPrefixRight(1);
			final int w1 = edgeWeight(head, dep);
			final SimpleConfig next = new SimpleConfig(config);
			reduceRight(next, "_");
			final int w2 = nextScore(next, false);
			scores.put(reduceRight, times(w1, w2));
		}
		return scores;
	}

	protected int nextScore(final SimpleConfig next, final boolean leftRightBlock) {
		final int initN = 117;
		final List<DependencyVertex> combiList =
				Stream.concat(next.prefixList().stream(),
						next.suffixList().stream())
						.collect(Collectors.toList());
		final String[] s = NormalArcEagerBilexGrammar.getInput(next.labelList(),
				next.suffixLength(), leftRightBlock);
		/*
		for (int j = 0; j < s.length; j++)
			System.out.print(s[j] + " ");
		System.out.println();
		*/
		final CubicSplitBilexReuse parser = new CubicSplitBilexReuse(gram, s, initN) {
		// final CubicSplitBilex parser = new CubicSplitBilex(gram, s) {
			public int weight(final int i, final int j) {
				return edgeWeight(combiList.get(i), combiList.get(j));
			}
		};
		return parser.rootWeight(NormalArcEagerBilexGrammar.Rtos);
	}
}
