/*
 * Copyright (c) 2019. University of St Andrews
 */

package standrews.depmethods;

import standrews.depautomata.DependencyVertex;
import standrews.depautomata.SimpleConfig;
import standrews.depbase.Id;
import standrews.depbase.Token;

import java.util.Map;

public class SwapParser extends SimpleParser {
	/**
	 * The first parts of actions.
	 */
	public static final String shift = "shift";
	public static final String swap = "swap";
	public static final String reduceLeft = "reduceLeft";
	public static final String reduceRight = "reduceRight";
	public static final String[] actionNames = {shift, swap, reduceLeft, reduceRight};
	public String[] actionNames() {
		return actionNames;
	}
	public String[] none() {
		return new String[]{};
	}
	public String[] shift() {
		return new String[]{shift};
	}
	public String[] swap() {
		return new String[]{swap};
	}
	public String[] reduceLeft(final String deprel) {
		return new String[]{reduceLeft, deprel};
	}
	public String[] reduceRight(final String deprel) {
		return new String[]{reduceRight, deprel};
	}

	/**
	 * Vertices mapped to representative vertex in maximal projective components.
	 */
	private Map<Id,Id> mpc;

	/**
	 * Vertices mapped to projective index.
	 */
	private Map<Id,Integer> pIndex;

	/**
	 * Construct parser from input.
	 *
	 * @param tokens Input sentence.
	 */
	public SwapParser(final Token[] tokens) {
		super(tokens);
	}

	protected SimpleConfig makeInitialConfig(final Token[] tokens) {
		return new SimpleConfig(tokens);
	}

	protected void prepareTraining() {
		super.prepareTraining();
		mpc = graph.componentMap();
		pIndex = graph.projectiveOrder();
	}

	/**
	 * Determine which action is needed in current configuration.
	 *
	 * @param config Current configuration.
	 * @return The action.
	 */
	protected String[] getAction(final SimpleConfig config) {
		if (config.prefixLength() >= 2) {
			final DependencyVertex v0 = config.getPrefixRight(0);
			final DependencyVertex v1 = config.getPrefixRight(1);
			final Token t0 = v0.getToken();
			final Token t1 = v1.getToken();
			if (graph.isDependentOf(t0.id, t1.id) &&
					areChildrenAttached(v0)) {
				final String deprel = deprel(t0);
				return reduceLeft(deprel);
			}
			if (graph.isDependentOf(t1.id, t0.id) &&
					areChildrenAttached(v1) &&
					(leftDependentsFirst || areRightChildrenAttached(v0))) {
				final String deprel = deprel(t1);
				return reduceRight(deprel);
			}
			if (pIndex.get(t0.id) < pIndex.get(t1.id) &&
					(config.suffixLength() == 0 ||
					!mpc.get(t0.id).equals(mpc.get(config.getSuffixLeft(0).getToken().id)))) {
				return swap();
			}
		}
		if (config.suffixLength() > 0)
			return shift();
		return none();
	}

	protected void apply(final SimpleConfig config, final String[] action) {
		switch (action[0]) {
			case shift:
				shift(config);
				break;
			case swap:
				swap(config);
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
			case swap:
				return config.prefixLength() > 2 &&
						config.getPrefixRight(0).compareTo(config.getPrefixRight(1)) > 0;
			case reduceLeft:
				return config.prefixLength() >= 2;
			case reduceRight:
				return config.prefixLength() > 2;
			default:
				return false;
		}
	}

	protected void swap(final SimpleConfig config) {
		DependencyVertex swapped = config.removePrefixRight(1);
		config.addSuffixLeft(swapped);
	}
}
