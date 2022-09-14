/*
 * Copyright (c) 2019. University of St Andrews
 */

package standrews.depmethods;

import standrews.depautomata.DependencyVertex;
import standrews.depextract.SimpleExtractor;
import standrews.depbase.Token;

import java.util.Vector;

public class SimpleDynamicParserLinear extends SimpleDynamicParserQuadratic {

	public SimpleDynamicParserLinear(final Token[] tokens,
									 final boolean leftDependentsFirst,
									 final boolean strict,
									 final SimpleExtractor preliminaryExtractor) {
		super(tokens, leftDependentsFirst, strict, preliminaryExtractor);
	}

	protected void printDiagnostics(int size, final PotentialGains gains) {
		/*
		if (size > 11) {
			System.out.println("LINEAR");
			printPotentialGains(gains);
		}
		*/
		/*
		int nonzeros = printPotentialGains(gains);
		DataCollectionSum.globalCollection.add("nonzeroslinear", size, nonzeros);
		*/
	}

	protected int scoreStack(final Vector<DependencyVertex> prefixList,
							 final Vector<DependencyVertex> suffixList,
							 final int i, final int j, final CompState state,
							 final PotentialGains gains, final boolean[] block,
							 final Analysis ana) {
		if (i < 0)
			return one();
		int g = gains.getStack(i, j, state);
		if (g != zero()) {
			return g;
		}
		g = one();

		CompState stateLeft = updateStack(state, j, i, ana);
		boolean childLoss = state != CompState.NONE &&
				ana.prefSufFirstChild[i] == ana.prefAndRightSufParentRepr(j);
		int gChildren = ana.prefSufFirstChildQualified(i, childLoss);
		int gLeft = times(times(gChildren,
				edgeWeight(prefixList.get(i), prefixList.get(j))),
				scoreStack(prefixList, suffixList, i - 1, i, stateLeft, gains, block, ana));
		g = plus(g, gLeft);

		if (!block[j]) {
			// int leftLink = leftLinkViaStack(i, j, ana);
			// if (leftLink < 0)
				// leftLink = leftLinkViaInput(i, j, ana);
			if (ana.prefPrefParent[i] == j) {
				int gRight = times(
					1,
						scoreStack(prefixList, suffixList, i-1, j, state, gains, block, ana));
				g = plus(g, gRight);
			} else if (hasleftLinkViaStack(i, j, ana)) {
			// if (leftLink >= 0) {
				int iLeft = skipLink(i, ana);
				int gRight = times(
						edgeWeight(prefixList.get(j), prefixList.get(i)),
						scoreStack(prefixList, suffixList, iLeft, j, state, gains, block, ana));
				g = plus(g, gRight);
			}
		}

		int shiftLink = ana.prefSufParent[j];
		if (shiftLink >= 0 && state != CompState.CONSUMED) {
			int gShift = times(
					state == CompState.NONE ? 1 : 0,
					scoreInput(prefixList, suffixList, i, shiftLink, gains, block, ana));
			g = plus(g, gShift);
		}

		gains.setStack(i, j, state, g);
		return g;
	}

	protected int scoreInput(final Vector<DependencyVertex> prefixList,
							 final Vector<DependencyVertex> suffixList,
							 final int i, final int j,
							 final PotentialGains gains, final boolean[] block,
							 final Analysis ana) {
		if (i < 0)
			return one();
		int g = gains.getInput(i, j);
		if (g != zero()) {
			return g;
		}
		g = one();

		int jNew = componentNext(i, j, ana);
		if (jNew >= 0) {
			int gRight = times(
					1,
					scoreInput(prefixList, suffixList, i - 1, jNew, gains, block, ana));
			g = plus(g, gRight);
		} else {
			// int leftLink = leftLinkViaComponent(i, j, ana);
			// if (leftLink >= 0) {
			if (hasleftLinkViaComponent(i, j, ana)) {
				int iLeft = skipLink(i, ana);
				int gRight =
						scoreInput(prefixList, suffixList, iLeft, j, gains, block, ana);
				g = plus(g, gRight);
			}
		}

		CompState shiftState = updateInput(j, i, ana);
		int gChildren = ana.prefSufNChildren[i];
		int gLeft = times(
				gChildren,
				scoreStack(prefixList, suffixList, i - 1, i, shiftState, gains, block, ana));
		g = plus(g, gLeft);

		gains.setInput(i, j, g);
		return g;
	}

	/**
	 * The rightmost node up to i, linked to j. Return -1 if none.
	 * @param j
	 * @return
	 */
	/*
	protected int leftLinkViaStack(final int i, final int j,
								   final Analysis ana) {
		int first = ana.prefPrefFirstLink[j];
		int last = ana.prefPrefLastLink[j];
		if (first >= 0) {
			last = Math.min(last, i);
			for (int k = last; k >= first; k--)
				if (k < i && ana.prefPrefParent[j] == k ||
						ana.prefPrefParent[k] == j)
					return k;
		}
		return -1;
	}
	*/

	protected boolean hasleftLinkViaStack(final int i, final int j,
									  final Analysis ana) {
		int first = ana.prefPrefFirstLink[j];
		return first >= 0 && first < i;
	}

	/**
	 * The rightmost node up to i, linked to j via remaining input. Return -1 if none.
	 */
	/*
	protected int leftLinkViaInput(final int i, final int j,
								   final Analysis ana) {
		final int p = ana.prefSufParent[j];
		return p < 0 ? -1 : leftLinkViaComponent(i, p, ana);
	}
	*/

	protected int leftLinkViaComponent(final int i, final int j,
									   final Analysis ana) {
		final int r = ana.sufRepr[j];
		int first = ana.sufPrefFirstLinkRep[r];
		int last = ana.sufPrefLastLinkRep[r];
		if (first >= 0) {
			last = Math.min(last, i);
			for (int k = last; k >= first; k--)
				if (ana.prefSufFirstLinkRepr(k) == r)
					return k == i ? -1 : k;
		}
		return -1;
	}

	protected boolean hasleftLinkViaComponent(final int i, final int j,
									  final Analysis ana) {
		final int r = ana.sufRepr[j];
		int first = ana.sufPrefFirstLinkRep[r];
		return first >= 0 && first < i;
	}
	/**
	 * The leftmost node greater than m, and smaller than i,
	 * such that there is a link between that node and i.
	 * If there is no such link, then i-1.
	 * The returned value should be at least m+1.
	 * @param i
	 * @return
	 */
	protected int skipLinkOld(final int m, final int i,
							  final Analysis ana) {
		if (m < i - 1) {
			/*
			for (int k = m; k < i; k++)
				if (ana.prefPrefLink(k, i))
					return Math.max(k, m + 1);
					*/
			int k = ana.prefPrefFirstLink[i];
			if (k >= 0)
				// return Math.max(k, m + 1);
				return k;
		}
		return i - 1;
	}

	protected int skipLink(final int i,
						   final Analysis ana) {
		int k = ana.prefPrefFirstLink[i];
		if (k >= 0)
			return k;
		return i - 1;
	}

}

