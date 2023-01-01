package standrews.classification;

//import javafx.util.Pair;
import javafx.beans.value.WritableDoubleValue;
import org.datavec.api.records.reader.RecordReader;
import org.datavec.api.records.reader.SequenceRecordReader;
import org.datavec.api.records.reader.factory.RecordReaderFactory;
import org.datavec.api.records.reader.impl.collection.CollectionRecordReader;
import org.datavec.api.records.reader.impl.csv.CSVRecordReader;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.deeplearning4j.datasets.datavec.RecordReaderMultiDataSetIterator;
import org.deeplearning4j.datasets.iterator.DoublesDataSetIterator;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.nd4j.common.primitives.Pair;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.MultiDataSet;
import org.nd4j.linalg.factory.Nd4j;

import java.io.*;
import java.util.*;

public class MLP {
    private final ComputationGraph network;
    private final ArrayList<Pair<FeatureVector, double[]>> observations;
    private final ResponseVectorGenerator responseVectorGenerator;
    private boolean isTraining;
    private boolean isValidating;
    private int epochsWithoutImprovement;
    private double bestLossScore;
    private double lastLossScore;
    private double lastTrainLossScore;
    private final int miniBatchSize;
    private final double tol;
    private final int patience;

    public MLP(ComputationGraph network, ResponseVectorGenerator responseVectorGenerator, int miniBatchSize, double tol, int patience) {
        this.network = network;
        this.responseVectorGenerator = responseVectorGenerator;
        this.miniBatchSize = miniBatchSize;
        this.tol = tol;
        this.patience = patience;

        observations = new ArrayList<>();
        isTraining = true;
        isValidating = false;
        epochsWithoutImprovement = 0;
        bestLossScore = Double.POSITIVE_INFINITY;
        lastLossScore = Double.POSITIVE_INFINITY;
    }

    public MLP(ComputationGraph network, ResponseVectorGenerator responseVectorGenerator, int miniBatchSize, double tol, int patience, File lossFile) throws IOException {
        this.network = network;
        this.responseVectorGenerator = responseVectorGenerator;
        this.miniBatchSize = miniBatchSize;
        this.tol = tol;
        this.patience = patience;

        observations = new ArrayList<>();

        double[] lossValues;
        try (BufferedReader br = new BufferedReader(new FileReader(lossFile))) {
            lossValues = Arrays.stream(br.readLine().split(",")).mapToDouble(Double::parseDouble).toArray();
        }

        bestLossScore = Double.POSITIVE_INFINITY;
        int minLossIndex = -1;
        for (int i = 0; i < lossValues.length; i++) {
            if (lossValues[i] < bestLossScore-tol) {
                bestLossScore = lossValues[i];
                minLossIndex = i;
            }
        }
        epochsWithoutImprovement = lossValues.length-1-minLossIndex;

        isTraining = epochsWithoutImprovement <= patience;
        isValidating = false;
        lastLossScore = lossValues[lossValues.length-1];
    }

    private void clearObservations() {
//        observations = new ArrayList<>();
        observations.clear();
//        System.gc();
    }

    public void addObservation(FeatureVector featureVectors, Object response) {
        if (isValidating && isTraining) {
            observations.add(new Pair<>(featureVectors, responseVectorGenerator.generateResponseVector(response)));
        } else if (isTraining) {
            observations.add(new Pair<>(featureVectors, responseVectorGenerator.generateResponseVector(response)));
            if (isTraining && observations.size() == miniBatchSize) {
                train();
            }
        }
    }

    public boolean isTraining() {
        return isTraining;
    }

    public void startValidating() {
        isValidating = true;
    }

    public void stopValidating() {
        isValidating = false;
    }

    public void applyEarlyStoppingIfApplicable(double trainLossScore, double validLossScore) {
        if (!isTraining) {
            clearObservations();
            return;
        }

        applyEarlyStoppingIfApplicable(validLossScore);
        this.lastTrainLossScore = trainLossScore;
    }

    public void applyEarlyStoppingIfApplicable(double lossScore) {
        if (!isTraining) {
            clearObservations();
            return;
        }

        lastLossScore = lossScore == 0 ? Double.POSITIVE_INFINITY : lossScore;

        if (lossScore < bestLossScore-tol) {
            bestLossScore = lossScore;
            epochsWithoutImprovement = 0;
        } else {
            epochsWithoutImprovement++;

            if (epochsWithoutImprovement > patience) {
                isTraining = false;
            }
        }
    }

    public int getEpochsWithoutImprovement() {
        return epochsWithoutImprovement;
    }

    public double getLastTrainLossScore() {
        return lastTrainLossScore;
    }

    public double getLastLossScore() {
        return lastLossScore;
    }

    public void train() {
        if (observations.isEmpty() || !isTraining) {
            clearObservations();
            return;
        }

        network.fit(new CustomMultiDataSetIterator(observations));

        clearObservations();
        Runtime runtime = Runtime.getRuntime();
        if (((double) runtime.totalMemory() - (double) runtime.freeMemory())/((double) runtime.maxMemory()) > 0.8) {
            System.gc();
            Nd4j.getMemoryManager().invokeGc();
        }
    }

    public void save(String filePath) throws IOException {
        network.save(new File(filePath));
    }

    public double validateMiniBatch() {
        if (isTraining && !observations.isEmpty()) {

            INDArray lossScores = network.scoreExamples(new CustomMultiDataSetIterator(observations).next(), false);
            double[] scores = lossScores.toDoubleVector();

            double lossScoreSum = network.score(new CustomMultiDataSetIterator(observations).next());

            clearObservations();

            Runtime runtime = Runtime.getRuntime();
            if (((double) runtime.totalMemory() - (double) runtime.freeMemory())/((double) runtime.maxMemory()) > 0.8) {
                System.gc();
                Nd4j.getMemoryManager().invokeGc();
            }

            return lossScoreSum;
        } else {
            clearObservations();
            return 0;
        }
    }

    public Object predict(FeatureVector featureVector) {
        return predictAll(featureVector)[0];
    }

    public Object[] predictAll(FeatureVector featureVector) {  // predicts action and orders them by most probable
        try (INDArray hatFeatureINDArray = Nd4j.create(featureVector.getStaticFeatures());
             INDArray stackFeatureINDArray = Nd4j.create(featureVector.getStackElementVectors());
             INDArray bufferFeatureINDArray = Nd4j.create(featureVector.getStackElementVectors())) {
            INDArray[] features = new INDArray[]{hatFeatureINDArray, stackFeatureINDArray, bufferFeatureINDArray};
            INDArray[] output = network.output(features);
            double[] scores = output[0].toDoubleVector();

            return responseVectorGenerator.getLabelsFromScores(scores);
        }
    }
}
