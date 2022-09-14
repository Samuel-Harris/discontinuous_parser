/*
 * Copyright (c) 2018. University of St Andrews
 */

package standrews.tabular;

/**
 * Bilexical grammar intended to analyze shift-reduce dependency parsing.
 */
public class ComplexShiftReduceBilexGrammar extends SplitBilexGrammar {

	// in prefix, but not last two
	public static final String pre = "pre";
	// in prefix, but not last two, having left children
	public static final String preLeft = "preLeft";
	// penultimate in prefix
	public static final String pen = "pen";
	// ultimate in prefix
	public static final String ult = "ult";
	// anywhere in suffix
	public static final String suf = "suf";

	// ultimate and right children
	public static final String ultAndRight = "ult and right";
	// ultimate and left children
	public static final String ultAndLeft = "ult and left";
	// ultimate having no left or right children
	public static final String ultNochildren = "ult no children";
	// ultimate having only right children
	public static final String ultChildren = "ult children";
	// ultimate having only penultimate as child
	public static final String ultPen = "ult pen";
	// ultimate having penultimate as left child, and having right children
	public static final String ultPenChildren = "ult pen children";

	// penultimate having ultimate as child, which is without right children
	public static final String penAndUltNochildren = "pen and ult no children";
	// penultimate having ultimate as child, which is with right children
	public static final String penAndUltChildren = "pen and ult children";
	// penultimate having other right children
	public static final String penAndRight = "pen and right";
	// penultimate having left children
	public static final String penAndLeft = "pen and left";
	// penultimate having no children
	public static final String penNochildren = "pen no children";
	// penultimate having ultimate as child, which is without right children
	public static final String penUltNochildren = "pen ult no children";
	// penultimate having ultimate as child, which is with right children
	public static final String penUltChildren = "pen ult children";

	public static String[] delexs = new String[] {pre, preLeft, pen, ult, suf,
			ultAndRight, ultAndLeft, ultNochildren, ultChildren, ultPen, ultPenChildren,
			penAndUltNochildren, penAndUltChildren, penAndRight, penAndLeft,
			penNochildren, penUltNochildren, penUltChildren
	};

	/**
	 *
	 * @param leftDependentsFirst left children attached before right children
	 * @param strict attachment order to be enforced
	 */
	public ComplexShiftReduceBilexGrammar(final boolean leftDependentsFirst,
										  final boolean strict) {
		super(delexs);
		final boolean restricted = leftDependentsFirst && strict;
		addRight(pre, suf, suf);
		addRight(pre, ultPen, ultPen);
		addRight(ultPen, suf, ultPen);
		addRight(pre, ultPenChildren, ultPenChildren);
		addRight(ultPenChildren, suf, ultPenChildren);
		addRight(pre, penUltNochildren, penUltNochildren);
		addRight(penUltNochildren, suf, penUltNochildren);
		addRight(pre, penUltChildren, penUltChildren);
		addRight(penUltChildren, suf, penUltChildren);
		if (!restricted) {
			addLeft(pre, pre, preLeft);
			addLeft(preLeft, pre, preLeft);
		}
		addMiddle(pre, pre, pre);
		addMiddle(pre, suf, suf);
		addMiddle(pre, ultPen, ultPen);
		addMiddle(pre, ultPenChildren, ultPenChildren);
		addMiddle(pre, penUltNochildren, penUltNochildren);
		addMiddle(pre, penUltChildren, penUltChildren);
		if (!restricted) {
			addMiddle(preLeft, suf, suf);
			addMiddle(preLeft, ultPen, ultPen);
			addMiddle(preLeft, ultPenChildren, ultPenChildren);
			addMiddle(preLeft, penUltNochildren, penUltNochildren);
			addMiddle(preLeft, penUltChildren, penUltChildren);
		}

		addRight(suf, suf, suf);
		addLeft(suf, suf, suf);
		addLeft(suf, pre, suf);
		addLeft(suf, ultPen, ultPen);
		addLeft(ultPen, pre, ultPen);
		addLeft(suf, ultPenChildren, ultPenChildren);
		addLeft(ultPenChildren, pre, ultPenChildren);
		addLeft(suf, ultNochildren, suf);
		addLeft(suf, ultChildren, suf);
		addLeft(suf, penNochildren, suf);
		addLeft(suf, penUltNochildren, penUltNochildren);
		addLeft(penUltNochildren, pre, penUltNochildren);
		addLeft(suf, penUltChildren, penUltChildren);
		addLeft(penUltChildren, pre, penUltChildren);
		addMiddle(suf, suf, suf);
		addMiddle(ultPen, suf, ultPen);
		addMiddle(ultPenChildren, suf, ultPenChildren);
		addMiddle(penUltNochildren, suf, penUltNochildren);
		addMiddle(penUltChildren, suf, penUltChildren);

		addRight(ult, suf, ultAndRight);
		addRight(ultAndRight, suf, ultAndRight);
		addLeft(ult, penNochildren, ultAndLeft);
		addLeft(ultAndLeft, pre, ultAndLeft);
		addMiddle(ult, ult, ultNochildren);
		addMiddle(ult, ultAndRight, ultChildren);
		addMiddle(ultAndLeft, ult, ultPen);
		addMiddle(ultAndLeft, ultAndRight, ultPenChildren);

		addRight(pen, ultNochildren, penAndUltNochildren);
		addRight(penAndUltNochildren, suf, penAndUltNochildren);
		addRight(pen, ultChildren, penAndUltChildren);
		addRight(penAndUltChildren, suf, penAndUltChildren);
		addRight(pen, suf, penAndRight);
		addRight(penAndRight, suf, penAndRight);
		if (!restricted) {
			addLeft(pen, pre, penAndLeft);
			addLeft(penAndLeft, pre, penAndLeft);
		}
		addMiddle(pen, pen, penNochildren);
		addMiddle(pen, penAndUltNochildren, penUltNochildren);
		addMiddle(pen, penAndUltChildren, penUltChildren);
		addMiddle(pen, penAndRight, suf);
		if (!restricted) {
			addMiddle(penAndLeft, penAndUltNochildren, penUltNochildren);
			addMiddle(penAndLeft, penAndUltChildren, penUltChildren);
			addMiddle(penAndLeft, penAndRight, suf);
		}
	}

	public static String[] getInput(final int prefixLen, final int suffixLen) {
		final String[] in = new String[prefixLen + suffixLen];
		for (int i = 0; i < prefixLen-2; i++) {
			in[i] = pre;
		}
		in[prefixLen-2] = pen;
		in[prefixLen-1] = ult;
		for (int j = prefixLen; j < prefixLen + suffixLen; j++) {
			in[j] = suf;
		}
		return in;
	}
}
