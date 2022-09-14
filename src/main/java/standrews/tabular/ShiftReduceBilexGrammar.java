/*
 * Copyright (c) 2018. University of St Andrews
 */

package standrews.tabular;

public class ShiftReduceBilexGrammar extends SplitBilexGrammar {

	// in prefix, but not last
	public static final String pre = "pre";
	// last in prefix, or anywhere in suffix
	public static final String suf = "suf";

	// in prefix, but not last, having left children
	// public static final String preLeft = "preLeft";

	public static String[] delexs = new String[] {pre, suf};

	/**
	 *
	 * @param leftDependentsFirst left children attached before right children
	 * @param strict attachment order to be enforced
	 */
	public ShiftReduceBilexGrammar(final boolean leftDependentsFirst,
										  final boolean strict) {
		super(delexs);
		final boolean restricted = leftDependentsFirst && strict;
		addLeft(suf, pre, suf);
		addLeft(suf, suf, suf);
		addRight(pre, suf, suf);
		addRight(suf, suf, suf);
		if (!restricted) {
			// addLeft(pre, pre, preLeft);
			// addLeft(preLeft, pre, preLeft);
			addLeft(pre, pre, suf);
		}

		addMiddle(pre, pre, pre);
		addMiddle(pre, suf, suf);
		addMiddle(suf, suf, suf);
		if (!restricted) {
			// addMiddle(preLeft, suf, suf);
		}
	}

	public static String[] getInput(final int prefixLen, final int suffixLen) {
		final String[] in = new String[prefixLen + suffixLen];
		for (int i = 0; i < prefixLen-1; i++) {
			in[i] = pre;
		}
		in[prefixLen-1] = suf;
		for (int j = prefixLen; j < prefixLen + suffixLen; j++) {
			in[j] = suf;
		}
		return in;
	}
}
