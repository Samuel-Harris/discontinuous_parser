/*
 * Copyright (c) 2018. University of St Andrews
 */

package standrews.classification;

public class LibSVMClassifierFactory extends ClassifierFactory {
    public Classifier makeClassifier(final String fileName) {
        return new LibSVMClassifier(fileName);
    }
}
