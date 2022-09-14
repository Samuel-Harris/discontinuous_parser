/*
 * Copyright (c) 2019. University of St Andrews
 */

package standrews.depbase;

public class SentenceFormIterator extends SentenceTokenIterator {

	public SentenceFormIterator(final String path) {
		super(path);
	}

	protected String flattenToken(final Token token) {
		return token.form.toString();
	}

}
