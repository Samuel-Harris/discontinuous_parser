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
 * alpha a^X | b beta => alpha a^X b^N | beta, where X in {L,R}
 * RIGHTARC:
 * alpha a^N | beta => alpha a^R | beta
 * LEFTARC:
 * alpha a^N | beta => alpha a^L | beta
 * REDUCELEFT:
 * alpha a^X b^Y | beta => alpha a^X | beta, where X, Y in {L,R}
 * REDUCERIGHT:
 * alpha a^L b^N | beta => alpha b^N | beta
 */

public class NormalArcEagerParser extends DeterministicParser {
	/**
	 * The first parts of actions.
	 */
	public static final String shift = "shift";
	public static final String rightArc = "rightArc";
	public static final String leftArc = "leftArc";
	public static final String reduceLeft = "reduceLeft";
	public static final String reduceRight = "reduceRight";
	public static final String[] actionNames =
			{shift, rightArc, leftArc, reduceLeft, reduceRight};
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
	public String[] leftArc() {
		return new String[]{leftArc};
	}
	public String[] reduceLeft(final String deprel) {
		return new String[]{reduceLeft, deprel};
	}
	public String[] reduceRight(final String deprel) {
		return new String[]{reduceRight, deprel};
	}
	public String[] reduceLeft() {
		return reduceLeft("_");
	}
	public String[] reduceRight() {
		return reduceRight("_");
	}

	/**
	 * Labels attached to prefix elements.
	 */
	public static final String nil = "nil";
	public static final String leftChild = "leftChild";
	public static final String rightChild = "rightChild";
	public static String[] prefixLabels = {nil, leftChild, rightChild};

	public NormalArcEagerParser(Token[] tokens) {
		super(tokens);
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
		if (config.prefixLength() >= 2) {
			final DependencyVertex v0 = config.getPrefixRight(0);
			final DependencyVertex v1 = config.getPrefixRight(1);
			if (isEdge(v1, v0) &&
					areChildrenAttached(v0) &&
					!config.getLabelRight(0).equals(nil)) {
				final String deprel = deprel(v0);
				return reduceLeft(deprel);
			}
			if (isEdge(v0, v1)) {
				final String deprel = deprel(v1);
				return reduceRight(deprel);
			}
		}
		if (config.getLabelRight(0).equals(nil)) {
			if (isRightChild(config.getPrefixRight(0)))
				return rightArc();
			else
				return leftArc();
		} else if (config.suffixLength() > 0) {
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
				leftArc(config);
				break;
			case reduceLeft:
				reduceLeft(config, action[1]);
				break;
			case reduceRight:
				reduceRight(config, action[1]);
				break;
		}
	}

	@Override
	protected boolean applicable(final SimpleConfig config, final String[] action) {
		switch (action[0]) {
			case shift:
				return config.suffixLength() > 0 &&
						!config.getLabelRight(0).equals(nil);
			case rightArc:
				return config.getLabelRight(0).equals(nil);
			case leftArc:
				return config.getLabelRight(0).equals(nil);
			case reduceLeft:
				return config.prefixLength() >= 2 &&
						!config.getLabelRight(0).equals(nil);
			case reduceRight:
				return config.prefixLength() > 2 &&
						config.getLabelRight(1).equals(leftChild) &&
						config.getLabelRight(0).equals(nil);
			default:
				return false;
		}
	}

	protected void shift(final SimpleConfig config) {
		final DependencyVertex shifted = config.removeSuffixLeft();
		config.addPrefixRight(shifted, nil);
	}

	protected void rightArc(final SimpleConfig config) {
		config.setLabelRight(0, rightChild);
	}

	protected void leftArc(final SimpleConfig config) {
		config.setLabelRight(0, leftChild);
	}

	protected void reduceLeft(final SimpleConfig config, final String deprelStr) {
		final DependencyVertex head = config.getPrefixRight(1);
		final DependencyVertex dep = config.removePrefixRight(0);
		head.addChild(dep);
		dep.setParent(head, new Deprel(deprelStr));
	}

	protected void reduceRight(final SimpleConfig config, final String deprelStr) {
		final DependencyVertex head = config.getPrefixRight(0);
		final DependencyVertex dep = config.removePrefixRight(1);
		head.addChild(dep);
		dep.setParent(head, new Deprel(deprelStr));
	}
}
