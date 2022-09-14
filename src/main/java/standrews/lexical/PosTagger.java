/*
 * Copyright (c) 2019. University of St Andrews
 */

package standrews.lexical;

import standrews.depbase.Token;

public interface PosTagger {
	public String[] tag(final String[] words);
	public Token[] retag(final Token[] words);
}
