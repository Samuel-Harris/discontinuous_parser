/*
 * Copyright (c) 2019. University of St Andrews
 */

package standrews.depmethods;

import standrews.depautomata.DependencyVertex;
import standrews.depautomata.SimpleConfig;
import standrews.depextract.SimpleExtractor;
import standrews.depbase.Token;

import java.util.ArrayList;
import java.util.TreeMap;

public class SimpleDynamicParserQuadratic extends SimpleDynamicParser {

	public SimpleDynamicParserQuadratic(final Token[] tokens,
										final boolean leftDependentsFirst,
										final boolean strict,
										final SimpleExtractor preliminaryExtractor) {
		super(tokens, leftDependentsFirst, strict, preliminaryExtractor);
	}

	/**
	 * Analysis of links between prefix and suffix.
	 */
	protected class Analysis {
		private final ArrayList<DependencyVertex> prefixList;
		private final ArrayList<DependencyVertex> suffixList;
		/**
		 * Size of prefix.
		 */
		private int m;
		/**
		 * Size of suffix.
		 */
		private int n;
		/**
		 * Mapping from vertex to prefix position.
		 */
		private TreeMap<DependencyVertex,Integer> prefixMap;
		/**
		 * Mapping from vertex to suffix position.
		 */
		private TreeMap<DependencyVertex,Integer> suffixMap;
		/**
		 * Parent of prefix element in prefix.
		 */
		public int[] prefPrefParent;
		/**
		 * Parent of prefix element in suffix.
		 */
		public int[] prefSufParent;
		/**
		 * Parent of suffix element in prefix.
		 */
		public int[] sufPrefParent;
		/**
		 * Parent of suffix element in suffix.
		 */
		public int[] sufSufParent;
		/**
		 * How many edges are there internal in the suffix.
		 */
		public int sufSufNEdges;
		/**
		 * Parent of prefix element in suffix.
		 * Or if the current element doesn't have parent,
		 * take element to the right.
		 */
		public int[] prefAndRightSufParent;
		/**
		 * Number of children of prefix element in suffix.
		 */
		public int[] prefSufNChildren;
		/**
		 * The leftmost child in the suffix.
		 * This is also representative of the relevant suffix component.
		 * If none then -1;
		 */
		public int[] prefSufFirstChild;

		/**
		 * The leftmost child or parent in the suffix.
		 * If none then -1;
		 */
		public int[] prefSufFirstLink;
		/**
		 * The suffix element that is parent to prefix element.
		 * If none for this suffix element, then elements further
		 * to the left are considered and any link to suffix is
		 * fine, also children.
		 */
		/**
		 * The leftmost element to the left that is linked to this element.
		 */
		public int[] prefPrefFirstLink;
				/**
		 * The rightmost element to the left that is linked to this element.
		 */
		public int[] prefPrefLastLink;
		// public int[] prefAndLeftSufLink;
		/**
		 * Component representative for suffix element.
		 */
		public int[] sufRepr;

		/**
		 * For representative in suffix, the first element in prefix linked to it.
		 */
		public int[] sufPrefFirstLinkRep;
		/**
		 * For representative in suffix, the last element in prefix linked to it.
		 */
		public int[] sufPrefLastLinkRep;

		/**
		 * Constructor.
		 * @param prefixList
		 * @param suffixList
		 */
		public Analysis(final ArrayList<DependencyVertex> prefixList,
					  final ArrayList<DependencyVertex> suffixList) {
			this.prefixList = prefixList;
			this.suffixList = suffixList;
			m = prefixList.size();
			n = suffixList.size();
			computeMaps();
			computeLinks();
			computeReps();
		}

		private void computeMaps() {
			prefixMap = new TreeMap<>();
			suffixMap = new TreeMap<>();
			for (int i = 0; i < m; i++) {
				prefixMap.put(prefixList.get(i), i);
			}
			for (int i = 0; i < n; i++) {
				suffixMap.put(suffixList.get(i), i);
			}
		}

