/*
 * Copyright (c) 2019. University of St Andrews
 */

package standrews.depmethods;

import standrews.depautomata.DependencyVertex;
import standrews.depautomata.SimpleConfig;
import standrews.depextract.NormalArcEagerExtractor;
import standrews.depbase.Token;

import java.util.*;

public abstract class NormalArcEagerDynamicParser extends NormalArcEagerParser {

	/**
	 * Extractor obtained by static oracle.
	 */
	final protected NormalArcEagerExtractor staticExtractor;

	protected DynamicChooseMode chooseMode = DynamicChooseMode.RANDOM;

	/**
	 * What is probability that not optimal action is taken.
	 */
	protected double strayProb = 0.0;

	final protected Random random = new Random();

	public NormalArcEagerDynamicParser(final Token[] tokens,
									   final NormalArcEagerExtractor preliminaryExtractor) {
		super(tokens);
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
		System.out.println(config);
		String gold = "nil";
		if (super.getAction(config).length > 0)
			gold = super.getAction(config)[0];
			*/
		// TEMP
		String[] gold = super.getAction(config);
		final TreeMap<String, Integer> tmpscores = scores(config);
		final Vector<String> tmpactions = getBestActions(tmpscores);
		if (!tmpactions.get(0).equals(gold[0]) && config.totalLength() < 10) {
			System.out.println(gold[0]);
			System.out.println(tmpscores);
			System.out.println(config.toString());
			System.exit(0);
		}
		System.out.println(gold[0]);
		System.out.println(tmpscores);
		if (4 == 4)
			return gold;

		if (config.prefixLength() >= 2) {
			final TreeMap<String, Integer> scores = scores(config);
			// System.out.println(toString(scores));
			final Vector<String> actions = getBestActions(scores);
			if (actions.contains(rightArc) && actions.contains(leftArc)) {
				if (isRightChild(config.getPrefixRight(0)))
					actions.remove(leftArc);
				else
					actions.remove(rightArc);
			}
			String[] otherAction = getOtherActions(config, actions);
			if (otherAction != null) {
				/*
				if (!gold.equals(otherAction[0]) && config.totalLength() < 5) {
					if (config.prefixLength() >= 2) {
						final DependencyVertex v0 = config.getPrefixRight(0);
						final DependencyVertex v1 = config.getPrefixRight(1);
						System.out.println("isEdge " + isEdge(v1, v0));
						System.out.println("areAtt " + areChildrenAttached(v0));
						System.out.println(config.getLabelRight(0).equals("nil"));
					}
					printConfig(config);
					System.out.println("diff " + gold + " " + otherAction[0]);
					printScores(scores);
				}
				*/
				return otherAction;
			}
		}
		if (config.suffixLength() > 0) {
			if (config.getLabelRight(0).equals(nil)) {
				/*
				if (!gold.equals(rightArc()[0]) && config.totalLength() < 5) {
					printConfig(config);
					System.out.println("diff " + gold + " " + leftArc());
				}
				*/
				return leftArc();
			} else {
				/*
				if (!gold.equals(shift()[0]) && config.totalLength() < 5) {
					printConfig(config);
					System.out.println("diff " + gold + " " + shift());
				}
				*/
				return shift();
			}
		}
		return none();
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
		if (config.suffixLength() > 0) {
			if (config.getLabelRight(0).equals(nil))
				return leftArc();
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
		if (action.equals(reduceLeft)) {
			final DependencyVertex v0 = config.getPrefixRight(0);
			final Token t0 = v0.getToken();
			final String deprel = deprel(t0);
			return reduceLeft(deprel);
		} else if (action.equals(reduceRight)) {
			final DependencyVertex v1 = config.getPrefixRight(1);
			final Token t1 = v1.getToken();
			final String deprel = deprel(t1);
			return reduceRight(deprel);
		} else if (action.equals(shift) ||
				action.equals(rightArc) ||
				action.equals(leftArc)) {
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
