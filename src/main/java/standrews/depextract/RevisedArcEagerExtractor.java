/*
 * Copyright (c) 2018. University of St Andrews
 */

package standrews.depextract;

import standrews.depautomata.DependencyVertex;
import standrews.depautomata.SimpleConfig;
import standrews.classification.ClassifierFactory;
import standrews.classification.FeatureSpecification;
import standrews.classification.Features;
import standrews.depbase.Upos;
import standrews.depmethods.RevisedArcEagerParser;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;

public class RevisedArcEagerExtractor extends ArcEagerExtractor {

	public RevisedArcEagerExtractor(final FeatureSpecification featSpec, final ClassifierFactory factory,
									final String actionFile,
									final String deprelLeftFile, final String deprelRightFile) {
		super(featSpec, factory, actionFile, deprelLeftFile, deprelRightFile);
	}

	public Iterator<String[]> predict(final SimpleConfig config) {
		final Features actionFeats = extract(config);
		final String[] acs = actionClassifier.predictAll(actionFeats);
		return new ActionIterator(config, acs);
	}

	protected class ActionIterator implements Iterator<String[]> {
		private final SimpleConfig config;
		private final LinkedList<String> acs;
		public ActionIterator(final SimpleConfig config, String[] acs) {
			this.config = config;
			this.acs = new LinkedList(Arrays.asList(acs));
		}

		@Override
		public boolean hasNext() {
			return !acs.isEmpty();
		}

		@Override
		public String[] next() {
			if (acs.isEmpty())
				return null;
			String ac = acs.removeFirst();
			if (ac.equals(shift) || ac.equals(rightArc)) {
				return new String[]{ac};
			} else {
				Features deprelFeats = extract(config);
				if (!useTwoDeprelClassifiers)
					deprelFeats.putString("action", ac);
				String deprel = getDeprelClassifier(ac).predict(deprelFeats);
				return new String[]{ac, deprel};
			}
		}
	}

	protected void extractParentPoss(final Features feats, final SimpleConfig config) {
		for (int i : featSpec.getIntsFeature("parentPoss")) {
			String posStr = null;
			if (i < config.prefixLength() - 1 &&
					config.getLabelRight(i).equals(RevisedArcEagerParser.rightChild)) {
				DependencyVertex parent = config.getPrefixRight(i+1);
				if (parent != null) {
					Upos pos = parent.getToken().upos;
					if (pos != null) {
						posStr = pos.toString();
					}
				}
			}
			feats.putString(parentPosFeature(i), posStr);
		}
	}

	protected String[] actionNames() {
		return RevisedArcEagerParser.actionNames;
	}

	protected String[] prefixLabels() {
		return RevisedArcEagerParser.prefixLabels;
	}
}
