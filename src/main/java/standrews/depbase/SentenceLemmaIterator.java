/*
 * Copyright (c) 2019. University of St Andrews
 */

package standrews.depbase;

public class SentenceLemmaIterator extends SentenceTokenIterator {

	public SentenceLemmaIterator(final String path) {
		super(path);
	}

	protected String flattenToken(final Token token) {
		return token.lemma.toString();
	}
}
