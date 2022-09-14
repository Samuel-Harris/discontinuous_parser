/*
 * Copyright (c) 2018. University of St Andrews
 */

package standrews.depextract;

import standrews.aux.Counter;
import standrews.depautomata.HatConfig;
import standrews.depautomata.SimpleConfig;
import standrews.classification.ClassifierFactory;
import standrews.classification.FeatureSpecification;
import standrews.depmethods.HatParser;

public class HatExtractorAnalysis extends HatExtractor {

	private Counter<Integer> fellowCountTo = new Counter<>();
	private Counter<Integer> fellowCountFrom = new Counter<>();
	private int compressionCountTo = 0;
	private int compressionCountFrom = 0;
	private int unpreservedCountTo = 0;
	private int unpreservedCountFrom = 0;
	private int totalCountTo = 0;
	private int totalCountFrom = 0;

	public HatExtractorAnalysis(final FeatureSpecification featSpec,
								final String actionFile, final String fellowFile,
								final String deprelLeftFile, final String deprelRightFile,
								final ClassifierFactory factory) {
		super(featSpec, factory, actionFile, fellowFile, deprelLeftFile, deprelRightFile, false);
	}

	public void extract(final SimpleConfig simpleConfig, final String[] action) {
		HatConfig config = (HatConfig) simpleConfig;
		final int viewMin = featSpec.getIntFeature("viewMin", 0);
		final int viewMax = featSpec.getIntFeature("viewMax", 0);
		final String[] actionCompressed = HatParser.actionToCompression(config, action, viewMin, viewMax);
		final String[] actionUncompressed = HatParser.actionFromCompression(config, actionCompressed, viewMin, viewMax);
		if (action[0].equals("reduceToHat")) {
			int fellow = Integer.parseInt(action[1]);
			if (fellow != -1) {
				// if (simpleConfig.totalLength() < 8)
					// System.out.println(simpleConfig);
			}
			fellowCountTo.incr(fellow);
			if (!actionEquals(action, actionCompressed)) {
				compressionCountTo++;
			}
			if (!actionEquals(action, actionUncompressed)) {
				unpreservedCountTo++;
				// System.out.println(simpleConfig);
			}
			totalCountTo++;
		} else if (action[0].equals("reduceFromHat")) {
			int fellow = Integer.parseInt(action[1]);
			fellowCountFrom.incr(fellow);
			if (!actionEquals(action, actionCompressed)) {
				compressionCountFrom++;
			}
			if (!actionEquals(action, actionUncompressed)) {
				unpreservedCountFrom++;
				// System.out.println(simpleConfig);
			}
			totalCountFrom++;
		}
	}

	private String actionString(final String[] fields) {
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < fields.length; i++)
			buf.append(fields[i] + " ");
		return buf.toString();
	}

	private boolean actionEquals(final String[] fields1, final String[] fields2) {
		if (fields1.length != fields2.length)
			return false;
		for (int i = 0; i < fields1.length; i++)
			if (!fields1[i].equals(fields2[i]))
				return false;
		return true;
	}

	public String analysis() {
		StringBuffer buf = new StringBuffer();
		buf.append("fellow to\n");
		for (int fellow : fellowCountTo.keySet()) {
			int c = fellowCountTo.get(fellow);
			buf.append(String.format("%s %6.4f%%\n", fellow, 100.0 * c / totalCountTo));
		}
		buf.append("fellow from\n");
		for (int fellow : fellowCountFrom.keySet()) {
			int c = fellowCountFrom.get(fellow);
			buf.append(String.format("%s %6.4f%%\n", fellow, 100.0 * c / totalCountFrom));
		}
		if (compressionCountTo > 0)
			buf.append(String.format("Compression (to) %6.4f%%\n",
					100.0 * compressionCountTo / totalCountTo));
		if (compressionCountFrom > 0)
			buf.append(String.format("Compression (from) %6.4f%%\n",
					100.0 * compressionCountFrom / totalCountFrom));
		if (unpreservedCountTo > 0)
			buf.append(String.format("Unpreserved (to) %6.4f%%\n",
					100.0 * unpreservedCountTo / totalCountTo));
		if (unpreservedCountFrom > 0)
			buf.append(String.format("Unpreserved (from) %6.4f%%\n",
					100.0 * unpreservedCountFrom / totalCountFrom));
		return buf.toString();
	}

	public void train() {
		// omit training
	}
}