		private void computeLinks() {
			prefPrefParent = new int[m];
			prefSufParent = new int[m];
			for (int i = 0; i < m; i++) {
				prefPrefParent[i] = -1;
				prefSufParent[i] = -1;
				DependencyVertex p = goldVertex(prefixList.get(i)).getParent();
				if (p != null) {
					final Integer jPref = prefixMap.get(p);
					if (jPref != null) {
						prefPrefParent[i] = jPref;
					}
					final Integer jSuf = suffixMap.get(p);
					if (jSuf != null) {
						prefSufParent[i] = jSuf;
					}
				}
			}
			sufPrefParent = new int[n];
			sufSufParent = new int[n];
			sufSufNEdges = 0;
			for (int i = 0; i < n; i++) {
				sufPrefParent[i] = -1;
				sufSufParent[i] = -1;
				DependencyVertex p = goldVertex(suffixList.get(i)).getParent();
				if (p != null) {
					final Integer jPref = prefixMap.get(p);
					if (jPref != null) {
						sufPrefParent[i] = jPref;
					}
					final Integer jSuf = suffixMap.get(p);
					if (jSuf != null) {
						sufSufParent[i] = jSuf;
						sufSufNEdges++;
					}
				}
			}

			prefAndRightSufParent = new int[m];
			int furtherRight = -1;
			for (int i = m-1; i >= 0; i--) {
				if (prefSufParent[i] < 0)
					prefAndRightSufParent[i] = furtherRight;
				else {
					prefAndRightSufParent[i] = prefSufParent[i];
					furtherRight = prefSufParent[i];
				}
			}

			prefSufNChildren = new int[m];
			for (int i = 0; i < n; i++) {
				int p = sufPrefParent[i];
				if (p >= 0)
					prefSufNChildren[p]++;
			}

			prefSufFirstChild = new int[m];
			for (int i = 0; i < m; i++)
				prefSufFirstChild[i] = -1;
			for (int i = 0; i < n; i++) {
				int p = sufPrefParent[i];
				if (p >= 0 && prefSufFirstChild[p] < 0) {
					prefSufFirstChild[p] = i;
				}
			}

			prefSufFirstLink = new int[m];
			for (int i = 0; i < m; i++) {
				prefSufFirstLink[i] = safeMin(prefSufFirstChild[i], prefSufParent[i]);
			}

			prefPrefFirstLink = new int[m];
			prefPrefLastLink = new int[m];
			for (int i = 0; i < m; i++) {
				final int p = prefPrefParent[i];
				prefPrefFirstLink[i] = p >= 0 && p < i ? p : -1;
				prefPrefLastLink[i] = p >= 0 && p < i ? p : -1;
			}
			for (int i = 0; i < m; i++) {
				final int p = prefPrefParent[i];
				if (i < p) {
					prefPrefFirstLink[p] = safeMin(prefPrefFirstLink[p], i);
					prefPrefLastLink[p] = safeMax(prefPrefLastLink[p], i);
				}
			}

			/*
			prefAndLeftSufLink = new int[m];
			int furtherLeft = -1;
			for (int i = 0; i < m; i++) {
				int p = prefSufParent[i];
				prefAndLeftSufLink[i] = p >= 0 ? p : furtherLeft;
				int q = prefSufFirstLink[i];
				if (q >= 0)
					furtherLeft = q;
			}
			*/
		}

		public int prefSufFirstChildQualified(final int i, final boolean used) {
			final int n = prefSufNChildren[i];
			return !used ? n : n-1;
		}

		public void computeReps() {
			sufRepr = new int[n];
			for (int j = n-1; j >= 0; j--)
				sufRepr[j] = sufSufParent[j] < 0 ? j : sufRepr[sufSufParent[j]];

			sufPrefFirstLinkRep = new int[n];
			sufPrefLastLinkRep = new int[n];
			for (int r = 0; r < n; r++) {
				sufPrefFirstLinkRep[r] = -1;
				sufPrefLastLinkRep[r] = -1;
			}
			for (int i = 0; i < m; i++) {
				int r = prefSufFirstLinkRepr(i);
				if (r >= 0) {
					sufPrefFirstLinkRep[r] = safeMin(sufPrefFirstLinkRep[r], i);
					sufPrefLastLinkRep[r] = safeMax(sufPrefLastLinkRep[r], i);
				}
			}

		}

		private int safeMin(final int i, final int j) {
			if (i < 0)
				return j;
			else if (j < 0)
				return i;
			else
				return Math.min(i, j);
		}

		private int safeMax(final int i, final int j) {
			if (i < 0)
				return j;
			else if (j < 0)
				return i;
			else
				return Math.max(i, j);
		}

		private int safeSufRepr(final int i) {
			return i < 0 ? -1 : sufRepr[i];
		}

		public boolean prefPrefLink(final int i, final int j) {
			return prefPrefParent[i] == j || prefPrefParent[j] == i;
		}

		public int prefAndRightSufParentRepr(final int i) {
			return safeSufRepr(prefAndRightSufParent[i]);
		}

		public int prefSufParentRepr(final int i) {
			return safeSufRepr(prefSufParent[i]);
		}

