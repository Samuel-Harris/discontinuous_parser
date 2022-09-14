/*
 * Copyright (c) 2019. University of St Andrews
 */

package standrews.depmethods;

import standrews.aux.DataCollectionSum;
import standrews.aux.TimerNano;
import standrews.depautomata.DependencyVertex;
import standrews.depautomata.SimpleConfig;
import standrews.depextract.RevisedArcEagerExtractor;
import standrews.depbase.Token;

import java.util.Iterator;
import java.util.Random;
import java.util.TreeMap;
import java.util.Vector;

public abstract class RevisedArcEagerDynamicParser extends RevisedArcEagerParser {

	/**
	 * Extractor obtained by static oracle.
	 */
	final protected RevisedArcEagerExtractor staticExtractor;

	protected DynamicChooseMode chooseMode = DynamicChooseMode.RANDOM;

	/**
	 * What is probability that not optimal action is taken.
	 */
	protected double strayProb = 0.0;

	final protected Random random = new Random();

	public RevisedArcEagerDynamicParser(final Token[] tokens,
										final boolean early,
										final boolean strict,
										final RevisedArcEagerExtractor preliminaryExtractor) {
		super(tokens, early, strict);
		this.staticExtractor = preliminaryExtractor;
	}

	public void setChooseMode(final DynamicChooseMode chooseMode) {
		this.chooseMode = chooseMode;
	}

	public void setStrayProb(final double strayProb) {
		this.strayProb = strayProb;
	}

	/**
	 * Get best action as in static oracle.
	 *
	 * @param config Current configuration.
	 * @return
	 */
	protected String[] getAction(final SimpleConfig config) {
		/*
		String gold = "nil";
		if (super.getAction(config).length > 0)
			gold = super.getAction(config)[0];
		*/

		TimerNano timer = new TimerNano();
		timer.init();
		final TreeMap<String, Integer> scores = scores(config);
		final Vector<String> actions = getBestActions(scores);
		double t = timer.stopMsec();
		int len = config.totalLength();
		DataCollectionSum.globalCollection.add("time", len, t);
		// TEMP
		/*
		String[] gold = super.getAction(config);
		final TreeMap<String, Integer> approxScores = getApproximateScores(config);
		if (!actions.get(0).equals(gold[0]) && config.totalLength() < 10) {
			System.out.println(gold[0]);
			System.out.println(scores);
			System.out.println(config.toString());
			System.exit(0);
		}
		DataCollectionSum.globalCollection.add("exact", 0, 1);
		if (!equalScores(scores, approxScores) && config.totalLength() < 8) {
			System.out.println(scores);
			System.out.println(approxScores);
			System.out.println(config);
			System.exit(0);
		}
		if (4 == 4)
			return gold;
			*/

		final TreeMap<String, Integer> approxScores = getApproximateScores(config);
		final Vector<String> approxActions = getBestActions(approxScores);
		final boolean isSubset = actions.containsAll(approxActions);
		// System.out.println(approxScores);
		// System.out.println(scores);
		if (!equalScores(scores, approxScores)) {
			/*
			if (config.totalLength() < 90) {
				System.out.println(scores);
				System.out.println(approxScores);
				System.out.println(config);
				printConfig(config);
				System.exit(0);
			}
			*/
			DataCollectionSum.globalCollection.add("unequalScores", 0, 1);
		} else {
			DataCollectionSum.globalCollection.add("unequalScores", 0, 0);
		}
		if (!equalActions(actions, approxActions)) {
			DataCollectionSum.globalCollection.add("unequalActions", 0, 1);
			DataCollectionSum.globalCollection.add("unequalActions with two strings", 0, 1);
			DataCollectionSum.globalCollection.add("unequalActions " + actions + " " + approxActions, 0, 1);
			if (isSubset)
				DataCollectionSum.globalCollection.add("unequalActions subset", 0, 1);
			else
				DataCollectionSum.globalCollection.add("unequalActions subset", 0, 0);
		} else {
			DataCollectionSum.globalCollection.add("unequalActions", 0, 0);
		}

		if (actions.contains(shift) && actions.contains(rightArc)) {
			if (isRightChild(config.getSuffixLeft(0)))
				actions.remove(shift);
			else
				actions.remove(rightArc);
		}
		String[] otherAction = getOtherActions(config, actions);
		if (otherAction != null) {
			return otherAction;
		}
		if (config.suffixLength() >= 1) {
			if (isRightChild(config.getSuffixLeft(0))) {
				return rightArc();
			} else {
				return shift();
			}
		}
		return none();
	}

