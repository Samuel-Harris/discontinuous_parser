/*
 * Copyright (c) 2019. University of St Andrews
 */

package standrews.constmethods;

import standrews.constautomata.HatConfig;
import standrews.constautomata.SimpleConfig;
import standrews.constbase.ConstInternal;
import standrews.constbase.ConstLeaf;
import standrews.constbase.ConstNode;
import standrews.constbase.ConstTree;

public class HatParser extends SimpleParser {
	/**
	 * The first parts of actions.
	 */
	public static final String shift = "shift";
	public static final String reduceUpHat = "reduceUpHat";
	public static final String reduceToHat = "reduceToHat";
	public static final String reduceFromHat = "reduceFromHat";
	public static final String[] actionNames =
			{shift, reduceUpHat, reduceToHat, reduceFromHat};
	public String[] actionNames() {
		return actionNames;
	}
	public String[] none() {
		return new String[]{};
	}
	public String[] shift() {
		return new String[]{shift};
	}
	public String[] reduceUpHat(final String cat) {
		return new String[]{reduceUpHat, cat};
	}
	public String[] reduceToHat(final int i) {
		return new String[]{reduceToHat, "" + i};
	}
	public String[] reduceFromHat(final int i) {
		return new String[]{reduceFromHat, "" + i};
	}

	/**
	 * How far back can features look (negative or zero).
	 */
	public final int viewMin;
	/**
	 * How far forward can features look (positive or zero).
	 */
	public final int viewMax;

	/**
	 * @param tree Input tree.
	 * @param viewMin How far back features can look.
	 * @param viewMax How far forward features can look.
	 */
	public HatParser(final ConstTree tree, final int viewMin, final int viewMax) {
		super(tree);
		this.viewMin = viewMin;
		this.viewMax = viewMax;
	}

	protected HatConfig makeInitialConfig(final ConstTree goldTree) {
		HatConfig config = new HatConfig(goldTree.getId(), goldTree.getLeaves());
		config.goldTree = goldTree;
		return config;
	}

	/**
	 * Determine which action is needed in current configuration.
	 *
	 * @param simpleConfig Current configuration.
	 * @return The action.
	 */
	@Override
	protected String[] getAction(final SimpleConfig simpleConfig) {
		final HatConfig config = (HatConfig) simpleConfig;
		final int i0 = config.getAbsoluteHatIndex();
		final ConstNode n0 = config.getStackLeft(i0);
		for (int i1 = config.stackLength() - 1; i1 >= 0; i1--) {
			if (i0 != i1) {
				final ConstNode n1 = config.getStackLeft(i1);
				if (goldParent(n0) != null && areChildrenAttached(n0) &&
						isHead(n0)) {
					return reduceUpHat(goldParent(n0).getCat());
				}
				if (hasChildGold(n0, n1) && areChildrenAttached(n1) &&
						arePreviousChildrenAttached(n0, n1)) {
					final int relIndex = config.getHatRelativeIndex(i1);
					return reduceToHat(relIndex);
				}
				if (hasChildGold(n1, n0) &&	areChildrenAttached(n0) &&
						arePreviousChildrenAttached(n1, n0)) {
					final int relIndex = config.getHatRelativeIndex(i1);
					return reduceFromHat(relIndex);
				}
			}
		}
		if (config.inputLength() > 0)
			return shift();
		return none();
	}

	@Override
	protected void apply(final SimpleConfig simpleConfig, final String[] action) {
		final HatConfig config = (HatConfig) simpleConfig;
		switch (action[0]) {
			case shift:
				shift(config);
				break;
			case reduceUpHat:
				reduceUpHat(config, action[1]);
				break;
			case reduceToHat:
				reduceToHat(config, Integer.parseInt(action[1]));
				break;
			case reduceFromHat:
				reduceFromHat(config, Integer.parseInt(action[1]));
				break;
			default:
				fail("apply", config);
				break;
		}
	}

	protected boolean applicable(final SimpleConfig simpleConfig, final String[] action) {
		final HatConfig config = (HatConfig) simpleConfig;
		switch (action[0]) {
			case shift: {
				return config.inputLength() > 0;
			}
			case reduceUpHat: {
				final int hat = config.getAbsoluteHatIndex();
				return config.stackLength() >= 2 && hat != 0 && !longChain(config.getStackLeft(hat));
			}
			case reduceToHat: {
				final int fellow = config.getHatAbsoluteIndex(Integer.parseInt(action[1]));
				final int hat = config.getAbsoluteHatIndex();
				return config.getStackLeft(hat) instanceof ConstInternal &&
						0 < fellow && fellow < config.stackLength();
			}
			case reduceFromHat: {
				final int fellow = config.getHatAbsoluteIndex(Integer.parseInt(action[1]));
				return config.getAbsoluteHatIndex() > 0 &&
						0 <= fellow && fellow < config.stackLength() &&
						config.getStackLeft(fellow) instanceof ConstInternal &&
						(0 < fellow || config.stackLength() == 2 || config.inputLength() > 0);
			}
			default:
				return false;
		}
	}