		public int prefSufFirstLinkRepr(final int i) {
			return safeSufRepr(prefSufFirstLink[i]);
		}

		/**
		 * For debugging.
		 */
		public void printAll() {
			for (int i = 0; i < m; i++)
				if (prefPrefParent[i] >= 0)
					System.out.println("P" + prefPrefParent[i] + " P" + i);
			for (int i = 0; i < m; i++)
				if (prefSufParent[i] >= 0)
					System.out.println("S" + prefSufParent[i] + " P" + i);
			for (int i = 0; i < n; i++)
				if (sufSufParent[i] >= 0)
					System.out.println("S" + sufSufParent[i] + " S" + i);
			for (int i = 0; i < n; i++)
				if (sufPrefParent[i] >= 0)
					System.out.println("P" + sufPrefParent[i] + " S" + i);
		}

	}

	/**
	 * NONE means that the component has not yet been used.
	 * CONSUMED means that the current node has been consumed.
	 * FRESH means that the component has been entered, but the current node
	 * has not been consumed.
	 */
	protected enum CompState {NONE, CONSUMED, FRESH};
	protected static final int nStates = CompState.values().length;

	/**
	 * How many edges are found hereafter by the ideal computation,
	 * for a prefix of a stack and an element in the remainder
	 * of the stack, or in the remaining input.
	 */
	protected static class PotentialGains {
		/**
		 * Total stack size.
		 */
		final public int m;
		/**
		 * Remaining input size.
		 */
		final public int n;
		/**
		 * gainStack[i][k][q] is maximal number of edges found with
		 * prefix of the total stack up to element with index i,
		 * and element with index k+i+1, with state q.
		 */
		final private int[][][] gainsStack;
		/**
		 * gainInput[i][k] is the same, but for stack up to element with index i,
		 * and with input element with index j.
		 */
		final private int[][] gainsInput;

		/**
		 * @param m
		 */
		public PotentialGains(final int m, final int n) {
			this.m = m;
			this.n = n;
			gainsStack = new int[m - 1][][];
			for (int i = 0; i < m - 1; i++) {
				gainsStack[i] = new int[m - 1 - i][];
				for (int j = 0; j < m - 1 - i; j++) {
					gainsStack[i][j] = new int[nStates];
					for (int state = 0; state < nStates; state++) {
						gainsStack[i][j][state] = zero();
					}
				}
			}
			gainsInput = new int[m - 1][];
			for (int i = 0; i < m - 1; i++) {
				gainsInput[i] = new int[n];
				for (int j = 0; j < n; j++) {
					gainsInput[i][j] = zero();
				}
			}
		}

		public int getStack(final int i, final int j, final CompState state) {
			return gainsStack[i][j-i-1][state.ordinal()];
		}
		public int getInput(final int i, final int j) {
			return gainsInput[i][j];
		}
		public void setStack(final int i, final int j, final CompState state, final int g) {
			gainsStack[i][j-i-1][state.ordinal()] = g;
		}
		public void setInput(final int i, final int j, final int g) {
			gainsInput[i][j] = g;
		}
	}

	protected int printPotentialGains(final PotentialGains gains) {
		int nonzeros = 0;
		for (int i = 0; i < gains.m - 1; i++) {
			// System.out.print("" + i + " ");
			for (int j = i+1; j < gains.m; j++)
				for (CompState q : CompState.values())
					if (gains.getStack(i, j, q) != zero()) {
						// System.out.print("" + j + "[" + q + "] ");
						nonzeros++;
					}
			for (int j = 0; j < gains.n; j++)
				if (gains.getInput(i, j) != zero()) {
					// System.out.print("" + j + " ");
					nonzeros++;
				}
			// System.out.println();
		}
		return nonzeros;
	}

