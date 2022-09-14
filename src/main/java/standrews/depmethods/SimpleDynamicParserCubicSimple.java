/*
 * Copyright (c) 2019. University of St Andrews
 */

package standrews.depmethods;

import standrews.depautomata.DependencyVertex;
import standrews.depautomata.SimpleConfig;
import standrews.aux.DataCollectionSum;
import standrews.depextract.SimpleExtractor;
import standrews.depbase.Token;
import standrews.tabular.CubicSplitBilexReuse;
import standrews.tabular.ShiftReduceBilexGrammar;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SimpleDynamicParserCubicSimple extends SimpleDynamicParser {

	/**
	 * Simple grammar for parsing configuration. Cached.
	 */
	private static ShiftReduceBilexGrammar gram = null;
	private static boolean gramLeftDependentsFirst = false;
	private static boolean gramStrict = false;

	public SimpleDynamicParserCubicSimple(final Token[] tokens,
									 final boolean leftDependentsFirst,
									 final boolean strict,
										  final SimpleExtractor preliminaryExtractor) {
		super(tokens, leftDependentsFirst, strict, preliminaryExtractor);
		chooseGrammar(leftDependentsFirst, strict);
	}

	private void chooseGrammar(final boolean leftDependentsFirst, final boolean strict) {
		if (gram == null ||
				gramLeftDependentsFirst != leftDependentsFirst ||
				gramStrict != strict) {
			gram = new ShiftReduceBilexGrammar(leftDependentsFirst, strict);
			gram.cacheNumMaps();
		}
	}

	protected TreeMap<String, Integer> scores(final SimpleConfig config) {
		final TreeMap<String,Integer> scores = scores();
		final ArrayList<DependencyVertex> prefixList = config.prefixList();
		ArrayList<DependencyVertex> suffixList = config.suffixList();
		int extraEdges = 0;

		if (isProjective() && suffixList.size() > 0) {
			ArrayList<DependencyVertex> prunedList = prunedSuffix(
					new ArrayList<>(suffixList.subList(1, suffixList.size())));
			prunedList.add(0, suffixList.get(0));
			extraEdges = suffixList.size() - prunedList.size();
			suffixList = prunedList;
		}

		final List<DependencyVertex> combiList =
				Stream.concat(prefixList.stream(), suffixList.stream())
                             .collect(Collectors.toList());
		final int m = prefixList.size();
		final int n = suffixList.size();
		final int initN = 117;

		DataCollectionSum.globalCollection.add("suffix", config.totalLength(), config.suffixLength());
		DataCollectionSum.globalCollection.add("total", config.totalLength(), combiList.size());
		DataCollectionSum.globalCollection.add("pruned", config.suffixLength(), suffixList.size());

		if (m > 2) {
			final String[] inRight = ShiftReduceBilexGrammar.getInput(m-1, n);
			final CubicSplitBilexReuse parserRight = new CubicSplitBilexReuse(gram, inRight, initN) {
				public int weight(final int i, final int j) {
					final int ip = i < m-2 ? i : i+1;
					final int jp = j < m-2 ? j : j+1;
					if (!leftDependentsFirst && i == m-2 && j > m-2)
						return zero();
					else
						return edgeWeight(combiList.get(ip), combiList.get(jp));
				}
			};
			scores.put("reduceRight", times(times(extraEdges,
					parserRight.rootWeight(ShiftReduceBilexGrammar.suf)),
					edgeWeight(combiList.get(m-1), combiList.get(m-2))));
		}

		final String[] inLeft = ShiftReduceBilexGrammar.getInput(m-1, n);
		final CubicSplitBilexReuse parserLeft = new CubicSplitBilexReuse(gram, inLeft, initN) {
			public int weight(final int i, final int j) {
				final int ip = i < m-1 ? i : i+1;
				final int jp = j < m-1 ? j : j+1;
				if (leftDependentsFirst && strict && i == m-2 && j < m-2)
					return zero();
				else
					return edgeWeight(combiList.get(ip), combiList.get(jp));
			}
		};
		scores.put("reduceLeft", times(times(extraEdges,
				parserLeft.rootWeight(ShiftReduceBilexGrammar.suf)),
				edgeWeight(combiList.get(m-2), combiList.get(m-1))));

		if (n > 0) {
			final String[] inShift = ShiftReduceBilexGrammar.getInput(m + 1, n - 1);
			final CubicSplitBilexReuse parserShift = new CubicSplitBilexReuse(gram, inShift, initN) {
				public int weight(final int i, final int j) {
					if (leftDependentsFirst && i == m-1 && j < m-1)
						return zero();
					else
						return edgeWeight(combiList.get(i), combiList.get(j));
				}
			};
			scores.put("shift", times(extraEdges,
					parserShift.rootWeight(ShiftReduceBilexGrammar.suf)));
		}

		return scores;
	}

}