	protected void shift(final HatConfig config) {
		final ConstLeaf shifted = config.removeInputLeft();
		config.addStackRight(shifted);
		config.setAbsoluteHatIndex(config.stackLength() - 1);
	}

	protected void reduceUpHat(final HatConfig config, final String cat) {
		final int i = config.getAbsoluteHatIndex();
		final ConstNode child = config.removeStackLeft(i);
		final ConstInternal goldParent = goldTree.getParent(child);
		final String id = goldParent == null ? "" : goldParent.getId();
		final ConstInternal parent = new ConstInternal(id, cat);
		parent.addChildRight(child);
		config.addStackRight(parent, i);
	}

	protected void reduceToHat(final HatConfig config, final int i) {
		final ConstInternal parent = (ConstInternal) config.getStackHat(0);
		final ConstNode child = config.removeStackHat(i);
		if (i < 0)
			parent.addChildLeft(child);
		else
			parent.addChildRight(child);
		if (i < 0)
			config.decrementHatIndex();
	}

	protected void reduceFromHat(final HatConfig config, final int i) {
		final ConstInternal parent = (ConstInternal) config.getStackHat(i);
		final ConstNode child = config.removeStackHat(0);
		if (i < 0)
			parent.addChildRight(child);
		else
			parent.addChildLeft(child);
		config.setRelativeHatIndex(i);
		if (i > 0)
			config.decrementHatIndex();
	}

	/**
	 * Convert action with numbered fellow to one with direction+CAT.
	 */
	protected String[] actionToCompression(final HatConfig config,
										   final String[] actionNumeric) {
		return actionToCompression(config, actionNumeric, viewMin, viewMax);
	}

	public static String[] actionToCompression(final HatConfig config,
												  final String[] actionNumeric,
												  final int viewMin,
												  final int viewMax) {
		String actionName = actionNumeric[0];
		if (actionName.equals(reduceToHat) || actionName.equals(reduceFromHat)) {
			String fellow = actionNumeric[1];
			final int rel = Integer.parseInt(fellow);
			if (rel < viewMin || rel > viewMax) {
				final int abs = config.getHatAbsoluteIndex(rel);
				String cat = config.getStackLeft(abs).getCat();
				fellow = rel < viewMin ?
						compressionLeft(cat) : compressionRight(cat);
			}
			return new String[]{actionName, fellow};
		}
		return actionNumeric;
	}

	public static String compressionLeft(final String cat) {
		return "left " + cat;
	}

	public static String compressionRight(final String cat) {
		return "right " + cat;
	}

	/**
	 * Convert action with fellow indicated with direction+POS to numbered fellow.
	 */
	protected String[] actionFromCompression(final HatConfig config,
											 final String[] actionSymbolic) {
		return actionFromCompression(config, actionSymbolic, viewMin, viewMax);
	}

	public static String[] actionFromCompression(final HatConfig config,
													final String[] actionSymbolic,
													final int viewMin,
													final int viewMax) {
		String actionName = actionSymbolic[0];
		if (actionName.equals(reduceToHat) || actionName.equals(reduceFromHat)) {
			String fellow = actionSymbolic[1];
			final String[] parts = fellow.split(" ");
			if (parts.length == 2 && parts[0].equals("left")) {
				fellow = firstCatIndexLeft(config, parts[1], viewMin);
			} else if (parts.length == 2 && parts[0].equals("right")) {
				fellow = firstCatIndexRight(config, parts[1], viewMax);
			}
			return fellow == null ?
					new String[]{} :
					new String[]{actionName, fellow};
		}
		return actionSymbolic;
	}

	/**
	 * For relative index of first element with cat beyond viewMin.
	 */
	protected static String firstCatIndexLeft(
			final HatConfig config, final String cat, final int viewMin) {
		for (int i = config.getAbsoluteHatIndex() + viewMin - 1; i > 0; i--) {
			if (config.getStackLeft(i).getCat().equals(cat))
				return "" + config.getHatRelativeIndex(i);
		}
		return null;
	}

	/**
	 * For relative index of first element with cat beyond viewMax.
	 */
	protected static String firstCatIndexRight(
			final HatConfig config, final String cat, final int viewMax) {
		for (int i = config.getAbsoluteHatIndex() + viewMax + 1; i < config.stackLength(); i++) {
			if (config.getStackLeft(i).getCat().equals(cat))
				return "" + config.getHatRelativeIndex(i);
		}
		return null;
	}

}
