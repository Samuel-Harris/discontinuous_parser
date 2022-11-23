/*
 * Copyright (c) 2019. University of St Andrews
 */

package standrews.constmethods;

import standrews.aux.PropertyWeights;
import standrews.constautomata.HatConfig;
import standrews.constautomata.SimpleConfig;
import standrews.constbase.*;
import standrews.constextract.WholeHatExtractor;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class WholeHatParser extends HatParser {
	/**
	 * The first parts of actions.
	 */
	public static final String shift = "shift";
	public static final String reduce = "reduce";
	public static final String reduceRoots = "reduceRoots";
	public static final String[] actionNames = {shift, reduce, reduceRoots};
	public String[] actionNames() {
		 return actionNames;
	}
	public String[] none() {
		return new String[]{};
	}
	public String[] shift() {
		return new String[]{shift};
	}
	public String[] reduce(final String cat, final int hdIndex,
						   final int[] children) {
		return Stream.concat(Stream.of(reduce, cat),
				Stream.concat(
						IntStream.of(hdIndex)
						.mapToObj(Integer::toString),
						Arrays.stream(children)
						.mapToObj(Integer::toString)))
				.toArray(String[]::new);
	}
	public String[] reduceRoots(final int[] children) {
		return Stream.concat(Stream.of(reduceRoots),
				Arrays.stream(children)
						.mapToObj(Integer::toString))
				.toArray(String[]::new);
	}


	public WholeHatParser(final ConstTree tree) {
		super(tree);
	}

	protected String[] getAction(final SimpleConfig simpleConfig) {
		final HatConfig config = (HatConfig) simpleConfig;
		final int i0 = config.getAbsoluteHatIndex();
		final ConstNode n0 = config.getStackLeft(i0);
		final ConstInternal parent = goldParent(n0);
		if (parent != null) {
			final int[] children = withParent(parent, config.stackList());
			if (parent.getChildren().length == children.length) {
				String cat = parent.getCat();
				int hdIndex = parent.getHeadIndex();
				return reduce(cat, hdIndex, children);
			}
		} else if (config.inputLength() == 0) {
			final int[] children = withRootParent(config.stackList());
			if (goldRoots().length == children.length) {
				return reduceRoots(children);
			}
		}
		if (config.inputLength() > 0)
			return shift();
		return none();
	}

	protected void apply(final SimpleConfig simpleConfig, final String[] action) {
		final HatConfig config = (HatConfig) simpleConfig;
		switch (action[0]) {
			case shift:
				shift(config);
				break;
			case reduce: {
				String cat = action[1];
				int hdIndex = Integer.parseInt(action[2]);
				int[] children = Arrays.stream(Arrays.copyOfRange(action, 3, action.length))
						.mapToInt(Integer::parseInt)
						.toArray();
				reduce(config, cat, hdIndex, children);
				break;
			}
			case reduceRoots:
				int[] children = Arrays.stream(Arrays.copyOfRange(action, 1, action.length))
						.mapToInt(Integer::parseInt)
						.toArray();
				reduceRoots(config, children);
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
			case reduce: {
				for (int i = 3; i < action.length; i++) {
					int intAc = Integer.parseInt(action[i]);
					if (intAc == 0 || intAc >= config.stackLength())
						return false;
				}
				final int hat = config.getAbsoluteHatIndex();
				return config.stackLength() >= 2 && !longChain(config.getStackLeft(hat));
			}
			case reduceRoots: {
				Set<Integer> roots = new TreeSet<>();
				Set<Integer> stackElems = new TreeSet<>();
				for (int i = 1; i < action.length; i++) {
					roots.add(Integer.parseInt(action[i]));
				}
				for (int i = 1; i < config.stackLength(); i++) {
					stackElems.add(i);
				}
				return stackElems.equals(roots) && config.inputLength() == 0;
			}
			default:
				return false;
		}
	}

	protected void reduce(final HatConfig config, final String cat,
						  final int hdIndex,
						  final int[] children) {
		final int hatIndex = config.getAbsoluteHatIndex();
		final ConstNode hatChild = config.getStackLeft(hatIndex);
		final ConstInternal goldParent = goldTree.getParent(hatChild);
		final String id = goldParent == null ? "" : goldParent.getId();
		final ConstInternal parent = new ConstInternal(id, cat);
		for (int i = hdIndex; i < children.length; i++) {
			final ConstNode child = config.removeStackLeft(children[i]-(i-hdIndex));
			parent.addChildRight(child);
		}
		for (int i = hdIndex-1; i >= 0; i--) {
			final ConstNode child = config.removeStackLeft(children[i]);
			parent.addChildLeft(child);
		}
		final int newHat = children[hdIndex] - hdIndex;
		config.addStackRight(parent, newHat);
		config.setAbsoluteHatIndex(newHat);
	}

	protected void reduceRoots(final HatConfig config, final int[] children) {
		final ConstInternal root = (ConstInternal) config.getStackLeft(0);
		for (int i = 0; i < children.length; i++) {
			final ConstNode child = config.removeStackLeft(children[i]-i);
			root.addChildRight(child);
		}
	}

	public void observe(final WholeHatExtractor extractor) {
		final HatConfig config = makeInitialConfig(goldTree);
		while (!config.isFinal()) {
			final String[] action = getAction(config);
			if (action.length == 0) {
				fail("training", config);
				break;
			} else if (!applicable(config, action)) {
				fail("training inapplicable action " +
					actionString(action), config);
				break;
			}
			extractor.extract(config, action);
			apply(config, action);
		}
	}

	public static final String rootWeight = "rootWeight";
	public static final String rootCatWeight = "rootCatWeight";
	public static final String catWeight = "catWeight";
	public static final String branchWeight = "branchWeight";
	public static final String[] weightedProperties =
			{rootWeight, rootCatWeight, catWeight, branchWeight};

	public PropertyWeights prob(final WholeHatExtractor extractor) {
		final HatConfig config = makeInitialConfig(goldTree);
		PropertyWeights weights = new PropertyWeights(weightedProperties);
		while (!config.isFinal()) {
			final String[] action = getAction(config);
			if (action.length == 0) {
				fail("training", config);
				break;
			}
			PropertyWeights stepWeights = extractor.prob(config, action);
			weights.add(stepWeights);
			apply(config, action);
		}
		return weights;
	}

}
