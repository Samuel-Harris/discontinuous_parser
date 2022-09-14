/*
 * Copyright (c) 2019. University of St Andrews
 */

package standrews.depmethods;


import standrews.depautomata.DependencyVertex;
import standrews.depautomata.SimpleConfig;
import standrews.depbase.Deprel;
import standrews.depbase.Token;

public class SimpleParser extends DeterministicParser {
	/**
	 * The first parts of actions.
	 */
	public static final String shift = "shift";
	public static final String reduceLeft = "reduceLeft";
	public static final String reduceRight = "reduceRight";
	public static final String[] actionNames = {shift, reduceLeft, reduceRight};
	public String[] actionNames() {
		return actionNames;
	}
	public String[] none() {
		return new String[]{};
	}
	public String[] shift() {
		return new String[]{shift};
	}
	public String[] reduceLeft(final String deprel) {
		return new String[]{reduceLeft, deprel};
	}
	public String[] reduceRight(final String deprel) {
		return new String[]{reduceRight, deprel};
	}

	/**
	 * Labels attached to prefix elements.
	 */
	public static final String[] prefixLabels = {""};

	/**
	 * During training should left dependents be attached before right dependents?
	 */
	protected boolean leftDependentsFirst = false;

	public SimpleParser(Token[] tokens) {
		super(tokens);
	}

	public void setLeftDependentsFirst(final boolean b) {
		leftDependentsFirst = b;
	}

	@Override
	protected SimpleConfig makeInitialConfig(final Token[] tokens) {
		return new SimpleConfig(tokens);
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
					areChildrenAttached(v0)) {
				final String deprel = deprel(v0);
				return reduceLeft(deprel);
			}
			if (isEdge(v0, v1) &&
					areChildrenAttached(v1) &&
					(leftDependentsFirst || areRightChildrenAttached(v0))) {
				final String deprel = deprel(v1);
				return reduceRight(deprel);
			}
		}
		if (config.suffixLength() > 0)
			return shift();
		return none();
	}

	/**
	 * Are preceding children already attached?
	 * If the next child is left child, then all right children should
	 * already be attached. If the next child is right child, then
	 * children between head and next child should already be attached.
	 *
	 * @param vParse  Vertex in parser.
	 * @param cParse  Possibly next vertex to be attached to vParse.
	 * @return Whether previous vertices were already attached.
	 */
	protected boolean arePreviousChildrenAttached(final DependencyVertex vParse,
												  final DependencyVertex cParse) {
		final DependencyVertex vGold = goldVertex(vParse);
		final DependencyVertex cGold = goldVertex(cParse);
		return vGold.getPreviousChildren(cGold, leftDependentsFirst).length == vParse.getChildren().length;
	}

	@Override
	protected void apply(SimpleConfig config, String[] action) {
		switch (action[0]) {
			case shift:
				shift(config);
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
				return config.suffixLength() > 0;
			case reduceLeft:
				return config.prefixLength() >= 2;
			case reduceRight:
				return config.prefixLength() > 2;
			default:
				return false;
		}
	}

	protected void shift(final SimpleConfig config) {
		final DependencyVertex shifted = config.removeSuffixLeft();
		config.addPrefixRight(shifted);
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
