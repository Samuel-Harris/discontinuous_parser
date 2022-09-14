/*
 * Copyright (c) 2019. University of St Andrews
 */

package standrews.constmain;

import standrews.constbase.ConstTree;
import standrews.constbase.ConstTreebank;
import standrews.constextract.WholeHatExtractor;
import standrews.constmethods.WholeHatParser;

public class WholeHatTrainer {

	public int train(
			final ConstTreebank treebank,
			final int n,
			final WholeHatExtractor extractor) {
		final ConstTreebank subbank = treebank.part(0, n);
		int i = 0;
		for (ConstTree tree : subbank.getTrees()) {
				WholeHatParser parser = new WholeHatParser(tree);
				parser.observe(extractor);
				i++;
		}
		return i;
	}

}
