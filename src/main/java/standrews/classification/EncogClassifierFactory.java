/*
 * Copyright (c) 2018. University of St Andrews
 */

package standrews.classification;

public class EncogClassifierFactory extends ClassifierFactory {
	public Classifier makeClassifier(final String filename) {
		return new EncogClassifier(filename);
	}
}
