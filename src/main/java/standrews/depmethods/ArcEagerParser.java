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
 * alpha a | b beta => alpha a b^R | beta
 * LEFTARC:
 * alpha a^L | b beta => alpha | b beta
 * REDUCE:
 * alpha a b^R | beta => alpha a | beta
 * REDUCECORRECT:
 * alpha a b^L | beta => alpha a | beta
 */

public class ArcEagerParser extends DeterministicParser {
	/**
	 * The first parts of actions.
	 */
	public static final String shift = "shift";
	public static final String rightArc = "rightArc";
	public static final String leftArc = "leftArc";
	public static final String reduce = "reduce";
	public static final String reduceCorrect = "reduceCorrect";
	public static final String[] actionNames = {shift, rightArc, leftArc,
			reduce, reduceCorrect};
	public String[] actionNames() {
		return actionNames;
	}
	public String[] none() {
		return new String[]{};
	}
	public String[] shift() {
		return new String[]{shift};
	}
	public String[] rightArc(final String deprel) {
		return new String[]{rightArc, deprel};
	}
	public String[] leftArc(final String deprel) {
		return new String[]{leftArc, deprel};
	}
	public String[] reduce() {
		return new String[]{reduce};
	}
	public String[] reduceCorrect(final String deprel) {
		return new String[]{reduceCorrect, deprel};
	}
	public String[] rightArc() {
		return rightArc("_");
	}
	public String[] leftArc() {
		return leftArc("_");
	}
	public String[] reduceCorrect() {
		return reduceCorrect("_");
	}

	/**
	 * Labels attached to prefix elements.
	 */
	public static final String leftChild = "leftChild";
	public static final String rightChild = "rightChild";
	public static final String[] prefixLabels = {leftChild, rightChild};

	/**
	 * Spurious ambiguity resolved in favour of doing reduce.
	 */
	protected boolean earlyReduce;

	protected boolean strict;

	public ArcEagerParser(final Token[] tokens,
						  final boolean early, final boolean strict) {
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
			if (isEdge(v0, v1)) {
				final String deprel1 = deprel(v1);
				return rightArc(deprel1);
			}
			if (isEdge(v1, v0)) {
				final String deprel0 = deprel(v0);
				return leftArc(deprel0);
			}
		}
		if (config.prefixLength() >= 2) {
			if (earlyReduce) {
				if (isRightChild(v0) && areChildrenAttached(v0)) {
					return reduceOrCorrect(config);
				}
			} else if (config.suffixLength() == 0) {
				return reduceOrCorrect(config);
			} else {
				final DependencyVertex v1 = config.getSuffixLeft(0);
				for (int i = 1; i < config.prefixLength(); i++) {
					final DependencyVertex v = config.getPrefixRight(i);
					if (isLink(v, v1))
						return reduceOrCorrect(config);
				}
			}
		}
		if (config.suffixLength() >= 1)
			return shift();
		return none();
	}

	private String[] reduceOrCorrect(final SimpleConfig config) {
		final String depLabel = config.getLabelRight(0);
		if (depLabel.equals(leftChild)) {
			final DependencyVertex v = config.getPrefixRight(0);
			final String deprel = deprel(v);
			return reduceCorrect(deprel);
		} else {
			return reduce();
		}
	}

	@Override
	protected void apply(SimpleConfig config, String[] action) {
		switch (action[0]) {
			case shift:
				shift(config);
				break;
			case rightArc:
				rightArc(config, action[1]);
				break;
			case leftArc:
				leftArc(config, action[1]);
				break;
			case reduce:
				reduce(config);
				break;
			case reduceCorrect:
				reduceCorrect(config, action[1]);
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
				return config.prefixLength() >= 2 && config.suffixLength() >= 1 &&
						config.getLabelRight(0).equals(leftChild);
			case reduce:
				return config.prefixLength() >= 2 && config.getLabelRight(0).equals(rightChild);
			case reduceCorrect:
				return config.prefixLength() >= 2 && config.getLabelRight(0).equals(leftChild);
			default:
				return false;
		}
	}

	protected void shift(final SimpleConfig config) {
		final DependencyVertex shifted = config.removeSuffixLeft();
		config.addPrefixRight(shifted, leftChild);
	}

	protected void rightArc(final SimpleConfig config, final String deprelStr) {
		final DependencyVertex shifted = config.removeSuffixLeft();
		final DependencyVertex head = config.getPrefixRight(0);
		head.addChild(shifted);
		shifted.setParent(head, new Deprel(deprelStr));
		config.addPrefixRight(shifted, rightChild);
	}

	protected void leftArc(final SimpleConfig config, final String deprelStr) {
		final DependencyVertex head = config.getSuffixLeft(0);
		final DependencyVertex dep = config.removePrefixRight(0);
		head.addChild(dep);
		dep.setParent(head, new Deprel(deprelStr));
	}

	protected void reduce(final SimpleConfig config) {
		config.removePrefixRight(0);
	}

	protected void reduceCorrect(final SimpleConfig config, final String deprelStr) {
		final DependencyVertex dep = config.getPrefixRight(0);
		final DependencyVertex head = config.getPrefixRight(1);
		head.addChild(dep);
		dep.setParent(head, new Deprel(deprelStr));
		config.removePrefixRight(0);
	}

}
