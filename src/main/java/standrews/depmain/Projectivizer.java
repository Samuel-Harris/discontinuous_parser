/*
 * Copyright (c) 2018. University of St Andrews
 */

package standrews.depmain;

import standrews.aux.DataCollection;
import standrews.depbase.DependencyStructure;
import standrews.depbase.Token;
import standrews.depbase.Treebank;
import standrews.tabular.OptimalProjectivizer;

import java.util.logging.Logger;

public class Projectivizer {

	public void projectivize(final String corpus, final String newCorpus) {
		projectivize(corpus, newCorpus, Integer.MAX_VALUE);
	}

	public void projectivize(final String corpus, final String newCorpus, final int n) {
		final Treebank treebank = new Treebank(corpus, n);
		final Treebank projBank = projectivize(treebank);
		projBank.write(newCorpus);
		/*
		PrintWriter writer = null;
		try {
			writer = new PrintWriter(newCorpus, "UTF-8");
		} catch (FileNotFoundException e) {
			fail("Cannot create file: " + e);
		} catch (UnsupportedEncodingException e) {
			fail("Unsupported encoding: " + e);
		}
		for (DependencyStructure struct : treebank.depStructs) {
			final Token[] tokens = struct.getNormalTokens();
			Token[] projTokens = OptimalProjectivizer.nonprojectiveAllowed(tokens);
			final DependencyStructure projStruct =
						new DependencyStructure(projTokens);
			writer.println(projStruct);
		}
		writer.close();
		*/
	}

	public Treebank projectivize(final Treebank treebank) {
		final DependencyStructure[] projStructs = new DependencyStructure[treebank.depStructs.length];
		for (int i = 0; i < treebank.depStructs.length; i++) {
			final Token[] tokens = treebank.depStructs[i].getNormalTokens();
			Token[] projTokens = OptimalProjectivizer.projectivize(tokens);
			projStructs[i] = new DependencyStructure(projTokens);
		}
		return new Treebank(projStructs);
	}

	public String projectivizeCount(final Treebank treebank) {
		DataCollection datas = new DataCollection();
		for (int i = 0; i < treebank.depStructs.length; i++) {
			final Token[] tokens = treebank.depStructs[i].getNormalTokens();
			double c = OptimalProjectivizer.countProjectivization(tokens);
			// double mult = Math.log(OptimalProjectivizer.countProjectivization(tokens)) / Math.log(2.0);
			// double mult = OptimalProjectivizer.countProjectivization(tokens);
			datas.add("", tokens.length, c);
		}
		return datas.geoAveragesString("", 1);
	}

	/**
	 * Report failure.
	 *
	 * @param message The thing that failed.
	 */
	protected static void fail(final String message) {
		final Logger log = Logger.getLogger(Projectivizer.class.getName());
		log.setParent(Logger.getGlobal());
		log.severe(message);
	}

}