	/**
	 * An approximation, believed to be equivalent to Aufrant et al.
	 */
	protected TreeMap<String, Integer> getApproximateScores(final SimpleConfig config) {
		final TreeMap<String,Integer> scores = scores();
		if (applicable(config, shift())) {
			final SimpleConfig next = new SimpleConfig(config);
			shift(next);
			scores.put(shift, nextApproximateScore(next, earlyReduce, false, true));
		}
		if (applicable(config, rightArc())) {
			final SimpleConfig next = new SimpleConfig(config);
			rightArc(next);
			scores.put(rightArc, nextApproximateScore(next, false,false, false));
		}
		if (applicable(config, leftArc())) {
			final DependencyVertex head = config.getSuffixLeft(0);
			final DependencyVertex dep = config.getPrefixRight(0);
			final int w1 = edgeWeight(head, dep);
			final SimpleConfig next = new SimpleConfig(config);
			leftArc(next, "_");
			final int w2 = nextApproximateScore(next, false, false, false);
			scores.put(leftArc, times(w1, w2));
		}
		if (applicable(config, reduce())) {
			final DependencyVertex head = config.getPrefixRight(1);
			final DependencyVertex dep = config.getPrefixRight(0);
			final int w1 = edgeWeight(head, dep);
			final SimpleConfig next = new SimpleConfig(config);
			reduce(next, "_");
			final int w2 = nextApproximateScore(next, false, !earlyReduce, false);
			scores.put(reduce, times(w1, w2));
		}
		return scores;
	}

	protected int nextApproximateScore(final SimpleConfig next,
									   final boolean reduceBlock,
									   final boolean shiftBlock,
									   final boolean leftRightBlock) {
		int score = 0;
		if (leftRightBlock && next.suffixLength() == 0)
			return zero();
		for (int i = 0; i < next.prefixLength(); i++) {
			final DependencyVertex prefElem = next.getPrefixLeft(i);
			for (int j = 0; j < next.suffixLength(); j++) {
				final DependencyVertex sufElem = next.getSuffixLeft(j);
				if (i == next.prefixLength()-1 && leftRightBlock) {
					if (edgeWeight(prefElem, sufElem) > 0 &&
								rightmostInputDescendant(next, j) < next.suffixLength() - 1) {
							score = times(score, edgeWeight(prefElem, sufElem));
					} else {
						// ignore
					}
				} else {
					score = times(score, edgeWeight(prefElem, sufElem));
				}
				if (next.getLabelLeft(i).equals(leftChild)) {
					score = times(score, edgeWeight(sufElem, prefElem));
				}
			}
		}
		for (int i = 0; i < next.prefixLength()-1; i++) {
			final DependencyVertex prefElem1 = next.getPrefixLeft(i);
			final DependencyVertex prefElem2 = next.getPrefixLeft(i+1);
			if (i+1 == next.prefixLength()-1 && leftRightBlock) {
				// ignore
			} else {
				score = times(score, edgeWeight(prefElem1, prefElem2));
			}
		}
		for (int i = 0; i < next.suffixLength(); i++) {
			final DependencyVertex head = next.getSuffixLeft(i);
			for (int j = 0; j < next.suffixLength(); j++) {
				final DependencyVertex dep = next.getSuffixLeft(j);
				if (j == 0 && shiftBlock && !hasChildInStack(next, dep)) {
					// ignore
				} else if (i != j) {
					score = times(score, edgeWeight(head, dep));
				}
			}
		}
		if (reduceBlock) {
			int corrL = reduceBlockCorrectionL(next);
			int corrR = reduceBlockCorrectionR(next);
			int corr = plus(corrL, corrR);
			// System.out.println(" " + corrL + " " + corrR);
			score = times(score, corr);
		}
		return score;
	}

	private boolean hasChildInStack(SimpleConfig config, DependencyVertex head) {
		for (int i = config.prefixLength()-1; i > 0; i--) {
			if (hasConnectionToInput(config, i)) {
				return false;
			}
			final DependencyVertex d1 = config.getPrefixLeft(i);
			final DependencyVertex d2 = config.getPrefixLeft(i-1);
			if (edgeWeight(d2, d1) == 0 && config.getLabelLeft(i).equals(leftChild)) {
				return true;
			}
		}
		return false;
	}

