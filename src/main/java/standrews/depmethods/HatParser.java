/*
 * Copyright (c) 2019. University of St Andrews
 */

package standrews.depmethods;

import standrews.depautomata.DependencyVertex;
import standrews.depautomata.HatConfig;
import standrews.depautomata.SimpleConfig;
import standrews.depbase.Deprel;
import standrews.depbase.Token;

public class HatParser extends SimpleParser {
	/**
	 * The first parts of actions.
	 */
	public static final String shift = "shift";
	public static final String reduceToHat = "reduceToHat";
	public static final String reduceFromHat = "reduceFromHat";
	public static final String[] actionNames = {shift, reduceToHat, reduceFromHat};
	public String[] actionNames() {
		 return actionNames;
	}
	public String[] none() {
		return new String[]{};
	}
	public String[] shift() {
		return new String[]{shift};
	}
	public String[] reduceToHat(final int i, final String deprel) {
		return new String[]{reduceToHat, "" + i, deprel};
	}
	public String[] reduceFromHat(final int i, final String deprel) {
		return new String[]{reduceFromHat, "" + i, deprel};
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
	 * @param tokens  Input string.
	 * @param viewMin How far back features can look.
	 * @param viewMax How far forward features can look.
	 */
	public HatParser(final Token[] tokens, final int viewMin, final int viewMax) {
		super(tokens);
		this.viewMin = viewMin;
		this.viewMax = viewMax;
	}

	protected HatConfig makeInitialConfig(final Token[] tokens) {
		return new HatConfig(tokens);
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
		final DependencyVertex v0 = config.getPrefixLeft(i0);
		final Token t0 = v0.getToken();
		for (int i1 = config.prefixLength() - 1; i1 >= 0; i1--) {
			if (i0 != i1) {
				final DependencyVertex v1 = config.getPrefixLeft(i1);
				final Token t1 = v1.getToken();
				if (graph.isDependentOf(t1.id, t0.id) &&
						areChildrenAttached(v1) &&
						arePreviousChildrenAttached(v0, v1)) {
					final String deprel = deprel(t1);
					final int relIndex = config.getHatRelativeIndex(i1);
					return reduceToHat(relIndex, deprel);
				}
				if (graph.isDependentOf(t0.id, t1.id) &&
						areChildrenAttached(v0) &&
						arePreviousChildrenAttached(v1, v0)) {
					final String deprel = deprel(t0);
					final int relIndex = config.getHatRelativeIndex(i1);
					return reduceFromHat(relIndex, deprel);
				}
			}
		}
		if (config.suffixLength() > 0)
			return shift();
		return none();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void apply(final SimpleConfig simpleConfig, final String[] action) {
		final HatConfig config = (HatConfig) simpleConfig;
		switch (action[0]) {
			case shift:
				shift(config);
				break;
			case reduceToHat:
				reduceToHat(config, Integer.parseInt(action[1]), action[2]);
				break;
			case reduceFromHat:
				reduceFromHat(config, Integer.parseInt(action[1]), action[2]);
				break;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected boolean applicable(final SimpleConfig simpleConfig, final String[] action) {
		final HatConfig config = (HatConfig) simpleConfig;
		if (action[0].equals(shift))
			return config.suffixLength() > 0;
		else {
			final int fellow = config.getHatAbsoluteIndex(Integer.parseInt(action[1]));
			switch (action[0]) {
				case reduceToHat:
					return 0 < fellow && fellow < config.prefixLength();
				case reduceFromHat:
					return config.getAbsoluteHatIndex() > 0 &&
							0 <= fellow && fellow < config.prefixLength() &&
							(0 < fellow || config.prefixLength() == 2 || config.suffixLength() > 0);
				default:
					return false;
			}
		}
	}

	protected void shift(final HatConfig config) {
		final DependencyVertex shifted = config.removeSuffixLeft();
		config.addPrefixRight(shifted);
		config.setAbsoluteHatIndex(config.prefixLength() - 1);
	}

	protected void reduceToHat(final HatConfig config, final int i, final String deprelStr) {
		final DependencyVertex head = config.getPrefixHat(0);
		final DependencyVertex dep = config.removePrefixHat(i);
		head.addChild(dep);
		dep.setParent(head, new Deprel(deprelStr));
		if (i < 0)
			config.decrementHatIndex();
	}

	protected void reduceFromHat(final HatConfig config, final int i, final String deprelStr) {
		final DependencyVertex head = config.getPrefixHat(i);
		final DependencyVertex dep = config.removePrefixHat(0);
		head.addChild(dep);
		dep.setParent(head, new Deprel(deprelStr));
		config.setRelativeHatIndex(i);
		if (i > 0)
			config.decrementHatIndex();
	}

	/**
	 * Convert action with numbered fellow to one with direction+POS.
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
				String pos = config.getPrefixLeft(abs).getToken().upos.toString();
				fellow = rel < viewMin ?
						compressionLeft(pos) : compressionRight(pos);
			}
			return new String[]{actionName, fellow, actionNumeric[2]};
		}
		return actionNumeric;
	}

	public static String compressionLeft(final String pos) {
		return "left " + pos;
	}

	public static String compressionRight(final String pos) {
		return "right " + pos;
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
				fellow = firstRelIndexLeft(config, parts[1], viewMin);
			} else if (parts.length == 2 && parts[0].equals("right")) {
				fellow = firstRelIndexRight(config, parts[1], viewMax);
			}
			return fellow == null ?
					new String[]{} :
					new String[]{actionName, fellow, actionSymbolic[2]};
		}
		return actionSymbolic;
	}

	/**
	 * For relative index of first element with POS beyond viewMin.
	 */
	protected static String firstRelIndexLeft(final HatConfig config, final String pos, final int viewMin) {
		for (int i = config.getAbsoluteHatIndex() + viewMin - 1; i > 0; i--) {
			if (config.getPrefixLeft(i).getToken().upos.toString().equals(pos))
				return "" + config.getHatRelativeIndex(i);
		}
		return null;
	}

	/**
	 * For relative index of first element with POS beyond viewMax.
	 */
	protected static String firstRelIndexRight(final HatConfig config, final String pos, final int viewMax) {
		for (int i = config.getAbsoluteHatIndex() + viewMax + 1; i < config.prefixLength(); i++) {
			if (config.getPrefixLeft(i).getToken().upos.toString().equals(pos))
				return "" + config.getHatRelativeIndex(i);
		}
		return null;
	}
}