	protected TreeMap<String, Integer> scores(final SimpleConfig config) {
		// if (config.prefixLength() > 10)
			// printConfig(config);
		final TreeMap<String, Integer> scores = scores();
		final ArrayList<DependencyVertex> prefixList = config.prefixList();
		final ArrayList<DependencyVertex> suffixList = config.suffixList();
		final int m = prefixList.size();
		final int n = suffixList.size();
		final boolean restricted = leftDependentsFirst && strict;

		final ArrayList<DependencyVertex> leftList =
				new ArrayList<>(prefixList.subList(0, m - 1));
		final boolean[] leftBlock = new boolean[m - 1];
		if (restricted) {
			for (int i = 0; i < m - 1; i++)
				leftBlock[i] = true;
		}
		final int leftScore = times(
				edgeWeight(prefixList.get(m - 2), prefixList.get(m - 1)),
				score(leftList, suffixList, leftBlock, false));
		scores.put("reduceLeft", leftScore);

		if (m > 2) {
			final ArrayList<DependencyVertex> rightList =
					new ArrayList(prefixList.subList(0, m - 2));
			rightList.add(prefixList.get(m - 1));
			final boolean[] rightBlock = new boolean[m - 1];
			if (restricted) {
				for (int i = 0; i < m - 2; i++)
					rightBlock[i] = true;
			}
			final int rightScore = times(
					edgeWeight(prefixList.get(m - 1), prefixList.get(m - 2)),
					score(rightList, suffixList, rightBlock, !leftDependentsFirst));
			scores.put("reduceRight", rightScore);
		}

		if (n > 0) {
			prefixList.add(suffixList.remove(0));
			final boolean[] shiftBlock = new boolean[m + 1];
			if (leftDependentsFirst) {
				shiftBlock[m - 1] = true;
				if (strict)
					for (int i = 0; i < m - 1; i++)
						shiftBlock[i] = true;
			}
			final int shiftScore = score(prefixList, suffixList, shiftBlock, false);
			scores.put("shift", shiftScore);
		}

		return scores;
	}

	protected int score(final ArrayList<DependencyVertex> prefixList,
						final ArrayList<DependencyVertex> suffixList,
						final boolean[] leftBlock, final boolean rightBlock) {
		final int m = prefixList.size();
		final int n = suffixList.size();
		final ArrayList<DependencyVertex> prunedSuffixList = prunedSuffix(suffixList);
		final int np = prunedSuffixList.size();
		final int pruneScore = n - np;
		final Analysis ana = new Analysis(prefixList, prunedSuffixList);
		final int internalScore = ana.sufSufNEdges;
		final PotentialGains gains = new PotentialGains(m, np);
		final int childScore = rightBlock ? one() : ana.prefSufNChildren[m-1];
		final int stackScore = scoreStack(prefixList, prunedSuffixList, m - 2, m - 1,
				CompState.NONE, gains, leftBlock, ana);
		printDiagnostics(m, gains);
		return times(times(times(childScore, pruneScore), internalScore), stackScore);
	}

	protected void printDiagnostics(final int size,
				final PotentialGains gains) {
		// if (size > 15)
		/*
		int nonzeros = printPotentialGains(gains);
		DataCollectionSum.globalCollection.add("nonzerosquadratic", size, nonzeros);
		*/
	}

	protected int scoreStack(final ArrayList<DependencyVertex> prefixList,
							 final ArrayList<DependencyVertex> suffixList,
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
			int gRight = times(
					edgeWeight(prefixList.get(j), prefixList.get(i)),
					scoreStack(prefixList, suffixList, i - 1, j, state, gains, block, ana));
			g = plus(g, gRight);
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

	protected int scoreInput(final ArrayList<DependencyVertex> prefixList,
							 final ArrayList<DependencyVertex> suffixList,
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
			int gRight =
					scoreInput(prefixList, suffixList, i - 1, j, gains, block, ana);
			g = plus(g, gRight);
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

	protected int componentNext(final int i, final int j,
								final Analysis ana) {
		int ri = ana.prefSufParentRepr(i);
		int rj = ana.sufRepr[j];
		return ri == rj ? ana.prefSufParent[i] : -1;
	}

	protected CompState updateStack(final CompState oldState,
									final int iOld, final int iNew,
									final Analysis ana) {
		if (oldState == CompState.CONSUMED || oldState == CompState.FRESH) {
			int pOld = ana.prefAndRightSufParent[iOld];
			int rOld = ana.prefAndRightSufParentRepr(iOld);
			int pNew = ana.prefAndRightSufParent[iNew];
			int rNew = ana.prefAndRightSufParentRepr(iNew);
			if (rNew != rOld)
				return CompState.NONE;
			else if (pOld == pNew)
				return oldState;
			else
				return CompState.FRESH;
		} else {
			return CompState.NONE;
		}
	}

	protected CompState updateInput(final int iOld, final int iNew,
									final Analysis ana) {
		int rOld = ana.sufRepr[iOld];
		int pNew = ana.prefAndRightSufParent[iNew];
		int rNew = ana.prefAndRightSufParentRepr(iNew);
		if (rNew != rOld)
			return CompState.NONE;
		else if (iOld == pNew)
			return CompState.CONSUMED;
		else
			return CompState.FRESH;
	}

}