	private boolean hasConnectionToInput(SimpleConfig config, final int i1) {
		final DependencyVertex d1 = config.getPrefixLeft(i1);
		for (int i2 = 1; i2 < config.suffixLength(); i2++) {
			final DependencyVertex d2 = config.getSuffixLeft(i2);
			if (config.getLabelLeft(i1).equals(leftChild) && edgeWeight(d2, d1) > 0 ||
					edgeWeight(d1, d2) > 0)
				return true;
		}
		return false;
	}

	private boolean hasSuffixParent(SimpleConfig config, DependencyVertex d) {
		for (int i = 0; i < config.suffixLength(); i++) {
			final DependencyVertex di = config.getSuffixLeft(i);
			if (edgeWeight(di, d) > 0)
				return true;
		}
		return false;
	}

	private int reduceBlockCorrectionL(SimpleConfig config) {
		if (!config.getLabelRight(1).equals(leftChild))
			return zero();
		final DependencyVertex d1 = config.getPrefixRight(1);
		final DependencyVertex d2 = config.getPrefixRight(2);
		return -edgeWeight(d2, d1);
	}

	private int reduceBlockCorrectionR(SimpleConfig config) {
		int corr = zero();
		final DependencyVertex d0 = config.getPrefixRight(0);
		final DependencyVertex d1 = config.getPrefixRight(1);
		if (hasSuffixChildren(config, d1))
			return one();
		if (rightmostInputDescendant(config, d0) >= 0 || suffixParent(config, d0) >= 1) {
			int assumedDescendant = rightmostInputDescendant(config, d0);
			if (assumedDescendant < 0)
				assumedDescendant = 0;
			DependencyVertex pdesc = config.getSuffixLeft(assumedDescendant);
			int corrNext = 0;
			if (suffixParent(config, d0) >= 1)
				corrNext--;
			if (suffixParent(config, pdesc) >= 0 || prefixParent(config, pdesc) >= 0)
				corrNext--;
			corr = Math.max(corr, corrNext);
		}
		int parent = assumedParent0(config);
		DependencyVertex pd = config.getSuffixLeft(parent);
		int crossingPrefixChildren = nEarlyPrefixChildren(config, pd);
		int corrParent = -crossingPrefixChildren;
		if (suffixParent(config, parent) > 0)
			corrParent--;
		else if (0 <= prefixParent(config, pd) &&
				prefixParent(config, pd) < config.prefixLength() - 2)
			corrParent--;
		corr = Math.max(corr, corrParent);
		int root = parent;
		while (suffixParent(config, root) >= 0) {
			root = suffixParent(config, root);
			DependencyVertex rd = config.getSuffixLeft(root);
			crossingPrefixChildren += nEarlyPrefixChildren(config, rd);
		}
		DependencyVertex rd = config.getSuffixLeft(root);
		int corrRoot = -crossingPrefixChildren;
		if (0 <= prefixParent(config, rd) &&
					prefixParent(config, rd) < config.prefixLength() - 2)
			corrRoot--;
		corr = Math.max(corr, corrRoot);
		return corr;
	}

	private int assumedParent0(SimpleConfig config) {
		final DependencyVertex d0 = config.getPrefixRight(0);
		int parent = suffixParent(config, d0);
		if (parent >= 0)
			return parent;
		int descendant = rightmostInputDescendant(config, d0);
		if (descendant < 0)
			return 0;
		else if (descendant == config.suffixLength()-1)
			return descendant;
		else
			return descendant + 1;
	}

	private int nEarlyPrefixChildren(SimpleConfig config, DependencyVertex d) {
		int n = 0;
		for (int i = 0; i < config.prefixLength() - 2; i++) {
			final DependencyVertex di = config.getPrefixLeft(i);
			if (edgeWeight(d, di) > 0 && config.getLabelLeft(i).equals(leftChild))
				n++;
		}
		return n;
	}

	private boolean hasSuffixChildren(SimpleConfig config, DependencyVertex d) {
		for (int i = 0; i < config.suffixLength(); i++) {
			final DependencyVertex di = config.getSuffixLeft(i);
			if (edgeWeight(d, di) > 0)
				return true;
		}
		return false;
	}

	private int suffixParent(SimpleConfig config, DependencyVertex d) {
		for (int i = 0; i < config.suffixLength(); i++) {
			final DependencyVertex di = config.getSuffixLeft(i);
			if (edgeWeight(di, d) > 0)
				return i;
		}
		return -1;
	}

	private int suffixParent(SimpleConfig config, int i) {
		final DependencyVertex di = config.getSuffixLeft(i);
		return suffixParent(config, di);
	}

