/*
 * Copyright (c) 2019. University of St Andrews
 */

package standrews.depmethods;

import standrews.depautomata.DependencyVertex;
import standrews.depautomata.SimpleConfig;
import standrews.depbase.Deprel;
import standrews.depbase.Token;

/**
 * SHIFT:
 * alpha | b beta => alpha b^L | beta
 * RIGHTARC:
 * alpha | b beta => alpha b^R | beta
 * LEFTARC:
 * alpha a^L | b beta => alpha | b beta
 * REDUCE:
 * alpha a b | beta => alpha a | beta
 */

public class RevisedArcEagerParser extends DeterministicParser {
	/**
	 * The first parts of actions.
	 */
	public static final String shift = ArcEagerParser.shift;
	public static final String rightArc = ArcEagerParser.rightArc;
	public static final String leftArc = ArcEagerParser.leftArc;
	public static final String reduce = ArcEagerParser.reduce;
	public static final String[] actionNames = {shift, rightArc, leftArc, reduce};
	public String[] actionNames() {
		return actionNames;
	}
	public String[] none() {
		return new String[]{};
	}
	public String[] shift() {
		return new String[]{shift};
	}
	public String[] rightArc() {
		return new String[]{rightArc};
	}
	public String[] leftArc(final String deprel) {
		return new String[]{leftArc, deprel};
	}
	public String[] reduce(final String deprel) {
		return new String[]{reduce, deprel};
	}
	public String[] leftArc() {
		return leftArc("_");
	}
	public String[] reduce() {
		return reduce("_");
	}

	/**
	 * Labels attached to prefix elements.
	 */
	public static final String leftChild = ArcEagerParser.leftChild;
	public static final String rightChild = ArcEagerParser.rightChild;
	public static String[] prefixLabels = ArcEagerParser.prefixLabels;

	/**
	 * Spurious ambiguity resolved in favour of doing reduce.
	 */
	protected boolean earlyReduce;

	protected boolean strict;

	public RevisedArcEagerParser(final Token[] tokens,
								 final boolean early,
								 final boolean strict) {
		super(tokens);
		setEarlyReduce(early);
		setStrict(strict);
	}

	public void setEarlyReduce(final boolean b) {
		earlyReduce = b;
	}

	public void setStrict(final boolean b) {
		strict = b;
	}

	@Override
	protected SimpleConfig makeInitialConfig(final Token[] tokens) {
		return new SimpleConfig(tokens, rightChild);
	}

	/**
	 * Determine which action is needed in current configuration.
	 *
	 * @param config Current configuration.
	 * @return The action.
	 */
	@Override
	protected String[] getAction(final SimpleConfig config) {
		final DependencyVertex v0 = config.getPrefixRight(0);
		if (config.suffixLength() >= 1) {
			final DependencyVertex v1 = config.getSuffixLeft(0);
			if (isEdge(v1, v0)) {
				final String deprel = deprel(v0);
				return leftArc(deprel);
			}
		}
		if (config.prefixLength() >= 2) {
			final String deprel = deprel(v0);
			if (earlyReduce) {
				if (isRightChild(v0) && areChildrenAttached(v0)) {
					return reduce(deprel);
				}
			} else if (config.suffixLength() == 0) {
				return reduce(deprel);
			} else {
				final DependencyVertex v1 = config.getSuffixLeft(0);
				for (int i = 1; i < config.prefixLength(); i++) {
					final DependencyVertex v = config.getPrefixRight(i);
					if (isLink(v, v1))
						return reduce(deprel);
				}
			}
		}
		if (config.suffixLength() >= 1) {
			if (isRightChild(config.getSuffixLeft(0)))
				return rightArc();
			else
				return shift();
		}
		return none();
	}

	@Override
	protected void apply(SimpleConfig config, String[] action) {
		switch (action[0]) {
			case shift:
				shift(config);
				break;
			case rightArc:
				rightArc(config);
				break;
			case leftArc:
				leftArc(config, action[1]);
				break;
			case reduce:
				reduce(config, action[1]);
				break;
		}
	}

	@Override
	protected boolean applicable(final SimpleConfig config, final String[] action) {
		switch (action[0]) {
			case shift:
				return config.suffixLength() >= 2;
			case rightArc:
				return config.prefixLength() >= 1 && config.suffixLength() >= 1;
			case leftArc:
				return config.prefixLength() >= 2 && config.suffixLength() >= 1;
			case reduce:
				return config.prefixLength() >= 2;
			default:
				return false;
		}
	}

	protected void shift(final SimpleConfig config) {
		final DependencyVertex shifted = config.removeSuffixLeft();
		config.addPrefixRight(shifted, leftChild);
	}

	protected void rightArc(final SimpleConfig config) {
		final DependencyVertex shifted = config.removeSuffixLeft();
		config.addPrefixRight(shifted, rightChild);
	}

	protected void leftArc(final SimpleConfig config, final String deprelStr) {
		final DependencyVertex head = config.getSuffixLeft(0);
		final DependencyVertex dep = config.removePrefixRight(0);
		head.addChild(dep);
		dep.setParent(head, new Deprel(deprelStr));
	}

	protected void reduce(final SimpleConfig config, final String deprelStr) {
		final DependencyVertex head = config.getPrefixRight(1);
		final DependencyVertex dep = config.removePrefixRight(0);
		head.addChild(dep);
		dep.setParent(head, new Deprel(deprelStr));
	}

}
