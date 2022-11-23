/*
 * Copyright (c) 2018. University of St Andrews
 */

package standrews.classification;

public class DeeplearningClassifierFactory extends ClassifierFactory {

    public Classifier makeClassifier(final String filename) {
        Classifier c = new DeeplearningClassifier(filename);
        c.setContinuousTraining(continuousTraining);
        c.setBatchSize(batchSize);
        c.setNEpochs(nEpochs);
        return c;
    }
}
