/*
 * Copyright (c) 2018. University of St Andrews
 */

package standrews.classification;

public abstract class ClassifierFactory {
    protected boolean continuousTraining = false;
    protected int batchSize = 200;
    protected int nEpochs = 1;

    public void setContinuousTraining(final boolean b) {
        continuousTraining = b;
    }

    public boolean getContinuousTraining() {
        return continuousTraining;
    }

    public void setBatchSize(final int n) {
        batchSize = n;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setNEpochs(final int n) {
        nEpochs = n;
    }

    public int getNEpochs() {
        return nEpochs;
    }

    public abstract Classifier makeClassifier(final String filename);

    public Classifier makeClassifier() {
        return makeClassifier(null);
    }
}
