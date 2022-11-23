/*
 * Copyright (c) 2019. University of St Andrews
 */

package standrews.constmethods;

import javafx.util.Pair;
import standrews.constautomata.SimpleConfig;
import standrews.constbase.ConstInternal;
import standrews.constbase.ConstLeaf;
import standrews.constbase.ConstNode;
import standrews.constbase.ConstTree;

import java.util.Optional;

public class SimpleParser extends DeterministicParser {
	/**
	 * The first parts of actions.
	 */
	public static final String shift = "shift";
	public static final String reduceUp = "reduceUp";
	public static final String reduceLeft = "reduceLeft";
	public static final String reduceRight = "reduceRight";
	public static final String[] actionNames =
			{shift, reduceUp, reduceLeft, reduceRight};
	public String[] actionNames() {
		return actionNames;
	}
	public String[] none() {
		return new String[] {};
	}
	public String[] shift() {
		return new String[]{shift};
	}
	public String[] reduceUp(final String cat) {
		return new String[]{reduceUp, cat};
	}
	public String[] reduceLeft() {
		return new String[]{reduceLeft};
	}
	public String[] reduceRight() {
		return new String[]{reduceRight};
	}

	/**
	 * During training should left dependents be attached before right dependents?
	 */
	protected boolean leftDependentsFirst = false;

	public SimpleParser(final ConstTree goldTree) {
		super(goldTree);
	}

	public void setLeftDependentsFirst(final boolean b) {
		leftDependentsFirst = b;
	}

	@Override
	protected SimpleConfig makeInitialConfig(final ConstTree goldTree) {
		SimpleConfig config = new SimpleConfig(goldTree.getId(), goldTree.getLeaves());
		config.goldTree = goldTree;
		return config;
	}

	/**
	 * Determine which action is needed in current configuration.
	 *
	 * @param config Current configuration.
	 * @return The action.
	 */
	@Override
	protected String[] getAction(final SimpleConfig config) {
		if (config.stackLength() >= 2) {
			final ConstNode n0 = config.getStackRight(0);
			final ConstNode n1 = config.getStackRight(1);
			if (goldParent(n0) != null && areChildrenAttached(n0) &&
					isHead(n0)) {
				return reduceUp(goldParent(n0).getCat());
			}
			if (hasChildGold(n1, n0) && areChildrenAttached(n0)) {
				return reduceLeft();
			}
			if (hasChildGold(n0, n1) && areChildrenAttached(n1) &&
					(leftDependentsFirst || areRightChildrenAttached(n0))) {
				return reduceRight();
			}
		}
		if (config.inputLength() > 0)
			return shift();
		return none();
	}

	protected boolean arePreviousChildrenAttached(final ConstNode nodeParse,
												  final ConstNode childParse) {
		if (nodeParse instanceof ConstLeaf)
			return false;
		if (nodeParse.isTop())
			return true;
		ConstInternal parentParse = (ConstInternal) nodeParse;
		final ConstInternal parentGold = goldInternal(parentParse);
		final ConstNode childGold = goldNode(childParse);
		return parentGold.getPreviousChildren(childGold, leftDependentsFirst).length == parentParse.getChildren().length;
	}

	@Override
	protected void apply(SimpleConfig config, String[] action) {
		switch (action[0]) {
			case shift:
				shift(config);
				break;
			case reduceUp:
				reduceUp(config, action[1]);
				break;
			case reduceLeft:
				reduceLeft(config);
				break;
			case reduceRight:
				reduceRight(config);
				break;
			default:
				fail("apply", config);
				break;
		}
	}

	@Override
	protected boolean applicable(final SimpleConfig config, final String[] action) {
		switch (action[0]) {
			case shift:
				return config.inputLength() > 0;
			case reduceUp:
				return config.stackLength() >= 2 && !longChain(config.getStackRight(0));
			case reduceLeft:
				return config.stackLength() >= 2 &&
						config.getStackRight(1) instanceof ConstInternal;
			case reduceRight:
				return config.stackLength() > 2 &&
						config.getStackRight(0) instanceof ConstInternal;
			default:
				return false;
		}
	}

	protected void shift(final SimpleConfig config) {
		final ConstLeaf shifted = config.removeInputLeft();
		config.addStackRight(shifted);
	}

	protected void reduceUp(final SimpleConfig config, final String cat) {
		final ConstNode child = config.removeStackRight(0);
		final ConstInternal goldParent = goldTree.getParent(child);
		final String id = goldParent == null ? "" : goldParent.getId();
		final ConstInternal parent = new ConstInternal(id, cat);
		parent.addChildRight(child);
		config.addStackRight(parent);
	}

	protected void reduceLeft(final SimpleConfig config) {
		final ConstInternal parent = (ConstInternal) config.getStackRight(1);
		final ConstNode child = config.removeStackRight(0);
		parent.addChildRight(child);
	}

	protected void reduceRight(final SimpleConfig config) {
		final ConstInternal parent = (ConstInternal) config.getStackRight(0);
		final ConstNode child = config.removeStackRight(1);
		parent.addChildLeft(child);
	}
}