	private int prefixParent(SimpleConfig config, DependencyVertex d) {
		for (int i = 0; i < config.prefixLength(); i++) {
			final DependencyVertex di = config.getPrefixLeft(i);
			if (edgeWeight(di, d) > 0)
				return i;
		}
		return -1;
	}

	private int suffixRoot(SimpleConfig config, int i) {
		int parent = suffixParent(config, i);
		if (parent < 0)
			return i;
		else
			return suffixRoot(config, parent);
	}

	private boolean crossingEdge(SimpleConfig config,
									int left, int j, int right) {
		for (int i = left; i < j; i++) {
			for (int k = j+1; k < right; k++) {
				final DependencyVertex di = config.getSuffixLeft(i);
				final DependencyVertex dk = config.getSuffixLeft(k);
				if (edgeWeight(di, dk) > 0 || edgeWeight(dk, di) > 0)
					return true;
			}
		}
		return false;
	}

	private int rightmostInputDescendant(SimpleConfig config,
						DependencyVertex d1) {
		for (int i2 = config.suffixLength()-1; i2 >= 0; i2--) {
			final DependencyVertex d2 = config.getSuffixLeft(i2);
			if (edgeWeight(d1, d2) > 0) {
				return rightmostInputDescendant(config, i2);
			}
		}
		return -1;
	}

	private int rightmostInputDescendant(SimpleConfig config, final int i1) {
		final DependencyVertex d1 = config.getSuffixLeft(i1);
		for (int i2 = config.suffixLength()-1; i2 > i1; i2--) {
			final DependencyVertex d2 = config.getSuffixLeft(i2);
			if (edgeWeight(d1, d2) > 0) {
				return rightmostInputDescendant(config, i2);
			}
		}
		return i1;
	}

	/**
	 * Get action that may stray from correct one.
	 *
	 * @param config
	 * @return
	 */
	public String[] getStrayAction(final SimpleConfig config) {
		if (config.prefixLength() >= 2) {
			if (chooseMode == DynamicChooseMode.PRELIM || chooseMode == DynamicChooseMode.PRELIM_PRELIM) {
				final Iterator<String[]> actions = staticExtractor.predict(config);
				while (actions.hasNext()) {
					final String[] ac = actions.next();
					if (applicable(config, ac)) {
						return ac;
					}
				}
			}
			final TreeMap<String, Integer> scores = scores(config);
			final Vector<String> actions = getPossibleActions(scores);
			String[] otherAction = getOtherActions(config, actions);
			if (otherAction != null)
				return otherAction;
		}
		if (config.suffixLength() >= 1) {
			if (isRightChild(config.getSuffixLeft(0)))
				return rightArc();
			else
				return shift();
		}
		return none();
	}

	private String[] getOtherActions(final SimpleConfig config,
									 final Vector<String> actions) {
		String action = "null";
		if (actions.size() == 0) {
			/* skip */
		} else if (actions.size() == 1 || chooseMode == DynamicChooseMode.FIRST) {
			action = actions.get(0);
		} else if (chooseMode == DynamicChooseMode.LAST) {
			action = actions.get(actions.size() - 1);
		} else {
			if (chooseMode == DynamicChooseMode.PRELIM_PRELIM) {
				Iterator<String[]> predActions = staticExtractor.predict(config);
				while (predActions.hasNext()) {
					final String[] ac = predActions.next();
					if (actions.contains(ac[0])) {
						return ac;
					}
				}
			}
			final int i = random.nextInt(actions.size());
			action = actions.get(i);
		}
		if (action.equals(leftArc)) {
			final DependencyVertex v = config.getPrefixRight(0);
			final Token t = v.getToken();
			final String deprel = deprel(t);
			return leftArc(deprel);
		} else if (action.equals(reduce)) {
			final DependencyVertex v = config.getPrefixRight(0);
			final Token t = v.getToken();
			final String deprel = deprel(t);
			return reduce(deprel);
		} else if (action.equals(shift) ||
				action.equals(rightArc)) {
			return new String[]{action};
		}
		return null;
	}

	/**
	 * Get action that is to be done for dynamic oracle.
	 * @param config
	 * @return
	 */
	protected String[] getStepAction(final SimpleConfig config) {
		if (random.nextDouble() <= strayProb) {
			return getStrayAction(config);
		} else {
			return getAction(config);
		}
	}

	protected abstract TreeMap<String,Integer> scores(final SimpleConfig config);
}
