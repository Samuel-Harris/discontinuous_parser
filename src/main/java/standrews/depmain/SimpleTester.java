/*
 * Copyright (c) 2018. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package standrews.depmain;

import standrews.classification.FeatureSpecification;
import standrews.depextract.SimpleExtractor;
import standrews.depbase.DependencyStructure;
import standrews.depbase.Token;
import standrews.depbase.Treebank;
import standrews.depmethods.DeterministicParser;
import standrews.depmethods.SimpleParser;

import java.io.*;
import java.util.logging.Logger;

public class SimpleTester {

	protected FeatureSpecification featSpec;

	public SimpleTester(final FeatureSpecification featSpec) {
		this.featSpec = featSpec;
	}

	public int test(final String corpus,
					final String corpusParsed,
					final int n, final SimpleExtractor extractor) {
		return test(corpus, null, corpusParsed, n, extractor);
	}

	public int test(final String corpus,
					final String corpusCopy,
					final String corpusParsed,
					final int n, final SimpleExtractor extractor) {
		final Treebank treebank = new Treebank(corpus, n);
		PrintWriter goldWriter = null;
		PrintWriter parsedWriter = null;
		try {
			if (corpusCopy != null)
				goldWriter = new PrintWriter(corpusCopy, "UTF-8");
			parsedWriter = new PrintWriter(corpusParsed, "UTF-8");
		} catch (FileNotFoundException e) {
			fail("Cannot create file: " + e);
		} catch (UnsupportedEncodingException e) {
			fail("Unsupported encoding: " + e);
		}
		int i = 0;
		for (DependencyStructure struct : treebank.depStructs) {
			final Token[] tokens = retaggedTokens(struct);
			final DependencyStructure goldStruct =
					new DependencyStructure(tokens);
			goldWriter.println(goldStruct);
			DeterministicParser parser = makeParser(tokens);
			Token[] parsed = parser.parse(extractor);

			final DependencyStructure parsedStruct =
					new DependencyStructure(parsed);
			parsedWriter.println(parsedStruct);
			i++;
		}
		if (goldWriter != null)
			goldWriter.close();
		parsedWriter.close();
		return i;
	}

	protected Token[] retaggedTokens(DependencyStructure struct) {
		final Token[] tokens = struct.getNormalTokens();
		if (!featSpec.getGoldPos()) {
			return featSpec.getPosTagger().retag(tokens);
		} else {
			return tokens;
		}
	}

	protected DeterministicParser makeParser(final Token[] tokens) {
		return new SimpleParser(tokens);
	}

	/**
	 * Report failure.
	 *
	 * @param message The thing that failed.
	 */
	protected static void fail(final String message) {
		final Logger log = Logger.getLogger(SimpleTester.class.getName());
		log.setParent(Logger.getGlobal());
		log.severe(message);
	}
}
