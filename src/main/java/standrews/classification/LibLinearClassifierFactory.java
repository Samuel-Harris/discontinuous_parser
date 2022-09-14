/*
 * Copyright (c) 2018. University of St Andrews
 */

package standrews.classification;

public class LibLinearClassifierFactory extends ClassifierFactory {
	public Classifier makeClassifier(final String filename) {
		return new LibLinearClassifier(filename);
	}
}
