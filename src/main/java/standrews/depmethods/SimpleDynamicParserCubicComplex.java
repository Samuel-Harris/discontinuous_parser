/*
 * Copyright (c) 2019. University of St Andrews
 */

package standrews.depmethods;

import standrews.depautomata.DependencyVertex;
import standrews.depautomata.SimpleConfig;
import standrews.depextract.SimpleExtractor;
import standrews.depbase.Token;
import standrews.tabular.ComplexShiftReduceBilexGrammar;
import standrews.tabular.CubicSplitBilex;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SimpleDynamicParserCubicComplex extends SimpleDynamicParser {

	/**
	 * Complex grammar for parsing configuration.
	 */
	private ComplexShiftReduceBilexGrammar gram;

	public SimpleDynamicParserCubicComplex(final Token[] tokens,
									 final boolean leftDependentsFirst,
									 final boolean strict,
										   final SimpleExtractor preliminaryExtractor) {
		super(tokens, leftDependentsFirst, strict, preliminaryExtractor);
		gram = new ComplexShiftReduceBilexGrammar(leftDependentsFirst, strict);
	}

	protected TreeMap<String,Integer> scores(final SimpleConfig config) {
		final ArrayList<DependencyVertex> prefixList = config.prefixList();
		final ArrayList<DependencyVertex> suffixList = config.suffixList();
		final List<DependencyVertex> combiList =
				Stream.concat(prefixList.stream(), suffixList.stream())
                             .collect(Collectors.toList());
		final int m = prefixList.size();
		final int n = suffixList.size();
		final String[] in = ComplexShiftReduceBilexGrammar.getInput(m, n);
		final CubicSplitBilex parser = new CubicSplitBilex(gram, in) {
			public int weight(final int i, final int j) {
				return edgeWeight(combiList.get(i), combiList.get(j));
			}
		};
		final TreeMap<String,Integer> scores = scores();
		final String [] rightNonts =
				leftDependentsFirst ?
						new String[] {gram.ultPen, gram.ultPenChildren} :
						new String[] {gram.ultPen };
		final String [] leftNonts =
				new String[] {gram.penUltNochildren};
		final String[] shiftNonts =
				leftDependentsFirst ?
						new String[] {gram.suf, gram.penUltChildren} :
						new String[] {gram.suf, gram.ultPenChildren, gram.penUltChildren};
		for (String delex : rightNonts)
			scores.put("reduceRight",
				plus(scores.get("reduceRight"), parser.rootWeight(delex)));
		for (String delex : leftNonts)
			scores.put("reduceLeft",
				plus(scores.get("reduceLeft"), parser.rootWeight(delex)));
		for (String delex : shiftNonts)
			scores.put("shift",
				plus(scores.get("shift"), parser.rootWeight(delex)));
		return scores;
	}

}
