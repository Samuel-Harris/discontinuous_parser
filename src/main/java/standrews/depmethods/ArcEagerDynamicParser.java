/*
 * Copyright (c) 2019. University of St Andrews
 */

package standrews.depmethods;

import standrews.depautomata.DependencyVertex;
import standrews.depautomata.SimpleConfig;
import standrews.depextract.ArcEagerExtractor;
import standrews.depbase.Token;

import java.util.Iterator;
import java.util.Random;
import java.util.TreeMap;
import java.util.Vector;

public abstract class ArcEagerDynamicParser extends ArcEagerParser {

	/**
	 * Extractor obtained by static oracle.
	 */
	final protected ArcEagerExtractor staticExtractor;

	protected DynamicChooseMode chooseMode = DynamicChooseMode.RANDOM;

	/**
	 * What is probability that not optimal action is taken.
	 */
	protected double strayProb = 0.0;

	final protected Random random = new Random();

	public ArcEagerDynamicParser(final Token[] tokens,
								 final boolean early,
								 final boolean strict,
								 final ArcEagerExtractor preliminaryExtractor) {
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
		final TreeMap<String, Integer> scores = scores(config);
		final Vector<String> actions = getBestActions(scores);
		if (actions.contains(shift) && actions.contains(rightArc)) {
			if (isRightChild(config.getSuffixLeft(0)))
				actions.remove(shift);
			else
				actions.remove(rightArc);
		}
		String[] otherAction = getOtherActions(config, actions);
		if (otherAction != null) {
			/*
			if (!gold.equals(otherAction[0]) && config.totalLength() < 20) {
				printConfig(config);
				printScores(scores);
				System.out.println("diff " + gold + " " + otherAction[0]);
			}
			*/
			return otherAction;
		}
		if (config.suffixLength() >= 1) {
			if (isRightChild(config.getSuffixLeft(0))) {
				/*
				if (!gold.equals(rightArc()[0]) && config.totalLength() < 20) {
					printConfig(config);
					printScores(scores);
					System.out.println("diff " + gold + " " + rightArc());
				}
				*/
				return rightArc();
			} else {
				/*
				if (!gold.equals(shift()[0]) && config.totalLength() < 20) {
					printConfig(config);
					printScores(scores);
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
		if (action.equals(rightArc)) {
			final DependencyVertex v = config.getSuffixLeft(0);
			final Token t = v.getToken();
			final String deprel = deprel(t);
			return rightArc(deprel);
		} else if (action.equals(leftArc)) {
			final DependencyVertex v = config.getPrefixRight(0);
			final Token t = v.getToken();
			final String deprel = deprel(t);
			return leftArc(deprel);
		} else if (action.equals(reduceCorrect)) {
			final DependencyVertex v = config.getPrefixRight(0);
			final Token t = v.getToken();
			final String deprel = deprel(t);
			return reduceCorrect(deprel);
		} else if (action.equals(shift) ||
				action.equals(reduce)) {
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
