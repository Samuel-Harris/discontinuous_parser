/*
 * Copyright (c) 2019. University of St Andrews
 */

package standrews.depmethods;

import standrews.depautomata.DependencyVertex;
import standrews.depautomata.SimpleConfig;
import standrews.depextract.ArcEagerExtractor;
import standrews.depbase.Token;
import standrews.tabular.CubicSplitBilexReuse;
import standrews.tabular.ArcEagerBilexGrammar;

import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ArcEagerDynamicParserCubic extends ArcEagerDynamicParser {
	/**
	 * Simple grammar for parsing configuration. Cached.
	 */
	private static ArcEagerBilexGrammar gram = null;

	public ArcEagerDynamicParserCubic(final Token[] tokens,
									  final boolean early,
									  final boolean strict,
									  final ArcEagerExtractor preliminaryExtractor) {
		super(tokens, early, strict, preliminaryExtractor);
		chooseGrammar();
	}

	private void chooseGrammar() {
		gram = new ArcEagerBilexGrammar();
		gram.cacheNumMaps();
	}

	protected TreeMap<String, Integer> scores(final SimpleConfig config) {
		final TreeMap<String,Integer> scores = scores();
		if (applicable(config, shift())) {
			final SimpleConfig next = new SimpleConfig(config);
			shift(next);
			int reduceBlockMin = -1;
			int reduceBlockMax = -1;
			if (earlyReduce) {
				reduceBlockMin = strict ? 0 : next.prefixLength()-2;
				reduceBlockMax = next.prefixLength()-1;
			}
			scores.put(shift, nextScore(next, reduceBlockMin, reduceBlockMax, false, false));
		}
		if (applicable(config, rightArc())) {
			final SimpleConfig next = new SimpleConfig(config);
			rightArc(next, "_");
			scores.put(rightArc, nextScore(next, -1, -1, false, false));
		}
		if (applicable(config, leftArc())) {
			final DependencyVertex head = config.getSuffixLeft(0);
			final DependencyVertex dep = config.getPrefixRight(0);
			final int w1 = edgeWeight(head, dep);
			final SimpleConfig next = new SimpleConfig(config);
			leftArc(next, "_");
			final int w2 = nextScore(next, -1, -1, false, true);
			scores.put(leftArc, times(w1, w2));
		}
		if (applicable(config, reduce())) {
			final DependencyVertex dep = config.getPrefixRight(0);
			final DependencyVertex head = config.getPrefixRight(1);
			final int w1 = edgeWeight(head, dep);
			final SimpleConfig next = new SimpleConfig(config);
			reduce(next);
			final int w2 = nextScore(next, -1, -1, !earlyReduce, false);
			scores.put(reduce, times(w1, w2));
		}
		if (applicable(config, reduceCorrect())) {
			final DependencyVertex dep = config.getPrefixRight(0);
			final DependencyVertex head = config.getPrefixRight(1);
			final int w1 = edgeWeight(head, dep);
			final SimpleConfig next = new SimpleConfig(config);
			reduceCorrect(next, "_");
			final int w2 = nextScore(next, -1, -1, !earlyReduce, false);
			scores.put(reduceCorrect, times(w1, w2));
		}
		return scores;
	}

	protected int nextScore(final SimpleConfig next,
							final int reduceBlockMin,
							final int reduceBlockMax,
							final boolean shiftBlock,
							final boolean leftRightBlock) {
		final int initN = 117;
		final List<DependencyVertex> combiList =
				Stream.concat(next.prefixList().stream(),
						next.suffixList().stream())
						.collect(Collectors.toList());
		final String[] s = ArcEagerBilexGrammar.getInput(next.labelList(),
				next.suffixLength(), reduceBlockMin, reduceBlockMax, shiftBlock, leftRightBlock);
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
		// parser.printTable();
		return parser.rootWeight(ArcEagerBilexGrammar.R);
	}
}
