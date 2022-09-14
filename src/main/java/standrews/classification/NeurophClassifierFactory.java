/*
 * Copyright (c) 2018. University of St Andrews
 */

package standrews.classification;

public class NeurophClassifierFactory extends ClassifierFactory {
	public Classifier makeClassifier(final String filename) {
		return new NeurophClassifier(filename);
	}
}
