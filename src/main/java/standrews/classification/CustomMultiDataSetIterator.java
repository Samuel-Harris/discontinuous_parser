package standrews.classification;

import org.nd4j.common.primitives.Pair;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.MultiDataSet;
import org.nd4j.linalg.dataset.api.MultiDataSetPreProcessor;
import org.nd4j.linalg.dataset.api.iterator.MultiDataSetIterator;
import org.nd4j.linalg.factory.Nd4j;

import java.util.ArrayList;
import java.util.Arrays;


public class CustomMultiDataSetIterator implements MultiDataSetIterator {
    double[][] staticFeatureArray;
    double[][][] stackFeatureArray;
    double[][][] bufferFeatureArray;
    double[][] labelArray;
    boolean returned;
    int largestStackSize;
    int largestBufferSize;
    double[][] stackFeatureMaskArray;
    double[][] bufferFeatureMaskArray;

    public CustomMultiDataSetIterator(ArrayList<Pair<FeatureVector, double[]>> observations) {
        largestStackSize = 0;
        largestBufferSize = 0;

        for (Pair<FeatureVector, double[]> featureLabelPair: observations) {
            FeatureVector featureVector = featureLabelPair.getKey();
            double[][] stackFeatureVectors = featureVector.getStackElementVectors();
            double[][] bufferFeatureVectors = featureVector.getBufferElementVectors();

            largestStackSize = Math.max(largestStackSize, stackFeatureVectors[0].length);
            largestBufferSize = Math.max(largestBufferSize, bufferFeatureVectors[0].length);
        }

        staticFeatureArray = new double[observations.size()][observations.get(0).getKey().getStaticFeatures().length];
        stackFeatureArray = new double[observations.size()][observations.get(0).getKey().getStackElementVectors().length][largestStackSize];
        bufferFeatureArray = new double[observations.size()][observations.get(0).getKey().getBufferElementVectors().length][largestBufferSize];
        labelArray = new double[observations.size()][observations.get(0).getValue().length];
        stackFeatureMaskArray = new double[observations.size()][largestStackSize];
        bufferFeatureMaskArray = new double[observations.size()][largestBufferSize];

        for (int i=0; i<observations.size(); i++) {
            Pair<FeatureVector, double[]> featureLabelPair = observations.get(i);
            FeatureVector featureVector = featureLabelPair.getKey();
            double[] staticFeatureVector = featureVector.getStaticFeatures();
            double[][] stackFeatureVectors = featureVector.getStackElementVectors();
            double[][] bufferFeatureVectors = featureVector.getBufferElementVectors();

            staticFeatureArray[i] = staticFeatureVector;

            int stackSize = stackFeatureVectors[0].length;
            for (int j = 0; j < stackFeatureVectors.length; j++) {
                System.arraycopy(stackFeatureVectors[j], 0, stackFeatureArray[i][j], largestStackSize-stackSize, stackSize);
            }
            Arrays.fill(stackFeatureMaskArray[i], largestStackSize-stackSize, largestStackSize, 1.0);

            int bufferSize = bufferFeatureVectors[0].length;
            for (int j = 0; j < bufferFeatureVectors.length; j++) {
                System.arraycopy(bufferFeatureVectors[j], 0, bufferFeatureArray[i][j], largestBufferSize-bufferSize, bufferSize);
            }
            Arrays.fill(bufferFeatureMaskArray[i], largestBufferSize-bufferSize, largestBufferSize, 1.0);

            labelArray[i] = featureLabelPair.getValue();
        }

        returned = false;
    }

    @Override
    public MultiDataSet next(int i) {
        return null;
    }

    @Override
    public void setPreProcessor(MultiDataSetPreProcessor multiDataSetPreProcessor) {

    }

    @Override
    public MultiDataSetPreProcessor getPreProcessor() {
        return null;
    }

    @Override
    public boolean resetSupported() {
        return false;
    }

    @Override
    public boolean asyncSupported() {
        return false;
    }

    @Override
    public void reset() {

    }

    @Override
    public boolean hasNext() {
        return !returned;
    }

    @Override
    public MultiDataSet next() {
        INDArray staticFeatureINDArray = Nd4j.create(staticFeatureArray);
        INDArray stackFeatureINDArray = Nd4j.create(stackFeatureArray);
        INDArray bufferFeatureINDArray = Nd4j.create(bufferFeatureArray);
        INDArray stackFeatureMaskINDArray = Nd4j.create(stackFeatureMaskArray);
        INDArray bufferFeatureMaskINDArray = Nd4j.create(bufferFeatureMaskArray);
        INDArray labelINDArray = Nd4j.create(labelArray);

        INDArray[] features = new INDArray[]{staticFeatureINDArray, stackFeatureINDArray, bufferFeatureINDArray};
        INDArray[] featureMasks = new INDArray[]{null, stackFeatureMaskINDArray, bufferFeatureMaskINDArray};
        INDArray[] labels = new INDArray[]{labelINDArray};

        returned = true;
        return new org.nd4j.linalg.dataset.MultiDataSet(features, labels, featureMasks, null);
    }
}
